package com.xianming.watch4sat.domain.footprint

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.geometry.GeographicPole
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FootprintRingSegmenterTest {

    @Test
    fun `closed ring crossing only on last to first edge has fill polygons and paired outline endpoints`() {
        val ring = listOf(
            point(latitude = 0.0, longitude = 170.0),
            point(latitude = 10.0, longitude = 160.0),
            point(latitude = 10.0, longitude = 150.0),
            point(latitude = 0.0, longitude = -20.0)
        )

        val result = FootprintRingSegmenter.splitClosedRingAtAntimeridian(
            points = ring,
            enclosedPole = GeographicPole.NORTH
        )

        assertTrue(result.fillPolygons.isNotEmpty())
        assertTrue(result.outlineSegments.isNotEmpty())
        result.outlineSegments.forEach { segment ->
            assertTrue(segment.zipWithNext().all { (previous, next) ->
                abs(next.longitudeDegrees - previous.longitudeDegrees) <= 180.0
            })
        }
    }

    @Test
    fun `closed ring away from seam remains fill safe`() {
        val ring = listOf(
            point(latitude = 0.0, longitude = 10.0),
            point(latitude = 10.0, longitude = 10.0),
            point(latitude = 10.0, longitude = 30.0),
            point(latitude = 0.0, longitude = 30.0)
        )

        val result = FootprintRingSegmenter.splitClosedRingAtAntimeridian(ring)

        assertEquals(1, result.fillPolygons.size)
        assertEquals(1, result.outlineSegments.size)
        assertEquals(ring + ring.first(), result.outlineSegments.single())
    }

    private fun point(latitude: Double, longitude: Double): GroundTrackPoint {
        return GroundTrackPoint(
            timeMillis = 0L,
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            altitudeKm = 400.0
        )
    }
}
