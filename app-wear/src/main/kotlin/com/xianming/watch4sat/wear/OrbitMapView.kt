package com.xianming.watch4sat.wear

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.material3.MaterialTheme
import com.xianming.watch4sat.BuildConfig
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.wear.map.MapOverlayStyle
import com.xianming.watch4sat.wear.map.MapTileStateReducer
import com.xianming.watch4sat.wear.map.OfflineWorldMapView
import com.xianming.watch4sat.wear.map.OrbitMapCommandLedger
import com.xianming.watch4sat.wear.map.OrbitMapAmbientDisplayPolicy
import com.xianming.watch4sat.wear.map.OrbitMapDisplayState
import com.xianming.watch4sat.wear.map.OrbitMapEngine
import com.xianming.watch4sat.wear.map.OrbitMapViewportCommand
import com.xianming.watch4sat.wear.map.OrbitMapViewportMotionPolicy
import com.xianming.watch4sat.wear.map.OrbitMapViewportPolicy
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshot
import com.xianming.watch4sat.wear.map.OrbitMapViewportSnapshots
import com.xianming.watch4sat.wear.map.OrbitMapViewportTransition
import com.xianming.watch4sat.wear.map.OrbitMapSwipeBackPolicy
import com.xianming.watch4sat.wear.map.OsmOrbitOverlaySnapshot
import com.xianming.watch4sat.wear.map.OsmOrbitOverlayUpdatePolicy
import com.xianming.watch4sat.wear.map.OsmOverlayDrawableFactory
import com.xianming.watch4sat.wear.map.OsmTileEvent
import com.xianming.watch4sat.wear.map.OsmTileInteractionPolicy
import com.xianming.watch4sat.wear.map.OsmUserAgent
import com.xianming.watch4sat.wear.state.OrbitMapUiState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

private const val TileFailureThreshold = 4
private const val TileNoSuccessTimeoutMillis = 8_000L
private const val TrackOverlayMinuteMillis = 60_000L
private const val OsmMinZoom = 0.0
private const val OsmMaxZoom = 6.0

