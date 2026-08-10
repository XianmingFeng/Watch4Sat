package com.xianming.watch4sat.wear.state

enum class PassWindowAdjusterControlStyle {
    HorizontalAdjuster
}

enum class PassWindowAdjusterLayoutStyle {
    SlimVerticalSideButtonsPlainValue
}

object PassWindowAdjusterPolicy {
    val range: IntRange = 1..96
    const val stepHours: Int = 1
    const val showAdjusterByDefault: Boolean = true
    val selectionStyle: SettingsSelectionStyle = SettingsSelectionStyle.SecondaryAdjusterPage
    val controlStyle: PassWindowAdjusterControlStyle = PassWindowAdjusterControlStyle.HorizontalAdjuster
    const val returnsToSettingsAfterApply: Boolean = true
    const val usesCompactNonScrollingPage: Boolean = true
    const val usesScreenScaffoldEdgeButton: Boolean = true
    const val edgeButtonUsesScrollableModifier: Boolean = true
    const val contentHostedInStateBackedLazyColumn: Boolean = true
    const val showsScrollIndicator: Boolean = false
    const val isUserScrollable: Boolean = false
    const val requiresIntermediateValuePage: Boolean = false
    val layoutStyle: PassWindowAdjusterLayoutStyle = PassWindowAdjusterLayoutStyle.SlimVerticalSideButtonsPlainValue
    val contentPlacement: EdgeActionVisualContentPlacement = EdgeActionVisualContentPlacement.TitleTopControlTrueScreenCenter
    const val titleKeepsDefaultTopPlacement: Boolean = true
    const val controlUsesEdgeButtonCenterBias: Boolean = false
    const val titleUsesEdgeActionContentCenterOffset: Boolean = false
    const val controlUsesEdgeActionContentCenterOffset: Boolean = false
    const val fixedVisualContentUsesFullScreenOverlay: Boolean = true
    const val lazyContentPaddingPreservesEdgeButtonSpace: Boolean = true
    const val valueTakesRemainingWidth: Boolean = true
    const val sideButtonsUseEqualWeight: Boolean = false
    const val usesButtonGroup: Boolean = true
    const val valueUsesBackgroundContainer: Boolean = false
    const val unitLabelUsesSmallType: Boolean = true
    const val unitAlignsToNumberBottom: Boolean = true
    const val sideButtonsUseIconOnly: Boolean = true
    const val sideButtonsUseVerticalPillShape: Boolean = true
    const val sideButtonContentPaddingIsZero: Boolean = true
    const val decreaseIconName: String = "Remove"
    const val increaseIconName: String = "Add"
    const val sideButtonsUseTextSymbols: Boolean = false
    const val usesFixedResponsiveSlots: Boolean = true
    const val valueWidthDoesNotAffectButtonPosition: Boolean = true
    const val sideButtonSlotsAreSymmetric: Boolean = true
    const val visualButtonNarrowerThanSlot: Boolean = true
    const val buttonVisualSizeForced: Boolean = true
    const val iconCenteredInButton: Boolean = true
    const val unitXPositionStable: Boolean = true
    const val buttonVisualSize: String = "45x55"

    fun coerceHours(hours: Int): Int = hours.coerceIn(range.first, range.last)

    fun canDecrease(hours: Int): Boolean = coerceHours(hours) > range.first

    fun canIncrease(hours: Int): Boolean = coerceHours(hours) < range.last

    fun decrease(hours: Int): Int = coerceHours(hours - stepHours)

    fun increase(hours: Int): Int = coerceHours(hours + stepHours)
}
