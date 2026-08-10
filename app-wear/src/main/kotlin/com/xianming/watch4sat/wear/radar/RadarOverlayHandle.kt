package com.xianming.watch4sat.wear.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xianming.watch4sat.wear.WatchUiMetrics
import com.xianming.watch4sat.R
import kotlin.math.abs

private val HandleHeight = WatchUiMetrics.MinimumSemanticTouchTarget
private val HandleVisualHeight = 34.dp
private val CloseDragThreshold = 34.dp
private const val TopEdgeArcVisibleInsetPx = 4f
private const val TopEdgeArcStartAngle = 254f
private const val TopEdgeArcSweepAngle = 32f

@Composable
fun RadarOverlayHandle(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeThresholdPx = with(LocalDensity.current) { CloseDragThreshold.toPx() }
    val closeDescription = stringResource(R.string.radar_pull_down_close_details)
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HandleHeight)
            .semantics { contentDescription = closeDescription }
            .pointerInput(onClose) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragEnd = {
                        if (accumulatedDrag > closeThresholdPx) {
                            onClose()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        if (abs(dragAmount) > 0f) {
                            change.consume()
                        }
                        accumulatedDrag += dragAmount
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(HandleVisualHeight)
        ) {
            val strokeWidth = 3.dp.toPx()
            val arcCenterlineInset = TopEdgeArcVisibleInsetPx + strokeWidth / 2f
            val radius = size.width / 2f - arcCenterlineInset
            val center = Offset(size.width / 2f, size.width / 2f)
            val diameter = radius * 2f
            val topLeft = Offset(center.x - radius, center.y - radius)
            drawArc(
                color = Color.White.copy(alpha = 0.58f),
                startAngle = TopEdgeArcStartAngle,
                sweepAngle = TopEdgeArcSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
