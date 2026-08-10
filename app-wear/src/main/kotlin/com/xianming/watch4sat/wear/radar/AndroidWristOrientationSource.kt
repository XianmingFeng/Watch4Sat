package com.xianming.watch4sat.wear.radar

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xianming.watch4sat.data.settings.RadarWristSide

internal class AndroidWristOrientationSource(
    private val contentResolver: ContentResolver
) {
    fun readSystemWristSide(): RadarWristSide? {
        val storedValue = runCatching {
            Settings.Global.getString(contentResolver, GlobalSettingName)
        }.getOrNull()
        return RadarWristOrientationPolicy.systemWristSide(storedValue)
    }

    fun observe(observer: ContentObserver): Boolean {
        return runCatching {
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(GlobalSettingName),
                false,
                observer
            )
        }.isSuccess
    }

    fun stopObserving(observer: ContentObserver) {
        runCatching {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    companion object {
        const val GlobalSettingName = "wear_wrist_orientation_mode"
    }
}

@Composable
internal fun rememberSystemRadarWristSide(): State<RadarWristSide?> {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val source = remember(context) {
        AndroidWristOrientationSource(context.contentResolver)
    }
    val state = remember(source) {
        mutableStateOf(source.readSystemWristSide())
    }

    DisposableEffect(source, lifecycleOwner) {
        fun refresh() {
            state.value = source.readSystemWristSide()
        }

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refresh()
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        val observing = source.observe(contentObserver)
        refresh()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (observing) {
                source.stopObserving(contentObserver)
            }
        }
    }

    return state
}
