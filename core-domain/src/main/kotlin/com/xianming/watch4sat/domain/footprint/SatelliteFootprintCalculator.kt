package com.xianming.watch4sat.domain.footprint

import com.xianming.watch4sat.domain.geometry.AntimeridianPolygonGeometry
import com.xianming.watch4sat.domain.geometry.GeographicPole
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class SatelliteFootprint(
    val ring: List<GroundTrackPoint>,
    val geometry: AntimeridianPolygonGeometry,
    val enclosedPole: GeographicPole?,
    val radiusKm: Double,
    val generatedAtMillis: Long
)

object SatelliteFootprintCalculator {
    private const val EarthRadiusKm = 6378.137
    private const val SampleCount = 96

    fun radiusKmForAltitude(altitudeKm: Double?): Double? {
        val validAltitudeKm = altitudeKm?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val centralAngleRadians = acos(EarthRadiusKm / (EarthRadiusKm + validAltitudeKm))
        return EarthRadiusKm * centralAngleRadians
    }

    fun calculate(
        point: GroundTrackPoint?,
        generatedAtMillis: Long
    ): SatelliteFootprint {
        val altitudeKm = point?.altitudeKm
        val radiusKm = radiusKmForAltitude(altitudeKm)
        if (point == null || radiusKm == null) {
            return SatelliteFootprint(
                ring = emptyList(),
                geometry = AntimeridianPolygonGeometry.Empty,
                enclosedPole = null,
                radiusKm = 0.0,
                generatedAtMillis = generatedAtMillis
            )
        }

        val centralAngleRadians = radiusKm / EarthRadiusKm
        val samples = (0 until SampleCount).map { index ->
            val bearingRadians = 2.0 * PI * index.toDouble() / SampleCount.toDouble()
            point.destinationAt(centralAngleRadians, bearingRadians)
        }
        val enclosedPole = point.enclosedPole(centralAngleRadians)
        val geometry = FootprintRingSegmenter.splitClosedRingAtAntimeridian(
            points = samples,
            enclosedPole = enclosedPole
        )
        return SatelliteFootprint(
            ring = samples,
            geometry = geometry,
            enclosedPole = enclosedPole,
            radiusKm = radiusKm,
            generatedAtMillis = generatedAtMillis
        )
    }

    private fun GroundTrackPoint.enclosedPole(angularDistanceRadians: Double): GeographicPole? {
        val latitudeRadians = latitudeDegrees.toRadians()
        return when {
            angularDistanceRadians >= PI / 2.0 - latitudeRadians -> GeographicPole.NORTH
            angularDistanceRadians >= PI / 2.0 + latitudeRadians -> GeographicPole.SOUTH
            else -> null
        }
    }

    private fun GroundTrackPoint.destinationAt(
        angularDistanceRadians: Double,
        bearingRadians: Double
    ): GroundTrackPoint {
        val startLatitude = latitudeDegrees.toRadians()
        val startLongitude = longitudeDegrees.toRadians()
        val destinationLatitude = asin(
            sin(startLatitude) * cos(angularDistanceRadians) +
                cos(startLatitude) * sin(angularDistanceRadians) * cos(bearingRadians)
        )
        val destinationLongitude = startLongitude + atan2(
            sin(bearingRadians) * sin(angularDistanceRadians) * cos(startLatitude),
            cos(angularDistanceRadians) - sin(startLatitude) * sin(destinationLatitude)
        )
        return GroundTrackPoint(
            timeMillis = timeMillis,
            latitudeDegrees = destinationLatitude.toDegrees().coerceIn(-90.0, 90.0),
            longitudeDegrees = destinationLongitude.toDegrees().normalizeLongitudeDegrees(),
            altitudeKm = altitudeKm
        )
    }

    private fun Double.toRadians(): Double = this / 180.0 * PI

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    private fun Double.normalizeLongitudeDegrees(): Double {
        var value = this
        while (value < -180.0) value += 360.0
        while (value > 180.0) value -= 360.0
        return value
    }
}
