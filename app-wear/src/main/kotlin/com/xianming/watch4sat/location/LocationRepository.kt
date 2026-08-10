package com.xianming.watch4sat.location

import com.xianming.watch4sat.domain.model.StationLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class LocationRepository(
    private val provider: LocationProvider,
    private val elapsedRealtimeMillis: () -> Long = LocationClock::elapsedRealtimeMillis
) {
    suspend fun resolveCurrentLocation(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): LocationResult {
        return try {
            when {
                !provider.hasLocationPermission -> LocationResult(LocationResultState.NO_PERMISSION)
                !provider.hasEnabledLocationProvider() -> LocationResult(LocationResultState.LOCATION_PROVIDER_DISABLED)
                provider is LocationUpdateProvider -> LocationAcquisitionSession(
                    provider = provider,
                    updateProvider = provider,
                    elapsedRealtimeMillis = elapsedRealtimeMillis
                ).acquire(timeoutMillis)
                else -> resolvePermittedLocation(timeoutMillis)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            LocationResult(state = LocationResultState.ERROR)
        }
    }

    private suspend fun resolvePermittedLocation(timeoutMillis: Long): LocationResult {
        return withTimeoutOrNull(timeoutMillis) {
            resolvePermittedLocationWithinBudget()
        } ?: LocationResult(LocationResultState.TIMEOUT)
    }

    private suspend fun resolvePermittedLocationWithinBudget(): LocationResult {
        var sawLowAccuracyFix = false
        val lastKnownBefore = boundedLastKnownLocations()
        bestAcceptableLastKnown(lastKnownBefore)
            ?.let { fix ->
                return LocationResult.success(fix.toStationLocation())
            }
        sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(lastKnownBefore)

        provider.currentLocation()?.let { fix ->
            if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                return LocationResult.success(fix.toStationLocation())
            }
            sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(listOf(fix))
        }

        val lastKnownAfter = boundedLastKnownLocations()
        bestAcceptableLastKnown(lastKnownAfter)
            ?.let { fix ->
                return LocationResult.success(fix.toStationLocation())
            }
        sawLowAccuracyFix = sawLowAccuracyFix || LocationFixQualityPolicy.hasLowAccuracyFix(lastKnownAfter)

        if (sawLowAccuracyFix) {
            return LocationResult(LocationResultState.LOW_ACCURACY_LOCATION)
        }

        val sawLastKnown = lastKnownBefore.isNotEmpty() || lastKnownAfter.isNotEmpty()
        return if (!sawLastKnown) {
            LocationResult(LocationResultState.NO_LAST_KNOWN_LOCATION)
        } else {
            LocationResult(LocationResultState.NO_FRESH_LOCATION)
        }
    }

    private suspend fun boundedLastKnownLocations(): List<LocationFix> {
        return withTimeoutOrNull(LocationAcquisitionPolicy.LastKnownReadTimeoutMillis) {
            provider.lastKnownLocations()
        } ?: emptyList()
    }

    private fun bestAcceptableLastKnown(fixes: List<LocationFix>): LocationFix? {
        if (fixes.isEmpty()) return null
        return LocationFixQualityPolicy.bestAcceptableLastKnown(fixes, elapsedRealtimeMillis())
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = LocationAcquisitionPolicy.RepositoryTimeoutMillis
        const val MAX_LAST_KNOWN_AGE_MILLIS = LocationFixQualityPolicy.MAX_LAST_KNOWN_AGE_MILLIS
    }
}

data class LocationResult(
    val state: LocationResultState,
    val stationLocation: StationLocation? = null
) {
    companion object {
        fun success(stationLocation: StationLocation): LocationResult {
            return LocationResult(
                state = LocationResultState.SUCCESS,
                stationLocation = stationLocation
            )
        }
    }
}

enum class LocationResultState {
    NO_PERMISSION,
    LOCATION_PROVIDER_DISABLED,
    NO_LAST_KNOWN_LOCATION,
    NO_FRESH_LOCATION,
    LOW_ACCURACY_LOCATION,
    TIMEOUT,
    SUCCESS,
    ERROR
}
