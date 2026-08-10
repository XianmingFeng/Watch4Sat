package com.xianming.watch4sat.wear.orbit

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.TransmitterRecord
import com.xianming.watch4sat.domain.qth.MaidenheadLocator
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

data class OrbitMapDetail(
    val catalogNumber: Int,
    val satelliteName: String,
    val catalogLine: String,
    val currentAltitudeKm: Int?,
    val footprintDiameterKm: Int?,
    val currentDistanceKm: Int?,
    val currentPosition: OrbitMapDetailPosition?,
    val updatedAt: OrbitMapDetailUpdate?,
    val orbitRows: List<OrbitMapDetailRow>,
    val transmitters: List<OrbitMapDetailTransmitter>
)

data class OrbitMapDetailPosition(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val grid: String?
)

data class OrbitMapDetailUpdate(
    val date: String,
    val time: String
)

data class OrbitMapDetailRow(
    val key: String,
    val metric: OrbitMapOrbitMetric,
    val value: OrbitMapOrbitValue?
)

enum class OrbitMapOrbitMetric {
    MeanAltitude,
    Period,
    MeanMotion,
    Inclination,
    Eccentricity,
    RightAscensionAscendingNode,
    ArgumentOfPerigee
}

sealed interface OrbitMapOrbitValue {
    data class MeanAltitude(
        val meanKilometers: Int,
        val perigeeKilometers: Int,
        val apogeeKilometers: Int
    ) : OrbitMapOrbitValue

    data class Scalar(val value: Double) : OrbitMapOrbitValue
}

data class OrbitMapDetailTransmitter(
    val key: String,
    val title: String,
    val status: String?,
    val isAlive: Boolean,
    val downlink: OrbitMapRadioLink?,
    val uplink: OrbitMapRadioLink?,
    val isInverted: Boolean
)

data class OrbitMapRadioLink(
    val lowHz: Long?,
    val highHz: Long?,
    val mode: String?
)

object OrbitMapDetailMapper {
    private const val EarthMuKm3PerSecond2 = 398600.4418
    private const val EarthRadiusKm = 6378.137
    private val updateDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun map(
        satellite: SatelliteRecord,
        transmitters: List<TransmitterRecord>,
        currentPosition: GroundTrackPoint?,
        footprintRadiusKm: Double,
        slantRangeKm: Double? = null,
        lastUpdatedMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clockTimeFormatter: ClockTimeFormatter = ClockTimeFormatter.twentyFourHour(Locale.US)
    ): OrbitMapDetail {
        val matchingTransmitters = transmitters.filter {
            it.catalogNumber == satellite.catalogNumber
        }
        val orbitalData = satellite.orbitalData
        val meanAltitude = orbitalData.meanMotion
            .takeIf { it > 0.0 && it.isFinite() }
            ?.let { meanMotion ->
                meanAltitudeValue(
                    meanMotionRevPerDay = meanMotion,
                    eccentricity = orbitalData.eccentricity
                )
            }
        val orbitRows = listOf(
            OrbitMapDetailRow(
                key = "orbit-mean-altitude",
                metric = OrbitMapOrbitMetric.MeanAltitude,
                value = meanAltitude
            ),
            OrbitMapDetailRow(
                key = "orbit-period",
                metric = OrbitMapOrbitMetric.Period,
                value = orbitalData.meanMotion
                    .takeIf { it > 0.0 && it.isFinite() }
                    ?.let { OrbitMapOrbitValue.Scalar(1_440.0 / it) }
            ),
            OrbitMapDetailRow(
                key = "orbit-mean-motion",
                metric = OrbitMapOrbitMetric.MeanMotion,
                value = orbitalData.meanMotion
                    .takeIf { it > 0.0 && it.isFinite() }
                    ?.let(OrbitMapOrbitValue::Scalar)
            ),
            OrbitMapDetailRow(
                key = "orbit-inclination",
                metric = OrbitMapOrbitMetric.Inclination,
                value = orbitalData.inclinationDegrees
                    .takeIf(Double::isFinite)
                    ?.let(OrbitMapOrbitValue::Scalar)
            ),
            OrbitMapDetailRow(
                key = "orbit-eccentricity",
                metric = OrbitMapOrbitMetric.Eccentricity,
                value = orbitalData.eccentricity
                    .takeIf(Double::isFinite)
                    ?.let(OrbitMapOrbitValue::Scalar)
            ),
            OrbitMapDetailRow(
                key = "orbit-raan",
                metric = OrbitMapOrbitMetric.RightAscensionAscendingNode,
                value = orbitalData.rightAscensionAscendingNodeDegrees
                    .takeIf(Double::isFinite)
                    ?.let(OrbitMapOrbitValue::Scalar)
            ),
            OrbitMapDetailRow(
                key = "orbit-argument-of-perigee",
                metric = OrbitMapOrbitMetric.ArgumentOfPerigee,
                value = orbitalData.argumentOfPerigeeDegrees
                    .takeIf(Double::isFinite)
                    ?.let(OrbitMapOrbitValue::Scalar)
            )
        )
        val transmitterRows = matchingTransmitters.map { source ->
            OrbitMapDetailTransmitter(
                key = source.uuid,
                title = source.description.ifBlank { source.uuid },
                status = source.status.takeIf(String::isNotBlank),
                isAlive = source.isAlive,
                downlink = source.radioLink(
                    lowHz = source.downlinkLowHz,
                    highHz = source.downlinkHighHz,
                    mode = source.downlinkMode
                ),
                uplink = source.radioLink(
                    lowHz = source.uplinkLowHz,
                    highHz = source.uplinkHighHz,
                    mode = source.uplinkMode
                ),
                isInverted = source.isInverted
            )
        }

        return OrbitMapDetail(
            catalogNumber = satellite.catalogNumber,
            satelliteName = satellite.displayName,
            catalogLine = "#${satellite.catalogNumber}",
            currentAltitudeKm = currentPosition?.altitudeKm
                ?.takeIf(Double::isFinite)
                ?.roundToInt(),
            footprintDiameterKm = footprintRadiusKm
                .takeIf { it.isFinite() && it > 0.0 }
                ?.times(2.0)
                ?.roundToInt(),
            currentDistanceKm = slantRangeKm
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?.roundToInt(),
            currentPosition = currentPosition.toDetailPosition(),
            updatedAt = lastUpdatedMillis
                .takeIf { it > 0L }
                ?.let {
                    OrbitMapDetailUpdate(
                        date = Instant.ofEpochMilli(it)
                            .atZone(zoneId)
                            .format(updateDateFormatter),
                        time = clockTimeFormatter.formatSeconds(it, zoneId)
                    )
                },
            orbitRows = orbitRows,
            transmitters = transmitterRows
        )
    }