@Composable
fun OrbitMapView(
    orbitMap: OrbitMapUiState,
    station: StationLocation?,
    mapTileMode: MapTileMode,
    modifier: Modifier = Modifier,
    viewportSnapshots: OrbitMapViewportSnapshots = OrbitMapViewportSnapshots(),
    viewportCommand: OrbitMapViewportCommand? = null,
    interactionEnabled: Boolean = true,
    swipeToDismissBoxState: SwipeToDismissBoxState? = null,
    onMapTap: () -> Unit = {},
    onViewportChanged: (OrbitMapEngine, OrbitMapViewportSnapshot) -> Unit = { _, _ -> },
    onViewportCommandApplied: (OrbitMapViewportCommand) -> Unit = {},
    onDisplayStateChanged: (OrbitMapDisplayState) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalWatchThemeColors.current
    val mapTapCallback = rememberUpdatedState(onMapTap)
    val viewportCallback = rememberUpdatedState(onViewportChanged)
    val commandAppliedCallback = rememberUpdatedState(onViewportCommandApplied)
    val displayStateCallback = rememberUpdatedState(onDisplayStateChanged)
    val commandLedger = remember { OrbitMapCommandLedger() }
    var actualViewports by remember {
        mutableStateOf(
            OrbitMapViewportSnapshots(
                osm = OrbitMapViewportPolicy.sanitize(
                    viewport = viewportSnapshots.osm,
                    minZoom = OsmMinZoom,
                    maxZoom = OsmMaxZoom
                ),
                offline = OrbitMapViewportPolicy.sanitize(
                    viewport = viewportSnapshots.offline,
                    minZoom = OrbitMapDefaults.OfflineWorldMinZoom.toDouble(),
                    maxZoom = OrbitMapDefaults.OfflineWorldMaxZoom.toDouble()
                )
            )
        )
    }
    var hasValidatedNetwork by remember(context) { mutableStateOf(context.hasValidatedNetwork()) }
    var tileEvent by remember { mutableStateOf(if (hasValidatedNetwork) OsmTileEvent.Loading else OsmTileEvent.NoValidatedNetwork) }
    val reducedTileDisplay = MapTileStateReducer.reduce(
        mode = mapTileMode,
        hasValidatedNetwork = hasValidatedNetwork,
        event = tileEvent
    )
    var lastInteractiveTileDisplay by remember {
        mutableStateOf(reducedTileDisplay)
    }
    SideEffect {
        if (interactionEnabled) {
            lastInteractiveTileDisplay = reducedTileDisplay
        }
    }
    val tileDisplay = OrbitMapAmbientDisplayPolicy.visibleDisplay(
        interactionEnabled = interactionEnabled,
        lastInteractiveDisplay = lastInteractiveTileDisplay,
        currentDisplay = reducedTileDisplay
    )
    val activeEngine = if (tileDisplay.showOfflineWorld) {
        OrbitMapEngine.OFFLINE
    } else {
        OrbitMapEngine.OSM
    }
    val mapInputModifier = swipeToDismissBoxState
        ?.takeIf {
            OrbitMapSwipeBackPolicy.usesComposeEdgeSwipe(Build.VERSION.SDK_INT)
        }
        ?.let { swipeState ->
        Modifier
            .fillMaxSize()
            .edgeSwipeToDismiss(swipeState)
        } ?: Modifier.fillMaxSize()

    fun reportViewport(
        engine: OrbitMapEngine,
        viewport: OrbitMapViewportSnapshot
    ) {
        actualViewports = actualViewports.updated(engine, viewport)
        viewportCallback.value(engine, viewport)
    }

    LaunchedEffect(activeEngine, actualViewports, tileDisplay) {
        displayStateCallback.value(
            OrbitMapDisplayState(
                engine = activeEngine,
                viewport = actualViewports.forEngine(activeEngine),
                tileState = tileDisplay
            )
        )
    }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update()

            override fun onLost(network: Network) = update()

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = update()

            private fun update() {
                mainHandler.post {
                    hasValidatedNetwork = context.hasValidatedNetwork()
                }
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }

    LaunchedEffect(mapTileMode, hasValidatedNetwork, interactionEnabled) {
        if (!interactionEnabled) return@LaunchedEffect
        tileEvent = OsmTileInteractionPolicy.eventOnInteractiveResume(
            hasValidatedNetwork = hasValidatedNetwork
        )
    }

    Box(modifier = modifier) {
        if (tileDisplay.showOfflineWorld) {
            OfflineWorldMapView(
                initialViewport = actualViewports.offline,
                minZoom = OrbitMapDefaults.OfflineWorldMinZoom,
                maxZoom = OrbitMapDefaults.OfflineWorldMaxZoom,
                colors = colors,
                station = station,
                satellitePosition = orbitMap.currentPosition,
                trackSegments = orbitMap.trackSegments,
                footprint = orbitMap.footprint,
                viewportCommand = viewportCommand,
                commandLedger = commandLedger,
                interactionEnabled = interactionEnabled,
                onMapTap = { mapTapCallback.value() },
                onViewportChanged = { viewport ->
                    reportViewport(OrbitMapEngine.OFFLINE, viewport)
                },
                onViewportCommandApplied = { command ->
                    commandAppliedCallback.value(command)
                },
                gestureStartInset = WatchUiMetrics.OrbitMapSystemBackGestureInset,
                modifier = mapInputModifier
            )
        } else {
            OsmOrbitMapContent(
                orbitMap = orbitMap,
                station = station,
                hasValidatedNetwork = hasValidatedNetwork,
                onTileEvent = { tileEvent = it },
                initialViewport = actualViewports.osm,
                viewportCommand = viewportCommand,
                commandLedger = commandLedger,
                interactionEnabled = interactionEnabled,
                onMapTap = { mapTapCallback.value() },
                onViewportChanged = { viewport ->
                    reportViewport(OrbitMapEngine.OSM, viewport)
                },
                onViewportCommandApplied = { command ->
                    commandAppliedCallback.value(command)
                },
                modifier = mapInputModifier
            )
        }
    }
}

