package com.xianming.watch4sat.wear.map

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.domain.footprint.SatelliteFootprint
import com.xianming.watch4sat.domain.geometry.AntimeridianClipper
import com.xianming.watch4sat.domain.geometry.AntimeridianPolygonGeometry
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.wear.TrackDirectionArrowPolicy
import com.xianming.watch4sat.wear.TrackDirectionPoint
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.theme.WatchThemeCatalog
import com.xianming.watch4sat.wear.theme.WatchThemeColors

@Composable
fun OfflineWorldMapView(
    centerLatitude: Double,
    centerLongitude: Double,
    modifier: Modifier = Modifier,
    initialZoom: Float = 1f,
    minZoom: Float = 1f,
    maxZoom: Float = 6f,
    showCrosshair: Boolean = false,
    colors: WatchThemeColors = WatchThemeCatalog.colorsFor(AppThemePreset.PIXEL_MINT),
    station: StationLocation? = null,
    satellitePosition: GroundTrackPoint? = null,
    trackSegments: List<List<GroundTrackPoint>> = emptyList(),
    footprint: SatelliteFootprint? = null,
    onCenterChanged: ((Double, Double) -> Unit)? = null
) {
    val centerCallback = rememberUpdatedState(onCenterChanged)
    OfflineWorldMapCanvas(
        initialViewport = OrbitMapViewportSnapshot(
            centerLatitude = centerLatitude,
            centerLongitude = centerLongitude,
            zoom = initialZoom.toDouble()
        ),
        requestedCenter = OfflineMapLocation(
            latitude = centerLatitude,
            longitude = centerLongitude
        ),
        minZoom = minZoom,
        maxZoom = maxZoom,
        showCrosshair = showCrosshair,
        colors = colors,
        station = station,
        satellitePosition = satellitePosition,
        trackSegments = trackSegments,
        footprint = footprint,
        viewportCommand = null,
        commandLedger = null,
        interactionEnabled = true,
        onMapTap = {},
        onViewportChanged = { viewport ->
            centerCallback.value?.invoke(
                viewport.centerLatitude,
                viewport.centerLongitude
            )
        },
        onViewportCommandApplied = {},
        modifier = modifier
    )
}

@Composable
internal fun OfflineWorldMapView(
    initialViewport: OrbitMapViewportSnapshot,
    modifier: Modifier = Modifier,
    minZoom: Float = 1f,
    maxZoom: Float = 6f,
    showCrosshair: Boolean = false,
    colors: WatchThemeColors = WatchThemeCatalog.colorsFor(AppThemePreset.PIXEL_MINT),
    station: StationLocation? = null,
    satellitePosition: GroundTrackPoint? = null,
    trackSegments: List<List<GroundTrackPoint>> = emptyList(),
    footprint: SatelliteFootprint? = null,
    viewportCommand: OrbitMapViewportCommand? = null,
    commandLedger: OrbitMapCommandLedger,
    interactionEnabled: Boolean = true,
    onMapTap: () -> Unit = {},
    onViewportChanged: (OrbitMapViewportSnapshot) -> Unit = {},
    onViewportCommandApplied: (OrbitMapViewportCommand) -> Unit = {},
    gestureStartInset: Dp = 0.dp
) {
    OfflineWorldMapCanvas(
        initialViewport = initialViewport,
        requestedCenter = null,
        minZoom = minZoom,
        maxZoom = maxZoom,
        showCrosshair = showCrosshair,
        colors = colors,
        station = station,
        satellitePosition = satellitePosition,
        trackSegments = trackSegments,
        footprint = footprint,
        viewportCommand = viewportCommand,
        commandLedger = commandLedger,
        interactionEnabled = interactionEnabled,
        onMapTap = onMapTap,
        onViewportChanged = onViewportChanged,
        onViewportCommandApplied = onViewportCommandApplied,
        gestureStartInset = gestureStartInset,
        modifier = modifier
    )
}

