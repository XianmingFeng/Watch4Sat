package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationCoordinates
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.pass.PassPredictionService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class OrbitMapDetailUiState(
    val catalogNumber: Int? = null,
    val currentPosition: GroundTrackPoint? = null,
    val footprintRadiusKm: Double? = null,
    val slantRangeKm: Double? = null,
    val lastUpdatedMillis: Long = 0L
)

data class OrbitMapDetailRequestKey(
    val catalogNumber: Int,
    val generation: Long
)

object OrbitMapDetailRequestPolicy {
    fun nextGeneration(current: Long): Long = current + 1L

    fun requestOrNull(
        catalogNumber: Int?,
        generation: Long
    ): OrbitMapDetailRequestKey? {
        return catalogNumber
            ?.takeIf { it > 0 }
            ?.let { OrbitMapDetailRequestKey(it, generation) }
    }

    fun canCommit(
        request: OrbitMapDetailRequestKey,
        activeCatalogNumber: Int?,
        activeGeneration: Long
    ): Boolean {
        return request.catalogNumber == activeCatalogNumber &&
            request.generation == activeGeneration
    }
}

object OrbitMapDetailUpdatePolicy {
    const val NextUpdateIntervalMillis: Long = 1_000L

    fun delayUntilNextUpdate(nowMillis: Long): Long {
        return NextUpdateIntervalMillis -
            Math.floorMod(nowMillis, NextUpdateIntervalMillis)
    }
}

internal class OrbitMapDetailUpdateLoop(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val delayMillis: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun run(
        request: OrbitMapDetailRequestKey,
        update: suspend (OrbitMapDetailRequestKey) -> Unit
    ) {
        while (currentCoroutineContext().isActive) {
            update(request)
            delayMillis(
                OrbitMapDetailUpdatePolicy.delayUntilNextUpdate(
                    currentTimeMillis()
                )
            )
        }
    }
}

object OrbitMapDetailStationPolicy {
    fun coordinatesOrNull(station: StationLocation?): StationCoordinates? {
        return station
            ?.takeIf {
                it.latitude.isFinite() &&
                    it.latitude in -90.0..90.0 &&
                    it.longitude.isFinite()
            }
            ?.let {
                StationCoordinates(
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
    }
}

internal object OrbitMapDetailPositionResolver {
    fun resolve(
        satellite: SatelliteRecord,
        savedStation: StationLocation?,
        timeMillis: Long,
        stationPositionAt: (
            satellite: SatelliteRecord,
            station: StationCoordinates,
            timeMillis: Long
        ) -> OrbitalPosition = { target, station, time ->
            PassPredictionService.positionAt(target, station, time)
        },
        groundPositionAt: (
            satellite: SatelliteRecord,
            timeMillis: Long
        ) -> OrbitalPosition = PassPredictionService::groundPositionAt
    ): OrbitalPosition {
        val station = OrbitMapDetailStationPolicy.coordinatesOrNull(savedStation)
        return if (station != null) {
            stationPositionAt(satellite, station, timeMillis)
        } else {
            groundPositionAt(satellite, timeMillis).copy(slantRangeKm = null)
        }
    }
}

internal data class OrbitMapDetailRuntimeState(
    val catalogNumber: Int? = null,
    val generation: Long = 0L,
    val currentPosition: GroundTrackPoint? = null,
    val footprintRadiusKm: Double? = null,
    val slantRangeKm: Double? = null,
    val lastUpdatedMillis: Long = 0L
) {
    fun accepts(request: OrbitMapDetailRequestKey): Boolean {
        return OrbitMapDetailRequestPolicy.canCommit(
            request = request,
            activeCatalogNumber = catalogNumber,
            activeGeneration = generation
        )
    }

    fun toUiState(): OrbitMapDetailUiState {
        return OrbitMapDetailUiState(
            catalogNumber = catalogNumber,
            currentPosition = currentPosition,
            footprintRadiusKm = footprintRadiusKm,
            slantRangeKm = slantRangeKm,
            lastUpdatedMillis = lastUpdatedMillis
        )
    }
}

internal object OrbitMapDetailRuntimeReducer {
    fun start(
        previous: OrbitMapDetailRuntimeState,
        catalogNumber: Int
    ): OrbitMapDetailRuntimeState {
        require(catalogNumber > 0) { "Catalog number must be positive" }
        val sameCatalog = previous.catalogNumber == catalogNumber
        return previous.copy(
            catalogNumber = catalogNumber,
            generation = OrbitMapDetailRequestPolicy.nextGeneration(previous.generation),
            currentPosition = previous.currentPosition.takeIf { sameCatalog },
            footprintRadiusKm = previous.footprintRadiusKm.takeIf { sameCatalog },
            slantRangeKm = previous.slantRangeKm.takeIf { sameCatalog },
            lastUpdatedMillis = previous.lastUpdatedMillis.takeIf { sameCatalog } ?: 0L
        )
    }

    fun stop(
        previous: OrbitMapDetailRuntimeState
    ): OrbitMapDetailRuntimeState {
        return previous.copy(
            generation = OrbitMapDetailRequestPolicy.nextGeneration(previous.generation)
        )
    }

    fun commitSuccess(
        previous: OrbitMapDetailRuntimeState,
        request: OrbitMapDetailRequestKey,
        currentPosition: GroundTrackPoint,
        footprintRadiusKm: Double?,
        slantRangeKm: Double?
    ): OrbitMapDetailRuntimeState {
        if (!previous.accepts(request)) return previous
        return previous.copy(
            currentPosition = currentPosition,
            footprintRadiusKm = footprintRadiusKm,
            slantRangeKm = slantRangeKm,
            lastUpdatedMillis = currentPosition.timeMillis
        )
    }

    fun commitFailure(
        previous: OrbitMapDetailRuntimeState
    ): OrbitMapDetailRuntimeState {
        return previous
    }

    fun commitMissingSatellite(
        previous: OrbitMapDetailRuntimeState,
        request: OrbitMapDetailRequestKey
    ): OrbitMapDetailRuntimeState {
        if (!previous.accepts(request)) return previous
        return previous.copy(
            currentPosition = null,
            footprintRadiusKm = null,
            slantRangeKm = null,
            lastUpdatedMillis = 0L
        )
    }
}
