package com.example.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PodEarDetectionGattClientTest {

    @Test
    fun buildsAndParsesAutomaticEarDetectionGattPayloadAccurately() {
        val enabledPayload = PodEarDetectionGattClient.buildEarDetectionGattPayload(enabled = true)
        assertArrayEquals(
            byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x0A, 0x01, 0x00, 0x00, 0x00),
            enabledPayload,
        )
        assertEquals(true, PodEarDetectionGattClient.parseEarDetectionGattPayload(enabledPayload))

        val disabledPayload = PodEarDetectionGattClient.buildEarDetectionGattPayload(enabled = false)
        assertArrayEquals(
            byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x0A, 0x02, 0x00, 0x00, 0x00),
            disabledPayload,
        )
        assertEquals(false, PodEarDetectionGattClient.parseEarDetectionGattPayload(disabledPayload))

        assertNull(PodEarDetectionGattClient.parseEarDetectionGattPayload(byteArrayOf(0x01, 0x02)))
    }

    @Test
    fun detectsWritableCharacteristicPropertiesAndWriteTypes() {
        assertTrue(
            PodEarDetectionGattClient.isWritable(BluetoothGattCharacteristic.PROPERTY_WRITE)
        )
        assertTrue(
            PodEarDetectionGattClient.isWritable(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        )
        assertFalse(
            PodEarDetectionGattClient.isWritable(BluetoothGattCharacteristic.PROPERTY_READ)
        )

        assertEquals(
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            PodEarDetectionGattClient.resolveWriteType(
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            )
        )
        assertEquals(
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            PodEarDetectionGattClient.resolveWriteType(
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            )
        )
    }

    @Test
    fun returnsSavedLocallyOnlyWhenNoBluetoothAddressIsLinked() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = PodEarDetectionGattClient(context)

        val result = client.writeEarDetectionState(
            bluetoothAddress = null,
            deviceName = "My AirPods Pro",
            enabled = false,
        )

        assertTrue(result is EarDetectionGattResult.SavedLocallyOnly)
        val local = result as EarDetectionGattResult.SavedLocallyOnly
        assertFalse(local.enabled)
        assertTrue(local.reason.contains("GATT", ignoreCase = true))
    }
}
