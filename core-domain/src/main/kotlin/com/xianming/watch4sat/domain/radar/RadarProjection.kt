package com.xianming.watch4sat.domain.radar

import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RadarPlotPoint(
    val x: Double,
    val y: Double,
    val radiusFraction: Double
)

object RadarProjection {

    fun project(azimuthDegrees: Double, elevationDegrees: Double): RadarPlotPoint {
        val normalizedAzimuth = normalizeDegrees(azimuthDegrees)
        val clampedElevation = elevationDegrees.coerceIn(0.0, 90.0)
        val radiusFraction = 1.0 - clampedElevation / 90.0
        val radians = normalizedAzimuth / 180.0 * PI
        return RadarPlotPoint(
            x = radiusFraction * sin(radians),
            y = -radiusFraction * cos(radians),
            radiusFraction = radiusFraction
        )
    }

    fun projectRelative(
        azimuthDegrees: Double,
        elevationDegrees: Double,
        referenceAzimuthDegrees: Double
    ): RadarPlotPoint {
        return project(
            azimuthDegrees = normalizeDegrees(azimuthDegrees - referenceAzimuthDegrees),
            elevationDegrees = elevationDegrees
        )
    }

    fun relativeAzimuth(
        azimuthDegrees: Double,
        referenceAzimuthDegrees: Double?
    ): Double {
        return normalizeDegrees(azimuthDegrees - (referenceAzimuthDegrees ?: 0.0))
    }

    fun drawableTrack(points: List<RadarTrackPoint>): List<RadarTrackPoint> {
        return points.filter { point ->
            isSatelliteVisible(
                elevationDegrees = point.elevationDegrees,
                aboveHorizon = point.aboveHorizon
            )
        }
    }

    fun labelMarkers(points: List<RadarTrackPoint>): List<RadarTrackPoint> {
        return points.filter { point ->
            point.label == RadarTrackLabel.AOS || point.label == RadarTrackLabel.LOS
        }
    }

    fun isSatelliteVisible(elevationDegrees: Double, aboveHorizon: Boolean): Boolean {
        return aboveHorizon && elevationDegrees >= 0.0
    }

    private fun normalizeDegrees(degrees: Double): Double {
        return ((degrees % 360.0) + 360.0) % 360.0
    }
}
