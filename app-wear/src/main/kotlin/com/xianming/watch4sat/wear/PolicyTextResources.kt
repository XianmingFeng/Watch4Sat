package com.xianming.watch4sat.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xianming.watch4sat.R
import com.xianming.watch4sat.wear.state.AboutInfoLabel
import com.xianming.watch4sat.wear.state.AboutInfoRow
import com.xianming.watch4sat.wear.state.AboutInfoValue
import com.xianming.watch4sat.wear.state.DashboardDataStatus
import com.xianming.watch4sat.wear.state.DashboardHeroSubtitle
import com.xianming.watch4sat.wear.state.DataRefreshAttemptStatus
import com.xianming.watch4sat.wear.state.DeveloperOptionAction
import com.xianming.watch4sat.wear.state.DeveloperOptionFailure
import com.xianming.watch4sat.wear.state.PassAlertStatusLabel
import com.xianming.watch4sat.wear.state.PassAlertStatusRow
import com.xianming.watch4sat.wear.state.PassAlertStatusValue
import com.xianming.watch4sat.wear.state.QthDisplayUi
import com.xianming.watch4sat.wear.state.SatelliteFrequencyDetail
import com.xianming.watch4sat.wear.state.SatelliteOrbitDetailKind
import com.xianming.watch4sat.wear.state.SatelliteOrbitDetailUi
import com.xianming.watch4sat.wear.state.SatelliteOrbitDetailValue
import com.xianming.watch4sat.wear.state.SettingsSummary
import com.xianming.watch4sat.wear.state.TleFreshnessUiKind
import com.xianming.watch4sat.wear.state.TleFreshnessUiModel
import com.xianming.watch4sat.wear.state.TleRelativeAge

data class TleFreshnessUiText(
    val shortLabel: String,
    val statusLabel: String,
    val tileHeader: String,
    val detail: String,
    val guidance: String
)

data class PassAlertStatusText(
    val label: String,
    val value: String
)

data class AboutInfoText(
    val label: String,
    val value: String
)

data class SatelliteOrbitDetailText(
    val label: String,
    val value: String
)

data class QthDisplayText(
    val primaryText: String,
    val secondaryText: String,
    val contentDescription: String
)

@Composable
fun DashboardHeroSubtitle.resolveText(): String = when (this) {
    DashboardHeroSubtitle.NeedsStation ->
        stringResource(R.string.dashboard_hero_needs_station)
    is DashboardHeroSubtitle.Active -> {
        when {
            remainingMinutes <= 0L ->
                stringResource(R.string.dashboard_hero_active_now)
            remainingMinutes < 60L ->
                stringResource(
                    R.string.dashboard_hero_active_minutes,
                    remainingMinutes
                )
            else ->
                stringResource(
                    R.string.dashboard_hero_active_hours_minutes,
                    remainingMinutes / 60L,
                    remainingMinutes % 60L
                )
        }
    }
    is DashboardHeroSubtitle.Upcoming ->
        stringResource(
            R.string.dashboard_hero_upcoming,
            aosCountdown,
            maxElevation
        )
    is DashboardHeroSubtitle.Fallback -> text
}

@Composable
fun passAlertAdvanceText(minutes: Int): String =
    com.xianming.watch4sat.wear.state.PassAlertsSettingsPolicy.summary(minutes).resolveText()

@Composable
fun TleFreshnessUiModel.resolveText(): TleFreshnessUiText {
    val cachedGuidance = stringResource(R.string.tle_guidance_cached_offline)
    val retrieval = retrievalAge.resolveText()
    val epoch = oldestEpochAge.resolveText()
    return when (kind) {
        TleFreshnessUiKind.ClockSkew -> TleFreshnessUiText(
            shortLabel = stringResource(R.string.tle_status_check_device_time),
            statusLabel = stringResource(R.string.tle_status_device_time_changed),
            tileHeader = stringResource(R.string.tle_tile_header_check_time),
            detail = stringResource(R.string.tle_detail_clock_skew),
            guidance = cachedGuidance
        )
        TleFreshnessUiKind.Unknown -> TleFreshnessUiText(
            shortLabel = stringResource(R.string.tle_status_age_unknown),
            statusLabel = stringResource(R.string.tle_status_age_unknown),
            tileHeader = stringResource(R.string.tle_tile_header_age_unknown),
            detail = stringResource(R.string.tle_detail_oldest_epoch, epoch),
            guidance = stringResource(R.string.tle_guidance_refresh, cachedGuidance)
        )
        TleFreshnessUiKind.Fresh -> TleFreshnessUiText(
            shortLabel = stringResource(R.string.tle_status_fresh),
            statusLabel = stringResource(R.string.tle_status_fresh),
            tileHeader = stringResource(R.string.tle_tile_header_default),
            detail = stringResource(R.string.tle_detail_downloaded, retrieval, epoch),
            guidance = stringResource(R.string.tle_guidance_current)
        )
        TleFreshnessUiKind.Stale -> TleFreshnessUiText(
            shortLabel = stringResource(R.string.tle_status_stale),
            statusLabel = stringResource(R.string.tle_status_stale),
            tileHeader = stringResource(R.string.tle_tile_header_stale),
            detail = stringResource(R.string.tle_detail_downloaded, retrieval, epoch),
            guidance = stringResource(R.string.tle_guidance_refresh, cachedGuidance)
        )
        TleFreshnessUiKind.VeryStale -> TleFreshnessUiText(
            shortLabel = stringResource(R.string.tle_status_very_stale),
            statusLabel = stringResource(R.string.tle_status_very_stale),
            tileHeader = stringResource(R.string.tle_tile_header_very_stale),
            detail = stringResource(R.string.tle_detail_downloaded, retrieval, epoch),
            guidance = stringResource(R.string.tle_guidance_accuracy, cachedGuidance)
        )
    }
}

