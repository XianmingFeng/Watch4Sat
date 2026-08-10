package com.xianming.watch4sat.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.StationLocation
import java.io.File
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class Watch4SatSettingsStoreSetupAndFiltersTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun setupAlertFreshnessAndFilterSettingsUseExpectedDefaults() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("defaults.preferences_pb"))

        val settings = store.getSettings()

        assertFalse(settings.setupCompleted)
        assertEquals("welcome", settings.setupStep)
        assertEquals(emptySet<String>(), settings.setupSkippedSteps)
        assertEquals(0, settings.passAlertAdvanceMinutes)
        assertTrue(settings.autoDataFreshnessEnabled)
        assertFalse(settings.minimumElevationFilterEnabled)
        assertEquals(10, settings.minimumElevationDegrees)
    }

    @Test
    fun setupProgressPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("setup.preferences_pb"))

        store.setSetupStep("satellites")
        store.setSetupCompleted(true)

        val settings = store.getSettings()
        assertEquals("satellites", settings.setupStep)
        assertTrue(settings.setupCompleted)

        store.setSetupCompleted(false)

        assertFalse(store.getSettings().setupCompleted)
    }

    @Test
    fun eligibleSetupCompletionWritesDoneAndCompletedAtomically() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("setup-complete.preferences_pb"))
        store.setStationLocation(stationLocation())
        store.setSelectedSatelliteIds(setOf(25_544, 43_000))
        val before = store.getSettings()
        val observed = mutableListOf<Watch4SatSettings>()
        val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            store.settings.take(2).toList(observed)
        }
        runCurrent()

        val result = store.completeSetupIfEligible(
            latestAvailableCatalogNumbers = setOf(25_544, 33_591)
        )
        collector.join()
        val after = store.getSettings()

        assertEquals(SetupCompletionResult.Completed, result)
        assertEquals("done", after.setupStep)
        assertTrue(after.setupCompleted)
        assertEquals(listOf(before, after), observed)
        assertFalse(
            observed.any { settings ->
                (settings.setupStep == "done") != settings.setupCompleted
            }
        )
    }

    @Test
    fun eligibleSetupCompletionAcceptsCurrentAndLegacyExplicitTleSkips() = runTest {
        listOf("data", "tle").forEachIndexed { index, tleStepName ->
            val store = Watch4SatSettingsStore(
                testDataStore("setup-complete-skipped-$index.preferences_pb")
            )
            val skippedSteps = setOf(tleStepName, "qth", "satellites")
            store.setSetupSkippedSteps(skippedSteps)

            val result = store.completeSetupIfEligible(
                latestAvailableCatalogNumbers = emptySet()
            )
            val settings = store.getSettings()

            assertEquals(SetupCompletionResult.Completed, result)
            assertEquals("done", settings.setupStep)
            assertTrue(settings.setupCompleted)
            assertEquals(skippedSteps, settings.setupSkippedSteps)
        }
    }

    @Test
    fun ineligibleSetupCompletionRejectsEachMissingRequirementWithoutMutation() = runTest {
        data class Case(
            val name: String,
            val availableCatalogNumbers: Set<Int>,
            val prepare: suspend Watch4SatSettingsStore.() -> Unit,
            val expectedRequirement: SetupCompletionRequirement
        )

        val cases = listOf(
            Case(
                name = "tle",
                availableCatalogNumbers = emptySet(),
                prepare = {
                    setStationLocation(stationLocation())
                    skipSetupStep("satellites")
                },
                expectedRequirement = SetupCompletionRequirement.TLE
            ),
            Case(
                name = "qth",
                availableCatalogNumbers = setOf(25_544),
                prepare = {
                    setSelectedSatelliteIds(setOf(25_544))
                },
                expectedRequirement = SetupCompletionRequirement.QTH
            ),
            Case(
                name = "satellites",
                availableCatalogNumbers = setOf(25_544),
                prepare = {
                    setStationLocation(stationLocation())
                    setSelectedSatelliteIds(setOf(43_000))
                },
                expectedRequirement = SetupCompletionRequirement.SATELLITES
            )
        )

        cases.forEach { case ->
            val store = Watch4SatSettingsStore(
                testDataStore("setup-rejected-${case.name}.preferences_pb")
            )
            case.prepare.invoke(store)
            val before = store.getSettings()

            val result = store.completeSetupIfEligible(case.availableCatalogNumbers)

            assertEquals(
                case.name,
                SetupCompletionResult.Rejected(setOf(case.expectedRequirement)),
                result
            )
            assertEquals(case.name, before, store.getSettings())
        }
    }

    @Test
    fun setupCompletionReadsCurrentQthAndSelectionInsideTheAtomicEdit() = runTest {
        val availableCatalogNumbers = setOf(25_544)
        val qthStore = Watch4SatSettingsStore(
            testDataStore("setup-current-qth.preferences_pb")
        )
        qthStore.setStationLocation(stationLocation())
        qthStore.setSelectedSatelliteIds(availableCatalogNumbers)
        val staleQthSnapshot = qthStore.getSettings()
        assertTrue(staleQthSnapshot.stationLocation != null)
        qthStore.clearStationLocation()
        val qthStateBeforeCompletion = qthStore.getSettings()

        val qthResult = qthStore.completeSetupIfEligible(availableCatalogNumbers)

        assertEquals(
            SetupCompletionResult.Rejected(setOf(SetupCompletionRequirement.QTH)),
            qthResult
        )
        assertEquals(qthStateBeforeCompletion, qthStore.getSettings())

        val selectionStore = Watch4SatSettingsStore(
            testDataStore("setup-current-selection.preferences_pb")
        )
        selectionStore.setStationLocation(stationLocation())
        selectionStore.setSelectedSatelliteIds(availableCatalogNumbers)
        val staleSelectionSnapshot = selectionStore.getSettings()
        assertTrue(
            staleSelectionSnapshot.selectedSatelliteIds
                .any { it in availableCatalogNumbers }
        )
        selectionStore.setSelectedSatelliteIds(setOf(43_000))
        val selectionStateBeforeCompletion = selectionStore.getSettings()

        val selectionResult = selectionStore.completeSetupIfEligible(availableCatalogNumbers)

        assertEquals(
            SetupCompletionResult.Rejected(setOf(SetupCompletionRequirement.SATELLITES)),
            selectionResult
        )
        assertEquals(selectionStateBeforeCompletion, selectionStore.getSettings())
    }

    @Test
    fun alreadyCompletedSetupReturnsTypedResultWithoutChangingPreferences() = runTest {
        val store = Watch4SatSettingsStore(
            testDataStore("setup-already-completed.preferences_pb")
        )
        store.setSetupStep("satellites")
        store.setSetupSkippedSteps(setOf("qth"))
        store.setSetupCompleted(true)
        val before = store.getSettings()

        val result = store.completeSetupIfEligible(
            latestAvailableCatalogNumbers = emptySet()
        )

        assertEquals(SetupCompletionResult.AlreadyCompleted, result)
        assertEquals(before, store.getSettings())
    }

    @Test
    fun setupSkippedStepsPersistThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("setup-skipped.preferences_pb"))

        store.skipSetupStep("data")
        store.skipSetupStep("qth")

        assertEquals(setOf("data", "qth"), store.getSettings().setupSkippedSteps)

        store.clearSetupSkippedStep("data")

        assertEquals(setOf("qth"), store.getSettings().setupSkippedSteps)

        store.setSetupSkippedSteps(emptySet())

        assertEquals(emptySet<String>(), store.getSettings().setupSkippedSteps)
    }

    @Test
    fun setupSkipAndStepAdvancePersistInSingleStoreEdit() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("setup-skip-advance.preferences_pb"))

        store.skipSetupStepAndMoveTo(step = "qth", nextStep = "satellites")

        val settings = store.getSettings()
        assertEquals(setOf("qth"), settings.setupSkippedSteps)
        assertEquals("satellites", settings.setupStep)
    }

    @Test
    fun passAlertAdvancePersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("alerts.preferences_pb"))

        store.setPassAlertAdvanceMinutes(15)

        assertEquals(15, store.getSettings().passAlertAdvanceMinutes)
    }

    @Test
    fun autoDataFreshnessFlagPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("freshness.preferences_pb"))

        store.setAutoDataFreshnessEnabled(false)

        assertFalse(store.getSettings().autoDataFreshnessEnabled)

        store.setAutoDataFreshnessEnabled(true)

        assertTrue(store.getSettings().autoDataFreshnessEnabled)
    }

    @Test
    fun minimumElevationFilterPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("elevation.preferences_pb"))

        store.setMinimumElevationFilterEnabled(true)
        store.setMinimumElevationDegrees(20)

        val settings = store.getSettings()
        assertTrue(settings.minimumElevationFilterEnabled)
        assertEquals(20, settings.minimumElevationDegrees)

        store.setMinimumElevationFilterEnabled(false)

        assertFalse(store.getSettings().minimumElevationFilterEnabled)
    }

    private fun stationLocation(): StationLocation {
        return StationLocation(
            latitude = 22.59,
            longitude = 113.96,
            altitudeMeters = 10.0,
            qthLocator = "OL72AX",
            timestampMillis = 1_000L,
            source = LocationSource.GPS,
            accuracyMeters = 20f
        )
    }

    private fun TestScope.testDataStore(fileName: String): DataStore<Preferences> {
        val file = File(temporaryFolder.root, fileName)
        return PreferenceDataStoreFactory.createWithPath(
            scope = this,
            produceFile = { file.absolutePath.toPath() }
        )
    }
}
