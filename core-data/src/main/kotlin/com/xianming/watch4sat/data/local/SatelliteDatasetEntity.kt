package com.xianming.watch4sat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "satellite_dataset")
data class SatelliteDatasetEntity(
    @PrimaryKey val singletonId: Int = SingletonId,
    val datasetGeneration: Long,
    val normalizedContentSha256: String,
    val retrievedAtMillis: Long,
    val acceptedRecordCount: Int,
    val sourceIdentity: String
) {
    companion object {
        const val SingletonId: Int = 0
    }
}
