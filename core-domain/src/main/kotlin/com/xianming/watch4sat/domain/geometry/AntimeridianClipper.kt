package com.xianming.watch4sat.domain.geometry

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

enum class GeographicPole {
    NORTH,
    SOUTH
}

data class AntimeridianPolygonGeometry(
    val fillPolygons: List<List<GroundTrackPoint>>,
    val outlineSegments: List<List<GroundTrackPoint>>
) {
    companion object {
        val Empty = AntimeridianPolygonGeometry(
            fillPolygons = emptyList(),
            outlineSegments = emptyList()
        )
    }
}

object AntimeridianClipper {

    fun splitPolyline(
        points: List<GroundTrackPoint>,
        centerLongitudeDegrees: Double = 0.0
    ): List<List<GroundTrackPoint>> {
        if (points.size < MinimumLinePoints || !points.all { it.hasFiniteCoordinates() }) {
            return emptyList()
        }

        val segments = mutableListOf<List<GroundTrackPoint>>()
        var previous = points.first().inLongitudeWindow(centerLongitudeDegrees)
        var current = mutableListOf(previous)

        points.drop(1).forEach { sourceNext ->
            val next = sourceNext.inLongitudeWindow(centerLongitudeDegrees)
            val longitudeDelta = next.longitudeDegrees - previous.longitudeDegrees
            if (abs(longitudeDelta) > HalfWorldDegrees) {
                val crossesEast = longitudeDelta < -HalfWorldDegrees
                val unwrappedNextLongitude = next.longitudeDegrees + if (crossesEast) {
                    FullWorldDegrees
                } else {
                    -FullWorldDegrees
                }
                val outgoingLongitude = centerLongitudeDegrees + if (crossesEast) {
                    HalfWorldDegrees
                } else {
                    -HalfWorldDegrees
                }
                val incomingLongitude = outgoingLongitude + if (crossesEast) {
                    -FullWorldDegrees
                } else {
                    FullWorldDegrees
                }
                val fraction = ((outgoingLongitude - previous.longitudeDegrees) /
                    (unwrappedNextLongitude - previous.longitudeDegrees)).coerceIn(0.0, 1.0)
                val seamPoint = interpolate(previous, next, fraction)
                current.appendDistinct(seamPoint.copy(longitudeDegrees = outgoingLongitude))
                if (current.size >= MinimumLinePoints) segments += current
                current = mutableListOf(seamPoint.copy(longitudeDegrees = incomingLongitude))
            }
            current.appendDistinct(next)
            previous = next
        }

        if (current.size >= MinimumLinePoints) segments += current
        return segments
    }

    fun clipClosedRing(
        points: List<GroundTrackPoint>,
        centerLongitudeDegrees: Double = 0.0,
        enclosedPole: GeographicPole? = null
    ): AntimeridianPolygonGeometry {
        val ring = points.withoutDuplicateClosure()
        if (ring.size < MinimumPolygonPoints || !ring.all { it.hasFiniteCoordinates() }) {
            return AntimeridianPolygonGeometry.Empty
        }

        val outlineSegments = splitClosedOutline(ring, centerLongitudeDegrees)
        val unwrapped = unwrapRing(ring, centerLongitudeDegrees)
        val closingLongitude = longitudeNearest(
            longitude = unwrapped.first().longitudeDegrees,
            referenceLongitude = unwrapped.last().longitudeDegrees
        )
        val windingDegrees = closingLongitude - unwrapped.first().longitudeDegrees
        val fillSource = when {
            abs(windingDegrees) <= HalfWorldDegrees -> unwrapped
            enclosedPole != null -> {
                val poleLatitude = if (enclosedPole == GeographicPole.NORTH) 90.0 else -90.0
                val closingPoint = unwrapped.first().copy(longitudeDegrees = closingLongitude)
                unwrapped +
                    closingPoint +
                    closingPoint.copy(latitudeDegrees = poleLatitude) +
                    unwrapped.first().copy(latitudeDegrees = poleLatitude)
            }
            else -> emptyList()
        }
        if (fillSource.size < MinimumPolygonPoints) {
            return AntimeridianPolygonGeometry(
                fillPolygons = emptyList(),
                outlineSegments = outlineSegments
            )
        }

        val leftBoundary = centerLongitudeDegrees - HalfWorldDegrees
        val rightBoundary = centerLongitudeDegrees + HalfWorldDegrees
        val minimumLongitude = fillSource.minOf { it.longitudeDegrees }
        val maximumLongitude = fillSource.maxOf { it.longitudeDegrees }
        val firstWorldCopy = ceil((leftBoundary - maximumLongitude) / FullWorldDegrees).toInt()
        val lastWorldCopy = floor((rightBoundary - minimumLongitude) / FullWorldDegrees).toInt()
        val fillPolygons = (firstWorldCopy..lastWorldCopy).mapNotNull { worldCopy ->
            val offset = worldCopy * FullWorldDegrees
            val shifted = fillSource.map { point ->
                point.copy(longitudeDegrees = point.longitudeDegrees + offset)
            }
            clipPolygonToLongitudeWindow(
                points = shifted,
                leftBoundary = leftBoundary,
                rightBoundary = rightBoundary
            ).takeIf { polygon ->
                polygon.size >= MinimumPolygonPoints && abs(polygon.signedArea()) > AreaEpsilon
            }
        }

        return AntimeridianPolygonGeometry(
            fillPolygons = fillPolygons,
            outlineSegments = outlineSegments
        )
    }

