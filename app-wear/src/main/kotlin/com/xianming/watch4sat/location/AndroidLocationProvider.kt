package com.xianming.watch4sat.location

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val locationManager: LocationManager,
    private val permissionChecker: () -> Boolean,
    private val callbackExecutor: Executor = DirectExecutor
) : LocationProvider, LocationUpdateProvider, ActiveLocationAcquisitionPreference {

    override val preferActiveUpdatesBeforeCurrent: Boolean = true

    override val hasLocationPermission: Boolean
        get() = permissionChecker()

    override fun hasEnabledLocationProvider(): Boolean {
        val allProviders = runCatching { locationManager.allProviders }.getOrDefault(emptyList())
        val enabledProviders = supportedProviders().filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider.name) }.getOrDefault(false)
        }.map { it.name }
        LocationDiagnostics.log(
            LocationDiagnostics.providerStateMessage(
                hasGooglePlayServicesPackage = false,
                googlePlayServicesAvailable = false,
                systemLocationEnabled = enabledProviders.isNotEmpty(),
                allProviders = allProviders,
                enabledProviders = enabledProviders
            )
        )
        return supportedProviders().any { provider ->
            runCatching { locationManager.isProviderEnabled(provider.name) }.getOrDefault(false)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocations(): List<LocationFix> {
        if (!hasLocationPermission) return emptyList()
        return supportedLastKnownProviders().mapNotNull { provider ->
            runCatching {
                locationManager
                    .getLastKnownLocation(provider.name)
                    ?.toLocationFix(provider.fixProvider)
            }.getOrNull()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): LocationFix? {
        if (!hasLocationPermission) return null
        var fallbackFix: LocationFix? = null
        supportedCurrentProviders().forEach { provider ->
            if (runCatching { locationManager.isProviderEnabled(provider.name) }.getOrDefault(false)) {
                withTimeoutOrNull(provider.timeoutMillis) {
                    currentLocation(provider)
                }?.let { fix ->
                    if (LocationFixQualityPolicy.isAcceptableCurrent(fix)) {
                        return fix
                    }
                    fallbackFix = fallbackFix ?: fix
                }
            }
        }
        return fallbackFix
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<LocationFix> = callbackFlow {
        if (!hasLocationPermission) {
            close()
            return@callbackFlow
        }
        val request = LocationRequestCompat.Builder(LocationAcquisitionPolicy.ActiveUpdateIntervalMillis)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(LocationAcquisitionPolicy.ActiveUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(LocationAcquisitionPolicy.ActiveMinDistanceMeters)
            .setMaxUpdateDelayMillis(0L)
            .build()
        val listener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                trySend(location.toLocationFix(fixProviderForName(location.provider)))
            }

            override fun onLocationChanged(locations: MutableList<Location>) {
                locations.forEach { location ->
                    trySend(location.toLocationFix(fixProviderForName(location.provider)))
                }
            }
        }
        val registeredProviders = supportedCurrentProviders().filter { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider.name)) {
                    LocationManagerCompat.requestLocationUpdates(
                        locationManager,
                        provider.name,
                        request,
                        callbackExecutor,
                        listener
                    )
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
        }
        LocationDiagnostics.log(
            LocationDiagnostics.activeRegistrationMessage(
                providers = registeredProviders.map { it.name },
                event = "registered"
            )
        )
        if (registeredProviders.isEmpty()) {
            close()
        }
        awaitClose {
            if (registeredProviders.isNotEmpty()) {
                runCatching {
                    LocationManagerCompat.removeUpdates(locationManager, listener)
                }
                LocationDiagnostics.log(
                    LocationDiagnostics.activeRegistrationMessage(
                        providers = registeredProviders.map { it.name },
                        event = "removed"
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(provider: ProviderSpec): LocationFix? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
            }
            locationManager.getCurrentLocation(
                provider.name,
                cancellationSignal,
                callbackExecutor
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location?.toLocationFix(provider.fixProvider))
                }
            }
        }
    }

    private fun supportedProviders(): List<ProviderSpec> {
        return supportedLastKnownProviders()
    }

    private fun supportedCurrentProviders(): List<ProviderSpec> {
        return FrameworkLocationProviderPolicy.currentRequests
            .map { request ->
                ProviderSpec(
                    name = request.providerName,
                    fixProvider = request.fixProvider,
                    timeoutMillis = request.timeoutMillis
                )
            }
            .filter { provider ->
                locationManager.allProviders.contains(provider.name)
            }
    }

    private fun supportedLastKnownProviders(): List<ProviderSpec> {
        return FrameworkLocationProviderPolicy.lastKnownProviders.map { (name, fixProvider) ->
            ProviderSpec(name = name, fixProvider = fixProvider)
        }.filter { provider ->
            locationManager.allProviders.contains(provider.name)
        }
    }

    private fun fixProviderForName(providerName: String?): LocationFixProvider {
        return supportedProviders().firstOrNull { provider ->
            provider.name == providerName
        }?.fixProvider ?: LocationFixProvider.GPS
    }

    private fun Location.toLocationFix(provider: LocationFixProvider): LocationFix {
        return LocationFix(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitude,
            timestampMillis = time,
            elapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000L,
            provider = provider,
            accuracyMeters = accuracy.takeIf { hasAccuracy() }
        )
    }

    private data class ProviderSpec(
        val name: String,
        val fixProvider: LocationFixProvider,
        val timeoutMillis: Long = 0L
    )

    private object DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }
}
