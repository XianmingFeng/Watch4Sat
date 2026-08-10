package com.xianming.watch4sat.wear.map

import com.xianming.watch4sat.domain.footprint.SatelliteFootprint
import com.xianming.watch4sat.domain.model.GroundTrackPoint

data class OsmOrbitOverlaySnapshot(
    val trackSegments: List<List<GroundTrackPoint>>,
    val footprint: SatelliteFootprint?,
    val selectedCatalogNumber: Int?,
    val trackMinuteBucketMillis: Long,
    val trackStyle: MapTrackStyle,
    val arrowStyle: MapArrowStyle,
    val footprintStyle: MapFootprintStyle,
    val viewportRevision: Long
)

data class OsmOrbitOverlayUpdateDecision(
    val rebuildTrackAndArrows: Boolean,
    val rebuildFootprint: Boolean
)

object OsmOrbitOverlayUpdatePolicy {
    fun decide(
        previous: OsmOrbitOverlaySnapshot?,
        current: OsmOrbitOverlaySnapshot
    ): OsmOrbitOverlayUpdateDecision {
        return OsmOrbitOverlayUpdateDecision(
            rebuildTrackAndArrows = previous == null ||
                previous.trackSegments !== current.trackSegments ||
                previous.selectedCatalogNumber != current.selectedCatalogNumber ||
                previous.trackMinuteBucketMillis != current.trackMinuteBucketMillis ||
                previous.trackStyle != current.trackStyle ||
                previous.arrowStyle != current.arrowStyle ||
                previous.viewportRevision != current.viewportRevision,
            rebuildFootprint = previous == null ||
                previous.footprint !== current.footprint ||
                previous.selectedCatalogNumber != current.selectedCatalogNumber ||
                previous.footprintStyle != current.footprintStyle
        )
    }
}