    private fun splitClosedOutline(
        ring: List<GroundTrackPoint>,
        centerLongitudeDegrees: Double
    ): List<List<GroundTrackPoint>> {
        val segments = splitPolyline(
            points = ring + ring.first(),
            centerLongitudeDegrees = centerLongitudeDegrees
        ).toMutableList()
        if (segments.size <= 1) return segments

        val first = segments.first()
        val last = segments.last()
        if (!last.last().sameLocationAs(first.first())) return segments

        val merged = (last + first.drop(1)).withoutAdjacentDuplicates()
        segments.removeAt(segments.lastIndex)
        segments.removeAt(0)
        if (merged.size >= MinimumLinePoints) segments += merged
        return segments
    }

    private fun unwrapRing(
        ring: List<GroundTrackPoint>,
        centerLongitudeDegrees: Double
    ): List<GroundTrackPoint> {
        val first = ring.first().inLongitudeWindow(centerLongitudeDegrees)
        val unwrapped = mutableListOf(first)
        var previousLongitude = first.longitudeDegrees
        ring.drop(1).forEach { source ->
            val normalized = source.inLongitudeWindow(centerLongitudeDegrees)
            val longitude = longitudeNearest(normalized.longitudeDegrees, previousLongitude)
            unwrapped += normalized.copy(longitudeDegrees = longitude)
            previousLongitude = longitude
        }
        return unwrapped
    }

    private fun clipPolygonToLongitudeWindow(
        points: List<GroundTrackPoint>,
        leftBoundary: Double,
        rightBoundary: Double
    ): List<GroundTrackPoint> {
        return clipAgainstLongitude(points, leftBoundary, keepGreater = true)
            .let { clipAgainstLongitude(it, rightBoundary, keepGreater = false) }
            .withoutAdjacentDuplicates()
            .withoutDuplicateClosure()
    }

    private fun clipAgainstLongitude(
        points: List<GroundTrackPoint>,
        boundary: Double,
        keepGreater: Boolean
    ): List<GroundTrackPoint> {
        if (points.isEmpty()) return emptyList()
        val output = mutableListOf<GroundTrackPoint>()
        var previous = points.last()
        var previousInside = previous.isInside(boundary, keepGreater)
        points.forEach { current ->
            val currentInside = current.isInside(boundary, keepGreater)
            when {
                currentInside && !previousInside -> output.appendDistinct(
                    intersectionAtLongitude(previous, current, boundary)
                )
                !currentInside && previousInside -> output.appendDistinct(
                    intersectionAtLongitude(previous, current, boundary)
                )
            }
            if (currentInside) output.appendDistinct(current)
            previous = current
            previousInside = currentInside
        }
        return output
    }

    private fun GroundTrackPoint.isInside(boundary: Double, keepGreater: Boolean): Boolean {
        return if (keepGreater) {
            longitudeDegrees >= boundary - CoordinateEpsilon
        } else {
            longitudeDegrees <= boundary + CoordinateEpsilon
        }
    }

