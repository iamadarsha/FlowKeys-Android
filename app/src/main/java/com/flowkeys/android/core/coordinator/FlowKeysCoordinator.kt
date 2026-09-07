package com.flowkeys.android.core.coordinator

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.flowkeys.android.accessibility.TextInsertionEngine
import com.flowkeys.android.core.model.DeviceTier
import com.flowkeys.android.core.model.DictationState
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central state coordinator orchestrating lifecycle events across:
 * - Accessibility Service (focus and insertion)
 * - Floating Overlay View (visual pill and touch input)
 * - Recording Service (audio capture & VAD)
 * - Speech Recognition & Polishing Engines
 */
object FlowKeysCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _dictationState = MutableStateFlow<DictationState>(DictationState.Dormant)
    val dictationState: StateFlow<DictationState> = _dictationState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(Language.DEFAULT)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    private val _scriptMode = MutableStateFlow(ScriptMode.DEFAULT)
    val scriptMode: StateFlow<ScriptMode> = _scriptMode.asStateFlow()

    private val _targetTranslationLanguage = MutableStateFlow<Language?>(null)
    val targetTranslationLanguage: StateFlow<Language?> = _targetTranslationLanguage.asStateFlow()

    private val _activeDictationContext = MutableStateFlow<com.flowkeys.android.core.model.DictationContext?>(null)
    val activeDictationContext: StateFlow<com.flowkeys.android.core.model.DictationContext?> = _activeDictationContext.asStateFlow()

    private var activeTargetNode: AccessibilityNodeInfo? = null
    private var accessibilityServiceRef: java.lang.ref.WeakReference<android.accessibilityservice.AccessibilityService>? = null
    var deviceTier: DeviceTier = DeviceTier.TIER_1_4GB
        private set
    private var dataStoreManager: com.flowkeys.android.data.DataStoreManager? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        deviceTier = DeviceTier.detect(appContext)
        val dsm = com.flowkeys.android.data.DataStoreManager(appContext)
        dataStoreManager = dsm
        scope.launch {
            dsm.selectedLanguage.collect { lang ->
                _selectedLanguage.value = lang
            }
        }
        scope.launch {
            dsm.scriptMode.collect { mode ->
                _scriptMode.value = mode
            }
        }
        scope.launch {
            dsm.targetTranslationLanguage.collect { target ->
                _targetTranslationLanguage.value = target
            }
        }
    }

    fun setAccessibilityService(service: android.accessibilityservice.AccessibilityService?) {
        accessibilityServiceRef = if (service != null) java.lang.ref.WeakReference(service) else null
    }

    fun setLanguage(language: Language) {
        _selectedLanguage.value = language
        scope.launch {
            dataStoreManager?.setLanguage(language)
        }
    }

    fun setScriptMode(mode: ScriptMode) {
        _scriptMode.value = mode
        scope.launch {
            dataStoreManager?.setScriptMode(mode)
        }
    }

    fun setTargetTranslation(language: Language?) {
        _targetTranslationLanguage.value = language
        scope.launch {
            dataStoreManager?.setTargetTranslation(language)
        }
    }

    private fun getActiveOrFocusedNode(): AccessibilityNodeInfo? {
        var target = activeTargetNode
        if (target == null || !target.isEditable) {
            val service = accessibilityServiceRef?.get()
            target = service?.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
        return target
    }

    /**
     * Called when dictation starts to prepare dynamic streaming insertion buffer.
     */
    fun onDictationStarted() {
        val target = getActiveOrFocusedNode()
        TextInsertionEngine.startSession(target)
    }

    /**
     * Called when real-time partial speech recognition tokens are available.
     */
    fun onPartialTextAvailable(partialText: String) {
        val target = getActiveOrFocusedNode()
        TextInsertionEngine.insertStreamingPartial(target, partialText)
    }

    /**
     * Called when an eligible editable field receives focus.
     */
    fun onFieldFocused(node: AccessibilityNodeInfo, packageName: String, bounds: Rect, imeTop: Int? = null) {
        if (activeTargetNode != node) {
            safelyReleaseActiveNode()
            activeTargetNode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                AccessibilityNodeInfo(node)
            } else {
                @Suppress("DEPRECATION")
                AccessibilityNodeInfo.obtain(node)
            }
        }

        val fieldType = com.flowkeys.android.accessibility.FieldClassifier.classifyField(node, packageName)
        _activeDictationContext.value = com.flowkeys.android.core.model.DictationContext(
            packageName = packageName,
            fieldType = fieldType,
            language = _selectedLanguage.value,
            existingContentLength = node.text?.length ?: 0
        )

        _dictationState.value = DictationState.FieldFocused(
            packageName = packageName,
            fieldBounds = bounds,
            selectedLanguage = _selectedLanguage.value,
            existingTextLength = node.text?.length ?: 0,
            imeTop = imeTop
        )
    }

    /**
     * Called when the text field loses focus or is dismissed.
     */
    fun onFieldUnfocused() {
        if (_dictationState.value is DictationState.Recording || _dictationState.value is DictationState.Processing) {
            // Do not dismiss while recording or processing is active
            return
        }
        _activeDictationContext.value = null
        safelyReleaseActiveNode()
        TextInsertionEngine.resetSession()
        _dictationState.value = DictationState.Dormant
    }

    /**
     * Updates recording status with audio level for waveform animation.
     */
    fun updateRecordingLevel(audioLevel: Float, durationMs: Long) {
        _dictationState.value = DictationState.Recording(
            durationMs = durationMs,
            audioLevel = audioLevel,
            language = _selectedLanguage.value
        )
    }

    /**
     * Transition to processing state when user finishes speaking.
     */
    fun setProcessing(message: String = "Processing…") {
        _dictationState.value = DictationState.Processing(
            language = _selectedLanguage.value,
            message = message
        )
    }

    /**
     * Delivers polished text to the active field.
     */
    fun onTextReady(text: String, context: Context): Boolean {
        val target = getActiveOrFocusedNode()
        val packageName = _activeDictationContext.value?.packageName ?: ""
        val language = _selectedLanguage.value

        if (target != null && target.isEditable) {
            val success = TextInsertionEngine.insertText(target, text, context)
            if (success) {
                com.flowkeys.android.history.DictationHistoryRepository.recordInsertion(text, language, packageName)
                _dictationState.value = DictationState.Success(
                    insertedText = text,
                    language = language
                )
                scope.launch {
                    kotlinx.coroutines.delay(1200L)
                    _dictationState.value = DictationState.Dormant
                }
                return true
            }
        }

        // Secondary Fallback: Save to recovery manager and clipboard so dictation is never lost
        com.flowkeys.android.recovery.DictationRecoveryManager.saveRecoverable(
            text = text,
            language = language,
            packageName = packageName,
            reason = "Direct node insertion failed, stored in recovery"
        )
        com.flowkeys.android.history.DictationHistoryRepository.recordInsertion(text, language, packageName)

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("FlowKeys Dictation", text)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "FlowKeys: Dictated text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            _dictationState.value = DictationState.Success(
                insertedText = text,
                language = language
            )
            scope.launch {
                kotlinx.coroutines.delay(1200L)
                _dictationState.value = DictationState.Dormant
            }
            return true
        } catch (e: Exception) {
            _dictationState.value = DictationState.Error(
                errorMessage = "Could not insert text",
                recoverableText = text
            )
            return false
        }
    }

    /**
     * Transition to error state.
     */
    fun onError(errorMessage: String, recoverableText: String? = null) {
        TextInsertionEngine.resetSession()
        if (!recoverableText.isNullOrBlank()) {
            val packageName = _activeDictationContext.value?.packageName ?: ""
            com.flowkeys.android.recovery.DictationRecoveryManager.saveRecoverable(
                text = recoverableText,
                language = _selectedLanguage.value,
                packageName = packageName,
                reason = errorMessage
            )
        }
        _dictationState.value = DictationState.Error(
            errorMessage = errorMessage,
            recoverableText = recoverableText
        )
        // Automatically dismiss error state after 1.8 seconds so pill never stays stuck
        scope.launch {
            kotlinx.coroutines.delay(1800L)
            if (_dictationState.value is DictationState.Error) {
                reset()
            }
        }
    }

    /**
     * Reset back to dormant.
     */
    fun reset() {
        _activeDictationContext.value = null
        safelyReleaseActiveNode()
        TextInsertionEngine.resetSession()
        _dictationState.value = DictationState.Dormant
    }

    private fun safelyReleaseActiveNode() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activeTargetNode?.recycle()
        }
        activeTargetNode = null
    }
}
