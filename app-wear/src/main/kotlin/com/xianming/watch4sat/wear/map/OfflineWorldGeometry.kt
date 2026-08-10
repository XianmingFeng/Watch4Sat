package com.xianming.watch4sat.wear.map

import com.xianming.watch4sat.domain.geometry.AntimeridianClipper
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import kotlin.math.abs

data class OfflineWorldPolygon(
    val rings: List<List<OfflineMapLocation>>
)

data class OfflineLandPolygonGeometry(
    val rings: List<List<OfflineMapLocation>>,
    val fillWorldCopies: List<List<List<OfflineMapLocation>>>,
    val coastlineWorldCopies: List<List<List<OfflineMapLocation>>>
)

object OfflineWorldGeometry {

    fun prepareLand(polygons: List<OfflineWorldPolygon>): List<OfflineLandPolygonGeometry> {
        return polygons.map { polygon ->
            val rings = polygon.rings.filter { it.size >= MinimumPolygonPoints }
            OfflineLandPolygonGeometry(
                rings = rings,
                fillWorldCopies = WorldCopyOffsets.map { offset ->
                    rings.map { ring -> ring.withLongitudeOffset(offset) }
                },
                coastlineWorldCopies = WorldCopyOffsets.map { offset ->
                    rings.flatMap(::coastlineSegments).map { segment ->
                        segment.withLongitudeOffset(offset)
                    }
                }
            )
        }
    }

    fun trackForProjection(
        points: List<GroundTrackPoint>,
        centerLongitude: Double
    ): List<List<GroundTrackPoint>> {
        return AntimeridianClipper.splitPolyline(
            points = points,
            centerLongitudeDegrees = centerLongitude
        )
    }

    fun isDataBoundaryEdge(
        first: OfflineMapLocation,
        second: OfflineMapLocation
    ): Boolean {
        val followsAntimeridianBoundary =
            abs(abs(first.longitude) - HalfWorldDegrees) <= CoordinateEpsilon &&
                abs(abs(second.longitude) - HalfWorldDegrees) <= CoordinateEpsilon
        val followsPoleBoundary =
            abs(abs(first.latitude) - PoleLatitudeDegrees) <= CoordinateEpsilon &&
                abs(abs(second.latitude) - PoleLatitudeDegrees) <= CoordinateEpsilon
        return followsAntimeridianBoundary || followsPoleBoundary
    }

    private fun coastlineSegments(ring: List<OfflineMapLocation>): List<List<OfflineMapLocation>> {
        val vertices = ring.withoutDuplicateClosure()
        if (vertices.size < MinimumLinePoints) return emptyList()
        val boundaryEdgeIndex = vertices.indices.firstOrNull { index ->
            isDataBoundaryEdge(vertices[index], vertices[(index + 1) % vertices.size])
        }
        if (boundaryEdgeIndex == null) return listOf(vertices + vertices.first())

        val segments = mutableListOf<List<OfflineMapLocation>>()
        val startIndex = (boundaryEdgeIndex + 1) % vertices.size
        var current = mutableListOf(vertices[startIndex])
        repeat(vertices.size) { offset ->
            val index = (startIndex + offset) % vertices.size
            val first = vertices[index]
            val second = vertices[(index + 1) % vertices.size]
            if (isDataBoundaryEdge(first, second)) {
                if (current.size >= MinimumLinePoints) segments += current
                current = mutableListOf(second)
            } else {
                if (current.lastOrNull() != first) current += first
                current += second
            }
        }
        if (current.size >= MinimumLinePoints) segments += current
        return segments
    }

    private fun List<OfflineMapLocation>.withLongitudeOffset(offset: Double): List<OfflineMapLocation> {
        return map { location -> location.copy(longitude = location.longitude + offset) }
    }

    private fun List<OfflineMapLocation>.withoutDuplicateClosure(): List<OfflineMapLocation> {
        return if (size > 1 && first() == last()) dropLast(1) else this
    }

    private const val MinimumLinePoints = 2
    private const val MinimumPolygonPoints = 3
    private const val HalfWorldDegrees = 180.0
    private const val PoleLatitudeDegrees = 90.0
    private const val CoordinateEpsilon = 1e-9
    private const val FullWorldDegrees = 360.0
    private val WorldCopyOffsets = listOf(-FullWorldDegrees, 0.0, FullWorldDegrees)
}
