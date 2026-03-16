package com.example.biometrianeonatal.domain.usecase.babies

import com.example.biometrianeonatal.domain.model.ArtifactInspection
import com.example.biometrianeonatal.domain.model.BabyDraft
import com.example.biometrianeonatal.domain.model.BabyListItem
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.ConsentAttachmentInput
import com.example.biometrianeonatal.domain.model.GuardianDraft
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.repository.BabyRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveBabiesUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveBabiesUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(): Flow<List<BabyListItem>> {
        return babyRepository.observeBabies()
    }
}

/**
 * Caso de uso `ObserveBabyUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<BabyDraft?> {
        return babyRepository.observeBaby(babyId)
    }
}

/**
 * Caso de uso `ObserveBabySummaryUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveBabySummaryUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<BabyProfileSummary?> {
        return babyRepository.observeBabySummary(babyId)
    }
}

/**
 * Caso de uso `SaveBabyUseCase` que persiste alteracoes solicitadas pela interface.
 */
class SaveBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    suspend operator fun invoke(draft: BabyDraft): String {
        return babyRepository.saveBaby(draft)
    }
}

/**
 * Caso de uso `DeleteBabyUseCase` que remove registros de acordo com as regras de negocio.
 */
class DeleteBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    suspend operator fun invoke(babyId: String) {
        babyRepository.deleteBaby(babyId)
    }
}

/**
 * Caso de uso `ObserveGuardiansUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveGuardiansUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<List<GuardianDraft>> {
        return babyRepository.observeGuardians(babyId)
    }
}

/**
 * Caso de uso `SaveGuardiansUseCase` que persiste alteracoes solicitadas pela interface.
 */
class SaveGuardiansUseCase(
    private val babyRepository: BabyRepository,
) {
    suspend operator fun invoke(
        babyId: String,
        guardians: List<GuardianDraft>,
    ) {
        babyRepository.saveGuardians(babyId, guardians)
    }
}

