package com.xianming.watch4sat.domain.geometry

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntimeridianClipperTest {

    @Test
    fun `eastbound polyline crossing has paired seam endpoints and keeps two point segments`() {
        val segments = AntimeridianClipper.splitPolyline(
            listOf(
                point(timeMillis = 0L, latitude = 10.0, longitude = 179.0, altitudeKm = 400.0),
                point(timeMillis = 10L, latitude = 20.0, longitude = -179.0, altitudeKm = 500.0)
            )
        )

        assertEquals(2, segments.size)
        assertEquals(listOf(179.0, 180.0), segments[0].map { it.longitudeDegrees })
        assertEquals(listOf(-180.0, -179.0), segments[1].map { it.longitudeDegrees })
        assertEquals(15.0, segments[0].last().latitudeDegrees, 0.000_001)
        assertEquals(segments[0].last().latitudeDegrees, segments[1].first().latitudeDegrees, 0.0)
        assertEquals(5L, segments[0].last().timeMillis)
        assertEquals(450.0, segments[0].last().altitudeKm!!, 0.000_001)
        assertTrue(segments.all { it.size == 2 })
    }

    @Test
    fun `westbound polyline crossing is symmetric`() {
        val segments = AntimeridianClipper.splitPolyline(
            listOf(
                point(timeMillis = 0L, latitude = 20.0, longitude = -179.0),
                point(timeMillis = 10L, latitude = 10.0, longitude = 179.0)
            )
        )

        assertEquals(listOf(-179.0, -180.0), segments[0].map { it.longitudeDegrees })
        assertEquals(listOf(180.0, 179.0), segments[1].map { it.longitudeDegrees })
        assertEquals(15.0, segments[0].last().latitudeDegrees, 0.000_001)
    }

    @Test
    fun `equivalent antimeridian endpoints remain finite without a synthetic crossing`() {
        listOf(
            listOf(
                point(timeMillis = 0L, latitude = 10.0, longitude = 180.0, altitudeKm = 400.0),
                point(timeMillis = 10L, latitude = 20.0, longitude = -180.0, altitudeKm = 500.0)
            ),
            listOf(
                point(timeMillis = 0L, latitude = 20.0, longitude = -180.0, altitudeKm = 500.0),
                point(timeMillis = 10L, latitude = 10.0, longitude = 180.0, altitudeKm = 400.0)
            )
        ).forEach { points ->
            val segments = AntimeridianClipper.splitPolyline(points)

            assertEquals(1, segments.size)
            assertEquals(listOf(180.0, 180.0), segments.single().map { it.longitudeDegrees })
            assertTrue(segments.flatten().all { point ->
                point.latitudeDegrees.isFinite() &&
                    point.longitudeDegrees.isFinite() &&
                    point.altitudeKm?.isFinite() != false
            })
        }
    }

    @Test
    fun `polyline can clip against a projection seam away from Greenwich`() {
        val segments = AntimeridianClipper.splitPolyline(
            points = listOf(
                point(longitude = -80.0),
                point(longitude = -100.0)
            ),
            centerLongitudeDegrees = 90.0
        )

        assertEquals(2, segments.size)
        assertEquals(-90.0, segments[0].last().longitudeDegrees, 0.0)
        assertEquals(270.0, segments[1].first().longitudeDegrees, 0.0)
        assertTrue(segments.flatten().all { it.longitudeDegrees in -90.0..270.0 })
    }

    @Test
    fun `closed ring crossing seam produces two fills and only real circumference outlines`() {
        val geometry = AntimeridianClipper.clipClosedRing(
            listOf(
                point(latitude = 10.0, longitude = 170.0),
                point(latitude = 10.0, longitude = -170.0),
                point(latitude = 0.0, longitude = -170.0),
                point(latitude = 0.0, longitude = 170.0)
            )
        )

        assertEquals(2, geometry.fillPolygons.size)
        assertEquals(2, geometry.outlineSegments.size)
        assertTrue(geometry.fillPolygons.all { polygon ->
            polygon.size >= 3 && polygon.all { it.longitudeDegrees in -180.0..180.0 }
        })
        assertTrue(geometry.outlineSegments.all { it.size >= 2 })
        assertTrue(geometry.outlineSegments.flatten().count { abs(it.longitudeDegrees) == 180.0 } >= 4)
        assertFalse(
            geometry.outlineSegments.any { segment ->
                segment.zipWithNext().any { (first, second) ->
                    abs(first.longitudeDegrees) == 180.0 && abs(second.longitudeDegrees) == 180.0
                }
            }
        )
        assertTrue(geometry.fillPolygons.containsLocationInAnyPolygon(latitude = 5.0, longitude = 175.0))
        assertTrue(geometry.fillPolygons.containsLocationInAnyPolygon(latitude = 5.0, longitude = -175.0))
        assertFalse(geometry.fillPolygons.containsLocationInAnyPolygon(latitude = 5.0, longitude = 0.0))
    }

    @Test
    fun `closed ring output is invariant to rotating the source start point`() {
        val ring = listOf(
            point(latitude = 10.0, longitude = 170.0),
            point(latitude = 10.0, longitude = -170.0),
            point(latitude = 0.0, longitude = -170.0),
            point(latitude = 0.0, longitude = 170.0)
        )

        val original = AntimeridianClipper.clipClosedRing(ring)
        val rotated = AntimeridianClipper.clipClosedRing(ring.drop(1) + ring.first())

        assertEquals(original.fillPolygons.totalAbsoluteArea(), rotated.fillPolygons.totalAbsoluteArea(), 0.000_001)
        assertEquals(original.outlineSegments.sumOf { it.size }, rotated.outlineSegments.sumOf { it.size })
    }

    @Test
    fun `north polar ring fills the cap while its outline excludes pole closure edges`() {
        val geometry = AntimeridianClipper.clipClosedRing(
            points = listOf(
                point(latitude = 80.0, longitude = 0.0),
                point(latitude = 80.0, longitude = 90.0),
                point(latitude = 80.0, longitude = 179.0),
                point(latitude = 80.0, longitude = -90.0)
            ),
            enclosedPole = GeographicPole.NORTH
        )

        assertTrue(geometry.fillPolygons.isNotEmpty())
        assertTrue(geometry.fillPolygons.containsLocationInAnyPolygon(latitude = 89.0, longitude = 0.0))
        assertFalse(geometry.fillPolygons.containsLocationInAnyPolygon(latitude = 70.0, longitude = 0.0))
        assertTrue(geometry.outlineSegments.flatten().all { it.latitudeDegrees < 90.0 })
    }

    private fun point(
        timeMillis: Long = 0L,
        latitude: Double = 0.0,
        longitude: Double,
        altitudeKm: Double? = null
    ): GroundTrackPoint {
        return GroundTrackPoint(
            timeMillis = timeMillis,
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            altitudeKm = altitudeKm
        )
    }

    private fun List<List<GroundTrackPoint>>.containsLocationInAnyPolygon(
        latitude: Double,
        longitude: Double
    ): Boolean {
        return count { polygon -> polygon.containsLocationInPolygon(latitude, longitude) } % 2 == 1
    }

    private fun List<GroundTrackPoint>.containsLocationInPolygon(latitude: Double, longitude: Double): Boolean {
        var inside = false
        var previous = last()
        for (current in this) {
            val crosses = (current.latitudeDegrees > latitude) != (previous.latitudeDegrees > latitude)
            if (crosses) {
                val intersectionLongitude =
                    (previous.longitudeDegrees - current.longitudeDegrees) *
                    (latitude - current.latitudeDegrees) /
                    (previous.latitudeDegrees - current.latitudeDegrees) +
                    current.longitudeDegrees
                if (longitude < intersectionLongitude) inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun List<List<GroundTrackPoint>>.totalAbsoluteArea(): Double {
        return sumOf { polygon ->
            abs(
                polygon.indices.sumOf { index ->
                    val next = polygon[(index + 1) % polygon.size]
                    polygon[index].longitudeDegrees * next.latitudeDegrees -
                        next.longitudeDegrees * polygon[index].latitudeDegrees
                } / 2.0
            )
        }
    }
}
