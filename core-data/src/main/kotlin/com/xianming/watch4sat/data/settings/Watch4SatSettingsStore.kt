package com.xianming.watch4sat.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xianming.watch4sat.domain.model.LocationSource
import com.xianming.watch4sat.domain.model.StationLocation
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.watch4SatDataStore by preferencesDataStore(
    name = "watch4sat_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

sealed interface SetupCompletionResult {
    data object Completed : SetupCompletionResult
    data object AlreadyCompleted : SetupCompletionResult
    data class Rejected(
        val unresolvedRequirements: Set<SetupCompletionRequirement>
    ) : SetupCompletionResult
}

enum class SetupCompletionRequirement {
    TLE,
    QTH,
    SATELLITES
}

class Watch4SatSettingsStore(
    private val dataStore: DataStore<Preferences>
) {

    constructor(context: Context) : this(context.applicationContext.watch4SatDataStore)

    val settings: Flow<Watch4SatSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences.toSettings() }

    val selectedSatelliteIds: Flow<Set<Int>> = settings
        .map { it.selectedSatelliteIds }
        .distinctUntilChanged()

    suspend fun getSettings(): Watch4SatSettings {
        return settings.first()
    }

    suspend fun setStationLocation(location: StationLocation) {
        dataStore.edit { preferences ->
            check(preferences[Keys.stationDataDeletionInProgress] != true)
            preferences[Keys.stationDataGeneration] =
                nextStationDataGeneration(preferences[Keys.stationDataGeneration] ?: 0L)
            preferences[Keys.stationLatitude] = location.latitude
            preferences[Keys.stationLongitude] = location.longitude
            preferences[Keys.stationAltitudeMeters] = location.altitudeMeters
            preferences[Keys.stationTimestampMillis] = location.timestampMillis
            preferences[Keys.stationSource] = location.source.name
            location.qthLocator?.let { preferences[Keys.stationQthLocator] = it }
                ?: preferences.remove(Keys.stationQthLocator)
            location.accuracyMeters?.let { preferences[Keys.stationAccuracyMeters] = it.toDouble() }
                ?: preferences.remove(Keys.stationAccuracyMeters)
        }
    }

    suspend fun clearStationLocation() {
        beginStationDataDeletion()
        completeStationDataDeletion()
    }

    suspend fun beginStationDataDeletion(): Long {
        var generation = 0L
        dataStore.edit { preferences ->
            val deletionInProgress = preferences[Keys.stationDataDeletionInProgress] == true
            generation = if (deletionInProgress) {
                preferences[Keys.stationDataGeneration] ?: 0L
            } else {
                nextStationDataGeneration(preferences[Keys.stationDataGeneration] ?: 0L)
                    .also { next ->
                        preferences[Keys.stationDataGeneration] = next
                        preferences[Keys.stationDataDeletionInProgress] = true
                    }
            }
        }
        return generation
    }

    suspend fun completeStationDataDeletion() {
        dataStore.edit { preferences ->
            preferences.removeStationLocation()
            preferences[Keys.stationDataDeletionInProgress] = false
        }
    }

    suspend fun setSelectedSatelliteIds(catalogNumbers: Set<Int>) {
        dataStore.edit { preferences ->
            preferences[Keys.selectedSatelliteIds] = catalogNumbers
                .toList()
                .sorted()
                .map { it.toString() }
                .toSet()
        }
    }

    suspend fun setPassWindowHours(hours: Int) {
        require(hours > 0) { "Pass window hours must be positive." }
        dataStore.edit { preferences ->
            preferences[Keys.passWindowHours] = hours
        }
    }

    suspend fun setMapTileMode(mode: MapTileMode) {
        dataStore.edit { preferences ->
            preferences[Keys.mapTileMode] = mode.name
        }
    }

    suspend fun setThemePreset(preset: AppThemePreset) {
        dataStore.edit { preferences ->
            preferences[Keys.themePreset] = preset.name
        }
    }

    suspend fun setDeveloperOptionsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.developerOptionsEnabled] = enabled
        }
    }

    suspend fun setRadarKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.radarKeepScreenOn] = enabled
        }
    }

    suspend fun setRadarForwardAxis(axis: RadarForwardAxis) {
        dataStore.edit { preferences ->
            preferences[Keys.radarForwardAxis] = axis.name
        }
    }

    suspend fun setRadarFallbackWristSide(side: RadarWristSide) {
        dataStore.edit { preferences ->
            preferences[Keys.radarFallbackWristSide] = side.name
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.setupCompleted] = completed
        }
    }

    suspend fun completeSetupIfEligible(
        latestAvailableCatalogNumbers: Set<Int>
    ): SetupCompletionResult {
        var result: SetupCompletionResult? = null
        dataStore.edit { preferences ->
            val current = preferences.toSettings()
            if (current.setupCompleted) {
                result = SetupCompletionResult.AlreadyCompleted
                return@edit
            }

            val skippedSteps = current.setupSkippedSteps
            val unresolvedRequirements = buildSet {
                val tleResolved = latestAvailableCatalogNumbers.isNotEmpty() ||
                    skippedSteps.any { it == "data" || it == "tle" }
                if (!tleResolved) add(SetupCompletionRequirement.TLE)

                val qthResolved = current.stationLocation != null || "qth" in skippedSteps
                if (!qthResolved) add(SetupCompletionRequirement.QTH)

                val satellitesResolved =
                    current.selectedSatelliteIds.any { it in latestAvailableCatalogNumbers } ||
                        "satellites" in skippedSteps
                if (!satellitesResolved) add(SetupCompletionRequirement.SATELLITES)
            }
            if (unresolvedRequirements.isNotEmpty()) {
                result = SetupCompletionResult.Rejected(unresolvedRequirements)
                return@edit
            }

            preferences[Keys.setupStep] = "done"
            preferences[Keys.setupCompleted] = true
            result = SetupCompletionResult.Completed
        }
        return checkNotNull(result)
    }

    suspend fun setSetupStep(step: String) {
        dataStore.edit { preferences ->
            preferences[Keys.setupStep] = step
        }
    }

    suspend fun setSetupSkippedSteps(steps: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.setupSkippedSteps] = steps.sorted().toSet()
        }
    }

    suspend fun skipSetupStep(step: String) {
        dataStore.edit { preferences ->
            preferences[Keys.setupSkippedSteps] = (
                preferences[Keys.setupSkippedSteps].orEmpty() + step
                ).sorted().toSet()
        }
    }

    suspend fun skipSetupStepAndMoveTo(step: String, nextStep: String) {
        dataStore.edit { preferences ->
            preferences[Keys.setupSkippedSteps] = (
                preferences[Keys.setupSkippedSteps].orEmpty() + step
                ).sorted().toSet()
            preferences[Keys.setupStep] = nextStep
        }
    }

    suspend fun clearSetupSkippedStep(step: String) {
        dataStore.edit { preferences ->
            preferences[Keys.setupSkippedSteps] = (
                preferences[Keys.setupSkippedSteps].orEmpty() - step
                ).sorted().toSet()
        }
    }

    suspend fun setPassAlertAdvanceMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.passAlertAdvanceMinutes] = minutes
        }
    }

    suspend fun setAutoDataFreshnessEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.autoDataFreshnessEnabled] = enabled
        }
    }

    suspend fun setMinimumElevationFilterEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.minimumElevationFilterEnabled] = enabled
        }
    }

    suspend fun setMinimumElevationDegrees(degrees: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.minimumElevationDegrees] = degrees
        }
    }

    suspend fun setLastSatelliteDataUpdateMillis(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastSatelliteDataUpdateMillis] = timestampMillis
            preferences.remove(Keys.lastSatelliteDataError)
            preferences.remove(Keys.lastSatelliteDataFailureMillis)
        }
    }

    suspend fun setLastTransmitterDataUpdateMillis(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastTransmitterDataUpdateMillis] = timestampMillis
            preferences.remove(Keys.lastTransmitterDataError)
            preferences.remove(Keys.lastTransmitterDataFailureMillis)
        }
    }

    suspend fun setSatelliteDataRefreshError(message: String, timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastSatelliteDataError] = message
            preferences[Keys.lastSatelliteDataFailureMillis] = timestampMillis
        }
    }

    suspend fun setTransmitterDataRefreshError(message: String, timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastTransmitterDataError] = message
            preferences[Keys.lastTransmitterDataFailureMillis] = timestampMillis
        }
    }

    suspend fun setCustomDopplerBaseFrequency(
        catalogNumber: Int,
        downlinkHz: Long?,
        uplinkHz: Long?
    ) {
        dataStore.edit { preferences ->
            val updated = preferences.decodeCustomDopplerBaseFrequencies().toMutableMap()
            if (downlinkHz == null && uplinkHz == null) {
                updated.remove(catalogNumber)
            } else {
                updated[catalogNumber] = CustomDopplerBaseFrequencies(
                    downlinkHz = downlinkHz,
                    uplinkHz = uplinkHz
                )
            }
            preferences[Keys.customDopplerBaseFrequenciesJson] = json.encodeToString(updated.toMap())
        }
    }

    suspend fun clearCustomDopplerBaseFrequency(catalogNumber: Int) {
        setCustomDopplerBaseFrequency(
            catalogNumber = catalogNumber,
            downlinkHz = null,
            uplinkHz = null
        )
    }

    private fun Preferences.toSettings(): Watch4SatSettings {
        val stationDataDeletionInProgress =
            this[Keys.stationDataDeletionInProgress] ?: false
        return Watch4SatSettings(
            stationLocation = if (stationDataDeletionInProgress) {
                null
            } else {
                stationLocationOrNull()
            },
            stationDataGeneration = this[Keys.stationDataGeneration] ?: 0L,
            stationDataDeletionInProgress = stationDataDeletionInProgress,
            selectedSatelliteIds = this[Keys.selectedSatelliteIds]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
            passWindowHours = this[Keys.passWindowHours] ?: DEFAULT_PASS_WINDOW_HOURS,
            lastSatelliteDataUpdateMillis = this[Keys.lastSatelliteDataUpdateMillis],
            lastTransmitterDataUpdateMillis = this[Keys.lastTransmitterDataUpdateMillis],
            lastSatelliteDataError = this[Keys.lastSatelliteDataError],
            lastTransmitterDataError = this[Keys.lastTransmitterDataError],
            lastSatelliteDataFailureMillis = this[Keys.lastSatelliteDataFailureMillis],
            lastTransmitterDataFailureMillis = this[Keys.lastTransmitterDataFailureMillis],
            mapTileMode = MapTileMode.fromStoredName(this[Keys.mapTileMode]),
            themePreset = this[Keys.themePreset]
                ?.let { AppThemePreset.fromStoredName(it) }
                ?: AppThemePreset.SYSTEM,
            developerOptionsEnabled = this[Keys.developerOptionsEnabled] ?: false,
            radarKeepScreenOn = this[Keys.radarKeepScreenOn] ?: false,
            radarForwardAxis = RadarForwardAxis.fromStoredName(this[Keys.radarForwardAxis]),
            radarFallbackWristSide = RadarWristSide.fromStoredName(
                this[Keys.radarFallbackWristSide]
            ),
            setupCompleted = this[Keys.setupCompleted] ?: false,
            setupStep = this[Keys.setupStep] ?: "welcome",
            setupSkippedSteps = this[Keys.setupSkippedSteps] ?: emptySet(),
            passAlertAdvanceMinutes = this[Keys.passAlertAdvanceMinutes] ?: 0,
            autoDataFreshnessEnabled = this[Keys.autoDataFreshnessEnabled] ?: true,
            minimumElevationFilterEnabled = this[Keys.minimumElevationFilterEnabled] ?: false,
            minimumElevationDegrees = this[Keys.minimumElevationDegrees] ?: 10,
            customDopplerBaseFrequencies = decodeCustomDopplerBaseFrequencies()
        )
    }

    private fun Preferences.stationLocationOrNull(): StationLocation? {
        val latitude = this[Keys.stationLatitude] ?: return null
        val longitude = this[Keys.stationLongitude] ?: return null
        val source = this[Keys.stationSource]
            ?.let { value -> runCatching { LocationSource.valueOf(value) }.getOrNull() }
            ?: LocationSource.MANUAL_COORDINATES

        return StationLocation(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = this[Keys.stationAltitudeMeters] ?: 0.0,
            qthLocator = this[Keys.stationQthLocator],
            timestampMillis = this[Keys.stationTimestampMillis] ?: 0L,
            source = source,
            accuracyMeters = this[Keys.stationAccuracyMeters]?.toFloat()
        )
    }

    private fun MutablePreferences.removeStationLocation() {
        remove(Keys.stationLatitude)
        remove(Keys.stationLongitude)
        remove(Keys.stationAltitudeMeters)
        remove(Keys.stationTimestampMillis)
        remove(Keys.stationSource)
        remove(Keys.stationQthLocator)
        remove(Keys.stationAccuracyMeters)
    }

    private fun Preferences.decodeCustomDopplerBaseFrequencies(): Map<Int, CustomDopplerBaseFrequencies> {
        val encoded = this[Keys.customDopplerBaseFrequenciesJson] ?: return emptyMap()
        return runCatching {
            json.decodeFromString<Map<Int, CustomDopplerBaseFrequencies>>(encoded)
        }.getOrDefault(emptyMap())
    }

    private object Keys {
        val stationLatitude = doublePreferencesKey("station_latitude")
        val stationLongitude = doublePreferencesKey("station_longitude")
        val stationAltitudeMeters = doublePreferencesKey("station_altitude_meters")
        val stationQthLocator = stringPreferencesKey("station_qth_locator")
        val stationTimestampMillis = longPreferencesKey("station_timestamp_millis")
        val stationSource = stringPreferencesKey("station_source")
        val stationAccuracyMeters = doublePreferencesKey("station_accuracy_meters")
        val stationDataGeneration = longPreferencesKey("station_data_generation")
        val stationDataDeletionInProgress =
            booleanPreferencesKey("station_data_deletion_in_progress")
        val selectedSatelliteIds = stringSetPreferencesKey("selected_satellite_ids")
        val passWindowHours = intPreferencesKey("pass_window_hours")
        val lastSatelliteDataUpdateMillis = longPreferencesKey("last_satellite_data_update_millis")
        val lastTransmitterDataUpdateMillis = longPreferencesKey("last_transmitter_data_update_millis")
        val lastSatelliteDataError = stringPreferencesKey("last_satellite_data_error")
        val lastTransmitterDataError = stringPreferencesKey("last_transmitter_data_error")
        val lastSatelliteDataFailureMillis = longPreferencesKey("last_satellite_data_failure_millis")
        val lastTransmitterDataFailureMillis = longPreferencesKey("last_transmitter_data_failure_millis")
        val mapTileMode = stringPreferencesKey("map_tile_mode")
        val themePreset = stringPreferencesKey("theme_preset")
        val developerOptionsEnabled = booleanPreferencesKey("developer_options_enabled")
        val radarKeepScreenOn = booleanPreferencesKey("radar_keep_screen_on")
        val radarForwardAxis = stringPreferencesKey("radar_forward_axis")
        val radarFallbackWristSide = stringPreferencesKey("radar_fallback_wrist_side")
        val setupCompleted = booleanPreferencesKey("setup_completed")
        val setupStep = stringPreferencesKey("setup_step")
        val setupSkippedSteps = stringSetPreferencesKey("setup_skipped_steps")
        val passAlertAdvanceMinutes = intPreferencesKey("pass_alert_advance_minutes")
        val autoDataFreshnessEnabled = booleanPreferencesKey("auto_data_freshness_enabled")
        val minimumElevationFilterEnabled = booleanPreferencesKey("minimum_elevation_filter_enabled")
        val minimumElevationDegrees = intPreferencesKey("minimum_elevation_degrees")
        val customDopplerBaseFrequenciesJson = stringPreferencesKey("custom_doppler_base_frequencies_json")
    }

    private companion object {
        fun nextStationDataGeneration(current: Long): Long {
            return if (current == Long.MAX_VALUE) 1L else current + 1L
        }

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
