package com.xianming.watch4sat.wear.radar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.ReportTimeTextVisibility
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchSemanticColors
import com.xianming.watch4sat.wear.theme.googleSansFlexTimeTextStyle

@Composable
fun RadarScreen(
    state: WatchUiState,
    overlayOpen: Boolean,
    onOverlayOpenChange: (Boolean) -> Unit,
    onSelectRadarTransmitter: (String?) -> Unit,
    onSelectRadarPass: (SatellitePass) -> Unit,
    calibrationHintRequestId: Int = 0,
    onCalibrationHintConsumed: () -> Unit = {},
    orientationOverride: RadarOrientationSnapshot? = null,
    visualCueOverride: RadarVisualCue? = null
) {
    ReportTimeTextVisibility(false)
    val ambientMode = LocalAmbientModeManager.current?.currentAmbientMode
    val isAmbient = ambientMode is AmbientMode.Ambient
    val updateMode = RadarPowerPolicy.updateMode(isAmbient)
    val textCatalog = radarTextCatalog()
    val hasFocusedPass = state.focusedPass != null
    KeepScreenOn(
        enabled = RadarPowerPolicy.shouldKeepScreenOn(
            settingEnabled = state.settings.radarKeepScreenOn,
            hasFocusedPass = hasFocusedPass,
            isAmbient = isAmbient
        )
    )

    val systemWristSide by rememberSystemRadarWristSide()
    val forwardEdge = RadarWristOrientationPolicy.resolveForwardEdge(
        forwardAxis = state.settings.radarForwardAxis,
        systemWristSide = systemWristSide,
        fallbackWristSide = state.settings.radarFallbackWristSide
    )
    val liveOrientation by rememberRadarOrientationState(
        station = state.station,
        active = RadarPowerPolicy.shouldListenToSensors(
            hasFocusedPass = hasFocusedPass,
            isAmbient = isAmbient
        ) && orientationOverride == null,
        forwardEdge = forwardEdge,
        updateMode = updateMode
    )
    val orientation = orientationOverride ?: liveOrientation
    val colors = LocalWatchThemeColors.current
    val summary = RadarUiText.semanticsSummary(
        pass = state.focusedPass,
        position = state.focusedPosition,
        orientationStatus = orientation.status,
        textCatalog = textCatalog
    )
    val passChromeKey = state.focusedPass?.let { RadarUiText.passChromeKey(it) }
    var pointingAssistVisible by remember { mutableStateOf(false) }
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    var orientationMode by rememberSaveable {
        mutableStateOf(RadarPlotOrientationMode.TrackingForward)
    }
    var forcedCalibrationHintRequestId by rememberSaveable { mutableStateOf(0) }
    var forcedCalibrationHintVisible by rememberSaveable { mutableStateOf(false) }
    val referenceAzimuthDegrees = when (orientationMode) {
        RadarPlotOrientationMode.TrackingForward -> orientation.referenceAzimuthDegrees
        RadarPlotOrientationMode.NorthReference -> null
    }
    val displayReferenceAzimuth = rememberDisplayReferenceAzimuth(
        rawReferenceAzimuthDegrees = referenceAzimuthDegrees,
        updateMode = updateMode
    )
    val plotRotationDegrees = remember(
        orientationMode,
        forwardEdge,
        referenceAzimuthDegrees,
        updateMode,
        displayReferenceAzimuth
    ) {
        derivedStateOf {
            val renderedReferenceAzimuthDegrees = referenceAzimuthDegrees?.let {
                RadarHeadingSmoothingPolicy.renderedDisplayAzimuthDegrees(
                    rawReferenceAzimuthDegrees = it,
                    animatedDisplayDegrees = displayReferenceAzimuth.value,
                    updateMode = updateMode
                )
            }
            RadarPlotRotationPolicy.rotationDegrees(
                mode = orientationMode,
                forwardEdge = forwardEdge,
                renderedReferenceAzimuthDegrees = renderedReferenceAzimuthDegrees
            )
        }
    }
    val displayFocusedPosition = rememberDisplayFocusedPosition(
        position = state.focusedPosition,
        markerKey = passChromeKey,
        updateMode = updateMode
    )
    val transientHint = RadarOrientationPolicy.transientHintFor(
        hasFocusedPass = state.focusedPass != null,
        chromeVisible = chromeVisible,
        pointingAssistVisible = pointingAssistVisible,
        overlayOpen = overlayOpen,
        sensorKind = orientation.sensorKind,
        accuracy = orientation.accuracy,
        forceCalibrationHint = forcedCalibrationHintVisible
    )
    val showCalibrationHint = transientHint == RadarTransientHint.FigureEightCalibration

    LaunchedEffect(passChromeKey, orientation.status) {
        pointingAssistVisible = passChromeKey != null &&
            orientation.status == RadarOrientationStatus.PointingAssistActive
        if (pointingAssistVisible) {
            delay(WatchUiMetrics.RadarPassBadgeVisibleMillis)
            pointingAssistVisible = false
        }
    }
    LaunchedEffect(calibrationHintRequestId) {
        if (calibrationHintRequestId > 0) {
            chromeVisible = true
            pointingAssistVisible = false
            forcedCalibrationHintRequestId = calibrationHintRequestId
            forcedCalibrationHintVisible = true
            onCalibrationHintConsumed()
        }
    }
    LaunchedEffect(forcedCalibrationHintRequestId) {
        val requestId = forcedCalibrationHintRequestId
        if (requestId > 0) {
            delay(WatchUiMetrics.RadarPassBadgeVisibleMillis)
            if (forcedCalibrationHintRequestId == requestId) {
                forcedCalibrationHintRequestId = 0
                forcedCalibrationHintVisible = false
            }
        }
    }
    LaunchedEffect(overlayOpen) {
        if (overlayOpen) {
            pointingAssistVisible = false
            forcedCalibrationHintRequestId = 0
            forcedCalibrationHintVisible = false
        }
    }

    ScreenScaffold(contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .semantics { contentDescription = summary.text }
        ) {
            RadarSkyPlot(
                track = state.focusedTrack,
                position = state.focusedPosition,
                displayPosition = displayFocusedPosition,
                pointing = orientation.pointing,
                referenceAzimuthDegrees = null,
                colors = colors,
                updateMode = updateMode,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = plotRotationDegrees.value
                    },
                visualCueOverride = visualCueOverride
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(overlayOpen) {
                        detectTapGestures {
                            if (!overlayOpen) {
                                chromeVisible = !chromeVisible
                            }
                        }
                    }
            )

            AnimatedVisibility(
                visible = RadarPowerPolicy.showTopTimeText(
                    isAmbient = isAmbient,
                    chromeVisible = chromeVisible && !overlayOpen
                ),
                enter = fadeIn(tween(durationMillis = 160)) +
                    scaleIn(tween(durationMillis = 160), initialScale = 0.96f),
                exit = fadeOut(tween(durationMillis = 140)) +
                    scaleOut(tween(durationMillis = 140), targetScale = 0.96f),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RadarTopTimeText(
                    pass = state.focusedPass,
                    nowMillis = state.radarNowMillis,
                    updateMode = updateMode,
                    textCatalog = textCatalog
                )
            }

            if (state.focusedPass == null && !showCalibrationHint) {
                Text(
                    text = if (state.hasStationLocation) {
                        state.passPlanningMessage
                    } else {
                        stringResource(R.string.radar_set_qth_first)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                )
            }
            if (state.focusedPass != null && !isAmbient) {
                AnimatedVisibility(
                    visible = transientHint == RadarTransientHint.PointingAssist,
                    enter = fadeIn(tween(durationMillis = 160)) +
                        scaleIn(tween(durationMillis = 160), initialScale = 0.94f),
                    exit = fadeOut(tween(durationMillis = 180)) +
                        scaleOut(tween(durationMillis = 180), targetScale = 0.94f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = WatchUiMetrics.RadarTransientHintBottomPadding)
                ) {
                    PointingAssistHint()
                }
            }
            AnimatedVisibility(
                visible = showCalibrationHint && !isAmbient,
                enter = fadeIn(tween(durationMillis = 160)) +
                    scaleIn(tween(durationMillis = 160), initialScale = 0.94f),
                exit = fadeOut(tween(durationMillis = 180)) +
                    scaleOut(tween(durationMillis = 180), targetScale = 0.94f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WatchUiMetrics.RadarTransientHintBottomPadding)
            ) {
                CalibrationHint()
            }

            AnimatedVisibility(
                visible = RadarPowerPolicy.showInteractiveChrome(
                    isAmbient = isAmbient,
                    chromeVisible = chromeVisible && !overlayOpen
                ) && state.focusedPass != null,
                enter = fadeIn(tween(durationMillis = 160)) +
                    scaleIn(tween(durationMillis = 160), initialScale = 0.92f),
                exit = fadeOut(tween(durationMillis = 140)) +
                    scaleOut(tween(durationMillis = 140), targetScale = 0.92f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WatchUiMetrics.RadarSatelliteEdgeButtonBottomPadding)
            ) {
                state.focusedPass?.let { pass ->
                    SatelliteEdgeButton(
                        label = RadarUiText.satelliteEdgeLabel(pass),
                        onClick = { onOverlayOpenChange(true) }
                    )
                }
            }

            AnimatedVisibility(
                visible = RadarPowerPolicy.showInteractiveChrome(
                    isAmbient = isAmbient,
                    chromeVisible = chromeVisible && !overlayOpen
                ) && state.focusedPass != null,
                enter = fadeIn(tween(durationMillis = 160)) +
                    scaleIn(tween(durationMillis = 160), initialScale = 0.92f),
                exit = fadeOut(tween(durationMillis = 140)) +
                    scaleOut(tween(durationMillis = 140), targetScale = 0.92f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = WatchUiMetrics.RadarControlEdgePadding)
            ) {
                val tracking = orientationMode == RadarPlotOrientationMode.TrackingForward
                val orientationDescription = if (tracking) {
                    stringResource(R.string.radar_switch_to_north_reference)
                } else {
                    stringResource(R.string.radar_switch_to_tracking)
                }
                IconButton(
                    onClick = {
                        orientationMode = if (tracking) {
                            RadarPlotOrientationMode.NorthReference
                        } else {
                            RadarPlotOrientationMode.TrackingForward
                        }
                    },
                    modifier = Modifier
                        .semantics {
                            contentDescription = orientationDescription
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(WatchUiMetrics.RadarOrientationIconBackdropSize)
                            .background(
                                color = Color.Black.copy(alpha = 0.92f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (tracking) Icons.Rounded.Explore else Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(WatchUiMetrics.RadarOrientationIconSize)
                        )
                    }
                }
            }

            RadarDetailOverlay(
                visible = overlayOpen,
                state = state,
                orientation = orientation.copy(referenceAzimuthDegrees = referenceAzimuthDegrees),
                plotRotationDegrees = plotRotationDegrees,
                displayPosition = displayFocusedPosition,
                onClose = { onOverlayOpenChange(false) },
                onSelectRadarTransmitter = onSelectRadarTransmitter,
                onSelectRadarPass = onSelectRadarPass
            )
        }
    }
}

