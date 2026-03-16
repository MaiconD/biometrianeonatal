package com.example.biometrianeonatal.features.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.model.DashboardSummary
import com.example.biometrianeonatal.domain.usecase.auth.LogoutUseCase
import com.example.biometrianeonatal.domain.usecase.auth.ObserveUserUseCase
import com.example.biometrianeonatal.domain.usecase.dashboard.ObserveDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que combina dados do usuario logado e metricas resumidas do dispositivo.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeUserUseCase: ObserveUserUseCase,
    observeDashboardSummaryUseCase: ObserveDashboardSummaryUseCase,
    private val logoutUseCase: LogoutUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    val user: StateFlow<AuthUser?> = observeUserUseCase(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val summary: StateFlow<DashboardSummary> = observeDashboardSummaryUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardSummary(0, 0, 0),
    )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}

