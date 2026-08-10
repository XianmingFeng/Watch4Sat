package com.xianming.watch4sat.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xianming.watch4sat.data.pass.PassSnapshotKey
import com.xianming.watch4sat.data.pass.PassSnapshotStore
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class DataStoreFailurePropagationTest {

    @Test
    fun settingsStorePropagatesNonIoFailures() = runTest {
        val failure = IllegalStateException("programming failure")
        val store = Watch4SatSettingsStore(failingDataStore(failure))

        val thrown = runCatching { store.getSettings() }.exceptionOrNull()

        assertSame(failure, thrown)
    }

    @Test
    fun snapshotStorePropagatesNonIoFailures() = runTest {
        val failure = IllegalStateException("programming failure")
        val store = PassSnapshotStore(failingDataStore(failure))
        val key = PassSnapshotKey(
            stationKey = "station",
            selectedSatelliteIds = emptyList(),
            tleFingerprint = "tle",
            passWindowHours = 12
        )

        val thrown = runCatching { store.read(key, nowMillis = 0L) }.exceptionOrNull()

        assertSame(failure, thrown)
    }

    private fun failingDataStore(failure: Throwable): DataStore<Preferences> {
        return object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw failure }

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = throw failure
        }
    }
}
