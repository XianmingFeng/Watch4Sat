package com.xianming.watch4sat.domain.radar

import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarProjectionTest {

    @Test
    fun `projects horizon cardinal directions around the outer ring`() {
        assertPoint(RadarProjection.project(0.0, 0.0), x = 0.0, y = -1.0)
        assertPoint(RadarProjection.project(90.0, 0.0), x = 1.0, y = 0.0)
        assertPoint(RadarProjection.project(180.0, 0.0), x = 0.0, y = 1.0)
        assertPoint(RadarProjection.project(270.0, 0.0), x = -1.0, y = 0.0)
    }

    @Test
    fun `projects zenith to center and clamps elevation`() {
        assertPoint(RadarProjection.project(45.0, 90.0), x = 0.0, y = 0.0)
        assertPoint(RadarProjection.project(45.0, 120.0), x = 0.0, y = 0.0)
        val belowHorizon = RadarProjection.project(90.0, -12.0)
        assertEquals(1.0, belowHorizon.radiusFraction, 0.0001)
    }

    @Test
    fun `keeps only above horizon track points for drawing path`() {
        val points = listOf(
            RadarTrackPoint(0L, 0.0, -2.0, aboveHorizon = false, label = RadarTrackLabel.NONE),
            RadarTrackPoint(1L, 12.0, 0.0, aboveHorizon = true, label = RadarTrackLabel.AOS),
            RadarTrackPoint(2L, 80.0, 45.0, aboveHorizon = true, label = RadarTrackLabel.NONE),
            RadarTrackPoint(3L, 120.0, -1.0, aboveHorizon = false, label = RadarTrackLabel.LOS)
        )

        val drawable = RadarProjection.drawableTrack(points)

        assertEquals(2, drawable.size)
        assertEquals(RadarTrackLabel.AOS, drawable.first().label)
        assertEquals(RadarTrackLabel.NONE, drawable.last().label)
        assertTrue(drawable.all { it.aboveHorizon })
    }

    @Test
    fun `preserves aos and los label markers even when endpoint is just below horizon`() {
        val points = listOf(
            RadarTrackPoint(1L, 12.0, -0.2, aboveHorizon = false, label = RadarTrackLabel.AOS),
            RadarTrackPoint(2L, 80.0, 45.0, aboveHorizon = true, label = RadarTrackLabel.NONE),
            RadarTrackPoint(3L, 120.0, -0.1, aboveHorizon = false, label = RadarTrackLabel.LOS)
        )

        val markers = RadarProjection.labelMarkers(points)

        assertEquals(listOf(RadarTrackLabel.AOS, RadarTrackLabel.LOS), markers.map { it.label })
    }

    @Test
    fun `normalizes azimuth outside zero to three sixty`() {
        assertPoint(RadarProjection.project(450.0, 0.0), x = 1.0, y = 0.0)
        assertPoint(RadarProjection.project(-90.0, 0.0), x = -1.0, y = 0.0)
    }

    @Test
    fun `projects relative to current pointing so reference heading stays at top`() {
        assertPoint(
            RadarProjection.projectRelative(
                azimuthDegrees = 90.0,
                elevationDegrees = 0.0,
                referenceAzimuthDegrees = 90.0
            ),
            x = 0.0,
            y = -1.0
        )
        assertPoint(
            RadarProjection.projectRelative(
                azimuthDegrees = 0.0,
                elevationDegrees = 0.0,
                referenceAzimuthDegrees = 90.0
            ),
            x = -1.0,
            y = 0.0
        )
        assertPoint(
            RadarProjection.projectRelative(
                azimuthDegrees = 180.0,
                elevationDegrees = 0.0,
                referenceAzimuthDegrees = 90.0
            ),
            x = 1.0,
            y = 0.0
        )
    }

    @Test
    fun `relative projection normalizes across zero degrees`() {
        assertPoint(
            RadarProjection.projectRelative(
                azimuthDegrees = 355.0,
                elevationDegrees = 0.0,
                referenceAzimuthDegrees = 5.0
            ),
            x = -0.1736,
            y = -0.9848
        )
    }

    @Test
    fun `relative azimuth follows current compass reference for canvas labels`() {
        assertEquals(0.0, RadarProjection.relativeAzimuth(90.0, 90.0), 0.0001)
        assertEquals(270.0, RadarProjection.relativeAzimuth(0.0, 90.0), 0.0001)
        assertEquals(10.0, RadarProjection.relativeAzimuth(5.0, 355.0), 0.0001)
        assertEquals(270.0, RadarProjection.relativeAzimuth(270.0, null), 0.0001)
    }

    @Test
    fun `reports visible satellite marker only above horizon`() {
        assertTrue(RadarProjection.isSatelliteVisible(elevationDegrees = 0.0, aboveHorizon = true))
        assertTrue(RadarProjection.isSatelliteVisible(elevationDegrees = 22.0, aboveHorizon = true))
        assertFalse(RadarProjection.isSatelliteVisible(elevationDegrees = -0.1, aboveHorizon = false))
        assertFalse(RadarProjection.isSatelliteVisible(elevationDegrees = 12.0, aboveHorizon = false))
    }

    private fun assertPoint(point: RadarPlotPoint, x: Double, y: Double) {
        assertTrue("x expected $x but was ${point.x}", abs(point.x - x) < 0.0001)
        assertTrue("y expected $y but was ${point.y}", abs(point.y - y) < 0.0001)
    }
}
