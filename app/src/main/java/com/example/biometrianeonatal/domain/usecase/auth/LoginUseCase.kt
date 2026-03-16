package com.example.biometrianeonatal.domain.usecase.auth

import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.repository.AuthRepository

/**
 * Caso de uso `LoginUseCase` que autentica o usuario e prepara a sessao local.
 */
class LoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String, hospitalId: String): AuthUser? {
        val user = authRepository.login(email, password, hospitalId)
        if (user != null) {
            authRepository.persistSession(user.id)
        }
        return user
    }
}

