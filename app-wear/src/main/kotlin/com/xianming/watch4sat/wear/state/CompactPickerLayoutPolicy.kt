package com.xianming.watch4sat.wear.state

enum class EdgeActionVisualContentPlacement {
    TitleTopControlCenteredWithEdgeButtonBias,
    TitleTopControlTrueScreenCenter
}

object CompactPickerLayoutPolicy {
    const val usesNonScrollingPage: Boolean = true
    const val bottomApplyIsFixed: Boolean = true
    const val exposesApplyInSemantics: Boolean = true
    const val usesFakeScrollState: Boolean = false
    const val usesScreenScaffoldEdgeButton: Boolean = true
    const val showsScrollIndicator: Boolean = false
    const val isUserScrollable: Boolean = false
    const val usesReservedContentBottomAction: Boolean = false
    const val contentReservesBottomActionSpace: Boolean = false
    const val applyDoesNotOverlayPicker: Boolean = true
    const val usesManualBottomOverlay: Boolean = false
    const val hidesVisibleHelperText: Boolean = true
    const val reducesPickerHeightForBottomAction: Boolean = true
    const val requiresNonZeroEdgeButtonBounds: Boolean = true
    const val qthPickerUsesExplicitRowHeight: Boolean = true
    const val qthPickerHeightAffectsContainer: Boolean = true
    const val titleKeepsDefaultTopPlacement: Boolean = true
    const val controlUsesEdgeButtonCenterBias: Boolean = true
    val contentPlacement: EdgeActionVisualContentPlacement = EdgeActionVisualContentPlacement.TitleTopControlCenteredWithEdgeButtonBias
}
