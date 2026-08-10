package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.PassWindow
import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation

class PassPredictionCache(
    private val maxEntries: Int = DefaultMaxEntries,
    private val predictor: (
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow
    ) -> List<SatellitePass>
) {

    private val cache = object : LinkedHashMap<CacheKey, List<SatellitePass>>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, List<SatellitePass>>): Boolean {
            return size > maxEntries
        }
    }

    fun predict(
        satellites: List<SatelliteRecord>,
        station: StationLocation,
        startMillis: Long,
        window: PassWindow
    ): List<SatellitePass> {
        val minuteBucket = Math.floorDiv(startMillis, MillisPerMinute)
        val key = CacheKey(
            satellites = satellites
                .map {
                    SatelliteKey(
                        catalogNumber = it.catalogNumber,
                        selected = it.selected,
                        orbital = it.orbitalData.toCacheKey()
                    )
                }
                .sortedBy { it.catalogNumber },
            station = StationKey(
                latitude = station.latitude,
                longitude = station.longitude,
                altitudeMeters = station.altitudeMeters,
                qthLocator = station.qthLocator?.uppercase(),
                source = station.source.name
            ),
            passWindowHours = window.hoursAhead,
            minimumElevationDegrees = window.minimumElevationDegrees,
            minuteBucket = minuteBucket
        )
        return cache.getOrPut(key) {
            predictor(satellites, station, minuteBucket * MillisPerMinute, window)
        }
    }

    fun clear() {
        cache.clear()
    }

    private data class CacheKey(
        val satellites: List<SatelliteKey>,
        val station: StationKey,
        val passWindowHours: Int,
        val minimumElevationDegrees: Double,
        val minuteBucket: Long
    )

    private data class SatelliteKey(
        val catalogNumber: Int,
        val selected: Boolean,
        val orbital: OrbitalKey
    )

    private data class OrbitalKey(
        val epoch: Double,
        val meanMotion: Double,
        val eccentricity: Double,
        val inclinationDegrees: Double,
        val rightAscensionAscendingNodeDegrees: Double,
        val argumentOfPerigeeDegrees: Double,
        val meanAnomalyDegrees: Double,
        val bstar: Double,
        val meanMotionDot: Double
    )

    private data class StationKey(
        val latitude: Double,
        val longitude: Double,
        val altitudeMeters: Double,
        val qthLocator: String?,
        val source: String
    )

    private fun OrbitalData.toCacheKey(): OrbitalKey {
        return OrbitalKey(
            epoch = epoch,
            meanMotion = meanMotion,
            eccentricity = eccentricity,
            inclinationDegrees = inclinationDegrees,
            rightAscensionAscendingNodeDegrees = rightAscensionAscendingNodeDegrees,
            argumentOfPerigeeDegrees = argumentOfPerigeeDegrees,
            meanAnomalyDegrees = meanAnomalyDegrees,
            bstar = bstar,
            meanMotionDot = meanMotionDot
        )
    }

    private companion object {
        const val DefaultMaxEntries = 8
        const val MillisPerMinute = 60_000L
    }
}
