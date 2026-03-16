package com.example.biometrianeonatal.features.sync

import androidx.work.WorkInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.core.sync.SyncWorker
import com.example.biometrianeonatal.domain.model.PendingSyncItem
import com.example.biometrianeonatal.domain.model.SyncExecutionStatus
import com.example.biometrianeonatal.domain.usecase.sync.ObserveImmediateSyncWorkUseCase
import com.example.biometrianeonatal.domain.usecase.sync.ObservePendingSyncItemsUseCase
import com.example.biometrianeonatal.domain.usecase.sync.ScheduleImmediateSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncUiState(
    val syncingNow: Boolean = false,
    val lastSyncedCount: Int = 0,
    val lastSyncSourceLabel: String? = null,
    val lastSyncMessage: String? = null,
    val lastSyncHadFallback: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    observePendingSyncItemsUseCase: ObservePendingSyncItemsUseCase,
    private val scheduleImmediateSyncUseCase: ScheduleImmediateSyncUseCase,
    observeImmediateSyncWorkUseCase: ObserveImmediateSyncWorkUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()
    private var pendingCompletion: (() -> Unit)? = null

    val pendingItems: StateFlow<List<PendingSyncItem>> = observePendingSyncItemsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            observeImmediateSyncWorkUseCase().collect { workInfo ->
                if (workInfo == null) return@collect

                val isRunning = workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
                val statusName = workInfo.outputData.getString(SyncWorker.KEY_STATUS)
                val status = statusName?.let {
                    runCatching { SyncExecutionStatus.valueOf(it) }.getOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    syncingNow = isRunning,
                    lastSyncedCount = workInfo.outputData.getInt(SyncWorker.KEY_SYNCED_COUNT, _uiState.value.lastSyncedCount),
                    lastSyncSourceLabel = workInfo.outputData.getString(SyncWorker.KEY_SOURCE_LABEL) ?: _uiState.value.lastSyncSourceLabel,
                    lastSyncMessage = workInfo.outputData.getString(SyncWorker.KEY_MESSAGE) ?: _uiState.value.lastSyncMessage,
                    lastSyncHadFallback = status == SyncExecutionStatus.FALLBACK_SUCCESS,
                    hasError = status == SyncExecutionStatus.ERROR || workInfo.state == WorkInfo.State.FAILED,
                )

                if (workInfo.state == WorkInfo.State.SUCCEEDED && status != SyncExecutionStatus.ERROR) {
                    pendingCompletion?.invoke()
                    pendingCompletion = null
                }
                if (workInfo.state == WorkInfo.State.FAILED || workInfo.state == WorkInfo.State.CANCELLED) {
                    pendingCompletion = null
                }
            }
        }
    }

    fun syncNow(onCompleted: (() -> Unit)? = null) {
        pendingCompletion = onCompleted
        _uiState.value = _uiState.value.copy(syncingNow = true, hasError = false)
        scheduleImmediateSyncUseCase()
    }
}

