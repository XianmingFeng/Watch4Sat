package com.xianming.watch4sat.wear.state

object PassPlanningClock {

    fun hourBucketStartMillis(nowMillis: Long): Long {
        return nowMillis - (nowMillis.floorMod(HourMillis))
    }

    private fun Long.floorMod(divisor: Long): Long {
        val remainder = this % divisor
        return if (remainder >= 0L) remainder else remainder + divisor
    }

    private const val HourMillis = 60L * 60L * 1000L
}
