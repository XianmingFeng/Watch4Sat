package com.xianming.watch4sat.wear.orbit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag as semanticsTestTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.time.rememberAndroidClockTimeFormatter
import com.xianming.watch4sat.wear.OrbitMapView
import com.xianming.watch4sat.wear.ReportTimeTextVisibility
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.map.MapBadgeVisibilityPolicy
import com.xianming.watch4sat.wear.map.OrbitMapDisplayState
import com.xianming.watch4sat.wear.map.OrbitMapEngine
import com.xianming.watch4sat.wear.map.OrbitMapViewportCommand
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshot
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshots
import com.xianming.watch4sat.wear.map.TransientMapBadge
import com.xianming.watch4sat.wear.map.resolveText
import com.xianming.watch4sat.wear.state.OrbitMapChromePolicy
import com.xianming.watch4sat.wear.state.OrbitMapSessionReducer
import com.xianming.watch4sat.wear.state.OrbitMapSessionState
import com.xianming.watch4sat.wear.state.OrbitMapUiState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.googleSansFlexTimeTextStyle
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

val OrbitMapTopTimeTextVisualValueKey =
    SemanticsPropertyKey<String>("OrbitMapTopTimeTextVisualValue")

var SemanticsPropertyReceiver.orbitMapTopTimeTextVisualValue by
    OrbitMapTopTimeTextVisualValueKey

data class OrbitMapSurfaceRenderState(
    val orbitMap: OrbitMapUiState,
    val viewports: OrbitMapViewportSnapshots,
    val viewportCommand: OrbitMapViewportCommand?,
    val interactionEnabled: Boolean,
    val onMapTap: () -> Unit,
    val onViewportChanged: (OrbitMapEngine, OrbitMapViewportSnapshot) -> Unit,
    val onViewportCommandApplied: (OrbitMapViewportCommand) -> Unit,
    val onDisplayStateChanged: (OrbitMapDisplayState) -> Unit
)

