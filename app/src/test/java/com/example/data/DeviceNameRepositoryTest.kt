package com.example.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceNameRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun saveAndResolveCustomNameByBluetoothAddressCaseInsensitively() = runTest {
        val file = File(tempFolder.newFolder(), "device_names_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val repository = DeviceNameRepository(dataStore)

        repository.saveCustomName(
            bluetoothAddress = " aa:bb:cc:11:22:33 ",
            profileId = 7L,
            customName = "  Studio AirPods Pro  ",
        )

        val snapshot = repository.customNames.first()
        assertEquals(
            "Studio AirPods Pro",
            snapshot.resolveForBluetoothAddress("AA:BB:CC:11:22:33")
        )
        assertEquals(
            "Studio AirPods Pro",
            snapshot.resolveForBluetoothAddress("aa:bb:cc:11:22:33")
        )
        assertEquals(
            "Studio AirPods Pro",
            snapshot.resolveForProfile(profileId = 7L, bluetoothAddress = "AA:BB:CC:11:22:33")
        )
        assertEquals(
            "Studio AirPods Pro",
            snapshot.resolveForProfile(profileId = 7L, bluetoothAddress = null)
        )
    }

    @Test
    fun clampsLongNamesAndClearsOverrideOnBlankOrReset() = runTest {
        val file = File(tempFolder.newFolder(), "device_names_clamp_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val repository = DeviceNameRepository(dataStore)

        val overlyLong = "A".repeat(DeviceNameRepository.MAX_NAME_LENGTH + 15)
        repository.saveCustomName(
            bluetoothAddress = "DE:AD:BE:EF:00:01",
            profileId = 3L,
            customName = overlyLong,
        )

        val clamped = repository.customNames.first().resolveForBluetoothAddress("DE:AD:BE:EF:00:01")
        assertEquals(DeviceNameRepository.MAX_NAME_LENGTH, clamped?.length)

        // Saving a blank name clears the override
        repository.saveCustomName(
            bluetoothAddress = "DE:AD:BE:EF:00:01",
            profileId = 3L,
            customName = "   ",
        )
        val cleared = repository.customNames.first()
        assertNull(cleared.resolveForBluetoothAddress("DE:AD:BE:EF:00:01"))
        assertNull(cleared.resolveForProfile(profileId = 3L, bluetoothAddress = "DE:AD:BE:EF:00:01"))
    }

    @Test
    fun clearCustomNameRemovesStoredPreference() = runTest {
        val file = File(tempFolder.newFolder(), "device_names_clear_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        val repository = DeviceNameRepository(dataStore)

        repository.saveCustomName(
            bluetoothAddress = "11:22:33:44:55:66",
            profileId = 12L,
            customName = "Workout Pods",
        )
        assertEquals(
            "Workout Pods",
            repository.customNames.first().resolveForBluetoothAddress("11:22:33:44:55:66")
        )

        repository.clearCustomName(
            bluetoothAddress = "11:22:33:44:55:66",
            profileId = 12L,
        )
        val snapshot = repository.customNames.first()
        assertNull(snapshot.resolveForBluetoothAddress("11:22:33:44:55:66"))
        assertNull(snapshot.resolveForProfile(profileId = 12L, bluetoothAddress = null))
    }
}
