package com.xianming.watch4sat.wear

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PassSnapshotRenewalEnqueuer {

    fun enqueueImmediate(
        context: Context,
        reason: PassSnapshotRenewalReason
    ) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            PassSnapshotRenewalPolicy.UniqueWorkName,
            immediatePolicy(reason),
            request(reason = reason, initialDelayMillis = 0L)
        )
    }

    internal fun immediatePolicy(
        reason: PassSnapshotRenewalReason
    ): ExistingWorkPolicy {
        return if (PassSnapshotRenewalPolicy.requiresImmediateReplacement(reason)) {
            ExistingWorkPolicy.REPLACE
        } else {
            ExistingWorkPolicy.KEEP
        }
    }

    fun enqueueContinuation(
        context: Context,
        coverageEndMillis: Long,
        nowMillis: Long
    ) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            PassSnapshotRenewalPolicy.ContinuationUniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            continuationRequest(
                initialDelayMillis = PassSnapshotRenewalPolicy.continuationDelayMillis(
                    coverageEndMillis = coverageEndMillis,
                    nowMillis = nowMillis
                )
            )
        )
    }

    fun enqueueTerminalRecovery(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            PassSnapshotRenewalPolicy.ContinuationUniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            continuationRequest(
                initialDelayMillis = PassSnapshotRenewalPolicy.TerminalRecoveryDelayMillis
            )
        )
    }

    suspend fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        withContext(Dispatchers.IO) {
            workManager.cancelUniqueWork(PassSnapshotRenewalPolicy.ContinuationUniqueWorkName)
                .result
                .get()
            workManager.cancelUniqueWork(PassSnapshotRenewalPolicy.UniqueWorkName)
                .result
                .get()
        }
    }

    internal fun request(
        reason: PassSnapshotRenewalReason,
        initialDelayMillis: Long
    ) = OneTimeWorkRequestBuilder<PassSnapshotRenewalWorker>()
        .setInputData(
            workDataOf(
                PassSnapshotRenewalWorker.InputReason to reason.name,
                PassSnapshotRenewalWorker.InputForceRebuild to
                    PassSnapshotRenewalPolicy.requiresImmediateReplacement(reason)
            )
        )
        .setInitialDelay(initialDelayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        .addTag(PassSnapshotRenewalPolicy.WorkTag)
        .build()

    internal fun continuationRequest(initialDelayMillis: Long) =
        OneTimeWorkRequestBuilder<PassSnapshotRenewalTriggerWorker>()
            .setInitialDelay(initialDelayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .addTag(PassSnapshotRenewalPolicy.ContinuationWorkTag)
            .build()
}