@Composable
private fun OfflineWorldMapCanvas(
    initialViewport: OrbitMapViewportSnapshot,
    requestedCenter: OfflineMapLocation?,
    minZoom: Float,
    maxZoom: Float,
    showCrosshair: Boolean,
    colors: WatchThemeColors,
    station: StationLocation?,
    satellitePosition: GroundTrackPoint?,
    trackSegments: List<List<GroundTrackPoint>>,
    footprint: SatelliteFootprint?,
    viewportCommand: OrbitMapViewportCommand?,
    commandLedger: OrbitMapCommandLedger?,
    interactionEnabled: Boolean,
    onMapTap: () -> Unit,
    onViewportChanged: (OrbitMapViewportSnapshot) -> Unit,
    onViewportCommandApplied: (OrbitMapViewportCommand) -> Unit,
    gestureStartInset: Dp = 0.dp,
    modifier: Modifier
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val mapTapCallback = rememberUpdatedState(onMapTap)
    val viewportCallback = rememberUpdatedState(onViewportChanged)
    val commandAppliedCallback = rememberUpdatedState(onViewportCommandApplied)
    val viewportCommandState = rememberUpdatedState(viewportCommand)
    val tapGestureGuard = remember { OfflineMapTapGestureGuard() }
    val viewportAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val trackArrowSizePx = with(density) { WatchUiMetrics.OrbitMapTrackArrowSize.toPx() }
    val trackArrowMinSegmentPx = with(density) { WatchUiMetrics.OrbitMapTrackArrowMinSegmentLength.toPx() }
    val trackArrowMaxSegmentPx = with(density) { WatchUiMetrics.OrbitMapTrackArrowMaxSegmentLength.toPx() }
    val gestureStartInsetPx = with(density) { gestureStartInset.toPx() }
    val landGeometry = remember(context) {
        OfflineWorldGeometry.prepareLand(NaturalEarthLandLoader.load(context))
    }
    val style = remember(colors) { MapOverlayStyle.from(colors) }
    var viewport by remember {
        mutableStateOf(
            OrbitMapViewportPolicy.sanitize(
                viewport = initialViewport,
                minZoom = minZoom.toDouble(),
                maxZoom = maxZoom.toDouble()
            )
        )
    }

    fun interruptViewportCommand() {
        val command = viewportCommandState.value ?: return
        val ledger = commandLedger ?: return
        if (
            ledger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OFFLINE,
                interactionEnabled = interactionEnabled
            )
        ) {
            ledger.markApplied(command)
            commandAppliedCallback.value(command)
        }
    }

    LaunchedEffect(requestedCenter) {
        if (requestedCenter == null) return@LaunchedEffect
        viewport = OrbitMapViewportPolicy.sanitize(
            viewport = viewport.copy(
                centerLatitude = requestedCenter.latitude,
                centerLongitude = requestedCenter.longitude
            ),
            minZoom = minZoom.toDouble(),
            maxZoom = maxZoom.toDouble()
        )
    }

    LaunchedEffect(
        viewportCommand?.id,
        viewportCommand?.engine,
        viewportCommand?.viewport,
        viewportCommand?.transition,
        viewportAnimationSpec,
        interactionEnabled
    ) {
        val command = viewportCommand ?: return@LaunchedEffect
        val ledger = commandLedger ?: return@LaunchedEffect
        if (
            !ledger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OFFLINE,
                interactionEnabled = interactionEnabled
            )
        ) {
            return@LaunchedEffect
        }
        val target = OrbitMapViewportPolicy.sanitize(
            viewport = command.viewport,
            minZoom = minZoom.toDouble(),
            maxZoom = maxZoom.toDouble()
        )
        if (command.transition == OrbitMapViewportTransition.ANIMATED) {
            val start = viewport
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = viewportAnimationSpec
            ) {
                if (
                    !ledger.shouldApply(
                        command = command,
                        engine = OrbitMapEngine.OFFLINE,
                        interactionEnabled = interactionEnabled
                    )
                ) {
                    return@animateTo
                }
                val frame = OrbitMapViewportMotionPolicy.interpolate(
                    start = start,
                    end = target,
                    progress = value.toDouble()
                )
                viewport = frame
                viewportCallback.value(frame)
            }
        } else {
            viewport = target
            viewportCallback.value(target)
        }
        if (
            !ledger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OFFLINE,
                interactionEnabled = interactionEnabled
            )
        ) {
            return@LaunchedEffect
        }
        ledger.markApplied(command)
        commandAppliedCallback.value(command)
    }

    val interactionModifier = if (interactionEnabled) {
        Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    var event = awaitPointerEvent(PointerEventPass.Initial)
                    interruptViewportCommand()
                    tapGestureGuard.beginGesture(
                        activePointerCount = event.changes.count { it.pressed }
                    )
                    while (event.changes.any { it.pressed }) {
                        event = awaitPointerEvent(PointerEventPass.Initial)
                        tapGestureGuard.observePointers(
                            activePointerCount = event.changes.count { it.pressed }
                        )
                    }
                }
            }
            .pointerInput(minZoom, maxZoom) {
                detectTapGestures(
                    onDoubleTap = {
                        interruptViewportCommand()
                        val target = OrbitMapViewportPolicy.sanitize(
                            viewport = viewport.copy(zoom = viewport.zoom * 2.0),
                            minZoom = minZoom.toDouble(),
                            maxZoom = maxZoom.toDouble()
                        )
                        viewport = target
                        viewportCallback.value(target)
                    },
                    onLongPress = {},
                    onTap = {
                        if (tapGestureGuard.acceptsConfirmedTap()) {
                            mapTapCallback.value()
                        }
                    }
                )
            }
            .pointerInput(minZoom, maxZoom) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    interruptViewportCommand()
                    val projection = OfflineWorldProjection(
                        widthPx = size.width + gestureStartInsetPx,
                        heightPx = size.height.toFloat(),
                        centerLatitude = viewport.centerLatitude,
                        centerLongitude = viewport.centerLongitude,
                        zoom = viewport.zoom.toFloat()
                    )
                    val movedCenter = projection.centerAfterDrag(
                        deltaXPx = pan.x,
                        deltaYPx = pan.y
                    )
                    val target = OrbitMapViewportPolicy.sanitize(
                        viewport = OrbitMapViewportSnapshot(
                            centerLatitude = movedCenter.latitude,
                            centerLongitude = movedCenter.longitude,
                            zoom = viewport.zoom * zoomChange.toDouble()
                        ),
                        minZoom = minZoom.toDouble(),
                        maxZoom = maxZoom.toDouble()
                    )
                    viewport = target
                    viewportCallback.value(target)
                }
            }
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val projection = OfflineWorldProjection(
                        widthPx = size.width,
                        heightPx = size.height,
                        centerLatitude = viewport.centerLatitude,
                        centerLongitude = viewport.centerLongitude,
                        zoom = viewport.zoom.toFloat()
                    )
                    val gridLines = offlineWorldGridLines(projection)
                    val landFillPaths = landGeometry.flatMap { polygon ->
                        polygon.fillWorldCopies.map { rings -> rings.toFillPath(projection) }
                    }
                    val landStrokePaths = landGeometry.flatMap { polygon ->
                        polygon.coastlineWorldCopies.flatten().map { segment ->
                            segment.toPath(projection, close = false, wrapLongitude = false)
                        }
                    }
                    val landStroke = Stroke(width = 0.55f)
                    onDrawBehind {
                        drawRect(colors.mapSea)
                        drawWorldGrid(gridLines, colors)
                        landFillPaths.forEach { path -> drawPath(path, colors.mapLand) }
                        if (colors.mapLandStrokeAlpha > 0f) {
                            landStrokePaths.forEach { path ->
                                drawPath(
                                    path,
                                    colors.mapLand.copy(alpha = colors.mapLandStrokeAlpha),
                                    style = landStroke
                                )
                            }
                        }
                    }
                }
        ) {
            val projection = OfflineWorldProjection(
                widthPx = size.width,
                heightPx = size.height,
                centerLatitude = viewport.centerLatitude,
                centerLongitude = viewport.centerLongitude,
                zoom = viewport.zoom.toFloat()
            )
            footprint?.let { satelliteFootprint ->
                drawFootprint(
                    geometry = AntimeridianClipper.clipClosedRing(
                        points = satelliteFootprint.ring,
                        centerLongitudeDegrees = projection.centerLongitude,
                        enclosedPole = satelliteFootprint.enclosedPole
                    ),
                    projection = projection,
                    style = style.footprint
                )
            }
            trackSegments.forEach { segment ->
                drawTrack(
                    segment = segment,
                    projection = projection,
                    style = style,
                    arrowSizePx = trackArrowSizePx,
                    arrowMinSegmentPx = trackArrowMinSegmentPx,
                    arrowMaxSegmentPx = trackArrowMaxSegmentPx
                )
            }
            station?.let { stationLocation ->
                val point = projection.project(stationLocation.latitude, stationLocation.longitude)
                drawStationMarker(center = Offset(point.x, point.y), style = style.station)
            }
            satellitePosition?.let { satellite ->
                val point = projection.project(satellite.latitudeDegrees, satellite.longitudeDegrees)
                drawSatelliteMarker(center = Offset(point.x, point.y), style = style.satellite)
            }
            if (showCrosshair) {
                drawMapCrosshair(style.crosshair)
            }
        }
        if (interactionEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = gestureStartInset)
                    .then(interactionModifier)
            )
        }
    }
}