@Composable
private fun rememberDisplayReferenceAzimuth(
    rawReferenceAzimuthDegrees: Double?,
    updateMode: RadarUpdateMode
): Animatable<Float, AnimationVector1D> {
    val displayAzimuth = remember {
        Animatable(rawReferenceAzimuthDegrees?.toFloat() ?: 0f)
    }
    var filterState by remember {
        mutableStateOf(RadarHeadingFilterState())
    }

    LaunchedEffect(rawReferenceAzimuthDegrees, updateMode) {
        val raw = rawReferenceAzimuthDegrees
        if (raw == null) {
            filterState = RadarHeadingFilterState()
            displayAzimuth.snapTo(0f)
            return@LaunchedEffect
        }

        val result = RadarHeadingSmoothingPolicy.adaptiveDisplayTarget(
            previous = filterState,
            rawReferenceAzimuthDegrees = raw,
            sampleElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            updateMode = updateMode
        )
        filterState = result.state

        if (result.snap || updateMode == RadarUpdateMode.AmbientOneHz) {
            displayAzimuth.snapTo(result.targetDegrees)
        } else {
            displayAzimuth.animateTo(
                targetValue = result.targetDegrees,
                animationSpec = radarHeadingFrameSpec()
            )
        }
    }

    return displayAzimuth
}

