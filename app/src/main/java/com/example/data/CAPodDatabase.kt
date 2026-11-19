package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DeviceProfileEntity::class, BatteryLogEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class CAPodDatabase : RoomDatabase() {
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun batteryLogDao(): BatteryLogDao

    companion object {
        @Volatile
        private var instance: CAPodDatabase? = null

        private fun existingColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
            val cols = mutableSetOf<String>()
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        cols.add(cursor.getString(nameIndex))
                    }
                }
            }
            return cols
        }

        private fun ensureAllSchemaUpgrades(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `battery_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `profileId` INTEGER NOT NULL,
                    `timestampMillis` INTEGER NOT NULL,
                    `leftBattery` INTEGER,
                    `rightBattery` INTEGER,
                    `caseBattery` INTEGER,
                    `headsetBattery` INTEGER,
                    `rssi` INTEGER NOT NULL,
                    FOREIGN KEY(`profileId`) REFERENCES `device_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_battery_logs_profileId_timestampMillis` " +
                    "ON `battery_logs` (`profileId`, `timestampMillis`)"
            )

            val existing = existingColumns(db, "device_profiles")
            val missingColumns = listOf(
                "conversationalAwarenessEnabled" to "INTEGER NOT NULL DEFAULT 0",
                "personalizedVolumeEnabled" to "INTEGER NOT NULL DEFAULT 0",
                "allowAncOnePodEnabled" to "INTEGER NOT NULL DEFAULT 0",
                "adaptiveNoiseLevel" to "INTEGER NOT NULL DEFAULT 50",
                "microphoneModeId" to "TEXT NOT NULL DEFAULT 'auto'",
                "lastFirmwareVersion" to "TEXT",
                "lastFirmwareCheckMillis" to "INTEGER",
            )
            for ((columnName, columnDef) in missingColumns) {
                if (columnName !in existing) {
                    db.execSQL("ALTER TABLE `device_profiles` ADD COLUMN `$columnName` $columnDef")
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = ensureAllSchemaUpgrades(db)
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = ensureAllSchemaUpgrades(db)
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = ensureAllSchemaUpgrades(db)
        }

        internal fun buildForName(context: Context, dbName: String): CAPodDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CAPodDatabase::class.java,
                dbName
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        fun getInstance(context: Context): CAPodDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildForName(context, "capod_profiles.db")
                    .also { instance = it }
            }
        }
    }
}
