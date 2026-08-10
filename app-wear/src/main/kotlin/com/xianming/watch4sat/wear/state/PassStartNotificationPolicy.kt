package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute
import com.xianming.watch4sat.domain.model.SatellitePass

object PassStartNotificationPolicy {
    const val channelId: String = "pass_start_urgent_v2"
    const val legacyChannelId: String = "pass_start"
    const val actionOpenPassNotificationRadar: String =
        "com.xianming.watch4sat.action.OPEN_PASS_RADAR"
    const val actionOpenOngoingActivityRadar: String =
        "com.xianming.watch4sat.action.OPEN_ONGOING_PASS_RADAR"
    const val actionPassStartAlarm: String = "com.xianming.watch4sat.action.PASS_START_ALARM"
    const val extraCatalogNumber: String = "extra_catalog_number"
    const val extraAosMillis: String = "extra_aos_millis"
    const val extraLosMillis: String = "extra_los_millis"
    const val usesNotificationCompat: Boolean = true
    const val usesContentPendingIntent: Boolean = true
    const val autoCancel: Boolean = true
    const val usesHighImportanceChannel: Boolean = true
    const val usesHighPriorityNotification: Boolean = true
    const val usesWearableExtender: Boolean = true
    const val usesFullScreenIntent: Boolean = false
    const val oppoPlatformExtraKey: String = "key_extra_platform"
    const val oppoPlatformExtraValue: String = "android"
    val vibrationPattern: LongArray = longArrayOf(0L, 120L, 80L, 120L)

    fun passKey(catalogNumber: Int, aosMillis: Long): String {
        return "$catalogNumber:$aosMillis"
    }

    fun shouldNotify(
        currentRoute: String?,
        activePassKey: String,
        radarFocusedPassKey: String?,
        setupActive: Boolean = false
    ): Boolean {
        return decision(currentRoute, activePassKey, radarFocusedPassKey, setupActive) ==
            PassStartNotificationDecision.Notify
    }

    fun decision(
        currentRoute: String?,
        activePassKey: String,
        radarFocusedPassKey: String?,
        setupActive: Boolean = false
    ): PassStartNotificationDecision {
        if (currentRoute == null) return PassStartNotificationDecision.WaitForKnownRoute
        if (setupActive) return PassStartNotificationDecision.SuppressAndMarkHandled
        if (currentRoute == WatchRoute.Dashboard.route) return PassStartNotificationDecision.SuppressAndMarkHandled
        if (currentRoute == WatchRoute.Radar.route && radarFocusedPassKey == activePassKey) {
            return PassStartNotificationDecision.SuppressAndMarkHandled
        }
        return PassStartNotificationDecision.Notify
    }

    fun shouldMarkHandled(
        decision: PassStartNotificationDecision,
        didPost: Boolean
    ): Boolean {
        return decision == PassStartNotificationDecision.SuppressAndMarkHandled ||
            (decision == PassStartNotificationDecision.Notify && didPost)
    }

    fun titleKindForPass(
        pass: SatellitePass,
        nowMillis: Long
    ): PassStartNotificationTitleKind {
        return if (nowMillis < pass.aosMillis) {
            PassStartNotificationTitleKind.SOON
        } else {
            PassStartNotificationTitleKind.STARTED
        }
    }
}

enum class PassStartNotificationTitleKind {
    SOON,
    STARTED
}

object PassStartReminderPolicy {
    const val inAppComponent: String = "FullScreenDialog"
    const val alertLayout: String = "SatelliteFirstEdgeCountdown"
    const val trackActionIconName: String = "TrackChanges"
    const val trackActionIconOnly: Boolean = true
    const val inAppAutoDismissMillis: Long = 5_000L
    const val usesEdgeButton: Boolean = true
    const val usesCountdownProgress: Boolean = true
    const val countdownProgressComponent: String = "WearMaterial3CircularProgressIndicator"
    const val countdownAnimationApi: String = "ComposeFrameClockElapsedTime"
    const val countdownProgressClockApi: String = "withFrameNanos"
    const val countdownProgressSource: String = "SingleElapsedTimeSnapshot"
    const val usesOfficialProgressIndicator: Boolean = true
    const val usesCustomCanvasArc: Boolean = false
    const val countdownProgressPlacement: String = "DialogFullScreenForegroundRingWithBottomGap"
    const val countdownStartAngle: Float = 130f
    const val countdownEndAngle: Float = 50f
    const val countdownFullSweepDegrees: Float = 280f
    const val countdownUsesThemeColors: Boolean = true
    const val countdownTrackAlpha: Float = 0.54f
    const val countdownDirectionMode: String = "OfficialProgressValuesNoMirror"
    const val countdownShowsSeconds: Boolean = true
    const val satelliteNameMaxLines: Int = 2
    const val autoDismissTracksPass: Boolean = false

