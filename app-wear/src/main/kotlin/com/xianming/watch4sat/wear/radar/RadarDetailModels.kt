package com.xianming.watch4sat.wear.radar

import com.xianming.watch4sat.domain.freshness.TleEpochConverter
import com.xianming.watch4sat.domain.freshness.TleEpochSample
import com.xianming.watch4sat.domain.freshness.TleFreshnessPolicy
import com.xianming.watch4sat.domain.model.DopplerReading
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import com.xianming.watch4sat.wear.WatchUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class RadarRadioDisplayModel(
    val satelliteName: String,
    val metadata: String,
    val summary: String,
    val transmitterOptions: List<RadarRadioTransmitterOption>,
    val selectedTransmitterUuid: String?,
    val downlinkCorrected: String,
    val downlinkOffset: String,
    val uplinkCorrected: String,
    val uplinkOffset: String
) {
    fun visibleText(): String {
        return listOf(
            satelliteName,
            summary,
            downlinkCorrected,
            downlinkOffset,
            uplinkCorrected,
            uplinkOffset
        ).joinToString(" ")
    }

    companion object {
        fun from(
            pass: SatellitePass?,
            position: OrbitalPosition?,
            transmitter: TransmitterRecord?,
            transmitters: List<TransmitterRecord>,
            doppler: DopplerReading?,
            nowMillis: Long,
            textCatalog: RadarTextCatalog
        ): RadarRadioDisplayModel {
            val satelliteName = pass?.satelliteName ?: textCatalog.text(RadarTextKey.NoPass)
            val metadata = listOfNotNull(
                pass?.let {
                    RadarUiText.topCountdownStatus(
                        pass = it,
                        nowMillis = nowMillis,
                        textCatalog = textCatalog
                    )
                },
                RadarUiText.angleLine(position, textCatalog)
            ).joinToString(" · ")
            return RadarRadioDisplayModel(
                satelliteName = satelliteName,
                metadata = metadata,
                summary = transmitter.summary(textCatalog),
                transmitterOptions = visibleTransmitterOptions(transmitters, textCatalog),
                selectedTransmitterUuid = transmitter?.uuid,
                downlinkCorrected = correctedFrequency(
                    correctedHz = doppler?.correctedDownlinkHz,
                    fallbackHz = transmitter?.downlinkLowHz ?: transmitter?.downlinkHighHz,
                    missing = textCatalog.text(RadarTextKey.NoDownlink),
                    textCatalog = textCatalog
                ),
                downlinkOffset = offsetText(
                    offsetKhz = doppler?.downlinkOffsetKhz,
                    hasFrequency = transmitter?.downlinkLowHz != null ||
                        transmitter?.downlinkHighHz != null,
                    textCatalog = textCatalog
                ),
                uplinkCorrected = correctedFrequency(
                    correctedHz = doppler?.correctedUplinkHz,
                    fallbackHz = transmitter?.uplinkLowHz ?: transmitter?.uplinkHighHz,
                    missing = textCatalog.text(RadarTextKey.NoUplink),
                    textCatalog = textCatalog
                ),
                uplinkOffset = offsetText(
                    offsetKhz = doppler?.uplinkOffsetKhz,
                    hasFrequency = transmitter?.uplinkLowHz != null ||
                        transmitter?.uplinkHighHz != null,
                    textCatalog = textCatalog
                )
            )
        }
    }
}

data class RadarRadioTransmitterOption(
    val uuid: String,
    val label: String
)

data class RadarPassTaskModel(
    val satelliteName: String,
    val remaining: String,
    val progress: Float,
    val pointingStatus: String,
    val maxAtTca: String,
    val los: String,
    val secondaryAngles: String
) {
    fun visibleText(): String {
        return listOf(
            satelliteName,
            remaining,
            pointingStatus,
            maxAtTca,
            los,
            secondaryAngles
        ).joinToString(" ")
    }

    companion object {
        fun from(
            pass: SatellitePass?,
            position: OrbitalPosition?,
            orientation: RadarOrientationSnapshot,
            nowMillis: Long,
            textCatalog: RadarTextCatalog,
            zoneId: ZoneId = ZoneId.systemDefault(),
            clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour()
        ): RadarPassTaskModel {
            if (pass == null) {
                return RadarPassTaskModel(
                    satelliteName = textCatalog.text(RadarTextKey.NoPass),
                    remaining = "--",
                    progress = 0f,
                    pointingStatus = textCatalog.orientationStatus(orientation.status),
                    maxAtTca = textCatalog.text(RadarTextKey.MaxUnknown),
                    los = textCatalog.text(RadarTextKey.LosUnknown),
                    secondaryAngles = RadarUiText.angleLine(position, textCatalog)
                )
            }
            val duration = (pass.losMillis - pass.aosMillis).coerceAtLeast(1L)
            val elapsed = (nowMillis - pass.aosMillis).coerceIn(0L, duration)
            return RadarPassTaskModel(
                satelliteName = pass.satelliteName,
                remaining = RadarUiText.topCountdownStatus(
                    pass = pass,
                    nowMillis = nowMillis,
                    textCatalog = textCatalog
                ),
                progress = elapsed.toFloat() / duration.toFloat(),
                pointingStatus = when (orientation.status) {
                    RadarOrientationStatus.PointingAssistActive ->
                        textCatalog.text(RadarTextKey.PointingGood)
                    else -> textCatalog.orientationStatus(orientation.status)
                },
                maxAtTca = textCatalog.text(
                    RadarTextKey.MaxAtTca,
                    pass.maxElevationDegrees.roundToInt(),
                    clockTimeFormatter.formatMinutes(pass.tcaMillis, zoneId)
                ),
                los = textCatalog.text(
                    RadarTextKey.LosAt,
                    clockTimeFormatter.formatMinutes(pass.losMillis, zoneId)
                ),
                secondaryAngles = RadarUiText.angleLine(position, textCatalog)
            )
        }
    }
}

