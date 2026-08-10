package com.xianming.watch4sat.wear.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailOverlay(
    visible: Boolean,
    state: WatchUiState,
    orientation: RadarOrientationSnapshot,
    plotRotationDegrees: State<Float>,
    displayPosition: RadarDisplayPoint?,
    onClose: () -> Unit,
    onSelectRadarTransmitter: (String?) -> Unit,
    onSelectRadarPass: (SatellitePass) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 220),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 180),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        val colors = LocalWatchThemeColors.current
        val detailsDescription = stringResource(R.string.radar_details)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .semantics { contentDescription = detailsDescription }
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
                    .background(Color.Black.copy(alpha = 0.78f))
            )
            RadarDetailPager(
                state = state,
                orientation = orientation,
                onClose = onClose,
                onSelectRadarTransmitter = onSelectRadarTransmitter,
                onSelectRadarPass = onSelectRadarPass,
                modifier = Modifier.matchParentSize()
            )
            RadarOverlayHandle(
                onClose = onClose,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
