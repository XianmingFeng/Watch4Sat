package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.WatchUiState

object DashboardHeroActionSelector {

    fun select(state: WatchUiState): DashboardHeroAction {
        if (!state.hasStationLocation) return DashboardHeroAction.OpenQth
        val active = state.passCards.firstOrNull { (_, card) -> card.isActive }?.first
        val upcoming = state.passCards.firstOrNull { (_, card) -> !card.isActive }?.first
        val passAction = (active ?: upcoming)?.let { DashboardHeroAction.OpenRadar(it) }
        if (passAction != null) return passAction
        return if (state.satellites.isEmpty() && !state.refreshInFlight) {
            DashboardHeroAction.RefreshData
        } else {
            DashboardHeroAction.None
        }
    }
}

sealed interface DashboardHeroAction {
    data class OpenRadar(val pass: SatellitePass) : DashboardHeroAction
    data object OpenQth : DashboardHeroAction
    data object RefreshData : DashboardHeroAction
    data object None : DashboardHeroAction
}
