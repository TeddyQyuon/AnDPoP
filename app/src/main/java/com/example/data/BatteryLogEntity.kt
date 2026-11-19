package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_logs",
    foreignKeys = [
        ForeignKey(
            entity = DeviceProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId", "timestampMillis"])]
)
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val timestampMillis: Long,
    val leftBattery: Int?,
    val rightBattery: Int?,
    val caseBattery: Int?,
    val headsetBattery: Int?,
    val rssi: Int,
)
