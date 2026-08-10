package com.xianming.watch4sat.wear

import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xianming.watch4sat.R
import com.xianming.watch4sat.domain.model.PassCardUi
import com.xianming.watch4sat.domain.model.SatellitePass
import com.xianming.watch4sat.wear.state.PassStartNotificationPolicy
import com.xianming.watch4sat.wear.state.PassStartNotificationRequest
import com.xianming.watch4sat.wear.state.PassStartNotificationTitleKind
import com.xianming.watch4sat.wear.state.ExactPassIdentity
import com.xianming.watch4sat.wear.state.ExactPassLaunchSource

class PassStartNotifier(
    private val context: Context
) {
    fun notify(pass: SatellitePass, card: PassCardUi): Boolean {
        if (!canPostNotifications(context)) return false
        val request = PassStartNotificationRequest(
            source = ExactPassLaunchSource.PassNotification,
            exactPassIdentity = ExactPassIdentity(
                catalogNumber = pass.catalogNumber,
                aosMillis = pass.aosMillis,
                losMillis = pass.losMillis
            )
        )
        val pendingIntent = passStartNotificationPendingIntent(context, request)
        val title = when (
            PassStartNotificationPolicy.titleKindForPass(
                pass = pass,
                nowMillis = System.currentTimeMillis()
            )
        ) {
            PassStartNotificationTitleKind.SOON ->
                context.getString(R.string.pass_notification_title_soon)
            PassStartNotificationTitleKind.STARTED ->
                context.getString(R.string.pass_notification_title_started)
        }
        val notification = NotificationCompat.Builder(context, PassStartNotificationPolicy.channelId)
            .setSmallIcon(R.drawable.ic_notification_satellite)
            .setContentTitle(title)
            .setContentText(
                context.getString(R.string.pass_notification_content, pass.satelliteName)
            )
            .setSubText(card.aosCountdown)
            .setContentIntent(pendingIntent)
            .setAutoCancel(PassStartNotificationPolicy.autoCancel)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .extend(
                NotificationCompat.WearableExtender()
                    .setDismissalId(request.passKey)
                    .setHintContentIntentLaunchesActivity(true)
            )
            .build()
            .apply {
                extras.putString(
                    PassStartNotificationPolicy.oppoPlatformExtraKey,
                    PassStartNotificationPolicy.oppoPlatformExtraValue
                )
            }

        return try {
            NotificationManagerCompat.from(context).notify(notificationIdFor(request), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun notificationIdFor(request: PassStartNotificationRequest): Int {
        return request.passKey.hashCode()
    }

    companion object {
        fun canPostNotifications(context: Context): Boolean {
            return NotificationDeliveryGate.canPost(
                context = context,
                channel = NotificationDeliveryChannel(
                    id = PassStartNotificationPolicy.channelId,
                    name = context.getString(R.string.pass_alert_channel_name),
                    importance = NotificationManager.IMPORTANCE_HIGH,
                    description = context.getString(
                        R.string.pass_alert_channel_description
                    ),
                    vibrationPattern = PassStartNotificationPolicy.vibrationPattern
                )
            )
        }
    }
}

internal fun passStartNotificationPendingIntent(
    context: Context,
    request: PassStartNotificationRequest
): PendingIntent {
    val identity = requireNotNull(request.exactPassIdentity)
    return mainActivityPendingIntent(
        context = context,
        requestCode = request.passKey.hashCode()
    ) {
        action = PassStartNotificationPolicy.actionOpenPassNotificationRadar
        putExtra(PassStartNotificationPolicy.extraCatalogNumber, identity.catalogNumber)
        putExtra(PassStartNotificationPolicy.extraAosMillis, identity.aosMillis)
        putExtra(PassStartNotificationPolicy.extraLosMillis, identity.losMillis)
    }
}

fun passStartNotificationRequestFrom(intent: Intent?): PassStartNotificationRequest? {
    if (intent == null) return null
    return passStartNotificationRequestFromValues(
        action = intent.action,
        catalogNumber = intent.getIntExtra(PassStartNotificationPolicy.extraCatalogNumber, -1),
        aosMillis = intent.getLongExtra(PassStartNotificationPolicy.extraAosMillis, Long.MIN_VALUE),
        losMillis = intent.getLongExtra(PassStartNotificationPolicy.extraLosMillis, Long.MIN_VALUE)
    )
}

fun passStartNotificationRequestFromValues(
    action: String?,
    catalogNumber: Int?,
    aosMillis: Long?,
    losMillis: Long?
): PassStartNotificationRequest? {
    val source = when (action) {
        PassStartNotificationPolicy.actionOpenPassNotificationRadar ->
            ExactPassLaunchSource.PassNotification
        PassStartNotificationPolicy.actionOpenOngoingActivityRadar ->
            ExactPassLaunchSource.OngoingActivity
        else -> return null
    }
    return PassStartNotificationRequest(
        source = source,
        exactPassIdentity = ExactPassIdentity.from(
            catalogNumber = catalogNumber,
            aosMillis = aosMillis,
            losMillis = losMillis
        )
    )
}
