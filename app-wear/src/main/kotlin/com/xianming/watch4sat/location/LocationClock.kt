package com.xianming.watch4sat.location

object LocationClock {
    fun elapsedRealtimeMillis(): Long {
        return runCatching {
            android.os.SystemClock.elapsedRealtime()
        }.getOrElse {
            System.currentTimeMillis()
        }
    }
}
