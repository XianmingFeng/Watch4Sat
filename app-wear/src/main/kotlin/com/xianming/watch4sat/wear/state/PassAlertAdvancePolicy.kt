package com.xianming.watch4sat.wear.state

data class PassAlertAdvanceOption(
    val minutes: Int
)

object PassAlertAdvancePolicy {
    const val offMinutes: Int = -1
    val activeMinutes: List<Int> = listOf(0, 5, 10, 15, 30)
    val options: List<PassAlertAdvanceOption> = listOf(
        PassAlertAdvanceOption(offMinutes),
        PassAlertAdvanceOption(0),
        PassAlertAdvanceOption(5),
        PassAlertAdvanceOption(10),
        PassAlertAdvanceOption(15),
        PassAlertAdvanceOption(30)
    )

    fun coerceMinutes(value: Int): Int {
        if (value == offMinutes) return offMinutes
        return activeMinutes.minBy { kotlin.math.abs(it - value) }
    }

    fun triggerAtMillis(
        aosMillis: Long,
        nowMillis: Long,
        minutes: Int
    ): Long? {
        val coerced = coerceMinutes(minutes)
        if (coerced == offMinutes) return null
        return (aosMillis - coerced * 60_000L).coerceAtLeast(nowMillis)
    }
}
