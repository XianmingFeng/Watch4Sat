package com.xianming.watch4sat.wear.radar

import androidx.compose.ui.graphics.Color
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.radar.RadarProjection
import com.xianming.watch4sat.wear.TrackDirectionArrow
import com.xianming.watch4sat.wear.TrackDirectionArrowPolicy
import com.xianming.watch4sat.wear.TrackDirectionPoint
import com.xianming.watch4sat.wear.theme.WatchThemeColors

data class RadarCompassLabel(
    val azimuthDegrees: Double,
    val direction: RadarCompassDirection
)

enum class RadarCompassDirection {
    North,
    East,
    South,
    West
}

data class RadarPreparedTrack(
    val drawableTrackPoints: List<RadarTrackPoint>,
    val drawableTrack: List<RadarTrackPoint>,
    val displayTrack: List<RadarDisplayPoint>,
    val labelMarkers: List<RadarTrackPoint>
)

data class RadarPathCommand(
    val x: Float,
    val y: Float
)

data class RadarStaticLabelMarker(
    val label: RadarTrackLabel,
    val x: Float,
    val y: Float
)

data class RadarStaticGeometry(
    val pathCommands: List<RadarPathCommand>,
    val trackOffsets: List<TrackDirectionPoint>,
    val trackArrows: List<TrackDirectionArrow>,
    val labelMarkers: List<RadarStaticLabelMarker>
)

object RadarSkyPlotRenderPolicy {
    const val trajectoryColorRole: String = "mapTrack"
    const val trajectoryAlpha: Float = 0.96f
    const val trajectoryArrowAlpha: Float = 1.0f

    val compassLabels = listOf(
        RadarCompassLabel(0.0, RadarCompassDirection.North),
        RadarCompassLabel(90.0, RadarCompassDirection.East),
        RadarCompassLabel(180.0, RadarCompassDirection.South),
        RadarCompassLabel(270.0, RadarCompassDirection.West)
    )

    fun prepareTrack(
        track: List<RadarTrackPoint>,
        referenceAzimuthDegrees: Double?
    ): RadarPreparedTrack {
        val drawableTrack = RadarProjection.drawableTrack(track)
        return RadarPreparedTrack(
            drawableTrackPoints = drawableTrack,
            drawableTrack = drawableTrack,
            displayTrack = drawableTrack.map { point ->
                radarDisplayPointForAzimuthElevation(
                    azimuthDegrees = point.azimuthDegrees,
                    elevationDegrees = point.elevationDegrees,
                    referenceAzimuthDegrees = referenceAzimuthDegrees
                )
            },
            labelMarkers = RadarProjection.labelMarkers(track)
        )
    }

    fun prepareStaticGeometry(
        preparedTrack: RadarPreparedTrack,
        widthPx: Float,
        heightPx: Float,
        horizonInsetPx: Float,
        trackArrowSizePx: Float,
        trackArrowMinSegmentPx: Float,
        trackArrowMaxSegmentPx: Float,
        referenceAzimuthDegrees: Double?
    ): RadarStaticGeometry {
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f
        val outer = (minOf(widthPx, heightPx) / 2f - horizonInsetPx).coerceAtLeast(0f)

        fun toTrackPoint(azimuthDegrees: Double, elevationDegrees: Double): TrackDirectionPoint {
            val projected = referenceAzimuthDegrees?.let { reference ->
                RadarProjection.projectRelative(
                    azimuthDegrees = azimuthDegrees,
                    elevationDegrees = elevationDegrees,
                    referenceAzimuthDegrees = reference
                )
            } ?: RadarProjection.project(azimuthDegrees, elevationDegrees)
            return TrackDirectionPoint(
                x = centerX + (projected.x * outer).toFloat(),
                y = centerY + (projected.y * outer).toFloat()
            )
        }

        val offsets = preparedTrack.drawableTrack.map { point ->
            toTrackPoint(point.azimuthDegrees, point.elevationDegrees)
        }
        val arrows = TrackDirectionArrowPolicy.arrowsFor(
            points = offsets,
            arrowSizePx = trackArrowSizePx,
            minSegmentLengthPx = trackArrowMinSegmentPx,
            maxSegmentLengthPx = trackArrowMaxSegmentPx
        )
        val markers = preparedTrack.labelMarkers.map { point ->
            val offset = toTrackPoint(point.azimuthDegrees, point.elevationDegrees)
            RadarStaticLabelMarker(
                label = point.label,
                x = offset.x,
                y = offset.y
            )
        }

        return RadarStaticGeometry(
            pathCommands = offsets.map { RadarPathCommand(x = it.x, y = it.y) },
            trackOffsets = offsets,
            trackArrows = arrows,
            labelMarkers = markers
        )
    }

    fun trajectoryColor(colors: WatchThemeColors): Color {
        return colors.mapTrack.copy(alpha = trajectoryAlpha)
    }

    fun trajectoryArrowColor(colors: WatchThemeColors): Color {
        return colors.mapTrack.copy(alpha = trajectoryArrowAlpha)
    }
}
