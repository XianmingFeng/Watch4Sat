package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SelectedSatellite

object PassWindowFilter {

    fun filter(
        passes: List<SatellitePass>,
        selectedSatellites: List<SelectedSatellite>,
        nowMillis: Long,
        window: PassWindow = PassWindow()
    ): List<SatellitePass> {
        val selectedCatalogNumbers = selectedSatellites.map { it.catalogNumber }.toSet()
        val windowEndMillis = nowMillis + window.hoursAhead * 60L * 60L * 1000L
        return passes
            .asSequence()
            .filter { it.catalogNumber in selectedCatalogNumbers }
            .filter { it.losMillis > nowMillis }
            .filter { it.aosMillis <= windowEndMillis }
            .filter { it.maxElevationDegrees >= window.minimumElevationDegrees }
            .sortedBy { it.aosMillis }
            .toList()
    }
}