private fun radarHeadingFrameSpec() = tween<Float>(
    durationMillis = RadarHeadingSmoothingPolicy.adaptiveFrameTweenMillis,
    easing = LinearEasing
)

@Composable
private fun rememberDisplayFocusedPosition(
    position: com.xianming.watch4sat.domain.model.OrbitalPosition?,
    markerKey: String?,
    updateMode: RadarUpdateMode
): RadarDisplayPoint? {
    val target = radarDisplayPointForPosition(position)
    val displayX = remember { Animatable(target?.x ?: 0f) }
    val displayY = remember { Animatable(target?.y ?: 0f) }
    var filterState by remember { mutableStateOf(RadarPositionFilterState()) }
    var visibleTarget by remember { mutableStateOf(target) }
    var lastMarkerKey by remember { mutableStateOf(markerKey) }

    LaunchedEffect(target, markerKey, updateMode) {
        val previousState = if (markerKey != lastMarkerKey) {
            lastMarkerKey = markerKey
            RadarPositionFilterState()
        } else {
            filterState
        }
        val result = RadarPositionSmoothingPolicy.displayTarget(
            previous = previousState,
            target = target,
            updateMode = updateMode
        )
        filterState = result.state
        visibleTarget = result.target

        val next = result.target
        if (next == null) {
            displayX.snapTo(0f)
            displayY.snapTo(0f)
            return@LaunchedEffect
        }

        if (result.snap || updateMode == RadarUpdateMode.AmbientOneHz) {
            displayX.snapTo(next.x)
            displayY.snapTo(next.y)
        } else {
            val spec = radarMarkerFrameSpec()
            coroutineScope {
                launch { displayX.animateTo(next.x, animationSpec = spec) }
                launch { displayY.animateTo(next.y, animationSpec = spec) }
            }
        }
    }

    return visibleTarget?.let {
        RadarDisplayPoint(x = displayX.value, y = displayY.value)
    }
}

