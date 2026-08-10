package com.xianming.watch4sat.domain.doppler

import com.xianming.watch4sat.domain.model.OrbitalPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DopplerCalculatorTest {

    @Test
    fun `calculate increases downlink and decreases uplink while satellite approaches`() {
        val reading = DopplerCalculator.calculate(
            baseDownlinkHz = 145_800_000L,
            baseUplinkHz = 435_000_000L,
            position = OrbitalPosition(
                timeMillis = 1_800_000L,
                azimuthDegrees = 180.0,
                elevationDegrees = 42.0,
                rangeRateKmPerSecond = -2.0
            )
        )

        val expectedDownlink = (145_800_000.0 * (299_792_458.0 - (-2.0 * 1000.0)) / 299_792_458.0).toLong()
        val expectedUplink = (435_000_000.0 * (299_792_458.0 + (-2.0 * 1000.0)) / 299_792_458.0).toLong()

        assertEquals(145_800_000L, reading.baseDownlinkHz)
        assertEquals(expectedDownlink, reading.correctedDownlinkHz)
        assertEquals(expectedDownlink - 145_800_000L, reading.downlinkOffsetHz)
        assertEquals((expectedDownlink - 145_800_000L) / 1000.0, requireNotNull(reading.downlinkOffsetKhz), 0.001)
        assertEquals(435_000_000L, reading.baseUplinkHz)
        assertEquals(expectedUplink, reading.correctedUplinkHz)
        assertEquals(expectedUplink - 435_000_000L, reading.uplinkOffsetHz)
        assertEquals((expectedUplink - 435_000_000L) / 1000.0, requireNotNull(reading.uplinkOffsetKhz), 0.001)
    }

    @Test
    fun `calculate leaves uplink fields null when no uplink frequency exists`() {
        val reading = DopplerCalculator.calculate(
            baseDownlinkHz = 145_800_000L,
            baseUplinkHz = null,
            position = OrbitalPosition(timeMillis = 0L, azimuthDegrees = 0.0, elevationDegrees = 0.0, rangeRateKmPerSecond = 0.4)
        )

        assertEquals(145_800_000L, reading.baseDownlinkHz)
        assertNull(reading.baseUplinkHz)
        assertNull(reading.correctedUplinkHz)
        assertNull(reading.uplinkOffsetHz)
        assertNull(reading.uplinkOffsetKhz)
    }
}
