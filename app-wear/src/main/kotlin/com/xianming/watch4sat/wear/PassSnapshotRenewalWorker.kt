package com.xianming.watch4sat.wear

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xianming.watch4sat.data.Watch4SatDataLayer
import kotlinx.coroutines.CancellationException
import java.io.IOException

class PassSnapshotRenewalWorker : CoroutineWorker {
    private val runtime: PassSnapshotRenewalWorkerRuntime

    constructor(
        appContext: Context,
        workerParameters: WorkerParameters
    ) : super(appContext, workerParameters) {
        runtime = DefaultPassSnapshotRenewalWorkerRuntime(appContext)
    }

    internal constructor(
        appContext: Context,
        workerParameters: WorkerParameters,
        runtime: PassSnapshotRenewalWorkerRuntime
    ) : super(appContext, workerParameters) {
        this.runtime = runtime
    }

    override suspend fun doWork(): Result {
        return try {
            val recoveryRequired = runtime.recoveryRequired()
            when (
                val result = runtime.renew(
                    forceRebuild = inputData.getBoolean(InputForceRebuild, false) ||
                        recoveryRequired
                )
            ) {
                PassSnapshotRenewalResult.PrerequisitesMissing -> Result.success()
                PassSnapshotRenewalResult.Superseded -> Result.success()
                is PassSnapshotRenewalResult.Ready -> {
                    runtime.acknowledgeRecovery()
                    PassSnapshotRenewalEnqueuer.enqueueContinuation(
                        context = applicationContext,
                        coverageEndMillis = result.snapshot.coverageEndMillis,
                        nowMillis = System.currentTimeMillis()
                    )
                    Result.success()
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            retryOrRecover()
        } catch (_: Exception) {
            retryOrRecover()
        }
    }

    private fun retryOrRecover(): Result {
        if (runAttemptCount + 1 < MaxRunAttempts) return Result.retry()
        PassSnapshotRenewalEnqueuer.enqueueTerminalRecovery(applicationContext)
        return Result.failure()
    }

    companion object {
        const val InputReason = "renewal_reason"
        const val InputForceRebuild = "force_rebuild"
        internal const val MaxRunAttempts = 3
    }
}

internal interface PassSnapshotRenewalWorkerRuntime {
    suspend fun recoveryRequired(): Boolean

    suspend fun renew(forceRebuild: Boolean): PassSnapshotRenewalResult

    suspend fun acknowledgeRecovery()
}

private class DefaultPassSnapshotRenewalWorkerRuntime(
    context: Context
) : PassSnapshotRenewalWorkerRuntime {
    private val appContext = context.applicationContext
    private val dependencies = Watch4SatDataLayer.createLocalOnly(appContext)
    private val alarmStateStore = PassStartAlarmStateStore(appContext)
    private val alarmScheduler = PassStartAlarmScheduler(
        appContext,
        alarmStateStore,
        stationDataScheduleGuard(appContext)
    )
    private val runner = PassSnapshotRenewalRunner(
        repository = dependencies.satelliteDataRepository,
        settingsProvider = dependencies.settingsStore::getSettings,
        snapshotCache = dependencies.passSnapshotCache,
        alarmStateProvider = alarmStateStore::read,
        alarmTarget = PassStartAlarmTarget { candidate, state ->
            alarmScheduler.schedule(candidate, state)
        }
    )

    override suspend fun recoveryRequired(): Boolean {
        return dependencies.passSnapshotCache.recoveryRequired() ||
            alarmStateStore.recoveryRequired()
    }

    override suspend fun renew(forceRebuild: Boolean): PassSnapshotRenewalResult {
        return runner.run(
            nowMillis = System.currentTimeMillis(),
            forceRebuild = forceRebuild
        )
    }

    override suspend fun acknowledgeRecovery() {
        dependencies.passSnapshotCache.consumeRecoveryRequired()
        alarmStateStore.consumeRecoveryRequired()
    }
}
