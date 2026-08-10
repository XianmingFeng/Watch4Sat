package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.PassBoundary
import com.xianming.watch4sat.domain.model.SatellitePass

object PassChunkMerger {

    fun merge(passes: List<SatellitePass>): List<SatellitePass> {
        return passes
            .distinctBy { pass ->
                PassIdentity(
                    catalogNumber = pass.catalogNumber,
                    aosMillis = pass.aosMillis,
                    losMillis = pass.losMillis
                )
            }
            .groupBy(SatellitePass::catalogNumber)
            .values
            .flatMap(::mergeCatalogPasses)
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
    }

    private fun mergeCatalogPasses(passes: List<SatellitePass>): List<SatellitePass> {
        val merged = mutableListOf<SatellitePass>()
        passes
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.losMillis })
            .forEach { current ->
                val previous = merged.lastOrNull()
                if (previous != null && previous.shouldMergeWith(current)) {
                    merged[merged.lastIndex] = previous.mergeWith(current)
                } else {
                    merged += current
                }
            }
        return merged
    }

    private fun SatellitePass.shouldMergeWith(other: SatellitePass): Boolean {
        if (catalogNumber != other.catalogNumber) return false
        return losBoundary == PassBoundary.WINDOW_CLIPPED &&
            other.aosBoundary == PassBoundary.WINDOW_CLIPPED &&
            other.aosMillis - losMillis <= BoundaryJoinToleranceMillis
    }

    private fun SatellitePass.mergeWith(other: SatellitePass): SatellitePass {
        val start = if (aosMillis <= other.aosMillis) this else other
        val end = if (losMillis >= other.losMillis) this else other
        val peak = listOf(this, other).maxWith(
            compareBy<SatellitePass> { it.maxElevationDegrees }
                .thenByDescending { it.tcaMillis }
        )
        return SatellitePass(
            catalogNumber = catalogNumber,
            satelliteName = start.satelliteName,
            aosMillis = start.aosMillis,
            losMillis = end.losMillis,
            tcaMillis = peak.tcaMillis.coerceIn(start.aosMillis, end.losMillis),
            maxElevationDegrees = peak.maxElevationDegrees,
            aosAzimuthDegrees = start.aosAzimuthDegrees,
            losAzimuthDegrees = end.losAzimuthDegrees,
            altitudeKm = peak.altitudeKm,
            orbitalData = peak.orbitalData ?: start.orbitalData ?: end.orbitalData,
            aosBoundary = start.aosBoundary,
            losBoundary = end.losBoundary
        )
    }

    private const val BoundaryJoinToleranceMillis = 1_000L

    private data class PassIdentity(
        val catalogNumber: Int,
        val aosMillis: Long,
        val losMillis: Long
    )
}
