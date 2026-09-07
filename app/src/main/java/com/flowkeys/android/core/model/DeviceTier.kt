package com.flowkeys.android.core.model

import android.app.ActivityManager
import android.content.Context

/**
 * Classifies the device into memory and capability tiers.
 * Used to dynamically adapt ASR threading, model preloading, and local LLM availability.
 */
enum class DeviceTier(
    val maxThreads: Int,
    val allowLocalLlm: Boolean,
    val speculativeWarmupAllowed: Boolean
) {
    TIER_1_4GB(
        maxThreads = 2,
        allowLocalLlm = false,
        speculativeWarmupAllowed = false // Conserve RAM under 4 GB pressure
    ),
    TIER_2_6GB(
        maxThreads = 3,
        allowLocalLlm = false, // Kept off by default on 6 GB for snappy response
        speculativeWarmupAllowed = true
    ),
    TIER_3_8GB_PLUS(
        maxThreads = 4,
        allowLocalLlm = true,
        speculativeWarmupAllowed = true
    );

    companion object {
        fun detect(context: Context): DeviceTier {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return TIER_1_4GB

            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalRamGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)

            return when {
                totalRamGb < 4.5 -> TIER_1_4GB
                totalRamGb < 7.2 -> TIER_2_6GB
                else -> TIER_3_8GB_PLUS
            }
        }
    }
}
