package com.xianming.watch4sat.data

import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.data.repository.DataRefreshResult
import com.xianming.watch4sat.data.repository.DataRefreshOutcome
import com.xianming.watch4sat.data.repository.DataRefreshSource
import com.xianming.watch4sat.data.repository.DataRefreshSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerContractTest {

    @Test
    fun satelliteEntityRoundTripsDomainRecordAndKeepsSelectionInSettings() {
        val mapperClass = Class.forName("com.xianming.watch4sat.data.local.SatelliteEntityMappersKt")
        val record = SatelliteRecord(
            catalogNumber = 25_544,
            displayName = "ISS (ZARYA)",
            objectId = "1998-067A",
            selected = true,
            orbitalData = OrbitalData(
                name = "ISS (ZARYA)",
                catalogNumber = 25_544,
                epoch = 26_156.50,
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

        val toEntity = mapperClass.getDeclaredMethod("toEntity", SatelliteRecord::class.java)
        val entity = toEntity.invoke(null, record)
        val toDomain = mapperClass.getDeclaredMethod("toDomain", entity.javaClass, Set::class.java)

        val unselected = toDomain.invoke(null, entity, emptySet<Int>()) as SatelliteRecord
        val selected = toDomain.invoke(null, entity, setOf(25_544)) as SatelliteRecord

        assertFalse(unselected.selected)
        assertTrue(selected.selected)
        assertEquals(record.copy(selected = false), unselected)
        assertEquals(record.copy(selected = true), selected)
    }

    @Test
    fun transmitterEntityRoundTripsDomainRecord() {
        val mapperClass = Class.forName("com.xianming.watch4sat.data.local.TransmitterEntityMappersKt")
        val record = TransmitterRecord(
            uuid = "abc123",
            catalogNumber = 25_544,
            description = "FM Repeater",
            isAlive = true,
            status = "active",
            downlinkLowHz = 437_800_000L,
            downlinkHighHz = null,
            downlinkMode = "FM",
            uplinkLowHz = 145_990_000L,
            uplinkHighHz = null,
            uplinkMode = "FM",
            isInverted = false
        )

        val toEntity = mapperClass.getDeclaredMethod("toEntity", TransmitterRecord::class.java)
        val entity = toEntity.invoke(null, record)
        val toDomain = mapperClass.getDeclaredMethod("toDomain", entity.javaClass)

        assertEquals(record, toDomain.invoke(null, entity))
    }

    @Test
    fun settingsDefaultsMatchWearMvp() {
        val settingsClass = Class.forName("com.xianming.watch4sat.data.settings.Watch4SatSettings")
        val settings = settingsClass.getDeclaredConstructor().newInstance()

        assertEquals(12, settingsClass.getDeclaredMethod("getPassWindowHours").invoke(settings))
        assertEquals(emptySet<Int>(), settingsClass.getDeclaredMethod("getSelectedSatelliteIds").invoke(settings))
        assertEquals(emptyMap<Int, Any>(), settingsClass.getDeclaredMethod("getCustomDopplerBaseFrequencies").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getStationLocation").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastSatelliteDataUpdateMillis").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastTransmitterDataUpdateMillis").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastSatelliteDataError").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastTransmitterDataError").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastSatelliteDataFailureMillis").invoke(settings))
        assertNull(settingsClass.getDeclaredMethod("getLastTransmitterDataFailureMillis").invoke(settings))
        assertEquals(
            Class.forName("com.xianming.watch4sat.data.settings.MapTileMode").getDeclaredField("AUTO").get(null),
            settingsClass.getDeclaredMethod("getMapTileMode").invoke(settings)
        )
    }

    @Test
    fun networkFeedUrlsTargetConfiguredSources() {
        val urlsClass = Class.forName("com.xianming.watch4sat.data.network.NetworkFeedUrlsKt")

        assertEquals(
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=csv",
            urlsClass.getDeclaredField("CELESTRAK_AMATEUR_CSV_URL").get(null)
        )
        assertEquals(
            "https://db.satnogs.org/api/transmitters/?status=active",
            urlsClass.getDeclaredField("SATNOGS_ACTIVE_TRANSMITTERS_URL").get(null)
        )
    }

    @Test
    fun refreshSummaryDistinguishesCompletePartialAndFailedOutcomes() {
        val success = DataRefreshResult(recordsPersisted = 20, updatedAtMillis = 1L)
        val failure = DataRefreshResult.failure("TX failed")
        val partial = DataRefreshSummary(satellites = success, transmitters = failure)
        val complete = DataRefreshSummary(satellites = success, transmitters = success)
        val failed = DataRefreshSummary(satellites = failure, transmitters = failure)

        assertTrue(partial.isPartialSuccess)
        assertEquals(
            DataRefreshOutcome.Partial(
                successfulSource = DataRefreshSource.Satellites,
                recordsPersisted = 20
            ),
            partial.outcome
        )
        assertFalse(complete.isPartialSuccess)
        assertEquals(
            DataRefreshOutcome.Complete(
                satelliteRecordsPersisted = 20,
                transmitterRecordsPersisted = 20
            ),
            complete.outcome
        )
        assertTrue(failed.failedCompletely)
        assertEquals(DataRefreshOutcome.Failed, failed.outcome)
    }
}
