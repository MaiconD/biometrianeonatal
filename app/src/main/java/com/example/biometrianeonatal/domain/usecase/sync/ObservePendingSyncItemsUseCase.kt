package com.example.biometrianeonatal.domain.usecase.sync

import com.example.biometrianeonatal.domain.model.PendingSyncItem
import com.example.biometrianeonatal.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow

class ObservePendingSyncItemsUseCase(
    private val syncRepository: SyncRepository,
) {
    operator fun invoke(): Flow<List<PendingSyncItem>> {
        return syncRepository.observePendingSyncItems()
    }
}

