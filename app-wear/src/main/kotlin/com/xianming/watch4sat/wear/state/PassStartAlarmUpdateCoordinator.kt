package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.domain.model.SatellitePass

data class PassStartAlarmScheduleKey(
    val passKey: String,
    val triggerAtMillis: Long
) {
    companion object {
        fun from(passKey: String?, triggerAtMillis: Long?): PassStartAlarmScheduleKey? {
            if (passKey == null || triggerAtMillis == null) return null
            return PassStartAlarmScheduleKey(passKey, triggerAtMillis)
        }
    }
}

val PassStartScheduleCandidate.scheduleKey: PassStartAlarmScheduleKey
    get() = PassStartAlarmScheduleKey(passKey, triggerAtMillis)

sealed interface PassStartAlarmUpdate {
    val scheduleKey: PassStartAlarmScheduleKey?

    data class Schedule(
        val candidate: PassStartScheduleCandidate
    ) : PassStartAlarmUpdate {
        override val scheduleKey: PassStartAlarmScheduleKey = candidate.scheduleKey
    }

    data object Cancel : PassStartAlarmUpdate {
        override val scheduleKey: PassStartAlarmScheduleKey? = null
    }
}

class PassStartAlarmUpdateCoordinator {
    private var hasAppliedRequest: Boolean = false
    private var lastRequestedKey: PassStartAlarmScheduleKey? = null

    @Synchronized
    fun update(
        candidate: PassStartScheduleCandidate?,
        force: Boolean = false
    ): PassStartAlarmUpdate? {
        val requestedKey = candidate?.scheduleKey
        if (!force && hasAppliedRequest && requestedKey == lastRequestedKey) return null
        hasAppliedRequest = true
        lastRequestedKey = requestedKey
        return candidate?.let(PassStartAlarmUpdate::Schedule) ?: PassStartAlarmUpdate.Cancel
    }
}

fun PassStartAlarmUpdate?.shouldApplyAgainst(
    persistedScheduleKey: PassStartAlarmScheduleKey?,
    force: Boolean
): Boolean {
    if (this == null) return false
    if (force) return true
    return this is PassStartAlarmUpdate.Cancel || scheduleKey != persistedScheduleKey
}

data class PassStartAlarmUiScheduleKey(
    val passes: List<PassStartAlarmPassKey>,
    val passAlertAdvanceMinutes: Int,
    val setupActive: Boolean,
    val shouldUpdate: Boolean
) {
    companion object {
        fun from(
            passes: List<SatellitePass>,
            passAlertAdvanceMinutes: Int,
            setupActive: Boolean,
            shouldUpdate: Boolean
        ): PassStartAlarmUiScheduleKey {
            val passKeys = passes
                .map(PassStartAlarmPassKey::from)
                .sortedWith(compareBy(PassStartAlarmPassKey::aosMillis).thenBy(PassStartAlarmPassKey::catalogNumber))
            return PassStartAlarmUiScheduleKey(
                passes = passKeys,
                passAlertAdvanceMinutes = passAlertAdvanceMinutes,
                setupActive = setupActive,
                shouldUpdate = shouldUpdate
            )
        }
    }
}

data class PassStartAlarmPassKey(
    val catalogNumber: Int,
    val aosMillis: Long,
    val losMillis: Long
) {
    val passKey: String = PassStartNotificationPolicy.passKey(catalogNumber, aosMillis)

    companion object {
        fun from(pass: SatellitePass): PassStartAlarmPassKey {
            return PassStartAlarmPassKey(
                catalogNumber = pass.catalogNumber,
                aosMillis = pass.aosMillis,
                losMillis = pass.losMillis
            )
        }
    }
}
