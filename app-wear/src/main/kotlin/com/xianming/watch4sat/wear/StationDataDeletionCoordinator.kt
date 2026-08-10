package com.xianming.watch4sat.wear

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StationDataDeletionCoordinator(
    private val beginStationDataDeletion: suspend () -> Unit,
    private val cancelRenewalWork: suspend () -> Unit,
    private val cancelPassAlarm: suspend () -> Unit,
    private val clearAlarmState: suspend () -> Unit,
    private val clearPassSnapshot: suspend () -> Unit,
    private val completeStationDataDeletion: suspend () -> Unit
) {
    private val deletionMutex = Mutex()

    suspend fun clear() {
        deletionMutex.withLock {
            beginStationDataDeletion()
            cancelRenewalWork()
            cancelPassAlarm()
            clearAlarmState()
            clearPassSnapshot()
            completeStationDataDeletion()
        }
    }
}
