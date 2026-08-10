package com.xianming.watch4sat.wear.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.wear.compose.material3.MaterialTheme
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.radar.RadarProjection
import com.xianming.watch4sat.wear.TrackDirectionArrow
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.theme.WatchThemeColors

private const val RadarCueEnterExitTweenMillis = 360

private data class MeasuredCompassLabel(
    val label: RadarCompassLabel,
    val text: TextLayoutResult
)

@Composable
fun RadarSkyPlot(
    track: List<RadarTrackPoint>,
    position: OrbitalPosition?,
    displayPosition: RadarDisplayPoint?,
    pointing: RadarPointing?,
    referenceAzimuthDegrees: Double?,
    colors: WatchThemeColors,
    updateMode: RadarUpdateMode,
    modifier: Modifier = Modifier,
    visualCueEnabled: Boolean = true,
    visualCueOverride: RadarVisualCue? = null
) {
    val density = LocalDensity.current
    val horizonInsetPx = with(density) { WatchUiMetrics.RadarHorizonRingInset.toPx() }
    val outerStrokePx = with(density) { WatchUiMetrics.RadarOuterGridStrokeWidth.toPx() }
    val innerStrokePx = with(density) { WatchUiMetrics.RadarInnerGridStrokeWidth.toPx() }
    val trajectoryStrokePx = with(density) { WatchUiMetrics.RadarTrajectoryStrokeWidth.toPx() }
    val trajectoryArrowStrokePx = with(density) { WatchUiMetrics.RadarTrajectoryArrowStrokeWidth.toPx() }
    val labelInsetPx = with(density) { WatchUiMetrics.RadarCompassLabelInset.toPx() }
    val trackArrowSizePx = with(density) { WatchUiMetrics.RadarTrackArrowSize.toPx() }
    val trackArrowMinSegmentPx = with(density) { WatchUiMetrics.RadarTrackArrowMinSegmentLength.toPx() }
    val trackArrowMaxSegmentPx = with(density) { WatchUiMetrics.RadarTrackArrowMaxSegmentLength.toPx() }
    val reticleCueRadiusPx = with(density) { WatchUiMetrics.RadarReticleCueRadius.toPx() }
    val satelliteCueRadiusPx = with(density) { WatchUiMetrics.RadarSatelliteCueRadius.toPx() }
    val satelliteCueCoreRadiusPx = with(density) { WatchUiMetrics.RadarSatelliteCueCoreRadius.toPx() }
    val labelTextMeasurer = rememberTextMeasurer()
    val compassLabelStyle = MaterialTheme.typography.labelSmall
    val compassText = mapOf(
        RadarCompassDirection.North to stringResource(R.string.radar_compass_north_short),
        RadarCompassDirection.East to stringResource(R.string.radar_compass_east_short),
        RadarCompassDirection.South to stringResource(R.string.radar_compass_south_short),
        RadarCompassDirection.West to stringResource(R.string.radar_compass_west_short)
    )
    val preparedTrack = remember(track, referenceAzimuthDegrees) {
        RadarSkyPlotRenderPolicy.prepareTrack(
            track = track,
            referenceAzimuthDegrees = referenceAzimuthDegrees
        )
    }
    val displayTrack = preparedTrack.displayTrack
    val reticleDisplayPoint = remember(pointing, referenceAzimuthDegrees) {
        pointing?.let {
            radarDisplayPointForAzimuthElevation(
                azimuthDegrees = it.azimuthDegrees,
                elevationDegrees = it.elevationDegrees,
                referenceAzimuthDegrees = referenceAzimuthDegrees
            )
        }
    }
    val labelStyle = compassLabelStyle.copy(
        color = colors.primary.copy(alpha = 0.86f),
    )
    val measuredCompassLabels = remember(labelTextMeasurer, labelStyle, compassText) {
        RadarSkyPlotRenderPolicy.compassLabels.map { label ->
            MeasuredCompassLabel(
                label = label,
                text = labelTextMeasurer.measure(
                    checkNotNull(compassText[label.direction]),
                    style = labelStyle
                )
            )
        }
    }
    val cue = remember(
        visualCueOverride,
        reticleDisplayPoint,
        displayPosition,
        displayTrack,
        updateMode
    ) {
        visualCueOverride ?: RadarVisualCuePolicy.cueFor(
            reticle = reticleDisplayPoint,
            satellite = displayPosition,
            track = displayTrack,
            updateMode = updateMode
        )
    }
    val animatedCue = if (visualCueEnabled) cue else RadarVisualCue(RadarVisualCueState.None, false)
    val cueTransition = updateTransition(
        targetState = animatedCue.state,
        label = "Radar visual cue"
    )
    val cueAnimationsEnabled = visualCueEnabled && updateMode != RadarUpdateMode.AmbientOneHz
    val cueTransitionSpec = if (cueAnimationsEnabled) {
        tween<Float>(durationMillis = RadarCueEnterExitTweenMillis, easing = FastOutSlowInEasing)
    } else {
        snap()
    }
    val activeCuePulseAlpha: Float
    val activeCuePulseRadiusScale: Float
    if (animatedCue.animated && cueAnimationsEnabled) {
        val cuePulse = rememberInfiniteTransition(label = "Radar visual cue pulse")
        val cuePulseAlpha by cuePulse.animateFloat(
            initialValue = 0.72f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cue pulse alpha"
        )
        val cuePulseRadiusScale by cuePulse.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cue pulse radius"
        )
        activeCuePulseAlpha = cuePulseAlpha
        activeCuePulseRadiusScale = cuePulseRadiusScale
    } else {
        activeCuePulseAlpha = 1f
        activeCuePulseRadiusScale = 1f
    }
    val cuePresence by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "cue presence"
    ) { state ->
        if (state == RadarVisualCueState.None) 0f else 1f
    }
    val cueLevel by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "cue level"
    ) { state ->
        when (state) {
            RadarVisualCueState.None -> 0f
            RadarVisualCueState.TrackAligned -> 0.72f
            RadarVisualCueState.SatelliteAligned -> 1f
        }
    }
    val cueEnterExitScale by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "cue enter exit scale"
    ) { state ->
        if (state == RadarVisualCueState.None) 0.84f else 1f
    }
    val satelliteCuePulseWeight by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "satellite cue pulse weight"
    ) { state ->
        if (state == RadarVisualCueState.SatelliteAligned) 1f else 0f
    }
    val reticleRingPulseAlpha =
        1f + ((activeCuePulseAlpha - 1f) * satelliteCuePulseWeight)
    val reticleRingPulseRadius =
        15f * (1f + ((activeCuePulseRadiusScale - 1f) * satelliteCuePulseWeight))
    val reticleRingPulseStrokeWidth =
        2f * (1f + ((activeCuePulseRadiusScale - 1f) * satelliteCuePulseWeight))
    val reticleGlowAlpha by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "reticle glow alpha"
    ) { state ->
        when (state) {
            RadarVisualCueState.None -> 0f
            RadarVisualCueState.TrackAligned -> 0.22f
            RadarVisualCueState.SatelliteAligned -> 0.30f
        }
    }
    val satelliteGlowAlpha by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "satellite glow alpha"
    ) { state ->
        if (state == RadarVisualCueState.SatelliteAligned) 0.34f else 0f
    }
    val reticleRingAlpha by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "reticle ring alpha"
    ) { state ->
        when (state) {
            RadarVisualCueState.None -> 0.30f
            RadarVisualCueState.TrackAligned -> 0.66f
            RadarVisualCueState.SatelliteAligned -> 0.82f
        }
    }
    val reticleRingCueAlpha by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "reticle ring cue alpha"
    ) { state ->
        when (state) {
            RadarVisualCueState.None -> 0f
            RadarVisualCueState.TrackAligned -> 0.36f
            RadarVisualCueState.SatelliteAligned -> 0.52f
        }
    }
    val satelliteCoreAlpha by cueTransition.animateFloat(
        transitionSpec = { cueTransitionSpec },
        label = "satellite core alpha"
    ) { state ->
        if (state == RadarVisualCueState.SatelliteAligned) 0.28f else 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        RadarSkyPlotStaticLayer(
            preparedTrack = preparedTrack,
            referenceAzimuthDegrees = referenceAzimuthDegrees,
            colors = colors,
            horizonInsetPx = horizonInsetPx,
            outerStrokePx = outerStrokePx,
            innerStrokePx = innerStrokePx,
            trajectoryStrokePx = trajectoryStrokePx,
            trajectoryArrowStrokePx = trajectoryArrowStrokePx,
            labelInsetPx = labelInsetPx,
            trackArrowSizePx = trackArrowSizePx,
            trackArrowMinSegmentPx = trackArrowMinSegmentPx,
            trackArrowMaxSegmentPx = trackArrowMaxSegmentPx,
            measuredCompassLabels = measuredCompassLabels,
            modifier = Modifier.matchParentSize()
        )
        RadarSkyPlotDynamicLayer(
            displayPosition = displayPosition,
            pointing = pointing,
            reticleDisplayPoint = reticleDisplayPoint,
            colors = colors,
            visualCueEnabled = visualCueEnabled,
            reticleGlowAlpha = reticleGlowAlpha,
            satelliteGlowAlpha = satelliteGlowAlpha,
            satelliteCoreAlpha = satelliteCoreAlpha,
            cuePresence = cuePresence,
            cueEnterExitScale = cueEnterExitScale,
            activeCuePulseAlpha = activeCuePulseAlpha,
            activeCuePulseRadiusScale = activeCuePulseRadiusScale,
            reticleRingAlpha = reticleRingAlpha,
            reticleRingCueAlpha = reticleRingCueAlpha,
            cueLevel = cueLevel,
            reticleRingPulseAlpha = reticleRingPulseAlpha,
            reticleRingPulseRadius = reticleRingPulseRadius,
            reticleRingPulseStrokeWidth = reticleRingPulseStrokeWidth,
            horizonInsetPx = horizonInsetPx,
            reticleCueRadiusPx = reticleCueRadiusPx,
            satelliteCueRadiusPx = satelliteCueRadiusPx,
            satelliteCueCoreRadiusPx = satelliteCueCoreRadiusPx,
            referenceAzimuthDegrees = referenceAzimuthDegrees,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun RadarSkyPlotStaticLayer(
    preparedTrack: RadarPreparedTrack,
    referenceAzimuthDegrees: Double?,
    colors: WatchThemeColors,
    horizonInsetPx: Float,
    outerStrokePx: Float,
    innerStrokePx: Float,
    trajectoryStrokePx: Float,
    trajectoryArrowStrokePx: Float,
    labelInsetPx: Float,
    trackArrowSizePx: Float,
    trackArrowMinSegmentPx: Float,
    trackArrowMaxSegmentPx: Float,
    measuredCompassLabels: List<MeasuredCompassLabel>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawWithCache {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = (size.minDimension / 2f - horizonInsetPx).coerceAtLeast(0f)
            val gridOuter = colors.primary.copy(alpha = 0.42f)
            val gridInner = colors.primary.copy(alpha = 0.24f)
            val trackColor = RadarSkyPlotRenderPolicy.trajectoryColor(colors)
            val arrowColor = RadarSkyPlotRenderPolicy.trajectoryArrowColor(colors)
            val geometry = RadarSkyPlotRenderPolicy.prepareStaticGeometry(
                preparedTrack = preparedTrack,
                widthPx = size.width,
                heightPx = size.height,
                horizonInsetPx = horizonInsetPx,
                trackArrowSizePx = trackArrowSizePx,
                trackArrowMinSegmentPx = trackArrowMinSegmentPx,
                trackArrowMaxSegmentPx = trackArrowMaxSegmentPx,
                referenceAzimuthDegrees = referenceAzimuthDegrees
            )
            val trackPath = Path().apply {
                geometry.pathCommands.forEachIndexed { index, command ->
                    if (index == 0) {
                        moveTo(command.x, command.y)
                    } else {
                        lineTo(command.x, command.y)
                    }
                }
            }
            val labelRadiusFraction = ((outer - labelInsetPx).coerceAtLeast(0f) / outer.coerceAtLeast(1f)).toDouble()
            val labelElevation = (1.0 - labelRadiusFraction) * 90.0
            val labelPlacements = measuredCompassLabels.map { measuredLabel ->
                val labelOffset = radarOffset(
                    center = center,
                    outer = outer,
                    azimuthDegrees = measuredLabel.label.azimuthDegrees,
                    elevationDegrees = labelElevation,
                    referenceAzimuthDegrees = referenceAzimuthDegrees
                )
                val labelRotation = RadarProjection.relativeAzimuth(
                    azimuthDegrees = measuredLabel.label.azimuthDegrees,
                    referenceAzimuthDegrees = referenceAzimuthDegrees
                ) + 180.0
                Triple(measuredLabel, labelOffset, labelRotation.toFloat())
            }
            onDrawBehind {
                drawCircle(gridOuter, outer, center, style = Stroke(width = outerStrokePx))
                drawCircle(gridInner, outer * 0.66f, center, style = Stroke(width = innerStrokePx))
                drawCircle(gridInner, outer * 0.33f, center, style = Stroke(width = innerStrokePx))
                drawLine(
                    gridInner,
                    radarOffset(center, outer, 0.0, 0.0, referenceAzimuthDegrees),
                    radarOffset(center, outer, 180.0, 0.0, referenceAzimuthDegrees),
                    strokeWidth = innerStrokePx
                )
                drawLine(
                    gridInner,
                    radarOffset(center, outer, 90.0, 0.0, referenceAzimuthDegrees),
                    radarOffset(center, outer, 270.0, 0.0, referenceAzimuthDegrees),
                    strokeWidth = innerStrokePx
                )

                if (geometry.pathCommands.size >= 2) {
                    drawPath(
                        trackPath,
                        trackColor,
                        style = Stroke(width = trajectoryStrokePx)
                    )
                }

                geometry.trackArrows.forEach { arrow ->
                    drawTrackDirectionArrow(
                        arrow = arrow,
                        color = arrowColor,
                        strokeWidth = trajectoryArrowStrokePx
                    )
                }

                labelPlacements.forEach { (measuredLabel, labelOffset, labelRotation) ->
                    rotate(degrees = labelRotation, pivot = labelOffset) {
                        drawText(
                            measuredLabel.text,
                            topLeft = Offset(
                                x = labelOffset.x - measuredLabel.text.size.width / 2f,
                                y = labelOffset.y - measuredLabel.text.size.height / 2f
                            )
                        )
                    }
                }

                geometry.labelMarkers.forEach { marker ->
                    val offset = Offset(marker.x, marker.y)
                    when (marker.label) {
                        RadarTrackLabel.AOS -> drawCircle(colors.primary, radius = 6f, center = offset)
                        RadarTrackLabel.LOS -> drawCircle(colors.error, radius = 6f, center = offset)
                        RadarTrackLabel.NONE -> Unit
                    }
                }
            }
        }
    )
}

@Composable
private fun RadarSkyPlotDynamicLayer(
    displayPosition: RadarDisplayPoint?,
    pointing: RadarPointing?,
    reticleDisplayPoint: RadarDisplayPoint?,
    colors: WatchThemeColors,
    visualCueEnabled: Boolean,
    reticleGlowAlpha: Float,
    satelliteGlowAlpha: Float,
    satelliteCoreAlpha: Float,
    cuePresence: Float,
    cueEnterExitScale: Float,
    activeCuePulseAlpha: Float,
    activeCuePulseRadiusScale: Float,
    reticleRingAlpha: Float,
    reticleRingCueAlpha: Float,
    cueLevel: Float,
    reticleRingPulseAlpha: Float,
    reticleRingPulseRadius: Float,
    reticleRingPulseStrokeWidth: Float,
    horizonInsetPx: Float,
    reticleCueRadiusPx: Float,
    satelliteCueRadiusPx: Float,
    satelliteCueCoreRadiusPx: Float,
    referenceAzimuthDegrees: Double?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = (size.minDimension / 2f - horizonInsetPx).coerceAtLeast(0f)

        fun toOffset(azimuthDegrees: Double, elevationDegrees: Double): Offset {
            val projected = referenceAzimuthDegrees?.let { reference ->
                RadarProjection.projectRelative(
                    azimuthDegrees = azimuthDegrees,
                    elevationDegrees = elevationDegrees,
                    referenceAzimuthDegrees = reference
                )
            } ?: RadarProjection.project(azimuthDegrees, elevationDegrees)
            return Offset(
                x = center.x + (projected.x * outer).toFloat(),
                y = center.y + (projected.y * outer).toFloat()
            )
        }

        if (visualCueEnabled && reticleGlowAlpha > 0.001f) {
            reticleDisplayPoint?.let { reticle ->
                val reticleOffset = Offset(
                    x = center.x + reticle.x * outer,
                    y = center.y + reticle.y * outer
                )
                drawCircle(
                    colors.primary.copy(alpha = reticleGlowAlpha * cuePresence * activeCuePulseAlpha),
                    radius = reticleCueRadiusPx * cueEnterExitScale * activeCuePulseRadiusScale,
                    center = reticleOffset
                )
            }
        }
        if (visualCueEnabled && (satelliteGlowAlpha > 0.001f || satelliteCoreAlpha > 0.001f)) {
            displayPosition?.let { display ->
                val dot = Offset(
                    x = center.x + display.x * outer,
                    y = center.y + display.y * outer
                )
                drawCircle(
                    colors.mapSatellite.copy(alpha = satelliteGlowAlpha * cuePresence * activeCuePulseAlpha),
                    radius = satelliteCueRadiusPx * cueEnterExitScale * activeCuePulseRadiusScale,
                    center = dot
                )
                drawCircle(
                    colors.mapSatellite.copy(alpha = satelliteCoreAlpha * cuePresence * activeCuePulseAlpha),
                    radius = satelliteCueCoreRadiusPx * cueEnterExitScale,
                    center = dot
                )
            }
        }

        displayPosition?.let { display ->
            val dot = Offset(
                x = center.x + display.x * outer,
                y = center.y + display.y * outer
            )
            drawCircle(colors.mapSatellite, radius = 9f, center = dot)
            drawCircle(Color.Black.copy(alpha = 0.58f), radius = 3.5f, center = dot)
        }

        pointing?.let {
            val reticle = toOffset(it.azimuthDegrees, it.elevationDegrees)
            drawCircle(
                colors.primary.copy(alpha = reticleRingAlpha),
                radius = 15f,
                center = reticle,
                style = Stroke(width = 2f)
            )
            drawCircle(
                colors.primary.copy(alpha = reticleRingCueAlpha * cueLevel * reticleRingPulseAlpha),
                radius = reticleRingPulseRadius * cueEnterExitScale,
                center = reticle,
                style = Stroke(width = reticleRingPulseStrokeWidth * cueEnterExitScale)
            )
            drawCircle(colors.primary, radius = 3.5f, center = reticle)
        }
    }
}

