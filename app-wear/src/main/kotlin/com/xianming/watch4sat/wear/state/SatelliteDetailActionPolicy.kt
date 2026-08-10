package com.xianming.watch4sat.wear.state

enum class SatelliteDetailActionPlacement {
    EdgeButton
}

enum class SatelliteDetailAction {
    Select,
    Remove
}

object SatelliteDetailActionPolicy {
    val placement: SatelliteDetailActionPlacement = SatelliteDetailActionPlacement.EdgeButton
    const val exposesListAction: Boolean = false
    const val usesExistingToggleEvent: Boolean = true
    const val exposesOrbitIntroText: Boolean = false

    fun actionFor(selected: Boolean): SatelliteDetailAction =
        if (selected) SatelliteDetailAction.Remove else SatelliteDetailAction.Select
}
