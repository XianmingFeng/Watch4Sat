package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord

object OrbitMapSatelliteSelector {

    fun candidates(satellites: List<SatelliteRecord>): List<SatelliteRecord> {
        val selected = satellites.filter { it.selected }
        if (selected.isNotEmpty()) return selected
        val starterIds = StarterSelectionPolicy.pickStarterSelection(satellites)
        val preferred = starterIds.mapNotNull { starterId ->
            satellites.firstOrNull { it.catalogNumber == starterId }
        }
        return (preferred + satellites)
            .distinctBy { it.catalogNumber }
            .take(StarterSelectionPolicy.selectionSize)
    }

    fun initialCatalogNumber(
        candidates: List<SatelliteRecord>,
        passCards: List<Pair<SatellitePass, PassCardUi>>,
        requestedCatalogNumber: Int?
    ): Int? {
        val heroCatalogNumber = passCards
            .firstOrNull { (_, card) -> card.isActive }
            ?.first
            ?.catalogNumber
            ?: passCards.firstOrNull { (_, card) -> !card.isActive }?.first?.catalogNumber
        return initialCatalogNumber(
            candidates = candidates,
            preferredCatalogNumber = heroCatalogNumber,
            requestedCatalogNumber = requestedCatalogNumber
        )
    }

    fun initialCatalogNumber(
        candidates: List<SatelliteRecord>,
        preferredCatalogNumber: Int?,
        requestedCatalogNumber: Int?
    ): Int? {
        val candidateIds = candidates.map { it.catalogNumber }.toSet()
        requestedCatalogNumber?.takeIf { it in candidateIds }?.let { return it }
        return preferredCatalogNumber?.takeIf { it in candidateIds }
            ?: candidates.firstOrNull()?.catalogNumber
    }

    fun reconcileCatalogNumber(
        candidates: List<SatelliteRecord>,
        preferredCatalogNumber: Int?,
        requestedCatalogNumber: Int?
    ): OrbitMapSelectionReconciliation {
        if (candidates.isEmpty()) {
            return OrbitMapSelectionReconciliation(
                selectedCatalogNumber = requestedCatalogNumber,
                candidatesReady = false
            )
        }
        return OrbitMapSelectionReconciliation(
            selectedCatalogNumber = initialCatalogNumber(
                candidates = candidates,
                preferredCatalogNumber = preferredCatalogNumber,
                requestedCatalogNumber = requestedCatalogNumber
            ),
            candidatesReady = true
        )
    }

}

data class OrbitMapSelectionReconciliation(
    val selectedCatalogNumber: Int?,
    val candidatesReady: Boolean
)

object OrbitMapSelectionReducer {

    fun nextCatalogNumber(candidates: List<SatelliteRecord>, currentCatalogNumber: Int?): Int? {
        return move(candidates, currentCatalogNumber, delta = 1)
    }

    fun previousCatalogNumber(candidates: List<SatelliteRecord>, currentCatalogNumber: Int?): Int? {
        return move(candidates, currentCatalogNumber, delta = -1)
    }

    private fun move(candidates: List<SatelliteRecord>, currentCatalogNumber: Int?, delta: Int): Int? {
        if (candidates.isEmpty()) return null
        val currentIndex = candidates.indexOfFirst { it.catalogNumber == currentCatalogNumber }
            .takeIf { it >= 0 }
            ?: 0
        val nextIndex = Math.floorMod(currentIndex + delta, candidates.size)
        return candidates[nextIndex].catalogNumber
    }
}

object OrbitMapUpdatePolicy {
    const val CurrentPositionIntervalMillis = 10_000L
    const val GroundTrackIntervalMillis = 60_000L

    fun currentPositionBucketMillis(nowMillis: Long): Long {
        return nowMillis / CurrentPositionIntervalMillis * CurrentPositionIntervalMillis
    }

    fun footprintBucketMillis(nowMillis: Long): Long {
        return currentPositionBucketMillis(nowMillis)
    }

    fun groundTrackBucketMillis(nowMillis: Long): Long {
        return nowMillis / GroundTrackIntervalMillis * GroundTrackIntervalMillis
    }
}