private fun radarMarkerFrameSpec() = tween<Float>(
    durationMillis = RadarPositionSmoothingPolicy.markerTweenMillis,
    easing = LinearEasing
)

@Composable
private fun RadarTopTimeText(
    pass: com.xianming.watch4sat.domain.model.SatellitePass?,
    nowMillis: Long,
    updateMode: RadarUpdateMode,
    textCatalog: RadarTextCatalog
) {
    val colors = LocalWatchThemeColors.current
    val style = googleSansFlexTimeTextStyle()
    val statusStyle = googleSansFlexTimeTextStyle(colors.primary)
    TimeText {
        if (pass == null) {
            timeTextCurvedText(it, style = style)
        } else {
            val status = RadarUiText.topCountdownStatus(
                pass = pass,
                nowMillis = nowMillis,
                textCatalog = textCatalog,
                showSeconds = RadarCountdownPolicy.showSecondsInFinalMinute(updateMode)
            )
            timeTextCurvedText(status, style = statusStyle)
            timeTextSeparator(style)
            timeTextCurvedText(it, style = style)
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window, enabled) {
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (enabled) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun SatelliteEdgeButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val contentDescription = stringResource(R.string.radar_open_details, label)
    EdgeButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary.copy(alpha = 0.78f),
            contentColor = colors.onPrimary
        ),
        buttonSize = EdgeButtonSize.Medium,
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun PointingAssistHint(modifier: Modifier = Modifier) {
    val colors = LocalWatchThemeColors.current
    Text(
        text = stringResource(R.string.radar_pointing_assist),
        color = colors.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .widthIn(max = WatchUiMetrics.RadarTransientHintMaxWidth)
            .background(
                color = colors.surfaceVariant.copy(alpha = 0.62f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun CalibrationHint(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.radar_calibration_hint),
        color = WatchSemanticColors.WarningForeground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .widthIn(max = WatchUiMetrics.RadarTransientHintMaxWidth)
            .background(
                color = WatchSemanticColors.WarningContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
