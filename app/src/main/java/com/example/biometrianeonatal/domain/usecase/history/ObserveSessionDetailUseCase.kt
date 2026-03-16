package com.example.biometrianeonatal.domain.usecase.history

import com.example.biometrianeonatal.domain.model.SessionHistoryDetail
import com.example.biometrianeonatal.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveSessionDetailUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveSessionDetailUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(sessionId: String): Flow<SessionHistoryDetail?> {
        return historyRepository.observeSessionDetail(sessionId)
    }
}

