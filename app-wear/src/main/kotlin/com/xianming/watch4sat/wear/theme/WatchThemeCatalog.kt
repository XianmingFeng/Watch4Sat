package com.xianming.watch4sat.wear.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import com.xianming.watch4sat.data.settings.AppThemePreset

data class WatchThemeColors(
    val appBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
    val mutedText: Color,
    val mapSea: Color,
    val mapLand: Color,
    val mapGrid: Color,
    val mapTrack: Color,
    val mapSatellite: Color,
    val mapStation: Color,
    val mapLandStrokeAlpha: Float = 0.10f
)

object WatchThemeCatalog {
    fun colorsFor(preset: AppThemePreset): WatchThemeColors {
        return when (preset) {
            AppThemePreset.SYSTEM -> colorsFor(AppThemePreset.SKY_BLUE)
            AppThemePreset.PIXEL_MINT -> WatchThemeColors(
                appBackground = Color(0xFF06110F),
                surface = Color(0xFF10201C),
                surfaceVariant = Color(0xFF19332D),
                primary = Color(0xFFA7F3D0),
                onPrimary = Color.Black,
                secondary = Color(0xFF80CBC4),
                tertiary = Color(0xFFE7F8B7),
                error = Color(0xFFFFB4AB),
                mutedText = Color(0xFFC2DCD4),
                mapSea = Color(0xFF071410),
                mapLand = Color(0xFF2D564B),
                mapGrid = Color(0xFF7FB4A6),
                mapTrack = Color(0xFF8EECD1),
                mapSatellite = Color(0xFFFFD166),
                mapStation = Color(0xFFA7F3D0)
            )

            AppThemePreset.SKY_BLUE -> WatchThemeColors(
                appBackground = Color(0xFF061018),
                surface = Color(0xFF102232),
                surfaceVariant = Color(0xFF17364C),
                primary = Color(0xFF9AD7FF),
                onPrimary = Color.Black,
                secondary = Color(0xFF89CFF0),
                tertiary = Color(0xFFC7E7FF),
                error = Color(0xFFFFB4AB),
                mutedText = Color(0xFFC8D9E8),
                mapSea = Color(0xFF06131D),
                mapLand = Color(0xFF25485B),
                mapGrid = Color(0xFF83BDE0),
                mapTrack = Color(0xFF9AD7FF),
                mapSatellite = Color(0xFFFFD166),
                mapStation = Color(0xFFB6E3FF)
            )

            AppThemePreset.AURORA_GREEN -> WatchThemeColors(
                appBackground = Color(0xFF071208),
                surface = Color(0xFF132316),
                surfaceVariant = Color(0xFF1E3A24),
                primary = Color(0xFFB8F397),
                onPrimary = Color.Black,
                secondary = Color(0xFF8CE99A),
                tertiary = Color(0xFFD8F8C0),
                error = Color(0xFFFFB4AB),
                mutedText = Color(0xFFD3E6CC),
                mapSea = Color(0xFF071408),
                mapLand = Color(0xFF315A35),
                mapGrid = Color(0xFF9ECD91),
                mapTrack = Color(0xFFB8F397),
                mapSatellite = Color(0xFFFFD166),
                mapStation = Color(0xFFB8F397)
            )

            AppThemePreset.SOLAR_YELLOW -> WatchThemeColors(
                appBackground = Color(0xFF141006),
                surface = Color(0xFF2A2110),
                surfaceVariant = Color(0xFF413418),
                primary = Color(0xFFFFD166),
                onPrimary = Color.Black,
                secondary = Color(0xFFEEC35D),
                tertiary = Color(0xFFFFE7A3),
                error = Color(0xFFFFB4AB),
                mutedText = Color(0xFFE7D8B4),
                mapSea = Color(0xFF141006),
                mapLand = Color(0xFF5D4B22),
                mapGrid = Color(0xFFC9A95D),
                mapTrack = Color(0xFFFFD166),
                mapSatellite = Color(0xFFFFF0B3),
                mapStation = Color(0xFFFFD166)
            )

            AppThemePreset.ROSE_CORAL -> WatchThemeColors(
                appBackground = Color(0xFF160B10),
                surface = Color(0xFF2A151D),
                surfaceVariant = Color(0xFF44202B),
                primary = Color(0xFFFFB1C8),
                onPrimary = Color.Black,
                secondary = Color(0xFFFF8FAB),
                tertiary = Color(0xFFFFD3DD),
                error = Color(0xFFFFB4AB),
                mutedText = Color(0xFFE8CAD2),
                mapSea = Color(0xFF150B10),
                mapLand = Color(0xFF5E3441),
                mapGrid = Color(0xFFD28A9B),
                mapTrack = Color(0xFFFFB1C8),
                mapSatellite = Color(0xFFFFD166),
                mapStation = Color(0xFFFFB1C8)
            )
        }
    }

    fun colorsFrom(scheme: ColorScheme): WatchThemeColors {
        return WatchThemeColors(
            appBackground = Color.Black,
            surface = scheme.surfaceContainer,
            surfaceVariant = scheme.surfaceContainerHigh,
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            secondary = scheme.secondary,
            tertiary = scheme.tertiary,
            error = scheme.error,
            mutedText = scheme.onSurfaceVariant,
            mapSea = Color.Black,
            mapLand = scheme.surfaceContainer,
            mapGrid = scheme.outlineVariant,
            mapTrack = scheme.primary,
            mapSatellite = scheme.tertiary,
            mapStation = scheme.primary,
            mapLandStrokeAlpha = 0.10f
        )
    }

    fun wearColorSchemeFor(colors: WatchThemeColors): ColorScheme {
        return ColorScheme(
            primary = colors.primary,
            primaryDim = colors.primary.copy(alpha = 0.76f),
            primaryContainer = colors.surfaceVariant,
            onPrimary = colors.onPrimary,
            onPrimaryContainer = Color.White,
            secondary = colors.secondary,
            secondaryDim = colors.secondary.copy(alpha = 0.76f),
            secondaryContainer = colors.surfaceVariant,
            onSecondary = Color.Black,
            onSecondaryContainer = Color.White,
            tertiary = colors.tertiary,
            tertiaryDim = colors.tertiary.copy(alpha = 0.76f),
            tertiaryContainer = colors.surfaceVariant,
            onTertiary = Color.Black,
            onTertiaryContainer = Color.White,
            surfaceContainerLow = colors.surface.copy(alpha = 0.82f),
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surfaceVariant,
            onSurface = Color.White,
            onSurfaceVariant = colors.mutedText,
            outline = colors.mutedText,
            outlineVariant = colors.mutedText.copy(alpha = 0.45f),
            background = Color.Black,
            onBackground = Color.White,
            error = WatchSemanticColors.ErrorForeground,
            errorDim = WatchSemanticColors.ErrorForeground.copy(alpha = 0.76f),
            errorContainer = WatchSemanticColors.ErrorContainer,
            onError = Color.Black,
            onErrorContainer = WatchSemanticColors.OnErrorContainer
        )
    }
}
