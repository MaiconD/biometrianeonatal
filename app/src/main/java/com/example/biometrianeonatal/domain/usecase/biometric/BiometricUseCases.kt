package com.example.biometrianeonatal.domain.usecase.biometric

import com.example.biometrianeonatal.domain.model.CapturePreview
import com.example.biometrianeonatal.domain.model.CaptureSource
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.SensorRuntimeInfo
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress
import com.example.biometrianeonatal.domain.model.SessionContext
import com.example.biometrianeonatal.domain.repository.BiometricRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionContextUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(babyId: String, operatorId: String): Flow<SessionContext?> {
        return biometricRepository.observeSessionContext(babyId, operatorId)
    }
}

class ObserveSensorRuntimeUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(): Flow<SensorRuntimeInfo> {
        return biometricRepository.observeSensorRuntime()
    }
}

class ObserveSessionProgressUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(sessionId: String): Flow<SessionCaptureProgress> {
        return biometricRepository.observeSessionProgress(sessionId)
    }
}

class SelectCaptureSourceUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(source: CaptureSource) {
        biometricRepository.selectCaptureSource(source)
    }
}

class RequestUsbPermissionUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke() {
        biometricRepository.requestUsbPermission()
    }
}

class SelectUsbDeviceUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(deviceId: Int) {
        biometricRepository.selectUsbDevice(deviceId)
    }
}

class ObservePendingCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    operator fun invoke(sessionId: String): Flow<CapturePreview?> {
        return biometricRepository.observePendingCapture(sessionId)
    }
}

class OpenPendingCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String): OpenedArtifact? {
        return biometricRepository.openPendingCapture(sessionId)
    }
}

class StartBiometricSessionUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(babyId: String, operatorId: String): String {
        return biometricRepository.startSession(babyId, operatorId)
    }
}

class GenerateCapturePreviewUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String, fingerCode: String): CapturePreview {
        return biometricRepository.generateCapturePreview(sessionId, fingerCode)
    }
}

class AcceptCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String): SessionCaptureProgress {
        return biometricRepository.acceptPendingCapture(sessionId)
    }
}

class DiscardCaptureUseCase(
    private val biometricRepository: BiometricRepository,
) {
    suspend operator fun invoke(sessionId: String) {
        biometricRepository.discardPendingCapture(sessionId)
    }
}

