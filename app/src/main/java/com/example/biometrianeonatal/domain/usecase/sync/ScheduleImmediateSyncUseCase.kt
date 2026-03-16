package com.example.biometrianeonatal.domain.usecase.sync

import com.example.biometrianeonatal.core.sync.SyncWorkScheduler

/**
 * Caso de uso `ScheduleImmediateSyncUseCase` que agenda uma execucao assincrona da operacao de dominio.
 */
class ScheduleImmediateSyncUseCase(
    private val syncWorkScheduler: SyncWorkScheduler,
) {
    operator fun invoke() {
        syncWorkScheduler.scheduleImmediateSync()
    }
}

