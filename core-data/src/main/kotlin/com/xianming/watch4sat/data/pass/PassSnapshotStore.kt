package com.xianming.watch4sat.data.pass

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xianming.watch4sat.data.identity.SatelliteDataIdentity
import com.xianming.watch4sat.data.settings.Watch4SatSettings
import com.xianming.watch4sat.domain.model.PassBoundary
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.domain.model.SatelliteRecord
import com.xianming.watch4sat.domain.model.StationLocation
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.watch4SatPassSnapshotDataStore by preferencesDataStore(
    name = "watch4sat_pass_snapshots",
    corruptionHandler = passSnapshotCorruptionHandler()
)

internal fun passSnapshotCorruptionHandler() = ReplaceFileCorruptionHandler {
    preferencesOf(SnapshotRecoveryRequiredKey to true)
}

interface PassSnapshotCache {
    suspend fun read(key: PassSnapshotKey, nowMillis: Long): PassSnapshot?

    suspend fun write(snapshot: PassSnapshot)

    suspend fun replace(snapshot: PassSnapshot) {
        write(snapshot)
    }

    suspend fun clear()

    suspend fun discard(snapshot: PassSnapshot) {
        clear()
    }

    suspend fun recoveryRequired(): Boolean = false

    suspend fun consumeRecoveryRequired(): Boolean = false
}

class PassSnapshotStore(
    private val dataStore: DataStore<Preferences>
) : PassSnapshotCache {

    constructor(context: Context) : this(context.applicationContext.watch4SatPassSnapshotDataStore)

    override suspend fun read(key: PassSnapshotKey, nowMillis: Long): PassSnapshot? {
        val encoded = dataStore.data
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()[Keys.snapshotJson] ?: return null
        val snapshot = runCatching { PassSnapshotCodec.decode(encoded) }.getOrNull()
        if (snapshot == null) {
            dataStore.edit { preferences ->
                preferences.remove(Keys.snapshotJson)
                preferences[SnapshotRecoveryRequiredKey] = true
            }
            return null
        }
        return snapshot.takeIf { it.key == key && it.isUsableAt(nowMillis) }
    }

    override suspend fun write(snapshot: PassSnapshot) {
        persist(snapshot, force = false)
    }

    override suspend fun replace(snapshot: PassSnapshot) {
        persist(snapshot, force = true)
    }

    private suspend fun persist(snapshot: PassSnapshot, force: Boolean) {
        require(snapshot.isComplete) {
            "Only current-schema complete pass snapshots may be persisted"
        }
        dataStore.edit { preferences ->
            val existing = preferences[Keys.snapshotJson]
                ?.let { encoded -> runCatching { PassSnapshotCodec.decode(encoded) }.getOrNull() }
            if (!force && !snapshot.shouldReplace(existing)) return@edit
            preferences[Keys.snapshotJson] = PassSnapshotCodec.encode(snapshot)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.snapshotJson)
        }
    }

    override suspend fun discard(snapshot: PassSnapshot) {
        dataStore.edit { preferences ->
            val current = preferences[Keys.snapshotJson]
                ?.let { encoded -> runCatching { PassSnapshotCodec.decode(encoded) }.getOrNull() }
            if (current == snapshot) {
                preferences.remove(Keys.snapshotJson)
            }
        }
    }

    override suspend fun recoveryRequired(): Boolean {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()[SnapshotRecoveryRequiredKey] == true
    }

    override suspend fun consumeRecoveryRequired(): Boolean {
        var recovered = false
        dataStore.edit { preferences ->
            recovered = preferences[SnapshotRecoveryRequiredKey] == true
            preferences.remove(SnapshotRecoveryRequiredKey)
        }
        return recovered
    }

    private object Keys {
        val snapshotJson = stringPreferencesKey("pass_snapshot_json")
    }
}

private fun PassSnapshot.shouldReplace(existing: PassSnapshot?): Boolean {
    if (existing == null) return true
    if (existing.key != key) {
        return generatedAtMillis >= existing.generatedAtMillis
    }
    if (existing.isComplete && !isComplete) return false
    if (!existing.isComplete && isComplete) return true
    if (coverageEndMillis != existing.coverageEndMillis) {
        return coverageEndMillis > existing.coverageEndMillis
    }
    return coverageStartMillis <= existing.coverageStartMillis
}

@Serializable
data class PassSnapshotKey(
    val stationKey: String,
    val selectedSatelliteIds: List<Int>,
    val tleFingerprint: String,
    val passWindowHours: Int,
    val minimumElevationDegrees: Double = 0.0
)