data class RadarFuturePassesModel(
    val groups: List<RadarFuturePassGroup>
) {
    companion object {
        fun from(
            focusedPass: SatellitePass?,
            passCards: List<Pair<SatellitePass, PassCardUi>>,
            textCatalog: RadarTextCatalog,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): RadarFuturePassesModel {
            if (focusedPass == null) return RadarFuturePassesModel(emptyList())
            val rows = passCards
                .filter { (pass, _) ->
                    pass.catalogNumber == focusedPass.catalogNumber &&
                        pass.aosMillis > focusedPass.aosMillis
                }
                .map { (pass, card) -> RadarFuturePassRow.from(pass, card, textCatalog) }
            val today = Instant.ofEpochMilli(focusedPass.aosMillis).atZone(zoneId).toLocalDate()
            val groups = rows
                .groupBy { row -> Instant.ofEpochMilli(row.aosMillis).atZone(zoneId).toLocalDate() }
                .toSortedMap()
                .map { (date, groupRows) ->
                    RadarFuturePassGroup(
                        label = date.groupLabel(today, textCatalog),
                        rows = groupRows
                    )
                }
            return RadarFuturePassesModel(groups)
        }
    }
}

private fun visibleTransmitterOptions(
    transmitters: List<TransmitterRecord>,
    textCatalog: RadarTextCatalog
): List<RadarRadioTransmitterOption> {
    return transmitters.map { option ->
        RadarRadioTransmitterOption(
            uuid = option.uuid,
            label = option.optionLabel(textCatalog)
        )
    }
}

data class RadarFuturePassGroup(
    val label: String,
    val rows: List<RadarFuturePassRow>
)

data class RadarFuturePassRow(
    val key: String,
    val catalogNumber: Int,
    val aosMillis: Long,
    val satelliteName: String,
    val startTime: String,
    val timeRange: String,
    val azimuthRange: String,
    val duration: String,
    val maxElevation: String,
    val tcaTime: String,
    val aosAzimuth: String,
    val losAzimuth: String
) {
    companion object {
        fun from(
            pass: SatellitePass,
            card: PassCardUi,
            textCatalog: RadarTextCatalog
        ): RadarFuturePassRow {
            return RadarFuturePassRow(
                key = "${pass.catalogNumber}-${pass.aosMillis}",
                catalogNumber = pass.catalogNumber,
                aosMillis = pass.aosMillis,
                satelliteName = pass.satelliteName,
                startTime = card.aosTime,
                timeRange = textCatalog.text(
                    RadarTextKey.Range,
                    card.aosTime,
                    card.losTime
                ),
                azimuthRange = textCatalog.text(
                    RadarTextKey.Range,
                    card.aosAzimuth,
                    card.losAzimuth
                ),
                duration = card.duration,
                maxElevation = card.maxElevation,
                tcaTime = card.tcaTime,
                aosAzimuth = card.aosAzimuth,
                losAzimuth = card.losAzimuth
            )
        }
    }
}

data class RadarDiagnosticsModel(
    val qualityChips: List<String>,
    val fields: List<RadarDiagnosticsField>
) {
    companion object {
        fun from(
            state: WatchUiState,
            orientation: RadarOrientationSnapshot,
            nowMillis: Long,
            textCatalog: RadarTextCatalog
        ): RadarDiagnosticsModel {
            val transmitter = state.focusedTransmitter
            val pass = state.focusedPass
            val fields = buildList {
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.RawDownlink),
                        rawFrequency(
                            transmitter?.downlinkLowHz ?: transmitter?.downlinkHighHz,
                            textCatalog
                        )
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.RawUplink),
                        rawFrequency(
                            transmitter?.uplinkLowHz ?: transmitter?.uplinkHighHz,
                            textCatalog
                        )
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.RadioSource),
                        transmitter.radioSource(textCatalog)
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.TleAge),
                        pass?.orbitalData?.epoch?.let {
                            tleAgeLabel(it, nowMillis, textCatalog)
                        } ?: textCatalog.text(RadarTextKey.UnknownTitle)
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.Sensor),
                        textCatalog.sensorKind(orientation.sensorKind)
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.QthSource),
                        state.qthSourceLabel(textCatalog)
                    )
                )
                add(
                    RadarDiagnosticsField(
                        textCatalog.text(RadarTextKey.Calibration),
                        textCatalog.sensorAccuracy(orientation.accuracy)
                    )
                )
            }
            return RadarDiagnosticsModel(
                qualityChips = listOf(
                    tleQualityChip(state, pass, nowMillis, textCatalog),
                    textCatalog.sensorQuality(orientation.accuracy)
                ),
                fields = fields
            )
        }
    }
}

