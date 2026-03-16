package com.example.biometrianeonatal.domain.usecase.biometric

import com.example.biometrianeonatal.domain.model.CapturePreview
import com.example.biometrianeonatal.domain.model.CaptureSource
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.SensorRuntimeInfo
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress
import com.example.biometrianeonatal.domain.model.SessionContext
import com.example.biometrianeonatal.domain.repository.BiometricRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso `ObserveSessionContextUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveSessionContextUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(babyId: String, operatorId: String): Flow<SessionContext?> {
        return biometricRepository.observeSessionContext(babyId, operatorId)
    }
}

/**
 * Caso de uso `ObserveSensorRuntimeUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveSensorRuntimeUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(): Flow<SensorRuntimeInfo> {
        return biometricRepository.observeSensorRuntime()
    }
}

/**
 * Caso de uso `ObserveSessionProgressUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObserveSessionProgressUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(sessionId: String): Flow<SessionCaptureProgress> {
        return biometricRepository.observeSessionProgress(sessionId)
    }
}

/**
 * Caso de uso `SelectCaptureSourceUseCase` que coordena uma acao pontual do fluxo biometrico.
 */
class SelectCaptureSourceUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(source: CaptureSource) {
        biometricRepository.selectCaptureSource(source)
    }
}

/**
 * Caso de uso `RequestUsbPermissionUseCase` que coordena uma acao pontual do fluxo biometrico.
 */
class RequestUsbPermissionUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke() {
        biometricRepository.requestUsbPermission()
    }
}

/**
 * Caso de uso `SelectUsbDeviceUseCase` que coordena uma acao pontual do fluxo biometrico.
 */
class SelectUsbDeviceUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(deviceId: Int) {
        biometricRepository.selectUsbDevice(deviceId)
    }
}

/**
 * Caso de uso `ObservePendingCaptureUseCase` que observa um fluxo de dados da camada de dominio.
 */
class ObservePendingCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(sessionId: String): Flow<CapturePreview?> {
        return biometricRepository.observePendingCapture(sessionId)
    }
}

/**
 * Caso de uso `OpenPendingCaptureUseCase` que abre um recurso seguro previamente armazenado.
 */
class OpenPendingCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String): OpenedArtifact? {
        return biometricRepository.openPendingCapture(sessionId)
    }
}

/**
 * Caso de uso `StartBiometricSessionUseCase` que inicia um novo fluxo operacional no dominio.
 */
class StartBiometricSessionUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(babyId: String, operatorId: String): String {
        return biometricRepository.startSession(babyId, operatorId)
    }
}

/**
 * Caso de uso `GenerateCapturePreviewUseCase` que gera um artefato intermediario usado pelo fluxo principal.
 */
class GenerateCapturePreviewUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String, fingerCode: String): CapturePreview {
        return biometricRepository.generateCapturePreview(sessionId, fingerCode)
    }
}

/**
 * Caso de uso `AcceptCaptureUseCase` que coordena uma acao pontual do fluxo biometrico.
 */
class AcceptCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String): SessionCaptureProgress {
        return biometricRepository.acceptPendingCapture(sessionId)
    }
}

/**
 * Caso de uso `DiscardCaptureUseCase` que coordena uma acao pontual do fluxo biometrico.
 */
class DiscardCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String) {
        biometricRepository.discardPendingCapture(sessionId)
    }
}

