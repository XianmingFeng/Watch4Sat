package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.data.settings.MapTileMode
import com.xianming.watch4sat.data.settings.RadarForwardAxis
import com.xianming.watch4sat.data.settings.RadarWristSide

enum class SettingsSelectionStyle {
    SecondaryRadioList,
    SecondaryAdjusterPage,
    SwitchPage,
    SwitchAndRadioList
}

sealed interface SettingsSummary {
    data object Off : SettingsSummary
    data object On : SettingsSummary
    data object AtAos : SettingsSummary
    data class Minutes(val value: Int) : SettingsSummary
    data class Degrees(val value: Int) : SettingsSummary
    data class DataFreshness(
        val enabled: Boolean,
        val freshness: TleFreshnessUiModel?
    ) : SettingsSummary
}

object MapSourceSelectionPolicy {
    val style: SettingsSelectionStyle = SettingsSelectionStyle.SecondaryRadioList
    const val returnsToSettingsAfterSelection: Boolean = true
    const val showSecondaryLabel: Boolean = false
    val options: List<MapTileMode> = listOf(
        MapTileMode.AUTO,
        MapTileMode.OSM_ONLY,
        MapTileMode.OFFLINE_WORLD
    )
}

object AppearanceSelectionPolicy {
    val style: SettingsSelectionStyle = SettingsSelectionStyle.SecondaryRadioList
    const val appliesImmediately: Boolean = true
    const val showSecondaryLabel: Boolean = false
}

object RadarKeepScreenOnPolicy {
    const val defaultEnabled: Boolean = false
    const val component: String = "SwitchButton"
}

object RadarForwardAxisSettingsPolicy {
    val defaultAxis: RadarForwardAxis = RadarForwardAxis.SCREEN_TOP
    const val component: String = "SwitchButton"

    fun isChecked(axis: RadarForwardAxis): Boolean {
        return axis == RadarForwardAxis.TOWARD_HAND
    }

    fun axisForChecked(checked: Boolean): RadarForwardAxis {
        return if (checked) {
            RadarForwardAxis.TOWARD_HAND
        } else {
            RadarForwardAxis.SCREEN_TOP
        }
    }
}

object RadarFallbackWristSideSettingsPolicy {
    val defaultSide: RadarWristSide = RadarWristSide.LEFT
    const val component: String = "SwitchButton"

    fun isChecked(side: RadarWristSide): Boolean {
        return side == RadarWristSide.RIGHT
    }

    fun sideForChecked(checked: Boolean): RadarWristSide {
        return if (checked) RadarWristSide.RIGHT else RadarWristSide.LEFT
    }
}

object PassAlertsSettingsPolicy {
    val style: SettingsSelectionStyle = SettingsSelectionStyle.SecondaryRadioList
    const val returnsToSettingsAfterSelection: Boolean = true
    val options: List<Int> = PassAlertAdvancePolicy.options.map { it.minutes }

    fun summary(minutes: Int): SettingsSummary {
        return when (val coerced = PassAlertAdvancePolicy.coerceMinutes(minutes)) {
            PassAlertAdvancePolicy.offMinutes -> SettingsSummary.Off
            0 -> SettingsSummary.AtAos
            else -> SettingsSummary.Minutes(coerced)
        }
    }
}

object DataFreshnessSettingsPolicy {
    val style: SettingsSelectionStyle = SettingsSelectionStyle.SwitchPage
    const val component: String = "SwitchButton"

    fun summary(
        enabled: Boolean,
        freshness: TleFreshnessUiModel? = null
    ): SettingsSummary = SettingsSummary.DataFreshness(enabled, freshness)
}

object MinimumElevationSettingsPolicy {
    val style: SettingsSelectionStyle = SettingsSelectionStyle.SwitchAndRadioList
    const val component: String = "SwitchButton"
    val thresholds: List<Int> = MinimumElevationPolicy.allowedThresholdDegrees

    fun summary(enabled: Boolean, thresholdDegrees: Int): SettingsSummary {
        return if (enabled) {
            SettingsSummary.Degrees(
                MinimumElevationPolicy.coerceThresholdDegrees(thresholdDegrees)
            )
        } else {
            SettingsSummary.Off
        }
    }
}
