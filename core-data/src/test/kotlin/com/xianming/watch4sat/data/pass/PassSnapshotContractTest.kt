package com.xianming.watch4sat.data.pass

import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.PassBoundary
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassSnapshotContractTest {

    private val station = StationLocation(
        latitude = 31.2304,
        longitude = 121.4737,
        altitudeMeters = 8.0,
        qthLocator = "PM01SU",
        source = LocationSource.MANUAL_QTH
    )
    private val iss = satellite(25_544, "ISS")
    private val ao91 = satellite(43_017, "AO-91")
    private val settings = Watch4SatSettings(
        stationLocation = station,
        selectedSatelliteIds = setOf(25_544, 43_017),
        passWindowHours = 12,
        lastSatelliteDataUpdateMillis = 1_812_326_000_000L
    )

    @Test
    fun `snapshot key changes with inputs but not the UI display window`() {
        val base = PassSnapshotKeys.forInputs(
            station = station,
            satellites = listOf(iss, ao91),
            settings = settings
        )

        assertNotEquals(base, PassSnapshotKeys.forInputs(station.copy(latitude = 31.3), listOf(iss, ao91), settings))
        assertNotEquals(base, PassSnapshotKeys.forInputs(station, listOf(iss), settings.copy(selectedSatelliteIds = setOf(25_544))))
        assertNotEquals(base, PassSnapshotKeys.forInputs(station, listOf(iss.copy(orbitalData = iss.orbitalData.copy(epoch = 1.0)), ao91), settings))
        assertEquals(base, PassSnapshotKeys.forInputs(station, listOf(iss, ao91), settings.copy(passWindowHours = 24)))
        assertEquals(CanonicalPassSnapshotCoverageHours, base.passWindowHours)
    }

    @Test
    fun `snapshot key ignores freshness timestamp when TLE content is unchanged`() {
        val first = PassSnapshotKeys.forInputs(station, listOf(iss, ao91), settings)
        val second = PassSnapshotKeys.forInputs(
            station,
            listOf(ao91, iss),
            settings.copy(lastSatelliteDataUpdateMillis = settings.lastSatelliteDataUpdateMillis?.plus(1L))
        )

        assertEquals(first, second)
    }

    @Test
    fun `snapshot round trips pass list and invalidates after covered window expires`() {
        val key = PassSnapshotKeys.forInputs(station, listOf(iss), settings.copy(selectedSatelliteIds = setOf(25_544)))
        val snapshot = PassSnapshot.fromPasses(
            key = key,
            generatedAtMillis = 1_812_326_400_000L,
            coveredWindowHours = CanonicalPassSnapshotCoverageHours,
            passes = listOf(
                pass(iss, 1_812_330_000_000L).copy(
                    aosBoundary = PassBoundary.WINDOW_CLIPPED,
                    losBoundary = PassBoundary.ACTUAL
                )
            )
        )

        val encoded = PassSnapshotCodec.encode(snapshot)
        val decoded = PassSnapshotCodec.decode(encoded)

        assertEquals(snapshot, decoded)
        assertTrue(decoded.isUsableAt(1_812_327_000_000L))
        assertFalse(
            decoded.isUsableAt(
                1_812_326_400_000L + CanonicalPassSnapshotCoverageHours * HourMillis + 1L
            )
        )
        val decodedPass = decoded.toPasses(listOf(iss)).single()
        assertEquals(25_544, decodedPass.catalogNumber)
        assertEquals(PassBoundary.WINDOW_CLIPPED, decodedPass.aosBoundary)
        assertEquals(PassBoundary.ACTUAL, decodedPass.losBoundary)
        assertTrue(decoded.isComplete)
        assertTrue(
            decoded.completelyCovers(
                startMillis = decoded.coverageStartMillis,
                endMillis = decoded.coverageEndMillis
            )
        )
    }

    @Test
    fun `snapshot exposes only coverage contiguous with requested start`() {
        val generatedAtMillis = 1_812_326_400_000L
        val snapshot = PassSnapshot.fromPasses(
            key = PassSnapshotKeys.forInputs(station, listOf(iss), settings.copy(selectedSatelliteIds = setOf(25_544))),
            generatedAtMillis = generatedAtMillis,
            coveredWindowHours = CanonicalPassSnapshotCoverageHours,
            passes = emptyList()
        )

        assertEquals(
            generatedAtMillis + CanonicalPassSnapshotCoverageHours * HourMillis,
            snapshot.coverageEndMillis
        )
        assertEquals(snapshot.coverageEndMillis, snapshot.reusableCoverageEndMillis(generatedAtMillis + HourMillis))
        assertEquals(
            generatedAtMillis + 25 * HourMillis,
            snapshot.reusableCoverageEndMillis(generatedAtMillis + 25 * HourMillis)
        )
        assertEquals(
            generatedAtMillis,
            snapshot.copy(
                generatedAtMillis = generatedAtMillis + HourMillis,
                coverageStartMillis = generatedAtMillis + HourMillis
            )
                .reusableCoverageEndMillis(generatedAtMillis)
        )
    }

    @Test
    fun `legacy snapshot fields default to partial schema and actual boundaries`() {
        val decoded = PassSnapshotCodec.decode(
            """
            {
              "key": {
                "stationKey": "station",
                "selectedSatelliteIds": [25544],
                "tleFingerprint": "tle",
                "passWindowHours": 12,
                "minimumElevationDegrees": 0.0
              },
              "generatedAtMillis": 1000,
              "coveredWindowHours": 2,
              "passes": [{
                "catalogNumber": 25544,
                "satelliteName": "ISS",
                "aosMillis": 2000,
                "losMillis": 3000,
                "tcaMillis": 2500,
                "maxElevationDegrees": 45.0,
                "aosAzimuthDegrees": 90.0,
                "losAzimuthDegrees": 270.0
              }]
            }
            """.trimIndent()
        )

        assertEquals(PassSnapshot.LegacySchemaVersion, decoded.schemaVersion)
        assertEquals(PassSnapshotCompletion.PARTIAL, decoded.completion)
        assertFalse(decoded.isComplete)
        assertEquals(PassBoundary.ACTUAL, decoded.passes.single().aosBoundary)
        assertEquals(PassBoundary.ACTUAL, decoded.passes.single().losBoundary)
    }

    private fun satellite(catalogNumber: Int, name: String): SatelliteRecord {
        return SatelliteRecord(
            catalogNumber = catalogNumber,
            displayName = name,
            selected = true,
            orbitalData = OrbitalData(
                name = name,
                catalogNumber = catalogNumber,
                epoch = 26_156.5,
                meanMotion = 15.49,
                eccentricity = 0.00068,
                inclinationDegrees = 51.64,
                rightAscensionAscendingNodeDegrees = 113.2,
                argumentOfPerigeeDegrees = 71.0,
                meanAnomalyDegrees = 42.0,
                bstar = 1.2e-4,
                meanMotionDot = 2.1e-5
            )
        )
    }

    private fun pass(satellite: SatelliteRecord, aosMillis: Long): SatellitePass {
        return SatellitePass(
            catalogNumber = satellite.catalogNumber,
            satelliteName = satellite.displayName,
            aosMillis = aosMillis,
            losMillis = aosMillis + 600_000L,
            tcaMillis = aosMillis + 300_000L,
            maxElevationDegrees = 45.0,
            aosAzimuthDegrees = 90.0,
            losAzimuthDegrees = 270.0,
            orbitalData = satellite.orbitalData
        )
    }

    private companion object {
        const val HourMillis = 60L * 60L * 1000L
    }
}
