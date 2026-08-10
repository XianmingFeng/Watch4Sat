/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Pass search logic adapted for Watch4Sat from Look4Sat SatelliteRepo.kt.
 */
package com.xianming.watch4sat.domain.pass

import com.rtbishop.look4sat.core.domain.predict.GeoPos
import com.rtbishop.look4sat.core.domain.predict.OrbitalData as PredictOrbitalData
import com.rtbishop.look4sat.core.domain.predict.OrbitalObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalPassBoundary
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass as PredictOrbitalPass
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.GroundTrackPoint
import com.xianming.watch4sat.domain.model.OrbitalPosition
import com.xianming.watch4sat.domain.model.PassBoundary
import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.RadarTrackPoint
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationCoordinates
import com.xianming.watch4sat.domain.model.StationLocation
import com.xianming.watch4sat.domain.radar.RadarTrackSampler
import kotlin.math.PI
import kotlin.math.round

object PassPredictionService {

    private val GroundTrackObserver = GeoPos(latitude = 0.0, longitude = 0.0)
    private const val HourMillis = 60L * 60L * 1_000L
    private const val ElevationSampleStepMillis = 30_000L
    private const val BoundaryRefinementToleranceMillis = 250L
    private const val TcaRefinementToleranceMillis = 1_000L
    private const val MaxRefinementIterations = 64
    private const val MaxElevationSamples = 31 * 24 * 60 * 2

    fun predictPasses(
        satellites: List<SatelliteRecord>,
        station: StationCoordinates,
        startMillis: Long,
        window: PassWindow = PassWindow()
    ): List<SatellitePass> {
        return predictPasses(
            satellites = satellites,
            station = StationLocation(latitude = station.latitude, longitude = station.longitude),
            startMillis = startMillis,
            window = window
        )
    }

    fun predictPasses(
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow = PassWindow()
    ): List<SatellitePass> {
        val normalizedStartMillis = startMillis / 60_000L * 60_000L
        val windowEndMillis = normalizedStartMillis + window.hoursAhead * 60L * 60L * 1000L
        val observer = station.toGeoPos()
        return satellites
            .flatMap { record ->
                runCatching {
                    val orbitalObject = record.orbitalData.toPredictData().getObject()
                    orbitalObject.getPasses(observer, normalizedStartMillis, window.hoursAhead)
                        .map { it.toSatellitePass(record) }
                }.getOrDefault(emptyList())
            }
            .filter { it.losMillis > startMillis }
            .filter { it.aosMillis < windowEndMillis }
            .filter { it.maxElevationDegrees >= window.minimumElevationDegrees }
            .sortedBy { it.aosMillis }
    }

    fun positionAt(
        pass: SatellitePass,
        station: StationCoordinates,
        timeMillis: Long
    ): OrbitalPosition {
        return positionAt(
            pass = pass,
            station = StationLocation(latitude = station.latitude, longitude = station.longitude),
            timeMillis = timeMillis
        )
    }

    fun positionAt(
        pass: SatellitePass,
        station: StationLocation,
        timeMillis: Long
    ): OrbitalPosition {
        val orbitalData = requireNotNull(pass.orbitalData) {
            "SatellitePass must come from PassPredictionService or include orbitalData"
        }
        return orbitalData.toPredictData().getObject()
            .getPosition(station.toGeoPos(), timeMillis)
            .toOrbitalPosition()
    }

    /**
     * Propagates one satellite relative to a sea-level observer.
     *
     * [StationCoordinates] intentionally carries no altitude. The resulting
     * slant range therefore always uses an observer altitude of exactly 0 m.
     */
    fun positionAt(
        satellite: SatelliteRecord,
        station: StationCoordinates,
        timeMillis: Long
    ): OrbitalPosition {
        val observer = GeoPos(
            latitude = station.latitude,
            longitude = station.longitude,
            altitude = 0.0
        )
        return satellite.orbitalData.toPredictData()
            .getObject()
            .getPosition(observer, timeMillis)
            .toOrbitalPosition()
    }