@Composable
fun OrbitMapScreen(
    state: WatchUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onOpenData: () -> Unit,
    swipeToDismissBoxState: SwipeToDismissBoxState? = null,
    mapContent: (@Composable BoxScope.(OrbitMapSurfaceRenderState) -> Unit)? = null,
    initialSession: OrbitMapSessionState? = null,
    isAmbientOverride: Boolean? = null
) {
    ReportTimeTextVisibility(false)
    val ambientMode = LocalAmbientModeManager.current?.currentAmbientMode
    val isAmbient = isAmbientOverride ?: (ambientMode is AmbientMode.Ambient)
    val colors = LocalWatchThemeColors.current
    var lastInteractiveState by remember { mutableStateOf(state) }
    LaunchedEffect(isAmbient, state) {
        if (!isAmbient) {
            lastInteractiveState = state
        }
    }
    val displayedState = if (isAmbient) lastInteractiveState else state
    var session by rememberSaveable(stateSaver = OrbitMapSessionState.Saver) {
        mutableStateOf(
            OrbitMapSessionState(
                selectedCatalogNumber = displayedState.orbitMap.selectedCatalogNumber
            ).let { initialSession ?: it }
        )
    }
    var viewportCommand by remember { mutableStateOf<OrbitMapViewportCommand?>(null) }
    var displayState by remember { mutableStateOf<OrbitMapDisplayState?>(null) }

    LaunchedEffect(displayedState.orbitMap.selectedCatalogNumber) {
        val selectedCatalogNumber = displayedState.orbitMap.selectedCatalogNumber
        val pendingCommand = viewportCommand
        session = OrbitMapSessionReducer.selectCatalog(
            state = session,
            catalogNumber = selectedCatalogNumber,
            pendingCommand = pendingCommand
        )
        if (
            OrbitMapSessionReducer.shouldCancelPendingViewportCommand(
                pendingCommand = pendingCommand,
                selectedCatalogNumber = selectedCatalogNumber
            )
        ) {
            viewportCommand = null
        }
    }
    LaunchedEffect(
        session.selectedCatalogNumber,
        session.lastCenteredCatalogNumber,
        displayedState.orbitMap.currentPosition,
        displayState?.engine,
        isAmbient
    ) {
        if (isAmbient) return@LaunchedEffect
        if (
            session.selectedCatalogNumber !=
            displayedState.orbitMap.selectedCatalogNumber
        ) {
            return@LaunchedEffect
        }
        val decision = OrbitMapSessionReducer.recenterIfNeeded(
            state = session,
            position = displayedState.orbitMap.currentPosition,
            engine = displayState?.engine
        )
        if (decision.command != null) {
            session = decision.session
            viewportCommand = decision.command
        }
    }

    val chromeVisible = OrbitMapChromePolicy.interactiveChromeVisible(
        state = session.chrome,
        isAmbient = isAmbient
    )
    val selectedSatellite = displayedState.orbitMap.selectedSatellite
    val mapSummary = orbitMapSummary(displayedState.orbitMap)
    val toggleActionLabel = if (session.chrome.interactiveVisible) {
        stringResource(R.string.orbit_map_hide_controls)
    } else {
        stringResource(R.string.orbit_map_show_controls)
    }
    val renderState = OrbitMapSurfaceRenderState(
        orbitMap = displayedState.orbitMap,
        viewports = session.viewports,
        viewportCommand = viewportCommand,
        interactionEnabled = !isAmbient,
        onMapTap = {
            if (!isAmbient) {
                session = OrbitMapSessionReducer.toggleChrome(session)
            }
        },
        onViewportChanged = { engine, viewport ->
            session = OrbitMapSessionReducer.updateViewport(
                state = session,
                engine = engine,
                viewport = viewport
            )
        },
        onViewportCommandApplied = { appliedCommand ->
            if (viewportCommand?.id == appliedCommand.id) {
                session = OrbitMapSessionReducer.acknowledgeViewportCommand(
                    state = session,
                    command = appliedCommand
                )
                viewportCommand = null
            }
        },
        onDisplayStateChanged = { displayState = it }
    )
    ScreenScaffold(contentPadding = PaddingValues(0.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(OrbitMapTestTags.Screen)
                .semantics {
                    contentDescription = mapSummary
                    if (!isAmbient) {
                        customActions = listOf(
                            CustomAccessibilityAction(toggleActionLabel) {
                                session = OrbitMapSessionReducer.toggleChrome(session)
                                true
                            }
                        )
                    }
                }
        ) {
            if (mapContent == null) {
                OrbitMapView(
                    orbitMap = displayedState.orbitMap,
                    station = displayedState.settings.stationLocation,
                    mapTileMode = displayedState.settings.mapTileMode,
                    viewportSnapshots = session.viewports,
                    viewportCommand = viewportCommand,
                    interactionEnabled = !isAmbient,
                    swipeToDismissBoxState = swipeToDismissBoxState,
                    onMapTap = renderState.onMapTap,
                    onViewportChanged = renderState.onViewportChanged,
                    onViewportCommandApplied = renderState.onViewportCommandApplied,
                    onDisplayStateChanged = renderState.onDisplayStateChanged,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                mapContent(renderState)
            }

            if (isAmbient) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = OrbitMapScreenPolicy.AmbientOverlayAlpha)
                        )
                )
            }

            displayState?.let { currentDisplay ->
                if (!isAmbient) {
                    MapStatusBadge(displayState = currentDisplay)
                }
            }
            if (!isAmbient) {
                orbitMapPredictionStatus(displayedState.orbitMap)?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = WatchUiMetrics.OrbitMapPersistentStatusTopPadding,
                                start = 42.dp,
                                end = 42.dp
                            )
                            .background(
                                color = Color.Black.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .testTag(OrbitMapTestTags.PersistentStatus)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = OrbitMapChromePolicy.topTimeTextVisible(
                    state = session.chrome,
                    isAmbient = isAmbient
                ),
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.96f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                OrbitMapTopTimeText(
                    footprintRadiusKm = displayedState.orbitMap.footprintRadiusKm,
                    isAmbient = isAmbient
                )
            }

            val sideControlsVisible = OrbitMapChromePolicy.sideControlsVisible(
                state = session.chrome,
                isAmbient = isAmbient,
                candidateCount = displayedState.orbitMap.candidates.size
            )
            val previousControlVisibility = remember {
                MutableTransitionState(sideControlsVisible)
            }.apply { targetState = sideControlsVisible }
            val nextControlVisibility = remember {
                MutableTransitionState(sideControlsVisible)
            }.apply { targetState = sideControlsVisible }
            val attributionNeedsSideControlClearance =
                previousControlVisibility.currentState ||
                    previousControlVisibility.targetState ||
                    nextControlVisibility.currentState ||
                    nextControlVisibility.targetState
            val attributionRoundSafePadding =
                WatchUiMetrics.orbitMapAttributionRoundSafeHorizontalPadding(
                    screenDiameter = minOf(maxWidth, maxHeight)
                )
            AnimatedVisibility(
                visibleState = previousControlVisibility,
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.92f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = WatchUiMetrics.OrbitMapControlEdgePadding)
            ) {
                OrbitMapSatelliteIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(
                        R.string.orbit_map_previous_satellite,
                        OrbitMapScreenPolicy.neighborName(
                            orbitMap = displayedState.orbitMap,
                            previous = true
                        ) ?: stringResource(R.string.orbit_map_neighbor_none)
                    ),
                    testTag = OrbitMapTestTags.Previous,
                    onClick = onPrevious
                )
            }

            AnimatedVisibility(
                visibleState = nextControlVisibility,
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.92f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = WatchUiMetrics.OrbitMapControlEdgePadding)
            ) {
                OrbitMapSatelliteIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(
                        R.string.orbit_map_next_satellite,
                        OrbitMapScreenPolicy.neighborName(
                            orbitMap = displayedState.orbitMap,
                            previous = false
                        ) ?: stringResource(R.string.orbit_map_neighbor_none)
                    ),
                    testTag = OrbitMapTestTags.Next,
                    onClick = onNext
                )
            }

            val edgeButtonVisible = OrbitMapChromePolicy.edgeButtonVisible(
                state = session.chrome,
                isAmbient = isAmbient
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                MapAttributionBadge(
                    showOsmAttribution = displayState?.tileState?.showOsmAttribution
                        ?: (displayedState.settings.mapTileMode != MapTileMode.OFFLINE_WORLD),
                    modifier = if (attributionNeedsSideControlClearance) {
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    WatchUiMetrics.OrbitMapAttributionSideControlClearance
                            )
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = attributionRoundSafePadding
                            )
                    }
                )
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = WatchUiMetrics.OrbitMapAttributionBottomPadding
                        )
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = edgeButtonVisible,
                        enter = fadeIn(tween(160)) +
                            scaleIn(tween(160), initialScale = 0.92f) +
                            expandVertically(
                                animationSpec = tween(160),
                                expandFrom = Alignment.Bottom
                            ),
                        exit = fadeOut(tween(140)) +
                            scaleOut(tween(140), targetScale = 0.92f) +
                            shrinkVertically(
                                animationSpec = tween(140),
                                shrinkTowards = Alignment.Bottom
                            )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(
                                modifier = Modifier.height(
                                    WatchUiMetrics.OrbitMapAttributionEdgeButtonSpacing
                                )
                            )
                            val label = selectedSatellite?.displayName
                                ?: stringResource(R.string.orbit_map_open_data)
                            val edgeButtonDescription = selectedSatellite?.let {
                                stringResource(
                                    R.string.orbit_map_open_details_for,
                                    it.displayName
                                )
                            } ?: stringResource(R.string.orbit_map_open_satellite_data)
                            EdgeButton(
                                onClick = {
                                    selectedSatellite?.catalogNumber
                                        ?.let(onOpenDetail)
                                        ?: onOpenData()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                buttonSize = EdgeButtonSize.Medium,
                                modifier = Modifier
                                    .testTag(OrbitMapTestTags.EdgeButton)
                                    .semantics {
                                        contentDescription = edgeButtonDescription
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (isAmbient && selectedSatellite != null) {
                Text(
                    text = selectedSatellite.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 34.dp,
                            end = 34.dp,
                            bottom = WatchUiMetrics.OrbitMapAmbientSatelliteBottomPadding
                        )
                        .testTag(OrbitMapTestTags.AmbientSatellite)
                        .alpha(0.82f)
                )
            }
        }
    }
}