@Composable
private fun TleRelativeAge?.resolveText(): String = when (this) {
    null,
    TleRelativeAge.Unknown -> stringResource(R.string.tle_age_unknown)
    TleRelativeAge.JustNow -> stringResource(R.string.relative_age_now)
    is TleRelativeAge.Minutes -> stringResource(R.string.relative_age_minutes, value)
    is TleRelativeAge.Hours -> stringResource(R.string.relative_age_hours, value)
    is TleRelativeAge.Days -> stringResource(R.string.relative_age_days, value)
}

@Composable
fun DashboardDataStatus.resolveText(): String {
    val freshnessLabel = freshness.resolveText().shortLabel
    return if (isRefreshing) {
        stringResource(
            R.string.dashboard_data_summary_refreshing,
            satelliteCount,
            selectedCount,
            freshnessLabel
        )
    } else {
        stringResource(
            R.string.dashboard_data_summary,
            satelliteCount,
            selectedCount,
            freshnessLabel
        )
    }
}

@Composable
fun DataRefreshAttemptStatus.resolveFailureSuffix(): String = when (this) {
    DataRefreshAttemptStatus.Failed -> stringResource(R.string.error_suffix)
    DataRefreshAttemptStatus.NeverAttempted,
    DataRefreshAttemptStatus.Succeeded -> ""
}

@Composable
fun QthDisplayUi.resolveText(): QthDisplayText = when (this) {
    QthDisplayUi.Unset -> {
        val primary = stringResource(R.string.qth_display_unset_primary)
        QthDisplayText(
            primaryText = primary,
            secondaryText = stringResource(R.string.qth_display_unset_secondary),
            contentDescription = primary
        )
    }
    is QthDisplayUi.Saved -> QthDisplayText(
        primaryText = locator,
        secondaryText = stringResource(R.string.qth_display_saved_secondary, coordinates),
        contentDescription = stringResource(R.string.qth_display_saved_description, locator)
    )
}

@Composable
fun SettingsSummary.resolveText(): String = when (this) {
    SettingsSummary.Off -> stringResource(R.string.settings_summary_off)
    SettingsSummary.On -> stringResource(R.string.settings_summary_on)
    SettingsSummary.AtAos -> stringResource(R.string.settings_summary_at_aos)
    is SettingsSummary.Minutes -> stringResource(R.string.settings_summary_minutes, value)
    is SettingsSummary.Degrees -> stringResource(R.string.settings_summary_degrees, value)
    is SettingsSummary.DataFreshness -> {
        val enabledText = stringResource(
            if (enabled) R.string.settings_summary_on else R.string.settings_summary_off
        )
        freshness?.let {
            stringResource(
                R.string.settings_summary_state_with_detail,
                enabledText,
                it.resolveText().shortLabel
            )
        } ?: enabledText
    }
}

@Composable
fun PassAlertStatusRow.resolveText(): PassAlertStatusText {
    val labelText = stringResource(
        when (label) {
            PassAlertStatusLabel.Notifications -> R.string.pass_alert_status_notifications
            PassAlertStatusLabel.ExactAlarm -> R.string.pass_alert_status_exact_alarm
            PassAlertStatusLabel.NextAlert -> R.string.pass_alert_status_next_alert
        }
    )
    val valueText = when (val item = value) {
        PassAlertStatusValue.PermissionNeeded ->
            stringResource(R.string.pass_alert_status_permission_needed)
        PassAlertStatusValue.SystemOff -> stringResource(R.string.pass_alert_status_system_off)
        PassAlertStatusValue.ChannelOff -> stringResource(R.string.pass_alert_status_channel_off)
        PassAlertStatusValue.Ready -> stringResource(R.string.pass_alert_status_ready)
        PassAlertStatusValue.InexactFallback ->
            stringResource(R.string.pass_alert_status_inexact_fallback)
        PassAlertStatusValue.NotScheduled ->
            stringResource(R.string.pass_alert_status_not_scheduled)
        is PassAlertStatusValue.InMinutes ->
            stringResource(R.string.pass_alert_status_in_minutes, item.minutes)
    }
    return PassAlertStatusText(labelText, valueText)
}