@Serializable
data class PassSnapshot(
    val key: PassSnapshotKey,
    val generatedAtMillis: Long,
    val coveredWindowHours: Int,
    val passes: List<PassSnapshotPass>,
    val schemaVersion: Int = LegacySchemaVersion,
    val coverageStartMillis: Long = generatedAtMillis,
    val coverageEndMillis: Long =
        generatedAtMillis + coveredWindowHours.coerceAtLeast(0).toLong() * HourMillis,
    val completion: PassSnapshotCompletion = PassSnapshotCompletion.PARTIAL
) {
    val isComplete: Boolean
        get() {
            val expectedCoverageMillis =
                key.passWindowHours.coerceAtLeast(0).toLong() * HourMillis
            return schemaVersion == CurrentSchemaVersion &&
                completion == PassSnapshotCompletion.COMPLETE &&
                coverageEndMillis >= coverageStartMillis &&
                coverageEndMillis - coverageStartMillis >= expectedCoverageMillis
        }

    fun isUsableAt(nowMillis: Long): Boolean {
        return nowMillis <= coverageEndMillis || passes.any { pass -> pass.losMillis > nowMillis }
    }

    fun reusableCoverageEndMillis(startMillis: Long): Long {
        if (coverageStartMillis > startMillis) return startMillis
        return coverageEndMillis.coerceAtLeast(startMillis)
    }

    fun completelyCovers(startMillis: Long, endMillis: Long): Boolean {
        return isComplete &&
            coverageStartMillis <= startMillis &&
            coverageEndMillis >= endMillis
    }

    fun toPasses(satellites: List<SatelliteRecord>): List<SatellitePass> {
        val orbitalByCatalog = satellites.associate { it.catalogNumber to it.orbitalData }
        return passes.map { pass ->
            SatellitePass(
                catalogNumber = pass.catalogNumber,
                satelliteName = pass.satelliteName,
                aosMillis = pass.aosMillis,
                losMillis = pass.losMillis,
                tcaMillis = pass.tcaMillis,
                maxElevationDegrees = pass.maxElevationDegrees,
                aosAzimuthDegrees = pass.aosAzimuthDegrees,
                losAzimuthDegrees = pass.losAzimuthDegrees,
                altitudeKm = pass.altitudeKm,
                orbitalData = orbitalByCatalog[pass.catalogNumber],
                aosBoundary = pass.aosBoundary,
                losBoundary = pass.losBoundary
            )
        }
    }

    companion object {
        const val CurrentSchemaVersion = 2
        const val LegacySchemaVersion = 1

        fun fromPasses(
            key: PassSnapshotKey,
            generatedAtMillis: Long,
            coveredWindowHours: Int,
            passes: List<SatellitePass>,
            coverageStartMillis: Long = generatedAtMillis,
            coverageEndMillis: Long =
                generatedAtMillis + coveredWindowHours.coerceAtLeast(0).toLong() * HourMillis,
            completion: PassSnapshotCompletion = PassSnapshotCompletion.COMPLETE
        ): PassSnapshot {
            return PassSnapshot(
                key = key,
                generatedAtMillis = generatedAtMillis,
                coveredWindowHours = coveredWindowHours,
                passes = passes
                    .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
                    .map { pass ->
                        PassSnapshotPass(
                            catalogNumber = pass.catalogNumber,
                            satelliteName = pass.satelliteName,
                            aosMillis = pass.aosMillis,
                            losMillis = pass.losMillis,
                            tcaMillis = pass.tcaMillis,
                            maxElevationDegrees = pass.maxElevationDegrees,
                            aosAzimuthDegrees = pass.aosAzimuthDegrees,
                            losAzimuthDegrees = pass.losAzimuthDegrees,
                            altitudeKm = pass.altitudeKm,
                            aosBoundary = pass.aosBoundary,
                            losBoundary = pass.losBoundary
                        )
                    },
                schemaVersion = CurrentSchemaVersion,
                coverageStartMillis = coverageStartMillis,
                coverageEndMillis = coverageEndMillis,
                completion = completion
            )
        }
    }
}

@Serializable
enum class PassSnapshotCompletion {
    PARTIAL,
    COMPLETE
}

@Serializable
data class PassSnapshotPass(
    val catalogNumber: Int,
    val satelliteName: String,
    val aosMillis: Long,
    val losMillis: Long,
    val tcaMillis: Long,
    val maxElevationDegrees: Double,
    val aosAzimuthDegrees: Double,
    val losAzimuthDegrees: Double,
    val altitudeKm: Int? = null,
    val aosBoundary: PassBoundary = PassBoundary.ACTUAL,
    val losBoundary: PassBoundary = PassBoundary.ACTUAL
)

object PassSnapshotKeys {
    fun forInputs(
        station: StationLocation,
        satellites: List<SatelliteRecord>,
        settings: Watch4SatSettings,
        minimumElevationDegrees: Double = 0.0
    ): PassSnapshotKey {
        val calculationIds = satellites
            .map { it.catalogNumber }
            .distinct()
            .sorted()
        return PassSnapshotKey(
            stationKey = "${station.cacheKey()}|g=${settings.stationDataGeneration}",
            selectedSatelliteIds = calculationIds,
            tleFingerprint = SatelliteDataIdentity.sha256(satellites),
            passWindowHours = CanonicalPassSnapshotCoverageHours,
            minimumElevationDegrees = minimumElevationDegrees
        )
    }

    private fun StationLocation.cacheKey(): String {
        return listOf(
            "%.6f".format(Locale.US, latitude),
            "%.6f".format(Locale.US, longitude),
            "%.1f".format(Locale.US, altitudeMeters),
            qthLocator?.uppercase(Locale.US).orEmpty(),
            source.name
        ).joinToString("|")
    }

}

object PassSnapshotCodec {
    fun encode(snapshot: PassSnapshot): String = json.encodeToString(snapshot)

    fun decode(encoded: String): PassSnapshot = json.decodeFromString(encoded)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

private val SnapshotRecoveryRequiredKey = booleanPreferencesKey("snapshot_recovery_required")
const val CanonicalPassSnapshotCoverageHours = 24
private const val HourMillis = 60L * 60L * 1000L
