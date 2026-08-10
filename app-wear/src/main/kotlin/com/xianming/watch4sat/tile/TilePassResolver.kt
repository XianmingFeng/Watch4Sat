package com.xianming.watch4sat.tile

import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.pass.PassSnapshotKeys
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.wear.state.MinimumElevationPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TilePassResolver(
    private val snapshotCache: PassSnapshotCache,
    private val predictor: (
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow
    ) -> List<SatellitePass>
) {

    suspend fun resolve(
        settings: Watch4SatSettings,
        selectedSatellites: List<SatelliteRecord>,
        cachedSatellites: List<SatelliteRecord>,
        nowMillis: Long
    ): TilePassResolution {
        val station = settings.stationLocation
            ?: return TilePassResolution(status = TilePassResolutionStatus.NeedsQth, station = null)
        val candidates = selectedSatellites
        if (candidates.isEmpty()) {
            return TilePassResolution(status = TilePassResolutionStatus.NoSatellites, station = station)
        }
        val tileWindowHours = settings.passWindowHours.coerceAtLeast(1)

        val minimumElevationDegrees = MinimumElevationPolicy.effectiveMinimumElevationDegrees(settings)
        val key = PassSnapshotKeys.forInputs(
            station = station,
            satellites = candidates,
            settings = settings,
            minimumElevationDegrees = minimumElevationDegrees
        )
        val snapshot = snapshotCache.read(key, nowMillis)
        if (snapshot != null) {
            val snapshotPasses = snapshot.toPasses(candidates).filter { pass -> pass.losMillis > nowMillis }
            val snapshotCoversRequestedWindow =
                snapshot.coverageEndMillis >= nowMillis + tileWindowHours * HourMillis
            if (snapshotCoversRequestedWindow || snapshotPasses.isNotEmpty()) {
                return TilePassResolution(
                    station = station,
                    nextPass = snapshotPasses.firstOrNull(),
                    status = TilePassResolutionStatus.FromSnapshot
                )
            }
        }

        val passes = withContext(Dispatchers.Default) {
            predictor(
                candidates,
                station,
                nowMillis,
                PassWindow(
                    hoursAhead = tileWindowHours,
                    minimumElevationDegrees = minimumElevationDegrees
                )
            )
        }.filter { pass -> pass.losMillis > nowMillis }
        return TilePassResolution(
            station = station,
            nextPass = passes.firstOrNull(),
            status = TilePassResolutionStatus.Calculated
        )
    }
}

data class TilePassResolution(
    val station: StationLocation? = null,
    val nextPass: SatellitePass? = null,
    val status: TilePassResolutionStatus
)

enum class TilePassResolutionStatus {
    NeedsQth,
    NoSatellites,
    FromSnapshot,
    Calculated
}

private const val HourMillis = 60L * 60L * 1_000L
