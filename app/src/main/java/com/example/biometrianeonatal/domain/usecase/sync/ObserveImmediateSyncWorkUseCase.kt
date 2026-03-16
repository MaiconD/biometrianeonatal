package com.example.biometrianeonatal.domain.usecase.sync

import androidx.work.WorkInfo
import com.example.biometrianeonatal.core.sync.SyncWorkScheduler
import kotlinx.coroutines.flow.Flow

class ObserveImmediateSyncWorkUseCase(
    private val syncWorkScheduler: SyncWorkScheduler,
) {
    operator fun invoke(): Flow<WorkInfo?> {
        return syncWorkScheduler.observeImmediateSyncWork()
    }
}

