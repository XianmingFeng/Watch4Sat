package com.xianming.watch4sat.wear

import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.state.RadarOngoingActivityPolicy
import kotlin.math.ceil

class RadarOngoingActivityController(
    private val context: Context
) {
    fun startOrUpdate(pass: SatellitePass, nowMillis: Long): Boolean {
        if (!NotificationDeliveryGate.canPost(
                context = context,
                channel = NotificationDeliveryChannel(
                    id = RadarOngoingActivityPolicy.channelId,
                    name = context.getString(R.string.radar_tracking_channel_name),
                    importance = NotificationManager.IMPORTANCE_LOW,
                    description = context.getString(
                        R.string.radar_tracking_channel_description
                    )
                )
            )
        ) return false
        val pendingIntent = radarOngoingActivityPendingIntent(context, pass)
        val minutesRemaining = ceil(
            (pass.losMillis - nowMillis).coerceAtLeast(0L) / 60_000.0
        ).toInt()
        val timeStatus = context.resources.getQuantityString(
            R.plurals.duration_minutes_short,
            minutesRemaining,
            minutesRemaining
        )
        val title = context.getString(R.string.radar_tracking_title)
        val contentText = context.getString(
            R.string.radar_tracking_content,
            pass.satelliteName,
            timeStatus
        )
        val notificationBuilder = NotificationCompat.Builder(context, RadarOngoingActivityPolicy.channelId)
            .setSmallIcon(R.drawable.ic_notification_satellite)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(RadarOngoingActivityPolicy.autoCancel)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val status = Status.Builder()
            .addTemplate("#pass#")
            .addPart("pass", Status.TextPart(contentText))
            .build()
        val ongoingActivity = OngoingActivity.Builder(
            context,
            RadarOngoingActivityPolicy.notificationId,
            notificationBuilder
        )
            .setStaticIcon(R.drawable.ic_notification_satellite)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setTitle(title)
            .setContentDescription(contentText)
            .build()
        ongoingActivity.apply(context)

        return try {
            NotificationManagerCompat.from(context)
                .notify(RadarOngoingActivityPolicy.notificationId, notificationBuilder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(RadarOngoingActivityPolicy.notificationId)
    }
}

internal fun radarOngoingActivityPendingIntent(
    context: Context,
    pass: SatellitePass
): PendingIntent {
    return mainActivityPendingIntent(
        context = context,
        requestCode = RadarOngoingActivityPolicy.notificationId
    ) {
        action = RadarOngoingActivityPolicy.actionOpenRadar
        putExtra(PassStartNotificationPolicy.extraCatalogNumber, pass.catalogNumber)
        putExtra(PassStartNotificationPolicy.extraAosMillis, pass.aosMillis)
        putExtra(PassStartNotificationPolicy.extraLosMillis, pass.losMillis)
    }
}
