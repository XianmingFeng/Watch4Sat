package com.xianming.watch4sat.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.StationLocation
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class Watch4SatSettingsStoreLocationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun stationAccuracyPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("station_accuracy.preferences_pb"))
        val location = StationLocation(
            latitude = 22.59,
            longitude = 113.96,
            altitudeMeters = 10.0,
            qthLocator = "OL72AX",
            timestampMillis = 1_000L,
            source = LocationSource.GPS,
            accuracyMeters = 35f
        )

        store.setStationLocation(location)

        val savedLocation = store.getSettings().stationLocation
        assertEquals(35f, savedLocation?.accuracyMeters)
    }

    @Test
    fun stationAccuracyIsClearedWhenNewLocationHasUnknownAccuracy() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("station_unknown_accuracy.preferences_pb"))

        store.setStationLocation(
            StationLocation(
                latitude = 22.59,
                longitude = 113.96,
                source = LocationSource.GPS,
                accuracyMeters = 35f
            )
        )
        store.setStationLocation(
            StationLocation(
                latitude = 22.60,
                longitude = 113.95,
                source = LocationSource.GPS
            )
        )

        assertNull(store.getSettings().stationLocation?.accuracyMeters)
    }

    @Test
    fun clearStationLocationRemovesAccuracy() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("station_clear_accuracy.preferences_pb"))

        store.setStationLocation(
            StationLocation(
                latitude = 22.59,
                longitude = 113.96,
                source = LocationSource.GPS,
                accuracyMeters = 35f
            )
        )
        store.clearStationLocation()

        assertNull(store.getSettings().stationLocation)
    }

    @Test
    fun oldStationLocationWithoutAccuracyReadsAsUnknownAccuracy() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("station_old_accuracy.preferences_pb"))

        store.setStationLocation(
            StationLocation(
                latitude = 22.59,
                longitude = 113.96,
                source = LocationSource.GPS
            )
        )

        assertNull(store.getSettings().stationLocation?.accuracyMeters)
    }

    @Test
    fun stationDeletionBarrierIsPersistentIdempotentAndGenerationBound() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("station_deletion.preferences_pb"))
        val firstLocation = StationLocation(
            latitude = 22.59,
            longitude = 113.96,
            source = LocationSource.GPS
        )
        store.setStationLocation(firstLocation)
        val savedGeneration = store.getSettings().stationDataGeneration

        val deletionGeneration = store.beginStationDataDeletion()
        val repeatedGeneration = store.beginStationDataDeletion()
        val deleting = store.getSettings()

        assertEquals(savedGeneration + 1L, deletionGeneration)
        assertEquals(deletionGeneration, repeatedGeneration)
        assertTrue(deleting.stationDataDeletionInProgress)
        assertNull(deleting.stationLocation)
        assertTrue(
            runCatching {
                store.setStationLocation(firstLocation.copy(latitude = 22.60))
            }.isFailure
        )

        store.completeStationDataDeletion()
        val cleared = store.getSettings()
        assertFalse(cleared.stationDataDeletionInProgress)
        assertNull(cleared.stationLocation)
        assertEquals(deletionGeneration, cleared.stationDataGeneration)

        store.setStationLocation(firstLocation)
        val restored = store.getSettings()
        assertEquals(deletionGeneration + 1L, restored.stationDataGeneration)
        assertEquals(firstLocation, restored.stationLocation)
    }

    private fun TestScope.testDataStore(fileName: String): DataStore<Preferences> {
        val file = File(temporaryFolder.root, fileName)
        return PreferenceDataStoreFactory.createWithPath(
            scope = this,
            produceFile = { file.absolutePath.toPath() }
        )
    }
}
