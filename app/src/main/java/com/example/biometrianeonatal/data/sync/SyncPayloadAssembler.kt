package com.example.biometrianeonatal.data.sync

import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.BabyEntity
import com.example.biometrianeonatal.core.database.FingerprintEntity
import com.example.biometrianeonatal.core.database.GuardianEntity
import com.example.biometrianeonatal.core.database.BiometricSessionEntity
import com.example.biometrianeonatal.core.database.SyncStatus
import com.example.biometrianeonatal.core.security.LocalCryptoService
import com.example.biometrianeonatal.data.remote.RemoteBabyDto
import com.example.biometrianeonatal.data.remote.RemoteFingerprintDto
import com.example.biometrianeonatal.data.remote.RemoteGuardianDto
import com.example.biometrianeonatal.data.remote.RemoteSessionDto
import com.example.biometrianeonatal.data.remote.SyncBatchPayloadDto

class SyncPayloadAssembler(
    private val database: AppDatabase,
    private val localCryptoService: LocalCryptoService,
) {
    suspend fun assemblePendingPayload(): SyncBatchPayloadDto {
        val babies = database.babyDao().getAllActive()
        val guardians = database.guardianDao().getAllActive()
        val sessions = database.sessionDao().getBySyncStatuses(
            listOf(SyncStatus.PENDING, SyncStatus.ERROR, SyncStatus.SYNCING),
        )
        val fingerprints = database.fingerprintDao().getBySessionIds(sessions.map { it.id })

        return SyncBatchPayloadDto(
            babies = babies.map(::toRemoteBabyDto),
            guardians = guardians.map(::toRemoteGuardianDto),
            sessions = sessions.map(::toRemoteSessionDto),
            fingerprints = fingerprints.map(::toRemoteFingerprintDto),
        )
    }

    private fun toRemoteBabyDto(entity: BabyEntity): RemoteBabyDto {
        return RemoteBabyDto(
            id = entity.id,
            name = entity.name,
            birthDate = entity.birthDate,
            birthTime = entity.birthTime,
            sex = entity.sex.name,
            weightGrams = entity.weightGrams,
            heightCm = entity.heightCm,
            hospitalId = entity.hospitalId,
            medicalRecord = localCryptoService.decryptIfNeeded(entity.medicalRecord),
            observations = localCryptoService.decryptIfNeeded(entity.observations),
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
        )
    }

    private fun toRemoteGuardianDto(entity: GuardianEntity): RemoteGuardianDto {
        return RemoteGuardianDto(
            id = entity.id,
            babyId = entity.babyId,
            name = entity.name,
            document = localCryptoService.decryptIfNeeded(entity.document),
            phone = localCryptoService.decryptIfNeeded(entity.phone),
            relation = entity.relation.name,
            addressCity = localCryptoService.decryptIfNeeded(entity.addressCity),
            addressState = localCryptoService.decryptIfNeeded(entity.addressState),
            addressLine = localCryptoService.decryptIfNeeded(entity.addressLine),
            signatureBase64 = entity.signatureBase64?.let(localCryptoService::decryptIfNeeded),
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
        )
    }

    private fun toRemoteSessionDto(entity: BiometricSessionEntity): RemoteSessionDto {
        return RemoteSessionDto(
            id = entity.id,
            babyId = entity.babyId,
            operatorId = entity.operatorId,
            sessionDate = entity.sessionDate,
            deviceId = entity.deviceId,
            sensorSerial = entity.sensorSerial,
            lifecycleStatus = entity.lifecycleStatus.name,
            syncStatus = entity.syncStatus.name,
            notes = entity.notes,
        )
    }

    private fun toRemoteFingerprintDto(entity: FingerprintEntity): RemoteFingerprintDto {
        return RemoteFingerprintDto(
            id = entity.id,
            sessionId = entity.sessionId,
            fingerCode = entity.fingerCode,
            imagePath = entity.imagePath,
            templateBase64 = entity.templateBase64,
            qualityScore = entity.qualityScore,
            fps = entity.fps,
            resolution = entity.resolution,
            capturedAt = entity.capturedAt,
        )
    }
}

