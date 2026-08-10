package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute

object RadarOngoingActivityPolicy {
    const val channelId: String = "radar_tracking"
    const val actionOpenRadar: String = PassStartNotificationPolicy.actionOpenOngoingActivityRadar
    const val usesNotificationCompat: Boolean = true
    const val usesWearOngoingActivity: Boolean = true
    const val notificationIsOngoing: Boolean = true
    const val autoCancel: Boolean = false
    const val notificationId: Int = 42_544

    fun decision(
        currentRoute: String?,
        focusedPassKey: String?,
        selectedPassKey: String?,
        nowMillis: Long,
        losMillis: Long?
    ): RadarOngoingActivityDecision {
        if (currentRoute != WatchRoute.Radar.route) return RadarOngoingActivityDecision.Cancel
        if (focusedPassKey == null || losMillis == null) return RadarOngoingActivityDecision.Cancel
        if (selectedPassKey == null || selectedPassKey != focusedPassKey) {
            return RadarOngoingActivityDecision.Cancel
        }
        return if (nowMillis < losMillis) {
            RadarOngoingActivityDecision.StartOrUpdate
        } else {
            RadarOngoingActivityDecision.Cancel
        }
    }
}

enum class RadarOngoingActivityDecision {
    StartOrUpdate,
    Cancel
}
