package com.example.biometrianeonatal.domain.usecase.sync

import com.example.biometrianeonatal.domain.model.SyncExecutionResult
import com.example.biometrianeonatal.domain.repository.SyncRepository

/**
 * Caso de uso `RunSyncNowUseCase` que dispara uma execucao imediata de processo de dominio.
 */
class RunSyncNowUseCase(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(): SyncExecutionResult {
        return syncRepository.syncNow()
    }
}

