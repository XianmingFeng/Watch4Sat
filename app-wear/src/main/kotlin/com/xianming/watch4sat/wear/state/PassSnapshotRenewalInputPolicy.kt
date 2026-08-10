package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.data.pass.PassSnapshotKey
import com.xianming.watch4sat.data.pass.PassSnapshotKeys
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.wear.PassSnapshotRenewalReason

object PassSnapshotRenewalInputPolicy {

    fun keyFor(
        settings: Watch4SatSettings,
        satellites: List<SatelliteRecord>
    ): PassSnapshotKey? {
        val station = settings.stationLocation
        if (!settings.setupCompleted || station == null) return null
        val selected = satellites.filter(SatelliteRecord::selected)
        val candidates = selected.ifEmpty {
            val starterIds = StarterSelectionPolicy.pickStarterSelection(satellites)
            satellites.filter { satellite -> satellite.catalogNumber in starterIds }
        }
        if (candidates.isEmpty()) return null
        return PassSnapshotKeys.forInputs(
            station = station,
            satellites = candidates,
            settings = settings,
            minimumElevationDegrees =
                MinimumElevationPolicy.effectiveMinimumElevationDegrees(settings)
        )
    }
}

class PassSnapshotRenewalInputTracker {
    private var hasTriggered = false
    private var wasReady = false
    private var lastReadyKey: PassSnapshotKey? = null

    fun nextReason(key: PassSnapshotKey?): PassSnapshotRenewalReason? {
        if (key == null) {
            wasReady = false
            return null
        }
        if (wasReady && key == lastReadyKey) return null
        val reason = if (hasTriggered) {
            PassSnapshotRenewalReason.PLANNING_INPUT_CHANGED
        } else {
            PassSnapshotRenewalReason.APP_START
        }
        hasTriggered = true
        wasReady = true
        lastReadyKey = key
        return reason
    }
}
