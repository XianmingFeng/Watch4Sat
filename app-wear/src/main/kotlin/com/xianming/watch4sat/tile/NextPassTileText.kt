package com.xianming.watch4sat.tile

import android.content.Context
import com.xianming.watch4sat.R
import java.util.Locale

data class NextPassTileText(
    val noQthTitle: String,
    val setLocationFirst: String,
    val setQthAction: String,
    val noTleTitle: String,
    val orbitalDataMissing: String,
    val updateAction: String,
    val noSatellitesTitle: String,
    val selectSatellitesFirst: String,
    val selectAction: String,
    val noPassSoonTitle: String,
    val openForPassList: String,
    val openAction: String,
    val tileOfflineTitle: String,
    val openAppToRetry: String,
    val aosLabel: String,
    val losLabel: String,
    val maxLabel: String,
    val radarAction: String,
    val passesAction: String,
    val passEnded: String,
    val secondsShort: String,
    val minutesShort: String,
    val opens: String,
    val tleAgeUnknown: String,
    val tleFreshness: TleFreshnessText = TleFreshnessText.English
) {
    companion object {
        fun from(context: Context): NextPassTileText {
            return NextPassTileText(
                noQthTitle = context.getString(R.string.tile_no_qth_title),
                setLocationFirst = context.getString(R.string.tile_set_location_first),
                setQthAction = context.getString(R.string.tile_set_qth_action),
                noTleTitle = context.getString(R.string.tile_no_tle_title),
                orbitalDataMissing = context.getString(R.string.tile_orbital_data_missing),
                updateAction = context.getString(R.string.tile_update_action),
                noSatellitesTitle = context.getString(R.string.tile_no_satellites_title),
                selectSatellitesFirst = context.getString(
                    R.string.tile_select_satellites_first
                ),
                selectAction = context.getString(R.string.tile_select_action),
                noPassSoonTitle = context.getString(R.string.tile_no_pass_soon_title),
                openForPassList = context.getString(R.string.tile_open_for_pass_list),
                openAction = context.getString(R.string.tile_open_action),
                tileOfflineTitle = context.getString(R.string.tile_offline_title),
                openAppToRetry = context.getString(R.string.tile_open_app_to_retry),
                aosLabel = context.getString(R.string.tile_aos_label),
                losLabel = context.getString(R.string.tile_los_label),
                maxLabel = context.getString(R.string.tile_max_label),
                radarAction = context.getString(R.string.tile_radar_action),
                passesAction = context.getString(R.string.tile_passes_action),
                passEnded = context.getString(R.string.tile_pass_ended),
                secondsShort = context.getString(R.string.tile_seconds_short),
                minutesShort = context.getString(R.string.tile_minutes_short),
                opens = context.getString(R.string.tile_opens),
                tleAgeUnknown = context.getString(R.string.tile_tle_age_unknown),
                tleFreshness = TleFreshnessText.from(context)
            )
        }

        val English = NextPassTileText(
            noQthTitle = "No QTH",
            setLocationFirst = "Set location first",
            setQthAction = "Set QTH",
            noTleTitle = "No TLE",
            orbitalDataMissing = "Orbital data missing",
            updateAction = "Update",
            noSatellitesTitle = "No satellites",
            selectSatellitesFirst = "Select satellites first",
            selectAction = "Select",
            noPassSoonTitle = "No pass soon",
            openForPassList = "Open for pass list",
            openAction = "Open",
            tileOfflineTitle = "Tile offline",
            openAppToRetry = "Open app to retry",
            aosLabel = "AOS",
            losLabel = "LOS",
            maxLabel = "Max",
            radarAction = "Radar",
            passesAction = "Passes",
            passEnded = "Pass ended",
            secondsShort = "sec",
            minutesShort = "min",
            opens = "opens",
            tleAgeUnknown = "TLE age unknown"
        )
    }
}

