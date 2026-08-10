package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.wear.WatchRoute

object AppChromePolicy {
    const val usesRootAppScaffold: Boolean = true
    const val rootOwnsTimeText: Boolean = true
    const val listPagesProvideScrollStateToScreenScaffold: Boolean = true
    const val radarRouteSuppressesRootTimeText: Boolean = true
    const val orbitMapRouteSuppressesRootTimeText: Boolean = true
    const val firstRunRouteSuppressesRootTimeText: Boolean = true
    const val scrollingPagesShowTimeTextOnlyAtTop: Boolean = true
    const val timeTextDoesNotReturnOnlyBecauseScrollingStopped: Boolean = true
    const val fixedPagesKeepTimeTextVisible: Boolean = true
    const val listPagesUseSnapFlingAndRotarySnap: Boolean = false
    const val listPagesUseSnapFlingAndStableRotaryScroll: Boolean = false
    const val listPagesUseStableTouchFlingAndRotaryScroll: Boolean = true
    const val rotaryAvoidsTopTimeTextSnapLoop: Boolean = true
    const val scrollIndicatorsHideDuringScrollCapture: Boolean = true
    const val pagesDefineDuplicateTimeText: Boolean = false
    const val usesCustomTimeTextAnimation: Boolean = false
    const val rootBackgroundIsPureBlack: Boolean = true
    const val navigationBackgroundIsPureBlack: Boolean = true
    const val usesThemeBackgroundForNavigationTransitions: Boolean = false

    fun shouldShowRootTimeText(
        currentRoute: String?,
        pageReportedVisible: Boolean,
        modalChromeHidden: Boolean
    ): Boolean {
        val routeSuppressesRootTimeText =
            currentRoute == WatchRoute.Radar.route ||
                currentRoute == WatchRoute.OrbitMap.route ||
                currentRoute == WatchRoute.FirstRunSetup.route
        return !routeSuppressesRootTimeText && pageReportedVisible && !modalChromeHidden
    }
}
