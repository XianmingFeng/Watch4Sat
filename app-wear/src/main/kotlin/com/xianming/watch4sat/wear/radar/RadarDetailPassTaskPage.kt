package com.xianming.watch4sat.wear.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.xianming.watch4sat.wear.WatchUiState
import com.xianming.watch4sat.wear.WearScrollIndicator
import com.xianming.watch4sat.time.AndroidClockTimeFormatter
import com.xianming.watch4sat.wear.theme.LocalWatchThemeColors

@Composable
fun RadarDetailPassTaskPage(
    state: WatchUiState,
    orientation: RadarOrientationSnapshot,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val clockTimeFormatter = remember(context, configuration) {
        AndroidClockTimeFormatter.create(context)
    }
    val textCatalog = radarTextCatalog()
    val model = RadarPassTaskModel.from(
        pass = state.focusedPass,
        position = state.focusedPosition,
        orientation = orientation,
        nowMillis = state.radarNowMillis,
        textCatalog = textCatalog,
        clockTimeFormatter = clockTimeFormatter
    )
    val colors = LocalWatchThemeColors.current
    val listState = rememberTransformingLazyColumnState()

    Box(modifier = modifier.fillMaxSize()) {
        PassProgressArc(
            progress = model.progress,
            modifier = Modifier.matchParentSize()
        )
        ScreenScaffold(
            scrollState = listState,
            scrollIndicator = { WearScrollIndicator(state = listState) },
            contentPadding = PaddingValues(start = 34.dp, end = 24.dp, top = 42.dp, bottom = 42.dp)
        ) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item {
                    Text(
                        text = model.satelliteName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                item {
                    Text(
                        text = model.remaining,
                        style = MaterialTheme.typography.numeralMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
                item {
                    Text(
                        text = model.pointingStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskMetric(model.maxAtTca)
                        Spacer(modifier = Modifier.width(8.dp))
                        TaskMetric(model.los)
                    }
                }
                item {
                    Text(
                        text = model.secondaryAngles,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskMetric(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PassProgressArc(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalWatchThemeColors.current
    Canvas(modifier = modifier) {
        val strokeWidth = 7.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth * 1.4f
        val topLeft = Offset(
            x = size.width / 2f - radius,
            y = size.height / 2f - radius
        )
        val diameter = radius * 2f
        val startAngle = 128f
        val sweep = 104f
        drawArc(
            color = colors.primary.copy(alpha = 0.22f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = colors.primary,
            startAngle = startAngle,
            sweepAngle = sweep * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
