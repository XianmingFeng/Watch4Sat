package com.xianming.watch4sat.wear.state

data class OrbitMapChromeState(
    val interactiveVisible: Boolean = true
)

object OrbitMapChromePolicy {
    fun onConfirmedMapTap(state: OrbitMapChromeState): OrbitMapChromeState {
        return state.copy(interactiveVisible = !state.interactiveVisible)
    }

    fun interactiveChromeVisible(
        state: OrbitMapChromeState,
        isAmbient: Boolean
    ): Boolean {
        return state.interactiveVisible && !isAmbient
    }

    fun topTimeTextVisible(
        state: OrbitMapChromeState,
        isAmbient: Boolean
    ): Boolean {
        return isAmbient || state.interactiveVisible
    }

    fun sideControlsVisible(
        state: OrbitMapChromeState,
        isAmbient: Boolean,
        candidateCount: Int
    ): Boolean {
        return interactiveChromeVisible(state, isAmbient) && candidateCount > 1
    }

    fun edgeButtonVisible(
        state: OrbitMapChromeState,
        isAmbient: Boolean
    ): Boolean {
        return interactiveChromeVisible(state, isAmbient)
    }

    fun persistentErrorVisible(message: String?): Boolean = !message.isNullOrBlank()
}
