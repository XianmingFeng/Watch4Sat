package com.xianming.watch4sat.wear.orbit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.footprint.SatelliteFootprintCalculator
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.map.OfflineWorldMapView
import com.xianming.watch4sat.wear.state.OrbitMapChromeState
import com.xianming.watch4sat.wear.state.OrbitMapSessionState
import com.xianming.watch4sat.wear.state.OrbitMapUiState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchThemeCatalog
import com.xianming.watch4sat.wear.theme.WatchTypography

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun OrbitMapTopTimeTextPreview() {
    OrbitMapPreview()
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun OrbitMapLongDiameterTimeTextPreview() {
    OrbitMapPreview(altitudeKm = 35_786.0)
}

@WearPreviewDevices
@Composable
private fun OrbitMapTopTimeTextHiddenWithChromePreview() {
    OrbitMapPreview(chromeVisible = false)
}

@WearPreviewDevices
@Composable
private fun OrbitMapSingleSatellitePreview() {
    OrbitMapPreview(singleSatellite = true)
}

@WearPreviewDevices
@Composable
private fun OrbitMapClockOnlyTimeTextPreview() {
    OrbitMapPreview(noData = true)
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun OrbitMapLongNameErrorPreview() {
    OrbitMapPreview(longName = true, predictionError = true)
}

@WearPreviewDevices
@Composable
private fun OrbitMapFrozenAmbientTimeTextPreview() {
    OrbitMapPreview(isAmbient = true)
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun OrbitMapOfflineAttributionPreview() {
    OrbitMapPreview(offlineWorld = true)
}

@Composable
private fun OrbitMapPreview(
    chromeVisible: Boolean = true,
    singleSatellite: Boolean = false,
    noData: Boolean = false,
    longName: Boolean = false,
    predictionError: Boolean = false,
    isAmbient: Boolean = false,
    offlineWorld: Boolean = false,
    altitudeKm: Double = 418.0
) {
    val colors = WatchThemeCatalog.colorsFor(AppThemePreset.SKY_BLUE)
    val first = previewSatellite(
        25_544,
        if (longName) "INTERNATIONAL SPACE STATION (ZARYA)" else "ISS (ZARYA)"
    )
    val second = previewSatellite(33_591, "NOAA 19")
    val position = GroundTrackPoint(
        timeMillis = 1_785_301_122_000L,
        latitudeDegrees = 31.23,
        longitudeDegrees = 121.47,
        altitudeKm = altitudeKm
    )
    val candidates = when {
        noData -> emptyList()
        singleSatellite -> listOf(first)
        else -> listOf(first, second)
    }
    val orbitMap = OrbitMapUiState(
        candidates = candidates,
        selectedCatalogNumber = first.catalogNumber.takeUnless { noData },
        selectedSatellite = first.takeUnless { noData },
        currentPosition = position.takeUnless { noData || predictionError },
        trackSegments = listOf(
            listOf(
                position.copy(latitudeDegrees = -22.0, longitudeDegrees = 84.0),
                position,
                position.copy(latitudeDegrees = 48.0, longitudeDegrees = 166.0)
            )
        ).takeUnless { noData || predictionError }.orEmpty(),
        footprint = SatelliteFootprintCalculator.calculate(
            point = position.takeUnless { noData || predictionError },
            generatedAtMillis = position.timeMillis
        ),
        message = when {
            noData -> stringResource(R.string.orbit_map_preview_refresh_tle)
            predictionError -> stringResource(
                R.string.orbit_map_preview_prediction_unavailable
            )
            else -> stringResource(
                R.string.orbit_map_preview_updated,
                first.displayName
            )
        },
        lastUpdatedMillis = position.timeMillis
    )

    MaterialTheme(
        colorScheme = WatchThemeCatalog.wearColorSchemeFor(colors),
        typography = WatchTypography
    ) {
        CompositionLocalProvider(LocalWatchThemeColors provides colors) {
            AppScaffold {
                OrbitMapScreen(
                    state = WatchUiState(
                        settings = Watch4SatSettings(
                            mapTileMode = if (offlineWorld) {
                                MapTileMode.OFFLINE_WORLD
                            } else {
                                MapTileMode.AUTO
                            }
                        ),
                        satellites = candidates,
                        orbitMap = orbitMap
                    ),
                    onPrevious = {},
                    onNext = {},
                    onOpenDetail = {},
                    onOpenData = {},
                    initialSession = OrbitMapSessionState(
                        selectedCatalogNumber = orbitMap.selectedCatalogNumber,
                        chrome = OrbitMapChromeState(chromeVisible)
                    ),
                    isAmbientOverride = isAmbient,
                    mapContent = { renderState ->
                        OfflineWorldMapView(
                            centerLatitude = renderState.orbitMap.currentPosition
                                ?.latitudeDegrees ?: 0.0,
                            centerLongitude = renderState.orbitMap.currentPosition
                                ?.longitudeDegrees ?: 0.0,
                            initialZoom = 2f,
                            minZoom = 1f,
                            maxZoom = 5f,
                            colors = colors,
                            satellitePosition = renderState.orbitMap.currentPosition,
                            trackSegments = renderState.orbitMap.trackSegments,
                            footprint = renderState.orbitMap.footprint,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                )
            }
        }
    }
}

private fun previewSatellite(
    catalogNumber: Int,
    name: String
): SatelliteRecord {
    return SatelliteRecord(
        catalogNumber = catalogNumber,
        displayName = name,
        selected = true,
        orbitalData = OrbitalData(
            name = name,
            catalogNumber = catalogNumber,
            epoch = 26_210.5,
            meanMotion = 15.5,
            eccentricity = 0.001,
            inclinationDegrees = 51.6,
            rightAscensionAscendingNodeDegrees = 123.45,
            argumentOfPerigeeDegrees = 87.65,
            meanAnomalyDegrees = 10.25,
            bstar = 0.0
        )
    )
}
