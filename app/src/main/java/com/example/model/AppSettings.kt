package com.example.model

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class BleScanMode(val key: String) {
    LOW_LATENCY("low_latency"),
    BALANCED("balanced"),
    LOW_POWER("low_power");

    companion object {
        fun fromKey(key: String?): BleScanMode = entries.firstOrNull { it.key == key } ?: BALANCED
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val scanMode: BleScanMode = BleScanMode.BALANCED,
    val showUnmatchedDevices: Boolean = true,
    val showConnectionPopup: Boolean = true,
    val batteryNotificationEnabled: Boolean = false,
    val firmwareAlertsEnabled: Boolean = true,
    val automaticEarDetectionEnabled: Boolean = true,
)
