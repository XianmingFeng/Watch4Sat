package com.xianming.watch4sat.data.pass

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
class PassSnapshotStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `shorter coverage cannot replace a complete snapshot with the same key`() = runTest {
        val store = PassSnapshotStore(testDataStore("coverage.preferences_pb"))
        val long = snapshot(startMillis = 0L, endMillis = 2L * DayMillis)
        val short = snapshot(startMillis = HourMillis, endMillis = DayMillis + HourMillis)

        store.write(long)
        store.write(short)

        assertEquals(long, store.read(Key, nowMillis = 2_000L))
    }

    @Test
    fun `later rolling coverage replaces an older complete snapshot`() = runTest {
        val store = PassSnapshotStore(testDataStore("rolling.preferences_pb"))
        val old = snapshot(startMillis = 0L, endMillis = DayMillis)
        val rolling = snapshot(startMillis = HourMillis, endMillis = DayMillis + HourMillis)

        store.write(old)
        store.write(rolling)

        assertEquals(rolling, store.read(Key, nowMillis = 5_000L))
    }

    @Test
    fun `partial snapshot cannot replace a complete snapshot`() = runTest {
        val store = PassSnapshotStore(testDataStore("partial.preferences_pb"))
        val complete = snapshot(startMillis = 0L, endMillis = DayMillis)
        val partial = snapshot(startMillis = 0L, endMillis = 2L * DayMillis)
            .copy(completion = PassSnapshotCompletion.PARTIAL)

        store.write(complete)
        val failure = runCatching { store.write(partial) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(complete, store.read(Key, nowMillis = HourMillis))
    }

    @Test
    fun `unknown schema and short complete claims are rejected`() = runTest {
        val store = PassSnapshotStore(testDataStore("invalid-write.preferences_pb"))
        val complete = snapshot(startMillis = 0L, endMillis = DayMillis)
        val unknownSchema = complete.copy(schemaVersion = 99)
        val shortCoverage = complete.copy(coverageEndMillis = DayMillis - 1L)

        assertTrue(
            runCatching { store.write(unknownSchema) }.exceptionOrNull()
                is IllegalArgumentException
        )
        assertTrue(
            runCatching { store.write(shortCoverage) }.exceptionOrNull()
                is IllegalArgumentException
        )
        assertNull(store.read(Key, nowMillis = 0L))
    }

    @Test
    fun `older different identity cannot overwrite a newer snapshot`() = runTest {
        val store = PassSnapshotStore(testDataStore("identity-race.preferences_pb"))
        val newer = snapshot(startMillis = HourMillis, endMillis = DayMillis + HourMillis)
        val olderDifferentKey = snapshot(startMillis = 0L, endMillis = DayMillis)
            .copy(key = Key.copy(tleFingerprint = "old"))

        store.write(newer)
        store.write(olderDifferentKey)

        assertEquals(newer, store.read(Key, nowMillis = HourMillis))
        assertNull(store.read(olderDifferentKey.key, nowMillis = HourMillis))
    }

    @Test
    fun `forced replace accepts a complete window after clock rollback`() = runTest {
        val store = PassSnapshotStore(testDataStore("clock-rollback.preferences_pb"))
        val beforeRollback = snapshot(
            startMillis = 2L * HourMillis,
            endMillis = DayMillis + 2L * HourMillis
        )
        val afterRollback = snapshot(startMillis = HourMillis, endMillis = DayMillis + HourMillis)

        store.write(beforeRollback)
        store.replace(afterRollback)

        assertEquals(afterRollback, store.read(Key, nowMillis = HourMillis))
    }

    @Test
    fun `invalid encoded snapshot is cleared and exposes a one-shot recovery marker`() = runTest {
        val dataStore = testDataStore("invalid.preferences_pb")
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("pass_snapshot_json")] = "{"
        }
        val store = PassSnapshotStore(dataStore)

        assertNull(store.read(Key, nowMillis = 2_000L))
        assertTrue(store.recoveryRequired())
        assertTrue(store.consumeRecoveryRequired())
        assertFalse(store.consumeRecoveryRequired())
    }

    @Test
    fun `intentional clear does not report corruption recovery`() = runTest {
        val store = PassSnapshotStore(testDataStore("clear.preferences_pb"))
        store.write(snapshot(startMillis = 0L, endMillis = DayMillis))

        store.clear()

        assertNull(store.read(Key, nowMillis = 0L))
        assertFalse(store.recoveryRequired())
    }

    private fun snapshot(startMillis: Long, endMillis: Long): PassSnapshot {
        return PassSnapshot.fromPasses(
            key = Key,
            generatedAtMillis = startMillis,
            coveredWindowHours = 24,
            passes = emptyList(),
            coverageStartMillis = startMillis,
            coverageEndMillis = endMillis
        )
    }

    private fun TestScope.testDataStore(fileName: String): DataStore<Preferences> {
        val file = File(temporaryFolder.root, fileName)
        return PreferenceDataStoreFactory.createWithPath(
            scope = this,
            produceFile = { file.absolutePath.toPath() }
        )
    }

    private companion object {
        const val HourMillis = 60L * 60L * 1_000L
        const val DayMillis = 24L * HourMillis
        val Key = PassSnapshotKey(
            stationKey = "station",
            selectedSatelliteIds = listOf(25_544),
            tleFingerprint = "tle",
            passWindowHours = CanonicalPassSnapshotCoverageHours
        )
    }
}
