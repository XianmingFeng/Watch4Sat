package com.xianming.watch4sat.wear

object OrbitMapRoutes {
    const val CatalogNumberArg = "catalogNumber"
    const val DetailPattern = "orbit_map/detail/{$CatalogNumberArg}"

    fun detail(catalogNumber: Int): String {
        require(catalogNumber > 0) { "Catalog number must be positive" }
        return "orbit_map/detail/$catalogNumber"
    }
}
