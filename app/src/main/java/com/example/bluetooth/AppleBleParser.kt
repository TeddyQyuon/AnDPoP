package com.example.bluetooth

import com.example.model.BlePodSnapshot
import com.example.model.PodFormFactor
import com.example.model.PodModel
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Parses Apple Continuity Proximity Pairing (message type 0x07) BLE advertisements
 * broadcast under Apple Inc. Manufacturer ID 0x004C.
 */
object AppleBleParser {
    const val APPLE_MANUFACTURER_ID = 0x004C
    private const val PROXIMITY_PAIRING_TYPE: Byte = 0x07
    private const val MIN_PROXIMITY_PAYLOAD_LEN = 11

    /**
     * Locates and parses an Apple Proximity Pairing TLV inside Manufacturer Specific Data (0x004C).
     * Returns null if the advertisement is not an AirPods / Beats Proximity Pairing packet.
     */
    fun parse(
        address: String,
        rssi: Int,
        manufacturerData: ByteArray,
        timestampMillis: Long = System.currentTimeMillis(),
    ): BlePodSnapshot? {
        val proximityPayload = extractProximityPayload(manufacturerData) ?: return null
        if (proximityPayload.size < MIN_PROXIMITY_PAYLOAD_LEN) return null

        // Bytes 3..4 (index 1..2 inside TLV value after type & length) are 16-bit big-endian Model ID
        val modelCode = ((proximityPayload[3].toInt() and 0xFF) shl 8) or
            (proximityPayload[4].toInt() and 0xFF)
        val model = PodModel.fromModelCode(modelCode)

        val statusByte = proximityPayload[5].toInt() and 0xFF
        val podsBatteryByte = proximityPayload[6].toInt() and 0xFF
        val caseAndChargeByte = proximityPayload[7].toInt() and 0xFF
        val lidByte = proximityPayload[8].toInt() and 0xFF
        val colorByte = proximityPayload[9].toInt() and 0xFF

        // Bit 5 (0x20) indicates whether left/right nibbles are flipped in this broadcast
        val isFlipped = (statusByte and 0x20) == 0

        val highPodRaw = (podsBatteryByte shr 4) and 0x0F
        val lowPodRaw = podsBatteryByte and 0x0F
        val caseRaw = caseAndChargeByte and 0x0F

        val highPodPercent = decodeBatteryNibble(highPodRaw)
        val lowPodPercent = decodeBatteryNibble(lowPodRaw)
        val casePercent = decodeBatteryNibble(caseRaw)

        val highCharging = (caseAndChargeByte and 0x10) != 0
        val lowCharging = (caseAndChargeByte and 0x20) != 0
        val caseCharging = (caseAndChargeByte and 0x40) != 0

        // Status byte ear-detection flags: 0x02 primary pod in ear, 0x08 secondary pod in ear
        val highInEar = (statusByte and 0x02) != 0
        val lowInEar = (statusByte and 0x08) != 0

        // Lid open state: bit 3 (0x08) in lid byte is 0 when open on Continuity packets,
        // or case is active while lid counter changes (bit 3 clear indicates lid open).
        val isLidOpen = (lidByte and 0x08) == 0

        val leftBattery: Int?
        val rightBattery: Int?
        val isLeftCharging: Boolean
        val isRightCharging: Boolean
        val isLeftInEar: Boolean
        val isRightInEar: Boolean

        if (isFlipped) {
            leftBattery = lowPodPercent
            rightBattery = highPodPercent
            isLeftCharging = lowCharging
            isRightCharging = highCharging
            isLeftInEar = lowInEar
            isRightInEar = highInEar
        } else {
            leftBattery = highPodPercent
            rightBattery = lowPodPercent
            isLeftCharging = highCharging
            isRightCharging = lowCharging
            isLeftInEar = highInEar
            isRightInEar = lowInEar
        }

        val isSingleHeadset = model.formFactor == PodFormFactor.SINGLE_HEADSET
        val headsetBattery = if (isSingleHeadset) {
            highPodPercent ?: lowPodPercent
        } else {
            null
        }
        val isHeadsetCharging = if (isSingleHeadset) {
            highCharging || lowCharging
        } else {
            false
        }

        return BlePodSnapshot(
            address = address,
            model = model,
            modelCode = modelCode,
            rssi = rssi,
            leftBattery = if (isSingleHeadset) null else leftBattery,
            rightBattery = if (isSingleHeadset) null else rightBattery,
            caseBattery = if (isSingleHeadset) null else casePercent,
            headsetBattery = headsetBattery,
            isLeftCharging = if (isSingleHeadset) false else isLeftCharging,
            isRightCharging = if (isSingleHeadset) false else isRightCharging,
            isCaseCharging = if (isSingleHeadset) false else caseCharging,
            isHeadsetCharging = isHeadsetCharging,
            isLeftInEar = if (isSingleHeadset) (highInEar || lowInEar) else isLeftInEar,
            isRightInEar = if (isSingleHeadset) false else isRightInEar,
            isLidOpen = if (isSingleHeadset) false else isLidOpen,
            isLeftPrimary = !isFlipped,
            deviceColorName = decodeColorName(colorByte),
            rawPayloadHex = proximityPayload.toHexString(),
            lastSeenMillis = timestampMillis,
        )
    }

