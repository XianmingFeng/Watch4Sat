package com.xianming.watch4sat.wear.state

enum class QthPeerAction {
    GPS,
    Map
}

object QthLayoutPolicy {
    val pageCategory: WearPageCategory = WearPageCategory.MAP_LIST
    val contentOrder: List<String> = listOf("locator", "map", "status", "gps_map_actions")
    val bottomActions: List<QthPeerAction> = QthPeerAction.entries
    const val bottomActionsArePinned: Boolean = false
    const val mainPageIsScrollable: Boolean = true
    const val pickerPageIsScrollable: Boolean = false
    const val peerActionsUseRoundListTransformation: Boolean = false
    const val bottomActionsUseSafeScrollSpacer: Boolean = true
    const val bottomActionsUseFixedVisualSafeArea: Boolean = false
    const val peerActionsExposeContentDescriptions: Boolean = true
    const val mapIsCompact: Boolean = true
}
