package com.flowkeys.android.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.flowkeys.android.MainActivity
import com.flowkeys.android.asr.SpeechRecognitionManager
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.DictationState
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle, positioning, and window flags of the floating micro-pill overlay.
 *
 * ARCHITECTURE NOTE: The overlay is rendered using TYPE_ACCESSIBILITY_OVERLAY, which is
 * provided directly by the AccessibilityService context. This eliminates the need for the
 * SYSTEM_ALERT_WINDOW permission entirely. The AccessibilityService must be running for
 * the overlay to exist — which is already required for all other FlowKeys features.
 */
class FloatingPillManager(private val context: Context) : FloatingPillView.Listener {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null

    private var pillView: FloatingPillView? = null
    private val speechRecognitionManager = SpeechRecognitionManager(context)

    private val windowParams = WindowManager.LayoutParams().apply {
        // TYPE_ACCESSIBILITY_OVERLAY: available to AccessibilityService context.
        // No SYSTEM_ALERT_WINDOW permission required. Overlay is managed by the OS
        // Accessibility layer and is not interactable by other processes.
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        gravity = Gravity.TOP or Gravity.START
        width = dpToPx(70)
        height = dpToPx(34)
        x = 0
        y = dpToPx(300)
    }

    var isAttached: Boolean = false
        private set

    private val screenSize = Point()
    private var hasCustomPosition = false

    fun attach() {
        if (isAttached) return
        // No canDrawOverlays check needed — TYPE_ACCESSIBILITY_OVERLAY is granted
        // implicitly when the AccessibilityService is running and connected.

        updateScreenDimensions()

        // Default position: snap to right margin right above soft keyboard
        windowParams.x = screenSize.x - windowParams.width - dpToPx(16)
        windowParams.y = (screenSize.y * 0.62f).toInt() - windowParams.height - dpToPx(8)

        pillView = FloatingPillView(context).apply {
            listener = this@FloatingPillManager
            visibility = View.GONE // Hidden initially; only appears when keyboard/field is active
        }

        windowManager.addView(pillView, windowParams)
        isAttached = true

        stateObserverJob = scope.launch {
            launch {
                FlowKeysCoordinator.dictationState.collectLatest { state ->
                    handleStateChange(state)
                }
            }
            launch {
                FlowKeysCoordinator.selectedLanguage.collectLatest { lang ->
                    pillView?.updateLanguage(lang)
                }
            }
            launch {
                FlowKeysCoordinator.scriptMode.collectLatest { mode ->
                    pillView?.updateScriptMode(mode)
                }
            }
            launch {
                FlowKeysCoordinator.targetTranslationLanguage.collectLatest { target ->
                    pillView?.updateTargetLanguage(target)
                }
            }
        }
    }

    fun detach() {
        speechRecognitionManager.cancel()
        stateObserverJob?.cancel()
        stateObserverJob = null
        if (isAttached && pillView != null) {
            try {
                windowManager.removeView(pillView)
            } catch (ignored: Exception) {}
            pillView = null
            isAttached = false
        }
    }

    private fun handleStateChange(state: DictationState) {
        val view = pillView ?: return
        view.updateState(state)

        when (state) {
            DictationState.Dormant -> {
                // Completely hide pill when keyboard is closed or no field is active
                view.visibility = View.GONE
            }
            is DictationState.FieldFocused -> {
                view.visibility = View.VISIBLE
                view.alpha = 1.0f
                positionAboveKeyboard(state.imeTop, state.fieldBounds)
            }
            is DictationState.Recording,
            is DictationState.Processing,
            is DictationState.Success,
            is DictationState.Error -> {
                view.visibility = View.VISIBLE
                view.alpha = 1.0f
            }
        }
    }