    fun secondsRemaining(remainingMillis: Long): Int {
        return ((remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L)
            .toInt()
            .coerceIn(0, (inAppAutoDismissMillis / 1_000L).toInt())
    }

    fun countdownProgress(
        remainingMillis: Long,
        durationMillis: Long = inAppAutoDismissMillis
    ): Float {
        val safeDurationMillis = durationMillis.coerceAtLeast(1L)
        return (remainingMillis.toFloat() / safeDurationMillis.toFloat())
            .coerceIn(0f, 1f)
    }

    fun countdownSnapshot(
        elapsedMillis: Long,
        startWallClockMillis: Long,
        durationMillis: Long = inAppAutoDismissMillis
    ): PassStartCountdownSnapshot {
        val safeDurationMillis = durationMillis.coerceIn(0L, inAppAutoDismissMillis)
        val remainingMillis = (safeDurationMillis - elapsedMillis)
            .coerceIn(0L, safeDurationMillis)
        return PassStartCountdownSnapshot(
            remainingMillis = remainingMillis,
            progress = countdownProgress(
                remainingMillis = remainingMillis,
                durationMillis = safeDurationMillis
            ),
            secondsRemaining = secondsRemaining(remainingMillis),
            nowMillis = startWallClockMillis + elapsedMillis.coerceAtLeast(0L)
        )
    }

    fun decision(
        currentRoute: String?,
        activePassKey: String,
        radarFocusedPassKey: String?,
        isAppForeground: Boolean,
        setupActive: Boolean = false
    ): PassStartReminderDecision {
        val routeDecision = PassStartNotificationPolicy.decision(
            currentRoute = currentRoute,
            activePassKey = activePassKey,
            radarFocusedPassKey = radarFocusedPassKey,
            setupActive = setupActive
        )
        return when (routeDecision) {
            PassStartNotificationDecision.WaitForKnownRoute -> PassStartReminderDecision.WaitForKnownRoute
            PassStartNotificationDecision.SuppressAndMarkHandled -> PassStartReminderDecision.SuppressAndMarkHandled
            PassStartNotificationDecision.Notify -> {
                if (isAppForeground) {
                    PassStartReminderDecision.ShowInAppAlert
                } else {
                    PassStartReminderDecision.PostSystemNotification
                }
            }
        }
    }

    fun shouldMarkHandled(
        decision: PassStartReminderDecision,
        didPost: Boolean
    ): Boolean {
        return decision == PassStartReminderDecision.SuppressAndMarkHandled ||
            decision == PassStartReminderDecision.ShowInAppAlert ||
            (decision == PassStartReminderDecision.PostSystemNotification && didPost)
    }

    fun selectAlertPass(
        passes: List<SatellitePass>,
        nowMillis: Long,
        passAlertAdvanceMinutes: Int
    ): SatellitePass? {
        if (PassAlertAdvancePolicy.coerceMinutes(passAlertAdvanceMinutes) == PassAlertAdvancePolicy.offMinutes) {
            return null
        }
        return passes
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
            .firstOrNull { pass -> pass.isActiveAt(nowMillis) }
            ?: passes
                .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
                .firstOrNull { pass ->
                    val triggerAtMillis = PassAlertAdvancePolicy.triggerAtMillis(
                        aosMillis = pass.aosMillis,
                        nowMillis = nowMillis,
                        minutes = passAlertAdvanceMinutes
                    ) ?: return@firstOrNull false
                    nowMillis in triggerAtMillis until pass.aosMillis
                }
    }
}

data class PassStartCountdownSnapshot(
    val remainingMillis: Long,
    val progress: Float,
    val secondsRemaining: Int,
    val nowMillis: Long
)

enum class PassStartReminderDecision {
    WaitForKnownRoute,
    SuppressAndMarkHandled,
    ShowInAppAlert,
    PostSystemNotification
}

enum class PassStartNotificationDecision {
    WaitForKnownRoute,
    SuppressAndMarkHandled,
    Notify
}

data class PassStartNotificationRequest(
    val source: ExactPassLaunchSource,
    val exactPassIdentity: ExactPassIdentity?
) {
    val catalogNumber: Int? = exactPassIdentity?.catalogNumber
    val aosMillis: Long? = exactPassIdentity?.aosMillis
    val losMillis: Long? = exactPassIdentity?.losMillis
    val exactPassLaunchRequest: ExactPassLaunchRequest = ExactPassLaunchRequest(
        source = source,
        identity = exactPassIdentity
    )
    val passKey: String = exactPassIdentity?.let { identity ->
        "${identity.catalogNumber}:${identity.aosMillis}:${identity.losMillis}"
    } ?: "invalid:${source.name}"
}

object PassStartSchedulePolicy {
    const val catchUpGraceMillis: Long = 2 * 60_000L
    const val inexactCatchUpGraceMillis: Long = 30 * 60_000L
    const val declaresScheduleExactAlarmPermission: Boolean = true
    const val usesForegroundService: Boolean = false
    const val usesWorkManagerPolling: Boolean = false
    const val usesBroadcastPrediction: Boolean = false

