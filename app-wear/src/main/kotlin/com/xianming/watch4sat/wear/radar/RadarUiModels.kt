package com.xianming.watch4sat.wear.radar

import com.xianming.watch4sat.domain.model.DopplerReading
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class RadarSemanticsSummary(
    val text: String
)

data class RadarPassBadge(
    val satelliteName: String,
    val countdown: String
)

object RadarUiText {
    fun semanticsSummary(
        pass: SatellitePass?,
        position: OrbitalPosition?,
        orientationStatus: RadarOrientationStatus,
        textCatalog: RadarTextCatalog
    ): RadarSemanticsSummary {
        if (pass == null) {
            return RadarSemanticsSummary(textCatalog.text(RadarTextKey.NoFocusedPass))
        }
        val unknown = textCatalog.text(RadarTextKey.Unknown)
        val azimuth = position?.azimuthDegrees?.roundToInt()?.toString() ?: unknown
        val elevation = position?.elevationDegrees?.roundToInt()?.toString() ?: unknown
        return RadarSemanticsSummary(
            textCatalog.text(
                RadarTextKey.SemanticsSummary,
                pass.satelliteName,
                azimuth,
                elevation,
                textCatalog.orientationStatus(orientationStatus)
            )
        )
    }

    fun passTiming(
        pass: SatellitePass,
        textCatalog: RadarTextCatalog,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour()
    ): String {
        val aos = clockTimeFormatter.formatMinutes(pass.aosMillis, zoneId)
        val tca = clockTimeFormatter.formatMinutes(pass.tcaMillis, zoneId)
        val los = clockTimeFormatter.formatMinutes(pass.losMillis, zoneId)
        return textCatalog.text(
            RadarTextKey.PassTiming,
            aos,
            tca,
            los,
            pass.maxElevationDegrees.roundToInt()
        )
    }

    fun countdownLine(
        pass: SatellitePass,
        nowMillis: Long,
        textCatalog: RadarTextCatalog
    ): String {
        return when {
            nowMillis < pass.aosMillis -> textCatalog.text(
                RadarTextKey.AosIn,
                formatDuration(pass.aosMillis - nowMillis, textCatalog)
            )
            nowMillis < pass.losMillis -> textCatalog.text(
                RadarTextKey.LosIn,
                formatDuration(pass.losMillis - nowMillis, textCatalog)
            )
            else -> textCatalog.text(RadarTextKey.Ended)
        }
    }

    fun satelliteEdgeLabel(pass: SatellitePass): String = pass.satelliteName

    fun topCountdownStatus(
        pass: SatellitePass,
        nowMillis: Long,
        textCatalog: RadarTextCatalog,
        showSeconds: Boolean = true
    ): String {
        val targetMillis = when {
            nowMillis < pass.aosMillis -> pass.aosMillis
            nowMillis < pass.losMillis -> pass.losMillis
            else -> return textCatalog.text(RadarTextKey.Ended)
        }
        return formatTopCountdownDuration(
            millis = targetMillis - nowMillis,
            showSeconds = showSeconds,
            textCatalog = textCatalog
        )
    }

    fun losMinutesStatus(
        pass: SatellitePass,
        nowMillis: Long,
        textCatalog: RadarTextCatalog
    ): String {
        if (nowMillis >= pass.losMillis) return textCatalog.text(RadarTextKey.Ended)
        val millis = (pass.losMillis - nowMillis).coerceAtLeast(0L)
        val minutes = ceil(millis / 60_000.0).toLong().coerceAtLeast(0L)
        return textCatalog.text(RadarTextKey.MinutesShort, minutes)
    }

    fun timeTextPreview(
        pass: SatellitePass,
        nowMillis: Long,
        textCatalog: RadarTextCatalog,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour()
    ): String {
        val time = clockTimeFormatter.formatMinutes(nowMillis, zoneId)
        return textCatalog.text(
            RadarTextKey.TimeStatus,
            topCountdownStatus(pass, nowMillis, textCatalog),
            time
        )
    }

    fun ambientMinuteBucket(pass: SatellitePass, nowMillis: Long): Long {
        if (nowMillis >= pass.losMillis) return 0L
        return ceil((pass.losMillis - nowMillis).coerceAtLeast(0L) / 60_000.0)
            .toLong()
            .coerceAtLeast(0L)
    }

