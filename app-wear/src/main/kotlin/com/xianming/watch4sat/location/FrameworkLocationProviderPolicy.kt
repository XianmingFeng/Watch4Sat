package com.xianming.watch4sat.location

object FrameworkLocationProviderPolicy {
    data class CurrentRequest(
        val providerName: String,
        val fixProvider: LocationFixProvider,
        val timeoutMillis: Long
    )

    const val GpsProviderName: String = "gps"
    const val NetworkProviderName: String = "network"
    const val PassiveProviderName: String = "passive"
    const val GpsCurrentTimeoutMillis: Long = LocationAcquisitionPolicy.FrameworkGpsCurrentTimeoutMillis
    const val NetworkCurrentTimeoutMillis: Long = LocationAcquisitionPolicy.FrameworkNetworkCurrentTimeoutMillis

    val currentRequests: List<CurrentRequest> = listOf(
        CurrentRequest(
            providerName = NetworkProviderName,
            fixProvider = LocationFixProvider.NETWORK,
            timeoutMillis = NetworkCurrentTimeoutMillis
        ),
        CurrentRequest(
            providerName = GpsProviderName,
            fixProvider = LocationFixProvider.GPS,
            timeoutMillis = GpsCurrentTimeoutMillis
        )
    )

    val lastKnownProviders: List<Pair<String, LocationFixProvider>> = listOf(
        GpsProviderName to LocationFixProvider.GPS,
        NetworkProviderName to LocationFixProvider.NETWORK,
        PassiveProviderName to LocationFixProvider.PASSIVE
    )
}
