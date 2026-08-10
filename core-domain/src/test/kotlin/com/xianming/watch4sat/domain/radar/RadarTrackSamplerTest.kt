package com.xianming.watch4sat.domain.radar

import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.SatellitePass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarTrackSamplerTest {

    @Test
    fun `sample includes AOS LOS endpoints and intermediate points every fifteen seconds`() {
        val pass = SatellitePass(
            catalogNumber = 25544,
            satelliteName = "ISS",
            aosMillis = 0L,
            losMillis = 61_000L,
            tcaMillis = 30_000L,
            maxElevationDegrees = 50.0,
            aosAzimuthDegrees = 10.0,
            losAzimuthDegrees = 200.0
        )

        val track = RadarTrackSampler.sample(pass) { timeMillis ->
            OrbitalPosition(
                timeMillis = timeMillis,
                azimuthDegrees = timeMillis / 1000.0,
                elevationDegrees = if (timeMillis in 0L..61_000L) 5.0 else -1.0,
                rangeRateKmPerSecond = 0.0
            )
        }

        assertEquals(listOf(0L, 15_000L, 30_000L, 45_000L, 60_000L, 61_000L), track.map { it.timeMillis })
        assertEquals(RadarTrackLabel.AOS, track.first().label)
        assertEquals(RadarTrackLabel.LOS, track.last().label)
        assertTrue(track.all { it.aboveHorizon })
        assertEquals(30.0, track[2].azimuthDegrees, 0.0)
    }

    @Test
    fun `sample does not duplicate LOS when duration lands on interval`() {
        val pass = SatellitePass(
            catalogNumber = 25544,
            satelliteName = "ISS",
            aosMillis = 0L,
            losMillis = 60_000L,
            tcaMillis = 30_000L,
            maxElevationDegrees = 50.0,
            aosAzimuthDegrees = 10.0,
            losAzimuthDegrees = 200.0
        )

        val track = RadarTrackSampler.sample(pass) { timeMillis ->
            OrbitalPosition(timeMillis, azimuthDegrees = 0.0, elevationDegrees = 1.0, rangeRateKmPerSecond = 0.0)
        }

        assertEquals(listOf(0L, 15_000L, 30_000L, 45_000L, 60_000L), track.map { it.timeMillis })
        assertEquals(RadarTrackLabel.LOS, track.last().label)
    }
}
