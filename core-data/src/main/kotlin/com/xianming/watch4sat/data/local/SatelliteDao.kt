package com.xianming.watch4sat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SatelliteDao {

    @Query("SELECT * FROM satellites ORDER BY displayName COLLATE NOCASE ASC")
    abstract fun observeAllSatellites(): Flow<List<SatelliteEntity>>

    @Query(
        """
        SELECT
            s.*,
            d.datasetGeneration AS dataset_generation,
            d.normalizedContentSha256 AS dataset_content_sha256,
            d.retrievedAtMillis AS dataset_retrieved_at_millis,
            d.acceptedRecordCount AS dataset_accepted_record_count,
            d.sourceIdentity AS dataset_source_identity
        FROM satellites AS s
        LEFT JOIN satellite_dataset AS d ON d.singletonId = 0
        ORDER BY s.displayName COLLATE NOCASE ASC
        """
    )
    abstract fun observeSatelliteCatalog(): Flow<List<SatelliteCatalogRow>>

    @Query("SELECT * FROM satellites ORDER BY displayName COLLATE NOCASE ASC")
    abstract suspend fun getAllSatellites(): List<SatelliteEntity>

    @Query("SELECT * FROM satellites WHERE catalogNumber IN (:catalogNumbers) ORDER BY displayName COLLATE NOCASE ASC")
    abstract fun observeSatellitesByCatalogNumbers(catalogNumbers: List<Int>): Flow<List<SatelliteEntity>>

    @Query("SELECT * FROM satellites WHERE catalogNumber IN (:catalogNumbers) ORDER BY displayName COLLATE NOCASE ASC")
    abstract suspend fun getSatellitesByCatalogNumbers(catalogNumbers: List<Int>): List<SatelliteEntity>

    @Query("SELECT COUNT(*) FROM satellites")
    abstract suspend fun satelliteCount(): Int

    @Query("SELECT * FROM satellite_dataset WHERE singletonId = 0")
    abstract suspend fun getDataset(): SatelliteDatasetEntity?

    @Transaction
    open suspend fun getCatalog(): SatelliteCatalogLocalSnapshot {
        return SatelliteCatalogLocalSnapshot(
            satellites = getAllSatellites(),
            dataset = getDataset()
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSatellites(entities: List<SatelliteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDataset(dataset: SatelliteDatasetEntity)

    @Query("DELETE FROM satellites")
    abstract suspend fun clearSatellites()

    @Transaction
    open suspend fun replaceAllSatellites(
        entities: List<SatelliteEntity>,
        normalizedContentSha256: String,
        retrievedAtMillis: Long,
        sourceIdentity: String
    ): SatelliteDatasetEntity {
        val dataset = SatelliteDatasetEntity(
            datasetGeneration = (getDataset()?.datasetGeneration ?: 0L) + 1L,
            normalizedContentSha256 = normalizedContentSha256,
            retrievedAtMillis = retrievedAtMillis,
            acceptedRecordCount = entities.size,
            sourceIdentity = sourceIdentity
        )
        clearSatellites()
        if (entities.isNotEmpty()) {
            insertSatellites(entities)
        }
        insertDataset(dataset)
        return dataset
    }
}
