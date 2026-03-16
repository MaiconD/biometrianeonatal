package com.example.biometrianeonatal.domain.usecase.history

import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.repository.HistoryRepository

/**
 * Caso de uso `OpenSessionCaptureUseCase` que abre um recurso seguro previamente armazenado.
 */
class OpenSessionCaptureUseCase(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(fingerprintId: String): OpenedArtifact? {
        return historyRepository.openSessionCapture(fingerprintId)
    }
}

