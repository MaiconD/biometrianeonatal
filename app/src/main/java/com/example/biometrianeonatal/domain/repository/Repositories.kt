package com.example.biometrianeonatal.domain.repository

import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.model.ArtifactInspection
import com.example.biometrianeonatal.domain.model.BabyDraft
import com.example.biometrianeonatal.domain.model.BabyListItem
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.CaptureSource
import com.example.biometrianeonatal.domain.model.ConsentAttachmentInput
import com.example.biometrianeonatal.domain.model.CapturePreview
import com.example.biometrianeonatal.domain.model.DashboardSummary
import com.example.biometrianeonatal.domain.model.GuardianDraft
import com.example.biometrianeonatal.domain.model.Hospital
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.PendingSyncItem
import com.example.biometrianeonatal.domain.model.SensorRuntimeInfo
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress
import com.example.biometrianeonatal.domain.model.SessionHistoryDetail
import com.example.biometrianeonatal.domain.model.SessionListItem
import com.example.biometrianeonatal.domain.model.SyncExecutionResult
import com.example.biometrianeonatal.domain.model.SessionContext
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeHospitals(): Flow<List<Hospital>>
    fun observeUser(userId: String): Flow<AuthUser?>
    fun observeCurrentSession(): Flow<AuthUser?>
    suspend fun login(email: String, password: String, hospitalId: String): AuthUser?
    suspend fun persistSession(userId: String)
    suspend fun clearSession()
}

interface DashboardRepository {
    fun observeSummary(): Flow<DashboardSummary>
}

interface HistoryRepository {
    fun observeSessions(): Flow<List<SessionListItem>>
    fun observeSessionDetail(sessionId: String): Flow<SessionHistoryDetail?>
    suspend fun openSessionCapture(fingerprintId: String): OpenedArtifact?
}

interface BabyRepository {
    fun observeBabies(): Flow<List<BabyListItem>>
    fun observeBaby(babyId: String): Flow<BabyDraft?>
    fun observeBabySummary(babyId: String): Flow<BabyProfileSummary?>
    fun observeGuardians(babyId: String): Flow<List<GuardianDraft>>
    suspend fun saveBaby(draft: BabyDraft): String
    suspend fun saveGuardians(
        babyId: String,
        guardians: List<GuardianDraft>,
    )
    suspend fun deleteBaby(babyId: String)
}

interface BiometricRepository {
    fun observeSessionContext(babyId: String, operatorId: String): Flow<SessionContext?>
    fun observeSensorRuntime(): Flow<SensorRuntimeInfo>
    fun observeSessionProgress(sessionId: String): Flow<SessionCaptureProgress>
    suspend fun selectCaptureSource(source: CaptureSource)
    suspend fun requestUsbPermission()
    suspend fun selectUsbDevice(deviceId: Int)
    suspend fun startSession(babyId: String, operatorId: String): String
    suspend fun generateCapturePreview(sessionId: String, fingerCode: String): CapturePreview
    fun observePendingCapture(sessionId: String): Flow<CapturePreview?>
    suspend fun openPendingCapture(sessionId: String): OpenedArtifact?
    suspend fun acceptPendingCapture(sessionId: String): SessionCaptureProgress
    suspend fun discardPendingCapture(sessionId: String)
}

interface SyncRepository {
    fun observePendingSyncItems(): Flow<List<PendingSyncItem>>
    suspend fun syncNow(): SyncExecutionResult
}