    fun trackFor(
        pass: SatellitePass,
        station: StationCoordinates,
        intervalSeconds: Long = 15
    ): List<RadarTrackPoint> {
        return trackFor(
            pass = pass,
            station = StationLocation(latitude = station.latitude, longitude = station.longitude),
            intervalSeconds = intervalSeconds
        )
    }

    fun trackFor(
        pass: SatellitePass,
        station: StationLocation,
        intervalSeconds: Long = 15
    ): List<RadarTrackPoint> {
        val orbitalData = requireNotNull(pass.orbitalData) {
            "SatellitePass must come from PassPredictionService or include orbitalData"
        }
        val orbitalObject = orbitalData.toPredictData().getObject()
        val observer = station.toGeoPos()
        return RadarTrackSampler.sample(pass, intervalSeconds) { timeMillis ->
            orbitalObject.getPosition(observer, timeMillis).toOrbitalPosition()
        }
    }

    fun groundPositionAt(
        satellite: SatelliteRecord,
        timeMillis: Long
    ): OrbitalPosition {
        return satellite.orbitalData.toPredictData()
            .getObject()
            .getPosition(GroundTrackObserver, timeMillis)
            .toOrbitalPosition()
    }

    fun groundTrackFor(
        satellite: SatelliteRecord,
        centerTimeMillis: Long,
        intervalSeconds: Long = 60,
        pointsBefore: Int = 30,
        pointsAfter: Int = 30
    ): List<GroundTrackPoint> {
        val intervalMillis = intervalSeconds.coerceAtLeast(1L) * 1_000L
        val startMillis = centerTimeMillis - pointsBefore.coerceAtLeast(0) * intervalMillis
        val endMillis = centerTimeMillis + pointsAfter.coerceAtLeast(0) * intervalMillis
        return generateSequence(startMillis) { previous ->
            (previous + intervalMillis).takeIf { it <= endMillis }
        }.map { timeMillis ->
            groundPositionAt(satellite, timeMillis).toGroundTrackPoint()
        }.toList()
    }

    private fun OrbitalObject.getPasses(pos: GeoPos, time: Long, hours: Int): List<PredictOrbitalPass> {
        if (hours <= 0) return emptyList()
        val durationMillis = Math.multiplyExact(hours.toLong(), HourMillis)
        val endDate = Math.addExact(time, durationMillis)
        val sampleCount = ((durationMillis + ElevationSampleStepMillis - 1L) /
            ElevationSampleStepMillis).also {
            require(it <= MaxElevationSamples) {
                "Pass search window exceeds the bounded sampling limit"
            }
        }.toInt()
        val passes = mutableListOf<PredictOrbitalPass>()
        val decayed = data.hasDecayed(time)
        var previous = ElevationSample(time, checkedElevation(pos, time))
        var activeStart: Long? = time.takeIf { previous.isVisible }
        var activeStartBoundary = OrbitalPassBoundary.WINDOW_CLIPPED
        val activeSamples = mutableListOf<ElevationSample>()
        if (previous.isVisible) activeSamples += previous

        for (sampleIndex in 1..sampleCount) {
            val sampleTime = minOf(
                Math.addExact(time, sampleIndex.toLong() * ElevationSampleStepMillis),
                endDate
            )
            val current = ElevationSample(sampleTime, checkedElevation(pos, sampleTime))
            when {
                !previous.isVisible && current.isVisible -> {
                    val aos = refineHorizonCrossing(pos, previous.timeMillis, current.timeMillis)
                    activeStart = aos
                    activeStartBoundary = OrbitalPassBoundary.ACTUAL
                    activeSamples.clear()
                    activeSamples += ElevationSample(aos, checkedElevation(pos, aos))
                    if (current.timeMillis != aos) activeSamples += current
                }

                previous.isVisible && !current.isVisible -> {
                    val los = refineHorizonCrossing(pos, previous.timeMillis, current.timeMillis)
                    activeSamples += ElevationSample(los, checkedElevation(pos, los))
                    activeStart?.let { aos ->
                        buildPass(
                            pos = pos,
                            aos = aos,
                            los = los,
                            aosBoundary = activeStartBoundary,
                            losBoundary = OrbitalPassBoundary.ACTUAL,
                            samples = activeSamples,
                            decayed = decayed
                        )?.let(passes::add)
                    }
                    activeStart = null
                    activeSamples.clear()
                }

                activeStart != null -> activeSamples += current
            }
            previous = current
        }

        activeStart?.let { aos ->
            if (activeSamples.lastOrNull()?.timeMillis != endDate) {
                activeSamples += ElevationSample(endDate, checkedElevation(pos, endDate))
            }
            buildPass(
                pos = pos,
                aos = aos,
                los = endDate,
                aosBoundary = activeStartBoundary,
                losBoundary = OrbitalPassBoundary.WINDOW_CLIPPED,
                samples = activeSamples,
                decayed = decayed
            )?.let(passes::add)
        }
        return passes
    }

