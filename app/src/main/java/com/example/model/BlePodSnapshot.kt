package com.example.model

data class BlePodSnapshot(
    val address: String,
    val model: PodModel,
    val modelCode: Int,
    val rssi: Int,
    val leftBattery: Int?,       // 0..100, null if unavailable/disconnected
    val rightBattery: Int?,      // 0..100, null if unavailable/disconnected
    val caseBattery: Int?,       // 0..100, null if unavailable/disconnected
    val headsetBattery: Int?,    // 0..100 for single-unit headsets (AirPods Max, Beats Over-Ear)
    val isLeftCharging: Boolean,
    val isRightCharging: Boolean,
    val isCaseCharging: Boolean,
    val isHeadsetCharging: Boolean,
    val isLeftInEar: Boolean,
    val isRightInEar: Boolean,
    val isLidOpen: Boolean,
    val isLeftPrimary: Boolean,
    val deviceColorName: String,
    val rawPayloadHex: String,
    val lastSeenMillis: Long,
    val isIrkVerified: Boolean = false,
) {
    /**
     * Normalizes RSSI (-100 dBm to -35 dBm) into 1..4 signal bars.
     */
    val signalBars: Int
        get() = when {
            rssi >= -55 -> 4
            rssi >= -68 -> 3
            rssi >= -80 -> 2
            else -> 1
        }

    fun isFresh(nowMillis: Long = System.currentTimeMillis(), maxAgeMs: Long = 35_000L): Boolean =
        (nowMillis - lastSeenMillis) <= maxAgeMs
}

data class BondedBluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val classicBatteryPercent: Int? = null,
    val customName: String? = null,
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: name
}

enum class BluetoothHardwareState {
    READY,
    PERMISSION_REQUIRED,
    BLUETOOTH_DISABLED,
    UNAVAILABLE,
}

fun formatDurationMinutes(minutes: Int): String {
    if (minutes <= 0) return "< 1m"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun estimateListeningTime(batteryPercent: Int?, model: PodModel): String? {
    if (batteryPercent == null || batteryPercent <= 0) return null
    val minutes = ((batteryPercent / 100f) * model.ratedListeningMinutes).toInt().coerceAtLeast(1)
    return formatDurationMinutes(minutes)
}

fun estimateChargeTimeRemaining(batteryPercent: Int?, model: PodModel): String? {
    if (batteryPercent == null || batteryPercent >= 100) return null
    val missingFraction = (100 - batteryPercent) / 100f
    val minutes = (missingFraction * model.fullChargeMinutes).toInt().coerceAtLeast(1)
    return formatDurationMinutes(minutes)
}
