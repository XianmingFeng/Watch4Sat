package com.xianming.watch4sat.location

object FusedLocationProviderPolicy {
    fun canUseFusedLocation(
        hasLocationPermission: Boolean,
        hasGooglePlayServicesPackage: Boolean,
        googlePlayServicesAvailable: Boolean,
        systemLocationEnabled: Boolean
    ): Boolean {
        return hasLocationPermission &&
            hasGooglePlayServicesPackage &&
            googlePlayServicesAvailable &&
            systemLocationEnabled
    }
}
