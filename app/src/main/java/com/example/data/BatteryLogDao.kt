package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {
    @Query(
        "SELECT * FROM battery_logs WHERE profileId = :profileId " +
            "ORDER BY timestampMillis DESC LIMIT :limit"
    )
    fun observeRecentForProfile(profileId: Long, limit: Int = 60): Flow<List<BatteryLogEntity>>

    @Query(
        "SELECT * FROM battery_logs WHERE profileId = :profileId " +
            "AND timestampMillis >= :sinceMillis " +
            "ORDER BY timestampMillis ASC"
    )
    fun observeSince(profileId: Long, sinceMillis: Long): Flow<List<BatteryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BatteryLogEntity)

    @Query("DELETE FROM battery_logs WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)

    @Query("DELETE FROM battery_logs")
    suspend fun clearAll()

    @Query("DELETE FROM battery_logs WHERE timestampMillis < :cutoffMillis")
    suspend fun pruneOlderThan(cutoffMillis: Long)
}
