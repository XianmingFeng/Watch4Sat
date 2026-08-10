package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.data.settings.Watch4SatSettings

object MinimumElevationPolicy {
    val allowedThresholdDegrees: List<Int> = listOf(10, 20, 30)

    fun effectiveMinimumElevationDegrees(settings: Watch4SatSettings): Double {
        if (!settings.minimumElevationFilterEnabled) return 0.0
        return coerceThresholdDegrees(settings.minimumElevationDegrees).toDouble()
    }

    fun coerceThresholdDegrees(value: Int): Int {
        return allowedThresholdDegrees.minBy { kotlin.math.abs(it - value) }
    }
}