internal class OfflineMapTapGestureGuard {
    private var maximumPointerCount: Int = 0

    fun beginGesture(activePointerCount: Int) {
        maximumPointerCount = activePointerCount
    }

    fun observePointers(activePointerCount: Int) {
        maximumPointerCount = maxOf(maximumPointerCount, activePointerCount)
    }

    fun acceptsConfirmedTap(): Boolean = maximumPointerCount == 1
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFootprint(
    geometry: AntimeridianPolygonGeometry,
    projection: OfflineWorldProjection,
    style: MapFootprintStyle
) {
    geometry.fillPolygons.forEach { polygon ->
        val path = Path()
        polygon.forEachIndexed { index, point ->
            val screen = projection.projectUnwrapped(point.latitudeDegrees, point.longitudeDegrees)
            if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
        }
        path.close()
        drawPath(path, style.fillColor.copy(alpha = style.fillAlpha))
    }
    geometry.outlineSegments.forEach { segment ->
        if (segment.size < 2) return@forEach
        val path = Path()
        segment.forEachIndexed { index, point ->
            val screen = projection.projectUnwrapped(point.latitudeDegrees, point.longitudeDegrees)
            if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
        }
        drawPath(
            path = path,
            color = style.outlineColor.copy(alpha = style.outlineAlpha),
            style = Stroke(width = style.outlineStrokePx, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawWorldGrid(
    lines: List<OfflineGridLine>,
    colors: WatchThemeColors
) {
    lines.forEach { line ->
        drawLine(
            color = colors.mapGrid.copy(alpha = 0.08f),
            start = line.start,
            end = line.end,
            strokeWidth = 1f
        )
    }
}

private fun offlineWorldGridLines(projection: OfflineWorldProjection): List<OfflineGridLine> {
    val lines = mutableListOf<OfflineGridLine>()
    for (longitude in -180..180 step 30) {
        val top = projection.project(80.0, longitude.toDouble())
        val bottom = projection.project(-80.0, longitude.toDouble())
        lines += OfflineGridLine(
            start = Offset(top.x, top.y),
            end = Offset(bottom.x, bottom.y)
        )
    }
    for (latitude in -60..60 step 30) {
        val left = projection.project(latitude.toDouble(), -180.0)
        val right = projection.project(latitude.toDouble(), 180.0)
        lines += OfflineGridLine(
            start = Offset(left.x, left.y),
            end = Offset(right.x, right.y)
        )
    }
    return lines
}

private fun List<List<OfflineMapLocation>>.toFillPath(projection: OfflineWorldProjection): Path {
    return Path().apply {
        fillType = PathFillType.EvenOdd
        forEach { ring -> addClosedRing(ring, projection) }
    }
}

private fun Path.addClosedRing(
    ring: List<OfflineMapLocation>,
    projection: OfflineWorldProjection
) {
    ring.forEachIndexed { index, location ->
        val point = projection.projectUnwrapped(location.latitude, location.longitude)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

private fun List<OfflineMapLocation>.toPath(
    projection: OfflineWorldProjection,
    close: Boolean,
    wrapLongitude: Boolean
): Path {
    val path = Path()
    forEachIndexed { index, location ->
        val point = if (wrapLongitude) {
            projection.project(location.latitude, location.longitude)
        } else {
            projection.projectUnwrapped(location.latitude, location.longitude)
        }
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    if (close) path.close()
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrack(
    segment: List<GroundTrackPoint>,
    projection: OfflineWorldProjection,
    style: MapOverlayStyle,
    arrowSizePx: Float,
    arrowMinSegmentPx: Float,
    arrowMaxSegmentPx: Float
) {
    if (segment.size < 2) return
    OfflineWorldGeometry.trackForProjection(
        points = segment,
        centerLongitude = projection.centerLongitude
    ).forEach { projectedSegment ->
        drawTrackSegment(
            segment = projectedSegment,
            projection = projection,
            style = style,
            arrowSizePx = arrowSizePx,
            arrowMinSegmentPx = arrowMinSegmentPx,
            arrowMaxSegmentPx = arrowMaxSegmentPx
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrackSegment(
    segment: List<GroundTrackPoint>,
    projection: OfflineWorldProjection,
    style: MapOverlayStyle,
    arrowSizePx: Float,
    arrowMinSegmentPx: Float,
    arrowMaxSegmentPx: Float
) {
    if (segment.size < 2) return
    val path = Path()
    val trackPoints = mutableListOf<TrackDirectionPoint>()
    segment.forEachIndexed { index, point ->
        val screen = projection.projectUnwrapped(point.latitudeDegrees, point.longitudeDegrees)
        trackPoints += TrackDirectionPoint(screen.x, screen.y)
        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
    }
    drawPath(
        path = path,
        color = style.track.color,
        style = Stroke(width = style.track.strokePx, cap = StrokeCap.Round)
    )
    TrackDirectionArrowPolicy.arrowsFor(
        points = trackPoints,
        arrowSizePx = arrowSizePx,
        minSegmentLengthPx = arrowMinSegmentPx,
        maxSegmentLengthPx = arrowMaxSegmentPx
    ).forEach { arrow ->
        drawTrackDirectionArrow(arrow, style.arrow)
    }
}

private data class OfflineGridLine(val start: Offset, val end: Offset)
