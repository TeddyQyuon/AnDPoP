package com.example.bluetooth

import com.example.model.PodModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleBleParserTest {

    @Test
    fun parsesAirPodsPro2ProximityPairingPacket() {
        // Type 0x07, Len 0x19 (25 bytes), Prefix 0x01, Model 0x1420 (AirPods Pro 2)
        // Status 0x22 (bit5=1 => not flipped, bit1=1 => left in ear)
        // PodsBattery 0x85 (left=80%, right=50%)
        // Case&Charge 0x56 (case=60%, left charging=0x10, case charging=0x40)
        // Lid 0x00 (bit3=0 => lid open), Color 0x00 (White)
        val payload = byteArrayOf(
            0x07, 0x19, 0x01,
            0x14, 0x20,
            0x22,
            0x85.toByte(),
            0x56,
            0x00,
            0x00,
            0x00,
            0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte(),
            0x99.toByte(), 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(), 0x01
        )

        val snapshot = AppleBleParser.parse(
            address = "41:22:33:44:55:66",
            rssi = -58,
            manufacturerData = payload,
            timestampMillis = 1_000L
        )

        assertNotNull(snapshot)
        assertEquals(PodModel.AIRPODS_PRO_2, snapshot!!.model)
        assertEquals(80, snapshot.leftBattery)
        assertEquals(50, snapshot.rightBattery)
        assertEquals(60, snapshot.caseBattery)
        assertTrue(snapshot.isLeftCharging)
        assertFalse(snapshot.isRightCharging)
        assertTrue(snapshot.isCaseCharging)
        assertTrue(snapshot.isLeftInEar)
        assertFalse(snapshot.isRightInEar)
        assertTrue(snapshot.isLidOpen)
        assertEquals(3, snapshot.signalBars)
    }

    @Test
    fun parsesFlippedPodsAndUnavailableCase() {
        // Status 0x08 (bit5=0 => flipped, bit3=1 => low pod=left in ear)
        // PodsBattery 0xA4 (high=right=100%, low=left=40%)
        // Case&Charge 0x2F (case=0xF unavailable, low charging=0x20 => left charging)
        val payload = byteArrayOf(
            0x07, 0x19, 0x01,
            0x24, 0x20, // AirPods Pro 2 USB-C
            0x08,
            0xA4.toByte(),
            0x2F,
            0x08, // lid closed
            0x00,
            0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )

        val snapshot = AppleBleParser.parse("40:11:22:33:44:55", -50, payload)
        assertNotNull(snapshot)
        assertEquals(PodModel.AIRPODS_PRO_2_USBC, snapshot!!.model)
        assertEquals(40, snapshot.leftBattery)
        assertEquals(100, snapshot.rightBattery)
        assertNull(snapshot.caseBattery)
        assertTrue(snapshot.isLeftCharging)
        assertFalse(snapshot.isRightCharging)
        assertTrue(snapshot.isLeftInEar)
        assertFalse(snapshot.isLidOpen)
    }

    @Test
    fun parsesAirPodsMaxSingleHeadset() {
        val payload = byteArrayOf(
            0x07, 0x19, 0x01,
            0x0A, 0x20, // AirPods Max
            0x20,
            0x9F.toByte(), // 90% headset
            0x0F,
            0x00,
            0x09, // Space Gray
            0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )

        val snapshot = AppleBleParser.parse("51:AA:BB:CC:DD:EE", -62, payload)
        assertNotNull(snapshot)
        assertEquals(PodModel.AIRPODS_MAX, snapshot!!.model)
        assertEquals(90, snapshot.headsetBattery)
        assertNull(snapshot.leftBattery)
        assertNull(snapshot.rightBattery)
        assertEquals("Space Gray", snapshot.deviceColorName)
    }

    @Test
    fun ignoresNonProximityApplePackets() {
        val nearbyInfoPacket = byteArrayOf(0x10, 0x05, 0x01, 0x18, 0x00, 0x00, 0x00)
        assertNull(AppleBleParser.parse("40:00:00:00:00:01", -70, nearbyInfoPacket))
    }
}
