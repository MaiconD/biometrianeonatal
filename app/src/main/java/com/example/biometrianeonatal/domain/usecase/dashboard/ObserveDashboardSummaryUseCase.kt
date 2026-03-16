package com.example.biometrianeonatal.domain.usecase.dashboard

import com.example.biometrianeonatal.domain.model.DashboardSummary
import com.example.biometrianeonatal.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveDashboardSummaryUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveDashboardSummaryUseCase(
    private val dashboardRepository: DashboardRepository,
) {
    operator fun invoke(): Flow<DashboardSummary> {
        return dashboardRepository.observeSummary()
    }
}

