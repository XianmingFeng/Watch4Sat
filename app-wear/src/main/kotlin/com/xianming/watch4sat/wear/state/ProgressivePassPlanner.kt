package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.pass.PassSnapshotKeys
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgressivePassPlanner(
    private val snapshotCache: PassSnapshotCache,
    private val predictor: (
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow
    ) -> List<SatellitePass>
) {

    suspend fun run(
        input: PassPlanningInput,
        emit: suspend (PassPlanningProgress) -> Unit
    ) {
        val targetHours = input.settings.passWindowHours.coerceAtLeast(1)
        emit(
            PassPlanningProgress(
                status = PassPlanningStatus.Calculating,
                targetWindowHours = targetHours
            )
        )
        val station = input.settings.stationLocation
        if (station == null) {
            emit(
                PassPlanningProgress(
                    status = PassPlanningStatus.NeedsQth,
                    targetWindowHours = targetHours
                )
            )
            return
        }
        if (input.satellites.isEmpty()) {
            emit(
                PassPlanningProgress(
                    status = PassPlanningStatus.NoSatellites,
                    targetWindowHours = targetHours
                )
            )
            return
        }

        val key = PassSnapshotKeys.forInputs(
            station = station,
            satellites = input.satellites,
            settings = input.settings,
            minimumElevationDegrees = input.minimumElevationDegrees
        )
        val targetEndMillis = input.nowMillis + targetHours * HourMillis
        val snapshot = snapshotCache.read(key, input.nowMillis)
        var mergedPasses = snapshot?.toPasses(input.satellites)
            ?.filterUsable(input.nowMillis, targetEndMillis)
            .orEmpty()
        var coveredUntilMillis = snapshot
            ?.reusableCoverageEndMillis(input.nowMillis)
            ?.coerceAtMost(targetEndMillis)
            ?: input.nowMillis

        if (snapshot != null) {
            emit(
                PassPlanningProgress(
                    passes = mergedPasses,
                    status = PassPlanningStatus.FromSnapshot,
                    coveredWindowHours = coveredHours(
                        startMillis = input.nowMillis,
                        coveredUntilMillis = coveredUntilMillis,
                        targetHours = targetHours
                    ),
                    fromSnapshot = true,
                    targetWindowHours = targetHours
                )
            )
        }

        if (coveredUntilMillis >= targetEndMillis) {
            emit(
                PassPlanningProgress(
                    passes = mergedPasses,
                    status = PassPlanningStatus.Ready,
                    coveredWindowHours = targetHours,
                    fromSnapshot = true,
                    targetWindowHours = targetHours
                )
            )
            return
        }

        val firstWindowHours = targetHours.coerceAtMost(InitialWindowHours)
        val firstWindowEndMillis = input.nowMillis + firstWindowHours * HourMillis
        if (coveredUntilMillis < firstWindowEndMillis) {
            mergedPasses = mergePasses(
                existing = mergedPasses,
                fresh = predictRange(
                    satellites = input.satellites,
                    station = station,
                    startMillis = coveredUntilMillis,
                    endMillis = firstWindowEndMillis,
                    nowMillis = input.nowMillis,
                    minimumElevationDegrees = input.minimumElevationDegrees
                ),
                nowMillis = input.nowMillis,
                targetEndMillis = targetEndMillis
            )
            coveredUntilMillis = firstWindowEndMillis
            emitProgress(
                emit = emit,
                passes = mergedPasses,
                coveredHours = firstWindowHours,
                targetHours = targetHours
            )
        }

        while (coveredUntilMillis < targetEndMillis) {
            val completedHours = coveredHours(
                startMillis = input.nowMillis,
                coveredUntilMillis = coveredUntilMillis,
                targetHours = targetHours
            )
            val nextCoveredHours = (completedHours + 1).coerceAtMost(targetHours)
            val chunkEndMillis = (input.nowMillis + nextCoveredHours * HourMillis)
                .coerceAtMost(targetEndMillis)
            mergedPasses = mergePasses(
                existing = mergedPasses,
                fresh = predictRange(
                    satellites = input.satellites,
                    station = station,
                    startMillis = coveredUntilMillis,
                    endMillis = chunkEndMillis,
                    nowMillis = input.nowMillis,
                    minimumElevationDegrees = input.minimumElevationDegrees
                ),
                nowMillis = input.nowMillis,
                targetEndMillis = targetEndMillis
            )
            coveredUntilMillis = chunkEndMillis
            emitProgress(
                emit = emit,
                passes = mergedPasses,
                coveredHours = nextCoveredHours,
                targetHours = targetHours
            )
        }
    }

    private suspend fun predictRange(
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        endMillis: Long,
        nowMillis: Long,
        minimumElevationDegrees: Double
    ): List<SatellitePass> {
        val durationMillis = (endMillis - startMillis).coerceAtLeast(1L)
        val hoursAhead = ((durationMillis + HourMillis - 1L) / HourMillis)
            .toInt()
            .coerceAtLeast(1)
        return predictChunk(
            satellites = satellites,
            station = station,
            startMillis = startMillis,
            hoursAhead = hoursAhead,
            minimumElevationDegrees = minimumElevationDegrees
        )
            .filter { pass -> pass.losMillis > nowMillis && pass.aosMillis < endMillis }
    }

    private suspend fun emitProgress(
        emit: suspend (PassPlanningProgress) -> Unit,
        passes: List<SatellitePass>,
        coveredHours: Int,
        targetHours: Int
    ) {
        emit(
            PassPlanningProgress(
                passes = passes,
                status = if (coveredHours >= targetHours) PassPlanningStatus.Ready else PassPlanningStatus.Calculating,
                coveredWindowHours = coveredHours,
                targetWindowHours = targetHours
            )
        )
    }

    private suspend fun predictChunk(
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        hoursAhead: Int,
        minimumElevationDegrees: Double
    ): List<SatellitePass> = withContext(Dispatchers.Default) {
        predictor(
            satellites,
            station,
            startMillis,
            PassWindow(hoursAhead = hoursAhead, minimumElevationDegrees = minimumElevationDegrees)
        )
    }

    private fun List<SatellitePass>.filterUsable(
        nowMillis: Long,
        targetEndMillis: Long
    ): List<SatellitePass> {
        return filter { pass -> pass.losMillis > nowMillis && pass.aosMillis < targetEndMillis }
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
    }

    private fun mergePasses(
        existing: List<SatellitePass>,
        fresh: List<SatellitePass>,
        nowMillis: Long,
        targetEndMillis: Long
    ): List<SatellitePass> {
        return PassChunkMerger.merge(existing + fresh)
            .filterUsable(nowMillis, targetEndMillis)
    }

    private fun coveredHours(
        startMillis: Long,
        coveredUntilMillis: Long,
        targetHours: Int
    ): Int {
        return ((coveredUntilMillis - startMillis).coerceAtLeast(0L) / HourMillis)
            .toInt()
            .coerceIn(0, targetHours)
    }

    private companion object {
        const val InitialWindowHours = 2
        const val HourMillis = 60L * 60L * 1000L
    }
}

