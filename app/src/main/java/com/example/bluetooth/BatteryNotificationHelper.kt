package com.example.bluetooth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.BlePodSnapshot
import com.example.model.PodFormFactor

/**
 * Posts and updates a clean live battery status notification for matched AirPods / Beats devices.
 */
class BatteryNotificationHelper(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CAPod Battery Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live battery levels for matched AirPods and Beats"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postBatteryUpdate(deviceName: String, snapshot: BlePodSnapshot) {
        if (!hasNotificationPermission()) return
        ensureChannel()

        val summary = if (snapshot.model.formFactor == PodFormFactor.SINGLE_HEADSET) {
            val pct = snapshot.headsetBattery?.let { "$it%" } ?: "--%"
            val charge = if (snapshot.isHeadsetCharging) " (Charging)" else ""
            "Battery: $pct$charge"
        } else {
            val left = snapshot.leftBattery?.let { "L $it%" + if (snapshot.isLeftCharging) "⚡" else "" }
            val right = snapshot.rightBattery?.let { "R $it%" + if (snapshot.isRightCharging) "⚡" else "" }
            val case = snapshot.caseBattery?.let { "Case $it%" + if (snapshot.isCaseCharging) "⚡" else "" }
            listOfNotNull(left, right, case).joinToString("  •  ").ifEmpty { "Connected" }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_capod)
            .setContentTitle(deviceName)
            .setContentText(summary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun clearNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val CHANNEL_ID = "capod_battery_status"
        private const val NOTIFICATION_ID = 4201
    }
}
