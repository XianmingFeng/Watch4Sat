package com.xianming.watch4sat.wear

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xianming.watch4sat.MainActivity

internal object MainActivityPendingIntentContract {
    const val activityIntentFlags: Int = Intent.FLAG_ACTIVITY_SINGLE_TOP
    val pendingIntentFlags: Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}

internal fun mainActivityPendingIntent(
    context: Context,
    requestCode: Int,
    configureIntent: Intent.() -> Unit
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply(configureIntent)
    intent.flags = MainActivityPendingIntentContract.activityIntentFlags
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        MainActivityPendingIntentContract.pendingIntentFlags
    )
}
