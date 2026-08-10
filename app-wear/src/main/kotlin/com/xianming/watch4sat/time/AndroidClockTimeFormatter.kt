package com.xianming.watch4sat.time

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.xianming.watch4sat.domain.time.ClockTimeFormatter
import java.util.Locale

object AndroidClockTimeFormatter {
    fun create(context: Context): ClockTimeFormatter {
        return ClockTimeFormatter(
            is24HourFormat = DateFormat.is24HourFormat(context),
            locale = Locale.US
        )
    }
}

@Composable
fun rememberAndroidClockTimeFormatter(): ClockTimeFormatter {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var formatter by remember(context, configuration) {
        mutableStateOf(AndroidClockTimeFormatter.create(context))
    }
    DisposableEffect(context, configuration) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                formatter = AndroidClockTimeFormatter.create(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24),
            false,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    return formatter
}
