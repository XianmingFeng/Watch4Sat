package com.xianming.watch4sat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import com.xianming.watch4sat.wear.Watch4SatApp
import com.xianming.watch4sat.wear.WearAmbientCompatibility

class MainActivity : ComponentActivity() {
    private val latestLaunch = mutableStateOf<ExternalLaunchEnvelope?>(null)
    private lateinit var launchEvents: ExternalLaunchEventSequence

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(EnglishLocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchEvents = ExternalLaunchEventSequence(
            startingEventId = savedInstanceState?.getLong(LastLaunchEventIdKey) ?: 0L
        )
        enableEdgeToEdge()
        publishLaunch(intent)
        val ambientRuntimeAvailable = WearAmbientCompatibility.isRuntimeAvailable(classLoader)
        setContent {
            if (ambientRuntimeAvailable) {
                val ambientModeManager = rememberAmbientModeManager()
                CompositionLocalProvider(LocalAmbientModeManager provides ambientModeManager) {
                    Watch4SatApp(
                        externalLaunch = latestLaunch.value,
                        onExternalLaunchConsumed = ::consumeLaunch,
                        onExitSetup = ::finish
                    )
                }
            } else {
                Watch4SatApp(
                    externalLaunch = latestLaunch.value,
                    onExternalLaunchConsumed = ::consumeLaunch,
                    onExitSetup = ::finish
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishLaunch(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(LastLaunchEventIdKey, launchEvents.lastPublishedEventId)
        super.onSaveInstanceState(outState)
    }

    private fun publishLaunch(intent: Intent) {
        latestLaunch.value = ExternalLaunchEnvelope(
            eventId = launchEvents.publish(),
            intent = intent
        )
    }

    private fun consumeLaunch(eventId: Long): Boolean {
        if (
            latestLaunch.value?.eventId != eventId ||
            !launchEvents.consumeIfLatest(eventId)
        ) return false
        latestLaunch.value = null
        setIntent(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            }
        )
        return true
    }

    private companion object {
        const val LastLaunchEventIdKey = "watch4sat_last_launch_event_id"
    }
}
