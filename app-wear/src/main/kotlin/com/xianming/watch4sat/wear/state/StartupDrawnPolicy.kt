package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute

object StartupDrawnPolicy {

    fun isFullyDrawn(
        settingsLoaded: Boolean,
        setupActive: Boolean,
        currentRoute: String?,
        passPlanningStatus: PassPlanningStatus
    ): Boolean {
        if (!settingsLoaded) return false

        return if (setupActive) {
            currentRoute == WatchRoute.FirstRunSetup.route
        } else {
            currentRoute == WatchRoute.Dashboard.route &&
                passPlanningStatus != PassPlanningStatus.Idle
        }
    }
}
