package com.xianming.watch4sat.domain.pass

interface PassCardTextFormatter {
    fun formatAosCountdown(remainingMillis: Long): String

    fun formatDuration(durationMillis: Long): String

    fun formatDegrees(degrees: Int): String

    fun formatModeFrequencyHint(
        mode: String,
        downlinkLowHz: Long?,
        uplinkLowHz: Long?
    ): String
}
