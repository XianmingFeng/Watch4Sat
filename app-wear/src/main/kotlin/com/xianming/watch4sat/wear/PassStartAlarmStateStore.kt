package com.xianming.watch4sat.wear

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.state.PassStartScheduleCandidate
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.watch4SatPassStartAlarmDataStore by preferencesDataStore(
    name = "watch4sat_pass_start_alarm",
    corruptionHandler = passStartAlarmCorruptionHandler()
)

internal fun passStartAlarmCorruptionHandler() = ReplaceFileCorruptionHandler {
    preferencesOf(AlarmStateRecoveryRequiredKey to true)
}

class PassStartAlarmStateStore(
    private val dataStore: DataStore<Preferences>
) {

    constructor(context: Context) : this(context.applicationContext.watch4SatPassStartAlarmDataStore)

    suspend fun read(): PassStartAlarmState {
        val preferences = dataStore.data
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()
        return PassStartAlarmState(
            scheduledPassKey = preferences[Keys.scheduledPassKey],
            scheduledTriggerAtMillis = preferences[Keys.scheduledTriggerAtMillis],
            handledPassKeys = preferences[Keys.handledPassKeys].orEmpty(),
            pendingForegroundAlarm = preferences.pendingForegroundAlarm()
        )
    }

    suspend fun setScheduledPass(passKey: String, triggerAtMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.scheduledPassKey] = passKey
            preferences[Keys.scheduledTriggerAtMillis] = triggerAtMillis
        }
    }

    suspend fun clearScheduledPass() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.scheduledPassKey)
            preferences.remove(Keys.scheduledTriggerAtMillis)
        }
    }

    suspend fun markHandled(passKey: String) {
        dataStore.edit { preferences ->
            val retained = (preferences[Keys.handledPassKeys].orEmpty().toList() + passKey)
                .takeLast(MaxHandledPassKeys)
                .toSet()
            preferences[Keys.handledPassKeys] = retained
        }
    }

    suspend fun setPendingForegroundAlarm(candidate: PassStartScheduleCandidate) {
        dataStore.edit { preferences ->
            preferences[Keys.pendingForegroundCatalogNumber] = candidate.pass.catalogNumber
            preferences[Keys.pendingForegroundAosMillis] = candidate.pass.aosMillis
            preferences[Keys.pendingForegroundTriggerAtMillis] = candidate.triggerAtMillis
        }
    }

    suspend fun clearPendingForegroundAlarm(passKey: String? = null) {
        dataStore.edit { preferences ->
            val pending = preferences.pendingForegroundAlarm()
            if (passKey == null || pending?.passKey == passKey) {
                preferences.remove(Keys.pendingForegroundCatalogNumber)
                preferences.remove(Keys.pendingForegroundAosMillis)
                preferences.remove(Keys.pendingForegroundTriggerAtMillis)
            }
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun consumeRecoveryRequired(): Boolean {
        var recovered = false
        dataStore.edit { preferences ->
            recovered = preferences[AlarmStateRecoveryRequiredKey] == true
            preferences.remove(AlarmStateRecoveryRequiredKey)
        }
        return recovered
    }

    suspend fun recoveryRequired(): Boolean {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()[AlarmStateRecoveryRequiredKey] == true
    }

    private fun Preferences.pendingForegroundAlarm(): PendingForegroundPassAlarm? {
        val catalogNumber = this[Keys.pendingForegroundCatalogNumber] ?: return null
        val aosMillis = this[Keys.pendingForegroundAosMillis] ?: return null
        val triggerAtMillis = this[Keys.pendingForegroundTriggerAtMillis] ?: return null
        return PendingForegroundPassAlarm(
            catalogNumber = catalogNumber,
            aosMillis = aosMillis,
            triggerAtMillis = triggerAtMillis
        )
    }

    private object Keys {
        val scheduledPassKey = stringPreferencesKey("scheduled_pass_key")
        val scheduledTriggerAtMillis = longPreferencesKey("scheduled_trigger_at_millis")
        val handledPassKeys = stringSetPreferencesKey("handled_pass_keys")
        val pendingForegroundCatalogNumber = intPreferencesKey("pending_foreground_catalog_number")
        val pendingForegroundAosMillis = longPreferencesKey("pending_foreground_aos_millis")
        val pendingForegroundTriggerAtMillis = longPreferencesKey("pending_foreground_trigger_at_millis")
    }

    private companion object {
        const val MaxHandledPassKeys = 48
    }
}

private val AlarmStateRecoveryRequiredKey = booleanPreferencesKey("alarm_state_recovery_required")

data class PassStartAlarmState(
    val scheduledPassKey: String? = null,
    val scheduledTriggerAtMillis: Long? = null,
    val handledPassKeys: Set<String> = emptySet(),
    val pendingForegroundAlarm: PendingForegroundPassAlarm? = null
)

data class PendingForegroundPassAlarm(
    val catalogNumber: Int,
    val aosMillis: Long,
    val triggerAtMillis: Long
) {
    val passKey: String = PassStartNotificationPolicy.passKey(catalogNumber, aosMillis)
}
