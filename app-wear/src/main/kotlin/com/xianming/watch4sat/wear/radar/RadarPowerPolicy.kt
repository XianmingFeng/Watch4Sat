package com.xianming.watch4sat.wear.radar

import android.hardware.SensorManager

enum class RadarUpdateMode {
    Interactive,
    AmbientOneHz
}

object RadarPowerPolicy {
    const val ambientRadarUpdateIntervalMillis: Long = 1_000L
    const val interactiveRadarUpdateIntervalMillis: Long = 250L
    const val ambientOrientationSensorDelayMicros: Int = 1_000_000

    fun shouldKeepScreenOn(
        settingEnabled: Boolean,
        hasFocusedPass: Boolean,
        isAmbient: Boolean
    ): Boolean {
        return settingEnabled && hasFocusedPass && !isAmbient
    }

    fun updateMode(isAmbient: Boolean): RadarUpdateMode {
        return if (isAmbient) RadarUpdateMode.AmbientOneHz else RadarUpdateMode.Interactive
    }

    fun radarUpdateIntervalMillis(updateMode: RadarUpdateMode): Long {
        return when (updateMode) {
            RadarUpdateMode.Interactive -> interactiveRadarUpdateIntervalMillis
            RadarUpdateMode.AmbientOneHz -> ambientRadarUpdateIntervalMillis
        }
    }

    fun orientationSensorDelayMicros(updateMode: RadarUpdateMode): Int {
        return when (updateMode) {
            RadarUpdateMode.Interactive -> SensorManager.SENSOR_DELAY_UI
            RadarUpdateMode.AmbientOneHz -> ambientOrientationSensorDelayMicros
        }
    }

    fun orientationPublishIntervalMillis(updateMode: RadarUpdateMode): Long? {
        return when (updateMode) {
            RadarUpdateMode.Interactive -> null
            RadarUpdateMode.AmbientOneHz -> ambientRadarUpdateIntervalMillis
        }
    }

    fun shouldListenToSensors(hasFocusedPass: Boolean, isAmbient: Boolean): Boolean {
        return hasFocusedPass
    }

    fun showTopTimeText(isAmbient: Boolean, chromeVisible: Boolean): Boolean {
        return isAmbient || chromeVisible
    }

    fun showSkyPlot(isAmbient: Boolean): Boolean = true

    fun showPointingReticle(isAmbient: Boolean): Boolean = true

    fun showInteractiveChrome(isAmbient: Boolean, chromeVisible: Boolean): Boolean {
        return !isAmbient && chromeVisible
    }
}
