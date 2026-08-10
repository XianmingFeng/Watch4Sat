package com.xianming.watch4sat.wear.theme

import androidx.wear.compose.material3.ColorScheme
import com.xianming.watch4sat.data.settings.AppThemePreset

data class ResolvedWatchTheme(
    val watchColors: WatchThemeColors,
    val wearColorScheme: ColorScheme,
    val source: String
)

object WatchThemeResolver {
    fun resolve(
        preset: AppThemePreset,
        dynamicColorScheme: ColorScheme?
    ): ResolvedWatchTheme {
        return if (preset == AppThemePreset.SYSTEM) {
            if (dynamicColorScheme != null) {
                ResolvedWatchTheme(
                    watchColors = WatchThemeCatalog.colorsFrom(dynamicColorScheme),
                    wearColorScheme = dynamicColorScheme,
                    source = "DYNAMIC_SYSTEM"
                )
            } else {
                val fallback = WatchThemeCatalog.colorsFor(AppThemePreset.SKY_BLUE)
                ResolvedWatchTheme(
                    watchColors = fallback,
                    wearColorScheme = WatchThemeCatalog.wearColorSchemeFor(fallback),
                    source = "SKY_BLUE_FALLBACK"
                )
            }
        } else {
            val colors = WatchThemeCatalog.colorsFor(preset)
            ResolvedWatchTheme(
                watchColors = colors,
                wearColorScheme = WatchThemeCatalog.wearColorSchemeFor(colors),
                source = "STATIC_PRESET"
            )
        }
    }
}
