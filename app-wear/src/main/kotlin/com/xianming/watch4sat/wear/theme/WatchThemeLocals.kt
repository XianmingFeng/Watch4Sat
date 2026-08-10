package com.xianming.watch4sat.wear.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.xianming.watch4sat.data.settings.AppThemePreset

val LocalWatchThemeColors = staticCompositionLocalOf {
    WatchThemeCatalog.colorsFor(AppThemePreset.SKY_BLUE)
}