fun radarDisplayPointForPosition(position: OrbitalPosition?): RadarDisplayPoint? {
    if (
        position == null ||
        !RadarProjection.isSatelliteVisible(position.elevationDegrees, position.aboveHorizon)
    ) {
        return null
    }
    val projected = RadarProjection.project(
        azimuthDegrees = position.azimuthDegrees,
        elevationDegrees = position.elevationDegrees
    )
    return RadarDisplayPoint(
        x = projected.x.toFloat(),
        y = projected.y.toFloat()
    )
}

internal fun radarDisplayPointForAzimuthElevation(
    azimuthDegrees: Double,
    elevationDegrees: Double,
    referenceAzimuthDegrees: Double?
): RadarDisplayPoint {
    val projected = referenceAzimuthDegrees?.let { reference ->
        RadarProjection.projectRelative(
            azimuthDegrees = azimuthDegrees,
            elevationDegrees = elevationDegrees,
            referenceAzimuthDegrees = reference
        )
    } ?: RadarProjection.project(azimuthDegrees, elevationDegrees)
    return RadarDisplayPoint(
        x = projected.x.toFloat(),
        y = projected.y.toFloat()
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrackDirectionArrow(
    arrow: TrackDirectionArrow,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = Offset(arrow.left.x, arrow.left.y),
        end = Offset(arrow.tip.x, arrow.tip.y),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(arrow.right.x, arrow.right.y),
        end = Offset(arrow.tip.x, arrow.tip.y),
        strokeWidth = strokeWidth
    )
}

private fun radarOffset(
    center: Offset,
    outer: Float,
    azimuthDegrees: Double,
    elevationDegrees: Double,
    referenceAzimuthDegrees: Double?
): Offset {
    val projected = referenceAzimuthDegrees?.let { reference ->
        RadarProjection.projectRelative(
            azimuthDegrees = azimuthDegrees,
            elevationDegrees = elevationDegrees,
            referenceAzimuthDegrees = reference
        )
    } ?: RadarProjection.project(azimuthDegrees, elevationDegrees)
    return Offset(
        x = center.x + (projected.x * outer).toFloat(),
        y = center.y + (projected.y * outer).toFloat()
    )
}
