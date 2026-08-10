package com.xianming.watch4sat.wear

import kotlin.math.atan2
import kotlin.math.hypot

data class TrackDirectionPoint(
    val x: Float,
    val y: Float
)

data class TrackDirectionArrow(
    val tip: TrackDirectionPoint,
    val left: TrackDirectionPoint,
    val right: TrackDirectionPoint,
    val rotationDegrees: Float
)

object TrackDirectionArrowPolicy {
    fun arrowsFor(
        points: List<TrackDirectionPoint>,
        arrowSizePx: Float,
        minSegmentLengthPx: Float,
        maxSegmentLengthPx: Float
    ): List<TrackDirectionArrow> {
        if (points.size < 2) return emptyList()
        val paths = continuousPaths(
            points = points,
            maxSegmentLengthPx = maxSegmentLengthPx
        )
        val candidate = paths
            .maxByOrNull { it.totalLength }
            ?: return emptyList()

        val validSegments = candidate.segments
            .filter { it.length >= minSegmentLengthPx }
        if (validSegments.isNotEmpty()) {
            return listOf(validSegments[validSegments.lastIndex / 2].toArrow(arrowSizePx))
        }

        if (candidate.totalLength < minSegmentLengthPx) return emptyList()
        return listOf(candidate.arrowAtMidpoint(arrowSizePx))
    }

    private fun TrackSegment.toArrow(arrowSizePx: Float): TrackDirectionArrow {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val unitX = dx / length
        val unitY = dy / length
        val tip = TrackDirectionPoint(
            x = end.x - unitX * arrowSizePx,
            y = end.y - unitY * arrowSizePx
        )
        val wingBase = arrowSizePx * 0.72f
        val wingSpread = arrowSizePx * 0.58f
        val baseX = tip.x - unitX * wingBase
        val baseY = tip.y - unitY * wingBase
        val perpX = -unitY
        val perpY = unitX
        return TrackDirectionArrow(
            tip = tip,
            left = TrackDirectionPoint(
                x = baseX + perpX * wingSpread,
                y = baseY + perpY * wingSpread
            ),
            right = TrackDirectionPoint(
                x = baseX - perpX * wingSpread,
                y = baseY - perpY * wingSpread
            ),
            rotationDegrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        )
    }

    private fun TrackPath.arrowAtMidpoint(arrowSizePx: Float): TrackDirectionArrow {
        val targetDistance = totalLength / 2f
        var walked = 0f
        segments.forEach { segment ->
            val nextWalked = walked + segment.length
            if (targetDistance <= nextWalked) {
                val distanceIntoSegment = (targetDistance - walked).coerceIn(0f, segment.length)
                return segment.toArrowAt(distanceIntoSegment, arrowSizePx)
            }
            walked = nextWalked
        }
        return segments.last().toArrowAt(segments.last().length, arrowSizePx)
    }

    private fun TrackSegment.toArrowAt(
        distanceIntoSegment: Float,
        arrowSizePx: Float
    ): TrackDirectionArrow {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val unitX = dx / length
        val unitY = dy / length
        val tipDistance = (distanceIntoSegment - arrowSizePx).coerceIn(0f, length)
        val tip = TrackDirectionPoint(
            x = start.x + unitX * tipDistance,
            y = start.y + unitY * tipDistance
        )
        val wingBase = arrowSizePx * 0.72f
        val wingSpread = arrowSizePx * 0.58f
        val baseX = tip.x - unitX * wingBase
        val baseY = tip.y - unitY * wingBase
        val perpX = -unitY
        val perpY = unitX
        return TrackDirectionArrow(
            tip = tip,
            left = TrackDirectionPoint(
                x = baseX + perpX * wingSpread,
                y = baseY + perpY * wingSpread
            ),
            right = TrackDirectionPoint(
                x = baseX - perpX * wingSpread,
                y = baseY - perpY * wingSpread
            ),
            rotationDegrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        )
    }

    private fun continuousPaths(
        points: List<TrackDirectionPoint>,
        maxSegmentLengthPx: Float
    ): List<TrackPath> {
        val paths = mutableListOf<TrackPath>()
        val current = mutableListOf<TrackSegment>()

        fun flushCurrent() {
            if (current.isNotEmpty()) {
                paths += TrackPath(
                    segments = current.toList(),
                    totalLength = current.sumOf { it.length.toDouble() }.toFloat()
                )
                current.clear()
            }
        }

        points.zipWithNext().forEach { (start, end) ->
            val length = distance(start, end)
            when {
                length <= 0f -> Unit
                length > maxSegmentLengthPx -> flushCurrent()
                else -> current += TrackSegment(start, end, length)
            }
        }
        flushCurrent()
        return paths
    }

    private fun distance(start: TrackDirectionPoint, end: TrackDirectionPoint): Float {
        return hypot(
            x = (end.x - start.x).toDouble(),
            y = (end.y - start.y).toDouble()
        ).toFloat()
    }

    private data class TrackPath(
        val segments: List<TrackSegment>,
        val totalLength: Float
    )

    private data class TrackSegment(
        val start: TrackDirectionPoint,
        val end: TrackDirectionPoint,
        val length: Float
    )
}