    fun nextScheduleCandidate(
        passes: List<SatellitePass>,
        nowMillis: Long,
        handledPassKeys: Set<String>,
        allowCatchUp: Boolean = true,
        catchUpGraceMillis: Long = PassStartSchedulePolicy.catchUpGraceMillis,
        passAlertAdvanceMinutes: Int = 0
    ): PassStartScheduleCandidate? {
        if (PassAlertAdvancePolicy.coerceMinutes(passAlertAdvanceMinutes) == PassAlertAdvancePolicy.offMinutes) {
            return null
        }
        val sorted = passes
            .filter(SatellitePass::isPassStartCandidate)
            .sortedWith(compareBy<SatellitePass> { it.aosMillis }.thenBy { it.catalogNumber })
        if (allowCatchUp) {
            val active = sorted.firstOrNull { pass ->
                val key = PassStartNotificationPolicy.passKey(pass.catalogNumber, pass.aosMillis)
                key !in handledPassKeys &&
                    pass.isActiveAt(nowMillis) &&
                    nowMillis - pass.aosMillis <= catchUpGraceMillis
            }
            if (active != null) {
                return PassStartScheduleCandidate(
                    pass = active,
                    triggerAtMillis = nowMillis,
                    reason = PassStartScheduleReason.CatchUpActivePass
                )
            }
        }

        val future = sorted.firstOrNull { pass ->
            val key = PassStartNotificationPolicy.passKey(pass.catalogNumber, pass.aosMillis)
            key !in handledPassKeys && pass.aosMillis > nowMillis && pass.losMillis > nowMillis
        } ?: return null
        return PassStartScheduleCandidate(
            pass = future,
            triggerAtMillis = PassAlertAdvancePolicy.triggerAtMillis(
                aosMillis = future.aosMillis,
                nowMillis = nowMillis,
                minutes = passAlertAdvanceMinutes
            ) ?: return null,
            reason = if (PassAlertAdvancePolicy.coerceMinutes(passAlertAdvanceMinutes) > 0) {
                PassStartScheduleReason.AdvanceWarning
            } else {
                PassStartScheduleReason.FutureAos
            }
        )
    }

