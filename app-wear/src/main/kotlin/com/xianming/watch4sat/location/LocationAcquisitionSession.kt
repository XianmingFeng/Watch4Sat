package com.xianming.watch4sat.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class LocationAcquisitionSession(
    private val provider: LocationProvider,
    private val updateProvider: LocationUpdateProvider,
    private val elapsedRealtimeMillis: () -> Long = LocationClock::elapsedRealtimeMillis
) {
    suspend fun acquire(timeoutMillis: Long = LocationAcquisitionPolicy.ActiveSessionTimeoutMillis): LocationResult {
        val startedAtMillis = elapsedRealtimeMillis()
        LocationDiagnostics.log(
            LocationDiagnostics.sessionMessage(
                event = "start",
                elapsedMillis = 0L,
                timeoutMillis = timeoutMillis,
                state = null
            )
        )
        return try {
            val result = when {
                !provider.hasLocationPermission -> LocationResult(LocationResultState.NO_PERMISSION)
                !provider.hasEnabledLocationProvider() -> LocationResult(LocationResultState.LOCATION_PROVIDER_DISABLED)
                else -> acquirePermitted(timeoutMillis)
            }
            LocationDiagnostics.log(
                LocationDiagnostics.sessionMessage(
                    event = "finish",
                    elapsedMillis = elapsedRealtimeMillis() - startedAtMillis,
                    timeoutMillis = timeoutMillis,
                    state = result.state
                )
            )
            result
        } catch (cancellation: CancellationException) {
            LocationDiagnostics.log(
                LocationDiagnostics.sessionMessage(
                    event = "cancel",
                    elapsedMillis = elapsedRealtimeMillis() - startedAtMillis,
                    timeoutMillis = timeoutMillis,
                    state = null
                )
            )
            throw cancellation
        } catch (_: Throwable) {
            val result = LocationResult(state = LocationResultState.ERROR)
            LocationDiagnostics.log(
                LocationDiagnostics.sessionMessage(
                    event = "error",
                    elapsedMillis = elapsedRealtimeMillis() - startedAtMillis,
                    timeoutMillis = timeoutMillis,
                    state = result.state
                )
            )
            result
        }
    }

    private suspend fun acquirePermitted(timeoutMillis: Long): LocationResult {
        var sawLowAccuracyFix = false
        val result = withTimeoutOrNull(timeoutMillis) {
            LocationDiagnostics.log(LocationDiagnostics.phaseMessage("last-known-before", "start"))
            val lastKnownBefore = boundedLastKnownLocations()
            logFixCandidates("last-known-before", lastKnownBefore)
            bestAcceptableLastKnown(lastKnownBefore)?.let { fix ->
                return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
            }
            sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(lastKnownBefore)

            if (shouldUseActiveUpdatesBeforeCurrent()) {
                LocationDiagnostics.log(LocationDiagnostics.phaseMessage("active-updates", "start-before-current"))
                collectAcceptableUpdate { sawLowAccuracyFix = sawLowAccuracyFix || it }?.let { fix ->
                    return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
                }
            } else {
                LocationDiagnostics.log(LocationDiagnostics.phaseMessage("current", "start"))
                provider.currentLocation()?.let { fix ->
                    logFixCandidate("current", fix)
                    if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                        return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
                    }
                    sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(listOf(fix))
                }
            }

            if (!shouldUseActiveUpdatesBeforeCurrent()) {
                LocationDiagnostics.log(LocationDiagnostics.phaseMessage("active-updates", "start-after-current"))
                collectAcceptableUpdate { sawLowAccuracyFix = sawLowAccuracyFix || it }?.let { fix ->
                    return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
                }
            } else {
                LocationDiagnostics.log(LocationDiagnostics.phaseMessage("current", "start-after-active"))
                provider.currentLocation()?.let { fix ->
                    logFixCandidate("current", fix)
                    if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                        return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
                    }
                    sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(listOf(fix))
                }
            }

            LocationDiagnostics.log(LocationDiagnostics.phaseMessage("last-known-after", "start"))
            val lastKnownAfter = boundedLastKnownLocations()
            logFixCandidates("last-known-after", lastKnownAfter)
            bestAcceptableLastKnown(lastKnownAfter)?.let { fix ->
                return@withTimeoutOrNull LocationResult.success(fix.toStationLocation())
            }
            sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(lastKnownAfter)

            if (sawLowAccuracyFix) {
                LocationResult(LocationResultState.LOW_ACCURACY_LOCATION)
            } else {
                val sawLastKnown = lastKnownBefore.isNotEmpty() || lastKnownAfter.isNotEmpty()
                if (sawLastKnown) {
                    LocationResult(LocationResultState.NO_FRESH_LOCATION)
                } else {
                    LocationResult(LocationResultState.NO_LAST_KNOWN_LOCATION)
                }
            }
        }
        return result ?: if (sawLowAccuracyFix) {
            LocationResult(LocationResultState.LOW_ACCURACY_LOCATION)
        } else {
            LocationResult(LocationResultState.TIMEOUT)
        }
    }

    private suspend fun boundedLastKnownLocations(): List<LocationFix> {
        return withTimeoutOrNull(LocationAcquisitionPolicy.LastKnownReadTimeoutMillis) {
            provider.lastKnownLocations()
        } ?: emptyList()
    }

    private suspend fun collectAcceptableUpdate(onLowAccuracy: (Boolean) -> Unit): LocationFix? {
        return updateProvider.locationUpdates().firstOrNull { fix ->
            logFixCandidate("active-updates", fix)
            if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                true
            } else {
                onLowAccuracy(LocationFixQualityPolicy.hasLowAccuracyFix(listOf(fix)))
                false
            }
        }
    }

    private fun shouldUseActiveUpdatesBeforeCurrent(): Boolean {
        return (provider as? ActiveLocationAcquisitionPreference)
            ?.preferActiveUpdatesBeforeCurrent == true
    }

    private fun bestAcceptableLastKnown(fixes: List<LocationFix>): LocationFix? {
        if (fixes.isEmpty()) return null
        return LocationFixQualityPolicy.bestAcceptableLastKnown(fixes, elapsedRealtimeMillis())
    }

    private fun logFixCandidates(phase: String, fixes: List<LocationFix>) {
        fixes.forEach { fix -> logFixCandidate(phase, fix) }
    }

    private fun logFixCandidate(phase: String, fix: LocationFix) {
        val accepted = if (phase.startsWith("last-known")) {
            LocationFixQualityPolicy.isAcceptableLastKnown(fix, elapsedRealtimeMillis())
        } else {
            LocationFixQualityPolicy.isAcceptableCurrent(fix)
        }
        LocationDiagnostics.log(
            LocationDiagnostics.fixCandidateMessage(
                phase = phase,
                fix = fix,
                nowElapsedRealtimeMillis = elapsedRealtimeMillis(),
                accepted = accepted,
                reason = fixReason(fix, accepted, phase)
            )
        )
    }

    private fun fixReason(fix: LocationFix, accepted: Boolean, phase: String): String {
        if (accepted) return "accepted"
        if (phase.startsWith("last-known")) {
            val ageMillis = elapsedRealtimeMillis() - fix.elapsedRealtimeMillis
            if (ageMillis !in 0L..LocationFixQualityPolicy.MAX_LAST_KNOWN_AGE_MILLIS) {
                return "stale"
            }
        }
        return if (LocationFixQualityPolicy.hasLowAccuracyFix(listOf(fix))) {
            "low-accuracy"
        } else {
            "policy"
        }
    }
}
