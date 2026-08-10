package com.xianming.watch4sat.domain.model

data class RadarTrackPoint(
    val timeMillis: Long,
    val azimuthDegrees: Double,
    val elevationDegrees: Double,
    val aboveHorizon: Boolean,
    val label: RadarTrackLabel = RadarTrackLabel.NONE
)

enum class RadarTrackLabel {
    NONE,
    AOS,
    LOS
}
