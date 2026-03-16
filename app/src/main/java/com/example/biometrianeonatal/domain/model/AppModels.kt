package com.example.biometrianeonatal.domain.model

import com.example.biometrianeonatal.core.database.GuardianRelation
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.database.Sex
import com.example.biometrianeonatal.core.database.SyncStatus
import com.example.biometrianeonatal.core.database.UserRole

/**
 * Tipo `Hospital` que organiza dados ou comportamento desta camada.
 */
data class Hospital(
    val id: String,
    val name: String,
    val city: String,
    val state: String,
)

/**
 * Tipo `AuthUser` que organiza dados ou comportamento desta camada.
 */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val hospitalId: String,
)

/**
 * Tipo `DashboardSummary` que organiza dados ou comportamento desta camada.
 */
data class DashboardSummary(
    val babiesToday: Int,
    val collectionsToday: Int,
    val pendingSync: Int,
)

/**
 * Tipo `BabyListItem` que organiza dados ou comportamento desta camada.
 */
data class BabyListItem(
    val id: String,
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val hospitalName: String,
    val status: SyncStatus,
)

/**
 * Tipo `BabyDraft` que organiza dados ou comportamento desta camada.
 */
data class BabyDraft(
    val id: String? = null,
    val hospitalId: String = "",
    val name: String = "",
    val birthDate: String = "",
    val birthTime: String = "",
    val sex: Sex = Sex.NAO_INFORMADO,
    val weightGrams: String = "",
    val heightCm: String = "",
    val medicalRecord: String = "",
    val observations: String = "",
)

/**
 * Tipo `GuardianDraft` que organiza dados ou comportamento desta camada.
 */
data class GuardianDraft(
    val id: String? = null,
    val name: String = "",
    val document: String = "",
    val phone: String = "",
    val relation: GuardianRelation,
    val addressCity: String = "",
    val addressState: String = "",
    val addressLine: String = "",
    val signatureBase64: String? = null,
)

/**
 * Tipo `ConsentAttachmentInput` que organiza dados ou comportamento desta camada.
 */
class ConsentAttachmentInput(
    val originalFileName: String,
    val contentBytes: ByteArray,
    val sourceUri: String? = null,
)

/**
 * Enumeracao `ArtifactIntegrityStatus` usada para restringir valores validos do dominio.
 */
enum class ArtifactIntegrityStatus {
    NOT_ATTACHED,
    READY_TO_ENCRYPT,
    VERIFIED,
    MISSING,
    CHECKSUM_MISMATCH,
    READ_ERROR,
}

/**
 * Tipo `ArtifactInspection` que organiza dados ou comportamento desta camada.
 */
data class ArtifactInspection(
    val fileName: String = "",
    val artifactUri: String = "",
    val checksumSha256: String = "",
    val sizeBytes: Int = 0,
    val status: ArtifactIntegrityStatus = ArtifactIntegrityStatus.NOT_ATTACHED,
    val message: String? = null,
)

/**
 * Tipo `OpenedArtifact` que organiza dados ou comportamento desta camada.
 */
data class OpenedArtifact(
    val fileName: String,
    val contentUri: String,
    val mimeType: String,
)

/**
 * Enumeracao `CaptureSource` usada para restringir valores validos do dominio.
 */
enum class CaptureSource {
    DEMO_LOCAL,
    TABLET_CAMERA,
    USB_SENSOR,
}

/**
 * Tipo `CaptureSourceOption` que organiza dados ou comportamento desta camada.
 */
data class CaptureSourceOption(
    val source: CaptureSource,
    val label: String,
    val isAvailable: Boolean,
    val detailMessage: String? = null,
)

/**
 * Tipo `UsbDeviceOption` que organiza dados ou comportamento desta camada.
 */
data class UsbDeviceOption(
    val deviceId: Int,
    val label: String,
    val isSelected: Boolean,
    val hasPermission: Boolean,
)

/**
 * Tipo `SessionContext` que organiza dados ou comportamento desta camada.
 */
data class SessionContext(
    val babyName: String,
    val babyAgeLabel: String,
    val operatorName: String,
    val hospitalName: String,
    val sensorName: String,
    val sensorVersion: String,
    val sensorStatusLabel: String = "Pronto",
    val sensorTransport: String = "local",
    val supportsLivePreview: Boolean = false,
)

/**
 * Tipo `SensorRuntimeInfo` que organiza dados ou comportamento desta camada.
 */
data class SensorRuntimeInfo(
    val sensorName: String = "Sensor indisponível",
    val sensorVersion: String = "-",
    val sensorSerial: String = "-",
    val manufacturer: String = "-",
    val connectionState: String = "Desconhecido",
    val transport: String = "local",
    val selectedSource: CaptureSource = CaptureSource.DEMO_LOCAL,
    val availableSources: List<CaptureSourceOption> = emptyList(),
    val usbDevices: List<UsbDeviceOption> = emptyList(),
    val usbPermissionRequired: Boolean = false,
    val isCaptureReady: Boolean = false,
    val supportsLivePreview: Boolean = false,
    val supportsUsbOtg: Boolean = false,
    val supportsNativeProcessing: Boolean = false,
    val lastErrorMessage: String? = null,
)

