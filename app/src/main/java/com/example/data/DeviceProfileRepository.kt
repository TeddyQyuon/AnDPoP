package com.example.data

import kotlinx.coroutines.flow.Flow

class DeviceProfileRepository(
    private val dao: DeviceProfileDao,
) {
    val profiles: Flow<List<DeviceProfileEntity>> = dao.observeAll()

    suspend fun getById(id: Long): DeviceProfileEntity? = dao.getById(id)

    suspend fun save(profile: DeviceProfileEntity): Long {
        return if (profile.id == 0L) {
            dao.insert(profile)
        } else {
            dao.update(profile)
            profile.id
        }
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun updateListeningMode(id: Long, modeId: String) =
        dao.updateListeningMode(id, modeId)

    suspend fun updateConversationalAwareness(id: Long, enabled: Boolean) =
        dao.updateConversationalAwareness(id, enabled)

    suspend fun updatePersonalizedVolume(id: Long, enabled: Boolean) =
        dao.updatePersonalizedVolume(id, enabled)

    suspend fun updateAllowAncOnePod(id: Long, enabled: Boolean) =
        dao.updateAllowAncOnePod(id, enabled)

    suspend fun updateAdaptiveNoiseLevel(id: Long, level: Int) =
        dao.updateAdaptiveNoiseLevel(id, level)

    suspend fun updateMicrophoneMode(id: Long, modeId: String) =
        dao.updateMicrophoneMode(id, modeId)

    suspend fun updateEarDetection(
        id: Long,
        autoPause: Boolean,
        autoResume: Boolean,
        onePod: Boolean,
    ) = dao.updateEarDetection(id, autoPause, autoResume, onePod)

    suspend fun updateFirmwareInfo(
        id: Long,
        version: String,
        checkedAtMillis: Long,
    ) = dao.updateFirmwareInfo(id, version, checkedAtMillis)
}