    private fun OrbitalObject.buildPass(
        pos: GeoPos,
        aos: Long,
        los: Long,
        aosBoundary: OrbitalPassBoundary,
        losBoundary: OrbitalPassBoundary,
        samples: List<ElevationSample>,
        decayed: Boolean
    ): PredictOrbitalPass? {
        if (los <= aos) return null
        val distinctSamples = samples.distinctBy(ElevationSample::timeMillis)
        val maximumIndex = distinctSamples.indices.maxByOrNull { distinctSamples[it].elevationRadians }
            ?: return null
        val maximumSample = distinctSamples[maximumIndex]
        if (!maximumSample.isVisible) return null
        val tca = when {
            maximumIndex == 0 && aosBoundary == OrbitalPassBoundary.WINDOW_CLIPPED -> aos
            maximumIndex == distinctSamples.lastIndex &&
                losBoundary == OrbitalPassBoundary.WINDOW_CLIPPED -> los
            else -> refineTca(
                pos = pos,
                startMillis = distinctSamples.getOrNull(maximumIndex - 1)?.timeMillis ?: aos,
                endMillis = distinctSamples.getOrNull(maximumIndex + 1)?.timeMillis ?: los
            )
        }
        val aosPos = getFullPosition(pos, aos)
        val losPos = getFullPosition(pos, los)
        val tcaPos = getFullPosition(pos, tca)
        return PredictOrbitalPass(
            aosTime = aos,
            aosAzimuth = aosPos.azimuth.toDegrees().roundToPlaces(1),
            losTime = los,
            losAzimuth = losPos.azimuth.toDegrees().roundToPlaces(1),
            altitude = tcaPos.altitude.toInt(),
            maxElevation = tcaPos.elevation.toDegrees().roundToPlaces(1),
            orbitalObject = this,
            hasDecayed = decayed,
            tcaTime = tca,
            aosBoundary = aosBoundary,
            losBoundary = losBoundary
        )
    }

    private fun OrbitalObject.refineHorizonCrossing(
        pos: GeoPos,
        startMillis: Long,
        endMillis: Long
    ): Long {
        var lower = startMillis
        var upper = endMillis
        val lowerVisible = checkedElevation(pos, lower) > 0.0
        require(lowerVisible != (checkedElevation(pos, upper) > 0.0)) {
            "Horizon refinement requires a visibility sign change"
        }
        var iteration = 0
        while (upper - lower > BoundaryRefinementToleranceMillis &&
            iteration < MaxRefinementIterations
        ) {
            val midpoint = lower + (upper - lower) / 2L
            if ((checkedElevation(pos, midpoint) > 0.0) == lowerVisible) {
                lower = midpoint
            } else {
                upper = midpoint
            }
            iteration++
        }
        return upper.ceilToSecond()
    }

    private fun OrbitalObject.refineTca(
        pos: GeoPos,
        startMillis: Long,
        endMillis: Long
    ): Long {
        var lower = startMillis
        var upper = endMillis
        var iteration = 0
        while (upper - lower > TcaRefinementToleranceMillis &&
            iteration < MaxRefinementIterations
        ) {
            val midpoint = lower + (upper - lower) / 2L
            val probeOffset = minOf(1_000L, (upper - lower) / 4L).coerceAtLeast(1L)
            val before = checkedElevation(pos, midpoint - probeOffset)
            val after = checkedElevation(pos, midpoint + probeOffset)
            if (after > before) {
                lower = midpoint
            } else {
                upper = midpoint
            }
            iteration++
        }
        return listOf(lower, lower + (upper - lower) / 2L, upper)
            .maxBy { checkedElevation(pos, it) }
            .roundToSecond()
            .coerceIn(startMillis, endMillis)
    }

