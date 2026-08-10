package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute
import com.xianming.watch4sat.wear.radar.RadarUpdateMode

data class RouteUpdateLifecycleDecision(
    val runMinuteUpdates: Boolean,
    val runOrbitMapUpdates: Boolean,
    val runOrbitMapDetailUpdates: Boolean,
    val radarUpdateMode: RadarUpdateMode?
) {
    companion object {
        val None = RouteUpdateLifecycleDecision(
            runMinuteUpdates = false,
            runOrbitMapUpdates = false,
            runOrbitMapDetailUpdates = false,
            radarUpdateMode = null
        )
    }
}

object RouteUpdateLifecyclePolicy {
    fun decide(
        route: String?,
        lifecycleStarted: Boolean,
        radarUpdateMode: RadarUpdateMode,
        isAmbient: Boolean = radarUpdateMode == RadarUpdateMode.AmbientOneHz
    ): RouteUpdateLifecycleDecision {
        if (!lifecycleStarted) return RouteUpdateLifecycleDecision.None
        return when (route) {
            WatchRoute.Dashboard.route,
            WatchRoute.Passes.route -> RouteUpdateLifecycleDecision.None.copy(runMinuteUpdates = true)

            WatchRoute.OrbitMap.route -> RouteUpdateLifecycleDecision.None.copy(
                runOrbitMapUpdates = !isAmbient
            )
            WatchRoute.OrbitMapDetail.route -> RouteUpdateLifecycleDecision.None.copy(
                runOrbitMapDetailUpdates = !isAmbient
            )
            WatchRoute.Radar.route -> RouteUpdateLifecycleDecision.None.copy(radarUpdateMode = radarUpdateMode)
            else -> RouteUpdateLifecycleDecision.None
        }
    }
}
