package com.example.biometrianeonatal.domain.usecase.sync

import com.example.biometrianeonatal.core.sync.SyncWorkScheduler

class ScheduleImmediateSyncUseCase(
    private val syncWorkScheduler: SyncWorkScheduler,
) {
    operator fun invoke() {
        syncWorkScheduler.scheduleImmediateSync()
    }
}