    /**
     * Walks Apple Continuity TLV frames inside manufacturer data to find type 0x07 (Proximity Pairing).
     */
    private fun extractProximityPayload(data: ByteArray): ByteArray? {
        var offset = 0
        while (offset + 2 <= data.size) {
            val type = data[offset]
            val length = data[offset + 1].toInt() and 0xFF
            if (length <= 0 || offset + 2 + length > data.size) {
                // Fallback: some stacks deliver raw 0x07 packet directly without strict trailing length
                if (offset == 0 && type == PROXIMITY_PAIRING_TYPE && data.size >= MIN_PROXIMITY_PAYLOAD_LEN) {
                    return data
                }
                return null
            }
            if (type == PROXIMITY_PAIRING_TYPE && length >= MIN_PROXIMITY_PAYLOAD_LEN - 2) {
                return data.copyOfRange(offset, offset + 2 + length)
            }
            offset += 2 + length
        }
        return null
    }

    /**
     * Decodes a 4-bit Apple battery nibble: 0..10 => 0%..100%, 0x0F (15) => unavailable.
     */
    fun decodeBatteryNibble(nibble: Int): Int? {
        return if (nibble in 0..10) {
            (nibble * 10).coerceIn(0, 100)
        } else {
            null
        }
    }

    /**
     * Verifies a Bluetooth Low Energy Resolvable Private Address (RPA) against a 16-byte
     * Identity Resolving Key (IRK) per Bluetooth Core Spec Vol 6, Part B, Section 1.3.2.3 (ah function).
     */
    fun verifyRpaWithIrk(macAddress: String, irkHex: String?): Boolean {
        if (irkHex.isNullOrBlank()) return false
        val cleanedIrk = irkHex.replace(":", "").replace("-", "").replace(" ", "").trim()
        if (cleanedIrk.length != 32) return false
        val irkBytes = hexToBytes(cleanedIrk) ?: return false

        val macParts = macAddress.split(":")
        if (macParts.size != 6) return false
        val macBytes = ByteArray(6) { idx ->
            macParts[idx].toIntOrNull(16)?.toByte() ?: return false
        }

        // RPA must have the two most significant bits of the top byte equal to 01 (0x40..0x7F)
        val topByte = macBytes[0].toInt() and 0xFF
        if ((topByte and 0xC0) != 0x40) return false

        // prand is the upper 3 bytes (macBytes[0..2]), hash is the lower 3 bytes (macBytes[3..5])
        // AES-128-ECB(key = IRK, plaintext = 13 zero bytes || prand[0..2])
        // Local hash is the last 3 bytes of the ciphertext.
        return try {
            val plaintext = ByteArray(16)
            plaintext[13] = macBytes[0]
            plaintext[14] = macBytes[1]
            plaintext[15] = macBytes[2]

            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(irkBytes, "AES"))
            val encrypted = cipher.doFinal(plaintext)

            encrypted[13] == macBytes[3] &&
                encrypted[14] == macBytes[4] &&
                encrypted[15] == macBytes[5]
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeColorName(code: Int): String = when (code) {
        0x00 -> "White"
        0x01 -> "Black"
        0x02 -> "Red"
        0x03 -> "Blue"
        0x04 -> "Pink"
        0x05 -> "Gray"
        0x06 -> "Silver"
        0x07 -> "Gold"
        0x08 -> "Rose Gold"
        0x09 -> "Space Gray"
        0x0A -> "Dark Blue"
        0x0B -> "Light Blue"
        0x0C -> "Yellow"
        0x0E -> "Sky Blue"
        0x0F -> "Green"
        0x10 -> "Midnight"
        0x11 -> "Starlight"
        else -> "Standard"
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return ByteArray(hex.length / 2) { i ->
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            if (hi == -1 || lo == -1) return null
            ((hi shl 4) or lo).toByte()
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
