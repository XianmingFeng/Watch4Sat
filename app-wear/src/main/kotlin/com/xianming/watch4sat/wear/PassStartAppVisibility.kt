package com.xianming.watch4sat.wear

import java.util.concurrent.atomic.AtomicBoolean

object PassStartAppVisibility {
    private val resumed = AtomicBoolean(false)

    fun setForeground(isForeground: Boolean) {
        resumed.set(isForeground)
    }

    fun isForeground(): Boolean = resumed.get()
}
