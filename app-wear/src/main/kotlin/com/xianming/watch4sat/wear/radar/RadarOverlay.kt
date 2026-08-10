package com.xianming.watch4sat.wear.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.time.AndroidClockTimeFormatter
import com.xianming.watch4sat.wear.state.RadarOverlayPolicy
import com.xianming.watch4sat.wear.state.RadarTransmitterSelector
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarOverlay(
    visible: Boolean,
    state: WatchUiState,
    orientation: RadarOrientationSnapshot,
    plotRotationDegrees: State<Float>,
    displayPosition: RadarDisplayPoint?,
    onClose: () -> Unit,
    onSelectRadarTransmitter: (String?) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val clockTimeFormatter = remember(context, configuration) {
        AndroidClockTimeFormatter.create(context)
    }
    val detailsDescription = stringResource(R.string.radar_details)
    val closeDetailsDescription = stringResource(R.string.radar_close_details)
    val switchTransmitterDescription = stringResource(
        R.string.radar_switch_transmitter
    )
    val textCatalog = radarTextCatalog()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(durationMillis = 160)) +
                scaleIn(tween(durationMillis = 160), initialScale = 0.96f),
            exit = fadeOut(tween(durationMillis = 140)) +
                scaleOut(tween(durationMillis = 140), targetScale = 0.96f)
        ) {
            val colors = LocalWatchThemeColors.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black)
            ) {
                RadarSkyPlot(
                    track = state.focusedTrack,
                    position = state.focusedPosition,
                    displayPosition = displayPosition,
                    pointing = orientation.pointing,
                    referenceAzimuthDegrees = null,
                    colors = colors,
                    updateMode = RadarPowerPolicy.updateMode(isAmbient = false),
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            rotationZ = plotRotationDegrees.value
                        }
                        .blur(8.dp),
                    visualCueEnabled = false
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = Color.Black.copy(alpha = 0.82f))
                )
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(
                            start = 14.dp,
                            end = 10.dp,
                            top = WatchUiMetrics.RadarOverlayTopPadding,
                            bottom = WatchUiMetrics.RadarOverlayBottomPadding
                        )
                        .verticalScroll(rememberScrollState())
                        .semantics { contentDescription = detailsDescription },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.focusedPass?.satelliteName
                                ?: stringResource(R.string.radar_no_pass),
                            maxLines = RadarOverlayPolicy.satelliteNameMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.semantics {
                                contentDescription = closeDetailsDescription
                            }
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        }
                    }

                    state.focusedPass?.let { pass ->
                        OverlayLine(
                            stringResource(R.string.radar_pass),
                            RadarUiText.passTiming(
                                pass = pass,
                                textCatalog = textCatalog,
                                clockTimeFormatter = clockTimeFormatter
                            )
                        )
                        OverlayLine(
                            stringResource(R.string.radar_countdown),
                            RadarUiText.countdownLine(
                                pass = pass,
                                nowMillis = state.radarNowMillis,
                                textCatalog = textCatalog
                            )
                        )
                    }
                    OverlayLine(
                        stringResource(R.string.radar_angles),
                        RadarUiText.angleLine(state.focusedPosition, textCatalog)
                    )
                    OverlayLine(
                        stringResource(R.string.radar_pointing),
                        textCatalog.orientationStatus(orientation.status)
                    )
                    OverlayLine(
                        stringResource(R.string.radar_transmitter),
                        RadarUiText.transmitterLine(
                            state.focusedTransmitter,
                            textCatalog
                        )
                    )
                    OverlayLine(
                        stringResource(R.string.radar_frequencies),
                        RadarUiText.frequencyLine(
                            state.focusedTransmitter,
                            textCatalog
                        )
                    )
                    OverlayLine(
                        stringResource(R.string.radar_doppler),
                        RadarUiText.dopplerLine(state.doppler, textCatalog)
                    )

                    if (state.focusedTransmitters.size > 1) {
                        IconButton(
                            onClick = {
                                onSelectRadarTransmitter(
                                    RadarTransmitterSelector.nextUuid(
                                        options = state.focusedTransmitters,
                                        selectedUuid = state.focusedTransmitter?.uuid
                                    )
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = switchTransmitterDescription
                            }
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            maxLines = RadarOverlayPolicy.valueMaxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
