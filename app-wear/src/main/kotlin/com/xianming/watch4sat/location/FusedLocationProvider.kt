package com.xianming.watch4sat.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

class FusedLocationProvider(
    private val context: Context,
    private val permissionChecker: () -> Boolean,
    private val clientFactory: (Context) -> FusedLocationProviderClient = LocationServices::getFusedLocationProviderClient,
    private val googlePlayServicesAvailability: (Context) -> Boolean = { appContext ->
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS
    },
    private val systemLocationEnabled: (Context) -> Boolean = { appContext ->
        runCatching {
            val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        }.getOrDefault(false)
    },
    private val callbackExecutor: Executor = DirectExecutor
) : LocationProvider, LocationUpdateProvider {

    override val hasLocationPermission: Boolean
        get() = permissionChecker()

    override fun hasEnabledLocationProvider(): Boolean {
        val hasGooglePlayServicesPackage = context.hasGooglePlayServicesPackage()
        val googlePlayServicesAvailable = googlePlayServicesAvailability(context)
        val systemLocationEnabled = systemLocationEnabled(context)
        LocationDiagnostics.log(
            LocationDiagnostics.providerStateMessage(
                hasGooglePlayServicesPackage = hasGooglePlayServicesPackage,
                googlePlayServicesAvailable = googlePlayServicesAvailable,
                systemLocationEnabled = systemLocationEnabled,
                allProviders = emptyList(),
                enabledProviders = if (systemLocationEnabled) listOf("fused") else emptyList()
            )
        )
        return FusedLocationProviderPolicy.canUseFusedLocation(
            hasLocationPermission = hasLocationPermission,
            hasGooglePlayServicesPackage = hasGooglePlayServicesPackage,
            googlePlayServicesAvailable = googlePlayServicesAvailable,
            systemLocationEnabled = systemLocationEnabled
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocations(): List<LocationFix> {
        if (!hasEnabledLocationProvider()) return emptyList()
        return suspendCancellableCoroutine { continuation ->
            val task = runCatching { clientFactory(context).lastLocation }
                .getOrElse {
                    continuation.resume(emptyList())
                    return@suspendCancellableCoroutine
                }
            task.addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(listOfNotNull(location?.toLocationFix()))
                }
            }
            task.addOnFailureListener {
                if (continuation.isActive) continuation.resume(emptyList())
            }
            task.addOnCanceledListener {
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): LocationFix? {
        if (!hasEnabledLocationProvider()) return null
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
            val task = runCatching {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(LocationAcquisitionPolicy.FusedMaxUpdateAgeMillis)
                    .setDurationMillis(LocationAcquisitionPolicy.FusedCurrentTimeoutMillis)
                    .build()
                clientFactory(context).getCurrentLocation(
                    request,
                    cancellationTokenSource.token
                )
            }.getOrElse {
                if (continuation.isActive) continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            task.addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location?.toLocationFix())
            }
            task.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
            task.addOnCanceledListener {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<LocationFix> = callbackFlow {
        if (!hasEnabledLocationProvider()) {
            close()
            return@callbackFlow
        }
        val client = runCatching { clientFactory(context) }.getOrElse { throwable ->
            close(throwable)
            return@callbackFlow
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LocationAcquisitionPolicy.ActiveUpdateIntervalMillis
        )
            .setMinUpdateIntervalMillis(LocationAcquisitionPolicy.ActiveUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(LocationAcquisitionPolicy.ActiveMinDistanceMeters)
            .setMaxUpdateDelayMillis(0L)
            .setDurationMillis(LocationAcquisitionPolicy.ActiveSessionTimeoutMillis)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location.toLocationFix())
                }
            }
        }
        runCatching {
            client.requestLocationUpdates(request, callbackExecutor, callback)
        }.onSuccess { task ->
            LocationDiagnostics.log(
                LocationDiagnostics.activeRegistrationMessage(
                    providers = listOf("fused"),
                    event = "registered"
                )
            )
            task.addOnFailureListener { throwable -> close(throwable) }
            task.addOnCanceledListener { close() }
        }.onFailure { throwable ->
            close(throwable)
        }
        awaitClose {
            runCatching {
                client.removeLocationUpdates(callback)
            }
            LocationDiagnostics.log(
                LocationDiagnostics.activeRegistrationMessage(
                    providers = listOf("fused"),
                    event = "removed"
                )
            )
        }
    }

    private fun Context.hasGooglePlayServicesPackage(): Boolean {
        return runCatching {
            packageManager.getPackageInfo(GOOGLE_PLAY_SERVICES_PACKAGE, 0)
        }.isSuccess
    }

    private fun Location.toLocationFix(): LocationFix {
        return LocationFix(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitude,
            timestampMillis = time,
            elapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000L,
            provider = LocationFixProvider.FUSED,
            accuracyMeters = accuracy.takeIf { hasAccuracy() }
        )
    }

    private companion object {
        const val GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms"
    }

    private object DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }
}
