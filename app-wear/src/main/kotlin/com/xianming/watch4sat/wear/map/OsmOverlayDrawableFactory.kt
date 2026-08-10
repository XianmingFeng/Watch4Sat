package com.xianming.watch4sat.wear.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.toArgb

object OsmOverlayDrawableFactory {
    fun stationMarker(
        context: Context,
        style: MapMarkerStyle
    ): Drawable = marker(context, style)

    fun orbitMarker(
        context: Context,
        style: MapMarkerStyle
    ): Drawable = marker(context, style)

    fun trackArrow(
        context: Context,
        style: MapArrowStyle
    ): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (18f * density).toInt().coerceAtLeast(18)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = style.strokePx
            color = style.color.toArgb()
        }
        val centerX = size / 2f
        val centerY = size / 2f
        val wingBaseX = centerX - 7f * density
        val wingSpreadY = 5f * density
        canvas.drawLine(wingBaseX, centerY - wingSpreadY, centerX, centerY, paint)
        canvas.drawLine(wingBaseX, centerY + wingSpreadY, centerX, centerY, paint)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun marker(
        context: Context,
        style: MapMarkerStyle
    ): Drawable {
        val outerRadius = style.outerRadiusPx
        val innerRadius = style.innerRadiusPx
        val padding = 2f
        val size = ((outerRadius + padding) * 2f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f
        paint.color = style.outerColor.copy(alpha = style.outerAlpha).toArgb()
        canvas.drawCircle(center, center, outerRadius, paint)
        paint.color = style.innerColor.toArgb()
        canvas.drawCircle(center, center, innerRadius, paint)
        return BitmapDrawable(context.resources, bitmap)
    }
}
