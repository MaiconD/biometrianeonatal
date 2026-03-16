package com.example.biometrianeonatal.di

import com.example.biometrianeonatal.domain.repository.AuthRepository
import com.example.biometrianeonatal.domain.usecase.auth.ObserveCurrentSessionUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppGraphDependencies @Inject constructor(
    val authRepository: AuthRepository,
    val observeCurrentSessionUseCase: ObserveCurrentSessionUseCase,
)

