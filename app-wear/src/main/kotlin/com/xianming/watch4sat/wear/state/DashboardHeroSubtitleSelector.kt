package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchUiState

object DashboardHeroSubtitleSelector {

    fun modelFor(state: WatchUiState): DashboardHeroSubtitle {
        if (!state.hasStationLocation) return DashboardHeroSubtitle.NeedsStation
        val active = state.passCards.firstOrNull { (_, card) -> card.isActive }
        val next = state.passCards.firstOrNull { (_, card) -> !card.isActive }
        val display = active ?: next
        return display?.let { (pass, card) ->
            if (card.isActive) {
                DashboardHeroSubtitle.Active(
                    remainingMinutes = pass.losMillis.remainingMinutesFrom(state.nowMillis)
                )
            } else {
                DashboardHeroSubtitle.Upcoming(
                    aosCountdown = card.aosCountdown,
                    maxElevation = card.maxElevation
                )
            }
        } ?: DashboardHeroSubtitle.Fallback(state.passPlanningMessage)
    }

    private fun Long.remainingMinutesFrom(nowMillis: Long): Long {
        val remainingMillis = this - nowMillis
        if (remainingMillis <= 0L) return 0L
        return ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
    }
}

sealed interface DashboardHeroSubtitle {
    data object NeedsStation : DashboardHeroSubtitle

    data class Active(
        val remainingMinutes: Long
    ) : DashboardHeroSubtitle

    data class Upcoming(
        val aosCountdown: String,
        val maxElevation: String
    ) : DashboardHeroSubtitle

    data class Fallback(
        val text: String
    ) : DashboardHeroSubtitle
}