    fun passChromeKey(pass: SatellitePass): String {
        return "${pass.catalogNumber}:${pass.aosMillis}:${pass.losMillis}"
    }

    fun angleLine(position: OrbitalPosition?, textCatalog: RadarTextCatalog): String {
        val azimuth = position?.azimuthDegrees?.roundToInt()?.toString() ?: "--"
        val elevation = position?.elevationDegrees?.roundToInt()?.toString() ?: "--"
        return textCatalog.text(RadarTextKey.AngleLine, azimuth, elevation)
    }

    fun transmitterLine(
        transmitter: TransmitterRecord?,
        textCatalog: RadarTextCatalog
    ): String {
        if (transmitter == null) return textCatalog.text(RadarTextKey.NoTransmitter)
        val mode = transmitter.downlinkMode ?: transmitter.uplinkMode ?: transmitter.description
        return mode.ifBlank { textCatalog.text(RadarTextKey.TransmitterFallback) }
    }

    fun frequencyLine(
        transmitter: TransmitterRecord?,
        textCatalog: RadarTextCatalog
    ): String {
        if (transmitter == null) return textCatalog.text(RadarTextKey.NoFrequency)
        val downlink = (transmitter.downlinkLowHz ?: transmitter.downlinkHighHz)?.let {
            textCatalog.text(RadarTextKey.DownlinkPrefix, formatMhz(it, textCatalog))
        }
        val uplink = (transmitter.uplinkLowHz ?: transmitter.uplinkHighHz)?.let {
            textCatalog.text(RadarTextKey.UplinkPrefix, formatMhz(it, textCatalog))
        }
        return listOfNotNull(downlink, uplink)
            .ifEmpty { listOf(textCatalog.text(RadarTextKey.NoFrequency)) }
            .joinToString(" · ")
    }

    fun dopplerLine(doppler: DopplerReading?, textCatalog: RadarTextCatalog): String {
        if (doppler == null) return textCatalog.text(RadarTextKey.DopplerWaitingFrequency)
        val downlink = doppler.correctedDownlinkHz?.let {
            textCatalog.text(
                RadarTextKey.DownlinkPrefix,
                textCatalog.text(
                    RadarTextKey.CorrectedFrequency,
                    formatMhz(it, textCatalog),
                    formatKhz(doppler.downlinkOffsetKhz, textCatalog)
                )
            )
        }
        val uplink = doppler.correctedUplinkHz?.let {
            textCatalog.text(
                RadarTextKey.UplinkPrefix,
                textCatalog.text(
                    RadarTextKey.CorrectedFrequency,
                    formatMhz(it, textCatalog),
                    formatKhz(doppler.uplinkOffsetKhz, textCatalog)
                )
            )
        }
        return listOfNotNull(downlink, uplink)
            .ifEmpty { listOf(textCatalog.text(RadarTextKey.NoBaseFrequency)) }
            .joinToString(" · ")
    }

    private fun formatMhz(hz: Long, textCatalog: RadarTextCatalog): String {
        return textCatalog.text(RadarTextKey.FrequencyMhz, hz / 1_000_000.0)
    }

    private fun formatKhz(khz: Double?, textCatalog: RadarTextCatalog): String {
        return khz?.let { textCatalog.text(RadarTextKey.FrequencyKhz, it) } ?: "--"
    }

    private fun formatTopCountdownDuration(
        millis: Long,
        showSeconds: Boolean,
        textCatalog: RadarTextCatalog
    ): String {
        if (millis < 0L) return textCatalog.text(RadarTextKey.Ended)
        if (showSeconds && millis < 60_000L) {
            val seconds = ceil(millis / 1_000.0).toLong().coerceAtLeast(1L)
            return textCatalog.text(RadarTextKey.SecondsShort, seconds)
        }
        val minutes = ceil(millis / 60_000.0).toLong().coerceAtLeast(1L)
        return textCatalog.text(RadarTextKey.MinutesShort, minutes)
    }

    private fun formatDuration(millis: Long, textCatalog: RadarTextCatalog): String {
        val seconds = (millis / 1000L).coerceAtLeast(0L)
        val minutes = seconds / 60L
        val remainingSeconds = seconds % 60L
        return if (minutes > 0) {
            textCatalog.text(RadarTextKey.MinutesSecondsShort, minutes, remainingSeconds)
        } else {
            textCatalog.text(RadarTextKey.SecondsCompact, remainingSeconds)
        }
    }
}
