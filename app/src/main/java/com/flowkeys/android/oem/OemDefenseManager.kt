package com.flowkeys.android.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Detects OEM manufacturers and produces deep-link intents for battery optimization exemption
 * and background autostart configuration (Xiaomi, Samsung, BBK, Vivo).
 */
object OemDefenseManager {

    enum class OemType {
        XIAOMI,
        SAMSUNG,
        BBK_OPPO_REALME_ONEPLUS,
        VIVO,
        GENERIC_STOCK
    }

    val currentOem: OemType by lazy {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> OemType.XIAOMI
            manufacturer.contains("samsung") -> OemType.SAMSUNG
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> OemType.BBK_OPPO_REALME_ONEPLUS
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> OemType.VIVO
            else -> OemType.GENERIC_STOCK
        }
    }

    /**
     * Produces the best direct intent to allow background running for the current OEM.
     */
    fun getOemBatteryOptimizationIntent(context: Context): Intent {
        return when (currentOem) {
            OemType.XIAOMI -> {
                try {
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    }
                } catch (e: Exception) {
                    getStandardBatteryIntent(context)
                }
            }
            OemType.BBK_OPPO_REALME_ONEPLUS -> {
                try {
                    Intent().apply {
                        component = ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    }
                } catch (e: Exception) {
                    getStandardBatteryIntent(context)
                }
            }
            OemType.VIVO -> {
                try {
                    Intent().apply {
                        component = ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    }
                } catch (e: Exception) {
                    getStandardBatteryIntent(context)
                }
            }
            OemType.SAMSUNG,
            OemType.GENERIC_STOCK -> getStandardBatteryIntent(context)
        }
    }

    private fun getStandardBatteryIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
