package com.xianming.watch4sat.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import com.xianming.watch4sat.wear.state.PassStartAlarmMode
import com.xianming.watch4sat.wear.state.PassStartAlarmScheduleKey
import com.xianming.watch4sat.wear.state.PassStartAlarmUpdate
import com.xianming.watch4sat.wear.state.PassStartAlarmUpdateCoordinator
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.state.PassStartScheduleCandidate
import com.xianming.watch4sat.wear.state.PassStartSchedulePolicy
import com.xianming.watch4sat.wear.state.shouldApplyAgainst

class PassStartAlarmScheduler(
    private val context: Context,
    private val stateStore: PassStartAlarmStateStore = PassStartAlarmStateStore(context),
    private val canScheduleStationData: suspend () -> Boolean = { true }
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val updateCoordinator = PassStartAlarmUpdateCoordinator()

    suspend fun schedule(
        candidate: PassStartScheduleCandidate?,
        knownState: PassStartAlarmState? = null
    ) {
        applyUpdate(candidate = candidate, knownState = knownState, force = false)
    }

    suspend fun forceReschedule(candidate: PassStartScheduleCandidate?) {
        applyUpdate(candidate = candidate, knownState = null, force = true)
    }

    private suspend fun applyUpdate(
        candidate: PassStartScheduleCandidate?,
        knownState: PassStartAlarmState?,
        force: Boolean
    ) {
        if (candidate != null && !canScheduleStationData()) {
            cancelNow()
            return
        }
        val update = updateCoordinator.update(candidate, force) ?: return
        val persistedScheduleKey = if (!force && update is PassStartAlarmUpdate.Schedule) {
            val state = knownState ?: stateStore.read()
            PassStartAlarmScheduleKey.from(state.scheduledPassKey, state.scheduledTriggerAtMillis)
        } else {
            null
        }
        if (!update.shouldApplyAgainst(persistedScheduleKey, force)) return
        when (update) {
            is PassStartAlarmUpdate.Schedule -> scheduleNow(update.candidate)
            PassStartAlarmUpdate.Cancel -> cancelNow()
        }
    }

    private suspend fun scheduleNow(candidate: PassStartScheduleCandidate) {
        val pendingIntent = pendingIntent(
            catalogNumber = candidate.pass.catalogNumber,
            aosMillis = candidate.pass.aosMillis,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val mode = PassStartSchedulePolicy.alarmMode(canScheduleExactAlarms())
        try {
            when (mode) {
                PassStartAlarmMode.ExactAllowWhileIdle -> alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    candidate.triggerAtMillis,
                    pendingIntent
                )
                PassStartAlarmMode.InexactAllowWhileIdle -> alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    candidate.triggerAtMillis,
                    pendingIntent
                )
            }
            stateStore.setScheduledPass(candidate.passKey, candidate.triggerAtMillis)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                candidate.triggerAtMillis,
                pendingIntent
            )
            stateStore.setScheduledPass(candidate.passKey, candidate.triggerAtMillis)
        }
    }

    suspend fun cancel() {
        schedule(null)
    }

    private suspend fun cancelNow() {
        val pendingIntent = pendingIntent(
            catalogNumber = 0,
            aosMillis = 0L,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        stateStore.clearScheduledPass()
    }

    fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(
        catalogNumber: Int,
        aosMillis: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(appContext, PassStartAlarmReceiver::class.java).apply {
            action = PassStartNotificationPolicy.actionPassStartAlarm
            data = Uri.parse("watch4sat://pass-start-alarm")
            putExtra(PassStartNotificationPolicy.extraCatalogNumber, catalogNumber)
            putExtra(PassStartNotificationPolicy.extraAosMillis, aosMillis)
        }
        return PendingIntent.getBroadcast(appContext, RequestCode, intent, flags)
    }

    private companion object {
        const val RequestCode = 0x51A7
    }
}

internal fun stationDataScheduleGuard(
    context: Context
): suspend () -> Boolean {
    val settingsStore = Watch4SatSettingsStore(context.applicationContext)
    return {
        val settings = settingsStore.getSettings()
        !settings.stationDataDeletionInProgress && settings.stationLocation != null
    }
}
