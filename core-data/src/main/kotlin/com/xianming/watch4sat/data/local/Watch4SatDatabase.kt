package com.xianming.watch4sat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SatelliteEntity::class,
        SatelliteDatasetEntity::class,
        TransmitterEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class Watch4SatDatabase : RoomDatabase() {

    abstract fun satelliteDao(): SatelliteDao

    abstract fun transmitterDao(): TransmitterDao

    companion object {
        private const val DATABASE_NAME = "watch4sat.db"

        @Volatile
        private var instance: Watch4SatDatabase? = null

        fun create(context: Context): Watch4SatDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Watch4SatDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(Migration1To2)
                    .build()
                    .also { instance = it }
            }
        }

        val Migration1To2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `satellite_dataset` (
                        `singletonId` INTEGER NOT NULL,
                        `datasetGeneration` INTEGER NOT NULL,
                        `normalizedContentSha256` TEXT NOT NULL,
                        `retrievedAtMillis` INTEGER NOT NULL,
                        `acceptedRecordCount` INTEGER NOT NULL,
                        `sourceIdentity` TEXT NOT NULL,
                        PRIMARY KEY(`singletonId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
