package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.example.model.ListeningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Handles real Bluetooth Classic Apple Accessory Protocol (AAP) control commands
 * (Active Noise Cancellation, Transparency, Adaptive, Off) and Android AudioManager
 * media key dispatch for automatic ear-detection pause/resume.
 */
class PodControlClient(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @SuppressLint("MissingPermission")
    suspend fun sendListeningMode(
        bluetoothAddress: String?,
        mode: ListeningMode,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAddress.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Link a paired Bluetooth device in this profile to send ANC commands.")
            )
        }

        val adapter = bluetoothManager?.adapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is turned off."))
        }

        try {
            val remoteDevice = adapter.getRemoteDevice(bluetoothAddress)
            val socket = remoteDevice.createInsecureRfcommSocketToServiceRecord(AAP_UUID)
            socket.use { rfcomm ->
                rfcomm.connect()
                val out = rfcomm.outputStream
                out.write(AAP_HANDSHAKE)
                out.flush()
                out.write(mode.toAapControlFrame())
                out.flush()
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth connect permission required for AAP command", e)
            Result.failure(IllegalStateException("Bluetooth Connect permission is required."))
        } catch (e: IOException) {
            Log.w(TAG, "AAP RFCOMM channel unavailable for $bluetoothAddress", e)
            Result.failure(
                IllegalStateException("Device not reachable over Bluetooth audio. Ensure pods are connected.")
            )
        } catch (e: IllegalArgumentException) {
            Result.failure(IllegalStateException("Invalid Bluetooth address configured."))
        }
    }

    fun dispatchMediaPause() {
        val manager = audioManager ?: return
        if (!manager.isMusicActive) return
        dispatchMediaKey(manager, KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    fun dispatchMediaPlay() {
        val manager = audioManager ?: return
        if (manager.isMusicActive) return
        dispatchMediaKey(manager, KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    private fun dispatchMediaKey(manager: AudioManager, keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        manager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        manager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    companion object {
        private const val TAG = "PodControlClient"

        // Standard Apple Accessory Protocol (AAP) Service UUID exposed by AirPods / Beats
        private val AAP_UUID: UUID = UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        // Standard AAP Handshake packet
        private val AAP_HANDSHAKE: ByteArray = byteArrayOf(
            0x00, 0x00, 0x04, 0x00,
            0x01, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
    }
}
