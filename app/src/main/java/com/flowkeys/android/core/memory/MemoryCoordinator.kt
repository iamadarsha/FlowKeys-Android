package com.flowkeys.android.core.memory

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * System-wide memory pressure coordinator.
 * Ensures FlowKeys respects 4 GB RAM limits and does not get killed by Android's Low Memory Killer (LMK).
 */
object MemoryCoordinator {

    private val _isLowMemoryAlert = MutableStateFlow(false)
    val isLowMemoryAlert: StateFlow<Boolean> = _isLowMemoryAlert.asStateFlow()

    @Suppress("DEPRECATION")
    fun handleTrimMemory(level: Int, context: Context) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Modest memory pressure; cancel any speculative warmups
                SpeculativeWarmupController.cancelWarmup()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // Severe memory pressure; hard-evict model pages and reset coordinator
                _isLowMemoryAlert.value = true
                SpeculativeWarmupController.cancelWarmup()
                FlowKeysCoordinator.reset()
            }
        }
    }

    fun getAvailableMemoryMb(context: Context): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }
}
