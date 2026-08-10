package com.xianming.watch4sat.wear.state

object PassCardInteractionReducer {

    fun onCardTap(expandedPassKey: String?, tappedPassKey: String): PassCardInteractionResult {
        return if (expandedPassKey == tappedPassKey) {
            PassCardInteractionResult(expandedPassKey = tappedPassKey, openRadar = true)
        } else {
            PassCardInteractionResult(expandedPassKey = tappedPassKey, openRadar = false)
        }
    }
}

data class PassCardInteractionResult(
    val expandedPassKey: String,
    val openRadar: Boolean
)
