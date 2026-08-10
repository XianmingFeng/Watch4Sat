package com.xianming.watch4sat.wear

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.xianming.watch4sat.data.settings.AppThemePreset
import com.xianming.watch4sat.domain.model.DopplerReading
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.wear.radar.RadarOrientationSensorKind
import com.xianming.watch4sat.wear.radar.RadarOrientationSnapshot
import com.xianming.watch4sat.wear.radar.RadarOrientationStatus
import com.xianming.watch4sat.wear.radar.RadarPointing
import com.xianming.watch4sat.wear.radar.RadarScreen
import com.xianming.watch4sat.wear.radar.RadarSensorAccuracy
import com.xianming.watch4sat.wear.radar.RadarVisualCue
import com.xianming.watch4sat.wear.radar.RadarVisualCueState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors
import com.xianming.watch4sat.wear.theme.WatchThemeCatalog
import com.xianming.watch4sat.wear.theme.WatchTypography

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RoundListPageWearPreview() {
    Watch4SatWearPreviewTheme {
        RoundListPage(title = "Passes") {
            item {
                val itemScope = this
                RoundAction(
                    label = "Next pass",
                    modifier = Modifier.fillMaxWidth(),
                    itemScope = itemScope,
                    onClick = {}
                )
            }
            item {
                StatusTextBlock(
                    title = "ISS",
                    subtitle = "AOS 21:18 · Max 62°"
                )
            }
            item {
                val itemScope = this
                InfoCard(
                    title = "QTH",
                    subtitle = "PM01 · GPS ready",
                    itemScope = itemScope
                )
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RoundVisualPageWearPreview() {
    Watch4SatWearPreviewTheme {
        RoundVisualPage(title = "Radar") {
            StatusTextBlock(
                title = "SO-50",
                subtitle = "Az 214° · El 38°"
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarScreenWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(),
            overlayOpen = false,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation()
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarTrackCueWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(),
            overlayOpen = false,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation(),
            visualCueOverride = RadarVisualCue(RadarVisualCueState.TrackAligned, animated = false)
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarSatelliteCueWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(),
            overlayOpen = false,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation(),
            visualCueOverride = RadarVisualCue(RadarVisualCueState.SatelliteAligned, animated = false)
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarOverlayWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(),
            overlayOpen = true,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation()
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarOverlayLongNameWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(
                satelliteName = "NOAA WEATHER DEMONSTRATION SATELLITE"
            ),
            overlayOpen = true,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation()
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun RadarOverlayMissingFrequencyWearPreview() {
    Watch4SatWearPreviewTheme {
        RadarScreen(
            state = previewRadarState(missingUplink = true),
            overlayOpen = true,
            onOverlayOpenChange = {},
            onSelectRadarTransmitter = {},
            onSelectRadarPass = {},
            orientationOverride = previewRadarOrientation()
        )
    }
}

@Composable
private fun Watch4SatWearPreviewTheme(content: @Composable () -> Unit) {
    val previewColors = WatchThemeCatalog.colorsFor(AppThemePreset.SKY_BLUE)
    MaterialTheme(
        colorScheme = WatchThemeCatalog.wearColorSchemeFor(previewColors),
        typography = WatchTypography
    ) {
        CompositionLocalProvider(
            LocalWatchThemeColors provides previewColors
        ) {
            AppScaffold {
                content()
            }
        }
    }
}

private fun previewRadarState(
    satelliteName: String = "ISS (ZARYA)",
    missingUplink: Boolean = false
): WatchUiState {
    val pass = SatellitePass(
        catalogNumber = 25544,
        satelliteName = satelliteName,
        aosMillis = 1_800_000L,
        losMillis = 2_400_000L,
        tcaMillis = 2_100_000L,
        maxElevationDegrees = 72.0,
        aosAzimuthDegrees = 330.0,
        losAzimuthDegrees = 160.0
    )
    val transmitter = TransmitterRecord(
        uuid = "preview-fm",
        catalogNumber = 25544,
        description = "FM voice",
        isAlive = true,
        status = "active",
        downlinkLowHz = 145_800_000L,
        downlinkHighHz = null,
        downlinkMode = "FM",
        uplinkLowHz = if (missingUplink) null else 437_800_000L,
        uplinkHighHz = null,
        uplinkMode = if (missingUplink) null else "FM",
        isInverted = false
    )
    return WatchUiState(
        focusedPass = pass,
        passCards = listOf(
            pass to PassCardUi(
                catalogNumber = pass.catalogNumber,
                satelliteName = pass.satelliteName,
                aosCountdown = "in 3m",
                aosTime = "00:30",
                losTime = "00:40",
                tcaTime = "00:35",
                maxElevation = "72°",
                aosAzimuth = "330°",
                losAzimuth = "160°",
                duration = "10m",
                modeFrequencyHint = "FM 145.800 MHz DL / 437.800 MHz UL",
                isActive = true,
                isUpcoming = false
            ),
            pass.copy(aosMillis = 90_000_000L, losMillis = 90_600_000L, tcaMillis = 90_300_000L) to PassCardUi(
                catalogNumber = pass.catalogNumber,
                satelliteName = pass.satelliteName,
                aosCountdown = "tomorrow",
                aosTime = "01:00",
                losTime = "01:10",
                tcaTime = "01:05",
                maxElevation = "58°",
                aosAzimuth = "288°",
                losAzimuth = "124°",
                duration = "10m",
                modeFrequencyHint = "FM 145.800 MHz DL / 437.800 MHz UL",
                isActive = false,
                isUpcoming = true
            )
        ),
        focusedTrack = listOf(
            RadarTrackPoint(1_800_000L, 330.0, 0.0, true, RadarTrackLabel.AOS),
            RadarTrackPoint(1_950_000L, 20.0, 28.0, true, RadarTrackLabel.NONE),
            RadarTrackPoint(2_100_000L, 90.0, 72.0, true, RadarTrackLabel.NONE),
            RadarTrackPoint(2_400_000L, 160.0, 0.0, true, RadarTrackLabel.LOS)
        ),
        focusedPosition = OrbitalPosition(
            timeMillis = 2_000_000L,
            azimuthDegrees = 42.0,
            elevationDegrees = 38.0,
            rangeRateKmPerSecond = -2.1
        ),
        focusedTransmitters = listOf(transmitter),
        focusedTransmitter = transmitter,
        doppler = DopplerReading(
            baseDownlinkHz = 145_800_000L,
            correctedDownlinkHz = 145_803_200L,
            downlinkOffsetHz = 3_200L,
            downlinkOffsetKhz = 3.2,
            baseUplinkHz = if (missingUplink) null else 437_800_000L,
            correctedUplinkHz = if (missingUplink) null else 437_790_400L,
            uplinkOffsetHz = if (missingUplink) null else -9_600L,
            uplinkOffsetKhz = if (missingUplink) null else -9.6
        ),
        radarNowMillis = 2_000_000L
    )
}

private fun previewRadarOrientation(): RadarOrientationSnapshot {
    return RadarOrientationSnapshot(
        sensorKind = RadarOrientationSensorKind.RotationVector,
        status = RadarOrientationStatus.PointingAssistActive,
        accuracy = RadarSensorAccuracy.High,
        pointing = RadarPointing(
            azimuthDegrees = 70.0,
            elevationDegrees = 28.0
        ),
        referenceAzimuthDegrees = 70.0
    )
}
