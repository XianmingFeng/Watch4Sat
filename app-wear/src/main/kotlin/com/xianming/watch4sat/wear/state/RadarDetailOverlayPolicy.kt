package com.xianming.watch4sat.wear.state

enum class RadarDetailPage(val id: String) {
    PassTask("pass_task"),
    Radio("radio"),
    FuturePasses("future_passes"),
    Diagnostics("diagnostics")
}

object RadarDetailOverlayPolicy {
    val pages: List<RadarDetailPage> = listOf(
        RadarDetailPage.PassTask,
        RadarDetailPage.Radio,
        RadarDetailPage.FuturePasses,
        RadarDetailPage.Diagnostics
    )
    val defaultPage: RadarDetailPage = RadarDetailPage.Radio
    val defaultPageIndex: Int = pages.indexOf(defaultPage)

    const val usesFullScreenOverlay: Boolean = true
    const val usesRightSideDrawer: Boolean = false
    const val usesBlurredRadarBackdrop: Boolean = true
    const val usesDarkScrim: Boolean = true
    const val usesHorizontalPagerScaffold: Boolean = true
    const val usesAnimatedPage: Boolean = true
    const val pullDownCloseStartsOnlyFromHandle: Boolean = true
    const val contentCanStartPullDownClose: Boolean = false
    const val systemBackClosesOverlayFirst: Boolean = true
    const val futurePassBackCollapsesExpandedDetailFirst: Boolean = true
    const val azElIsSecondaryMetadata: Boolean = true
    const val handleArcConcentricWithScreen: Boolean = true
    const val handleArcVisibleInsetPx: Float = 4f
    const val handleArcUsesStrokeHalfWidthCompensation: Boolean = true
    const val diagnosticsUsesAboutStyleInfoRows: Boolean = true
    const val futurePassCollapsedSecondLineShowsAzimuthRange: Boolean = true
    const val radioTransmitterSummaryUsesBodySmall: Boolean = true
    const val passTaskShowsPointingStatusLine: Boolean = true
    const val passTaskCombinesMaxElevationWithTcaTime: Boolean = true
}
