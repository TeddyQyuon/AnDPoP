/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import me.kavishdevar.librepods.QuickSettingsDialogActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.BluetoothConnectionManager
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.data.ListeningModeCycle
import kotlin.io.encoding.ExperimentalEncodingApi

@RequiresApi(Build.VERSION_CODES.Q)
class AirPodsQSService : TileService() {

    private lateinit var sharedPreferences: SharedPreferences
    private var currentAncMode: Int = ListeningModeCycle.OFF
    private var isAirPodsConnected: Boolean = false

    private val ancStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AirPodsNotifications.ANC_DATA) {
                currentAncMode = intent.getIntExtra("data", ListeningModeCycle.OFF)
                updateTile()
            }
        }
    }

    private val availabilityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AirPodsNotifications.AIRPODS_CONNECTED -> {
                    isAirPodsConnected = true
                    currentAncMode = ServiceManager.getService()?.getANC() ?: ListeningModeCycle.OFF
                    updateTile()
                }
                AirPodsNotifications.AIRPODS_DISCONNECTED -> {
                    isAirPodsConnected = false
                    updateTile()
                }
            }
        }
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "off_listening_mode") {
            if (currentAncMode == ListeningModeCycle.OFF && !isOffModeEnabled()) {
                currentAncMode = ListeningModeCycle.TRANSPARENCY
            }
            updateTile()
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
    }

    @SuppressLint("InlinedApi", "UnspecifiedRegisterReceiverFlag")
    override fun onStartListening() {
        super.onStartListening()
        val service = ServiceManager.getService()
        isAirPodsConnected = BluetoothConnectionManager.aacpSocket?.isConnected == true
        currentAncMode = service?.getANC() ?: ListeningModeCycle.OFF

        if (currentAncMode == ListeningModeCycle.OFF && !isOffModeEnabled()) {
            currentAncMode = ListeningModeCycle.TRANSPARENCY
        }

        val ancIntentFilter = IntentFilter(AirPodsNotifications.ANC_DATA)
        val availabilityIntentFilter = IntentFilter().apply {
            addAction(AirPodsNotifications.AIRPODS_CONNECTED)
            addAction(AirPodsNotifications.AIRPODS_DISCONNECTED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(ancStatusReceiver, ancIntentFilter, RECEIVER_NOT_EXPORTED)
                registerReceiver(availabilityReceiver, availabilityIntentFilter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(ancStatusReceiver, ancIntentFilter)
                registerReceiver(availabilityReceiver, availabilityIntentFilter)
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        } catch (e: Exception) {
            Log.e("AirPodsQSService", "Error registering receivers: $e")
        }

        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(ancStatusReceiver)
            unregisterReceiver(availabilityReceiver)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        } catch (e: Exception) {
            Log.e("AirPodsQSService", "Error unregistering receivers: $e")
        }
    }

    override fun onClick() {
        super.onClick()
        if (!isAirPodsConnected) return

        val clickBehavior = sharedPreferences.getString("qs_click_behavior", "cycle") ?: "cycle"
        if (clickBehavior == "dialog") {
            launchDialogActivity()
        } else {
            cycleAncMode()
        }
    }

    private fun launchDialogActivity() {
        try {
            val intent = Intent(this, QuickSettingsDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                @SuppressLint("StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e("AirPodsQSService", "Error launching QuickSettingsDialogActivity: $e")
        }
    }

    private fun cycleAncMode() {
        val service = ServiceManager.getService() ?: return
        val adaptiveSupported = isAdaptiveSupported(service)
        val nextMode = ListeningModeCycle.next(currentAncMode, isOffModeEnabled(), adaptiveSupported)
        service.aacpManager.sendControlCommand(
            AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
            nextMode,
        )
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val fallbackName = getString(R.string.andpop_airpods_fallback)
        val deviceName = sharedPreferences.getString("name", fallbackName)?.takeIf { it.isNotBlank() } ?: fallbackName

        if (isAirPodsConnected) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(ListeningModeCycle.label(currentAncMode))
            tile.subtitle = deviceName
            tile.icon = Icon.createWithResource(this, ListeningModeCycle.icon(currentAncMode))
        } else {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.andpop_qs_tile_label)
            tile.subtitle = getString(R.string.andpop_not_connected)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_andpop)
        }

        try {
            tile.updateTile()
        } catch (e: Exception) {
            Log.e("AirPodsQSService", "Error updating tile: $e")
        }
    }

    private fun isOffModeEnabled(): Boolean = sharedPreferences.getBoolean("off_listening_mode", true)

    private fun isAdaptiveSupported(service: AirPodsService?): Boolean {
        val caps = service?.airpodsInstance?.model?.capabilities ?: return true
        return Capability.ADAPTIVE_AUDIO in caps
    }
}
