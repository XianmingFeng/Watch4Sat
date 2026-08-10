package com.xianming.watch4sat.wear

import androidx.annotation.StringRes
import com.xianming.watch4sat.R

enum class WatchRoute(val route: String, @get:StringRes val titleRes: Int) {
    FirstRunSetup("first_run_setup", R.string.route_setup),
    Dashboard("dashboard", R.string.app_name),
    Passes("passes", R.string.nav_passes),
    OrbitMap("orbit_map", R.string.route_orbit_map),
    OrbitMapDetail(OrbitMapRoutes.DetailPattern, R.string.orbit_detail_title),
    Qth("qth", R.string.nav_qth),
    Data("data", R.string.tle_title),
    Satellites("satellites", R.string.nav_satellites),
    Settings("settings", R.string.nav_settings),
    SettingsAppearance("settings/appearance", R.string.appearance_title),
    SettingsPassWindowAdjuster("settings/pass_window/adjuster", R.string.pass_window_title),
    SettingsPassAlerts("settings/pass_alerts", R.string.pass_alerts_title),
    SettingsDataFreshness("settings/data_freshness", R.string.data_freshness_title),
    SettingsMinimumElevation("settings/minimum_elevation", R.string.minimum_elevation_title),
    SettingsMapSource("settings/map_source", R.string.map_source_title),
    SettingsAbout("settings/about", R.string.about_title),
    SettingsPrivacy("settings/privacy", R.string.legal_privacy_policy),
    SettingsLegal("settings/legal", R.string.legal_notices),
    SettingsLegalDocument(
        com.xianming.watch4sat.wear.legal.LegalRoutes.DocumentPattern,
        R.string.route_legal_document
    ),
    SettingsDeveloperOptions("settings/developer_options", R.string.developer_options_title),
    QthPicker("qth/picker", R.string.qth_edit_locator_title),
    SatelliteDetail("satellites/detail", R.string.route_satellite_detail),
    SatellitesClearConfirm("satellites/clear_confirm", R.string.route_clear_selected),
    Radar("radar", R.string.nav_radar)
}
