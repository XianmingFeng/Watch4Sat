/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.fixture.SyntheticOrbitFixtures
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.StationCoordinates
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.parser.CelestrakParser
import java.time.Instant
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPredictionServicePositionTest {

    private val satellite = CelestrakParser.parseTle(SyntheticOrbitFixtures.lowEarthTle).single()
    private val timeMillis = Instant.parse("2026-07-19T12:00:00Z").toEpochMilli()
    private val london = StationCoordinates(latitude = 51.4878, longitude = -0.2146)

    @Test
    fun `satellite position exposes deterministic topocentric slant range`() {
        val position = PassPredictionService.positionAt(
            satellite = satellite,
            station = london,
            timeMillis = timeMillis
        )

        val slantRangeKm = requireNotNull(position.slantRangeKm)
        assertEquals(timeMillis, position.timeMillis)
        assertTrue(slantRangeKm.isFinite())
        assertEquals(12_710.064891, slantRangeKm, 0.01)
    }

    @Test
    fun `satellite position API fixes observer altitude to zero meters`() {
        val pass = SatellitePass(
            catalogNumber = satellite.catalogNumber,
            satelliteName = satellite.displayName,
            aosMillis = timeMillis,
            losMillis = timeMillis + 1L,
            tcaMillis = timeMillis,
            maxElevationDegrees = 0.0,
            aosAzimuthDegrees = 0.0,
            losAzimuthDegrees = 0.0,
            orbitalData = satellite.orbitalData
        )
        val fromCoordinates = PassPredictionService.positionAt(satellite, london, timeMillis)
        val fromZeroMeterLocation = PassPredictionService.positionAt(
            pass = pass,
            station = StationLocation(
                latitude = london.latitude,
                longitude = london.longitude,
                altitudeMeters = 0.0
            ),
            timeMillis = timeMillis
        )
        val fromElevatedLocation = PassPredictionService.positionAt(
            pass = pass,
            station = StationLocation(
                latitude = london.latitude,
                longitude = london.longitude,
                altitudeMeters = 5_000.0
            ),
            timeMillis = timeMillis
        )

        assertEquals(
            requireNotNull(fromZeroMeterLocation.slantRangeKm),
            requireNotNull(fromCoordinates.slantRangeKm),
            1e-9
        )
        assertTrue(
            abs(
                requireNotNull(fromCoordinates.slantRangeKm) -
                    requireNotNull(fromElevatedLocation.slantRangeKm)
            ) > 0.1
        )
    }

    @Test
    fun `station at satellite subpoint has range approximately equal to altitude`() {
        val subpoint = PassPredictionService.groundPositionAt(satellite, timeMillis)
        val position = PassPredictionService.positionAt(
            satellite = satellite,
            station = StationCoordinates(
                latitude = requireNotNull(subpoint.latitudeDegrees),
                longitude = requireNotNull(subpoint.longitudeDegrees)
            ),
            timeMillis = timeMillis
        )

        assertTrue(position.elevationDegrees > 89.0)
        assertEquals(
            requireNotNull(position.altitudeKm),
            requireNotNull(position.slantRangeKm),
            1.0
        )
    }

    @Test
    fun `slant range remains available below the horizon`() {
        val subpoint = PassPredictionService.groundPositionAt(satellite, timeMillis)
        val antipodalLongitude = requireNotNull(subpoint.longitudeDegrees)
            .plus(180.0)
            .let { if (it >= 180.0) it - 360.0 else it }
        val position = PassPredictionService.positionAt(
            satellite = satellite,
            station = StationCoordinates(
                latitude = -requireNotNull(subpoint.latitudeDegrees),
                longitude = antipodalLongitude
            ),
            timeMillis = timeMillis
        )

        assertFalse(position.aboveHorizon)
        assertNotNull(position.slantRangeKm)
        assertTrue(requireNotNull(position.slantRangeKm) > requireNotNull(position.altitudeKm))
    }
}
