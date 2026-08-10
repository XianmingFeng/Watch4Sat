package com.xianming.watch4sat.wear.map

import androidx.compose.ui.graphics.Color
import com.xianming.watch4sat.wear.theme.WatchThemeColors

data class MapOverlayStyle(
    val crosshair: MapCrosshairStyle,
    val station: MapMarkerStyle,
    val satellite: MapMarkerStyle,
    val track: MapTrackStyle,
    val footprint: MapFootprintStyle,
    val arrow: MapArrowStyle
) {
    companion object {
        fun from(colors: WatchThemeColors): MapOverlayStyle {
            return MapOverlayStyle(
                crosshair = MapCrosshairStyle(
                    shadowColor = Color.Black,
                    shadowAlpha = 0.70f,
                    foregroundColor = Color.White,
                    armPx = 12f,
                    gapPx = 4f,
                    shadowStrokePx = 4f,
                    foregroundStrokePx = 2f,
                    shadowDotRadiusPx = 3.5f,
                    foregroundDotRadiusPx = 2f
                ),
                station = MapMarkerStyle(
                    outerColor = colors.surface,
                    outerAlpha = 0.86f,
                    innerColor = colors.mapStation,
                    outerRadiusPx = 6f,
                    innerRadiusPx = 4f
                ),
                satellite = MapMarkerStyle(
                    outerColor = colors.surface,
                    outerAlpha = 0.88f,
                    innerColor = colors.mapSatellite,
                    outerRadiusPx = 8f,
                    innerRadiusPx = 5f
                ),
                track = MapTrackStyle(
                    color = colors.mapTrack,
                    strokePx = 3f
                ),
                footprint = MapFootprintStyle(
                    fillColor = colors.primary,
                    fillAlpha = 0.18f,
                    outlineColor = colors.primary,
                    outlineAlpha = 0.72f,
                    outlineStrokePx = 1.4f
                ),
                arrow = MapArrowStyle(
                    color = colors.mapTrack,
                    strokePx = 2.4f
                )
            )
        }
    }
}

data class MapCrosshairStyle(
    val shadowColor: Color,
    val shadowAlpha: Float,
    val foregroundColor: Color,
    val armPx: Float,
    val gapPx: Float,
    val shadowStrokePx: Float,
    val foregroundStrokePx: Float,
    val shadowDotRadiusPx: Float,
    val foregroundDotRadiusPx: Float
)

data class MapMarkerStyle(
    val outerColor: Color,
    val outerAlpha: Float,
    val innerColor: Color,
    val outerRadiusPx: Float,
    val innerRadiusPx: Float
)

data class MapTrackStyle(
    val color: Color,
    val strokePx: Float
)

data class MapFootprintStyle(
    val fillColor: Color,
    val fillAlpha: Float,
    val outlineColor: Color,
    val outlineAlpha: Float,
    val outlineStrokePx: Float
)

data class MapArrowStyle(
    val color: Color,
    val strokePx: Float
)
