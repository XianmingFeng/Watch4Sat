package com.xianming.watch4sat.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class MapTileModeContractTest {

    @Test
    fun settingsDefaultThemePresetIsSystemColor() {
        assertEquals(AppThemePreset.SYSTEM, Watch4SatSettings().themePreset)
    }

    @Test
    fun storedThemePresetNamesRoundTripAndUnknownFallsBackToSkyBlue() {
        assertEquals(AppThemePreset.SYSTEM, AppThemePreset.fromStoredName("SYSTEM"))
        assertEquals(AppThemePreset.PIXEL_MINT, AppThemePreset.fromStoredName("PIXEL_MINT"))
        assertEquals(AppThemePreset.SKY_BLUE, AppThemePreset.fromStoredName("SKY_BLUE"))
        assertEquals(AppThemePreset.AURORA_GREEN, AppThemePreset.fromStoredName("AURORA_GREEN"))
        assertEquals(AppThemePreset.SOLAR_YELLOW, AppThemePreset.fromStoredName("SOLAR_YELLOW"))
        assertEquals(AppThemePreset.ROSE_CORAL, AppThemePreset.fromStoredName("ROSE_CORAL"))
        assertEquals(AppThemePreset.SKY_BLUE, AppThemePreset.fromStoredName("unexpected"))
        assertEquals(AppThemePreset.SKY_BLUE, AppThemePreset.fromStoredName(null))
    }

    @Test
    fun settingsDefaultMapTileModeIsAuto() {
        assertEquals(MapTileMode.AUTO, Watch4SatSettings().mapTileMode)
    }

    @Test
    fun storedMapTileModeNamesRoundTripAndUnknownFallsBackToAuto() {
        assertEquals(MapTileMode.AUTO, MapTileMode.fromStoredName("AUTO"))
        assertEquals(MapTileMode.OSM_ONLY, MapTileMode.fromStoredName("OSM_ONLY"))
        assertEquals(MapTileMode.OFFLINE_WORLD, MapTileMode.fromStoredName("OFFLINE_WORLD"))
        assertEquals(MapTileMode.AUTO, MapTileMode.fromStoredName("unexpected"))
        assertEquals(MapTileMode.AUTO, MapTileMode.fromStoredName(null))
    }
}
