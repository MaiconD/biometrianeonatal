package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveUserUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveUserUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(userId: String): Flow<AuthUser?> {
        return authRepository.observeUser(userId)
    }
}

