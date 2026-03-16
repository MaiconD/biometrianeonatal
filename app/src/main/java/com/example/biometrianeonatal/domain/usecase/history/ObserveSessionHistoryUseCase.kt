package com.example.biometrianeonatal.domain.usecase.history

import com.example.biometrianeonatal.domain.model.SessionListItem
import com.example.biometrianeonatal.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionHistoryUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<SessionListItem>> {
        return historyRepository.observeSessions()
    }
}