data class PassPlanningInput(
    val settings: Watch4SatSettings,
    val satellites: List<SatelliteRecord>,
    val nowMillis: Long,
    val minimumElevationDegrees: Double = 0.0
) {
    fun isSamePlanningRequestAs(other: PassPlanningInput): Boolean {
        return settings.stationLocation.isSamePassPlanningStationAs(other.settings.stationLocation) &&
            settings.passWindowHours == other.settings.passWindowHours &&
            satellites == other.satellites &&
            minimumElevationDegrees == other.minimumElevationDegrees
    }

    private fun StationLocation?.isSamePassPlanningStationAs(other: StationLocation?): Boolean {
        if (this == null || other == null) return this == other
        return latitude == other.latitude &&
            longitude == other.longitude &&
            altitudeMeters == other.altitudeMeters &&
            qthLocator == other.qthLocator &&
            timestampMillis == other.timestampMillis &&
            source == other.source
    }
}

data class PassPlanningProgress(
    val passes: List<SatellitePass> = emptyList(),
    val status: PassPlanningStatus = PassPlanningStatus.Idle,
    val coveredWindowHours: Int = 0,
    val fromSnapshot: Boolean = false,
    val targetWindowHours: Int = 0
)

enum class PassPlanningStatus {
    Idle,
    NeedsQth,
    NoSatellites,
    FromSnapshot,
    Calculating,
    Ready,
    Failed
}
