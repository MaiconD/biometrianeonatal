package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.repository.AuthRepository

/**
 * Caso de uso `LogoutUseCase` que encerra a sessao autenticada atual.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.clearSession()
    }
}

