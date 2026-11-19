package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.example.model.AapProtocol
import com.example.model.ListeningMode
import com.example.model.MicrophoneMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles real Bluetooth Classic Apple Accessory Protocol (AAP) control commands
 * (Active Noise Cancellation, Transparency, Adaptive, Off, Microphone Mode,
 * Conversational Awareness, Personalized Volume, Adaptive Noise Level, and One-Pod ANC)
 * as well as Android AudioManager media key dispatch for automatic ear-detection pause/resume.
 */
class PodControlClient(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    suspend fun sendListeningMode(
        bluetoothAddress: String?,
        mode: ListeningMode,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = mode.toAapControlFrame(),
        unlinkedMessage = "Link a paired Bluetooth device in this profile to send ANC commands.",
    )

    suspend fun sendMicrophoneMode(
        bluetoothAddress: String?,
        mode: MicrophoneMode,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = mode.toAapControlFrame(),
        unlinkedMessage = "Saved preference; link a paired Bluetooth device to apply microphone mode.",
    )

    suspend fun sendConversationalAwareness(
        bluetoothAddress: String?,
        enabled: Boolean,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = AapProtocol.conversationalAwarenessFrame(enabled),
        unlinkedMessage = "Saved preference; link a paired Bluetooth device to apply Conversation Awareness.",
    )

    suspend fun sendPersonalizedVolume(
        bluetoothAddress: String?,
        enabled: Boolean,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = AapProtocol.personalizedVolumeFrame(enabled),
        unlinkedMessage = "Saved preference; link a paired Bluetooth device to apply Personalized Volume.",
    )

    suspend fun sendAllowAncWithOnePod(
        bluetoothAddress: String?,
        enabled: Boolean,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = AapProtocol.allowAncWithOnePodFrame(enabled),
        unlinkedMessage = "Saved preference; link a paired Bluetooth device to apply One-Pod ANC.",
    )

    suspend fun sendAdaptiveNoiseLevel(
        bluetoothAddress: String?,
        levelPercent: Int,
    ): Result<Unit> = sendAapControlFrame(
        bluetoothAddress = bluetoothAddress,
        frame = AapProtocol.adaptiveNoiseLevelFrame(levelPercent),
        unlinkedMessage = "Saved preference; link a paired Bluetooth device to apply Adaptive Audio level.",
    )

    @SuppressLint("MissingPermission")
    suspend fun sendAapControlFrame(
        bluetoothAddress: String?,
        frame: ByteArray,
        unlinkedMessage: String = "Link a paired Bluetooth device in this profile to send control commands.",
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAddress.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException(unlinkedMessage))
        }

        val adapter = bluetoothManager?.adapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter unavailable."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is turned off."))
        }

        val remoteDevice = try {
            adapter.getRemoteDevice(bluetoothAddress)
        } catch (_: IllegalArgumentException) {
            return@withContext Result.failure(IllegalStateException("Invalid Bluetooth address configured."))
        }

        val socket = try {
            remoteDevice.createInsecureRfcommSocketToServiceRecord(AAP_UUID)
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth connect permission required for AAP command", e)
            return@withContext Result.failure(IllegalStateException("Bluetooth Connect permission is required."))
        } catch (e: IOException) {
            Log.w(TAG, "Unable to create RFCOMM socket for $bluetoothAddress", e)
            return@withContext Result.failure(
                IllegalStateException("Device not reachable over Bluetooth audio. Ensure pods are connected.")
            )
        }

        val outcome = withTimeoutOrNull(RFCOMM_TIMEOUT_MS.milliseconds) {
            try {
                socket.use { rfcomm ->
                    rfcomm.connect()
                    val out = rfcomm.outputStream
                    out.write(AapProtocol.HANDSHAKE_FRAME)
                    out.flush()
                    out.write(frame)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: SecurityException) {
                Log.w(TAG, "Bluetooth connect permission required for AAP command", e)
                Result.failure(IllegalStateException("Bluetooth Connect permission is required."))
            } catch (e: IOException) {
                Log.w(TAG, "AAP RFCOMM channel unavailable for $bluetoothAddress", e)
                Result.failure(
                    IllegalStateException("Saved preference; ensure pods are connected to apply.")
                )
            }
        }

        if (outcome == null) {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            Result.failure(IllegalStateException("Saved preference; Bluetooth audio connection timed out."))
        } else {
            outcome
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
        private const val RFCOMM_TIMEOUT_MS = 4_500L

        // Standard Apple Accessory Protocol (AAP) Service UUID exposed by AirPods / Beats
        private val AAP_UUID: UUID = UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")
    }
}
