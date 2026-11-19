package com.example.bluetooth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

enum class ChimeChannel {
    LEFT,
    RIGHT,
    BOTH,
}

/**
 * Generates a high-clarity stereo-panned locating chirp via Android AudioTrack so users can
 * audibly locate a misplaced Left earbud, Right earbud, or both over Bluetooth audio.
 */
class PodChimePlayer {

    suspend fun playLocatingChime(channel: ChimeChannel) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val pulseDurationMs = 140
        val gapDurationMs = 120
        val totalPulses = 5

        val pulseSamples = (sampleRate * pulseDurationMs) / 1000
        val gapSamples = (sampleRate * gapDurationMs) / 1000
        val cycleSamples = pulseSamples + gapSamples
        val totalFrames = cycleSamples * totalPulses
        // Stereo 16-bit PCM => 2 shorts per frame
        val pcmBuffer = ShortArray(totalFrames * 2)

        val leftGain = if (channel == ChimeChannel.RIGHT) 0f else 1f
        val rightGain = if (channel == ChimeChannel.LEFT) 0f else 1f

        val freqs = doubleArrayOf(2637.02, 3135.96, 2637.02, 3520.00, 3135.96) // E7 / G7 / A7 crisp sonar

        for (pulseIndex in 0 until totalPulses) {
            val baseFreq = freqs[pulseIndex % freqs.size]
            val frameOffset = pulseIndex * cycleSamples
            for (i in 0 until pulseSamples) {
                val t = i.toDouble() / sampleRate
                // Smooth attack/decay envelope to avoid pops/clicks
                val env = when {
                    i < 440 -> i / 440.0
                    i > pulseSamples - 880 -> (pulseSamples - i) / 880.0
                    else -> 1.0
                }.coerceIn(0.0, 1.0)

                val wave = (
                    0.75 * sin(2.0 * PI * baseFreq * t) +
                        0.25 * sin(2.0 * PI * (baseFreq * 1.5) * t)
                    ) * env * 22_000.0

                val sampleShort = wave.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                val bufIdx = (frameOffset + i) * 2
                pcmBuffer[bufIdx] = (sampleShort * leftGain).toInt().toShort()
                pcmBuffer[bufIdx + 1] = (sampleShort * rightGain).toInt().toShort()
            }
        }

        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            var offset = 0
            val chunkShorts = 4096
            while (offset < pcmBuffer.size && currentCoroutineContext().isActive) {
                val count = minOf(chunkShorts, pcmBuffer.size - offset)
                val written = audioTrack.write(pcmBuffer, offset, count)
                if (written <= 0) break
                offset += written
            }
            audioTrack.stop()
        } catch (e: CancellationException) {
            try {
                audioTrack.pause()
                audioTrack.flush()
            } catch (_: IllegalStateException) {
            }
            throw e
        } finally {
            audioTrack.release()
        }
    }
}