    private fun positionAboveKeyboard(imeTop: Int?, fieldBounds: android.graphics.Rect) {
        if (hasCustomPosition) return

        updateScreenDimensions()
        val targetY: Int = if (imeTop != null && imeTop > 0) {
            imeTop - windowParams.height - dpToPx(12)
        } else if (fieldBounds.top > 0 && fieldBounds.top < screenSize.y) {
            fieldBounds.top - windowParams.height - dpToPx(8)
        } else {
            (screenSize.y * 0.60f).toInt() - windowParams.height - dpToPx(12)
        }.coerceIn(dpToPx(50), screenSize.y - windowParams.height - dpToPx(60))

        val targetX = screenSize.x - windowParams.width - dpToPx(16)

        windowParams.x = targetX
        animateYTo(targetY)
    }

    override fun onPillPressStart() {
        startDictation()
    }

    override fun onPillPressRelease(isHoldAction: Boolean) {
        if (isHoldAction) {
            stopDictation()
        }
    }

    override fun onPillDoubleTapped() {
        val current = FlowKeysCoordinator.selectedLanguage.value
        val next = when (current) {
            Language.ENGLISH -> Language.BENGALI
            Language.BENGALI -> Language.HINDI
            Language.HINDI -> Language.ENGLISH
        }
        FlowKeysCoordinator.setLanguage(next)
        Toast.makeText(context, "Language: ${next.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onPillTripleTapped() {
        val current = FlowKeysCoordinator.scriptMode.value
        val next = when (current) {
            ScriptMode.TRANSLATED_ENGLISH -> ScriptMode.NATIVE_SCRIPT
            ScriptMode.NATIVE_SCRIPT -> ScriptMode.PHONETIC_LATIN
            ScriptMode.PHONETIC_LATIN -> ScriptMode.TRANSLATED_ENGLISH
        }
        FlowKeysCoordinator.setScriptMode(next)
        Toast.makeText(context, "Script: ${next.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onPillClicked() {
        val currentState = FlowKeysCoordinator.dictationState.value
        when (currentState) {
            is DictationState.Recording -> {
                stopDictation()
            }
            is DictationState.FieldFocused,
            DictationState.Dormant -> {
                startDictation()
            }
            is DictationState.Error -> {
                if (currentState.recoverableText != null) {
                    FlowKeysCoordinator.onTextReady(currentState.recoverableText, context)
                } else {
                    FlowKeysCoordinator.reset()
                }
            }
            else -> {}
        }
    }

    private fun startDictation() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            FlowKeysCoordinator.onError("Microphone permission required")
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        val language = FlowKeysCoordinator.selectedLanguage.value
        speechRecognitionManager.startListening(language)
    }

    private fun stopDictation() {
        speechRecognitionManager.stopListening()
    }

    override fun onPillDragged(dx: Float, dy: Float) {
        hasCustomPosition = true
        windowParams.x += dx.toInt()
        windowParams.y += dy.toInt()
        if (isAttached && pillView != null) {
            windowManager.updateViewLayout(pillView, windowParams)
        }
    }

    override fun onDragFinished() {
        snapToNearestEdge()
    }

    private fun snapToNearestEdge() {
        val currentX = windowParams.x
        val midX = (screenSize.x - windowParams.width) / 2
        val targetX = if (currentX < midX) {
            dpToPx(12) // Snap Left
        } else {
            screenSize.x - windowParams.width - dpToPx(12) // Snap Right
        }

        ValueAnimator.ofInt(windowParams.x, targetX).apply {
            duration = 180
            addUpdateListener {
                windowParams.x = it.animatedValue as Int
                if (isAttached && pillView != null) {
                    windowManager.updateViewLayout(pillView, windowParams)
                }
            }
            start()
        }
    }

    private fun animateYTo(targetY: Int) {
        ValueAnimator.ofInt(windowParams.y, targetY).apply {
            duration = 200
            addUpdateListener {
                windowParams.y = it.animatedValue as Int
                if (isAttached && pillView != null) {
                    windowManager.updateViewLayout(pillView, windowParams)
                }
            }
            start()
        }
    }

    private fun updateScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenSize.x = bounds.width()
            screenSize.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(screenSize)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
