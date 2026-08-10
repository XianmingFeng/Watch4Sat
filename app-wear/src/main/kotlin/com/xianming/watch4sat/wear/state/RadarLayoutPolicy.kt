package com.xianming.watch4sat.wear.state

object RadarLayoutPolicy {
    const val usesFullScreenSkyPlot: Boolean = true
    const val hidesRootTimeText: Boolean = true
    const val usesScreenScaffold: Boolean = true
    const val usesRightSideOverlay: Boolean = false
    const val overlayKeepsBlurredRadarBackground: Boolean = true
    const val usesIconOnlyInfoFallback: Boolean = false
    const val usesSensorPointingReticle: Boolean = true
    const val mainContentUsesRoundListTransformation: Boolean = false
    const val usesOldSmallCanvasWithTextDetails: Boolean = false
    const val preservesLeftEdgeSystemSwipeBackWhenOverlayClosed: Boolean = true
    const val preservesLeftEdgeSystemSwipeBackWhenOverlayOpen: Boolean = false
    const val disablesRouteSwipeDismissWhenOverlayOpen: Boolean = true
    const val exposesCanvasSemanticsSummary: Boolean = true
    const val insetsHorizonRingFromScreenEdge: Boolean = true
    const val usesThemeColoredRadarGrid: Boolean = true
    const val showsCompassCardinalLabels: Boolean = true
    const val rotatesCompassLabelsTowardCenter: Boolean = true
    const val rotatesSkyPlotUnderCurrentDeviceHeading: Boolean = true
    const val usesGraphicsLayerForHeadingSmoothing: Boolean = true
    const val ambientHeadingSnapsWithoutAnimation: Boolean = true
    const val usesScreenTopEdgeAsForwardAxis: Boolean = true
    const val usesTransientPassBadge: Boolean = false
    const val showsNonBlockingCompassCalibrationHint: Boolean = true
    const val calibrationHintUsesSemanticWarningColor: Boolean = true
    const val usesMaterial3SatelliteEdgeButton: Boolean = true
    const val satelliteEdgeButtonUsesBadgeWidthConstraint: Boolean = false
    const val satelliteEdgeButtonOverridesMaterialTypography: Boolean = false
    const val satelliteEdgeButtonTextMaxLines: Int = 2
}

object RadarOverlayPolicy {
    enum class OpenEdge {
        None,
        Right
    }

    val openEdge: OpenEdge = OpenEdge.None
    const val usesCustomLeftEdgeGesture: Boolean = false
    const val disablesRouteSwipeDismissWhenOpen: Boolean = true
    const val systemBackClosesOverlayFirst: Boolean = true
    const val usesBlurredRadarBackdrop: Boolean = true
    const val backdropUsesDisplayLayerHeadingSmoothing: Boolean = true
    const val usesDarkScrim: Boolean = true
    const val satelliteNameMaxLines: Int = 2
    const val valueMaxLines: Int = 2
    val contentOrder: List<String> = RadarDetailOverlayPolicy.pages.map { it.id }
}
