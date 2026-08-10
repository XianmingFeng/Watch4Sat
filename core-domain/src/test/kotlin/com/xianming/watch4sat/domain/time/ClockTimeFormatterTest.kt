package com.xianming.watch4sat.domain.time

import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTimeFormatterTest {
    private val instant = Instant.parse("2026-06-05T13:05:09Z").toEpochMilli()

    @Test
    fun `24 hour format uses leading hours for minutes and seconds`() {
        val formatter = ClockTimeFormatter(
            is24HourFormat = true,
            locale = Locale.US
        )

        assertEquals("13:05", formatter.formatMinutes(instant, ZoneOffset.UTC))
        assertEquals("13:05:09", formatter.formatSeconds(instant, ZoneOffset.UTC))
        assertEquals("13:05", formatter.formatCompactMinutes(instant, ZoneOffset.UTC))
    }

    @Test
    fun `12 hour format uses unpadded hours and day period for minutes and seconds`() {
        val formatter = ClockTimeFormatter(
            is24HourFormat = false,
            locale = Locale.US
        )

        assertEquals("1:05 PM", formatter.formatMinutes(instant, ZoneOffset.UTC))
        assertEquals("1:05:09 PM", formatter.formatSeconds(instant, ZoneOffset.UTC))
        assertEquals("1:05", formatter.formatCompactMinutes(instant, ZoneOffset.UTC))
    }

    @Test
    fun `default locale remains English when process locale is unsupported`() {
        synchronized(Locale::class.java) {
            val originalLocale = Locale.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("zh-Hans-CN"))
                val formatter = ClockTimeFormatter(is24HourFormat = false)

                assertEquals("1:05 PM", formatter.formatMinutes(instant, ZoneOffset.UTC))
                assertEquals("1:05:09 PM", formatter.formatSeconds(instant, ZoneOffset.UTC))
                assertEquals("1:05", formatter.formatCompactMinutes(instant, ZoneOffset.UTC))
            } finally {
                Locale.setDefault(originalLocale)
            }
        }
    }
}
