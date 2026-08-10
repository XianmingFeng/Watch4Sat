package com.xianming.watch4sat.wear.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailRadioPage(
    state: WatchUiState,
    onSelectRadarTransmitter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textCatalog = radarTextCatalog()
    val model = RadarRadioDisplayModel.from(
        pass = state.focusedPass,
        position = state.focusedPosition,
        transmitter = state.focusedTransmitter,
        transmitters = state.focusedTransmitters,
        doppler = state.doppler,
        nowMillis = state.radarNowMillis,
        textCatalog = textCatalog
    )
    val colors = LocalWatchThemeColors.current
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        modifier = modifier,
        scrollState = listState,
        scrollIndicator = { WearScrollIndicator(state = listState) },
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 42.dp, bottom = 42.dp)
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = model.satelliteName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                Text(
                    text = model.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (model.transmitterOptions.size > 1) {
                item {
                    TransmitterChooser(
                        options = model.transmitterOptions,
                        selectedUuid = model.selectedTransmitterUuid,
                        onSelectRadarTransmitter = onSelectRadarTransmitter
                    )
                }
            }
            item {
                FrequencyCard(
                    label = stringResource(R.string.radar_downlink),
                    frequency = model.downlinkCorrected,
                    offset = model.downlinkOffset,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                FrequencyCard(
                    label = stringResource(R.string.radar_uplink),
                    frequency = model.uplinkCorrected,
                    offset = model.uplinkOffset,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TransmitterChooser(
    options: List<RadarRadioTransmitterOption>,
    selectedUuid: String?,
    onSelectRadarTransmitter: (String?) -> Unit
) {
    val colors = LocalWatchThemeColors.current
    val rowState = rememberLazyListState()

    LaunchedEffect(selectedUuid, options) {
        val selectedIndex = options.indexOfFirst { it.uuid == selectedUuid }
        if (selectedIndex >= 0) {
            rowState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = rowState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = options,
            key = { option -> option.uuid }
        ) { option ->
            val selected = option.uuid == selectedUuid
            CompactButton(
                onClick = { onSelectRadarTransmitter(option.uuid) },
                colors = if (selected) {
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary.copy(alpha = 0.16f),
                        contentColor = colors.primary
                    )
                },
                border = if (selected) null else ButtonDefaults.outlinedButtonBorder(enabled = true),
                modifier = Modifier.widthIn(min = WatchUiMetrics.MinimumSemanticTouchTarget)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FrequencyCard(
    label: String,
    frequency: String,
    offset: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalWatchThemeColors.current
    Box(
        modifier = modifier
            .background(
                color = colors.surface.copy(alpha = 0.82f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    maxLines = 1
                )
                Text(
                    text = frequency,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = offset,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 42.dp, max = 58.dp)
            )
        }
    }
}
