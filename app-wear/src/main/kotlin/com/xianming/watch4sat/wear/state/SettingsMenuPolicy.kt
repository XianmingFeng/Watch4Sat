package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute

enum class SettingsMenuKey {
    Appearance,
    PassWindow,
    PassAlerts,
    DataFreshness,
    MinimumElevation,
    MapSource,
    About,
    DeveloperOptions,
    Location,
    Doppler
}

data class SettingsMenuItem(
    val key: SettingsMenuKey,
    val route: WatchRoute?
)

object SettingsMenuPolicy {
    private val baseTopLevelItems: List<SettingsMenuItem> = listOf(
        SettingsMenuItem(SettingsMenuKey.Appearance, WatchRoute.SettingsAppearance),
        SettingsMenuItem(SettingsMenuKey.PassWindow, WatchRoute.SettingsPassWindowAdjuster),
        SettingsMenuItem(SettingsMenuKey.PassAlerts, WatchRoute.SettingsPassAlerts),
        SettingsMenuItem(SettingsMenuKey.DataFreshness, WatchRoute.SettingsDataFreshness),
        SettingsMenuItem(SettingsMenuKey.MinimumElevation, WatchRoute.SettingsMinimumElevation),
        SettingsMenuItem(SettingsMenuKey.MapSource, WatchRoute.SettingsMapSource),
        SettingsMenuItem(SettingsMenuKey.About, WatchRoute.SettingsAbout)
    )
    private val developerOptionsItem = SettingsMenuItem(
        SettingsMenuKey.DeveloperOptions,
        WatchRoute.SettingsDeveloperOptions
    )

    val topLevelItems: List<SettingsMenuItem> = topLevelItems(developerOptionsEnabled = false)

    fun topLevelItems(developerOptionsEnabled: Boolean): List<SettingsMenuItem> {
        return if (developerOptionsEnabled) {
            baseTopLevelItems + developerOptionsItem
        } else {
            baseTopLevelItems
        }
    }

    val statusItems: List<SettingsMenuItem> = listOf(
        SettingsMenuItem(SettingsMenuKey.Location, null),
        SettingsMenuItem(SettingsMenuKey.Doppler, null)
    )
}
