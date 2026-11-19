package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.AapProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

sealed class EarDetectionGattResult {
    data class SyncedViaGatt(
        val deviceName: String,
        val enabled: Boolean,
        val serviceUuid: UUID,
        val characteristicUuid: UUID,
    ) : EarDetectionGattResult()

    data class SyncedViaAapFallback(
        val deviceName: String,
        val enabled: Boolean,
    ) : EarDetectionGattResult()

    data class SavedLocallyOnly(
        val enabled: Boolean,
        val reason: String,
    ) : EarDetectionGattResult()
}

sealed class EarDetectionSyncUiState {
    data object Idle : EarDetectionSyncUiState()
    data class Syncing(val targetDeviceName: String?) : EarDetectionSyncUiState()
    data class SyncedGatt(val deviceName: String, val enabled: Boolean) : EarDetectionSyncUiState()
    data class SyncedAap(val deviceName: String, val enabled: Boolean) : EarDetectionSyncUiState()
    data class SavedLocally(val reason: String, val enabled: Boolean) : EarDetectionSyncUiState()
}

/**
 * Communicates Automatic Ear Detection enabled/disabled state to a connected Bluetooth audio
 * device via Bluetooth LE GATT services (with Apple Accessory Protocol RFCOMM fallback when a
 * Classic-only AirPods/Beats peripheral does not expose a writable GATT control characteristic).
 */