    fun alarmCandidateForRequest(
        passes: List<SatellitePass>,
        requestedPassKey: String?,
        nowMillis: Long,
        handledPassKeys: Set<String>,
        catchUpGraceMillis: Long = inexactCatchUpGraceMillis,
        passAlertAdvanceMinutes: Int = 0
    ): PassStartScheduleCandidate? {
        if (requestedPassKey == null || requestedPassKey in handledPassKeys) return null
        val requestedPass = passes.firstOrNull { pass ->
            pass.isPassStartCandidate &&
                PassStartNotificationPolicy.passKey(pass.catalogNumber, pass.aosMillis) == requestedPassKey
        } ?: return null
        val isActiveDeliverable = requestedPass.isActiveAt(nowMillis) &&
            nowMillis - requestedPass.aosMillis <= catchUpGraceMillis
        val advanceMinutes = PassAlertAdvancePolicy.coerceMinutes(passAlertAdvanceMinutes)
        val advanceTriggerMillis = if (advanceMinutes > 0) {
            requestedPass.aosMillis - advanceMinutes * 60_000L
        } else {
            requestedPass.aosMillis
        }
        val isAdvanceDeliverable = advanceMinutes > 0 &&
            nowMillis in advanceTriggerMillis until requestedPass.aosMillis
        if (!isActiveDeliverable && !isAdvanceDeliverable) return null
        return PassStartScheduleCandidate(
            pass = requestedPass,
            triggerAtMillis = nowMillis,
            reason = if (isAdvanceDeliverable) {
                PassStartScheduleReason.AdvanceWarning
            } else {
                PassStartScheduleReason.CatchUpActivePass
            }
        )
    }

    fun receiverDispatch(
        isAppForeground: Boolean,
        notificationsAllowed: Boolean,
        candidateValid: Boolean,
        setupCompleted: Boolean = true
    ): PassStartAlarmDispatch {
        if (!candidateValid) return PassStartAlarmDispatch.RescheduleOnly
        if (!setupCompleted) return PassStartAlarmDispatch.RescheduleOnly
        if (isAppForeground) return PassStartAlarmDispatch.SkipForegroundAndReschedule
        if (!notificationsAllowed) return PassStartAlarmDispatch.SkipNotificationPermissionAndReschedule
        return PassStartAlarmDispatch.PostNotificationAndReschedule
    }

    fun shouldPostForReceiverAction(
        isAlarmAction: Boolean,
        requestedPassKey: String?,
        candidate: PassStartScheduleCandidate?
    ): Boolean {
        if (!isAlarmAction || candidate == null) return false
        return requestedPassKey == candidate.passKey
    }

    fun shouldMarkHandledAfterReceiverSkip(
        dispatch: PassStartAlarmDispatch,
        isAlarmAction: Boolean,
        candidateValid: Boolean
    ): Boolean {
        if (!isAlarmAction || !candidateValid) return false
        return dispatch == PassStartAlarmDispatch.SkipNotificationPermissionAndReschedule
    }

    fun alarmMode(canScheduleExactAlarms: Boolean): PassStartAlarmMode {
        return if (canScheduleExactAlarms) {
            PassStartAlarmMode.ExactAllowWhileIdle
        } else {
            PassStartAlarmMode.InexactAllowWhileIdle
        }
    }

    fun shouldUpdateAlarmFromUiState(passPlanningStatusName: String): Boolean {
        return passPlanningStatusName in setOf(
            "NeedsQth",
            "NoSatellites",
            "FromSnapshot",
            "Ready",
            "Failed"
        )
    }
}

data class PassStartScheduleCandidate(
    val pass: SatellitePass,
    val triggerAtMillis: Long,
    val reason: PassStartScheduleReason
) {
    val passKey: String = PassStartNotificationPolicy.passKey(pass.catalogNumber, pass.aosMillis)
}

enum class PassStartScheduleReason {
    FutureAos,
    CatchUpActivePass,
    AdvanceWarning
}

enum class PassStartAlarmDispatch {
    PostNotificationAndReschedule,
    SkipForegroundAndReschedule,
    SkipNotificationPermissionAndReschedule,
    RescheduleOnly
}

enum class PassStartAlarmMode(
    val methodName: String
) {
    ExactAllowWhileIdle("AlarmManager.setExactAndAllowWhileIdle"),
    InexactAllowWhileIdle("AlarmManager.setAndAllowWhileIdle")
}
