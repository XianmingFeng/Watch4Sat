package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.footprint.SatelliteFootprint
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.SatelliteRecord

data class OrbitMapUiState(
    val candidates: List<SatelliteRecord> = emptyList(),
    val selectedCatalogNumber: Int? = null,
    val selectedSatellite: SatelliteRecord? = null,
    val currentPosition: GroundTrackPoint? = null,
    val trackSegments: List<List<GroundTrackPoint>> = emptyList(),
    val footprint: SatelliteFootprint? = null,
    val message: String = "",
    val lastUpdatedMillis: Long = 0L
) {
    val footprintRadiusKm: Double
        get() = footprint?.radiusKm ?: 0.0
}
