package com.xianming.watch4sat.domain.model

data class SatelliteRecord(
    val catalogNumber: Int,
    val displayName: String,
    val orbitalData: OrbitalData,
    val objectId: String? = null,
    val selected: Boolean = false
)

data class SelectedSatellite(
    val catalogNumber: Int,
    val displayName: String
)
