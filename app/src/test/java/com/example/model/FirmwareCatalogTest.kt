package com.example.model

import com.example.bluetooth.PodFirmwareReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareCatalogTest {

    @Test
    fun comparesAppleAlphanumericBuildVersionsAccurately() {
        assertTrue(FirmwareCatalog.compareVersions("6F21", "7E93") < 0)
        assertTrue(FirmwareCatalog.compareVersions("7B21", "7E93") < 0)
        assertTrue(FirmwareCatalog.compareVersions("7E93", "7E101") < 0)
        assertEquals(0, FirmwareCatalog.compareVersions("7e93", "7E93"))
        assertTrue(FirmwareCatalog.compareVersions("7E101", "7E93") > 0)
    }

    @Test
    fun comparesDottedFirmwareVersionsAccurately() {
        assertTrue(FirmwareCatalog.compareVersions("6.8.8", "6.9.0") < 0)
        assertEquals(0, FirmwareCatalog.compareVersions("6.8.8", "6.8.8"))
        assertTrue(FirmwareCatalog.compareVersions("6.10.0", "6.8.8") > 0)
    }

    @Test
    fun evaluatesUpdateAvailableAndUpToDateStates() {
        val outdated = FirmwareCatalog.evaluate(
            model = PodModel.AIRPODS_PRO_2,
            currentVersion = "6F21",
            lastCheckedMillis = 1_000L
        )
        assertEquals(FirmwareCheckState.UPDATE_AVAILABLE, outdated.state)
        assertTrue(outdated.isUpdateAvailable)

        val current = FirmwareCatalog.evaluate(
            model = PodModel.AIRPODS_PRO_2,
            currentVersion = "7E93",
            lastCheckedMillis = 2_000L
        )
        assertEquals(FirmwareCheckState.UP_TO_DATE, current.state)
    }

    @Test
    fun extractsFirmwareVersionFromAapMetadataPayload() {
        val frame = byteArrayOf(
            0x04, 0x00, 0x04, 0x00, 0x1D, 0x00, 0x01, 0x00
        ) + "Apple Inc.\u0000A3048\u00007B21\u00001.0".toByteArray()

        val extracted = PodFirmwareReader.extractFirmwareFromAapPacket(frame)
        assertEquals("7B21", extracted)
    }
}
