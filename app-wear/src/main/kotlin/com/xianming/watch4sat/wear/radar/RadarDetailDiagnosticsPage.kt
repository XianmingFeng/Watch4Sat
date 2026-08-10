package com.xianming.watch4sat.wear.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.wear.RoundListTransformationProvider
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.StatusTextBlock
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailDiagnosticsPage(
    state: WatchUiState,
    orientation: RadarOrientationSnapshot,
    modifier: Modifier = Modifier
) {
    val textCatalog = radarTextCatalog()
    val model = RadarDiagnosticsModel.from(
        state = state,
        orientation = orientation,
        nowMillis = state.radarNowMillis,
        textCatalog = textCatalog
    )
    val listState = rememberTransformingLazyColumnState()
    val colors = LocalWatchThemeColors.current

    ScreenScaffold(
        modifier = modifier,
        scrollState = listState,
        scrollIndicator = { WearScrollIndicator(state = listState) },
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 34.dp)
    ) { contentPadding ->
        RoundListTransformationProvider {
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = state.focusedPass?.satelliteName
                                ?: stringResource(R.string.radar_no_pass),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.radar_data_source_quality),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            maxLines = 1
                        )
                    }
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        model.qualityChips.forEach { chip ->
                            QualityChip(chip)
                        }
                    }
                }
                model.fields.forEach { field ->
                    item(key = field.label) {
                        StatusTextBlock(field.label, field.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityChip(text: String) {
    val colors = LocalWatchThemeColors.current
    Box(
        modifier = Modifier
            .background(
                color = colors.primary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
