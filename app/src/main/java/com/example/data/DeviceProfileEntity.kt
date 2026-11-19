package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ListeningMode
import com.example.model.MicrophoneMode
import com.example.model.PodModel

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val modelId: String,
    val linkedBluetoothAddress: String? = null,
    val linkedBluetoothName: String? = null,
    val minRssiDbm: Int = -85,
    val identityResolvingKeyHex: String? = null,
    val autoPauseEnabled: Boolean = true,
    val autoResumeEnabled: Boolean = false,
    val onePodModeEnabled: Boolean = false,
    val preferredListeningModeId: String = ListeningMode.ADAPTIVE.id,
    val conversationalAwarenessEnabled: Boolean = false,
    val personalizedVolumeEnabled: Boolean = false,
    val allowAncOnePodEnabled: Boolean = false,
    val adaptiveNoiseLevel: Int = 50,
    val microphoneModeId: String = MicrophoneMode.AUTOMATIC.id,
    val lastFirmwareVersion: String? = null,
    val lastFirmwareCheckMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val model: PodModel
        get() = PodModel.fromId(modelId)

    val preferredListeningMode: ListeningMode
        get() = ListeningMode.fromId(preferredListeningModeId)

    val microphoneMode: MicrophoneMode
        get() = MicrophoneMode.fromId(microphoneModeId)
}
