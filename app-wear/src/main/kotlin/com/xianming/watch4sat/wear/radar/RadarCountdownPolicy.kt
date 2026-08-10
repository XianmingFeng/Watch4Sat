package com.xianming.watch4sat.wear.radar

object RadarCountdownPolicy {
    fun showSecondsInFinalMinute(updateMode: RadarUpdateMode): Boolean {
        return when (updateMode) {
            RadarUpdateMode.Interactive -> true
            RadarUpdateMode.AmbientOneHz -> true
        }
    }
}
