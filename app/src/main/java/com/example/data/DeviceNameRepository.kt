package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.capodDeviceNamesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "capod_device_names"
)

/**
 * Snapshot of user-assigned custom device names persisted in Preferences DataStore.
 * Names for Bluetooth devices are keyed by normalized uppercase MAC address so the
 * custom name follows the connected device across all screens, notifications, and widgets.
 */
data class CustomDeviceNames(
    val byBluetoothAddress: Map<String, String> = emptyMap(),
    val byProfileId: Map<Long, String> = emptyMap(),
) {
    fun resolveForBluetoothAddress(address: String?): String? {
        val normalized = normalizeAddress(address) ?: return null
        return byBluetoothAddress[normalized]?.takeIf { it.isNotBlank() }
    }

    fun resolveForProfile(profileId: Long, bluetoothAddress: String?): String? {
        return resolveForBluetoothAddress(bluetoothAddress)
            ?: byProfileId[profileId]?.takeIf { it.isNotBlank() }
    }

    companion object {
        internal fun normalizeAddress(address: String?): String? {
            return address?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        }
    }
}

class DeviceNameRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.capodDeviceNamesDataStore)

    val customNames: Flow<CustomDeviceNames> = dataStore.data.map { prefs ->
        val byAddress = mutableMapOf<String, String>()
        val byProfileId = mutableMapOf<Long, String>()

        for ((key, rawValue) in prefs.asMap()) {
            val value = (rawValue as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val keyName = key.name
            when {
                keyName.startsWith(PREFIX_BT_ADDRESS) -> {
                    val addr = keyName.removePrefix(PREFIX_BT_ADDRESS)
                    if (addr.isNotEmpty()) {
                        byAddress[addr] = value
                    }
                }
                keyName.startsWith(PREFIX_PROFILE_ID) -> {
                    val id = keyName.removePrefix(PREFIX_PROFILE_ID).toLongOrNull()
                    if (id != null) {
                        byProfileId[id] = value
                    }
                }
            }
        }

        CustomDeviceNames(
            byBluetoothAddress = byAddress,
            byProfileId = byProfileId,
        )
    }

    suspend fun saveCustomName(
        bluetoothAddress: String?,
        profileId: Long? = null,
        customName: String,
    ) {
        val cleaned = customName.trim().take(MAX_NAME_LENGTH)
        if (cleaned.isEmpty()) {
            clearCustomName(bluetoothAddress = bluetoothAddress, profileId = profileId)
            return
        }

        val normalizedAddress = CustomDeviceNames.normalizeAddress(bluetoothAddress)
        dataStore.edit { prefs ->
            if (normalizedAddress != null) {
                prefs[addressKey(normalizedAddress)] = cleaned
            }
            if (profileId != null && profileId > 0L) {
                prefs[profileKey(profileId)] = cleaned
            }
        }
    }

    suspend fun clearCustomName(
        bluetoothAddress: String?,
        profileId: Long? = null,
    ) {
        val normalizedAddress = CustomDeviceNames.normalizeAddress(bluetoothAddress)
        dataStore.edit { prefs ->
            if (normalizedAddress != null) {
                prefs.remove(addressKey(normalizedAddress))
            }
            if (profileId != null && profileId > 0L) {
                prefs.remove(profileKey(profileId))
            }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        private const val PREFIX_BT_ADDRESS = "bt_addr_"
        private const val PREFIX_PROFILE_ID = "profile_id_"

        private fun addressKey(normalizedAddress: String): Preferences.Key<String> =
            stringPreferencesKey("$PREFIX_BT_ADDRESS$normalizedAddress")

        private fun profileKey(profileId: Long): Preferences.Key<String> =
            stringPreferencesKey("$PREFIX_PROFILE_ID$profileId")
    }
}