    private fun meanAltitudeValue(
        meanMotionRevPerDay: Double,
        eccentricity: Double
    ): OrbitMapOrbitValue.MeanAltitude {
        val periodSeconds = 86_400.0 / meanMotionRevPerDay
        val semiMajorAxisKm =
            (
                EarthMuKm3PerSecond2 *
                    (periodSeconds / (2.0 * PI)).pow(2.0)
                ).pow(1.0 / 3.0)
        val clampedEccentricity = eccentricity.coerceIn(0.0, 0.999999)
        return OrbitMapOrbitValue.MeanAltitude(
            meanKilometers = (semiMajorAxisKm - EarthRadiusKm).roundToInt(),
            perigeeKilometers =
                (semiMajorAxisKm * (1.0 - clampedEccentricity) - EarthRadiusKm)
                    .roundToInt(),
            apogeeKilometers =
                (semiMajorAxisKm * (1.0 + clampedEccentricity) - EarthRadiusKm)
                    .roundToInt()
        )
    }

    private fun GroundTrackPoint?.toDetailPosition(): OrbitMapDetailPosition? {
        if (this == null ||
            !latitudeDegrees.isFinite() ||
            !longitudeDegrees.isFinite()
        ) {
            return null
        }
        return OrbitMapDetailPosition(
            latitudeDegrees = latitudeDegrees,
            longitudeDegrees = longitudeDegrees,
            grid = MaidenheadLocator.fromCoordinates(
                latitude = latitudeDegrees,
                longitude = longitudeDegrees
            )?.uppercase(Locale.US)
        )
    }

    private fun TransmitterRecord.radioLink(
        lowHz: Long?,
        highHz: Long?,
        mode: String?
    ): OrbitMapRadioLink? {
        if (lowHz == null && highHz == null) return null
        return OrbitMapRadioLink(
            lowHz = lowHz,
            highHz = highHz,
            mode = mode?.trim()?.takeIf(String::isNotEmpty)
        )
    }
}

object OrbitMapDetailLayoutPolicy {
    const val SummaryColumnCount: Int = 3
    const val MinimumRowHeightDp: Int = 32
}
