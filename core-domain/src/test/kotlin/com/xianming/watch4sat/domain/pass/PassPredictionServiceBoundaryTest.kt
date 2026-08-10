package com.xianming.watch4sat.domain.pass

import com.rtbishop.look4sat.core.domain.predict.DeepSpaceObject
import com.rtbishop.look4sat.core.domain.predict.NearEarthObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalData as PredictOrbitalData
import com.xianming.watch4sat.domain.fixture.SyntheticOrbitFixtures
import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.PassBoundary
import com.xianming.watch4sat.domain.model.PassVisibility
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationCoordinates
import com.xianming.watch4sat.domain.parser.CelestrakParser
import java.time.Instant
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassPredictionServiceBoundaryTest {

    // Fixed references were generated independently with satellite.js 6.0.1 from the
    // project-authored inputs documented in src/test/resources/orbital-fixtures/README.md.
    private val lowEarth = satellite(SyntheticOrbitFixtures.lowEarthTle)
    private val highEccentricity = satellite(SyntheticOrbitFixtures.highEccentricityTle)
    private val molniyaLike = satellite(SyntheticOrbitFixtures.molniyaLikeTle)
    private val geostationary = satellite(SyntheticOrbitFixtures.geostationaryTle)

    @Test
    fun `deep-space classification selects SDP4 without defining pass behavior`() {
        assertTrue(highEccentricity.orbitalData.toPredictData().getObject() is DeepSpaceObject)
        assertTrue(molniyaLike.orbitalData.toPredictData().getObject() is DeepSpaceObject)
        assertTrue(geostationary.orbitalData.toPredictData().getObject() is DeepSpaceObject)
        assertTrue(lowEarth.orbitalData.toPredictData().getObject() is NearEarthObject)
    }

    @Test
    fun `SGP4 and SDP4 propagated positions match independent fixed references`() {
        val lowEarthPosition = PassPredictionService.groundPositionAt(
            lowEarth,
            Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        )
        val highEccentricityPosition = PassPredictionService.groundPositionAt(
            highEccentricity,
            Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        )

        assertEquals(-35.330541, requireNotNull(lowEarthPosition.latitudeDegrees), 0.05)
        assertEquals(152.236848, requireNotNull(lowEarthPosition.longitudeDegrees), 0.05)
        assertEquals(427.863915, requireNotNull(lowEarthPosition.altitudeKm), 10.0)
        assertEquals(-3.475951, requireNotNull(highEccentricityPosition.latitudeDegrees), 0.05)
        assertEquals(-94.162061, requireNotNull(highEccentricityPosition.longitudeDegrees), 0.05)
        assertEquals(21_960.390525, requireNotNull(highEccentricityPosition.altitudeKm), 10.0)
    }

    @Test
    fun `synthetic near-Earth pass has refined actual boundaries and TCA`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val london = StationCoordinates(latitude = 51.4878, longitude = -0.2146)

        val pass = actualPasses(lowEarth, london, start, hoursAhead = 48).first()
        val tcaPosition = PassPredictionService.positionAt(lowEarth, london, pass.tcaMillis)

        assertEquals(PassBoundary.ACTUAL, pass.aosBoundary)
        assertEquals(PassBoundary.ACTUAL, pass.losBoundary)
        assertTrue(pass.aosMillis < pass.tcaMillis)
        assertTrue(pass.tcaMillis < pass.losMillis)
        assertEquals(pass.maxElevationDegrees, tcaPosition.elevationDegrees, 0.11)
        assertTrue(abs(pass.aosMillis % 1_000L) == 0L)
        assertTrue(abs(pass.losMillis % 1_000L) == 0L)
        assertTrue(abs(pass.tcaMillis % 1_000L) == 0L)
        assertWithin(
            expected = Instant.parse("2026-07-19T14:18:23Z").toEpochMilli(),
            actual = pass.aosMillis,
            toleranceMillis = 5_000L
        )
        assertWithin(
            expected = Instant.parse("2026-07-19T14:26:58Z").toEpochMilli(),
            actual = pass.losMillis,
            toleranceMillis = 5_000L
        )
        assertWithin(
            expected = Instant.parse("2026-07-19T14:22:39Z").toEpochMilli(),
            actual = pass.tcaMillis,
            toleranceMillis = 5_000L
        )
        assertEquals(10.436189, pass.maxElevationDegrees, 0.2)
    }

    @Test(timeout = 10_000L)
    fun `synthetic high-eccentricity SDP4 produces real horizon crossings`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val pennsylvania = StationCoordinates(latitude = 40.0, longitude = -75.0)

        val passes = actualPasses(highEccentricity, pennsylvania, start, hoursAhead = 48)

        assertFalse("Expected the synthetic HEO to cross the Pennsylvania horizon", passes.isEmpty())
        assertTrue(passes.all { it.aosBoundary == PassBoundary.ACTUAL })
        assertTrue(passes.all { it.losBoundary == PassBoundary.ACTUAL })
        assertTrue(passes.all { it.tcaMillis in it.aosMillis..it.losMillis })
        assertTrue(passes.maxOf { it.maxElevationDegrees } > 10.0)
        val first = passes.first()
        assertWithin(
            expected = Instant.parse("2026-07-19T14:08:44Z").toEpochMilli(),
            actual = first.aosMillis,
            toleranceMillis = 5_000L
        )
        assertWithin(
            expected = Instant.parse("2026-07-20T01:17:31Z").toEpochMilli(),
            actual = first.losMillis,
            toleranceMillis = 5_000L
        )
        assertWithin(
            expected = Instant.parse("2026-07-20T00:39:05Z").toEpochMilli(),
            actual = first.tcaMillis,
            toleranceMillis = 5_000L
        )
        assertEquals(47.965008, first.maxElevationDegrees, 0.2)
    }

    @Test(timeout = 10_000L)
    fun `synthetic Molniya-like orbit uses the same bounded pass search`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val northernStation = StationCoordinates(latitude = 60.0, longitude = 30.0)

        val passes = actualPasses(molniyaLike, northernStation, start, hoursAhead = 48)

        assertFalse("Expected a Molniya pass at the northern station", passes.isEmpty())
        assertTrue(passes.all { it.aosBoundary == PassBoundary.ACTUAL })
        assertTrue(passes.all { it.losBoundary == PassBoundary.ACTUAL })
        assertTrue(passes.any { it.durationMillis > 2L * 60L * 60L * 1_000L })
    }

    @Test
    fun `visible GEO is retained as continuous visibility clipped to the window`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val subpoint = PassPredictionService.groundPositionAt(geostationary, start)
        val beneathSatellite = StationCoordinates(
            latitude = requireNotNull(subpoint.latitudeDegrees),
            longitude = requireNotNull(subpoint.longitudeDegrees) + 0.1
        )
        val window = PassWindow(hoursAhead = 12)

        val passes = PassPredictionService.predictPasses(
            listOf(geostationary),
            beneathSatellite,
            start,
            window
        )

        assertEquals(1, passes.size)
        val pass = passes.single()
        assertEquals(start, pass.aosMillis)
        assertEquals(start + 12L * 60L * 60L * 1_000L, pass.losMillis)
        assertEquals(PassBoundary.WINDOW_CLIPPED, pass.aosBoundary)
        assertEquals(PassBoundary.WINDOW_CLIPPED, pass.losBoundary)
        assertEquals(PassVisibility.CONTINUOUS, pass.visibility)
        assertTrue(pass.isContinuouslyVisible)
        assertFalse(pass.isPassStartCandidate)
        assertTrue(pass.maxElevationDegrees > 80.0)
    }

    @Test(timeout = 5_000L)
    fun `GEO below the local horizon produces no pass and terminates`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val subpoint = PassPredictionService.groundPositionAt(geostationary, start)
        val antipodalLongitude = requireNotNull(subpoint.longitudeDegrees)
            .plus(180.0)
            .let { if (it > 180.0) it - 360.0 else it }
        val oppositeLongitude = StationCoordinates(
            latitude = -requireNotNull(subpoint.latitudeDegrees),
            longitude = antipodalLongitude
        )

        val passes = PassPredictionService.predictPasses(
            satellites = listOf(geostationary),
            station = oppositeLongitude,
            startMillis = start,
            window = PassWindow(hoursAhead = 24)
        )

        assertTrue(passes.isEmpty())
    }

    @Test
    fun `window start during active pass clips only AOS`() {
        val searchStart = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val london = StationCoordinates(latitude = 51.4878, longitude = -0.2146)
        val actual = actualPasses(lowEarth, london, searchStart, hoursAhead = 48).first()
        val activeStart = actual.tcaMillis

        val clipped = PassPredictionService.predictPasses(
            satellites = listOf(lowEarth),
            station = london,
            startMillis = activeStart,
            window = PassWindow(hoursAhead = 1)
        ).first { activeStart in it.aosMillis..it.losMillis }

        assertEquals(PassBoundary.WINDOW_CLIPPED, clipped.aosBoundary)
        assertEquals(PassBoundary.ACTUAL, clipped.losBoundary)
        assertFalse(clipped.isPassStartCandidate)
        assertEquals(actual.losMillis, clipped.losMillis)
    }

    @Test
    fun `window end during active pass clips only LOS`() {
        val searchStart = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val london = StationCoordinates(latitude = 51.4878, longitude = -0.2146)
        val actual = actualPasses(lowEarth, london, searchStart, hoursAhead = 48).first()
        val requestedStart = actual.aosMillis + 5L * 60_000L - 60L * 60_000L
        val normalizedStart = requestedStart / 60_000L * 60_000L
        val expectedWindowEnd = normalizedStart + 60L * 60_000L

        val clipped = PassPredictionService.predictPasses(
            satellites = listOf(lowEarth),
            station = london,
            startMillis = requestedStart,
            window = PassWindow(hoursAhead = 1)
        ).first { it.catalogNumber == lowEarth.catalogNumber && it.losMillis == expectedWindowEnd }

        assertEquals(PassBoundary.ACTUAL, clipped.aosBoundary)
        assertEquals(PassBoundary.WINDOW_CLIPPED, clipped.losBoundary)
        assertTrue(clipped.isPassStartCandidate)
        assertEquals(actual.aosMillis, clipped.aosMillis)
    }

    @Test
    fun `synthetic LEO pass search covers northern and southern hemisphere stations`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val london = StationCoordinates(latitude = 51.4878, longitude = -0.2146)
        val sydney = StationCoordinates(latitude = -33.8688, longitude = 151.2093)

        val northernPasses = actualPasses(lowEarth, london, start, hoursAhead = 48)
        val southernPasses = actualPasses(lowEarth, sydney, start, hoursAhead = 48)

        assertFalse(northernPasses.isEmpty())
        assertFalse(southernPasses.isEmpty())
        assertTrue(northernPasses.all(SatellitePass::isPassStartCandidate))
        assertTrue(southernPasses.all(SatellitePass::isPassStartCandidate))
    }

    @Test(timeout = 10_000L)
    fun `grazing synthetic horizon crossing is retained with actual boundaries`() {
        val start = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli()
        val arcticStation = StationCoordinates(latitude = 68.5, longitude = 0.0)

        val grazing = actualPasses(lowEarth, arcticStation, start, hoursAhead = 48)
            .minByOrNull(SatellitePass::maxElevationDegrees)

        assertTrue("Expected a grazing synthetic horizon crossing", grazing != null)
        assertTrue(requireNotNull(grazing).maxElevationDegrees in 0.0..0.2)
        assertTrue(grazing.durationMillis > 0L)
    }

    @Test(timeout = 5_000L)
    fun `oversized search window fails closed instead of entering an unbounded loop`() {
        val passes = PassPredictionService.predictPasses(
            satellites = listOf(geostationary),
            station = StationCoordinates(latitude = 0.0, longitude = -75.0),
            startMillis = Instant.parse("2026-07-19T00:00:00Z").toEpochMilli(),
            window = PassWindow(hoursAhead = 32 * 24)
        )

        assertTrue(passes.isEmpty())
    }

    private fun actualPasses(
        satellite: SatelliteRecord,
        station: StationCoordinates,
        startMillis: Long,
        hoursAhead: Int
    ): List<SatellitePass> {
        return PassPredictionService.predictPasses(
            satellites = listOf(satellite),
            station = station,
            startMillis = startMillis,
            window = PassWindow(hoursAhead = hoursAhead)
        ).filter {
            it.aosBoundary == PassBoundary.ACTUAL &&
                it.losBoundary == PassBoundary.ACTUAL
        }
    }

    private fun satellite(tle: String): SatelliteRecord {
        return CelestrakParser.parseTle(tle.trimIndent()).single()
    }

    private fun assertWithin(expected: Long, actual: Long, toleranceMillis: Long) {
        assertTrue(
            "Expected $actual to be within ${toleranceMillis}ms of $expected",
            abs(actual - expected) <= toleranceMillis
        )
    }

    private fun OrbitalData.toPredictData(): PredictOrbitalData {
        return PredictOrbitalData(
            name = name,
            epoch = epoch,
            meanmo = meanMotion,
            eccn = eccentricity,
            incl = inclinationDegrees,
            raan = rightAscensionAscendingNodeDegrees,
            argper = argumentOfPerigeeDegrees,
            meanan = meanAnomalyDegrees,
            catnum = catalogNumber,
            bstar = bstar,
            ndot = meanMotionDot
        )
    }
}
