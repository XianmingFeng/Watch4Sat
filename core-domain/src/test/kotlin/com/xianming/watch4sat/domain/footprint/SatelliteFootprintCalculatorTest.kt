package com.xianming.watch4sat.domain.footprint

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatelliteFootprintCalculatorTest {

    @Test
    fun `scalar radius uses the same horizon calculation without requiring polygon geometry`() {
        val radiusKm = SatelliteFootprintCalculator.radiusKmForAltitude(400.0)
        val footprint = SatelliteFootprintCalculator.calculate(
            point = GroundTrackPoint(
                timeMillis = 123_000L,
                latitudeDegrees = 0.0,
                longitudeDegrees = 0.0,
                altitudeKm = 400.0
            ),
            generatedAtMillis = 130_000L
        )

        assertEquals(footprint.radiusKm, requireNotNull(radiusKm), 1e-9)
        assertNull(SatelliteFootprintCalculator.radiusKmForAltitude(null))
        assertNull(SatelliteFootprintCalculator.radiusKmForAltitude(0.0))
        assertNull(SatelliteFootprintCalculator.radiusKmForAltitude(Double.NaN))
    }

    @Test
    fun `leo altitude produces horizon footprint radius and sampled boundary`() {
        val footprint = SatelliteFootprintCalculator.calculate(
            point = GroundTrackPoint(
                timeMillis = 123_000L,
                latitudeDegrees = 0.0,
                longitudeDegrees = 0.0,
                altitudeKm = 400.0
            ),
            generatedAtMillis = 130_000L
        )

        assertEquals(130_000L, footprint.generatedAtMillis)
        assertTrue(footprint.radiusKm in 2_150.0..2_250.0)
        assertEquals(96, footprint.ring.size)
        assertEquals(1, footprint.geometry.fillPolygons.size)
        assertEquals(1, footprint.geometry.outlineSegments.size)
        assertTrue(footprint.ring.all { it.latitudeDegrees in -90.0..90.0 })
        assertTrue(footprint.ring.all { it.longitudeDegrees in -180.0..180.0 })
    }

    @Test
    fun `missing altitude returns empty footprint`() {
        val footprint = SatelliteFootprintCalculator.calculate(
            point = GroundTrackPoint(
                timeMillis = 123_000L,
                latitudeDegrees = 0.0,
                longitudeDegrees = 0.0,
                altitudeKm = null
            ),
            generatedAtMillis = 130_000L
        )

        assertEquals(0.0, footprint.radiusKm, 0.001)
        assertTrue(footprint.ring.isEmpty())
        assertTrue(footprint.geometry.fillPolygons.isEmpty())
        assertTrue(footprint.geometry.outlineSegments.isEmpty())
    }

    @Test
    fun `footprint crossing antimeridian is split into safe segments`() {
        val footprint = SatelliteFootprintCalculator.calculate(
            point = GroundTrackPoint(
                timeMillis = 123_000L,
                latitudeDegrees = 0.0,
                longitudeDegrees = 179.0,
                altitudeKm = 400.0
            ),
            generatedAtMillis = 130_000L
        )

        assertEquals(2, footprint.geometry.fillPolygons.size)
        assertEquals(2, footprint.geometry.outlineSegments.size)
        footprint.geometry.outlineSegments.forEach { segment ->
            segment.zipWithNext { previous, next ->
                assertTrue(kotlin.math.abs(next.longitudeDegrees - previous.longitudeDegrees) <= 180.0)
            }
        }
    }

    @Test
    fun `footprint containing north pole remains filled and keeps real outline separate`() {
        val footprint = SatelliteFootprintCalculator.calculate(
            point = GroundTrackPoint(
                timeMillis = 123_000L,
                latitudeDegrees = 85.0,
                longitudeDegrees = 45.0,
                altitudeKm = 400.0
            ),
            generatedAtMillis = 130_000L
        )

        assertEquals(com.xianming.watch4sat.domain.geometry.GeographicPole.NORTH, footprint.enclosedPole)
        assertTrue(footprint.geometry.fillPolygons.isNotEmpty())
        assertTrue(footprint.geometry.outlineSegments.flatten().all { it.latitudeDegrees < 90.0 })
    }
}
