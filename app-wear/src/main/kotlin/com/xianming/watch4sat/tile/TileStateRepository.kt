package com.xianming.watch4sat.tile

import com.xianming.watch4sat.data.repository.SatelliteDataRepository
import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import com.xianming.watch4sat.domain.freshness.TleEpochSample
import com.xianming.watch4sat.domain.freshness.TleFreshnessAssessment
import com.xianming.watch4sat.domain.freshness.TleFreshnessPolicy
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.pass.PassPredictionService
import com.xianming.watch4sat.wear.state.TleFreshnessScopePolicy

class TileStateRepository(
    private val satelliteDataRepository: SatelliteDataRepository,
    private val settingsStore: Watch4SatSettingsStore,
    passSnapshotCache: PassSnapshotCache
) {
    private val passResolver = TilePassResolver(passSnapshotCache) { satellites, station, startMillis, window ->
        PassPredictionService.predictPasses(satellites, station, startMillis, window)
    }

    suspend fun load(nowMillis: Long = System.currentTimeMillis()): TileState {
        val settings = settingsStore.getSettings()
        val catalog = satelliteDataRepository.getSatelliteCatalog()
        val cached = catalog.records
        val selected = cached.filter { satellite -> satellite.selected }
        val resolution = passResolver.resolve(
            settings = settings,
            selectedSatellites = selected,
            cachedSatellites = cached,
            nowMillis = nowMillis
        )
        return TileState(
            station = resolution.station,
            selectedSatelliteCount = selected.size,
            cachedSatelliteCount = cached.size,
            nextPass = resolution.nextPass,
            passResolutionStatus = resolution.status,
            tleFreshness = TleFreshnessPolicy.assess(
                nowMillis = nowMillis,
                retrievedAtMillis =
                    catalog.dataset?.retrievedAtMillis
                        ?: settings.lastSatelliteDataUpdateMillis,
                samples = TleFreshnessScopePolicy
                    .relevantSatellites(cached)
                    .map { satellite ->
                        TleEpochSample(
                            catalogNumber = satellite.catalogNumber,
                            epoch = satellite.orbitalData.epoch
                        )
                    }
            ),
            satelliteDataError = settings.lastSatelliteDataError,
            transmitterDataError = settings.lastTransmitterDataError
        )
    }
}

data class TileState(
    val station: StationLocation?,
    val selectedSatelliteCount: Int,
    val cachedSatelliteCount: Int,
    val nextPass: SatellitePass?,
    val passResolutionStatus: TilePassResolutionStatus,
    val tleFreshness: TleFreshnessAssessment,
    val satelliteDataError: String?,
    val transmitterDataError: String?
)
