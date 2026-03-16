package com.example.biometrianeonatal.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.biometrianeonatal.domain.model.SyncExecutionStatus
import com.example.biometrianeonatal.domain.usecase.sync.RunSyncNowUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java,
        )
        val trigger = inputData.getString(KEY_TRIGGER).orEmpty()

        return runCatching {
            val syncResult = entryPoint.runSyncNowUseCase().invoke()
            val output = Data.Builder()
                .putString(KEY_STATUS, syncResult.status.name)
                .putInt(KEY_SYNCED_COUNT, syncResult.syncedCount)
                .putString(KEY_SOURCE_LABEL, syncResult.sourceLabel)
                .putString(KEY_MESSAGE, syncResult.message)
                .putString(KEY_TRIGGER, trigger)
                .build()

            when (syncResult.status) {
                SyncExecutionStatus.SUCCESS,
                SyncExecutionStatus.FALLBACK_SUCCESS -> Result.success(output)
                SyncExecutionStatus.ERROR -> {
                    if (runAttemptCount >= MAX_RETRY_COUNT) Result.failure(output) else Result.retry()
                }
            }
        }.getOrElse {
            if (runAttemptCount >= MAX_RETRY_COUNT) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val PERIODIC_SYNC_WORK_NAME = "biometria_periodic_sync"
        const val IMMEDIATE_SYNC_WORK_NAME = "biometria_immediate_sync"
        const val KEY_STATUS = "sync_status"
        const val KEY_SYNCED_COUNT = "sync_synced_count"
        const val KEY_SOURCE_LABEL = "sync_source_label"
        const val KEY_MESSAGE = "sync_message"
        const val KEY_TRIGGER = "sync_trigger"
        const val TRIGGER_MANUAL_IMMEDIATE = "manual_immediate"
        const val TRIGGER_PERIODIC_BACKGROUND = "periodic_background"
        private const val MAX_RETRY_COUNT = 3
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun runSyncNowUseCase(): RunSyncNowUseCase
}

