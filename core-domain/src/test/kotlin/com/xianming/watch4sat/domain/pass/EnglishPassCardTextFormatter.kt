package com.xianming.watch4sat.domain.pass

import java.util.Locale

object EnglishPassCardTextFormatter : PassCardTextFormatter {
    override fun formatAosCountdown(remainingMillis: Long): String {
        if (remainingMillis == 0L) return "now"
        if (remainingMillis < 0L) return "NOW"
        val totalMinutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) "in ${hours}h ${minutes}m" else "in ${minutes}m"
    }

    override fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
    }

    override fun formatDegrees(degrees: Int): String = "$degrees°"

    override fun formatModeFrequencyHint(
        mode: String,
        downlinkLowHz: Long?,
        uplinkLowHz: Long?
    ): String {
        val frequencies = buildList {
            downlinkLowHz?.let { add("${it.formatMhz()} DL") }
            uplinkLowHz?.let { add("${it.formatMhz()} UL") }
        }
        return if (frequencies.isEmpty()) mode else "$mode ${frequencies.joinToString(" / ")}"
    }

    private fun Long.formatMhz(): String =
        String.format(Locale.US, "%.3f MHz", this / 1_000_000.0)
}
