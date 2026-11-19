package com.example.model

import android.bluetooth.BluetoothDevice

enum class BleDiscoveryFilter {
    ALL,
    NAMED_ONLY,
    APPLE_AND_AUDIO,
}

data class DiscoveredBleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val bondState: Int = BluetoothDevice.BOND_NONE,
    val isConnectable: Boolean = true,
    val manufacturerLabel: String? = null,
    val detectedPodModel: PodModel? = null,
    val lastSeenMillis: Long = System.currentTimeMillis(),
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: detectedPodModel?.displayName
            ?: "Unnamed BLE Device"

    val hasName: Boolean
        get() = !name.isNullOrBlank() || detectedPodModel != null

    val isBonded: Boolean
        get() = bondState == BluetoothDevice.BOND_BONDED

    val isBonding: Boolean
        get() = bondState == BluetoothDevice.BOND_BONDING

    val signalBars: Int
        get() = when {
            rssi >= -55 -> 4
            rssi >= -68 -> 3
            rssi >= -80 -> 2
            else -> 1
        }
}