@Composable
private fun OrbitMapTopTimeText(
    footprintRadiusKm: Double,
    isAmbient: Boolean
) {
    OrbitMapTopTimeTextContent(
        clockText = rememberOrbitMapClockText(isAmbient),
        footprintRadiusKm = footprintRadiusKm
    )
}

@Composable
internal fun OrbitMapTopTimeTextContent(
    clockText: String,
    footprintRadiusKm: Double
) {
    val style = googleSansFlexTimeTextStyle()
    val diameterStyle = style.copy(color = LocalWatchThemeColors.current.primary)
    val density = LocalDensity.current
    val diameterKilometers = OrbitMapScreenPolicy.footprintDiameterKilometers(
        footprintRadiusKm
    )
    val accessibilityLabel = diameterKilometers?.let {
        stringResource(
            R.string.orbit_map_time_diameter_description,
            clockText,
            it
        )
    } ?: clockText
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthToFontSizeRatio = with(density) {
            maxWidth.toPx() / style.fontSize.toPx()
        }
        val diameterValue = remember(footprintRadiusKm, widthToFontSizeRatio) {
            OrbitMapScreenPolicy.topTimeVisualDiameter(
                radiusKm = footprintRadiusKm,
                widthToFontSizeRatio = widthToFontSizeRatio
            )
        }
        val diameter = diameterValue?.let { value ->
            when (value.presentation) {
                OrbitMapDiameterPresentation.Full -> stringResource(
                    R.string.orbit_map_diameter_compact,
                    value.kilometers
                )
                OrbitMapDiameterPresentation.TwoDigits -> stringResource(
                    R.string.orbit_map_diameter_truncated,
                    value.kilometers.toString().take(2)
                )
                OrbitMapDiameterPresentation.OneDigit -> stringResource(
                    R.string.orbit_map_diameter_truncated,
                    value.kilometers.toString().take(1)
                )
                OrbitMapDiameterPresentation.NoDigits -> stringResource(
                    R.string.orbit_map_diameter_truncated,
                    ""
                )
            }
        }
        val visualValue = remember(clockText, diameter) {
            diameter?.let { "$clockText · $it" } ?: clockText
        }
        TimeText(
            maxSweepAngle = OrbitMapScreenPolicy.TopTimeTextMaxSweepAngle,
            modifier = Modifier
                .clearAndSetSemantics {
                    contentDescription = accessibilityLabel
                    orbitMapTopTimeTextVisualValue = visualValue
                    semanticsTestTag = OrbitMapTestTags.TopTimeText
                }
        ) {
            timeTextCurvedText(clockText, style = style)
            diameter?.let {
                timeTextSeparator(style)
                timeTextCurvedText(it, style = diameterStyle)
            }
        }
    }
}

