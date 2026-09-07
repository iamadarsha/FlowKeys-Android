package com.flowkeys.android

import android.app.Application
import android.content.ComponentCallbacks2
import com.flowkeys.android.core.coordinator.FlowKeysCoordinator

/**
 * Main Application class for FlowKeys Android.
 * Initializes the central coordinator and registers system memory pressure listeners.
 */
class FlowKeysApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FlowKeysCoordinator.initialize(this)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Drop speculative model caches and reset non-essential resources
            FlowKeysCoordinator.reset()
        }
    }
}
