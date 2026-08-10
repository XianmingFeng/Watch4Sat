package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.fixture.SyntheticOrbitFixtures
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.StationCoordinates
import com.xianming.watch4sat.domain.parser.CelestrakParser
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPredictionServiceTest {

    private val station = StationCoordinates(latitude = 51.4878, longitude = -0.2146)
    private val startMillis = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()

    @Test
    fun `predictPasses generates visible passes from parsed TLE records sorted by AOS`() {
        val satellite = CelestrakParser.parseTle(SyntheticOrbitFixtures.lowEarthTle).single()

        val passes = PassPredictionService.predictPasses(
            satellites = listOf(satellite),
            station = station,
            startMillis = startMillis,
            window = PassWindow(hoursAhead = 48, minimumElevationDegrees = 0.0)
        )

        assertFalse("Expected at least one generated synthetic pass in the 48 hour window", passes.isEmpty())
        assertEquals(passes.map { it.aosMillis }.sorted(), passes.map { it.aosMillis })
        assertTrue(passes.all { it.catalogNumber == SyntheticOrbitFixtures.LOW_EARTH_CATALOG })
        assertTrue(passes.all { it.aosMillis < startMillis + 48L * 60L * 60L * 1000L })
        assertTrue(passes.all { it.losMillis > startMillis })
        assertTrue(passes.all { it.losMillis > it.aosMillis })
        assertTrue(passes.all { it.maxElevationDegrees >= 0.0 })
    }

    @Test
    fun `trackFor generated pass samples real orbital positions with endpoint labels`() {
        val satellite = CelestrakParser.parseTle(SyntheticOrbitFixtures.lowEarthTle).single()
        val pass = PassPredictionService.predictPasses(
            satellites = listOf(satellite),
            station = station,
            startMillis = startMillis,
            window = PassWindow(hoursAhead = 48, minimumElevationDegrees = 0.0)
        ).first()

        val track = PassPredictionService.trackFor(pass, station)

        assertFalse(track.isEmpty())
        assertEquals(pass.aosMillis, track.first().timeMillis)
        assertEquals(pass.losMillis, track.last().timeMillis)
        assertEquals(RadarTrackLabel.AOS, track.first().label)
        assertEquals(RadarTrackLabel.LOS, track.last().label)
        assertTrue(track.any { it.elevationDegrees > 0.0 })
        assertTrue(track.all { it.azimuthDegrees in 0.0..360.0 })
    }
}
