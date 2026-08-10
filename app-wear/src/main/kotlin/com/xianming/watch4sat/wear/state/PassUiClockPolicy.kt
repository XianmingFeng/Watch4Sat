package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatellitePass

object PassUiClockPolicy {
    private const val MillisPerMinute = 60_000L

    fun nextWakeDelayMillis(
        nowMillis: Long,
        passes: List<SatellitePass>
    ): Long {
        val nextMinuteMillis =
            nowMillis + (MillisPerMinute - Math.floorMod(nowMillis, MillisPerMinute))
        val nextPassBoundaryMillis = passes
            .asSequence()
            .flatMap { pass -> sequenceOf(pass.aosMillis, pass.losMillis) }
            .filter { boundaryMillis -> boundaryMillis > nowMillis }
            .minOrNull()
        val targetMillis = minOf(
            nextMinuteMillis,
            nextPassBoundaryMillis ?: Long.MAX_VALUE
        )
        return (targetMillis - nowMillis).coerceAtLeast(1L)
    }
}
