package com.flowkeys.android.recovery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.flowkeys.android.accessibility.FlowKeysAccessibilityService

/**
 * Monitors the operational health of FlowKeys background bindings.
 * Posts self-healing recovery notifications if an aggressive OEM terminates accessibility.
 */
object ServiceHealthWatchdog {

    private const val RECOVERY_CHANNEL_ID = "flowkeys_recovery_channel"
    private const val RECOVERY_NOTIFICATION_ID = 2002

    fun isServiceHealthy(context: Context): Boolean {
        val expectedService = "${context.packageName}/${FlowKeysAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedService)
    }

    fun triggerRecoveryNotification(context: Context) {
        if (isServiceHealthy(context)) return

        createNotificationChannel(context)

        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RECOVERY_CHANNEL_ID)
            .setContentTitle("FlowKeys needs a quick reconnect")
            .setContentText("Tap here to restore floating dictation after your phone paused it.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RECOVERY_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RECOVERY_CHANNEL_ID,
                "FlowKeys Recovery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when FlowKeys accessibility is disconnected by system battery cleaners."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
