package com.xianming.watch4sat.wear.radar

enum class RadarVisualCueState {
    None,
    TrackAligned,
    SatelliteAligned
}

data class RadarVisualCue(
    val state: RadarVisualCueState,
    val animated: Boolean
)

object RadarVisualCuePolicy {
    const val satelliteAlignedDistanceFraction: Float = 0.12f
    const val trackAlignedDistanceFraction: Float = 0.075f

    fun cueFor(
        reticle: RadarDisplayPoint?,
        satellite: RadarDisplayPoint?,
        track: List<RadarDisplayPoint>,
        updateMode: RadarUpdateMode
    ): RadarVisualCue {
        val state = when {
            reticle == null -> RadarVisualCueState.None
            satellite != null && distance(reticle, satellite) <= satelliteAlignedDistanceFraction ->
                RadarVisualCueState.SatelliteAligned
            track.size >= 2 && minDistanceToTrack(reticle, track) <= trackAlignedDistanceFraction ->
                RadarVisualCueState.TrackAligned
            else -> RadarVisualCueState.None
        }
        return RadarVisualCue(
            state = state,
            animated = state != RadarVisualCueState.None && updateMode != RadarUpdateMode.AmbientOneHz
        )
    }

    fun minDistanceToTrack(point: RadarDisplayPoint, track: List<RadarDisplayPoint>): Float {
        if (track.size < 2) return Float.POSITIVE_INFINITY
        var minimum = Float.POSITIVE_INFINITY
        var index = 1
        while (index < track.size) {
            minimum = minOf(minimum, distanceToSegment(point, track[index - 1], track[index]))
            index += 1
        }
        return minimum
    }

    private fun distance(a: RadarDisplayPoint, b: RadarDisplayPoint): Float {
        return kotlin.math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
    }

    private fun distanceToSegment(
        point: RadarDisplayPoint,
        start: RadarDisplayPoint,
        end: RadarDisplayPoint
    ): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return distance(point, start)
        val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
            .coerceIn(0f, 1f)
        val projectionX = start.x + t * dx
        val projectionY = start.y + t * dy
        return kotlin.math.hypot(
            (point.x - projectionX).toDouble(),
            (point.y - projectionY).toDouble()
        ).toFloat()
    }
}
