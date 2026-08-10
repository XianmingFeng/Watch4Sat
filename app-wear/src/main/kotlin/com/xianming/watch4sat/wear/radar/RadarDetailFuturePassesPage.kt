package com.xianming.watch4sat.wear.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.wear.RoundListTransformationProvider
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.roundListSurfaceTransformation
import com.xianming.watch4sat.wear.roundListTransformedHeight
import com.xianming.watch4sat.wear.state.RoundListSurface
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.state.PassCardAnimationPolicy
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailFuturePassesPage(
    state: WatchUiState,
    expandedPassKey: String?,
    onPassTap: (SatellitePass, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textCatalog = radarTextCatalog()
    val model = RadarFuturePassesModel.from(
        focusedPass = state.focusedPass,
        passCards = state.passCards,
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = state.focusedPass?.satelliteName
                                ?: stringResource(R.string.radar_no_pass),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.radar_current_pass_window),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText,
                            maxLines = 1
                        )
                    }
                }
                if (model.groups.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.radar_no_future_passes),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                } else {
                    model.groups.forEach { group ->
                        item(key = "group-${group.label}") {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        group.rows.forEach { row ->
                            val pass = state.passCards.firstOrNull { (candidate, _) ->
                                candidate.catalogNumber == row.catalogNumber &&
                                    candidate.aosMillis == row.aosMillis
                            }?.first
                            if (pass != null) {
                                item(key = row.key) {
                                    FuturePassRowCard(
                                        row = row,
                                        expanded = expandedPassKey == row.key,
                                        itemScope = this,
                                        onClick = { onPassTap(pass, row.key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuturePassRowCard(
    row: RadarFuturePassRow,
    expanded: Boolean,
    itemScope: TransformingLazyColumnItemScope,
    onClick: () -> Unit
) {
    val colors = LocalWatchThemeColors.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .roundListTransformedHeight(itemScope, RoundListSurface.STANDARD_CARD)
            .animateContentSize(
                animationSpec = tween(durationMillis = PassCardAnimationPolicy.contentSizeMillis)
            ),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.88f),
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        transformation = roundListSurfaceTransformation(itemScope, RoundListSurface.STANDARD_CARD)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = row.startTime,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = row.maxElevation,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    maxLines = 1
                )
            }
            Text(
                    text = stringResource(
                        R.string.radar_summary,
                        row.azimuthRange,
                        row.duration
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailExpandMillis)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailFadeMillis)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailExpandMillis)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = PassCardAnimationPolicy.detailFadeMillis)
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    DetailLine(
                        stringResource(R.string.radar_time),
                        row.timeRange
                    )
                    DetailLine(
                        stringResource(R.string.radar_tca),
                        row.tcaTime
                    )
                    DetailLine(
                        stringResource(R.string.radar_azimuth_short),
                        stringResource(
                            R.string.radar_range,
                            row.aosAzimuth,
                            row.losAzimuth
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalWatchThemeColors.current.mutedText,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