@Composable
private fun rememberOrbitMapClockText(isAmbient: Boolean): String {
    val clockTimeFormatter = rememberAndroidClockTimeFormatter()
    var clockTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isAmbient) {
        if (!isAmbient) {
            while (true) {
                val nowMillis = System.currentTimeMillis()
                clockTimeMillis = nowMillis
                delay(60_000L - (nowMillis % 60_000L))
            }
        }
    }
    return remember(clockTimeMillis, clockTimeFormatter) {
        clockTimeFormatter.formatCompactMinutes(clockTimeMillis, ZoneId.systemDefault())
    }
}

@Composable
private fun OrbitMapSatelliteIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(WatchUiMetrics.OrbitMapControlTouchTargetSize)
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.82f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun orbitMapSummary(orbitMap: OrbitMapUiState): String {
    val satellite = orbitMap.selectedSatellite?.displayName
        ?: stringResource(R.string.orbit_map_no_satellite_selected)
    val position = orbitMap.currentPosition?.let {
        stringResource(
            R.string.orbit_map_position_degrees,
            it.latitudeDegrees.roundToInt(),
            it.longitudeDegrees.roundToInt()
        )
    } ?: stringResource(R.string.orbit_map_position_unavailable)
    return stringResource(R.string.orbit_map_summary, satellite, position)
}

@Composable
private fun orbitMapPredictionStatus(orbitMap: OrbitMapUiState): String? {
    return when (OrbitMapScreenPolicy.predictionPresentation(orbitMap)) {
        OrbitMapPredictionPresentation.NoSatelliteData ->
            stringResource(R.string.orbit_map_no_satellite_data)
        OrbitMapPredictionPresentation.Message -> orbitMap.message
        OrbitMapPredictionPresentation.Hidden -> null
    }
}

@Composable
private fun BoxScope.MapStatusBadge(displayState: OrbitMapDisplayState) {
    val statusLabel = displayState.tileState.status.resolveText()
    TransientMapBadge(
        label = statusLabel,
        textStyle = MaterialTheme.typography.bodyExtraSmall,
        transient = MapBadgeVisibilityPolicy.isTransientStatus(displayState.tileState.status),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = WatchUiMetrics.OrbitMapSourceStatusTopPadding)
    )
}

