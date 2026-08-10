package com.xianming.watch4sat.data.repository

import com.xianming.watch4sat.data.identity.SatelliteDataIdentity
import com.xianming.watch4sat.data.local.SatelliteDao
import com.xianming.watch4sat.data.local.SatelliteDatasetEntity
import com.xianming.watch4sat.data.local.TransmitterDao
import com.xianming.watch4sat.data.local.toDomain
import com.xianming.watch4sat.data.local.toEntity
import com.xianming.watch4sat.data.network.CELESTRAK_AMATEUR_CSV_URL
import com.xianming.watch4sat.data.network.Watch4SatNetworkDataSource
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.parser.CelestrakParser
import com.xianming.watch4sat.domain.parser.SatnogsTransmitterParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DefaultSatelliteDataRepository(
    private val satelliteDao: SatelliteDao,
    private val transmitterDao: TransmitterDao,
    private val settingsStore: Watch4SatSettingsStore,
    private val networkDataSource: Watch4SatNetworkDataSource,
    private val clockMillis: () -> Long = System::currentTimeMillis
) : SatelliteDataRepository {

    override val selectedSatelliteIds: Flow<Set<Int>> = settingsStore.selectedSatelliteIds

    override val satelliteCatalog: Flow<SatelliteCatalog> = combine(
        satelliteDao.observeSatelliteCatalog(),
        settingsStore.selectedSatelliteIds
    ) { rows, selectedIds ->
        SatelliteCatalog(
            records = rows.map { row -> row.satellite.toDomain(selectedIds) },
            dataset = rows.firstOrNull()?.let { row ->
                val generation = row.datasetGeneration ?: return@let null
                SatelliteDatasetMetadata(
                    generation = generation,
                    normalizedContentSha256 =
                        row.normalizedContentSha256 ?: return@let null,
                    retrievedAtMillis = row.retrievedAtMillis ?: return@let null,
                    acceptedRecordCount = row.acceptedRecordCount ?: return@let null,
                    sourceIdentity = row.sourceIdentity ?: return@let null
                )
            }
        )
    }

    override val satellites: Flow<List<SatelliteRecord>> =
        satelliteCatalog.map { catalog -> catalog.records }

    override val transmitters: Flow<List<TransmitterRecord>> = transmitterDao
        .observeAllTransmitters()
        .map { entities -> entities.map { it.toDomain() } }

    override val selectedSatellites: Flow<List<SatelliteRecord>> =
        satelliteCatalog.map { catalog -> catalog.records.filter(SatelliteRecord::selected) }

    override suspend fun refreshSatellites(): DataRefreshResult {
        return runCatching {
            val parseResult = CelestrakParser.parseCsvResult(
                networkDataSource.fetchCelestrakAmateur()
            )
            val acceptance = RefreshBatchPolicy.evaluateTle(
                result = parseResult,
                previousAcceptedCount = satelliteDao.satelliteCount()
            )
            if (acceptance is BatchAcceptance.Rejected) {
                throw RefreshBatchRejectedException("CelesTrak Amateur", acceptance.reasons)
            }
            val records = parseResult.records
            val updatedAt = clockMillis()
            satelliteDao.replaceAllSatellites(
                entities = records.map { it.toEntity() },
                normalizedContentSha256 = SatelliteDataIdentity.sha256(records),
                retrievedAtMillis = updatedAt,
                sourceIdentity = CELESTRAK_AMATEUR_CSV_URL
            )
            runCatching { settingsStore.setLastSatelliteDataUpdateMillis(updatedAt) }
            DataRefreshResult(recordsPersisted = records.size, updatedAtMillis = updatedAt)
        }.getOrElse { throwable ->
            runCatching {
                settingsStore.setSatelliteDataRefreshError(throwable.refreshMessage(), clockMillis())
            }
            throw throwable
        }
    }

    override suspend fun refreshTransmitters(): DataRefreshResult {
        return runCatching {
            val parseResult = SatnogsTransmitterParser.parseActiveTransmittersResult(
                networkDataSource.fetchSatnogsActiveTransmitters()
            )
            val acceptance = RefreshBatchPolicy.evaluateTransmitters(
                result = parseResult,
                previousAcceptedCount = transmitterDao.transmitterCount()
            )
            if (acceptance is BatchAcceptance.Rejected) {
                throw RefreshBatchRejectedException("SatNOGS transmitter", acceptance.reasons)
            }
            val records = parseResult.records
            transmitterDao.replaceAllTransmitters(records.map { it.toEntity() })
            val updatedAt = clockMillis()
            settingsStore.setLastTransmitterDataUpdateMillis(updatedAt)
            DataRefreshResult(recordsPersisted = records.size, updatedAtMillis = updatedAt)
        }.getOrElse { throwable ->
            runCatching {
                settingsStore.setTransmitterDataRefreshError(throwable.refreshMessage(), clockMillis())
            }
            throw throwable
        }
    }

    override suspend fun refreshAll(): DataRefreshSummary {
        return DataRefreshSummary(
            satellites = independentRefresh(::refreshSatellites),
            transmitters = independentRefresh(::refreshTransmitters)
        )
    }

    override suspend fun setSelectedSatelliteIds(catalogNumbers: Set<Int>) {
        settingsStore.setSelectedSatelliteIds(catalogNumbers)
    }

    override suspend fun getCachedSatellites(): List<SatelliteRecord> {
        return getSatelliteCatalog().records
    }

    override suspend fun getCachedTransmitters(): List<TransmitterRecord> {
        return transmitterDao.getAllTransmitters().map { it.toDomain() }
    }

    override suspend fun getTransmittersForSatellite(catalogNumber: Int): List<TransmitterRecord> {
        return transmitterDao.getTransmittersForSatellite(catalogNumber).map { it.toDomain() }
    }

    override suspend fun getSelectedSatelliteRecords(): List<SatelliteRecord> {
        val selectedIds = settingsStore.getSettings().selectedSatelliteIds
        if (selectedIds.isEmpty()) {
            return emptyList()
        }
        return satelliteDao
            .getSatellitesByCatalogNumbers(selectedIds.toList())
            .map { it.toDomain(selectedIds) }
    }

    override suspend fun getSatelliteCatalog(): SatelliteCatalog {
        val selectedIds = settingsStore.getSettings().selectedSatelliteIds
        val snapshot = satelliteDao.getCatalog()
        return SatelliteCatalog(
            records = snapshot.satellites.map { entity -> entity.toDomain(selectedIds) },
            dataset = snapshot.dataset
                ?.takeIf { snapshot.satellites.isNotEmpty() }
                ?.toExternalMetadata()
        )
    }

    private fun Throwable.refreshMessage(): String {
        return message?.take(180) ?: this::class.simpleName.orEmpty()
    }

    private suspend fun independentRefresh(
        refresh: suspend () -> DataRefreshResult
    ): DataRefreshResult {
        return runCatching { refresh() }
            .getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                DataRefreshResult.failure(throwable.refreshMessage())
            }
    }

    private fun SatelliteDatasetEntity.toExternalMetadata(): SatelliteDatasetMetadata {
        return SatelliteDatasetMetadata(
            generation = datasetGeneration,
            normalizedContentSha256 = normalizedContentSha256,
            retrievedAtMillis = retrievedAtMillis,
            acceptedRecordCount = acceptedRecordCount,
            sourceIdentity = sourceIdentity
        )
    }
}
