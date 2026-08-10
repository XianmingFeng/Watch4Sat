package com.xianming.watch4sat.wear.state

object PassAlertStatusPolicy {
    fun rows(
        runtimePermissionGranted: Boolean,
        appNotificationsEnabled: Boolean,
        channelEnabled: Boolean,
        exactAlarmAvailable: Boolean,
        scheduledTriggerAtMillis: Long?,
        nowMillis: Long
    ): List<PassAlertStatusRow> {
        return listOf(
            notificationRow(runtimePermissionGranted, appNotificationsEnabled, channelEnabled),
            exactAlarmRow(exactAlarmAvailable),
            nextAlertRow(scheduledTriggerAtMillis, nowMillis)
        )
    }

    private fun notificationRow(
        runtimePermissionGranted: Boolean,
        appNotificationsEnabled: Boolean,
        channelEnabled: Boolean
    ): PassAlertStatusRow {
        val value = when {
            !runtimePermissionGranted -> PassAlertStatusValue.PermissionNeeded
            !appNotificationsEnabled -> PassAlertStatusValue.SystemOff
            !channelEnabled -> PassAlertStatusValue.ChannelOff
            else -> PassAlertStatusValue.Ready
        }
        return PassAlertStatusRow(
            label = PassAlertStatusLabel.Notifications,
            value = value,
            tone = if (value == PassAlertStatusValue.Ready) {
                PassAlertStatusTone.Ready
            } else {
                PassAlertStatusTone.Warning
            }
        )
    }

    private fun exactAlarmRow(exactAlarmAvailable: Boolean): PassAlertStatusRow {
        return if (exactAlarmAvailable) {
            PassAlertStatusRow(
                PassAlertStatusLabel.ExactAlarm,
                PassAlertStatusValue.Ready,
                PassAlertStatusTone.Ready
            )
        } else {
            PassAlertStatusRow(
                PassAlertStatusLabel.ExactAlarm,
                PassAlertStatusValue.InexactFallback,
                PassAlertStatusTone.Info
            )
        }
    }

    private fun nextAlertRow(
        scheduledTriggerAtMillis: Long?,
        nowMillis: Long
    ): PassAlertStatusRow {
        val trigger = scheduledTriggerAtMillis ?: return PassAlertStatusRow(
            label = PassAlertStatusLabel.NextAlert,
            value = PassAlertStatusValue.NotScheduled,
            tone = PassAlertStatusTone.Info
        )
        val minutes = ((trigger - nowMillis).coerceAtLeast(0L) + 59_999L) / 60_000L
        return PassAlertStatusRow(
            label = PassAlertStatusLabel.NextAlert,
            value = PassAlertStatusValue.InMinutes(minutes),
            tone = PassAlertStatusTone.Info
        )
    }
}

data class PassAlertStatusRow(
    val label: PassAlertStatusLabel,
    val value: PassAlertStatusValue,
    val tone: PassAlertStatusTone
)

enum class PassAlertStatusLabel {
    Notifications,
    ExactAlarm,
    NextAlert
}

sealed interface PassAlertStatusValue {
    data object PermissionNeeded : PassAlertStatusValue
    data object SystemOff : PassAlertStatusValue
    data object ChannelOff : PassAlertStatusValue
    data object Ready : PassAlertStatusValue
    data object InexactFallback : PassAlertStatusValue
    data object NotScheduled : PassAlertStatusValue
    data class InMinutes(val minutes: Long) : PassAlertStatusValue
}

enum class PassAlertStatusTone {
    Ready,
    Info,
    Warning
}