data class RadarDiagnosticsField(
    val label: String,
    val value: String
)

private fun LocalDate.groupLabel(
    today: LocalDate,
    textCatalog: RadarTextCatalog
): String {
    return when (this) {
        today -> textCatalog.text(RadarTextKey.Today)
        today.plusDays(1) -> textCatalog.text(RadarTextKey.Tomorrow)
        else -> format(textCatalog.dateFormatter())
    }
}

private fun TransmitterRecord?.summary(textCatalog: RadarTextCatalog): String {
    if (this == null) return textCatalog.text(RadarTextKey.NoTransmitter)
    val mode = downlinkMode ?: uplinkMode
        ?: description.ifBlank { textCatalog.text(RadarTextKey.TransmitterFallback) }
    val link = when {
        hasDownlink() && hasUplink() -> textCatalog.text(RadarTextKey.LinkBoth)
        hasDownlink() -> textCatalog.text(RadarTextKey.Downlink)
        hasUplink() -> textCatalog.text(RadarTextKey.Uplink)
        else -> textCatalog.text(RadarTextKey.NoFrequency)
    }
    return textCatalog.text(RadarTextKey.Summary, mode, link)
}

private fun TransmitterRecord.optionLabel(textCatalog: RadarTextCatalog): String {
    val mode = downlinkMode ?: uplinkMode
        ?: description.ifBlank { textCatalog.text(RadarTextKey.TransmitterShort) }
    return mode.take(8)
}

private fun TransmitterRecord.hasDownlink(): Boolean {
    return downlinkLowHz != null || downlinkHighHz != null
}

private fun TransmitterRecord.hasUplink(): Boolean {
    return uplinkLowHz != null || uplinkHighHz != null
}

private fun correctedFrequency(
    correctedHz: Long?,
    fallbackHz: Long?,
    missing: String,
    textCatalog: RadarTextCatalog
): String {
    return (correctedHz ?: fallbackHz)?.formatRadioMhz(textCatalog) ?: missing
}

private fun offsetText(
    offsetKhz: Double?,
    hasFrequency: Boolean,
    textCatalog: RadarTextCatalog
): String {
    if (!hasFrequency) return "--"
    return offsetKhz?.formatRadioKhz(textCatalog)
        ?: textCatalog.text(RadarTextKey.DopplerWaiting)
}

private fun rawFrequency(hz: Long?, textCatalog: RadarTextCatalog): String {
    return hz?.formatMhz(textCatalog) ?: textCatalog.text(RadarTextKey.Missing)
}

private fun Long.formatMhz(textCatalog: RadarTextCatalog): String {
    return textCatalog.text(RadarTextKey.FrequencyMhz, this / 1_000_000.0)
}

private fun Long.formatRadioMhz(textCatalog: RadarTextCatalog): String {
    return textCatalog.text(RadarTextKey.RadioFrequencyMhz, this / 1_000_000.0)
}

private fun Double.formatRadioKhz(textCatalog: RadarTextCatalog): String {
    return textCatalog.text(RadarTextKey.RadioFrequencyKhz, this)
}

private fun TransmitterRecord?.radioSource(textCatalog: RadarTextCatalog): String {
    if (this == null) return textCatalog.text(RadarTextKey.UnknownTitle)
    return when {
        description.contains("SatNOGS", ignoreCase = true) ->
            textCatalog.text(RadarTextKey.Satnogs)
        else -> textCatalog.text(RadarTextKey.Catalog)
    }
}

private fun tleAgeLabel(
    epoch: Double,
    nowMillis: Long,
    textCatalog: RadarTextCatalog
): String {
    val epochMillis = TleEpochConverter.toEpochMillisOrNull(epoch)
        ?: return textCatalog.text(RadarTextKey.UnknownTitle)
    return textCatalog.age((nowMillis - epochMillis).coerceAtLeast(0L))
}

private fun tleQualityChip(
    state: WatchUiState,
    pass: SatellitePass?,
    nowMillis: Long,
    textCatalog: RadarTextCatalog
): String {
    val orbitalData = pass?.orbitalData ?: return textCatalog.text(RadarTextKey.TleMissing)
    val assessment = TleFreshnessPolicy.assess(
        nowMillis = nowMillis,
        retrievedAtMillis = state.tleFreshness.retrievedAtMillis,
        samples = listOf(
            TleEpochSample(
                catalogNumber = orbitalData.catalogNumber,
                epoch = orbitalData.epoch
            )
        )
    )
    return textCatalog.tleQuality(assessment)
}

private fun WatchUiState.qthSourceLabel(textCatalog: RadarTextCatalog): String {
    val locator = station.qthLocator ?: "--"
    return textCatalog.text(
        RadarTextKey.QthSourceValue,
        textCatalog.locationSource(station.source),
        locator
    )
}
