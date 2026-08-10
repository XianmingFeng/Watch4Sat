package com.xianming.watch4sat.wear

import android.content.Context
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.pass.PassCardTextFormatter

class AndroidPassCardTextFormatter(
    context: Context
) : PassCardTextFormatter {
    private val context = context.applicationContext

    override fun formatAosCountdown(remainingMillis: Long): String {
        if (remainingMillis == 0L) {
            return context.getString(R.string.pass_card_countdown_now)
        }
        if (remainingMillis < 0L) {
            return context.getString(R.string.pass_card_countdown_active)
        }
        val totalMinutes = ((remainingMillis - 1L) / MillisPerMinute + 1L)
            .coerceAtLeast(1L)
        val hours = totalMinutes / MinutesPerHour
        val minutes = totalMinutes % MinutesPerHour
        return if (hours > 0L) {
            context.resources.getQuantityString(
                R.plurals.pass_card_countdown_hours_minutes,
                hours.asQuantity(),
                hours,
                minutes
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.pass_card_countdown_minutes,
                minutes.asQuantity(),
                minutes
            )
        }
    }

    override fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / MillisPerMinute
        val hours = totalMinutes / MinutesPerHour
        val minutes = totalMinutes % MinutesPerHour
        return if (hours > 0L) {
            context.resources.getQuantityString(
                R.plurals.pass_card_duration_hours_minutes,
                hours.asQuantity(),
                hours,
                minutes
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.pass_card_duration_minutes,
                minutes.asQuantity(),
                minutes
            )
        }
    }

    override fun formatDegrees(degrees: Int): String =
        context.getString(R.string.pass_card_degrees, degrees)

    override fun formatModeFrequencyHint(
        mode: String,
        downlinkLowHz: Long?,
        uplinkLowHz: Long?
    ): String {
        val frequencies = buildList {
            downlinkLowHz?.let { frequencyHz ->
                add(
                    context.getString(
                        R.string.pass_card_frequency_downlink,
                        frequencyHz.toMhz()
                    )
                )
            }
            uplinkLowHz?.let { frequencyHz ->
                add(
                    context.getString(
                        R.string.pass_card_frequency_uplink,
                        frequencyHz.toMhz()
                    )
                )
            }
        }
        return if (frequencies.isEmpty()) {
            mode
        } else {
            context.getString(
                R.string.pass_card_mode_with_frequencies,
                mode,
                frequencies.joinToString(
                    separator = context.getString(R.string.pass_card_frequency_separator)
                )
            )
        }
    }

    private fun Long.asQuantity(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    private fun Long.toMhz(): Double = this / HertzPerMegahertz

    private companion object {
        const val MillisPerMinute = 60_000L
        const val MinutesPerHour = 60L
        const val HertzPerMegahertz = 1_000_000.0
    }
}
