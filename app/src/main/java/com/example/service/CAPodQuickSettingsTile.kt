package com.example.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.widget.CAPodBatteryWidgetProvider

/**
 * Android Quick Settings Tile showing the latest matched AirPods / Beats battery summary
 * right inside the system Quick Settings shade and launching CAPod on tap.
 */
class CAPodQuickSettingsTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileVisuals()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            // The PendingIntent overload is available from API 34.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTileVisuals() {
        val tile = qsTile ?: return
        val cached = CAPodBatteryWidgetProvider.readCachedState(this)
        tile.label = cached.deviceName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = cached.summaryLine
        }
        tile.state = if (cached.leftValue != "--%" || cached.rightValue != "--%") {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    companion object {
        fun requestRefresh(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, CAPodQuickSettingsTile::class.java)
                )
            } catch (_: Exception) {
                // Safe no-op if Quick Settings tile is not currently added by the user
            }
        }
    }
}
