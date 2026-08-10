package com.xianming.watch4sat.domain.model

data class StationLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val qthLocator: String? = null,
    val timestampMillis: Long = 0L,
    val source: LocationSource = LocationSource.MANUAL_QTH,
    val accuracyMeters: Float? = null
)

enum class LocationSource {
    GPS,
    NETWORK,
    FUSED,
    MANUAL_QTH,
    MANUAL_COORDINATES
}
