package com.example.biometrianeonatal.features.biometric

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.CapturePreview
import com.example.biometrianeonatal.domain.model.CaptureSource
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.SensorRuntimeInfo
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress
import com.example.biometrianeonatal.domain.model.SessionContext
import com.example.biometrianeonatal.domain.model.UsbDeviceOption
import com.example.biometrianeonatal.domain.usecase.biometric.AcceptCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.DiscardCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.GenerateCapturePreviewUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.OpenPendingCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObservePendingCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.RequestUsbPermissionUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSensorRuntimeUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSessionProgressUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSessionContextUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.SelectCaptureSourceUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.SelectUsbDeviceUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.StartBiometricSessionUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado do fluxo biometrico com dedo ativo, sessao corrente e resultado da revisao segura.
 */
data class BiometricUiState(
    val activeFinger: String = "POLEGAR_DIREITO",
    val sessionId: String? = null,
    val isCapturing: Boolean = false,
    val captureErrorMessage: String? = null,
    val reviewArtifact: OpenedArtifact? = null,
    val isLoadingReviewArtifact: Boolean = false,
    val reviewArtifactErrorMessage: String? = null,
)

/**
 * ViewModel central da coleta biometrica com captura, revisao, aceite e descarte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BiometricViewModel @Inject constructor(
    observeSessionContextUseCase: ObserveSessionContextUseCase,
    observeSensorRuntimeUseCase: ObserveSensorRuntimeUseCase,
    private val observeSessionProgressUseCase: ObserveSessionProgressUseCase,
    private val observePendingCaptureUseCase: ObservePendingCaptureUseCase,
    private val openPendingCaptureUseCase: OpenPendingCaptureUseCase,
    private val selectCaptureSourceUseCase: SelectCaptureSourceUseCase,
    private val requestUsbPermissionUseCase: RequestUsbPermissionUseCase,
    private val selectUsbDeviceUseCase: SelectUsbDeviceUseCase,
    private val startBiometricSessionUseCase: StartBiometricSessionUseCase,
    private val generateCapturePreviewUseCase: GenerateCapturePreviewUseCase,
    private val acceptCaptureUseCase: AcceptCaptureUseCase,
    private val discardCaptureUseCase: DiscardCaptureUseCase,
    observeBabySummaryUseCase: ObserveBabySummaryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Identificadores recebidos pela navegação para reconstruir o contexto completo da coleta.
    private val babyId: String = checkNotNull(savedStateHandle["babyId"])
    private val operatorId: String = checkNotNull(savedStateHandle["userId"])
    private val initialSessionId: String? = savedStateHandle["sessionId"]

    private val _uiState = MutableStateFlow(BiometricUiState(sessionId = initialSessionId))
    val uiState: StateFlow<BiometricUiState> = _uiState.asStateFlow()
    // Garante que a sugestão automática do próximo dedo aconteça só uma vez ao retomar uma sessão em andamento.
    private var hasAlignedInitialSuggestedFinger = false

    val sessionContext: StateFlow<SessionContext?> = observeSessionContextUseCase(
        babyId = babyId,
        operatorId = operatorId,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val sensorRuntime: StateFlow<SensorRuntimeInfo> = observeSensorRuntimeUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SensorRuntimeInfo(),
    )

    val babyProfileSummary: StateFlow<BabyProfileSummary?> = observeBabySummaryUseCase(babyId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val pendingCapture: StateFlow<CapturePreview?> = _uiState
        .map { it.sessionId }
        .filterNotNull()
        // A captura pendente sempre é observada a partir da sessão atualmente ativa na UI.
        .flatMapLatest { sessionId ->
            observePendingCaptureUseCase(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val sessionProgress: StateFlow<SessionCaptureProgress> = _uiState
        .map { it.sessionId }
        .filterNotNull()
        // O progresso também troca dinamicamente quando a sessão muda ou é retomada.
        .flatMapLatest { sessionId ->
            observeSessionProgressUseCase(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionCaptureProgress(),
        )

    init {
        if (initialSessionId != null) {
            viewModelScope.launch {
                sessionProgress.collect { progress ->
                    // Ao abrir uma sessão existente, a UI é reposicionada no próximo dedo sugerido pelo domínio.
                    if (hasAlignedInitialSuggestedFinger || progress.sessionId.isBlank()) return@collect
                    hasAlignedInitialSuggestedFinger = true
                    progress.nextSuggestedFingerCode
                        ?.takeIf { suggestedFinger -> suggestedFinger != _uiState.value.activeFinger }
                        ?.let { suggestedFinger ->
                            _uiState.value = _uiState.value.copy(activeFinger = suggestedFinger)
                        }
                }
            }
        }
    }

    fun updateFinger(value: String) {
        _uiState.value = _uiState.value.copy(
            activeFinger = value,
            captureErrorMessage = null,
            reviewArtifactErrorMessage = null,
        )
    }

    fun selectCaptureSource(source: CaptureSource) {
        viewModelScope.launch {
            selectCaptureSourceUseCase(source)
            _uiState.value = _uiState.value.copy(
                captureErrorMessage = null,
                reviewArtifact = null,
                reviewArtifactErrorMessage = null,
            )
        }
    }

    fun requestUsbPermission() {
        viewModelScope.launch {
            requestUsbPermissionUseCase()
        }
    }

    fun selectUsbDevice(device: UsbDeviceOption) {
        viewModelScope.launch {
            selectUsbDeviceUseCase(device.deviceId)
        }
    }

    fun loadReviewArtifact() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingReviewArtifact = true,
                reviewArtifactErrorMessage = null,
            )
            runCatching {
                // Abertura protegida da captura temporária para revisão visual antes do aceite definitivo.
                openPendingCaptureUseCase(sessionId)
            }.onSuccess { openedArtifact ->
                _uiState.value = _uiState.value.copy(
                    isLoadingReviewArtifact = false,
                    reviewArtifact = openedArtifact,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingReviewArtifact = false,
                    reviewArtifact = null,
                    reviewArtifactErrorMessage = error.message ?: "Falha ao abrir a captura para revisão.",
                )
            }
        }
    }

    fun startSession(onStarted: (String) -> Unit) {
        viewModelScope.launch {
            val sessionId = startBiometricSessionUseCase(babyId, operatorId)
            _uiState.value = _uiState.value.copy(sessionId = sessionId)
            onStarted(sessionId)
        }
    }

    fun capture(onCaptured: () -> Unit) {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCapturing = true, captureErrorMessage = null)
            runCatching {
                // O dedo ativo vem do estado atual para que a UI possa orientar a coleta seguinte sem acoplamento extra.
                generateCapturePreviewUseCase(sessionId, _uiState.value.activeFinger)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isCapturing = false)
                onCaptured()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    captureErrorMessage = error.message ?: "Falha ao executar a captura biométrica.",
                    reviewArtifact = null,
                )
            }
        }
    }

    fun acceptCapture(
        onContinueCollection: () -> Unit,
        onSessionCompleted: () -> Unit,
    ) {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            val progress = acceptCaptureUseCase(sessionId)
            if (progress.isComplete) {
                _uiState.value = _uiState.value.copy(
                    reviewArtifact = null,
                    reviewArtifactErrorMessage = null,
                )
                onSessionCompleted()
            } else {
                _uiState.value = _uiState.value.copy(
                    // O próprio progresso devolve o próximo dedo recomendado para reduzir erros operacionais.
                    activeFinger = progress.nextSuggestedFingerCode ?: _uiState.value.activeFinger,
                    reviewArtifact = null,
                    reviewArtifactErrorMessage = null,
                )
                onContinueCollection()
            }
        }
    }

    fun discardCapture() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            discardCaptureUseCase(sessionId)
            _uiState.value = _uiState.value.copy(reviewArtifact = null, reviewArtifactErrorMessage = null)
        }
    }
}




