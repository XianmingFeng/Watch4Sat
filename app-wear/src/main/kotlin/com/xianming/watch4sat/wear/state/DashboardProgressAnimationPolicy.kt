package com.xianming.watch4sat.wear.state

enum class DashboardProgressTransformOrigin {
    LeftCenter
}

object DashboardProgressAnimationPolicy {
    const val usesAnimatedVisibility: Boolean = true
    const val usesFadeAnimation: Boolean = true
    const val usesScaleAnimation: Boolean = false
    const val usesOfficialComposeAnimation: Boolean = true
    const val usesDirectConditionalToggle: Boolean = false
    const val usesCustomGraphicsLayerAnimation: Boolean = false
    const val enterMillis: Int = 420
    const val exitMillis: Int = 520
    val transformOrigin: DashboardProgressTransformOrigin = DashboardProgressTransformOrigin.LeftCenter
}
