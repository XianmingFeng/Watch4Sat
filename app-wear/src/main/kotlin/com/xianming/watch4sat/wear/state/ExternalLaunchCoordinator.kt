package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.tile.TileLaunchDestination
import com.xianming.watch4sat.wear.TileLaunchIntentPolicy
import com.xianming.watch4sat.wear.TileLaunchRequest
import com.xianming.watch4sat.wear.WatchRoute

sealed interface ExternalLaunchTarget {
    data object None : ExternalLaunchTarget
    data class Route(val route: WatchRoute) : ExternalLaunchTarget
    data class ExactPass(val request: ExactPassLaunchRequest) : ExternalLaunchTarget
}

sealed interface ExternalLaunchDecision {
    data object Wait : ExternalLaunchDecision
    data object Consume : ExternalLaunchDecision
    data class Navigate(val route: WatchRoute) : ExternalLaunchDecision
    data class OpenPass(val pass: SatellitePass) : ExternalLaunchDecision
    data object PassUnavailable : ExternalLaunchDecision
}

object ExternalLaunchCoordinator {
    fun target(
        tileRequest: TileLaunchRequest?,
        passRequest: PassStartNotificationRequest?
    ): ExternalLaunchTarget {
        if (tileRequest != null) {
            return if (tileRequest.destination == TileLaunchDestination.Radar) {
                ExternalLaunchTarget.ExactPass(tileRequest.exactPassLaunchRequest)
            } else {
                ExternalLaunchTarget.Route(
                    TileLaunchIntentPolicy.routeFor(tileRequest.destination)
                )
            }
        }
        if (passRequest != null) {
            return ExternalLaunchTarget.ExactPass(passRequest.exactPassLaunchRequest)
        }
        return ExternalLaunchTarget.None
    }

    fun decide(
        target: ExternalLaunchTarget,
        settingsLoaded: Boolean,
        setupIncomplete: Boolean,
        passPlanningStatus: PassPlanningStatus,
        passes: List<SatellitePass>,
        nowMillis: Long
    ): ExternalLaunchDecision {
        if (!settingsLoaded) return ExternalLaunchDecision.Wait
        if (setupIncomplete) return ExternalLaunchDecision.Consume

        return when (target) {
            ExternalLaunchTarget.None -> ExternalLaunchDecision.Consume
            is ExternalLaunchTarget.Route -> ExternalLaunchDecision.Navigate(target.route)
            is ExternalLaunchTarget.ExactPass -> {
                when (
                    val resolution = ExactPassLaunchResolver.resolve(
                        request = target.request,
                        setupIncomplete = false,
                        passPlanningStatus = passPlanningStatus,
                        passes = passes,
                        nowMillis = nowMillis
                    )
                ) {
                    ExactPassLaunchResolution.ConsumeDuringSetup ->
                        ExternalLaunchDecision.Consume
                    ExactPassLaunchResolution.Waiting ->
                        ExternalLaunchDecision.Wait
                    is ExactPassLaunchResolution.Matched ->
                        ExternalLaunchDecision.OpenPass(resolution.pass)
                    ExactPassLaunchResolution.Unavailable ->
                        ExternalLaunchDecision.PassUnavailable
                }
            }
        }
    }

    fun decideAtCurrentTime(
        target: ExternalLaunchTarget,
        settingsLoaded: Boolean,
        setupIncomplete: Boolean,
        passPlanningStatus: PassPlanningStatus,
        passes: List<SatellitePass>,
        clockMillis: () -> Long = System::currentTimeMillis
    ): ExternalLaunchDecision {
        return decide(
            target = target,
            settingsLoaded = settingsLoaded,
            setupIncomplete = setupIncomplete,
            passPlanningStatus = passPlanningStatus,
            passes = passes,
            nowMillis = clockMillis()
        )
    }
}
