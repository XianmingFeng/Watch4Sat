package com.xianming.watch4sat.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xianming.watch4sat.EnglishLocaleContext
import com.xianming.watch4sat.data.Watch4SatDataLayer
import com.xianming.watch4sat.domain.pass.PassCardMapper
import com.xianming.watch4sat.time.AndroidClockTimeFormatter
import com.xianming.watch4sat.wear.state.PassStartAlarmDispatch
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.state.PassStartSchedulePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PassStartAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val englishContext = EnglishLocaleContext.wrap(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(ReceiverTimeoutMillis) {
                    handle(englishContext, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val dependencies = Watch4SatDataLayer.createLocalOnly(context)
        val stateStore = PassStartAlarmStateStore(context)
        val scheduler = PassStartAlarmScheduler(
            context,
            stateStore,
            stationDataScheduleGuard(context)
        )
        val state = stateStore.read()
        val alarmStateRecovered = stateStore.recoveryRequired()
        val settings = dependencies.settingsStore.getSettings()
        val resolver = PassStartBackgroundResolver(
            repository = dependencies.satelliteDataRepository,
            settingsProvider = { settings },
            snapshotCache = dependencies.passSnapshotCache
        )
        val nowMillis = System.currentTimeMillis()
        val isAlarmAction = intent.action == PassStartNotificationPolicy.actionPassStartAlarm
        val request = alarmRequestFrom(intent)
        val resolution = resolver.resolve(nowMillis)
        val snapshotRecovered = dependencies.passSnapshotCache.recoveryRequired()
        val candidate = if (isAlarmAction) {
            PassStartSchedulePolicy.alarmCandidateForRequest(
                passes = resolution.passes,
                requestedPassKey = request?.passKey,
                nowMillis = nowMillis,
                handledPassKeys = state.handledPassKeys,
                catchUpGraceMillis = PassStartSchedulePolicy.inexactCatchUpGraceMillis,
                passAlertAdvanceMinutes = settings.passAlertAdvanceMinutes
            )
        } else {
            null
        }
        val candidateMatchesAlarm = PassStartSchedulePolicy.shouldPostForReceiverAction(
            isAlarmAction = isAlarmAction,
            requestedPassKey = request?.passKey,
            candidate = candidate
        )
        val dispatch = PassStartSchedulePolicy.receiverDispatch(
            isAppForeground = PassStartAppVisibility.isForeground(),
            notificationsAllowed = PassStartNotifier.canPostNotifications(context),
            candidateValid = candidateMatchesAlarm,
            setupCompleted = settings.setupCompleted
        )

        if (dispatch == PassStartAlarmDispatch.PostNotificationAndReschedule && candidate != null) {
            val card = PassCardMapper.map(
                pass = candidate.pass,
                transmitters = emptyList(),
                nowMillis = nowMillis,
                textFormatter = AndroidPassCardTextFormatter(context),
                clockTimeFormatter = AndroidClockTimeFormatter.create(context)
            )
            if (PassStartNotifier(context).notify(candidate.pass, card)) {
                stateStore.markHandled(candidate.passKey)
            }
        } else if (dispatch == PassStartAlarmDispatch.SkipForegroundAndReschedule && candidate != null) {
            stateStore.setPendingForegroundAlarm(candidate)
            PassStartForegroundAlarmEvents.emit()
        } else if (
            PassStartSchedulePolicy.shouldMarkHandledAfterReceiverSkip(
                dispatch = dispatch,
                isAlarmAction = isAlarmAction,
                candidateValid = candidateMatchesAlarm
            ) && candidate != null
        ) {
            stateStore.markHandled(candidate.passKey)
        }

        val updatedAlarmState = stateStore.read()
        val updatedHandled = updatedAlarmState.handledPassKeys +
            if (dispatch == PassStartAlarmDispatch.SkipForegroundAndReschedule && candidate != null) {
                setOf(candidate.passKey)
            } else {
                emptySet()
            }
        val nextCandidate = if (settings.setupCompleted) {
            PassStartSchedulePolicy.nextScheduleCandidate(
                passes = resolution.passes,
                nowMillis = nowMillis,
                handledPassKeys = updatedHandled,
                allowCatchUp = false,
                passAlertAdvanceMinutes = settings.passAlertAdvanceMinutes
            )
        } else {
            null
        }
        if (isAlarmAction) {
            scheduler.schedule(nextCandidate, updatedAlarmState)
        } else {
            scheduler.forceReschedule(nextCandidate)
        }
        val eventReason = PassSnapshotRenewalPolicy.reasonForReceiverAction(intent.action)
        when {
            snapshotRecovered -> PassSnapshotRenewalEnqueuer.enqueueImmediate(
                context,
                PassSnapshotRenewalReason.SNAPSHOT_RECOVERED
            )
            alarmStateRecovered -> PassSnapshotRenewalEnqueuer.enqueueImmediate(
                context,
                PassSnapshotRenewalReason.ALARM_STATE_RECOVERED
            )
            eventReason != null -> PassSnapshotRenewalEnqueuer.enqueueImmediate(context, eventReason)
            resolution.renewalRequired -> PassSnapshotRenewalEnqueuer.enqueueImmediate(
                context,
                PassSnapshotRenewalReason.COVERAGE_THRESHOLD
            )
            nextCandidate == null && resolution.coverageEndMillis != null ->
                PassSnapshotRenewalEnqueuer.enqueueContinuation(
                    context = context,
                    coverageEndMillis = resolution.coverageEndMillis,
                    nowMillis = nowMillis
                )
        }
    }

    private fun alarmRequestFrom(intent: Intent): PassStartAlarmRequest? {
        val catalogNumber = intent.getIntExtra(PassStartNotificationPolicy.extraCatalogNumber, -1)
        val aosMillis = intent.getLongExtra(PassStartNotificationPolicy.extraAosMillis, Long.MIN_VALUE)
        if (catalogNumber <= 0 || aosMillis == Long.MIN_VALUE) return null
        return PassStartAlarmRequest(catalogNumber = catalogNumber, aosMillis = aosMillis)
    }

}

private data class PassStartAlarmRequest(
    val catalogNumber: Int,
    val aosMillis: Long
) {
    val passKey: String = PassStartNotificationPolicy.passKey(catalogNumber, aosMillis)
}

private const val ReceiverTimeoutMillis: Long = 8_000L
