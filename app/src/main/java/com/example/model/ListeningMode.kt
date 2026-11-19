package com.example.model

enum class ListeningMode(
    val id: String,
    val aapByte: Byte,
) {
    OFF("off", 0x01),
    NOISE_CANCELLATION("anc", 0x02),
    TRANSPARENCY("transparency", 0x03),
    ADAPTIVE("adaptive", 0x04);

    /**
     * Standard Apple Accessory Protocol (AAP) Listening Mode control frame:
     * 04 00 04 00 09 00 0D <mode> 00 00 00
     */
    fun toAapControlFrame(): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x0D,
        aapByte,
        0x00, 0x00, 0x00
    )

    /**
     * Cycles through the 3 core Active Noise Cancellation Quick Settings modes:
     * Noise Cancellation -> Transparency -> Off -> Noise Cancellation.
     */
    fun nextQuickToggleMode(): ListeningMode = when (this) {
        NOISE_CANCELLATION -> TRANSPARENCY
        TRANSPARENCY -> OFF
        OFF, ADAPTIVE -> NOISE_CANCELLATION
    }

    companion object {
        fun fromId(id: String): ListeningMode = entries.firstOrNull { it.id == id } ?: OFF
        fun fromAapByte(byte: Byte): ListeningMode? = entries.firstOrNull { it.aapByte == byte }
    }
}

enum class MicrophoneMode(
    val id: String,
    val aapByte: Byte,
) {
    AUTOMATIC("auto", 0x00),
    ALWAYS_LEFT("left", 0x02),
    ALWAYS_RIGHT("right", 0x01);

    fun toAapControlFrame(): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x01,
        aapByte,
        0x00, 0x00, 0x00
    )

    companion object {
        fun fromId(id: String): MicrophoneMode = entries.firstOrNull { it.id == id } ?: AUTOMATIC
    }
}

enum class ChimeTarget {
    NONE,
    LEFT,
    RIGHT,
    BOTH,
}

/**
 * Builders and decoders for Apple Accessory Protocol (AAP) binary frames over Bluetooth Classic RFCOMM.
 */
object AapProtocol {
    val HANDSHAKE_FRAME: ByteArray = byteArrayOf(
        0x00, 0x00, 0x04, 0x00,
        0x01, 0x00, 0x02, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    )

    val REQUEST_NOTIFICATIONS_FRAME: ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x0F, 0x00,
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
    )

    fun conversationalAwarenessFrame(enabled: Boolean): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x28,
        if (enabled) 0x01 else 0x02,
        0x00, 0x00, 0x00
    )

    fun personalizedVolumeFrame(enabled: Boolean): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x26,
        if (enabled) 0x01 else 0x02,
        0x00, 0x00, 0x00
    )

    fun allowAncWithOnePodFrame(enabled: Boolean): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x1B,
        if (enabled) 0x01 else 0x02,
        0x00, 0x00, 0x00
    )

    fun adaptiveNoiseLevelFrame(levelPercent: Int): ByteArray = byteArrayOf(
        0x04, 0x00, 0x04, 0x00,
        0x09, 0x00, 0x2E,
        levelPercent.coerceIn(0, 100).toByte(),
        0x00, 0x00, 0x00
    )

    sealed interface IncomingEvent {
        data class BatteryReport(
            val leftPercent: Int?,
            val rightPercent: Int?,
            val casePercent: Int?,
            val headsetPercent: Int?,
            val isLeftCharging: Boolean,
            val isRightCharging: Boolean,
            val isCaseCharging: Boolean,
            val isHeadsetCharging: Boolean,
        ) : IncomingEvent

        data class ListeningModeChanged(val mode: ListeningMode) : IncomingEvent
        data class EarDetectionChanged(val primaryInEar: Boolean, val secondaryInEar: Boolean) : IncomingEvent
        data class ConversationalAwarenessChanged(val enabled: Boolean) : IncomingEvent
    }

    /**
     * Decodes incoming AAP binary frames emitted by connected AirPods / Beats.
     */
    fun decodeIncomingFrame(frame: ByteArray): IncomingEvent? {
        if (frame.size < 8) return null
        // Verify AAP header 04 00 04 00
        if (frame[0] != 0x04.toByte() || frame[1] != 0x00.toByte() ||
            frame[2] != 0x04.toByte() || frame[3] != 0x00.toByte()
        ) {
            return null
        }

        val opcode = frame[4].toInt() and 0xFF
        return when (opcode) {
            // 0x04: Battery status report
            // Layout: 04 00 04 00 04 00 <count> [component, 01, level(0..100), status(01=charging,02=discharging,04=disconnected), 01] ...
            0x04 -> parseBatteryReport(frame)

            // 0x06: Ear detection state
            // Layout: 04 00 04 00 06 00 <primary(00=in-ear,01=out,02=in-case)> <secondary>
            0x06 -> {
                val primaryInEar = (frame[6].toInt() and 0xFF) == 0x00
                val secondaryInEar = (frame[7].toInt() and 0xFF) == 0x00
                IncomingEvent.EarDetectionChanged(primaryInEar, secondaryInEar)
            }

            // 0x09: Control / Setting notification
            0x09 -> {
                val settingId = frame[6].toInt() and 0xFF
                val value = frame[7]
                when (settingId) {
                    0x0D -> ListeningMode.fromAapByte(value)?.let {
                        IncomingEvent.ListeningModeChanged(it)
                    }
                    0x28 -> IncomingEvent.ConversationalAwarenessChanged(value == 0x01.toByte())
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun parseBatteryReport(frame: ByteArray): IncomingEvent.BatteryReport? {
        val count = frame[6].toInt() and 0xFF
        if (count <= 0 || frame.size < 7 + count * 5) return null

        var left: Int? = null
        var right: Int? = null
        var caseBat: Int? = null
        var single: Int? = null
        var leftCharging = false
        var rightCharging = false
        var caseCharging = false
        var singleCharging = false

        var offset = 7
        repeat(count) {
            val component = frame[offset].toInt() and 0xFF
            val levelRaw = frame[offset + 2].toInt() and 0xFF
            val statusRaw = frame[offset + 3].toInt() and 0xFF
            val validLevel = if (levelRaw in 0..100 && statusRaw != 0x04) levelRaw else null
            val isCharging = statusRaw == 0x01

            when (component) {
                0x01 -> { // Single headset (AirPods Max / Beats over-ear)
                    single = validLevel
                    singleCharging = isCharging
                }
                0x02 -> { // Right pod
                    right = validLevel
                    rightCharging = isCharging
                }
                0x04 -> { // Left pod
                    left = validLevel
                    leftCharging = isCharging
                }
                0x08 -> { // Charging case
                    caseBat = validLevel
                    caseCharging = isCharging
                }
            }
            offset += 5
        }

        return IncomingEvent.BatteryReport(
            leftPercent = left,
            rightPercent = right,
            casePercent = caseBat,
            headsetPercent = single,
            isLeftCharging = leftCharging,
            isRightCharging = rightCharging,
            isCaseCharging = caseCharging,
            isHeadsetCharging = singleCharging,
        )
    }
}
