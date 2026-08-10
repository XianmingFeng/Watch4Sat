package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatelliteRecord

object StarterSelectionPolicy {
    val preferredCatalogNumbers: List<Int> = listOf(
        61781,
        43678,
        25544,
        24278,
        7530,
        44909,
        27607
    )

    val selectionSize: Int = preferredCatalogNumbers.size

    fun pickStarterSelection(satellites: List<SatelliteRecord>): Set<Int> {
        if (satellites.isEmpty()) return emptySet()
        val cachedIds = satellites.map { it.catalogNumber }.toSet()
        val selected = mutableListOf<Int>()
        preferredCatalogNumbers.forEach { catalogNumber ->
            if (catalogNumber in cachedIds) selected += catalogNumber
        }
        satellites.forEach { satellite ->
            if (selected.size >= selectionSize) return@forEach
            if (satellite.catalogNumber !in selected) selected += satellite.catalogNumber
        }
        return selected.toSet()
    }
}
