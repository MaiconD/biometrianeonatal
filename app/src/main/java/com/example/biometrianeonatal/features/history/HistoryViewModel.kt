package com.example.biometrianeonatal.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.domain.model.SessionListItem
import com.example.biometrianeonatal.domain.usecase.history.ObserveSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeSessionHistoryUseCase: ObserveSessionHistoryUseCase,
) : ViewModel() {
    val sessions: StateFlow<List<SessionListItem>> = observeSessionHistoryUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}

