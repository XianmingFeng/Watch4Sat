package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.location.LocationResultState

object QthGpsFailureDialogPolicy {
    const val dialogComponent: String = "FailureConfirmationDialog"
    const val iconKey: String = "Timer"
    const val colorRole: String = "SemanticError"
    const val autoDismissMillis: Long = 1_000L

    fun shouldEmitFor(state: LocationResultState): Boolean {
        return state == LocationResultState.TIMEOUT
    }

    fun shouldShowEvent(
        currentEventId: Long,
        lastShownEventId: Long
    ): Boolean {
        return currentEventId > 0L &&
            currentEventId != lastShownEventId
    }
}
