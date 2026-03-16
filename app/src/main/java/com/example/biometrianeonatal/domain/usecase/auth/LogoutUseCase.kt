package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.clearSession()
    }
}

