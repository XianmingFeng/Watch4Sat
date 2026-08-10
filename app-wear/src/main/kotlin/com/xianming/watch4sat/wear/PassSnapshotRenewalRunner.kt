package com.xianming.watch4sat.wear

import com.xianming.watch4sat.data.pass.PassSnapshot
import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.pass.PassSnapshotKeys
import com.xianming.watch4sat.data.repository.SatelliteDataRepository
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.pass.PassPredictionService
import com.xianming.watch4sat.wear.state.MinimumElevationPolicy
import com.xianming.watch4sat.wear.state.PassChunkMerger
import com.xianming.watch4sat.wear.state.PassStartScheduleCandidate
import com.xianming.watch4sat.wear.state.PassStartSchedulePolicy
import com.xianming.watch4sat.wear.state.StarterSelectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

fun interface PassStartAlarmTarget {
    suspend fun replace(candidate: PassStartScheduleCandidate?, knownState: PassStartAlarmState)
}

class PassSnapshotRenewalRunner(
    private val repository: SatelliteDataRepository,
    private val settingsProvider: suspend () -> Watch4SatSettings,
    private val snapshotCache: PassSnapshotCache,
    private val alarmStateProvider: suspend () -> PassStartAlarmState,
    private val alarmTarget: PassStartAlarmTarget,
    private val predictor: suspend (
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow
    ) -> List<SatellitePass> = ::predictPassesCooperatively
) {

    suspend fun run(nowMillis: Long, forceRebuild: Boolean): PassSnapshotRenewalResult {
        val settings = settingsProvider()
        val station = settings.stationLocation
        val satellites = resolveSatellites()
        if (
            settings.stationDataDeletionInProgress ||
            !settings.setupCompleted ||
            station == null ||
            satellites.isEmpty()
        ) {
            alarmTarget.replace(null, alarmStateProvider())
            return PassSnapshotRenewalResult.PrerequisitesMissing
        }

        val minimumElevationDegrees =
            MinimumElevationPolicy.effectiveMinimumElevationDegrees(settings)
        val key = PassSnapshotKeys.forInputs(
            station = station,
            satellites = satellites,
            settings = settings,
            minimumElevationDegrees = minimumElevationDegrees
        )
        val existing = snapshotCache.read(key, nowMillis)
        val renewed = PassSnapshotRenewalPolicy.needsRenewal(
            snapshot = existing,
            expectedKey = key,
            nowMillis = nowMillis,
            force = forceRebuild
        )
        val snapshot = if (renewed) {
            val coverageEndMillis = Math.addExact(
                nowMillis,
                PassSnapshotRenewalPolicy.CoverageMillis
            )
            val passes = withContext(Dispatchers.Default) {
                predictor(
                    satellites,
                    station,
                    nowMillis,
                    PassWindow(
                        hoursAhead = PassSnapshotRenewalPolicy.CoverageHours,
                        minimumElevationDegrees = minimumElevationDegrees
                    )
                )
            }
                .filter { pass ->
                    pass.losMillis > nowMillis && pass.aosMillis < coverageEndMillis
                }
                .let(PassChunkMerger::merge)
            val generated = PassSnapshot.fromPasses(
                key = key,
                generatedAtMillis = nowMillis,
                coveredWindowHours = PassSnapshotRenewalPolicy.CoverageHours,
                passes = passes,
                coverageStartMillis = nowMillis,
                coverageEndMillis = coverageEndMillis
            )
            if (!isStationContextCurrent(settings, station)) {
                alarmTarget.replace(null, alarmStateProvider())
                return PassSnapshotRenewalResult.Superseded
            }
            if (forceRebuild) {
                snapshotCache.replace(generated)
            } else {
                snapshotCache.write(generated)
            }
            if (!isStationContextCurrent(settings, station)) {
                snapshotCache.discard(generated)
                alarmTarget.replace(null, alarmStateProvider())
                return PassSnapshotRenewalResult.Superseded
            }
            generated
        } else {
            requireNotNull(existing)
        }

        val effectiveSnapshot = snapshotCache.read(key, nowMillis)
            ?: return PassSnapshotRenewalResult.Superseded
        val alarmState = alarmStateProvider()
        val candidate = PassStartSchedulePolicy.nextScheduleCandidate(
            passes = effectiveSnapshot.toPasses(satellites),
            nowMillis = nowMillis,
            handledPassKeys = alarmState.handledPassKeys,
            allowCatchUp = false,
            passAlertAdvanceMinutes = settings.passAlertAdvanceMinutes
        )
        if (!isStationContextCurrent(settings, station)) {
            snapshotCache.discard(effectiveSnapshot)
            alarmTarget.replace(null, alarmState)
            return PassSnapshotRenewalResult.Superseded
        }
        alarmTarget.replace(candidate, alarmState)
        return PassSnapshotRenewalResult.Ready(
            snapshot = effectiveSnapshot,
            candidate = candidate,
            renewed = renewed
        )
    }

    private suspend fun resolveSatellites(): List<SatelliteRecord> {
        val selected = repository.getSelectedSatelliteRecords()
        if (selected.isNotEmpty()) return selected
        val cached = repository.getCachedSatellites()
        val starterIds = StarterSelectionPolicy.pickStarterSelection(cached)
        return cached.filter { satellite -> satellite.catalogNumber in starterIds }
    }

    private suspend fun isStationContextCurrent(
        expectedSettings: Watch4SatSettings,
        expectedStation: StationLocation
    ): Boolean {
        val current = settingsProvider()
        return !current.stationDataDeletionInProgress &&
            current.stationDataGeneration == expectedSettings.stationDataGeneration &&
            current.stationLocation == expectedStation
    }
}

private suspend fun predictPassesCooperatively(
    satellites: List<SatelliteRecord>,
    station: StationLocation,
    startMillis: Long,
    window: PassWindow
): List<SatellitePass> {
    return satellites.flatMap { satellite ->
        coroutineContext.ensureActive()
        PassPredictionService.predictPasses(
            satellites = listOf(satellite),
            station = station,
            startMillis = startMillis,
            window = window
        ).also {
            coroutineContext.ensureActive()
        }
    }
}

sealed interface PassSnapshotRenewalResult {
    data object PrerequisitesMissing : PassSnapshotRenewalResult
    data object Superseded : PassSnapshotRenewalResult

    data class Ready(
        val snapshot: PassSnapshot,
        val candidate: PassStartScheduleCandidate?,
        val renewed: Boolean
    ) : PassSnapshotRenewalResult
}
