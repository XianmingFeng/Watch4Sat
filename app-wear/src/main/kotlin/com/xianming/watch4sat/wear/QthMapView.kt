package com.xianming.watch4sat.wear

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import com.xianming.watch4sat.BuildConfig
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.wear.map.MapOverlayStyle
import com.xianming.watch4sat.wear.map.MapTileStateReducer
import com.xianming.watch4sat.wear.map.OfflineWorldMapView
import com.xianming.watch4sat.wear.map.OsmTileEvent
import com.xianming.watch4sat.wear.map.OsmOverlayDrawableFactory
import com.xianming.watch4sat.wear.map.OsmUserAgent
import com.xianming.watch4sat.wear.map.TransientMapBadge
import com.xianming.watch4sat.wear.map.MapBadgeVisibilityPolicy
import com.xianming.watch4sat.wear.map.resolveText
import com.xianming.watch4sat.wear.map.drawMapCrosshair
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import org.osmdroid.config.Configuration
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
import org.osmdroid.views.overlay.Marker
import java.io.File

private const val TileFailureThreshold = 4
private const val TileNoSuccessTimeoutMillis = 8_000L

@Composable
fun QthMapView(
    station: StationLocation,
    mapTileMode: MapTileMode,
    modifier: Modifier = Modifier,
    onCenterChanged: (Double, Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapDescription = stringResource(R.string.qth_map_description)
    val colors = LocalWatchThemeColors.current
    val style = MapOverlayStyle.from(colors)
    var hasValidatedNetwork by remember(context) { mutableStateOf(context.hasValidatedNetwork()) }
    var tileEvent by remember { mutableStateOf(if (hasValidatedNetwork) OsmTileEvent.Loading else OsmTileEvent.NoValidatedNetwork) }
    val tileDisplay = MapTileStateReducer.reduce(
        mode = mapTileMode,
        hasValidatedNetwork = hasValidatedNetwork,
        event = tileEvent
    )
    val tileStatusLabel = tileDisplay.status.resolveText()

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

    LaunchedEffect(mapTileMode, hasValidatedNetwork) {
        tileEvent = if (hasValidatedNetwork) OsmTileEvent.Loading else OsmTileEvent.NoValidatedNetwork
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WatchUiMetrics.QthMapAspectRatio)
            .clip(RoundedCornerShape(28.dp))
            .semantics {
                contentDescription = mapDescription
            }
    ) {
        if (tileDisplay.showOfflineWorld) {
            OfflineWorldMapView(
                centerLatitude = station.latitude,
                centerLongitude = station.longitude,
                station = station,
                showCrosshair = true,
                colors = colors,
                initialZoom = 2.8f,
                minZoom = 1.4f,
                maxZoom = 8f,
                onCenterChanged = onCenterChanged,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            OsmQthMapContent(
                station = station,
                hasValidatedNetwork = hasValidatedNetwork,
                onCenterChanged = onCenterChanged,
                onTileEvent = { tileEvent = it },
                modifier = Modifier.fillMaxSize()
            )
            SelectedCenterCrosshair(style = style, modifier = Modifier.fillMaxSize())
        }
        val mapStatusStyle = MaterialTheme.typography.bodyExtraSmall.copy(
            fontSize = WatchTypographyTokens.MapStatus
        )
        val mapAttributionStyle = MaterialTheme.typography.bodyExtraSmall.copy(
            fontSize = WatchTypographyTokens.MapAttribution
        )
        TransientMapBadge(
            label = tileStatusLabel,
            textStyle = mapStatusStyle,
            transient = MapBadgeVisibilityPolicy.isTransientStatus(tileDisplay.status),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
        TransientMapBadge(
            label = if (tileDisplay.showOsmAttribution) {
                stringResource(R.string.map_osm_attribution)
            } else {
                stringResource(R.string.map_natural_earth_attribution)
            },
            textStyle = mapAttributionStyle,
            transient = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
        )
    }
}

@Composable
private fun OsmQthMapContent(
    station: StationLocation,
    hasValidatedNetwork: Boolean,
    modifier: Modifier = Modifier,
    onCenterChanged: (Double, Double) -> Unit,
    onTileEvent: (OsmTileEvent) -> Unit
) {
    var tileFailures by remember { mutableIntStateOf(0) }
    val centerCallback = rememberUpdatedState(onCenterChanged)
    val tileCallback = rememberUpdatedState(onTileEvent)
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalWatchThemeColors.current
    val style = MapOverlayStyle.from(colors)
    val mapView = remember(context) { createMapView(context) }
    val stationMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = mapView.context.getString(R.string.qth_saved_station)
            icon = OsmOverlayDrawableFactory.stationMarker(mapView.context, style.station)
            mapView.overlays.add(this)
        }
    }

    LaunchedEffect(style.station) {
        stationMarker.icon = OsmOverlayDrawableFactory.stationMarker(mapView.context, style.station)
    }

    LaunchedEffect(hasValidatedNetwork) {
        tileFailures = 0
        tileCallback.value(if (hasValidatedNetwork) OsmTileEvent.Loading else OsmTileEvent.NoValidatedNetwork)
        mapView.tileProvider.setUseDataConnection(hasValidatedNetwork)
        if (hasValidatedNetwork) {
            mapView.invalidate()
        }
    }

    LaunchedEffect(hasValidatedNetwork) {
        if (hasValidatedNetwork) {
            kotlinx.coroutines.delay(TileNoSuccessTimeoutMillis)
            if (tileFailures == 0) {
                tileCallback.value(OsmTileEvent.Unavailable)
            }
        }
    }

    LaunchedEffect(station.latitude, station.longitude) {
        val point = GeoPoint(station.latitude, station.longitude)
        stationMarker.position = point
        mapView.controller.setCenter(point)
        centerCallback.value(station.latitude, station.longitude)
        mapView.invalidate()
    }

    DisposableEffect(mapView, hasValidatedNetwork) {
        val tileHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MapTileProviderBase.MAPTILE_SUCCESS_ID -> {
                        tileFailures = 0
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
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                publishCenter(event.source, centerCallback.value)
                return false
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                publishCenter(event.source, centerCallback.value)
                return false
            }
        }
        mapView.addMapListener(listener)
        mapView.onResume()
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            stationMarker.title = view.context.getString(
                R.string.qth_saved_station_with_locator,
                station.qthLocator ?: view.context.getString(R.string.qth_title)
            )
            view.tileProvider.setUseDataConnection(hasValidatedNetwork)
        }
    )
}

private fun createMapView(context: Context): MapView {
    val appContext = context.applicationContext
    configureOsmDroid(appContext)
    val provider = MapTileProviderBasic(
        SimpleRegisterReceiver(appContext),
        ValidatedNetworkAvailabilityCheck(appContext),
        TileSourceFactory.MAPNIK,
        appContext,
        null
    )
    return MapView(context, provider).apply {
        useComposeAccessibilityOwner()
        setMultiTouchControls(true)
        minZoomLevel = 2.0
        maxZoomLevel = 18.0
        controller.setZoom(7.0)
        isTilesScaledToDpi = true
        setBuiltInZoomControls(false)
    }
}

@Composable
private fun SelectedCenterCrosshair(
    style: MapOverlayStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawMapCrosshair(style.crosshair)
    }
}

private fun publishCenter(
    mapView: MapView,
    onCenterChanged: (Double, Double) -> Unit
) {
    val center = mapView.mapCenter
    onCenterChanged(center.latitude, center.longitude)
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

private class ValidatedNetworkAvailabilityCheck(
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