@Composable
private fun OsmOrbitMapContent(
    orbitMap: OrbitMapUiState,
    station: StationLocation?,
    hasValidatedNetwork: Boolean,
    initialViewport: OrbitMapViewportSnapshot,
    viewportCommand: OrbitMapViewportCommand?,
    commandLedger: OrbitMapCommandLedger,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
    onMapTap: () -> Unit,
    onViewportChanged: (OrbitMapViewportSnapshot) -> Unit,
    onViewportCommandApplied: (OrbitMapViewportCommand) -> Unit,
    onTileEvent: (OsmTileEvent) -> Unit
) {
    var tileFailures by remember { mutableIntStateOf(0) }
    var tileSuccessObserved by remember { mutableStateOf(false) }
    val tileCallback = rememberUpdatedState(onTileEvent)
    val mapTapCallback = rememberUpdatedState(onMapTap)
    val viewportCallback = rememberUpdatedState(onViewportChanged)
    val commandAppliedCallback = rememberUpdatedState(onViewportCommandApplied)
    val viewportCommandState = rememberUpdatedState(viewportCommand)
    val interactionEnabledState = rememberUpdatedState(interactionEnabled)
    val context = LocalContext.current
    val satelliteLabel = stringResource(R.string.map_satellite)
    val stationLabel = stringResource(R.string.map_station)
    val colors = LocalWatchThemeColors.current
    val style = remember(colors) { MapOverlayStyle.from(colors) }
    val mapView = remember(context) { createOrbitMapView(context, initialViewport) }
    val viewportAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    var viewportRevision by remember(mapView) { mutableIntStateOf(0) }
    val trackOverlays = remember(mapView) { mutableListOf<Polyline>() }
    val trackArrowMarkers = remember(mapView) { mutableListOf<Marker>() }
    val footprintOverlays = remember(mapView) { mutableListOf<Polygon>() }
    val footprintLineOverlays = remember(mapView) { mutableListOf<Polyline>() }
    val overlayState = remember(mapView) { OsmOrbitOverlayStateHolder() }

    fun interruptViewportCommand() {
        val command = viewportCommandState.value ?: return
        if (
            commandLedger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OSM,
                interactionEnabled = interactionEnabledState.value
            )
        ) {
            commandLedger.markApplied(command)
            commandAppliedCallback.value(command)
        }
    }

    val gestureDetector = remember(mapView) {
        GestureDetector(
            mapView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    interruptViewportCommand()
                    return false
                }
            }
        )
    }
    val mapEventsOverlay = remember(mapView) {
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    return dispatchConfirmedMapTap(
                        mapView = mapView,
                        interactionEnabled = interactionEnabledState.value,
                        onMapTap = { mapTapCallback.value() }
                    )
                }

                override fun longPressHelper(point: GeoPoint): Boolean {
                    return false
                }
            }
        ).also { mapView.overlays.add(it) }
    }
    val trackArrowDrawable = remember(mapView, style.arrow) {
        OsmOverlayDrawableFactory.trackArrow(mapView.context, style.arrow)
    }
    val satelliteMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = OsmOverlayDrawableFactory.orbitMarker(mapView.context, style.satellite)
            mapView.overlays.add(this)
        }
    }
    val stationMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = stationLabel
            icon = OsmOverlayDrawableFactory.stationMarker(mapView.context, style.station)
            mapView.overlays.add(this)
        }
    }

    LaunchedEffect(style.satellite, style.station) {
        satelliteMarker.icon = OsmOverlayDrawableFactory.orbitMarker(mapView.context, style.satellite)
        stationMarker.icon = OsmOverlayDrawableFactory.stationMarker(mapView.context, style.station)
    }

    LaunchedEffect(
        mapView,
        viewportCommand?.id,
        viewportCommand?.engine,
        viewportCommand?.viewport,
        viewportCommand?.transition,
        viewportAnimationSpec,
        interactionEnabled
    ) {
        val command = viewportCommand ?: return@LaunchedEffect
        if (
            !commandLedger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OSM,
                interactionEnabled = interactionEnabled
            )
        ) {
            return@LaunchedEffect
        }
        val target = OrbitMapViewportPolicy.sanitize(
            viewport = command.viewport,
            minZoom = OsmMinZoom,
            maxZoom = OsmMaxZoom
        )
        if (command.transition == OrbitMapViewportTransition.ANIMATED) {
            val start = mapView.viewportSnapshot()
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = viewportAnimationSpec
            ) {
                if (
                    !commandLedger.shouldApply(
                        command = command,
                        engine = OrbitMapEngine.OSM,
                        interactionEnabled = interactionEnabledState.value
                    )
                ) {
                    return@animateTo
                }
                val frame = OrbitMapViewportMotionPolicy.interpolate(
                    start = start,
                    end = target,
                    progress = value.toDouble()
                )
                mapView.controller.setZoom(frame.zoom)
                mapView.controller.setCenter(
                    GeoPoint(frame.centerLatitude, frame.centerLongitude)
                )
                viewportRevision += 1
                viewportCallback.value(frame)
            }
        } else {
            mapView.controller.setZoom(target.zoom)
            mapView.controller.setCenter(
                GeoPoint(target.centerLatitude, target.centerLongitude)
            )
            viewportRevision += 1
            viewportCallback.value(target)
        }
        if (
            !commandLedger.shouldApply(
                command = command,
                engine = OrbitMapEngine.OSM,
                interactionEnabled = interactionEnabledState.value
            )
        ) {
            return@LaunchedEffect
        }
        commandLedger.markApplied(command)
        commandAppliedCallback.value(command)
    }

    LaunchedEffect(mapView, interactionEnabled) {
        mapView.setFlingEnabled(interactionEnabled)
        mapView.tileProvider.setUseDataConnection(hasValidatedNetwork && interactionEnabled)
        if (!interactionEnabled) {
            mapView.controller.stopAnimation(false)
            mapView.controller.stopPanning()
        } else if (hasValidatedNetwork) {
            mapView.invalidate()
        }
    }

    LaunchedEffect(hasValidatedNetwork, interactionEnabled) {
        if (!interactionEnabled) return@LaunchedEffect
        tileFailures = 0
        tileSuccessObserved = false
        tileCallback.value(
            OsmTileInteractionPolicy.eventOnInteractiveResume(hasValidatedNetwork)
        )
        mapView.tileProvider.setUseDataConnection(hasValidatedNetwork && interactionEnabled)
        if (hasValidatedNetwork) mapView.invalidate()
        if (hasValidatedNetwork && interactionEnabled) {
            kotlinx.coroutines.delay(TileNoSuccessTimeoutMillis)
            if (interactionEnabledState.value && !tileSuccessObserved) {
                tileCallback.value(OsmTileEvent.Unavailable)
            }
        }
    }

    DisposableEffect(mapView, hasValidatedNetwork) {
        val tileHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (
                    !OsmTileInteractionPolicy.acceptsCallback(
                        interactionEnabled = interactionEnabledState.value
                    )
                ) {
                    return
                }
                when (msg.what) {
                    MapTileProviderBase.MAPTILE_SUCCESS_ID -> {
                        tileFailures = 0
                        tileSuccessObserved = true
                        tileCallback.value(OsmTileEvent.Success)
                    }

                    MapTileProviderBase.MAPTILE_FAIL_ID -> {
                        tileFailures += 1
                        tileCallback.value(
                            if (!hasValidatedNetwork) {
                                OsmTileEvent.NoValidatedNetwork
                            } else if (tileFailures >= TileFailureThreshold) {
                                OsmTileEvent.Unavailable
                            } else {
                                OsmTileEvent.Loading
                            }
                        )
                    }
                }
            }
        }
        mapView.tileProvider.setTileRequestCompleteHandler(tileHandler)
        onDispose {
            tileHandler.removeCallbacksAndMessages(null)
        }
    }

    DisposableEffect(mapView) {
        mapView.setOnTouchListener { _, event ->
            if (interactionEnabledState.value) {
                gestureDetector.onTouchEvent(event)
                if (
                    event.actionMasked == MotionEvent.ACTION_MOVE ||
                    event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
                ) {
                    interruptViewportCommand()
                }
            }
            !interactionEnabledState.value
        }
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                viewportRevision += 1
                viewportCallback.value(event.source.viewportSnapshot())
                return false
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                viewportRevision += 1
                viewportCallback.value(event.source.viewportSnapshot())
                return false
            }
        }
        mapView.addMapListener(listener)
        mapView.onResume()
        onDispose {
            mapView.setOnTouchListener(null)
            mapView.removeMapListener(listener)
            mapView.overlays.remove(mapEventsOverlay)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            view.tileProvider.setUseDataConnection(hasValidatedNetwork && interactionEnabled)
            val currentOverlaySnapshot = OsmOrbitOverlaySnapshot(
                trackSegments = orbitMap.trackSegments,
                footprint = orbitMap.footprint,
                selectedCatalogNumber = orbitMap.selectedCatalogNumber,
                trackMinuteBucketMillis = orbitMap.lastUpdatedMillis.minuteBucket(),
                trackStyle = style.track,
                arrowStyle = style.arrow,
                footprintStyle = style.footprint,
                viewportRevision = viewportRevision.toLong()
            )
            val decision = OsmOrbitOverlayUpdatePolicy.decide(
                previous = overlayState.snapshot,
                current = currentOverlaySnapshot
            )
            if (decision.rebuildFootprint) {
                rebuildFootprintOverlays(
                    view = view,
                    orbitMap = orbitMap,
                    style = style,
                    footprintOverlays = footprintOverlays,
                    footprintLineOverlays = footprintLineOverlays,
                    trackOverlays = trackOverlays,
                    trackArrowMarkers = trackArrowMarkers
                )
            }
            if (decision.rebuildTrackAndArrows) {
                rebuildTrackOverlays(
                    view = view,
                    trackSegments = orbitMap.trackSegments,
                    style = style,
                    arrowDrawable = trackArrowDrawable,
                    trackOverlays = trackOverlays,
                    trackArrowMarkers = trackArrowMarkers
                )
            }
            overlayState.snapshot = currentOverlaySnapshot
            orbitMap.currentPosition?.let { point ->
                satelliteMarker.position = point.toGeoPoint()
                satelliteMarker.title = orbitMap.selectedSatellite?.displayName ?: satelliteLabel
                satelliteMarker.isEnabled = true
            } ?: run {
                satelliteMarker.isEnabled = false
            }
            station?.let {
                stationMarker.position = GeoPoint(it.latitude, it.longitude)
                stationMarker.title = it.qthLocator ?: stationLabel
                stationMarker.isEnabled = true
            } ?: run {
                stationMarker.isEnabled = false
            }
            ensureOrbitOverlayInputOrder(
                view = view,
                inputOverlay = mapEventsOverlay,
                satelliteMarker = satelliteMarker,
                stationMarker = stationMarker
            )
            view.invalidate()
        }
    )
}

