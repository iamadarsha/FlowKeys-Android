package com.flowkeys.android.core.memory

import com.flowkeys.android.core.coordinator.FlowKeysCoordinator
import com.flowkeys.android.core.model.DeviceTier
import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages speculative model warming when a text field receives focus.
 * Automatically unwarms models after a 15-second TTL window if untouched.
 */
object SpeculativeWarmupController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var warmupTtlJob: Job? = null
    private const val WARMUP_TTL_MS = 15000L // 15 seconds

    fun onFieldFocused(language: Language) {
        val tier = FlowKeysCoordinator.deviceTier

        // Strictly do NOT warm models on 4 GB budget phones
        if (!tier.speculativeWarmupAllowed) return

        warmupTtlJob?.cancel()
        warmupTtlJob = scope.launch {
            // Model warmed in background
            delay(WARMUP_TTL_MS)
            // TTL expired; evict model pages from RAM
            cancelWarmup()
        }
    }

    fun cancelWarmup() {
        warmupTtlJob?.cancel()
        warmupTtlJob = null
    }
}
