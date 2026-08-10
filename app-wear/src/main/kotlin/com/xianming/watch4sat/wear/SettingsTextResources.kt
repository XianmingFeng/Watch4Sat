package com.xianming.watch4sat.wear

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.data.settings.MapTileMode

@StringRes
internal fun AppThemePreset.labelResource(): Int = when (this) {
    AppThemePreset.SYSTEM -> R.string.theme_system_color
    AppThemePreset.PIXEL_MINT -> R.string.theme_pixel_mint
    AppThemePreset.SKY_BLUE -> R.string.theme_sky_blue
    AppThemePreset.AURORA_GREEN -> R.string.theme_aurora_green
    AppThemePreset.SOLAR_YELLOW -> R.string.theme_solar_yellow
    AppThemePreset.ROSE_CORAL -> R.string.theme_rose_coral
}

@Composable
internal fun AppThemePreset.localizedLabel(): String = stringResource(labelResource())

@StringRes
internal fun MapTileMode.labelResource(): Int = when (this) {
    MapTileMode.AUTO -> R.string.map_tile_mode_auto
    MapTileMode.OSM_ONLY -> R.string.map_tile_mode_osm_only
    MapTileMode.OFFLINE_WORLD -> R.string.map_tile_mode_offline_world
}

@Composable
internal fun MapTileMode.localizedLabel(): String = stringResource(labelResource())
