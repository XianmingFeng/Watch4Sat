package com.xianming.watch4sat.wear

import com.xianming.watch4sat.data.pass.PassSnapshot
import com.xianming.watch4sat.data.pass.PassSnapshotKey

enum class PassSnapshotRenewalReason {
    APP_START,
    PLANNING_INPUT_CHANGED,
    COVERAGE_THRESHOLD,
    CANDIDATE_EXHAUSTED,
    BOOT,
    TIME_CHANGED,
    TIMEZONE_CHANGED,
    PACKAGE_REPLACED,
    EXACT_ALARM_PERMISSION_CHANGED,
    SNAPSHOT_RECOVERED,
    ALARM_STATE_RECOVERED
}

object PassSnapshotRenewalPolicy {
    const val CoverageHours = 24
    const val RenewBeforeExpiryHours = 6
    const val UniqueWorkName = "watch4sat-pass-snapshot-renewal"
    const val ContinuationUniqueWorkName = "watch4sat-pass-snapshot-renewal-trigger"
    const val WorkTag = "watch4sat-pass-snapshot"
    const val ContinuationWorkTag = "watch4sat-pass-snapshot-trigger"

    fun needsRenewal(
        snapshot: PassSnapshot?,
        expectedKey: PassSnapshotKey,
        nowMillis: Long,
        force: Boolean
    ): Boolean {
        if (force || snapshot == null || snapshot.key != expectedKey) return true
        if (!snapshot.isComplete || snapshot.coverageStartMillis > nowMillis) return true
        return snapshot.coverageEndMillis - nowMillis < RenewBeforeExpiryMillis
    }

    fun continuationDelayMillis(coverageEndMillis: Long, nowMillis: Long): Long {
        val untilThreshold = coverageEndMillis - nowMillis - RenewBeforeExpiryMillis
        return if (untilThreshold >= 0L) {
            Math.addExact(untilThreshold, 1L)
        } else {
            0L
        }
    }

    fun requiresImmediateReplacement(reason: PassSnapshotRenewalReason): Boolean {
        return reason in setOf(
            PassSnapshotRenewalReason.PLANNING_INPUT_CHANGED,
            PassSnapshotRenewalReason.BOOT,
            PassSnapshotRenewalReason.TIME_CHANGED,
            PassSnapshotRenewalReason.TIMEZONE_CHANGED,
            PassSnapshotRenewalReason.PACKAGE_REPLACED,
            PassSnapshotRenewalReason.SNAPSHOT_RECOVERED,
            PassSnapshotRenewalReason.ALARM_STATE_RECOVERED
        )
    }

    fun reasonForReceiverAction(action: String?): PassSnapshotRenewalReason? {
        return when (action) {
            android.content.Intent.ACTION_BOOT_COMPLETED -> PassSnapshotRenewalReason.BOOT
            android.content.Intent.ACTION_TIME_CHANGED -> PassSnapshotRenewalReason.TIME_CHANGED
            android.content.Intent.ACTION_TIMEZONE_CHANGED ->
                PassSnapshotRenewalReason.TIMEZONE_CHANGED
            android.content.Intent.ACTION_MY_PACKAGE_REPLACED ->
                PassSnapshotRenewalReason.PACKAGE_REPLACED
            ExactAlarmPermissionChangedAction ->
                PassSnapshotRenewalReason.EXACT_ALARM_PERMISSION_CHANGED
            else -> null
        }
    }

    const val HourMillis = 60L * 60L * 1_000L
    const val RenewBeforeExpiryMillis = RenewBeforeExpiryHours * HourMillis
    const val CoverageMillis = CoverageHours * HourMillis
    const val TerminalRecoveryDelayMillis = HourMillis
    const val ExactAlarmPermissionChangedAction =
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
}