private fun ensureOrbitOverlayInputOrder(
    view: MapView,
    inputOverlay: MapEventsOverlay,
    satelliteMarker: Marker,
    stationMarker: Marker
) {
    view.overlays.remove(inputOverlay)
    view.overlays.remove(satelliteMarker)
    view.overlays.remove(stationMarker)
    view.overlays.add(satelliteMarker)
    view.overlays.add(stationMarker)
    view.overlays.add(inputOverlay)
}

private fun rebuildFootprintOverlays(
    view: MapView,
    orbitMap: OrbitMapUiState,
    style: MapOverlayStyle,
    footprintOverlays: MutableList<Polygon>,
    footprintLineOverlays: MutableList<Polyline>,
    trackOverlays: List<Polyline>,
    trackArrowMarkers: List<Marker>
) {
    val geometry = orbitMap.footprint?.geometry
    val polygons = geometry?.fillPolygons.orEmpty().filter { it.size >= 3 }
    polygons.forEachIndexed { index, points ->
        val polygon = footprintOverlays.getOrNull(index) ?: Polygon(view).also { overlay ->
            footprintOverlays += overlay
            addBeforeTrack(view, overlay, trackOverlays, trackArrowMarkers)
        }
        polygon.points = points.map { it.toGeoPoint() }
        polygon.fillPaint.color = style.footprint.fillColor.copy(alpha = style.footprint.fillAlpha).toArgb()
        polygon.outlinePaint.color = android.graphics.Color.TRANSPARENT
        polygon.outlinePaint.strokeWidth = 0f
    }
    trimOverlays(view, footprintOverlays, desiredSize = polygons.size)

    val segments = geometry?.outlineSegments.orEmpty().filter { it.size >= 2 }
    segments.forEachIndexed { index, segment ->
        val line = footprintLineOverlays.getOrNull(index) ?: Polyline(view).also { overlay ->
            footprintLineOverlays += overlay
            addBeforeTrack(view, overlay, trackOverlays, trackArrowMarkers)
        }
        line.setPoints(segment.map { it.toGeoPoint() })
        line.outlinePaint.color = style.footprint.outlineColor.copy(alpha = style.footprint.outlineAlpha).toArgb()
        line.outlinePaint.strokeWidth = style.footprint.outlineStrokePx
        line.outlinePaint.isAntiAlias = true
    }
    trimOverlays(view, footprintLineOverlays, desiredSize = segments.size)
}

