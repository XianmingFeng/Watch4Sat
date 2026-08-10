package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SelectedSatellite
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPlanningTest {

    private val now = Instant.parse("2026-06-05T00:00:00Z").toEpochMilli()
    private val iss = SelectedSatellite(catalogNumber = 25544, displayName = "ISS")
    private val ao91 = SelectedSatellite(catalogNumber = 43017, displayName = "AO-91")

    @Test
    fun `filter returns every selected satellite pass in the default future 12 hour window sorted by AOS`() {
        val passes = listOf(
            pass(name = "Unselected", catalogNumber = 999, aosOffsetMinutes = 20),
            pass(name = "ISS late", catalogNumber = 25544, aosOffsetMinutes = 90),
            pass(name = "AO-91", catalogNumber = 43017, aosOffsetMinutes = 30),
            pass(name = "ISS early", catalogNumber = 25544, aosOffsetMinutes = 10),
            pass(name = "ISS after window", catalogNumber = 25544, aosOffsetMinutes = 13 * 60),
            pass(name = "ISS ended", catalogNumber = 25544, aosOffsetMinutes = -30, durationMinutes = 10)
        )

        val filtered = PassWindowFilter.filter(
            passes = passes,
            selectedSatellites = listOf(iss, ao91),
            nowMillis = now
        )

        assertEquals(listOf("ISS early", "AO-91", "ISS late"), filtered.map { it.satelliteName })
    }

    @Test
    fun `filter includes active pass whose LOS is still inside the window`() {
        val active = pass(name = "ISS active", catalogNumber = 25544, aosOffsetMinutes = -5, durationMinutes = 15)

        val filtered = PassWindowFilter.filter(
            passes = listOf(active),
            selectedSatellites = listOf(iss),
            nowMillis = now
        )

        assertEquals(listOf("ISS active"), filtered.map { it.satelliteName })
    }

    @Test
    fun `pass is active at AOS and ended at LOS`() {
        val pass = pass(
            name = "ISS boundary",
            catalogNumber = 25_544,
            aosOffsetMinutes = 0,
            durationMinutes = 10
        )

        assertTrue(pass.isActiveAt(pass.aosMillis))
        assertTrue(pass.isActiveAt(pass.losMillis - 1L))
        assertFalse(pass.isActiveAt(pass.losMillis))
        assertFalse(pass.isActiveAt(pass.aosMillis - 1L))
    }

    @Test
    fun `filter and pass card exclude a pass exactly at LOS`() {
        val ended = pass(
            name = "ISS ended at boundary",
            catalogNumber = 25_544,
            aosOffsetMinutes = -10,
            durationMinutes = 10
        )

        val filtered = PassWindowFilter.filter(
            passes = listOf(ended),
            selectedSatellites = listOf(iss),
            nowMillis = now
        )
        val card = PassCardMapper.map(
            pass = ended,
            transmitters = emptyList(),
            nowMillis = now,
            textFormatter = EnglishPassCardTextFormatter,
            zoneId = ZoneOffset.UTC
        )

        assertTrue(filtered.isEmpty())
        assertFalse(card.isActive)
        assertFalse(card.isUpcoming)
    }

    @Test
    fun `filter applies configurable hours and minimum elevation`() {
        val filtered = PassWindowFilter.filter(
            passes = listOf(
                pass(name = "low", catalogNumber = 25544, aosOffsetMinutes = 10, maxElevationDegrees = 9.0),
                pass(name = "high", catalogNumber = 25544, aosOffsetMinutes = 20, maxElevationDegrees = 30.0),
                pass(name = "late", catalogNumber = 25544, aosOffsetMinutes = 80, maxElevationDegrees = 70.0)
            ),
            selectedSatellites = listOf(iss),
            nowMillis = now,
            window = PassWindow(hoursAhead = 1, minimumElevationDegrees = 10.0)
        )

        assertEquals(listOf("high"), filtered.map { it.satelliteName })
    }

    @Test
    fun `pass card mapper exposes AOS LOS TCA max elevation azimuth duration and mode frequency hint`() {
        val satellitePass = pass(
            name = "ISS",
            catalogNumber = 25544,
            aosOffsetMinutes = 0,
            durationMinutes = 10,
            tcaOffsetMinutes = 5,
            maxElevationDegrees = 67.4,
            aosAzimuthDegrees = 123.4,
            losAzimuthDegrees = 278.9
        )
        val transmitter = TransmitterRecord(
            uuid = "fm",
            catalogNumber = 25544,
            description = "FM Voice",
            isAlive = true,
            status = "active",
            downlinkLowHz = 145_800_000L,
            downlinkHighHz = null,
            downlinkMode = "FM",
            uplinkLowHz = 435_000_000L,
            uplinkHighHz = null,
            uplinkMode = "FM",
            isInverted = false
        )

        val card = PassCardMapper.map(
            satellitePass,
            listOf(transmitter),
            nowMillis = now,
            textFormatter = EnglishPassCardTextFormatter,
            zoneId = ZoneOffset.UTC
        )

        assertEquals("ISS", card.satelliteName)
        assertEquals("00:00", card.aosTime)
        assertEquals("00:10", card.losTime)
        assertEquals("00:05", card.tcaTime)
        assertEquals("67°", card.maxElevation)
        assertEquals("123°", card.aosAzimuth)
        assertEquals("279°", card.losAzimuth)
        assertEquals("now", card.aosCountdown)
        assertEquals("10m", card.duration)
        assertEquals("FM 145.800 MHz DL / 435.000 MHz UL", card.modeFrequencyHint)
        assertTrue(card.isActive)
        assertFalse(card.isUpcoming)
    }

    @Test
    fun `pass card mapper exposes compact AOS countdown before upcoming pass`() {
        val satellitePass = pass(
            name = "AO-91",
            catalogNumber = 43_017,
            aosOffsetMinutes = 17,
            durationMinutes = 9,
            maxElevationDegrees = 42.1
        )

        val card = PassCardMapper.map(
            satellitePass,
            emptyList(),
            nowMillis = now,
            textFormatter = EnglishPassCardTextFormatter,
            zoneId = ZoneOffset.UTC
        )

        assertEquals("in 17m", card.aosCountdown)
        assertFalse(card.aosCountdown.startsWith("AOS "))
        assertEquals("42°", card.maxElevation)
    }

    @Test
    fun `pass card mapper honors injected 12 hour clock format`() {
        val satellitePass = pass(
            name = "AO-91",
            catalogNumber = 43_017,
            aosOffsetMinutes = 13 * 60 + 5,
            durationMinutes = 10
        )

        val card = PassCardMapper.map(
            pass = satellitePass,
            transmitters = emptyList(),
            nowMillis = now,
            textFormatter = EnglishPassCardTextFormatter,
            zoneId = ZoneOffset.UTC,
            clockTimeFormatter = ClockTimeFormatter(
                is24HourFormat = false,
                locale = Locale.US
            )
        )

        assertEquals("1:05 PM", card.aosTime)
        assertEquals("1:15 PM", card.losTime)
        assertEquals("1:10 PM", card.tcaTime)
    }

    @Test
    fun `pass card mapper keeps active pass status compact`() {
        val satellitePass = pass(
            name = "AO-91 active",
            catalogNumber = 43_017,
            aosOffsetMinutes = -2,
            durationMinutes = 9
        )

        val card = PassCardMapper.map(
            satellitePass,
            emptyList(),
            nowMillis = now,
            textFormatter = EnglishPassCardTextFormatter,
            zoneId = ZoneOffset.UTC
        )

        assertEquals("NOW", card.aosCountdown)
        assertTrue(card.isActive)
        assertFalse(card.aosCountdown.startsWith("AOS "))
    }

    private fun pass(
        name: String,
        catalogNumber: Int,
        aosOffsetMinutes: Int,
        durationMinutes: Int = 10,
        tcaOffsetMinutes: Int = aosOffsetMinutes + durationMinutes / 2,
        maxElevationDegrees: Double = 45.0,
        aosAzimuthDegrees: Double = 90.0,
        losAzimuthDegrees: Double = 270.0
    ): SatellitePass {
        val aos = now + aosOffsetMinutes * 60_000L
        return SatellitePass(
            catalogNumber = catalogNumber,
            satelliteName = name,
            aosMillis = aos,
            losMillis = aos + durationMinutes * 60_000L,
            tcaMillis = now + tcaOffsetMinutes * 60_000L,
            maxElevationDegrees = maxElevationDegrees,
            aosAzimuthDegrees = aosAzimuthDegrees,
            losAzimuthDegrees = losAzimuthDegrees
        )
    }
}
