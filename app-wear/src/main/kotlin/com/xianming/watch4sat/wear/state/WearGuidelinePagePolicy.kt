package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute

enum class WearPageCategory {
    SCROLLING_LIST,
    MAP_LIST,
    MAP_VISUAL,
    PICKER,
    ADJUSTER,
    CONFIRMATION,
    TEXT_INPUT,
    MAIN_VISUAL
}

enum class WearPrimaryActionPattern {
    EDGE_BUTTON,
    ALERT_DIALOG,
    BUTTON_GROUP,
    LIST_ACTION,
    NONE
}

data class WearGuidelinePageEntry(
    val route: WatchRoute,
    val category: WearPageCategory,
    val primaryActionPattern: WearPrimaryActionPattern,
    val usesRoundListTransformation: Boolean,
    val transformsMainContent: Boolean
)

object WearGuidelinePagePolicy {
    val entries: List<WearGuidelinePageEntry> = listOf(
        WearGuidelinePageEntry(
            WatchRoute.FirstRunSetup,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Dashboard,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Passes,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.OrbitMap,
            WearPageCategory.MAP_VISUAL,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = false,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.OrbitMapDetail,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.NONE,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Qth,
            WearPageCategory.MAP_LIST,
            WearPrimaryActionPattern.BUTTON_GROUP,
            usesRoundListTransformation = true,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.Data,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Satellites,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Settings,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsAppearance,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsPassWindowAdjuster,
            WearPageCategory.ADJUSTER,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsPassAlerts,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsDataFreshness,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsMinimumElevation,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsMapSource,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsAbout,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.NONE,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsPrivacy,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.NONE,
            usesRoundListTransformation = false,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsLegal,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsLegalDocument,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.NONE,
            usesRoundListTransformation = false,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.SettingsDeveloperOptions,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.LIST_ACTION,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.QthPicker,
            WearPageCategory.PICKER,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = false
        ),
        WearGuidelinePageEntry(
            WatchRoute.SatelliteDetail,
            WearPageCategory.SCROLLING_LIST,
            WearPrimaryActionPattern.EDGE_BUTTON,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.SatellitesClearConfirm,
            WearPageCategory.CONFIRMATION,
            WearPrimaryActionPattern.ALERT_DIALOG,
            usesRoundListTransformation = true,
            transformsMainContent = true
        ),
        WearGuidelinePageEntry(
            WatchRoute.Radar,
            WearPageCategory.MAIN_VISUAL,
            WearPrimaryActionPattern.NONE,
            usesRoundListTransformation = false,
            transformsMainContent = false
        )
    )

    fun entryFor(route: WatchRoute): WearGuidelinePageEntry {
        return entries.first { it.route == route }
    }
}
