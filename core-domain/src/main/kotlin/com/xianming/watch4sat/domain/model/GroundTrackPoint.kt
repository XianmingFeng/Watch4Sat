package com.xianming.watch4sat.domain.model

data class GroundTrackPoint(
    val timeMillis: Long,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeKm: Double? = null
)
