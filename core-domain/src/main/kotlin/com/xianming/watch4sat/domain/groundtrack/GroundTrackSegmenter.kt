package com.xianming.watch4sat.domain.groundtrack

import com.xianming.watch4sat.domain.geometry.AntimeridianClipper
import com.xianming.watch4sat.domain.model.GroundTrackPoint

object GroundTrackSegmenter {

    fun splitAtAntimeridian(points: List<GroundTrackPoint>): List<List<GroundTrackPoint>> {
        return AntimeridianClipper.splitPolyline(points)
    }
}