data class TleFreshnessText(
    val checkDeviceTime: String,
    val deviceTimeChanged: String,
    val ageUnknown: String,
    val fresh: String,
    val stale: String,
    val veryStale: String,
    val tileHeaderDefault: String,
    val tileHeaderCheckTime: String,
    val tileHeaderAgeUnknown: String,
    val tileHeaderStale: String,
    val tileHeaderVeryStale: String,
    val detailClockSkew: String,
    val detailOldestEpochFormat: String,
    val detailDownloadedFormat: String,
    val relativeJustNow: String,
    val relativeMinutesFormat: String,
    val relativeHoursFormat: String,
    val relativeDaysFormat: String,
    val cachedOfflineGuidance: String,
    val currentGuidance: String,
    val refreshGuidanceFormat: String,
    val accuracyGuidanceFormat: String
) {
    fun format(template: String, vararg args: Any): String =
        String.format(Locale.US, template, *args)

    companion object {
        fun from(context: Context): TleFreshnessText = TleFreshnessText(
            checkDeviceTime = context.getString(R.string.tle_status_check_device_time),
            deviceTimeChanged = context.getString(R.string.tle_status_device_time_changed),
            ageUnknown = context.getString(R.string.tle_age_unknown),
            fresh = context.getString(R.string.tle_status_fresh),
            stale = context.getString(R.string.tle_status_stale),
            veryStale = context.getString(R.string.tle_status_very_stale),
            tileHeaderDefault = context.getString(R.string.tle_tile_header_default),
            tileHeaderCheckTime = context.getString(R.string.tle_tile_header_check_time),
            tileHeaderAgeUnknown = context.getString(R.string.tle_tile_header_age_unknown),
            tileHeaderStale = context.getString(R.string.tle_tile_header_stale),
            tileHeaderVeryStale = context.getString(R.string.tle_tile_header_very_stale),
            detailClockSkew = context.getString(R.string.tle_detail_clock_skew),
            detailOldestEpochFormat = context.getString(R.string.tle_detail_oldest_epoch),
            detailDownloadedFormat = context.getString(R.string.tle_detail_downloaded),
            relativeJustNow = context.getString(R.string.relative_age_now),
            relativeMinutesFormat = context.getString(R.string.relative_age_minutes),
            relativeHoursFormat = context.getString(R.string.relative_age_hours),
            relativeDaysFormat = context.getString(R.string.relative_age_days),
            cachedOfflineGuidance = context.getString(R.string.tle_guidance_cached_offline),
            currentGuidance = context.getString(R.string.tle_guidance_current),
            refreshGuidanceFormat = context.getString(R.string.tle_guidance_refresh),
            accuracyGuidanceFormat = context.getString(R.string.tle_guidance_accuracy)
        )

        val English = TleFreshnessText(
            checkDeviceTime = "Check device time",
            deviceTimeChanged = "Device time changed",
            ageUnknown = "unknown",
            fresh = "TLE fresh",
            stale = "TLE stale",
            veryStale = "TLE very stale",
            tileHeaderDefault = "Watch4Sat",
            tileHeaderCheckTime = "Watch4Sat · Check time",
            tileHeaderAgeUnknown = "Watch4Sat · TLE age?",
            tileHeaderStale = "Watch4Sat · TLE stale",
            tileHeaderVeryStale = "Watch4Sat · TLE 7d+",
            detailClockSkew = "Download time is ahead of the device clock",
            detailOldestEpochFormat = "Oldest epoch %1\$s",
            detailDownloadedFormat = "Downloaded %1\$s · Oldest epoch %2\$s",
            relativeJustNow = "just now",
            relativeMinutesFormat = "%1\$dm ago",
            relativeHoursFormat = "%1\$dh ago",
            relativeDaysFormat = "%1\$dd ago",
            cachedOfflineGuidance = "Cached predictions and alerts remain available offline.",
            currentGuidance = "Orbital data is current.",
            refreshGuidanceFormat = "Refresh recommended. %1\$s",
            accuracyGuidanceFormat =
                "Refresh recommended; accuracy may be reduced. %1\$s"
        )
    }
}
