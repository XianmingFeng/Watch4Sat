package com.xianming.watch4sat.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationDeliveryChannel(
    val id: String,
    val name: String,
    val importance: Int,
    val description: String? = null,
    val vibrationPattern: LongArray? = null
)

object NotificationDeliveryGate {
    fun canPost(context: Context, channel: NotificationDeliveryChannel): Boolean {
        if (!hasRuntimePermission(context)) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return ensureChannelAndCanPost(context, channel)
    }

    private fun hasRuntimePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannelAndCanPost(
        context: Context,
        channelSpec: NotificationDeliveryChannel
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return false
        val existing = notificationManager.getNotificationChannel(channelSpec.id)
        if (existing != null) {
            return existing.importance != NotificationManager.IMPORTANCE_NONE
        }
        val channel = NotificationChannel(
            channelSpec.id,
            channelSpec.name,
            channelSpec.importance
        ).apply {
            channelSpec.description?.let { description = it }
            channelSpec.vibrationPattern?.let { pattern ->
                enableVibration(true)
                setVibrationPattern(pattern)
            }
        }
        notificationManager.createNotificationChannel(channel)
        return true
    }
}
