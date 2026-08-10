package com.xianming.watch4sat.data.settings

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class Watch4SatSettingsStoreThemeTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun themePresetPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("settings.preferences_pb"))

        assertEquals(AppThemePreset.SYSTEM, store.getSettings().themePreset)

        store.setThemePreset(AppThemePreset.ROSE_CORAL)

        assertEquals(AppThemePreset.ROSE_CORAL, store.getSettings().themePreset)

        store.setThemePreset(AppThemePreset.SYSTEM)

        assertEquals(AppThemePreset.SYSTEM, store.getSettings().themePreset)
    }

    @Test
    fun invalidStoredThemePresetFallsBackToSkyBlue() = runTest {
        val dataStore = testDataStore("invalid_theme.preferences_pb")
        val store = Watch4SatSettingsStore(dataStore)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_preset")] = "unexpected"
        }

        assertEquals(AppThemePreset.SKY_BLUE, store.getSettings().themePreset)
    }

    @Test
    fun developerOptionsFlagPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("developer_options.preferences_pb"))

        assertFalse(store.getSettings().developerOptionsEnabled)

        store.setDeveloperOptionsEnabled(true)

        assertTrue(store.getSettings().developerOptionsEnabled)

        store.setDeveloperOptionsEnabled(false)

        assertFalse(store.getSettings().developerOptionsEnabled)
    }

    @Test
    fun radarKeepScreenOnFlagPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("radar_keep_screen_on.preferences_pb"))

        assertFalse(store.getSettings().radarKeepScreenOn)

        store.setRadarKeepScreenOn(true)

        assertTrue(store.getSettings().radarKeepScreenOn)

        store.setRadarKeepScreenOn(false)

        assertFalse(store.getSettings().radarKeepScreenOn)
    }

    @Test
    fun radarForwardAxisDefaultsToScreenTopAndPersistsThroughSettingsStore() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("radar_forward_axis.preferences_pb"))

        assertEquals(RadarForwardAxis.SCREEN_TOP, store.getSettings().radarForwardAxis)

        store.setRadarForwardAxis(RadarForwardAxis.TOWARD_HAND)

        assertEquals(RadarForwardAxis.TOWARD_HAND, store.getSettings().radarForwardAxis)

        store.setRadarForwardAxis(RadarForwardAxis.SCREEN_TOP)

        assertEquals(RadarForwardAxis.SCREEN_TOP, store.getSettings().radarForwardAxis)
    }

    @Test
    fun invalidStoredRadarForwardAxisFallsBackToScreenTop() = runTest {
        val dataStore = testDataStore("invalid_radar_forward_axis.preferences_pb")
        val store = Watch4SatSettingsStore(dataStore)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("radar_forward_axis")] = "unexpected"
        }

        assertEquals(RadarForwardAxis.SCREEN_TOP, store.getSettings().radarForwardAxis)
    }

    @Test
    fun legacyScreenRightRadarForwardAxisMigratesToTowardHand() = runTest {
        val dataStore = testDataStore("legacy_radar_forward_axis.preferences_pb")
        val store = Watch4SatSettingsStore(dataStore)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("radar_forward_axis")] = "SCREEN_RIGHT"
        }

        assertEquals(RadarForwardAxis.TOWARD_HAND, store.getSettings().radarForwardAxis)
    }

    @Test
    fun radarFallbackWristSideDefaultsLeftAndPersists() = runTest {
        val store = Watch4SatSettingsStore(testDataStore("radar_fallback_wrist.preferences_pb"))

        assertEquals(RadarWristSide.LEFT, store.getSettings().radarFallbackWristSide)

        store.setRadarFallbackWristSide(RadarWristSide.RIGHT)
        assertEquals(RadarWristSide.RIGHT, store.getSettings().radarFallbackWristSide)

        store.setRadarFallbackWristSide(RadarWristSide.LEFT)
        assertEquals(RadarWristSide.LEFT, store.getSettings().radarFallbackWristSide)
    }

    @Test
    fun invalidStoredRadarFallbackWristSideFallsBackLeft() = runTest {
        val dataStore = testDataStore("invalid_radar_fallback_wrist.preferences_pb")
        val store = Watch4SatSettingsStore(dataStore)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("radar_fallback_wrist_side")] = "unexpected"
        }

        assertEquals(RadarWristSide.LEFT, store.getSettings().radarFallbackWristSide)
    }

    private fun TestScope.testDataStore(fileName: String): DataStore<Preferences> {
        val file = File(temporaryFolder.root, fileName)
        return PreferenceDataStoreFactory.createWithPath(
            scope = this,
            produceFile = { file.absolutePath.toPath() }
        )
    }
}
