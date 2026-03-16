package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentSessionUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthUser?> {
        return authRepository.observeCurrentSession()
    }
}