    private fun OrbitalObject.checkedElevation(pos: GeoPos, timeMillis: Long): Double {
        return getElevation(pos, timeMillis).also {
            require(it.isFinite()) { "Non-finite propagated elevation" }
        }
    }

    private fun PredictOrbitalPass.toSatellitePass(record: SatelliteRecord): SatellitePass {
        return SatellitePass(
            catalogNumber = catNum,
            satelliteName = record.displayName,
            aosMillis = aosTime,
            losMillis = losTime,
            tcaMillis = tcaTime,
            maxElevationDegrees = maxElevation,
            aosAzimuthDegrees = aosAzimuth,
            losAzimuthDegrees = losAzimuth,
            altitudeKm = altitude,
            orbitalData = record.orbitalData,
            aosBoundary = aosBoundary.toPassBoundary(),
            losBoundary = losBoundary.toPassBoundary()
        )
    }

    private fun OrbitalPassBoundary.toPassBoundary(): PassBoundary {
        return when (this) {
            OrbitalPassBoundary.ACTUAL -> PassBoundary.ACTUAL
            OrbitalPassBoundary.WINDOW_CLIPPED -> PassBoundary.WINDOW_CLIPPED
        }
    }

    private fun OrbitalData.toPredictData(): PredictOrbitalData {
        return PredictOrbitalData(
            name = name,
            epoch = epoch,
            meanmo = meanMotion,
            eccn = eccentricity,
            incl = inclinationDegrees,
            raan = rightAscensionAscendingNodeDegrees,
            argper = argumentOfPerigeeDegrees,
            meanan = meanAnomalyDegrees,
            catnum = catalogNumber,
            bstar = bstar,
            ndot = meanMotionDot
        )
    }

    private fun StationLocation.toGeoPos(): GeoPos {
        return GeoPos(
            latitude = latitude,
            longitude = longitude,
            altitude = altitudeMeters,
            qthLocator = qthLocator ?: "null",
            timestamp = timestampMillis
        )
    }

    private fun OrbitalPos.toOrbitalPosition(): OrbitalPosition {
        return OrbitalPosition(
            timeMillis = time,
            azimuthDegrees = azimuth.toDegrees().normalizeAzimuthDegrees(),
            elevationDegrees = elevation.toDegrees(),
            rangeRateKmPerSecond = distanceRate,
            aboveHorizon = aboveHorizon,
            latitudeDegrees = latitude.toDegrees().coerceIn(-90.0, 90.0),
            longitudeDegrees = longitude.toDegrees().normalizeLongitudeDegrees(),
            altitudeKm = altitude,
            slantRangeKm = distance.takeIf { it.isFinite() }
        )
    }

    private fun OrbitalPosition.toGroundTrackPoint(): GroundTrackPoint {
        return GroundTrackPoint(
            timeMillis = timeMillis,
            latitudeDegrees = requireNotNull(latitudeDegrees),
            longitudeDegrees = requireNotNull(longitudeDegrees),
            altitudeKm = altitudeKm
        )
    }

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    private fun Double.normalizeAzimuthDegrees(): Double {
        val normalized = this % 360.0
        return if (normalized < 0.0) normalized + 360.0 else normalized
    }

    private fun Double.normalizeLongitudeDegrees(): Double {
        val normalized = ((this + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return if (normalized == -180.0) 180.0 else normalized
    }

    private fun Double.roundToPlaces(places: Int): Double {
        val multiplier = Math.pow(10.0, places.toDouble())
        return round(this * multiplier) / multiplier
    }

    private fun Long.ceilToSecond(): Long = Math.addExact(this, 999L) / 1_000L * 1_000L

    private fun Long.roundToSecond(): Long = Math.addExact(this, 500L) / 1_000L * 1_000L

    private data class ElevationSample(
        val timeMillis: Long,
        val elevationRadians: Double
    ) {
        val isVisible: Boolean = elevationRadians > 0.0
    }
}
