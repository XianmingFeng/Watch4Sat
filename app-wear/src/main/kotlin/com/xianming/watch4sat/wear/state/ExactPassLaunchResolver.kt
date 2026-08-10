package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatellitePass

data class ExactPassIdentity(
    val catalogNumber: Int,
    val aosMillis: Long,
    val losMillis: Long
) {
    companion object {
        fun from(
            catalogNumber: Int?,
            aosMillis: Long?,
            losMillis: Long?
        ): ExactPassIdentity? {
            if (catalogNumber == null || catalogNumber <= 0) return null
            if (aosMillis == null || aosMillis < 0L) return null
            if (losMillis == null || losMillis <= aosMillis) return null
            return ExactPassIdentity(
                catalogNumber = catalogNumber,
                aosMillis = aosMillis,
                losMillis = losMillis
            )
        }
    }
}

enum class ExactPassLaunchSource {
    Tile,
    PassNotification,
    OngoingActivity
}

data class ExactPassLaunchRequest(
    val source: ExactPassLaunchSource,
    val identity: ExactPassIdentity?
)

sealed interface ExactPassLaunchResolution {
    data object ConsumeDuringSetup : ExactPassLaunchResolution
    data object Waiting : ExactPassLaunchResolution
    data class Matched(val pass: SatellitePass) : ExactPassLaunchResolution
    data object Unavailable : ExactPassLaunchResolution
}

object ExactPassLaunchResolver {
    fun resolve(
        request: ExactPassLaunchRequest,
        setupIncomplete: Boolean,
        passPlanningStatus: PassPlanningStatus,
        passes: List<SatellitePass>,
        nowMillis: Long
    ): ExactPassLaunchResolution {
        if (setupIncomplete) return ExactPassLaunchResolution.ConsumeDuringSetup

        val identity = request.identity ?: return ExactPassLaunchResolution.Unavailable
        if (nowMillis >= identity.losMillis) return ExactPassLaunchResolution.Unavailable

        val match = passes.firstOrNull { pass ->
            pass.catalogNumber == identity.catalogNumber &&
                pass.aosMillis == identity.aosMillis &&
                pass.losMillis == identity.losMillis
        }
        if (match != null) return ExactPassLaunchResolution.Matched(match)

        return if (passPlanningStatus.isExactPassLaunchTerminal()) {
            ExactPassLaunchResolution.Unavailable
        } else {
            ExactPassLaunchResolution.Waiting
        }
    }

    private fun PassPlanningStatus.isExactPassLaunchTerminal(): Boolean {
        return when (this) {
            PassPlanningStatus.NeedsQth,
            PassPlanningStatus.NoSatellites,
            PassPlanningStatus.Ready,
            PassPlanningStatus.Failed -> true
            PassPlanningStatus.Idle,
            PassPlanningStatus.FromSnapshot,
            PassPlanningStatus.Calculating -> false
        }
    }
}
