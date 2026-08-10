package com.xianming.watch4sat.wear.state

data class OrbitMapRequestKey(
    val catalogNumber: Int,
    val generation: Long
)

object OrbitMapRequestPolicy {

    fun nextGeneration(currentGeneration: Long): Long = currentGeneration + 1L

    fun requestOrNull(catalogNumber: Int?, generation: Long): OrbitMapRequestKey? {
        return catalogNumber?.let { OrbitMapRequestKey(it, generation) }
    }

    fun canCommit(
        request: OrbitMapRequestKey,
        selectedCatalogNumber: Int?,
        currentGeneration: Long
    ): Boolean {
        return request.catalogNumber == selectedCatalogNumber &&
            request.generation == currentGeneration
    }
}
