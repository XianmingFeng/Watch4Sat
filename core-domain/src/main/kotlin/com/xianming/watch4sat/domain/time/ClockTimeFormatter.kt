package com.xianming.watch4sat.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockTimeFormatter(
    is24HourFormat: Boolean,
    locale: Locale = Locale.US
) {
    private val minuteFormatter = DateTimeFormatter.ofPattern(
        if (is24HourFormat) "HH:mm" else "h:mm a",
        locale
    )
    private val secondFormatter = DateTimeFormatter.ofPattern(
        if (is24HourFormat) "HH:mm:ss" else "h:mm:ss a",
        locale
    )
    private val compactMinuteFormatter = DateTimeFormatter.ofPattern(
        if (is24HourFormat) "HH:mm" else "h:mm",
        locale
    )

    fun formatMinutes(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return minuteFormatter.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis))
    }

    fun formatSeconds(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return secondFormatter.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis))
    }

    fun formatCompactMinutes(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return compactMinuteFormatter.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis))
    }

    companion object {
        fun twentyFourHour(locale: Locale = Locale.US): ClockTimeFormatter {
            return ClockTimeFormatter(is24HourFormat = true, locale = locale)
        }
    }
}
