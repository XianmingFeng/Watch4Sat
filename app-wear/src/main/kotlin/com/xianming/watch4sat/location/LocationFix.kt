package com.xianming.watch4sat.location

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val timestampMillis: Long = 0L,
    val elapsedRealtimeMillis: Long,
    val provider: LocationFixProvider = LocationFixProvider.GPS,
    val accuracyMeters: Float? = null
)

enum class LocationFixProvider {
    FUSED,
    GPS,
    NETWORK,
    PASSIVE
}
