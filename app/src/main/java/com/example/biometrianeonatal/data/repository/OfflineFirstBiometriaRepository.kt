package com.example.biometrianeonatal.data.repository

import androidx.room.withTransaction
import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.AuditLogEntity
import com.example.biometrianeonatal.core.database.BabyEntity
import com.example.biometrianeonatal.core.database.BiometricSessionEntity
import com.example.biometrianeonatal.core.database.FingerprintEntity
import com.example.biometrianeonatal.core.database.GuardianEntity
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.database.SyncQueueEntity
import com.example.biometrianeonatal.core.database.SyncStatus
import com.example.biometrianeonatal.core.database.UserEntity
import com.example.biometrianeonatal.core.security.EncryptedArtifactStore
import com.example.biometrianeonatal.core.security.LocalCryptoService
import com.example.biometrianeonatal.core.security.StoredArtifact
import com.example.biometrianeonatal.core.security.SessionStore
import com.example.biometrianeonatal.core.sync.SyncCoordinator
import com.example.biometrianeonatal.core.sensors.SensorCapturePort
import com.example.biometrianeonatal.data.remote.AuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.AuthenticatedRemoteSession
import com.example.biometrianeonatal.data.remote.toUserRole
import com.example.biometrianeonatal.domain.model.AuthUser
import com.example.biometrianeonatal.domain.model.ArtifactInspection
import com.example.biometrianeonatal.domain.model.ArtifactIntegrityStatus
import com.example.biometrianeonatal.domain.model.BabyDraft
import com.example.biometrianeonatal.domain.model.BabyListItem
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.CaptureSource
import com.example.biometrianeonatal.domain.model.CaptureSourceOption
import com.example.biometrianeonatal.domain.model.ConsentAttachmentInput
import com.example.biometrianeonatal.domain.model.CapturePreview
import com.example.biometrianeonatal.domain.model.DemoRequiredFingerCodes
import com.example.biometrianeonatal.domain.model.DashboardSummary
import com.example.biometrianeonatal.domain.model.GuardianDraft
import com.example.biometrianeonatal.domain.model.Hospital
import com.example.biometrianeonatal.domain.model.OpenedArtifact
import com.example.biometrianeonatal.domain.model.PendingSyncItem
import com.example.biometrianeonatal.domain.model.SensorRuntimeInfo
import com.example.biometrianeonatal.domain.model.SessionCaptureRecord
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress
import com.example.biometrianeonatal.domain.model.SessionContext
import com.example.biometrianeonatal.domain.model.SessionHistoryDetail
import com.example.biometrianeonatal.domain.model.SessionListItem
import com.example.biometrianeonatal.domain.model.SyncExecutionResult
import com.example.biometrianeonatal.domain.model.UsbDeviceOption
import com.example.biometrianeonatal.domain.repository.AuthRepository
import com.example.biometrianeonatal.domain.repository.BabyRepository
import com.example.biometrianeonatal.domain.repository.BiometricRepository
import com.example.biometrianeonatal.domain.repository.DashboardRepository
import com.example.biometrianeonatal.domain.repository.HistoryRepository
import com.example.biometrianeonatal.domain.repository.SyncRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstBiometriaRepository(
    private val database: AppDatabase,
    private val localCryptoService: LocalCryptoService,
    private val encryptedArtifactStore: EncryptedArtifactStore,
    private val sessionStore: SessionStore,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val sensorAdapter: SensorCapturePort,
) : AuthRepository, DashboardRepository, HistoryRepository, BabyRepository, BiometricRepository, SyncRepository {
    private val pendingCaptures = MutableStateFlow<Map<String, CapturePreview>>(emptyMap())
    private val currentSessionUserId = MutableStateFlow(sessionStore.getCurrentUserId())
    private var pendingRemoteSession: AuthenticatedRemoteSession? = null

    override fun observeHospitals(): Flow<List<Hospital>> {
        return database.hospitalDao().observeAll().map { items ->
            items.map { hospital ->
                Hospital(
                    id = hospital.id,
                    name = hospital.name,
                    city = hospital.city,
                    state = hospital.state,
                )
            }
        }
    }

    override fun observeUser(userId: String): Flow<AuthUser?> {
        return database.userDao().observeById(userId).map { it?.toDomain() }
    }

    override fun observeCurrentSession(): Flow<AuthUser?> {
        return currentSessionUserId.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(null)
            } else {
                observeUser(userId)
            }
        }
    }

    override suspend fun login(email: String, password: String, hospitalId: String): AuthUser? {
        val remoteSession = authRemoteDataSource.login(email, password, hospitalId)
            ?: return null.also { pendingRemoteSession = null }
        pendingRemoteSession = remoteSession
        val localUser = upsertAuthenticatedUser(remoteSession)
        return localUser.toDomain()
    }

    override suspend fun persistSession(userId: String) {
        pendingRemoteSession?.takeIf { it.user.id == userId }?.let { remoteSession ->
            sessionStore.saveAuthSession(
                accessToken = remoteSession.accessToken,
                refreshToken = remoteSession.refreshToken,
                expiresAtEpochMillis = remoteSession.expiresAtEpochMillis,
            )
        }
        sessionStore.saveCurrentUserId(userId)
        currentSessionUserId.value = userId
        pendingRemoteSession = null
    }

    override suspend fun clearSession() {
        sessionStore.getRefreshToken()?.takeIf { it.isNotBlank() }?.let { refreshToken ->
            runCatching { authRemoteDataSource.logout(refreshToken) }
        }
        sessionStore.clear()
        currentSessionUserId.value = null
        pendingRemoteSession = null
    }

    override fun observeSummary(): Flow<DashboardSummary> {
        return combine(
            database.babyDao().observeBabiesTodayCount(),
            database.sessionDao().observeCollectionsTodayCount(),
            database.syncQueueDao().observePendingCount(),
        ) { babiesToday, collectionsToday, pendingSync ->
            DashboardSummary(
                babiesToday = babiesToday,
                collectionsToday = collectionsToday,
                pendingSync = pendingSync,
            )
        }
    }

    override fun observeSessions(): Flow<List<SessionListItem>> {
        return database.sessionDao().observeSessionHistory().map { items ->
            items.map { item ->
                SessionListItem(
                    id = item.id,
                    babyName = item.babyName,
                    sessionDate = item.sessionDate,
                    status = item.lifecycleStatus,
                    syncStatus = item.syncStatus,
                )
            }
        }
    }

    override fun observeSessionDetail(sessionId: String): Flow<SessionHistoryDetail?> {
        return combine(
            database.sessionDao().observeSessionHistoryDetail(sessionId),
            database.fingerprintDao().observeBySessionId(sessionId),
        ) { detail, captures ->
            detail?.let {
                SessionHistoryDetail(
                    id = it.id,
                    babyId = it.babyId,
                    babyName = it.babyName,
                    operatorName = it.operatorName,
                    hospitalName = it.hospitalName,
                    sessionDate = it.sessionDate,
                    deviceId = it.deviceId,
                    sensorSerial = it.sensorSerial,
                    status = it.lifecycleStatus,
                    syncStatus = it.syncStatus,
                    notes = it.notes,
                    progress = buildSessionCaptureProgress(
                        sessionId = it.id,
                        acceptedFingerCodes = captures.map { capture -> capture.fingerCode },
                    ),
                    captures = captures.map { capture ->
                        SessionCaptureRecord(
                            id = capture.id,
                            fingerCode = capture.fingerCode,
                            qualityScore = capture.qualityScore,
                            fps = capture.fps,
                            resolution = capture.resolution,
                            capturedAt = capture.capturedAt,
                            imagePath = capture.imagePath,
                            imageChecksumSha256 = capture.imageChecksumSha256,
                        )
                    },
                )
            }
        }
    }

    override fun observeSessionProgress(sessionId: String): Flow<SessionCaptureProgress> {
        return database.fingerprintDao().observeBySessionId(sessionId).map { captures ->
            buildSessionCaptureProgress(
                sessionId = sessionId,
                acceptedFingerCodes = captures.map { it.fingerCode },
            )
        }
    }

    override suspend fun openSessionCapture(fingerprintId: String): OpenedArtifact? {
        val fingerprint = database.fingerprintDao().getById(fingerprintId) ?: return null
        return encryptedArtifactStore.openArtifact(
            artifactUri = fingerprint.imagePath,
            expectedSha256 = fingerprint.imageChecksumSha256,
            originalFileName = buildHistoryCaptureFileName(fingerprint),
        )
    }

    override fun observeBabies(): Flow<List<BabyListItem>> {
        return database.babyDao().observeBabyListItems().map { items ->
            items.map {
                BabyListItem(
                    id = it.id,
                    name = it.name,
                    birthDate = it.birthDate,
                    birthTime = it.birthTime,
                    hospitalName = it.hospitalName,
                    status = it.status,
                )
            }
        }
    }

    override fun observeBaby(babyId: String): Flow<BabyDraft?> {
        return database.babyDao().observeById(babyId).map { entity ->
            entity?.let {
                BabyDraft(
                    id = it.id,
                    hospitalId = it.hospitalId,
                    name = it.name,
                    birthDate = it.birthDate,
                    birthTime = it.birthTime,
                    sex = it.sex,
                    weightGrams = it.weightGrams,
                    heightCm = it.heightCm,
                    medicalRecord = localCryptoService.decryptIfNeeded(it.medicalRecord),
                    observations = localCryptoService.decryptIfNeeded(it.observations),
                )
            }
        }
    }

    override fun observeBabySummary(babyId: String): Flow<BabyProfileSummary?> {
        return combine(
            observeBaby(babyId),
            observeGuardians(babyId),
            database.sessionDao().observeLatestSessionSummary(babyId),
        ) { baby, guardians, latestSession ->
            Triple(baby, guardians, latestSession)
        }.flatMapLatest { (baby, guardians, latestSession) ->
            if (baby == null) {
                flowOf(null)
            } else {
                val latestSessionId = latestSession?.id
                val captureFlow = latestSessionId?.let(database.fingerprintDao()::observeBySessionId)
                    ?: flowOf(emptyList())
                captureFlow.map { captures ->
                    BabyProfileSummary(
                        baby = baby,
                        guardiansCount = guardians.size,
                        guardiansWithConsentCount = guardians.count { !it.signatureBase64.isNullOrBlank() },
                        consentAcceptedAt = null, // Consent date no longer tracked separately
                        latestSessionId = latestSessionId,
                        latestSessionDate = latestSession?.sessionDate,
                        latestSessionStatus = latestSession?.lifecycleStatus,
                        latestSessionSyncStatus = latestSession?.syncStatus,
                        latestSessionProgress = buildSessionCaptureProgress(
                            sessionId = latestSessionId.orEmpty(),
                            acceptedFingerCodes = captures.map { it.fingerCode },
                        ),
                    )
                }
            }
        }
    }

    override fun observeGuardians(babyId: String): Flow<List<GuardianDraft>> {
        return database.guardianDao().observeByBabyId(babyId).map { items ->
            items.map {
                GuardianDraft(
                    id = it.id,
                    name = it.name,
                    document = localCryptoService.decryptIfNeeded(it.document),
                    phone = localCryptoService.decryptIfNeeded(it.phone),
                    relation = it.relation,
                    addressCity = localCryptoService.decryptIfNeeded(it.addressCity),
                    addressState = localCryptoService.decryptIfNeeded(it.addressState),
                    addressLine = localCryptoService.decryptIfNeeded(it.addressLine),
                    signatureBase64 = it.signatureBase64?.let { sig -> localCryptoService.decryptIfNeeded(sig) },
                )
            }
        }
    }


    override suspend fun saveBaby(draft: BabyDraft): String {
        val babyId = draft.id ?: UUID.randomUUID().toString()
        val now = LocalDateTime.now().toString()
        val existing = database.babyDao().getById(babyId)
        database.withTransaction {
            database.babyDao().insert(
                BabyEntity(
                    id = babyId,
                    name = draft.name,
                    birthDate = draft.birthDate,
                    birthTime = draft.birthTime,
                    sex = draft.sex,
                    weightGrams = draft.weightGrams,
                    heightCm = draft.heightCm,
                    hospitalId = draft.hospitalId,
                    medicalRecord = localCryptoService.encrypt(draft.medicalRecord),
                    observations = localCryptoService.encrypt(draft.observations),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = null,
                )
            )
            database.syncQueueDao().insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "baby",
                    entityId = babyId,
                    status = SyncStatus.PENDING,
                    createdAt = now,
                    lastAttemptAt = null,
                    errorMessage = null,
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorUserId = "system",
                    action = if (draft.id == null) "CREATE_BABY" else "UPDATE_BABY",
                    targetEntity = "baby",
                    targetEntityId = babyId,
                    createdAt = now,
                )
            )
        }
        return babyId
    }

    override suspend fun saveGuardians(
        babyId: String,
        guardians: List<GuardianDraft>,
    ) {
        val now = LocalDateTime.now().toString()
        val existingGuardians = database.guardianDao().getActiveByBabyId(babyId)
        val existingById = existingGuardians.associateBy { it.id }
        val upsertGuardians = guardians.filter { it.name.isNotBlank() }
        
        database.withTransaction {
            val persistedIds = mutableSetOf<String>()
            database.guardianDao().insertAll(
                upsertGuardians.map { guardian ->
                    val guardianId = guardian.id ?: UUID.randomUUID().toString()
                    persistedIds += guardianId
                    val existing = existingById[guardianId]
                    GuardianEntity(
                        id = guardianId,
                        babyId = babyId,
                        name = guardian.name,
                        document = localCryptoService.encrypt(guardian.document),
                        phone = localCryptoService.encrypt(guardian.phone),
                        relation = guardian.relation,
                        addressCity = localCryptoService.encrypt(guardian.addressCity),
                        addressState = localCryptoService.encrypt(guardian.addressState),
                        addressLine = localCryptoService.encrypt(guardian.addressLine),
                        signatureBase64 = guardian.signatureBase64?.let { localCryptoService.encrypt(it) },
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                        deletedAt = null,
                    )
                }
            )
            val deletedIds = existingGuardians.map { it.id }.filterNot { it in persistedIds }
            if (deletedIds.isNotEmpty()) {
                database.guardianDao().softDeleteByIds(
                    guardianIds = deletedIds,
                    updatedAt = now,
                    deletedAt = now,
                )
            }
            database.syncQueueDao().insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "guardians",
                    entityId = babyId,
                    status = SyncStatus.PENDING,
                    createdAt = now,
                    lastAttemptAt = null,
                    errorMessage = null,
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorUserId = "system",
                    action = "UPSERT_GUARDIANS",
                    targetEntity = "baby",
                    targetEntityId = babyId,
                    createdAt = now,
                )
            )
        }
    }

    override suspend fun deleteBaby(babyId: String) {
        val now = LocalDateTime.now().toString()
        database.withTransaction {
            database.babyDao().softDelete(
                babyId = babyId,
                updatedAt = now,
                deletedAt = now,
            )
            database.guardianDao().softDeleteByBabyId(
                babyId = babyId,
                updatedAt = now,
                deletedAt = now,
            )
            database.syncQueueDao().insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "baby",
                    entityId = babyId,
                    status = SyncStatus.PENDING,
                    createdAt = now,
                    lastAttemptAt = null,
                    errorMessage = null,
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorUserId = "system",
                    action = "DELETE_BABY",
                    targetEntity = "baby",
                    targetEntityId = babyId,
                    createdAt = now,
                )
            )
        }
    }

    override fun observeSessionContext(babyId: String, operatorId: String): Flow<SessionContext?> {
        return combine(
            database.sessionDao().observeSessionContext(babyId, operatorId),
            observeSensorRuntime(),
        ) { projection, sensorRuntime ->
            projection?.let {
                SessionContext(
                    babyName = it.babyName,
                    babyAgeLabel = buildAgeLabel(it.birthDate),
                    operatorName = it.operatorName,
                    hospitalName = it.hospitalName,
                    sensorName = sensorRuntime.sensorName,
                    sensorVersion = sensorRuntime.sensorVersion,
                    sensorStatusLabel = sensorRuntime.connectionState,
                    sensorTransport = sensorRuntime.transport,
                    supportsLivePreview = sensorRuntime.supportsLivePreview,
                )
            }
        }
    }

    override fun observeSensorRuntime(): Flow<SensorRuntimeInfo> {
        return sensorAdapter.observeRuntimeSnapshot().map { runtimeSnapshot ->
            SensorRuntimeInfo(
                sensorName = runtimeSnapshot.deviceMetadata.name,
                sensorVersion = runtimeSnapshot.deviceMetadata.version,
                sensorSerial = runtimeSnapshot.deviceMetadata.serial,
                manufacturer = runtimeSnapshot.deviceMetadata.manufacturer,
                connectionState = runtimeSnapshot.connectionState.toUiLabel(),
                transport = runtimeSnapshot.capabilities.transport,
                selectedSource = runtimeSnapshot.selectedSource.toDomain(),
                availableSources = runtimeSnapshot.availableSources.map { sourceOption ->
                    CaptureSourceOption(
                        source = sourceOption.source.toDomain(),
                        label = sourceOption.label,
                        isAvailable = sourceOption.isAvailable,
                        detailMessage = sourceOption.detailMessage,
                    )
                },
                usbDevices = runtimeSnapshot.availableUsbDevices.map { usbDevice ->
                    UsbDeviceOption(
                        deviceId = usbDevice.deviceId,
                        label = usbDevice.label,
                        isSelected = usbDevice.isSelected,
                        hasPermission = usbDevice.hasPermission,
                    )
                },
                usbPermissionRequired = runtimeSnapshot.usbPermissionRequired,
                isCaptureReady = runtimeSnapshot.isCaptureReady,
                supportsLivePreview = runtimeSnapshot.capabilities.supportsLivePreview,
                supportsUsbOtg = runtimeSnapshot.capabilities.supportsUsbOtg,
                supportsNativeProcessing = runtimeSnapshot.capabilities.supportsNativeProcessing,
                lastErrorMessage = runtimeSnapshot.lastErrorMessage,
            )
        }
    }

    override suspend fun selectCaptureSource(source: CaptureSource) {
        sensorAdapter.selectCaptureSource(source.toInfrastructure())
    }

    override suspend fun requestUsbPermission() {
        sensorAdapter.requestUsbPermission()
    }

    override suspend fun selectUsbDevice(deviceId: Int) {
        sensorAdapter.selectUsbDevice(deviceId)
    }

    override suspend fun startSession(babyId: String, operatorId: String): String {
        val sessionId = UUID.randomUUID().toString()
        val now = LocalDateTime.now().toString()
        val sensorRuntime = observeSensorRuntime().first()
        database.withTransaction {
            database.sessionDao().insertSession(
                BiometricSessionEntity(
                    id = sessionId,
                    babyId = babyId,
                    operatorId = operatorId,
                    sessionDate = now,
                    deviceId = sensorRuntime.selectedSource.name,
                    sensorSerial = sensorRuntime.sensorSerial,
                    lifecycleStatus = SessionLifecycleStatus.IN_PROGRESS,
                    syncStatus = SyncStatus.PENDING,
                    notes = "Sessão iniciada pelo operador. Fonte: ${sensorRuntime.selectedSource.name} (${sensorRuntime.sensorName}).",
                )
            )
            database.syncQueueDao().insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "session",
                    entityId = sessionId,
                    status = SyncStatus.PENDING,
                    createdAt = now,
                    lastAttemptAt = null,
                    errorMessage = null,
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorUserId = operatorId,
                    action = "START_SESSION",
                    targetEntity = "session",
                    targetEntityId = sessionId,
                    createdAt = now,
                )
            )
        }
        return sessionId
    }

    override suspend fun generateCapturePreview(sessionId: String, fingerCode: String): CapturePreview {
        val captureSource = sensorAdapter.getSelectedCaptureSource().toDomain()
        val originalFileName = when (captureSource) {
            CaptureSource.TABLET_CAMERA -> "$fingerCode.jpg"
            CaptureSource.DEMO_LOCAL -> "$fingerCode.txt"
            CaptureSource.USB_SENSOR -> "$fingerCode.bin"
        }
        val sensorCapture = sensorAdapter.capture(sessionId, fingerCode)
        val storedCapture = encryptedArtifactStore.saveCaptureArtifact(
            sessionId = sensorCapture.sessionId,
            fingerCode = sensorCapture.fingerCode,
            originalFileName = originalFileName,
            content = sensorCapture.imageBytes,
        )
        return CapturePreview(
            sessionId = sensorCapture.sessionId,
            fingerCode = sensorCapture.fingerCode,
            qualityScore = sensorCapture.qualityScore,
            resolution = sensorCapture.resolution,
            fps = sensorCapture.fps,
            imagePath = storedCapture.artifactUri,
            imageChecksumSha256 = storedCapture.sha256,
            templateBase64 = sensorCapture.templateBase64,
            captureSource = captureSource,
            originalFileName = originalFileName,
        ).also { preview ->
            pendingCaptures.value = pendingCaptures.value + (sessionId to preview)
        }
    }

    override fun observePendingCapture(sessionId: String): Flow<CapturePreview?> {
        return pendingCaptures.map { it[sessionId] }
    }

    override suspend fun openPendingCapture(sessionId: String): OpenedArtifact? {
        val pendingCapture = pendingCaptures.value[sessionId] ?: return null
        return encryptedArtifactStore.openArtifact(
            artifactUri = pendingCapture.imagePath,
            expectedSha256 = pendingCapture.imageChecksumSha256,
            originalFileName = pendingCapture.originalFileName.ifBlank {
                when (pendingCapture.captureSource) {
                    CaptureSource.TABLET_CAMERA -> "${pendingCapture.fingerCode}.jpg"
                    CaptureSource.DEMO_LOCAL -> "${pendingCapture.fingerCode}.txt"
                    CaptureSource.USB_SENSOR -> "${pendingCapture.fingerCode}.bin"
                }
            },
        )
    }

    override suspend fun acceptPendingCapture(sessionId: String): SessionCaptureProgress {
        val pendingCapture = pendingCaptures.value[sessionId]
            ?: return buildSessionCaptureProgress(sessionId, emptyList())
        val now = LocalDateTime.now().toString()
        check(
            encryptedArtifactStore.verifyArtifact(
                artifactUri = pendingCapture.imagePath,
                expectedSha256 = pendingCapture.imageChecksumSha256,
            )
        ) {
            "Capture artifact integrity check failed for session $sessionId"
        }
        val progress = database.withTransaction {
            database.fingerprintDao().deleteBySessionAndFingerCode(sessionId, pendingCapture.fingerCode)
            database.fingerprintDao().insert(
                FingerprintEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    fingerCode = pendingCapture.fingerCode,
                    imagePath = pendingCapture.imagePath,
                    imageChecksumSha256 = pendingCapture.imageChecksumSha256,
                    templateBase64 = pendingCapture.templateBase64,
                    qualityScore = pendingCapture.qualityScore,
                    fps = pendingCapture.fps,
                    resolution = pendingCapture.resolution,
                    capturedAt = now,
                )
            )
            val updatedProgress = buildSessionCaptureProgress(
                sessionId = sessionId,
                acceptedFingerCodes = database.fingerprintDao().getFingerCodesBySessionId(sessionId),
            )
            database.sessionDao().updateSessionStatus(
                sessionId = sessionId,
                status = if (updatedProgress.isComplete) {
                    SessionLifecycleStatus.COMPLETED
                } else {
                    SessionLifecycleStatus.IN_PROGRESS
                },
                syncStatus = SyncStatus.PENDING,
            )
            database.syncQueueDao().insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "fingerprint",
                    entityId = sessionId,
                    status = SyncStatus.PENDING,
                    createdAt = now,
                    lastAttemptAt = null,
                    errorMessage = null,
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorUserId = "system",
                    action = "ACCEPT_CAPTURE_${pendingCapture.fingerCode}",
                    targetEntity = "session",
                    targetEntityId = sessionId,
                    createdAt = now,
                )
            )
            updatedProgress
        }
        pendingCaptures.value = pendingCaptures.value - sessionId
        return progress
    }

    override suspend fun discardPendingCapture(sessionId: String) {
        pendingCaptures.value[sessionId]?.imagePath?.let { artifactUri ->
            encryptedArtifactStore.deleteArtifact(artifactUri)
        }
        pendingCaptures.value = pendingCaptures.value - sessionId
    }


    override fun observePendingSyncItems(): Flow<List<PendingSyncItem>> {
        return database.sessionDao().observePendingSyncSessions().map { items ->
            items.map { item ->
                PendingSyncItem(
                    sessionId = item.sessionId,
                    babyName = item.babyName,
                    sessionLabel = buildSessionLabel(item.sessionId, item.sessionDate),
                    syncStatus = item.syncStatus,
                )
            }
        }
    }

    override suspend fun syncNow(): SyncExecutionResult {
        return syncCoordinator.syncNow()
    }

    private fun UserEntity.toDomain(): AuthUser {
        return AuthUser(
            id = id,
            name = name,
            email = email,
            role = role,
            hospitalId = hospitalId,
        )
    }

    private suspend fun upsertAuthenticatedUser(remoteSession: AuthenticatedRemoteSession): UserEntity {
        val userDto = remoteSession.user
        val persistedUser = UserEntity(
            id = userDto.id,
            name = userDto.name,
            email = userDto.email,
            password = database.userDao().getById(userDto.id)?.password ?: "REMOTE_AUTH_ONLY",
            role = userDto.toUserRole(),
            hospitalId = userDto.hospitalId,
        )
        database.userDao().insertAll(listOf(persistedUser))
        return persistedUser
    }

    private fun buildAgeLabel(birthDate: String): String {
        val days = ChronoUnit.DAYS.between(LocalDate.parse(birthDate), LocalDate.now())
        return when {
            days <= 0 -> "Bebê • hoje"
            days == 1L -> "Bebê • 1 dia de vida"
            else -> "Bebê • $days dias de vida"
        }
    }

    private fun buildSessionLabel(sessionId: String, sessionDate: String): String {
        return "Sessão #${sessionId.takeLast(4)} • ${sessionDate.replace('T', ' ').take(16)}"
    }

    private fun buildSessionCaptureProgress(
        sessionId: String,
        acceptedFingerCodes: List<String>,
    ): SessionCaptureProgress {
        return SessionCaptureProgress(
            sessionId = sessionId,
            requiredFingerCodes = DemoRequiredFingerCodes,
            completedFingerCodes = acceptedFingerCodes,
        )
    }

    private fun buildHistoryCaptureFileName(fingerprint: com.example.biometrianeonatal.core.database.FingerprintEntity): String {
        val extension = when {
            fingerprint.resolution.startsWith("CameraX/", ignoreCase = true) -> "jpg"
            fingerprint.resolution.startsWith("USB-UVC/", ignoreCase = true) -> "bin"
            else -> "txt"
        }
        return "${fingerprint.fingerCode.lowercase()}.$extension"
    }

    private fun com.example.biometrianeonatal.core.sensors.SensorConnectionState.toUiLabel(): String {
        return when (this) {
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.DISCONNECTED -> "Desconectado"
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.WAITING_PERMISSION -> "Aguardando permissão"
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.READY -> "Pronto"
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.PREVIEW_ACTIVE -> "Preview ativo"
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.CAPTURING -> "Capturando"
            com.example.biometrianeonatal.core.sensors.SensorConnectionState.ERROR -> "Erro"
        }
    }

    private fun com.example.biometrianeonatal.core.sensors.SensorCaptureSource.toDomain(): CaptureSource {
        return when (this) {
            com.example.biometrianeonatal.core.sensors.SensorCaptureSource.DEMO_LOCAL -> CaptureSource.DEMO_LOCAL
            com.example.biometrianeonatal.core.sensors.SensorCaptureSource.TABLET_CAMERA -> CaptureSource.TABLET_CAMERA
            com.example.biometrianeonatal.core.sensors.SensorCaptureSource.USB_SENSOR -> CaptureSource.USB_SENSOR
        }
    }

    private fun CaptureSource.toInfrastructure(): com.example.biometrianeonatal.core.sensors.SensorCaptureSource {
        return when (this) {
            CaptureSource.DEMO_LOCAL -> com.example.biometrianeonatal.core.sensors.SensorCaptureSource.DEMO_LOCAL
            CaptureSource.TABLET_CAMERA -> com.example.biometrianeonatal.core.sensors.SensorCaptureSource.TABLET_CAMERA
            CaptureSource.USB_SENSOR -> com.example.biometrianeonatal.core.sensors.SensorCaptureSource.USB_SENSOR
        }
    }
}


