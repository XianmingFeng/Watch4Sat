package com.xianming.watch4sat.wear.state

import androidx.lifecycle.SavedStateHandle

internal class OrbitMapSavedSelectionStore(
    private val savedStateHandle: SavedStateHandle
) {
    var selectedCatalogNumber: Int?
        get() = savedStateHandle[SelectedCatalogNumberKey]
        set(value) {
            if (value == null) {
                savedStateHandle.remove<Int>(SelectedCatalogNumberKey)
            } else {
                savedStateHandle[SelectedCatalogNumberKey] = value
            }
        }

    internal companion object {
        const val SelectedCatalogNumberKey = "orbit_map_selected_catalog_number"
    }
}
