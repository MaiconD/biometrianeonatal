package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.model.Hospital
import com.example.biometrianeonatal.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveHospitalsUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveHospitalsUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<List<Hospital>> {
        return authRepository.observeHospitals()
    }
}

