package com.xianming.watch4sat.wear.state

import com.xianming.watch4sat.data.settings.Watch4SatSettings

enum class DataRefreshAttemptStatus {
    NeverAttempted,
    Succeeded,
    Failed
}

object DataRefreshAttemptStatusPolicy {
    fun status(
        lastSuccessMillis: Long?,
        lastFailureMillis: Long?
    ): DataRefreshAttemptStatus {
        return when {
            lastFailureMillis != null -> DataRefreshAttemptStatus.Failed
            lastSuccessMillis != null -> DataRefreshAttemptStatus.Succeeded
            else -> DataRefreshAttemptStatus.NeverAttempted
        }
    }
}

fun Watch4SatSettings.satelliteDataRefreshStatus(): DataRefreshAttemptStatus {
    return DataRefreshAttemptStatusPolicy.status(
        lastSuccessMillis = lastSatelliteDataUpdateMillis,
        lastFailureMillis = lastSatelliteDataFailureMillis
    )
}

fun Watch4SatSettings.transmitterDataRefreshStatus(): DataRefreshAttemptStatus {
    return DataRefreshAttemptStatusPolicy.status(
        lastSuccessMillis = lastTransmitterDataUpdateMillis,
        lastFailureMillis = lastTransmitterDataFailureMillis
    )
}
