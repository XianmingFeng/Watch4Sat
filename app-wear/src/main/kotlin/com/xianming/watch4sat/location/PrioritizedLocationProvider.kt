package com.xianming.watch4sat.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withTimeoutOrNull

class PrioritizedLocationProvider(
    private val preferred: LocationProvider,
    private val fallback: LocationProvider,
    private val preferredCurrentTimeoutMillis: Long = DEFAULT_PREFERRED_CURRENT_TIMEOUT_MILLIS,
    private val preferredLastKnownTimeoutMillis: Long = DEFAULT_PREFERRED_LAST_KNOWN_TIMEOUT_MILLIS,
    private val preferredUpdateFallbackGraceMillis: Long = DEFAULT_PREFERRED_UPDATE_FALLBACK_GRACE_MILLIS
) : LocationProvider, LocationUpdateProvider, ActiveLocationAcquisitionPreference {

    override val preferActiveUpdatesBeforeCurrent: Boolean
        get() = !preferred.canReadLocation() &&
            (fallback as? ActiveLocationAcquisitionPreference)?.preferActiveUpdatesBeforeCurrent == true

    override val hasLocationPermission: Boolean
        get() = preferred.hasLocationPermission || fallback.hasLocationPermission

    override fun hasEnabledLocationProvider(): Boolean {
        return preferred.canReadLocation() || fallback.canReadLocation()
    }

    override suspend fun lastKnownLocations(): List<LocationFix> {
        val preferredLocations = if (preferred.canReadLocation()) {
            preferred.readLastKnownWithinTimeout()
        } else {
            emptyList()
        }
        return preferredLocations + fallback.lastKnownLocationsSafely()
    }

    override suspend fun currentLocation(): LocationFix? {
        if (preferred.canReadLocation()) {
            val preferredFix = preferred.readCurrentWithinTimeout()
            if (preferredFix != null && LocationFixQualityPolicy.isAcceptableCurrent(preferredFix)) {
                return preferredFix
            }
            return fallback.currentLocationSafely() ?: preferredFix
        }
        return fallback.currentLocationSafely()
    }

    override fun locationUpdates(): Flow<LocationFix> {
        val preferredUpdates = preferred as? LocationUpdateProvider
        val fallbackUpdates = fallback as? LocationUpdateProvider
        val canReadPreferred = preferred.canReadLocation() && preferredUpdates != null
        val canReadFallback = fallback.canReadLocation() && fallbackUpdates != null
        return when {
            canReadPreferred && canReadFallback -> flow {
                var sawAcceptablePreferredFix = false
                withTimeoutOrNull(preferredUpdateFallbackGraceMillis) {
                    preferredUpdates.locationUpdates()
                        .catch { throwable ->
                            if (throwable is CancellationException) throw throwable
                        }
                        .collect { fix ->
                            if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                                sawAcceptablePreferredFix = true
                            }
                            emit(fix)
                        }
                }
                if (sawAcceptablePreferredFix) return@flow
                fallbackUpdates.locationUpdates().collect { fix -> emit(fix) }
            }
            canReadPreferred -> preferredUpdates.locationUpdates()
            canReadFallback -> fallbackUpdates.locationUpdates()
            else -> emptyFlow()
        }
    }

    private fun LocationProvider.canReadLocation(): Boolean {
        return hasLocationPermission && hasEnabledLocationProvider()
    }

    private suspend fun LocationProvider.readCurrentWithinTimeout(): LocationFix? {
        return try {
            withTimeoutOrNull(preferredCurrentTimeoutMillis) {
                currentLocation()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun LocationProvider.readLastKnownWithinTimeout(): List<LocationFix> {
        return try {
            withTimeoutOrNull(preferredLastKnownTimeoutMillis) {
                lastKnownLocations()
            } ?: emptyList()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private suspend fun LocationProvider.currentLocationSafely(): LocationFix? {
        return try {
            if (canReadLocation()) currentLocation() else null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun LocationProvider.lastKnownLocationsSafely(): List<LocationFix> {
        return try {
            if (canReadLocation()) lastKnownLocations() else emptyList()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        const val DEFAULT_PREFERRED_CURRENT_TIMEOUT_MILLIS = LocationAcquisitionPolicy.FusedCurrentTimeoutMillis
        const val DEFAULT_PREFERRED_LAST_KNOWN_TIMEOUT_MILLIS = LocationAcquisitionPolicy.FusedLastKnownTimeoutMillis
        const val DEFAULT_PREFERRED_UPDATE_FALLBACK_GRACE_MILLIS =
            LocationAcquisitionPolicy.FusedUpdateFallbackGraceMillis
    }
}
