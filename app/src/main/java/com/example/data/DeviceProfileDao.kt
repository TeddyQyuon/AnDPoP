package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profiles ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<DeviceProfileEntity>>

    @Query("SELECT * FROM device_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DeviceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DeviceProfileEntity): Long

    @Update
    suspend fun update(profile: DeviceProfileEntity)

    @Query("DELETE FROM device_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE device_profiles SET preferredListeningModeId = :modeId WHERE id = :id")
    suspend fun updateListeningMode(id: Long, modeId: String)

    @Query("UPDATE device_profiles SET conversationalAwarenessEnabled = :enabled WHERE id = :id")
    suspend fun updateConversationalAwareness(id: Long, enabled: Boolean)

    @Query("UPDATE device_profiles SET personalizedVolumeEnabled = :enabled WHERE id = :id")
    suspend fun updatePersonalizedVolume(id: Long, enabled: Boolean)

    @Query("UPDATE device_profiles SET allowAncOnePodEnabled = :enabled WHERE id = :id")
    suspend fun updateAllowAncOnePod(id: Long, enabled: Boolean)

    @Query("UPDATE device_profiles SET adaptiveNoiseLevel = :level WHERE id = :id")
    suspend fun updateAdaptiveNoiseLevel(id: Long, level: Int)

    @Query("UPDATE device_profiles SET microphoneModeId = :modeId WHERE id = :id")
    suspend fun updateMicrophoneMode(id: Long, modeId: String)

    @Query("UPDATE device_profiles SET autoPauseEnabled = :autoPause, autoResumeEnabled = :autoResume, onePodModeEnabled = :onePod WHERE id = :id")
    suspend fun updateEarDetection(id: Long, autoPause: Boolean, autoResume: Boolean, onePod: Boolean)

    @Query("UPDATE device_profiles SET lastFirmwareVersion = :version, lastFirmwareCheckMillis = :checkedAtMillis WHERE id = :id")
    suspend fun updateFirmwareInfo(id: Long, version: String, checkedAtMillis: Long)
}
