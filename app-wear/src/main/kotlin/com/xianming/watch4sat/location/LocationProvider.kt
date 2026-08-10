package com.xianming.watch4sat.location

interface LocationProvider {
    val hasLocationPermission: Boolean

    fun hasEnabledLocationProvider(): Boolean

    suspend fun lastKnownLocations(): List<LocationFix>

    suspend fun currentLocation(): LocationFix?
}
