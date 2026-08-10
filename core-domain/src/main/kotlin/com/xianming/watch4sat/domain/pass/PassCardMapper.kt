package com.xianming.watch4sat.domain.pass

import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import java.time.ZoneId
import kotlin.math.roundToInt

object PassCardMapper {

    fun map(
        pass: SatellitePass,
        transmitters: List<TransmitterRecord>,
        nowMillis: Long,
        textFormatter: PassCardTextFormatter,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour()
    ): PassCardUi {
        val matchingTransmitter = transmitters.firstOrNull { it.catalogNumber == pass.catalogNumber }
        return PassCardUi(
            catalogNumber = pass.catalogNumber,
            satelliteName = pass.satelliteName,
            aosCountdown = textFormatter.formatAosCountdown(pass.aosMillis - nowMillis),
            aosTime = clockTimeFormatter.formatMinutes(pass.aosMillis, zoneId),
            losTime = clockTimeFormatter.formatMinutes(pass.losMillis, zoneId),
            tcaTime = clockTimeFormatter.formatMinutes(pass.tcaMillis, zoneId),
            maxElevation = textFormatter.formatDegrees(pass.maxElevationDegrees.roundedDegrees()),
            aosAzimuth = textFormatter.formatDegrees(pass.aosAzimuthDegrees.roundedDegrees()),
            losAzimuth = textFormatter.formatDegrees(pass.losAzimuthDegrees.roundedDegrees()),
            duration = textFormatter.formatDuration(pass.durationMillis),
            modeFrequencyHint = matchingTransmitter?.let { transmitter ->
                val mode = transmitter.downlinkMode ?: transmitter.uplinkMode
                mode?.let {
                    textFormatter.formatModeFrequencyHint(
                        mode = it,
                        downlinkLowHz = transmitter.downlinkLowHz,
                        uplinkLowHz = transmitter.uplinkLowHz
                    )
                }
            },
            isActive = pass.isActiveAt(nowMillis),
            isUpcoming = pass.aosMillis > nowMillis
        )
    }

    private fun Double.roundedDegrees(): Int = roundToInt()
}
