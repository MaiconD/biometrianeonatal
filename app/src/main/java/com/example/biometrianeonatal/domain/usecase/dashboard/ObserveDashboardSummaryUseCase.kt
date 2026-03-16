package com.example.biometrianeonatal.domain.usecase.dashboard

import com.example.biometrianeonatal.domain.model.DashboardSummary
import com.example.biometrianeonatal.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveDashboardSummaryUseCase(
    private val dashboardRepository: DashboardRepository,
) {
    operator fun invoke(): Flow<DashboardSummary> {
        return dashboardRepository.observeSummary()
    }
}

