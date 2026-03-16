package com.example.biometrianeonatal.domain.usecase.history

import com.example.biometrianeonatal.domain.model.SessionListItem
import com.example.biometrianeonatal.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveSessionHistoryUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveSessionHistoryUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<SessionListItem>> {
        return historyRepository.observeSessions()
    }
}

