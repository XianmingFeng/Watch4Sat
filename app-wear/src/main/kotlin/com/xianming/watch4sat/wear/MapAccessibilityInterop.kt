package com.xianming.watch4sat.wear

import android.view.View

internal fun View.useComposeAccessibilityOwner() {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
}

internal fun dispatchConfirmedMapTap(
    mapView: View,
    interactionEnabled: Boolean,
    onMapTap: () -> Unit
): Boolean {
    if (interactionEnabled) {
        mapView.performClick()
        onMapTap()
    }
    return interactionEnabled
}
