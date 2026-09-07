package com.flowkeys.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.DictationState
import com.flowkeys.android.overlay.FloatingPillManager
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.*

/**
 * System observer monitoring input focus and providing a bridge for text insertion.
 *
 * Privacy Guarantees:
 * - NEVER reads password or sensitive inputs.
 * - Inspects ONLY the currently focused input node; never traverses or logs the full screen tree.
 * - Does not collect personal analytics or telemetry.
 */
class FlowKeysAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: FloatingPillManager
    private var pendingFocusNode: AccessibilityNodeInfo? = null
    private var pendingPackageName: String = ""
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var imeMonitorJob: Job? = null

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // Screen turned off / device locked: abort recording and ensure overlay is hidden
                clearPendingFocus()
                imeMonitorJob?.cancel()
                try {
                    val cancelIntent = Intent(context, com.flowkeys.android.recording.DictationRecordingService::class.java).apply {
                        action = com.flowkeys.android.recording.DictationRecordingService.ACTION_CANCEL_RECORDING
                    }
                    context?.startService(cancelIntent)
                } catch (ignored: Exception) {}
                FlowKeysCoordinator.reset()
            }
        }
    }

    private fun clearPendingFocus() {
        pendingFocusNode = null
        pendingPackageName = ""
        pendingImePollJob?.cancel()
        imeMonitorJob?.cancel()
    }

    override fun onCreate() {
        super.onCreate()
        overlayManager = FloatingPillManager(this)
        FlowKeysCoordinator.initialize(this)
        val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FlowKeysCoordinator.setAccessibilityService(this)
        ensureOverlayAttached()
    }

    private fun ensureOverlayAttached() {
        if (!overlayManager.isAttached && Settings.canDrawOverlays(this)) {
            overlayManager.attach()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        ensureOverlayAttached()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewFocused(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                handleWindowStateChanged(event)
            }
        }
    }

    private var lastFocusedNode: AccessibilityNodeInfo? = null
    private var lastPackageName: String = ""
    private var lastBounds: Rect = Rect()

    private fun handleViewFocused(event: AccessibilityEvent) {
        val node: AccessibilityNodeInfo? = event.source
        if (node == null) {
            clearPendingFocus()
            lastFocusedNode = null
            FlowKeysCoordinator.onFieldUnfocused()
            return
        }

        val packageName = event.packageName?.toString() ?: ""
        if (FieldClassifier.isEligibleEditableField(node, packageName)) {
            val bounds = FieldClassifier.getBoundsInScreen(node)
            lastFocusedNode = node
            lastPackageName = packageName
            lastBounds = bounds
            notifyFieldFocused(node, packageName, bounds)
        } else {
            clearPendingFocus()
            lastFocusedNode = null
            FlowKeysCoordinator.onFieldUnfocused()
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val currentState = FlowKeysCoordinator.dictationState.value
        if (currentState is DictationState.Recording || currentState is DictationState.Processing) {
            return
        }

        val imeTop = detectImeTop()
        val node = lastFocusedNode

        // Check if the previously focused node is still valid and in an active editable field
        if (node != null && try { node.refresh() && FieldClassifier.isEligibleEditableField(node, lastPackageName) } catch (e: Exception) { false }) {
            FlowKeysCoordinator.onFieldFocused(node, lastPackageName, lastBounds, imeTop)
            if (imeTop != null) {
                startImeMonitor(node, lastPackageName, lastBounds)
            }
            return
        }

        // Otherwise scan active application window
        val windowList = try { windows } catch (e: Exception) { emptyList() }
        val appWindow = windowList.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val rootNode = appWindow?.root ?: rootInActiveWindow
        val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val packageName = event.packageName?.toString() ?: lastPackageName

        if (focusedNode != null && FieldClassifier.isEligibleEditableField(focusedNode, packageName)) {
            val bounds = FieldClassifier.getBoundsInScreen(focusedNode)
            lastFocusedNode = focusedNode
            lastPackageName = packageName
            lastBounds = bounds
            notifyFieldFocused(focusedNode, packageName, bounds)
        } else if (imeTop == null) {
            clearPendingFocus()
            lastFocusedNode = null
            FlowKeysCoordinator.onFieldUnfocused()
        }
    }

    private var pendingImePollJob: Job? = null

    private fun notifyFieldFocused(node: AccessibilityNodeInfo, packageName: String, bounds: Rect) {
        val imeTop = detectImeTop()
        android.util.Log.d("FlowKeysA11y", "notifyFieldFocused imeTop=$imeTop, pkg=$packageName")
        FlowKeysCoordinator.onFieldFocused(node, packageName, bounds, imeTop)

        if (imeTop != null) {
            startImeMonitor(node, packageName, bounds)
        } else {
            pendingImePollJob?.cancel()
            pendingImePollJob = serviceScope.launch {
                for (delayMs in listOf(150L, 300L, 500L)) {
                    delay(delayMs)
                    val delayedImeTop = detectImeTop()
                    android.util.Log.d("FlowKeysA11y", "delayedImeTop after ${delayMs}ms = $delayedImeTop")
                    if (delayedImeTop != null) {
                        FlowKeysCoordinator.onFieldFocused(node, packageName, bounds, delayedImeTop)
                        startImeMonitor(node, packageName, bounds)
                        break
                    }
                }
            }
        }
    }

    private fun startImeMonitor(node: AccessibilityNodeInfo, packageName: String, bounds: Rect) {
        imeMonitorJob?.cancel()
        imeMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(250)
                val state = FlowKeysCoordinator.dictationState.value
                if (state !is DictationState.FieldFocused) {
                    break
                }
                val currentImeTop = detectImeTop()
                if (currentImeTop == null) {
                    // Soft keyboard was dismissed
                    FlowKeysCoordinator.onFieldFocused(node, packageName, bounds, null)
                    break
                } else if (currentImeTop != state.imeTop) {
                    FlowKeysCoordinator.onFieldFocused(node, packageName, bounds, currentImeTop)
                }
            }
        }
    }

    /**
     * Inspects interactive window frames to locate the precise top edge of the soft keyboard (GBoard/IME).
     */
    private fun detectImeTop(): Int? {
        val windowList = try { windows } catch (e: Exception) { emptyList() }
        val imeWindow = windowList.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        if (imeWindow != null) {
            val rect = Rect()
            imeWindow.getBoundsInScreen(rect)
            android.util.Log.d("FlowKeysA11y", "Found IME window: bounds=$rect, layer=${imeWindow.layer}")
            if (rect.top > 0 && rect.height() > 0) {
                return rect.top
            }
            // Fallback to screen height - keyboard height if top is 0
            if (rect.height() > 0) {
                val displayMetrics = resources.displayMetrics
                val calculatedTop = displayMetrics.heightPixels - rect.height()
                android.util.Log.d("FlowKeysA11y", "Calculated IME top: $calculatedTop")
                return calculatedTop
            }
        }
        return null
    }

    private fun isImeVisible(): Boolean {
        val windowList = try { windows } catch (e: Exception) { emptyList() }
        val hasIme = windowList.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        android.util.Log.d("FlowKeysA11y", "isImeVisible: $hasIme (window count: ${windowList.size})")
        return hasIme
    }

    override fun onInterrupt() {
        clearPendingFocus()
        FlowKeysCoordinator.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (ignored: Exception) {}
        clearPendingFocus()
        FlowKeysCoordinator.setAccessibilityService(null)
        overlayManager.detach()
        FlowKeysCoordinator.reset()
    }
}