@Composable
private fun MapAttributionBadge(
    showOsmAttribution: Boolean,
    modifier: Modifier = Modifier
) {
    val attribution = if (showOsmAttribution) {
        stringResource(R.string.orbit_map_osm_attribution)
    } else {
        stringResource(R.string.orbit_map_natural_earth_attribution)
    }
    val colors = LocalWatchThemeColors.current
    val baseStyle = MaterialTheme.typography.bodyExtraSmall
    val minimumFontSize =
        (baseStyle.fontSize.value * AttributionMinimumFontSizeMultiplier).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        BasicText(
            text = attribution,
            style = baseStyle.copy(
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight =
                    (baseStyle.fontSize.value * AttributionLineHeightMultiplier).sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(
                minFontSize = minimumFontSize,
                maxFontSize = baseStyle.fontSize,
                stepSize = AttributionFontSizeStepSp.sp
            ),
            modifier = Modifier
                .testTag(OrbitMapTestTags.Attribution)
                .background(colors.surface.copy(alpha = 0.74f), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 3.dp)
        )
    }
}

private const val AttributionFontSizeStepSp = 0.5f
private const val AttributionLineHeightMultiplier = 1.2f
private const val AttributionMinimumFontSizeMultiplier = 0.55f

enum class OrbitMapDiameterPresentation {
    Full,
    TwoDigits,
    OneDigit,
    NoDigits
}

data class OrbitMapVisualDiameter(
    val kilometers: Int,
    val presentation: OrbitMapDiameterPresentation
)

enum class OrbitMapPredictionPresentation {
    NoSatelliteData,
    Message,
    Hidden
}

object OrbitMapScreenPolicy {
    const val AmbientOverlayAlpha = 0.85f
    const val TopTimeTextMaxSweepAngle = 90f
    // Leave one 4dp glyph-raster reserve before selecting the longest visual label.
    internal const val FullDiameterWidthToFontSizeRatio = 184f / 15f
    internal const val TwoDigitDiameterWidthToFontSizeRatio = 155f / 15f
    internal const val OneDigitDiameterWidthToFontSizeRatio = 140f / 15f

    fun topTimeVisualDiameter(
        radiusKm: Double,
        widthToFontSizeRatio: Float
    ): OrbitMapVisualDiameter? {
        val diameterKilometers = footprintDiameterKilometers(radiusKm) ?: return null
        if (!widthToFontSizeRatio.isFinite() || widthToFontSizeRatio <= 0f) {
            return OrbitMapVisualDiameter(
                kilometers = diameterKilometers,
                presentation = OrbitMapDiameterPresentation.Full
            )
        }
        val presentation = when {
            widthToFontSizeRatio >= FullDiameterWidthToFontSizeRatio ->
                OrbitMapDiameterPresentation.Full
            widthToFontSizeRatio >= TwoDigitDiameterWidthToFontSizeRatio ->
                OrbitMapDiameterPresentation.TwoDigits
            widthToFontSizeRatio >= OneDigitDiameterWidthToFontSizeRatio ->
                OrbitMapDiameterPresentation.OneDigit
            else -> OrbitMapDiameterPresentation.NoDigits
        }
        return OrbitMapVisualDiameter(
            kilometers = diameterKilometers,
            presentation = presentation
        )
    }

    fun footprintDiameterKilometers(radiusKm: Double): Int? {
        return radiusKm
            .takeIf { it.isFinite() && it > 0.0 }
            ?.times(2.0)
            ?.roundToInt()
    }

    fun predictionPresentation(
        orbitMap: OrbitMapUiState
    ): OrbitMapPredictionPresentation {
        return when {
            orbitMap.candidates.isEmpty() ->
                OrbitMapPredictionPresentation.NoSatelliteData
            orbitMap.currentPosition == null -> OrbitMapPredictionPresentation.Message
            else -> OrbitMapPredictionPresentation.Hidden
        }
    }

    fun neighborName(
        orbitMap: OrbitMapUiState,
        previous: Boolean
    ): String? {
        val candidates = orbitMap.candidates
        if (candidates.isEmpty()) return null
        val currentIndex = candidates.indexOfFirst {
            it.catalogNumber == orbitMap.selectedCatalogNumber
        }.takeIf { it >= 0 } ?: 0
        val targetIndex = if (previous) {
            (currentIndex - 1 + candidates.size) % candidates.size
        } else {
            (currentIndex + 1) % candidates.size
        }
        return candidates[targetIndex].displayName
    }
}

object OrbitMapTestTags {
    const val Screen = "orbit-map-screen"
    const val Surface = "orbit-map-surface"
    const val Previous = "orbit-map-previous"
    const val Next = "orbit-map-next"
    const val EdgeButton = "orbit-map-edge-button"
    const val Attribution = "orbit-map-attribution"
    const val TopTimeText = "orbit-map-top-time-text"
    const val PersistentStatus = "orbit-map-persistent-status"
    const val AmbientSatellite = "orbit-map-ambient-satellite"
}
