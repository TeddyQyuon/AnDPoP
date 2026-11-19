package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.BleScanMode
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.capodSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "capod_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SCAN_MODE = stringPreferencesKey("scan_mode")
        val SHOW_UNMATCHED = booleanPreferencesKey("show_unmatched")
        val SHOW_POPUP = booleanPreferencesKey("show_popup")
        val BATTERY_NOTIFICATION = booleanPreferencesKey("battery_notification")
        val FIRMWARE_ALERTS = booleanPreferencesKey("firmware_alerts")
        val AUTOMATIC_EAR_DETECTION = booleanPreferencesKey("automatic_ear_detection")
    }

    val settings: Flow<AppSettings> = context.capodSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            scanMode = BleScanMode.fromKey(prefs[Keys.SCAN_MODE]),
            showUnmatchedDevices = prefs[Keys.SHOW_UNMATCHED] ?: true,
            showConnectionPopup = prefs[Keys.SHOW_POPUP] ?: true,
            batteryNotificationEnabled = prefs[Keys.BATTERY_NOTIFICATION] ?: false,
            firmwareAlertsEnabled = prefs[Keys.FIRMWARE_ALERTS] ?: true,
            automaticEarDetectionEnabled = prefs[Keys.AUTOMATIC_EAR_DETECTION] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.capodSettingsDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setScanMode(mode: BleScanMode) {
        context.capodSettingsDataStore.edit { it[Keys.SCAN_MODE] = mode.key }
    }

    suspend fun setShowUnmatchedDevices(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.SHOW_UNMATCHED] = enabled }
    }

    suspend fun setShowConnectionPopup(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.SHOW_POPUP] = enabled }
    }

    suspend fun setBatteryNotificationEnabled(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.BATTERY_NOTIFICATION] = enabled }
    }

    suspend fun setFirmwareAlertsEnabled(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.FIRMWARE_ALERTS] = enabled }
    }

    suspend fun setAutomaticEarDetectionEnabled(enabled: Boolean) {
        context.capodSettingsDataStore.edit { it[Keys.AUTOMATIC_EAR_DETECTION] = enabled }
    }
}
