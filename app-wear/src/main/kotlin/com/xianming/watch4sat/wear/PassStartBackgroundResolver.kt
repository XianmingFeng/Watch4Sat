package com.xianming.watch4sat.wear

import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.pass.PassSnapshotKeys
import com.xianming.watch4sat.data.repository.SatelliteDataRepository
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.state.PassStartScheduleCandidate
import com.xianming.watch4sat.wear.state.PassStartSchedulePolicy
import com.xianming.watch4sat.wear.state.MinimumElevationPolicy
import com.xianming.watch4sat.wear.state.StarterSelectionPolicy

class PassStartBackgroundResolver(
    private val repository: SatelliteDataRepository,
    private val settingsProvider: suspend () -> Watch4SatSettings,
    private val snapshotCache: PassSnapshotCache
) {

    suspend fun resolveNextCandidate(
        nowMillis: Long,
        handledPassKeys: Set<String>,
        allowCatchUp: Boolean = true,
        catchUpGraceMillis: Long = PassStartSchedulePolicy.catchUpGraceMillis
    ): PassStartScheduleCandidate? {
        val settings = settingsProvider()
        val passes = resolve(settings = settings, nowMillis = nowMillis).passes
        return PassStartSchedulePolicy.nextScheduleCandidate(
            passes = passes,
            nowMillis = nowMillis,
            handledPassKeys = handledPassKeys,
            allowCatchUp = allowCatchUp,
            catchUpGraceMillis = catchUpGraceMillis,
            passAlertAdvanceMinutes = settings.passAlertAdvanceMinutes
        )
    }

    suspend fun resolvePasses(
        nowMillis: Long
    ): List<SatellitePass> {
        val settings = settingsProvider()
        return resolve(settings = settings, nowMillis = nowMillis).passes
    }

    suspend fun resolve(nowMillis: Long): PassStartBackgroundResolution {
        return resolve(settingsProvider(), nowMillis)
    }

    private suspend fun resolve(
        settings: Watch4SatSettings,
        nowMillis: Long
    ): PassStartBackgroundResolution {
        val station = settings.stationLocation
            ?: return PassStartBackgroundResolution(renewalRequired = true)
        val selected = repository.getSelectedSatelliteRecords()
        val candidates = selected.ifEmpty {
            val cached = repository.getCachedSatellites()
            val starterIds = StarterSelectionPolicy.pickStarterSelection(cached)
            cached.filter { satellite -> satellite.catalogNumber in starterIds }
        }
        if (candidates.isEmpty()) {
            return PassStartBackgroundResolution(renewalRequired = true)
        }
        val minimumElevationDegrees = MinimumElevationPolicy.effectiveMinimumElevationDegrees(settings)
        val key = PassSnapshotKeys.forInputs(
            station = station,
            satellites = candidates,
            settings = settings,
            minimumElevationDegrees = minimumElevationDegrees
        )
        val snapshot = snapshotCache.read(key, nowMillis)
        val valid = snapshot?.takeIf {
            it.isComplete &&
                it.coverageStartMillis <= nowMillis &&
                it.coverageEndMillis >= nowMillis
        }
        return PassStartBackgroundResolution(
            passes = valid
                ?.toPasses(candidates)
                ?.filter { pass -> pass.losMillis > nowMillis }
                .orEmpty(),
            coverageEndMillis = valid?.coverageEndMillis,
            renewalRequired = valid == null ||
                valid.coverageEndMillis - nowMillis <
                PassSnapshotRenewalPolicy.RenewBeforeExpiryMillis
        )
    }
}

data class PassStartBackgroundResolution(
    val passes: List<SatellitePass> = emptyList(),
    val coverageEndMillis: Long? = null,
    val renewalRequired: Boolean
)
