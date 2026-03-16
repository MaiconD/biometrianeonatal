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

class ObserveBabiesUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(): Flow<List<BabyListItem>> {
        return babyRepository.observeBabies()
    }
}

class ObserveBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<BabyDraft?> {
        return babyRepository.observeBaby(babyId)
    }
}

class ObserveBabySummaryUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<BabyProfileSummary?> {
        return babyRepository.observeBabySummary(babyId)
    }
}

class SaveBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    suspend operator fun invoke(draft: BabyDraft): String {
        return babyRepository.saveBaby(draft)
    }
}

class DeleteBabyUseCase(
    private val babyRepository: BabyRepository,
) {
    suspend operator fun invoke(babyId: String) {
        babyRepository.deleteBaby(babyId)
    }
}

class ObserveGuardiansUseCase(
    private val babyRepository: BabyRepository,
) {
    operator fun invoke(babyId: String): Flow<List<GuardianDraft>> {
        return babyRepository.observeGuardians(babyId)
    }
}

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

