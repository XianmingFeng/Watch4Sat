package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.fixture.SyntheticOrbitFixtures
import com.xianming.watch4sat.domain.parser.CelestrakParser
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPredictionServiceGroundTrackTest {

    private val satellite = CelestrakParser.parseTle(SyntheticOrbitFixtures.lowEarthTle).single()
    private val centerMillis = Instant.parse("2026-07-19T12:00:00Z").toEpochMilli()

    @Test
    fun `ground position exposes normalized latitude longitude and altitude`() {
        val position = PassPredictionService.groundPositionAt(satellite, centerMillis)

        assertNotNull(position.latitudeDegrees)
        assertNotNull(position.longitudeDegrees)
        assertNotNull(position.altitudeKm)
        assertTrue(position.latitudeDegrees!! in -90.0..90.0)
        assertTrue(position.longitudeDegrees!! in -180.0..180.0)
        assertTrue(position.altitudeKm!! > 100.0)
    }

    @Test
    fun `ground track samples before and after center time`() {
        val track = PassPredictionService.groundTrackFor(
            satellite = satellite,
            centerTimeMillis = centerMillis,
            intervalSeconds = 60,
            pointsBefore = 2,
            pointsAfter = 2
        )

        assertEquals(5, track.size)
        assertEquals(centerMillis - 120_000L, track.first().timeMillis)
        assertEquals(centerMillis, track[2].timeMillis)
        assertEquals(centerMillis + 120_000L, track.last().timeMillis)
        assertTrue(track.all { it.latitudeDegrees in -90.0..90.0 })
        assertTrue(track.all { it.longitudeDegrees in -180.0..180.0 })
    }
}