@Composable
fun AboutInfoRow.resolveText(): AboutInfoText {
    val labelText = stringResource(
        when (label) {
            AboutInfoLabel.Version -> R.string.about_info_version
            AboutInfoLabel.Package -> R.string.about_info_package
            AboutInfoLabel.App -> R.string.about_info_app
            AboutInfoLabel.Data -> R.string.about_info_data
            AboutInfoLabel.Maps -> R.string.about_info_maps
            AboutInfoLabel.Location -> R.string.about_info_location
            AboutInfoLabel.License -> R.string.about_info_license
        }
    )
    val valueText = when (val item = value) {
        is AboutInfoValue.Version -> stringResource(
            R.string.about_info_version_value,
            item.versionName,
            item.versionCode
        )
        AboutInfoValue.Package -> stringResource(R.string.about_info_package_value)
        AboutInfoValue.App -> stringResource(R.string.about_info_app_value)
        AboutInfoValue.Data -> stringResource(R.string.about_info_data_value)
        AboutInfoValue.Maps -> stringResource(R.string.about_info_maps_value)
        AboutInfoValue.Location -> stringResource(R.string.about_info_location_value)
        AboutInfoValue.License -> stringResource(R.string.about_info_license_value)
    }
    return AboutInfoText(labelText, valueText)
}

@Composable
fun DeveloperOptionAction.resolveLabel(): String = stringResource(
    when (this) {
        DeveloperOptionAction.TriggerPassAlert ->
            R.string.developer_action_trigger_pass_alert
        DeveloperOptionAction.TriggerPassNotification ->
            R.string.developer_action_trigger_pass_notification
        DeveloperOptionAction.TriggerCalibrationHint ->
            R.string.developer_action_trigger_calibration_hint
        DeveloperOptionAction.DisableDeveloperOptions -> R.string.developer_action_disable
    }
)

@Composable
fun DeveloperOptionFailure.resolveTitle(): String = stringResource(
    when (this) {
        DeveloperOptionFailure.NoPass -> R.string.developer_failure_no_pass
        DeveloperOptionFailure.NotificationPermission ->
            R.string.developer_failure_notifications_off
    }
)

@Composable
fun SatelliteOrbitDetailUi.resolveText(): SatelliteOrbitDetailText {
    val labelText = stringResource(
        when (kind) {
            SatelliteOrbitDetailKind.Altitude -> R.string.satellite_detail_altitude
            SatelliteOrbitDetailKind.Period -> R.string.satellite_detail_period
            SatelliteOrbitDetailKind.MeanMotion -> R.string.satellite_detail_mean_motion
            SatelliteOrbitDetailKind.Inclination -> R.string.satellite_detail_inclination
            SatelliteOrbitDetailKind.Eccentricity -> R.string.satellite_detail_eccentricity
            SatelliteOrbitDetailKind.Raan -> R.string.satellite_detail_raan
            SatelliteOrbitDetailKind.ArgumentOfPerigee ->
                R.string.satellite_detail_argument_of_perigee
        }
    )
    val valueText = when (val item = value) {
        is SatelliteOrbitDetailValue.Altitude -> stringResource(
            R.string.satellite_detail_altitude_value,
            item.meanKm,
            item.perigeeKm,
            item.apogeeKm
        )
        is SatelliteOrbitDetailValue.PeriodMinutes ->
            stringResource(R.string.satellite_detail_period_value, item.value)
        is SatelliteOrbitDetailValue.MeanMotionRevolutionsPerDay ->
            stringResource(R.string.satellite_detail_mean_motion_value, item.value)
        is SatelliteOrbitDetailValue.Degrees ->
            stringResource(R.string.satellite_detail_degrees_value, item.value)
        is SatelliteOrbitDetailValue.Eccentricity ->
            stringResource(R.string.satellite_detail_eccentricity_value, item.value)
    }
    return SatelliteOrbitDetailText(labelText, valueText)
}

@Composable
fun SatelliteFrequencyDetail.resolveText(): String {
    val frequencyText = when {
        lowMhz != null && highMhz != null && lowMhz != highMhz ->
            stringResource(R.string.satellite_transmitter_frequency_range, lowMhz, highMhz)
        lowMhz != null ->
            stringResource(R.string.satellite_transmitter_frequency_single, lowMhz)
        highMhz != null ->
            stringResource(R.string.satellite_transmitter_frequency_single, highMhz)
        else -> return ""
    }
    return mode?.let {
        stringResource(R.string.satellite_transmitter_frequency_with_mode, frequencyText, it)
    } ?: frequencyText
}
