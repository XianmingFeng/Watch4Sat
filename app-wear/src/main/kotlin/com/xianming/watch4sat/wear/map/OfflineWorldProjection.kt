package com.xianming.watch4sat.wear.map

import kotlin.math.max
import kotlin.math.min

data class OfflineMapLocation(
    val latitude: Double,
    val longitude: Double
)

data class OfflineScreenPoint(
    val x: Float,
    val y: Float
)

class OfflineWorldProjection(
    private val widthPx: Float,
    private val heightPx: Float,
    private val centerLatitude: Double,
    val centerLongitude: Double,
    private val zoom: Float
) {
    private val safeZoom = zoom.coerceAtLeast(0.25f)
    private val pixelsPerDegree = min(widthPx / 360f, heightPx / 180f) * safeZoom

    fun project(latitude: Double, longitude: Double): OfflineScreenPoint {
        val deltaLongitude = wrapLongitude(longitude - centerLongitude)
        return projectDelta(latitude = latitude, deltaLongitude = deltaLongitude)
    }

    fun projectUnwrapped(latitude: Double, longitude: Double): OfflineScreenPoint {
        val deltaLongitude = longitude - centerLongitude
        return projectDelta(latitude = latitude, deltaLongitude = deltaLongitude)
    }

    private fun projectDelta(latitude: Double, deltaLongitude: Double): OfflineScreenPoint {
        val deltaLatitude = latitude - centerLatitude
        return OfflineScreenPoint(
            x = widthPx / 2f + (deltaLongitude * pixelsPerDegree).toFloat(),
            y = heightPx / 2f - (deltaLatitude * pixelsPerDegree).toFloat()
        )
    }

    fun unproject(x: Float, y: Float): OfflineMapLocation {
        val longitude = centerLongitude + ((x - widthPx / 2f) / pixelsPerDegree)
        val latitude = centerLatitude - ((y - heightPx / 2f) / pixelsPerDegree)
        return OfflineMapLocation(
            latitude = clampLatitude(latitude),
            longitude = wrapLongitude(longitude)
        )
    }

    fun centerAfterDrag(deltaXPx: Float, deltaYPx: Float): OfflineMapLocation {
        return OfflineMapLocation(
            latitude = clampLatitude(centerLatitude + deltaYPx / pixelsPerDegree),
            longitude = wrapLongitude(centerLongitude - deltaXPx / pixelsPerDegree)
        )
    }

    companion object {
        fun wrapLongitude(value: Double): Double {
            var longitude = value
            while (longitude > 180.0) longitude -= 360.0
            while (longitude < -180.0) longitude += 360.0
            return longitude
        }

        fun clampLatitude(value: Double): Double {
            return min(85.0, max(-85.0, value))
        }
    }
}