    private fun intersectionAtLongitude(
        first: GroundTrackPoint,
        second: GroundTrackPoint,
        longitude: Double
    ): GroundTrackPoint {
        val delta = second.longitudeDegrees - first.longitudeDegrees
        if (abs(delta) <= CoordinateEpsilon) return first.copy(longitudeDegrees = longitude)
        val fraction = ((longitude - first.longitudeDegrees) / delta).coerceIn(0.0, 1.0)
        return interpolate(first, second, fraction).copy(longitudeDegrees = longitude)
    }

    private fun interpolate(
        first: GroundTrackPoint,
        second: GroundTrackPoint,
        fraction: Double
    ): GroundTrackPoint {
        val altitude = if (first.altitudeKm != null && second.altitudeKm != null) {
            first.altitudeKm + (second.altitudeKm - first.altitudeKm) * fraction
        } else {
            null
        }
        return GroundTrackPoint(
            timeMillis = (first.timeMillis + (second.timeMillis - first.timeMillis) * fraction).roundToLong(),
            latitudeDegrees = first.latitudeDegrees +
                (second.latitudeDegrees - first.latitudeDegrees) * fraction,
            longitudeDegrees = first.longitudeDegrees +
                (second.longitudeDegrees - first.longitudeDegrees) * fraction,
            altitudeKm = altitude
        )
    }

    private fun GroundTrackPoint.inLongitudeWindow(centerLongitudeDegrees: Double): GroundTrackPoint {
        val leftBoundary = centerLongitudeDegrees - HalfWorldDegrees
        val rightBoundary = centerLongitudeDegrees + HalfWorldDegrees
        var longitude = longitudeDegrees
        while (longitude > rightBoundary) longitude -= FullWorldDegrees
        while (longitude < leftBoundary) longitude += FullWorldDegrees
        if (abs(longitude - leftBoundary) <= CoordinateEpsilon) {
            longitude = rightBoundary
        }
        return copy(longitudeDegrees = longitude)
    }

    private fun longitudeNearest(longitude: Double, referenceLongitude: Double): Double {
        var value = longitude
        while (value - referenceLongitude > HalfWorldDegrees) value -= FullWorldDegrees
        while (value - referenceLongitude < -HalfWorldDegrees) value += FullWorldDegrees
        return value
    }

    private fun MutableList<GroundTrackPoint>.appendDistinct(point: GroundTrackPoint) {
        if (lastOrNull()?.sameLocationAs(point) != true) add(point)
    }

    private fun List<GroundTrackPoint>.withoutAdjacentDuplicates(): List<GroundTrackPoint> {
        val result = mutableListOf<GroundTrackPoint>()
        forEach { result.appendDistinct(it) }
        return result
    }

    private fun List<GroundTrackPoint>.withoutDuplicateClosure(): List<GroundTrackPoint> {
        return if (size > 1 && first().sameLocationAs(last())) dropLast(1) else this
    }

    private fun GroundTrackPoint.sameLocationAs(other: GroundTrackPoint): Boolean {
        return abs(latitudeDegrees - other.latitudeDegrees) <= CoordinateEpsilon &&
            abs(longitudeDegrees - other.longitudeDegrees) <= CoordinateEpsilon
    }

    private fun GroundTrackPoint.hasFiniteCoordinates(): Boolean {
        return latitudeDegrees.isFinite() && longitudeDegrees.isFinite() &&
            latitudeDegrees in -90.0..90.0
    }

    private fun List<GroundTrackPoint>.signedArea(): Double {
        return indices.sumOf { index ->
            val current = this[index]
            val next = this[(index + 1) % size]
            current.longitudeDegrees * next.latitudeDegrees -
                next.longitudeDegrees * current.latitudeDegrees
        } / 2.0
    }

    private const val MinimumLinePoints = 2
    private const val MinimumPolygonPoints = 3
    private const val HalfWorldDegrees = 180.0
    private const val FullWorldDegrees = 360.0
    private const val CoordinateEpsilon = 1e-9
    private const val AreaEpsilon = 1e-9
}
