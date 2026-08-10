package com.xianming.watch4sat.domain.model

data class OrbitalPosition(
    val timeMillis: Long,
    val azimuthDegrees: Double,
    val elevationDegrees: Double,
    val rangeRateKmPerSecond: Double,
    val aboveHorizon: Boolean = elevationDegrees >= 0.0,
    val latitudeDegrees: Double? = null,
    val longitudeDegrees: Double? = null,
    val altitudeKm: Double? = null,
    val slantRangeKm: Double? = null
)