private fun addBeforeTrack(
    view: MapView,
    overlay: Overlay,
    trackOverlays: List<Polyline>,
    trackArrowMarkers: List<Marker>
) {
    val insertionIndex = (trackOverlays + trackArrowMarkers)
        .map { view.overlays.indexOf(it) }
        .filter { it >= 0 }
        .minOrNull()
        ?: view.overlays.size
    view.overlays.add(insertionIndex, overlay)
}

private fun <T : Overlay> removeOverlays(view: MapView, overlays: MutableList<T>) {
    overlays.forEach { view.overlays.remove(it) }
    overlays.clear()
}

private fun <T : Overlay> trimOverlays(
    view: MapView,
    overlays: MutableList<T>,
    desiredSize: Int
) {
    while (overlays.size > desiredSize) {
        view.overlays.remove(overlays.removeAt(overlays.lastIndex))
    }
}

private fun rebuildTrackOverlays(
    view: MapView,
    trackSegments: List<List<GroundTrackPoint>>,
    style: MapOverlayStyle,
    arrowDrawable: Drawable,
    trackOverlays: MutableList<Polyline>,
    trackArrowMarkers: MutableList<Marker>
) {
    trackOverlays.forEach { view.overlays.remove(it) }
    trackOverlays.clear()
    trackArrowMarkers.forEach { view.overlays.remove(it) }
    trackArrowMarkers.clear()
    trackSegments.forEach { segment ->
        if (segment.size >= 2) {
            trackOverlays += Polyline(view).apply {
                setPoints(segment.map { it.toGeoPoint() })
                outlinePaint.color = style.track.color.toArgb()
                outlinePaint.strokeWidth = style.track.strokePx
                outlinePaint.isAntiAlias = true
                view.overlays.add(this)
            }
            trackArrowMarkers += orbitTrackArrowMarkers(
                mapView = view,
                segment = segment,
                arrowDrawable = arrowDrawable
            ).onEach { marker ->
                view.overlays.add(marker)
            }
        }
    }
}

