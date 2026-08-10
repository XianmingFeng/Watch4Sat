package com.xianming.watch4sat.wear

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PassStartForegroundAlarmEvents {
    private val mutableEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1
    )

    val events: SharedFlow<Unit> = mutableEvents.asSharedFlow()

    fun emit(): Boolean {
        return mutableEvents.tryEmit(Unit)
    }
}
