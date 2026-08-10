package com.xianming.watch4sat.wear.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.xianming.watch4sat.wear.TrackDirectionArrow

fun DrawScope.drawMapCrosshair(style: MapCrosshairStyle) {
    val center = this.center
    drawCrosshairLines(
        center = center,
        arm = style.armPx,
        gap = style.gapPx,
        color = style.shadowColor.copy(alpha = style.shadowAlpha),
        strokeWidth = style.shadowStrokePx
    )
    drawCircle(
        color = style.shadowColor.copy(alpha = style.shadowAlpha),
        radius = style.shadowDotRadiusPx,
        center = center
    )
    drawCrosshairLines(
        center = center,
        arm = style.armPx,
        gap = style.gapPx,
        color = style.foregroundColor,
        strokeWidth = style.foregroundStrokePx
    )
    drawCircle(
        color = style.foregroundColor,
        radius = style.foregroundDotRadiusPx,
        center = center
    )
}

fun DrawScope.drawStationMarker(
    center: Offset,
    style: MapMarkerStyle
) {
    drawMapMarker(center = center, style = style)
}

fun DrawScope.drawSatelliteMarker(
    center: Offset,
    style: MapMarkerStyle
) {
    drawMapMarker(center = center, style = style)
}

fun DrawScope.drawTrackDirectionArrow(
    arrow: TrackDirectionArrow,
    style: MapArrowStyle
) {
    drawLine(
        color = style.color,
        start = Offset(arrow.left.x, arrow.left.y),
        end = Offset(arrow.tip.x, arrow.tip.y),
        strokeWidth = style.strokePx,
        cap = StrokeCap.Round
    )
    drawLine(
        color = style.color,
        start = Offset(arrow.right.x, arrow.right.y),
        end = Offset(arrow.tip.x, arrow.tip.y),
        strokeWidth = style.strokePx,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMapMarker(
    center: Offset,
    style: MapMarkerStyle
) {
    drawCircle(
        color = style.outerColor.copy(alpha = style.outerAlpha),
        radius = style.outerRadiusPx,
        center = center
    )
    drawCircle(
        color = style.innerColor,
        radius = style.innerRadiusPx,
        center = center
    )
}

private fun DrawScope.drawCrosshairLines(
    center: Offset,
    arm: Float,
    gap: Float,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float
) {
    drawLine(color, center.copy(x = center.x - arm), center.copy(x = center.x - gap), strokeWidth, StrokeCap.Round)
    drawLine(color, center.copy(x = center.x + gap), center.copy(x = center.x + arm), strokeWidth, StrokeCap.Round)
    drawLine(color, center.copy(y = center.y - arm), center.copy(y = center.y - gap), strokeWidth, StrokeCap.Round)
    drawLine(color, center.copy(y = center.y + gap), center.copy(y = center.y + arm), strokeWidth, StrokeCap.Round)
}