private fun createOrbitMapView(
    context: Context,
    initialViewport: OrbitMapViewportSnapshot
): MapView {
    val appContext = context.applicationContext
    configureOsmDroid(appContext)
    val viewport = OrbitMapViewportPolicy.sanitize(
        viewport = initialViewport,
        minZoom = OsmMinZoom,
        maxZoom = OsmMaxZoom
    )
    val provider = MapTileProviderBasic(
        SimpleRegisterReceiver(appContext),
        OrbitMapNetworkAvailabilityCheck(appContext),
        TileSourceFactory.MAPNIK,
        appContext,
        null
    )
    return MapView(context, provider).apply {
        useComposeAccessibilityOwner()
        setMultiTouchControls(true)
        minZoomLevel = OsmMinZoom
        maxZoomLevel = OsmMaxZoom
        controller.setZoom(viewport.zoom)
        controller.setCenter(GeoPoint(viewport.centerLatitude, viewport.centerLongitude))
        isTilesScaledToDpi = true
        setBuiltInZoomControls(false)
    }
}

private fun MapView.viewportSnapshot(): OrbitMapViewportSnapshot {
    val center = mapCenter
    return OrbitMapViewportSnapshot(
        centerLatitude = center.latitude,
        centerLongitude = center.longitude,
        zoom = zoomLevelDouble
    )
}

