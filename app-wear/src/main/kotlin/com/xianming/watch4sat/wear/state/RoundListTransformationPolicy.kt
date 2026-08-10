package com.xianming.watch4sat.wear.state

enum class RoundListSurface {
    STANDARD_CARD,
    STANDARD_BUTTON,
    SWITCH_BUTTON,
    RADIO_BUTTON,
    SPLIT_CHECKBOX_BUTTON,
    LIST_HEADER,
    MAP,
    RADAR_CANVAS,
    TEXT_INPUT,
    PICKER,
    EDGE_BUTTON,
    PAGE_TITLE
}

object RoundListTransformationPolicy {
    const val usesCustomOfficialResponsiveSpec: Boolean = true
    const val usesSingleOfficialSpecForAllStandardLists: Boolean = true
    const val usesTransformedHeightAndSurfaceTransformation: Boolean = true
    const val usesComponentDefaultMinimumVerticalContentPadding: Boolean = true
    const val disallowsCustomGraphicsLayerScaling: Boolean = true
    const val edgeScaleTarget: Float = 0.72f
    const val edgeContentAlpha: Float = 0.82f
    const val edgeContainerAlpha: Float = 0.90f

    fun appliesTo(surface: RoundListSurface): Boolean {
        return when (surface) {
            RoundListSurface.STANDARD_CARD,
            RoundListSurface.STANDARD_BUTTON,
            RoundListSurface.SWITCH_BUTTON,
            RoundListSurface.RADIO_BUTTON,
            RoundListSurface.SPLIT_CHECKBOX_BUTTON,
            RoundListSurface.LIST_HEADER -> true
            RoundListSurface.MAP,
            RoundListSurface.RADAR_CANVAS,
            RoundListSurface.TEXT_INPUT,
            RoundListSurface.PICKER,
            RoundListSurface.EDGE_BUTTON,
            RoundListSurface.PAGE_TITLE -> false
        }
    }
}
