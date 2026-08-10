package com.xianming.watch4sat.domain.qth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaidenheadLocatorTest {

    @Test
    fun `toCoordinates uses Look4Sat-compatible center of six character subsquare`() {
        val london = requireNotNull(MaidenheadLocator.toCoordinates("io91VL39FX"))
        val montevideo = requireNotNull(MaidenheadLocator.toCoordinates("gf15vc"))

        assertEquals(51.4792, london.latitude, 0.0)
        assertEquals(-0.2083, london.longitude, 0.0)
        assertEquals(-34.8958, montevideo.latitude, 0.0)
        assertEquals(-56.2083, montevideo.longitude, 0.0)
    }

    @Test
    fun `toCoordinates rejects non six character locators after trimming to Look4Sat input length`() {
        assertNull(MaidenheadLocator.toCoordinates("ZZ00zz"))
        assertNull(MaidenheadLocator.toCoordinates("JN58"))
    }

    @Test
    fun `fromCoordinates returns uppercase six character locators for known cities and origin`() {
        assertEquals("IO91VL", MaidenheadLocator.fromCoordinates(latitude = 51.4878, longitude = -0.2146))
        assertEquals("JN58TD", MaidenheadLocator.fromCoordinates(latitude = 48.1466, longitude = 11.6083))
        assertEquals("PM01RF", MaidenheadLocator.fromCoordinates(latitude = 31.2304, longitude = 121.4737))
        assertEquals("JJ00AA", MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = 0.0))
    }

    @Test
    fun `fromCoordinates normalizes finite longitude across the date line`() {
        assertEquals(
            MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = -180.0),
            MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = 180.0)
        )
        assertEquals(
            MaidenheadLocator.fromCoordinates(latitude = 12.5, longitude = 120.0),
            MaidenheadLocator.fromCoordinates(latitude = 12.5, longitude = -240.0)
        )
        assertEquals(
            MaidenheadLocator.fromCoordinates(latitude = -12.5, longitude = -120.0),
            MaidenheadLocator.fromCoordinates(latitude = -12.5, longitude = 600.0)
        )
    }

    @Test
    fun `fromCoordinates keeps exact poles inside the six character grid`() {
        assertEquals("JR09AX", MaidenheadLocator.fromCoordinates(latitude = 90.0, longitude = 0.0))
        assertEquals("JA00AA", MaidenheadLocator.fromCoordinates(latitude = -90.0, longitude = 0.0))
    }

    @Test
    fun `fromCoordinates keeps values adjacent to exclusive upper bounds in their final subsquares`() {
        assertEquals(
            "JR09AX",
            MaidenheadLocator.fromCoordinates(latitude = Math.nextDown(90.0), longitude = 0.0)
        )
        assertEquals(
            "RJ90XA",
            MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = Math.nextDown(180.0))
        )
    }

    @Test
    fun `fromCoordinates rejects non finite values and invalid latitude`() {
        assertNull(MaidenheadLocator.fromCoordinates(latitude = 91.0542, longitude = -170.1142))
        assertNull(MaidenheadLocator.fromCoordinates(latitude = -91.0542, longitude = 170.1142))
        assertNull(MaidenheadLocator.fromCoordinates(latitude = Double.NaN, longitude = 0.0))
        assertNull(MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = Double.NaN))
        assertNull(MaidenheadLocator.fromCoordinates(latitude = Double.POSITIVE_INFINITY, longitude = 0.0))
        assertNull(MaidenheadLocator.fromCoordinates(latitude = 0.0, longitude = Double.NEGATIVE_INFINITY))
    }
}