private fun GroundTrackPoint.toGeoPoint(): GeoPoint {
    return GeoPoint(latitudeDegrees, longitudeDegrees)
}

private fun orbitTrackArrowMarkers(
    mapView: MapView,
    segment: List<GroundTrackPoint>,
    arrowDrawable: Drawable
): List<Marker> {
    if (segment.size < 2) return emptyList()
    val projection = mapView.projection
    val density = mapView.context.resources.displayMetrics.density
    val screenPoints = segment.map { point ->
        val screen = projection.toPixels(point.toGeoPoint(), null)
        TrackDirectionPoint(screen.x.toFloat(), screen.y.toFloat())
    }
    return TrackDirectionArrowPolicy.arrowsFor(
        points = screenPoints,
        arrowSizePx = WatchUiMetrics.OrbitMapTrackArrowSize.value * density,
        minSegmentLengthPx = WatchUiMetrics.OrbitMapTrackArrowMinSegmentLength.value * density,
        maxSegmentLengthPx = WatchUiMetrics.OrbitMapTrackArrowMaxSegmentLength.value * density
    ).mapNotNull { arrow ->
        val geoPoint = projection.geoPointFor(arrow.tip)
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setPosition(geoPoint)
            setRotation(arrow.rotationDegrees)
            setFlat(true)
            icon = arrowDrawable
        }
    }
}

private fun Long.minuteBucket(): Long = (this / TrackOverlayMinuteMillis) * TrackOverlayMinuteMillis

private class OsmOrbitOverlayStateHolder(
    var snapshot: OsmOrbitOverlaySnapshot? = null
)

private fun Projection.geoPointFor(point: TrackDirectionPoint): GeoPoint {
    val geoPoint = fromPixels(point.x.toInt(), point.y.toInt())
    return GeoPoint(geoPoint.latitude, geoPoint.longitude)
}

private fun configureOsmDroid(context: Context) {
    val osmBase = File(context.cacheDir, "osmdroid")
    val osmTiles = File(osmBase, "tiles")
    osmTiles.mkdirs()
    Configuration.getInstance().apply {
        userAgentValue = OsmUserAgent.forVersion(BuildConfig.VERSION_NAME)
        setOsmdroidBasePath(osmBase)
        setOsmdroidTileCache(osmTiles)
        tileDownloadThreads = 2
        tileFileSystemThreads = 1
        tileDownloadMaxQueueSize = 20
        tileFileSystemMaxQueueSize = 20
        tileFileSystemCacheMaxBytes = 24L * 1024L * 1024L
        tileFileSystemCacheTrimBytes = 16L * 1024L * 1024L
    }
}

private class OrbitMapNetworkAvailabilityCheck(
    private val context: Context
) : INetworkAvailablityCheck {
    override fun getNetworkAvailable(): Boolean = context.hasValidatedNetwork()

    override fun getWiFiNetworkAvailable(): Boolean = context.hasValidatedNetwork(NetworkCapabilities.TRANSPORT_WIFI)

    override fun getCellularDataNetworkAvailable(): Boolean = context.hasValidatedNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)

    override fun getRouteToPathExists(hostAddress: Int): Boolean = getNetworkAvailable()
}

private fun Context.hasValidatedNetwork(requiredTransport: Int? = null): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    val hasTransport = requiredTransport == null || capabilities.hasTransport(requiredTransport)
    return hasTransport &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
