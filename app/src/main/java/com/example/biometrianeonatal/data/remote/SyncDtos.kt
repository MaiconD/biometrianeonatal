package com.example.biometrianeonatal.data.remote

data class SyncBatchPayloadDto(
    val babies: List<RemoteBabyDto>,
    val guardians: List<RemoteGuardianDto>,
    val sessions: List<RemoteSessionDto>,
    val fingerprints: List<RemoteFingerprintDto>,
)

data class RemoteBabyDto(
    val id: String,
    val name: String,
    val birthDate: String,
    val birthTime: String,
    val sex: String,
    val weightGrams: String,
    val heightCm: String,
    val hospitalId: String,
    val medicalRecord: String,
    val observations: String,
    val updatedAt: String,
    val deletedAt: String?,
)

data class RemoteGuardianDto(
    val id: String,
    val babyId: String,
    val name: String,
    val document: String,
    val phone: String,
    val relation: String,
    val addressCity: String,
    val addressState: String,
    val addressLine: String,
    val consentFileName: String? = null,
    val consentAcceptedAt: String? = null,
    val signatureBase64: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

data class RemoteSessionDto(
    val id: String,
    val babyId: String,
    val operatorId: String,
    val sessionDate: String,
    val deviceId: String,
    val sensorSerial: String,
    val lifecycleStatus: String,
    val syncStatus: String,
    val notes: String,
)

data class RemoteFingerprintDto(
    val id: String,
    val sessionId: String,
    val fingerCode: String,
    val imagePath: String,
    val templateBase64: String,
    val qualityScore: Int,
    val fps: Int,
    val resolution: String,
    val capturedAt: String,
)

