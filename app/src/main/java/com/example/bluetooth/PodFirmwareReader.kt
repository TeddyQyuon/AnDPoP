package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

sealed class FirmwareReadResult {
    data class Success(
        val version: String,
        val source: String,
    ) : FirmwareReadResult()

    data class Failure(
        val reason: String,
    ) : FirmwareReadResult()
}

/**
 * Reads the real firmware revision from a connected AirPods / Beats device via:
 * 1. Standard Bluetooth LE GATT Device Information Service (0x180A -> Firmware Revision 0x2A26)
 * 2. Apple Accessory Protocol (AAP) RFCOMM metadata fallback for Classic-connected AirPods
 */
class PodFirmwareReader(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    @SuppressLint("MissingPermission")
    suspend fun readFirmwareVersion(bluetoothAddress: String?): FirmwareReadResult =
        withContext(Dispatchers.IO) {
            if (bluetoothAddress.isNullOrBlank()) {
                return@withContext FirmwareReadResult.Failure(
                    "Link a paired Bluetooth device in this profile to read its firmware version."
                )
            }

            val adapter = bluetoothManager?.adapter
                ?: return@withContext FirmwareReadResult.Failure("Bluetooth adapter unavailable.")

            if (!adapter.isEnabled) {
                return@withContext FirmwareReadResult.Failure("Turn on Bluetooth to check firmware.")
            }

            val device = try {
                adapter.getRemoteDevice(bluetoothAddress)
            } catch (_: IllegalArgumentException) {
                return@withContext FirmwareReadResult.Failure("Invalid Bluetooth address configured.")
            }

            // 1. Try standard Bluetooth SIG GATT Device Information Service (0x180A / 0x2A26)
            val gattVersion = readViaGattDis(device)
            if (!gattVersion.isNullOrBlank()) {
                return@withContext FirmwareReadResult.Success(
                    version = gattVersion,
                    source = "BLE GATT (0x2A26)"
                )
            }

            // 2. Fallback: Apple Accessory Protocol (AAP) metadata response over RFCOMM
            val aapVersion = readViaAapMetadata(device)
            if (!aapVersion.isNullOrBlank()) {
                return@withContext FirmwareReadResult.Success(
                    version = aapVersion,
                    source = "Apple Accessory Protocol"
                )
            }

            FirmwareReadResult.Failure(
                "Could not read firmware. Ensure the headphones are connected and awake nearby."
            )
        }

    @SuppressLint("MissingPermission")
    private suspend fun readViaGattDis(device: BluetoothDevice): String? {
        return withTimeoutOrNull(7_000L) {
            suspendCancellableCoroutine { cont ->
                var gattRef: BluetoothGatt? = null

                fun finish(value: String?) {
                    try {
                        gattRef?.disconnect()
                        gattRef?.close()
                    } catch (_: Exception) {
                    }
                    if (cont.isActive) {
                        cont.resume(value)
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
                        val dis = gatt.getService(DIS_SERVICE_UUID)
                        val fwChar = dis?.getCharacteristic(FIRMWARE_REVISION_UUID)
                        if (fwChar == null) {
                            finish(null)
                            return
                        }
                        try {
                            val started = gatt.readCharacteristic(fwChar)
                            if (!started) finish(null)
                        } catch (_: SecurityException) {
                            finish(null)
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS &&
                            characteristic.uuid == FIRMWARE_REVISION_UUID
                        ) {
                            val text = value.decodeToString().trim('\u0000', ' ', '\n', '\r')
                            finish(text.takeIf { it.isNotEmpty() })
                        } else {
                            finish(null)
                        }
                    }

                    @Suppress("DEPRECATION")
                    @Deprecated("Deprecated in API 33")
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            if (status == BluetoothGatt.GATT_SUCCESS &&
                                characteristic.uuid == FIRMWARE_REVISION_UUID
                            ) {
                                val text = characteristic.value
                                    ?.decodeToString()
                                    ?.trim('\u0000', ' ', '\n', '\r')
                                finish(text?.takeIf { it.isNotEmpty() })
                            } else {
                                finish(null)
                            }
                        }
                    }
                }

                try {
                    gattRef = device.connectGatt(
                        context,
                        false,
                        callback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                    if (gattRef == null) {
                        finish(null)
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "BLUETOOTH_CONNECT missing for GATT firmware read", e)
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
    private suspend fun readViaAapMetadata(device: BluetoothDevice): String? {
        return withTimeoutOrNull(4_500L) {
            try {
                val socket = device.createInsecureRfcommSocketToServiceRecord(AAP_UUID)
                socket.use { rfcomm ->
                    rfcomm.connect()
                    val out = rfcomm.outputStream
                    out.write(AAP_HANDSHAKE)
                    out.flush()
                    out.write(AAP_ENABLE_NOTIFICATIONS)
                    out.flush()

                    val input = rfcomm.inputStream
                    val buffer = ByteArray(1024)
                    val deadline = System.currentTimeMillis() + 3_000L
                    while (System.currentTimeMillis() < deadline) {
                        if (input.available() > 0) {
                            val read = input.read(buffer)
                            if (read > 0) {
                                val extracted = extractFirmwareFromAapPacket(buffer.copyOf(read))
                                if (extracted != null) return@withTimeoutOrNull extracted
                            }
                        } else {
                            kotlinx.coroutines.delay(120L)
                        }
                    }
                }
                null
            } catch (e: SecurityException) {
                Log.w(TAG, "BLUETOOTH_CONNECT missing for AAP firmware read", e)
                null
            } catch (e: IOException) {
                Log.w(TAG, "AAP RFCOMM firmware read failed", e)
                null
            }
        }
    }

    companion object {
        private const val TAG = "PodFirmwareReader"

        private val DIS_SERVICE_UUID: UUID =
            UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        private val FIRMWARE_REVISION_UUID: UUID =
            UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        private val AAP_UUID: UUID =
            UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        private val AAP_HANDSHAKE = byteArrayOf(
            0x00, 0x00, 0x04, 0x00,
            0x01, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )

        private val AAP_ENABLE_NOTIFICATIONS = byteArrayOf(
            0x04, 0x00, 0x04, 0x00,
            0x0F, 0x00, 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte()
        )

        // Matches Apple/Beats firmware build strings inside AAP 0x1D metadata frames (e.g., 7E93, 6F21, 7B24, 6.8.8)
        private val APPLE_FW_TOKEN_REGEX = Regex("\\b(\\d{1,2}[A-Z]\\d{1,4}[A-Z]?|\\d+\\.\\d+\\.\\d+)\\b")

        /**
         * Extracts an Apple firmware version token from null-delimited ASCII strings
         * inside an AAP metadata response frame.
         */
        fun extractFirmwareFromAapPacket(bytes: ByteArray): String? {
            if (bytes.size < 8) return null
            val ascii = buildString {
                for (b in bytes) {
                    val ch = b.toInt() and 0xFF
                    if (ch in 0x20..0x7E) {
                        append(ch.toChar())
                    } else {
                        append(' ')
                    }
                }
            }
            val candidates = APPLE_FW_TOKEN_REGEX.findAll(ascii).map { it.value }.toList()
            // Ignore model numbers like A2931 / A3048 (which start with a letter, already excluded by regex)
            return candidates.firstOrNull { token ->
                token.length in 3..8
            }
        }
    }
}
