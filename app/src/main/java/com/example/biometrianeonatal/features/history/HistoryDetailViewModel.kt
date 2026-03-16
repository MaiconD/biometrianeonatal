package com.example.biometrianeonatal.features.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.SessionHistoryDetail
import com.example.biometrianeonatal.domain.usecase.history.ObserveSessionDetailUseCase
import com.example.biometrianeonatal.domain.usecase.history.OpenSessionCaptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryDetailUiState(
    val openingCaptureId: String? = null,
    val openedArtifact: OpenedArtifact? = null,
    val openArtifactErrorMessage: String? = null,
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    observeSessionDetailUseCase: ObserveSessionDetailUseCase,
    private val openSessionCaptureUseCase: OpenSessionCaptureUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    val detail: StateFlow<SessionHistoryDetail?> = observeSessionDetailUseCase(sessionId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun openCapture(fingerprintId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                openingCaptureId = fingerprintId,
                openArtifactErrorMessage = null,
            )
            runCatching {
                openSessionCaptureUseCase(fingerprintId)
            }.onSuccess { artifact ->
                _uiState.value = _uiState.value.copy(
                    openingCaptureId = null,
                    openedArtifact = artifact,
                    openArtifactErrorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    openingCaptureId = null,
                    openedArtifact = null,
                    openArtifactErrorMessage = error.message ?: "Não foi possível abrir a captura armazenada.",
                )
            }
        }
    }
}

