package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchUiState

object DashboardHeroProgressSelector {

    fun progressFor(state: WatchUiState): Float? {
        if (!state.hasStationLocation) return null
        val activePass = state.passCards.firstOrNull { (_, card) -> card.isActive }?.first ?: return null
        val durationMillis = activePass.losMillis - activePass.aosMillis
        if (durationMillis <= 0L) return null
        val elapsedMillis = state.nowMillis - activePass.aosMillis
        return (elapsedMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    }
}

object DashboardHeroProgressVisibilitySelector {
    const val heroItemKey: String = "dashboard_hero"
    const val dashboardTitleItemKey: String = "dashboard_title"

    fun shouldShow(
        hasActiveProgress: Boolean,
        firstVisibleItemKey: Any?,
        firstVisibleItemOffsetPx: Int,
        topSlackPx: Int
    ): Boolean {
        return hasActiveProgress &&
            firstVisibleItemKey == dashboardTitleItemKey &&
            firstVisibleItemOffsetPx >= -topSlackPx
    }
}

enum class DashboardHeroProgressPagePlacement {
    PageLeftEdge
}

object DashboardHeroProgressPlacement {
    val pagePlacement: DashboardHeroProgressPagePlacement = DashboardHeroProgressPagePlacement.PageLeftEdge
    const val renderInsideHeroCard: Boolean = false
    const val mirrorsScrollIndicatorSide: Boolean = true
    const val usesFixedSideIndicatorHeight: Boolean = false
    const val usesSweepAngleForArcLength: Boolean = true
    const val hidesUnlessDashboardAtTop: Boolean = true
}
