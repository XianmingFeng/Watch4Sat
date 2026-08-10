package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatelliteRecord

object TleFreshnessScopePolicy {
    fun relevantSatellites(satellites: List<SatelliteRecord>): List<SatelliteRecord> {
        val selected = satellites.filter(SatelliteRecord::selected)
        if (selected.isNotEmpty()) return selected
        val starterIds = StarterSelectionPolicy.pickStarterSelection(satellites)
        return satellites.filter { satellite -> satellite.catalogNumber in starterIds }
    }
}
