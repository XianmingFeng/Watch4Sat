package com.xianming.watch4sat.data.repository

import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SatelliteDataRepository {

    val satellites: Flow<List<SatelliteRecord>>

    val satelliteCatalog: Flow<SatelliteCatalog>
        get() = satellites.map { records -> SatelliteCatalog(records = records) }

    val transmitters: Flow<List<TransmitterRecord>>

    val selectedSatelliteIds: Flow<Set<Int>>

    val selectedSatellites: Flow<List<SatelliteRecord>>

    suspend fun refreshSatellites(): DataRefreshResult

    suspend fun refreshTransmitters(): DataRefreshResult

    suspend fun refreshAll(): DataRefreshSummary

    suspend fun setSelectedSatelliteIds(catalogNumbers: Set<Int>)

    suspend fun getCachedSatellites(): List<SatelliteRecord>

    suspend fun getCachedTransmitters(): List<TransmitterRecord>

    suspend fun getTransmittersForSatellite(catalogNumber: Int): List<TransmitterRecord>

    suspend fun getSelectedSatelliteRecords(): List<SatelliteRecord>

    suspend fun getSatelliteCatalog(): SatelliteCatalog {
        return SatelliteCatalog(records = getCachedSatellites())
    }
}

data class SatelliteDatasetMetadata(
    val generation: Long,
    val normalizedContentSha256: String,
    val retrievedAtMillis: Long,
    val acceptedRecordCount: Int,
    val sourceIdentity: String
)

data class SatelliteCatalog(
    val records: List<SatelliteRecord>,
    val dataset: SatelliteDatasetMetadata? = null
)

data class DataRefreshResult(
    val recordsPersisted: Int,
    val updatedAtMillis: Long,
    val failureMessage: String? = null
) {
    val succeeded: Boolean
        get() = failureMessage == null

    companion object {
        fun failure(message: String): DataRefreshResult {
            return DataRefreshResult(
                recordsPersisted = 0,
                updatedAtMillis = 0L,
                failureMessage = message
            )
        }
    }
}

data class DataRefreshSummary(
    val satellites: DataRefreshResult,
    val transmitters: DataRefreshResult
) {
    val succeededCount: Int
        get() = listOf(satellites, transmitters).count(DataRefreshResult::succeeded)

    val isPartialSuccess: Boolean
        get() = succeededCount == 1

    val failedCompletely: Boolean
        get() = succeededCount == 0

    val outcome: DataRefreshOutcome
        get() = when {
            failedCompletely -> DataRefreshOutcome.Failed
            isPartialSuccess && satellites.succeeded -> DataRefreshOutcome.Partial(
                successfulSource = DataRefreshSource.Satellites,
                recordsPersisted = satellites.recordsPersisted
            )
            isPartialSuccess -> DataRefreshOutcome.Partial(
                successfulSource = DataRefreshSource.Transmitters,
                recordsPersisted = transmitters.recordsPersisted
            )
            else -> DataRefreshOutcome.Complete(
                satelliteRecordsPersisted = satellites.recordsPersisted,
                transmitterRecordsPersisted = transmitters.recordsPersisted
            )
        }
}

enum class DataRefreshSource {
    Satellites,
    Transmitters
}

sealed interface DataRefreshOutcome {
    data class Complete(
        val satelliteRecordsPersisted: Int,
        val transmitterRecordsPersisted: Int
    ) : DataRefreshOutcome

    data class Partial(
        val successfulSource: DataRefreshSource,
        val recordsPersisted: Int
    ) : DataRefreshOutcome

    data object Failed : DataRefreshOutcome
}