/**
 * Tipo `CapturePreview` que organiza dados ou comportamento desta camada.
 */
data class CapturePreview(
    val sessionId: String,
    val fingerCode: String,
    val qualityScore: Int,
    val resolution: String,
    val fps: Int,
    val imagePath: String,
    val imageChecksumSha256: String,
    val templateBase64: String,
    val captureSource: CaptureSource = CaptureSource.DEMO_LOCAL,
    val originalFileName: String = "",
)

val DemoRequiredFingerCodes = listOf(
    "POLEGAR_DIREITO",
    "INDICADOR_DIREITO",
    "POLEGAR_ESQUERDO",
)

/**
 * Modelo que calcula progresso, proximo dedo sugerido e conclusao da sessao biometrica.
 */
data class SessionCaptureProgress(
    val sessionId: String = "",
    val requiredFingerCodes: List<String> = DemoRequiredFingerCodes,
    val completedFingerCodes: List<String> = emptyList(),
) {
    private val uniqueCompletedFingerCodes: List<String>
        get() = completedFingerCodes.distinct()

    val remainingFingerCodes: List<String>
        get() = requiredFingerCodes.filterNot { it in uniqueCompletedFingerCodes }

    val nextSuggestedFingerCode: String?
        get() = remainingFingerCodes.firstOrNull()

    val completedCount: Int
        get() = requiredFingerCodes.count { it in uniqueCompletedFingerCodes }

    val totalRequiredCount: Int
        get() = requiredFingerCodes.size

    val isComplete: Boolean
        get() = sessionId.isNotBlank() && remainingFingerCodes.isEmpty()
}

/**
 * Resumo agregado do perfil do bebe com responsaveis, consentimento e ultima sessao biometrica.
 */
data class BabyProfileSummary(
    val baby: BabyDraft,
    val guardiansCount: Int = 0,
    val guardiansWithConsentCount: Int = 0,
    val consentAcceptedAt: String? = null,
    val latestSessionId: String? = null,
    val latestSessionDate: String? = null,
    val latestSessionStatus: SessionLifecycleStatus? = null,
    val latestSessionSyncStatus: SyncStatus? = null,
    val latestSessionProgress: SessionCaptureProgress = SessionCaptureProgress(),
) {
    val guardiansPendingConsentCount: Int
        get() = (guardiansCount - guardiansWithConsentCount).coerceAtLeast(0)

    val hasGuardians: Boolean
        get() = guardiansCount > 0

    val hasRequiredConsent: Boolean
        get() = hasGuardians && guardiansWithConsentCount >= guardiansCount

    val hasLatestSession: Boolean
        get() = !latestSessionId.isNullOrBlank()

    val isReadyForCollection: Boolean
        get() = hasRequiredConsent

    val shouldResumeCollection: Boolean
        get() = latestSessionStatus == SessionLifecycleStatus.IN_PROGRESS &&
            hasLatestSession &&
            !latestSessionProgress.isComplete
}

/**
 * Tipo `SessionListItem` que organiza dados ou comportamento desta camada.
 */
data class SessionListItem(
    val id: String,
    val babyName: String,
    val sessionDate: String,
    val status: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
)

/**
 * Tipo `SessionCaptureRecord` que organiza dados ou comportamento desta camada.
 */
data class SessionCaptureRecord(
    val id: String,
    val fingerCode: String,
    val qualityScore: Int,
    val fps: Int,
    val resolution: String,
    val capturedAt: String,
    val imagePath: String,
    val imageChecksumSha256: String,
)

/**
 * Tipo `SessionHistoryDetail` que organiza dados ou comportamento desta camada.
 */
data class SessionHistoryDetail(
    val id: String,
    val babyId: String,
    val babyName: String,
    val operatorName: String,
    val hospitalName: String,
    val sessionDate: String,
    val deviceId: String,
    val sensorSerial: String,
    val status: SessionLifecycleStatus,
    val syncStatus: SyncStatus,
    val notes: String,
    val progress: SessionCaptureProgress = SessionCaptureProgress(),
    val captures: List<SessionCaptureRecord> = emptyList(),
)

/**
 * Tipo `PendingSyncItem` que organiza dados ou comportamento desta camada.
 */
data class PendingSyncItem(
    val sessionId: String,
    val babyName: String,
    val sessionLabel: String,
    val syncStatus: SyncStatus,
)

/**
 * Enumeracao `SyncExecutionStatus` usada para restringir valores validos do dominio.
 */
enum class SyncExecutionStatus {
    SUCCESS,
    FALLBACK_SUCCESS,
    ERROR,
}

/**
 * Tipo `SyncExecutionResult` que organiza dados ou comportamento desta camada.
 */
data class SyncExecutionResult(
    val status: SyncExecutionStatus,
    val syncedCount: Int,
    val sourceLabel: String,
    val message: String? = null,
)