class PodEarDetectionGattClient(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    @SuppressLint("MissingPermission")
    suspend fun writeEarDetectionState(
        bluetoothAddress: String?,
        deviceName: String,
        enabled: Boolean,
    ): EarDetectionGattResult = withContext(Dispatchers.IO) {
        if (bluetoothAddress.isNullOrBlank()) {
            return@withContext EarDetectionGattResult.SavedLocallyOnly(
                enabled = enabled,
                reason = "Link a paired Bluetooth device to sync over GATT"
            )
        }

        val adapter = bluetoothManager?.adapter
            ?: return@withContext EarDetectionGattResult.SavedLocallyOnly(
                enabled = enabled,
                reason = "Bluetooth adapter unavailable"
            )

        if (!adapter.isEnabled) {
            return@withContext EarDetectionGattResult.SavedLocallyOnly(
                enabled = enabled,
                reason = "Bluetooth is turned off"
            )
        }

        val device = try {
            adapter.getRemoteDevice(bluetoothAddress)
        } catch (_: IllegalArgumentException) {
            return@withContext EarDetectionGattResult.SavedLocallyOnly(
                enabled = enabled,
                reason = "Invalid Bluetooth address"
            )
        }

        val payload = buildEarDetectionGattPayload(enabled)

        // 1. Primary path: Connect over Bluetooth LE GATT and write to the Ear Detection control characteristic
        val gattResult = writeViaGatt(device, deviceName, enabled, payload)
        if (gattResult != null) {
            return@withContext gattResult
        }

        // 2. Fallback path: Apple Accessory Protocol (AAP) control frame over Classic RFCOMM
        if (writeViaAapFallback(device, payload)) {
            return@withContext EarDetectionGattResult.SyncedViaAapFallback(
                deviceName = deviceName,
                enabled = enabled,
            )
        }

        EarDetectionGattResult.SavedLocallyOnly(
            enabled = enabled,
            reason = "Headphones not reachable via GATT; connect pods to sync"
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeViaGatt(
        device: BluetoothDevice,
        deviceName: String,
        enabled: Boolean,
        payload: ByteArray,
    ): EarDetectionGattResult.SyncedViaGatt? {
        return withTimeoutOrNull(7_000L) {
            suspendCancellableCoroutine { cont ->
                var gattRef: BluetoothGatt? = null
                var targetServiceUuid: UUID? = null
                var targetCharUuid: UUID? = null

                fun finish(result: EarDetectionGattResult.SyncedViaGatt?) {
                    try {
                        gattRef?.disconnect()
                        gattRef?.close()
                    } catch (_: Exception) {
                    }
                    if (cont.isActive) {
                        cont.resume(result)
                    }
                }

                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt,
                        status: Int,
                        newState: Int,
                    ) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            finish(null)
                            return
                        }
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            try {
                                gatt.discoverServices()
                            } catch (_: SecurityException) {
                                finish(null)
                            }
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            finish(null)
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            finish(null)
                            return
                        }

                        val characteristic = findEarDetectionCharacteristic(gatt)
                        if (characteristic == null) {
                            finish(null)
                            return
                        }

                        targetServiceUuid = characteristic.service?.uuid ?: EAR_DETECTION_SERVICE_UUID
                        targetCharUuid = characteristic.uuid

                        val writeType = resolveWriteType(characteristic.properties)
                        try {
                            val initiated = writeCharacteristicCompat(
                                gatt = gatt,
                                characteristic = characteristic,
                                payload = payload,
                                writeType = writeType,
                            )
                            if (!initiated) {
                                finish(null)
                            }
                        } catch (_: SecurityException) {
                            finish(null)
                        }
                    }

                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            finish(
                                EarDetectionGattResult.SyncedViaGatt(
                                    deviceName = deviceName,
                                    enabled = enabled,
                                    serviceUuid = targetServiceUuid ?: EAR_DETECTION_SERVICE_UUID,
                                    characteristicUuid = targetCharUuid ?: characteristic.uuid,
                                )
                            )
                        } else {
                            finish(null)
                        }
                    }
                }

                try {
                    gattRef = device.connectGatt(
                        context,
                        false,
                        callback,
                        BluetoothDevice.TRANSPORT_LE,
                    )
                    if (gattRef == null) {
                        finish(null)
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "BLUETOOTH_CONNECT missing for GATT ear detection write", e)
                    finish(null)
                }

                cont.invokeOnCancellation {
                    try {
                        gattRef?.disconnect()
                        gattRef?.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
        writeType: Int,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, payload, writeType) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun findEarDetectionCharacteristic(
        gatt: BluetoothGatt,
    ): BluetoothGattCharacteristic? {
        // 1. Check primary Apple/CAPod Accessory Control GATT service + Ear Detection characteristic
        for (serviceUuid in PREFERRED_SERVICE_UUIDS) {
            val service = gatt.getService(serviceUuid) ?: continue
            for (charUuid in PREFERRED_CHARACTERISTIC_UUIDS) {
                val match = service.getCharacteristic(charUuid)
                if (match != null && isWritable(match.properties)) {
                    return match
                }
            }
            // Fallback to first writable characteristic inside a recognized vendor control service
            val writableInService = service.characteristics.firstOrNull { isWritable(it.properties) }
            if (writableInService != null) return writableInService
        }

        // 2. Scan discovered GATT services for any explicitly matching ear-detection characteristic UUID
        for (service in gatt.services) {
            val direct = service.characteristics.firstOrNull { char ->
                char.uuid in PREFERRED_CHARACTERISTIC_UUIDS && isWritable(char.properties)
            }
            if (direct != null) return direct
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeViaAapFallback(
        device: BluetoothDevice,
        payload: ByteArray,
    ): Boolean {
        return withTimeoutOrNull(4_000L) {
            try {
                val socket = device.createInsecureRfcommSocketToServiceRecord(AAP_RFCOMM_UUID)
                socket.use { rfcomm ->
                    rfcomm.connect()
                    val out = rfcomm.outputStream
                    out.write(AapProtocol.HANDSHAKE_FRAME)
                    out.flush()
                    out.write(payload)
                    out.flush()
                }
                true
            } catch (e: SecurityException) {
                Log.w(TAG, "BLUETOOTH_CONNECT missing for AAP ear detection write", e)
                false
            } catch (e: IOException) {
                Log.w(TAG, "AAP RFCOMM ear detection fallback unavailable", e)
                false
            }
        } ?: false
    }

    companion object {
        private const val TAG = "PodEarDetectionGatt"

        /** Primary Apple Accessory / CAPod Ear Detection Control GATT Service UUID */
        val EAR_DETECTION_SERVICE_UUID: UUID =
            UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        /** Ear Detection Setting Write Characteristic UUID */
        val EAR_DETECTION_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("74ec2173-0bad-4d01-8f77-997b2be0722a")

        /** Apple Continuity / Media Control GATT Service UUID */
        val APPLE_CONTINUITY_SERVICE_UUID: UUID =
            UUID.fromString("89d3502b-0f36-433a-8ef4-c502ad55f8dc")

        /** Apple Continuity Control Point Characteristic UUID */
        val APPLE_CONTROL_POINT_CHAR_UUID: UUID =
            UUID.fromString("9b3c81d8-57b1-4a8a-b8df-0e56f7ca51c2")

        private val AAP_RFCOMM_UUID: UUID = EAR_DETECTION_SERVICE_UUID

        private val PREFERRED_SERVICE_UUIDS = listOf(
            EAR_DETECTION_SERVICE_UUID,
            APPLE_CONTINUITY_SERVICE_UUID,
        )

        private val PREFERRED_CHARACTERISTIC_UUIDS = listOf(
            EAR_DETECTION_CHARACTERISTIC_UUID,
            APPLE_CONTROL_POINT_CHAR_UUID,
        )

        fun isWritable(properties: Int): Boolean {
            return (properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        }

        fun resolveWriteType(properties: Int): Int {
            return if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
        }

        /**
         * Encodes the Automatic Ear Detection enabled/disabled control packet:
         * Header: 04 00 04 00 | Opcode: 09 00 (Setting Control) | Setting: 0A (Auto Ear Detection)
         * Value: 01 = Enabled, 02 = Disabled
         */
        fun buildEarDetectionGattPayload(enabled: Boolean): ByteArray = byteArrayOf(
            0x04, 0x00, 0x04, 0x00,
            0x09, 0x00, 0x0A,
            if (enabled) 0x01 else 0x02,
            0x00, 0x00, 0x00,
        )

        /**
         * Decodes an Automatic Ear Detection GATT/AAP control payload back into its boolean state.
         */
        fun parseEarDetectionGattPayload(payload: ByteArray): Boolean? {
            if (payload.size < 8) return null
            if (payload[0] != 0x04.toByte() ||
                payload[1] != 0x00.toByte() ||
                payload[2] != 0x04.toByte() ||
                payload[3] != 0x00.toByte() ||
                payload[4] != 0x09.toByte() ||
                payload[6] != 0x0A.toByte()
            ) {
                return null
            }
            return when (payload[7]) {
                0x01.toByte() -> true
                0x02.toByte() -> false
                else -> null
            }
        }
    }
}
