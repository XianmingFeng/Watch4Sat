package com.xianming.watch4sat.domain.footprint

import com.xianming.watch4sat.domain.geometry.AntimeridianClipper
import com.xianming.watch4sat.domain.geometry.AntimeridianPolygonGeometry
import com.xianming.watch4sat.domain.geometry.GeographicPole
import com.xianming.watch4sat.domain.model.GroundTrackPoint

object FootprintRingSegmenter {

    fun splitClosedRingAtAntimeridian(
        points: List<GroundTrackPoint>,
        enclosedPole: GeographicPole? = null
    ): AntimeridianPolygonGeometry {
        return AntimeridianClipper.clipClosedRing(
            points = points,
            enclosedPole = enclosedPole
        )
    }
}
