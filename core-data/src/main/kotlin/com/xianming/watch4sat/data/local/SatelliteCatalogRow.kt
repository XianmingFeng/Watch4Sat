package com.xianming.watch4sat.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class SatelliteCatalogRow(
    @Embedded val satellite: SatelliteEntity,
    @ColumnInfo(name = "dataset_generation")
    val datasetGeneration: Long?,
    @ColumnInfo(name = "dataset_content_sha256")
    val normalizedContentSha256: String?,
    @ColumnInfo(name = "dataset_retrieved_at_millis")
    val retrievedAtMillis: Long?,
    @ColumnInfo(name = "dataset_accepted_record_count")
    val acceptedRecordCount: Int?,
    @ColumnInfo(name = "dataset_source_identity")
    val sourceIdentity: String?
)

data class SatelliteCatalogLocalSnapshot(
    val satellites: List<SatelliteEntity>,
    val dataset: SatelliteDatasetEntity?
)
