package com.xianming.watch4sat.data.settings

import com.xianming.watch4sat.domain.model.StationLocation
import kotlinx.serialization.Serializable

const val DEFAULT_PASS_WINDOW_HOURS = 12

data class Watch4SatSettings(
    val stationLocation: StationLocation? = null,
    val stationDataGeneration: Long = 0L,
    val stationDataDeletionInProgress: Boolean = false,
    val selectedSatelliteIds: Set<Int> = emptySet(),
    val passWindowHours: Int = DEFAULT_PASS_WINDOW_HOURS,
    val lastSatelliteDataUpdateMillis: Long? = null,
    val lastTransmitterDataUpdateMillis: Long? = null,
    val lastSatelliteDataError: String? = null,
    val lastTransmitterDataError: String? = null,
    val lastSatelliteDataFailureMillis: Long? = null,
    val lastTransmitterDataFailureMillis: Long? = null,
    val mapTileMode: MapTileMode = MapTileMode.AUTO,
    val themePreset: AppThemePreset = AppThemePreset.SYSTEM,
    val developerOptionsEnabled: Boolean = false,
    val radarKeepScreenOn: Boolean = false,
    val radarForwardAxis: RadarForwardAxis = RadarForwardAxis.SCREEN_TOP,
    val radarFallbackWristSide: RadarWristSide = RadarWristSide.LEFT,
    val setupCompleted: Boolean = false,
    val setupStep: String = "welcome",
    val setupSkippedSteps: Set<String> = emptySet(),
    val passAlertAdvanceMinutes: Int = 0,
    val autoDataFreshnessEnabled: Boolean = true,
    val minimumElevationFilterEnabled: Boolean = false,
    val minimumElevationDegrees: Int = 10,
    val customDopplerBaseFrequencies: Map<Int, CustomDopplerBaseFrequencies> = emptyMap()
)

@Serializable
data class CustomDopplerBaseFrequencies(
    val downlinkHz: Long? = null,
    val uplinkHz: Long? = null
)
