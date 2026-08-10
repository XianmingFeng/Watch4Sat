package com.xianming.watch4sat.domain.radar

import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.RadarTrackLabel
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.model.SatellitePass

object RadarTrackSampler {

    fun sample(
        pass: SatellitePass,
        intervalSeconds: Long = 15,
        positionAt: (timeMillis: Long) -> OrbitalPosition
    ): List<RadarTrackPoint> {
        if (pass.losMillis < pass.aosMillis || intervalSeconds <= 0) return emptyList()

        val intervalMillis = intervalSeconds * 1000L
        val times = mutableListOf<Long>()
        var time = pass.aosMillis
        while (time < pass.losMillis) {
            times += time
            time += intervalMillis
        }
        if (times.lastOrNull() != pass.losMillis) {
            times += pass.losMillis
        }

        return times.mapIndexed { index, timeMillis ->
            val position = positionAt(timeMillis)
            RadarTrackPoint(
                timeMillis = timeMillis,
                azimuthDegrees = position.azimuthDegrees,
                elevationDegrees = position.elevationDegrees,
                aboveHorizon = position.aboveHorizon,
                label = when (index) {
                    0 -> RadarTrackLabel.AOS
                    times.lastIndex -> RadarTrackLabel.LOS
                    else -> RadarTrackLabel.NONE
                }
            )
        }
    }
}
