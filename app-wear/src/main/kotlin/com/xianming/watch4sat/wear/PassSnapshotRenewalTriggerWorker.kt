package com.xianming.watch4sat.wear

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore

class PassSnapshotRenewalTriggerWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val settings = Watch4SatSettingsStore(applicationContext).getSettings()
        if (settings.stationDataDeletionInProgress || settings.stationLocation == null) {
            return Result.success()
        }
        PassSnapshotRenewalEnqueuer.enqueueImmediate(
            context = applicationContext,
            reason = PassSnapshotRenewalReason.COVERAGE_THRESHOLD
        )
        return Result.success()
    }
}
