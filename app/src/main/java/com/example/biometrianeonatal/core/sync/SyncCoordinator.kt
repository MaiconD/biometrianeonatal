package com.example.biometrianeonatal.core.sync

import androidx.room.withTransaction
import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.AuditLogEntity
import com.example.biometrianeonatal.core.database.SyncStatus
import com.example.biometrianeonatal.data.remote.RemoteSyncTransport
import com.example.biometrianeonatal.data.remote.SyncRemoteDataSource
import com.example.biometrianeonatal.data.sync.SyncPayloadAssembler
import com.example.biometrianeonatal.domain.model.SyncExecutionResult
import com.example.biometrianeonatal.domain.model.SyncExecutionStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * Orquestrador da sincronizacao local-remota com tratamento de sucesso, fallback e falhas.
 */
class SyncCoordinator(
    private val database: AppDatabase,
    private val payloadAssembler: SyncPayloadAssembler,
    private val remoteDataSource: SyncRemoteDataSource,
) {
    suspend fun syncNow(): SyncExecutionResult {
        val now = LocalDateTime.now().toString()
        val pendingCount = database.syncQueueDao().pendingCount()
        if (pendingCount == 0) {
            return SyncExecutionResult(
                status = SyncExecutionStatus.SUCCESS,
                syncedCount = 0,
                sourceLabel = "fila_local",
                message = "Nenhum item pendente para sincronização.",
            )
        }

        database.withTransaction {
            database.sessionDao().updateSyncStatusForStatuses(
                fromStatuses = listOf(SyncStatus.PENDING, SyncStatus.ERROR),
                newStatus = SyncStatus.SYNCING,
            )
            database.syncQueueDao().updateStatuses(
                fromStatuses = listOf(SyncStatus.PENDING, SyncStatus.ERROR),
                newStatus = SyncStatus.SYNCING,
                attemptedAt = now,
                errorMessage = null,
            )
        }

        return runCatching {
            val payload = payloadAssembler.assemblePendingPayload()
            val remoteResult = remoteDataSource.pushBatch(payload)

            database.withTransaction {
                database.sessionDao().updateSyncStatusForStatuses(
                    fromStatuses = listOf(SyncStatus.SYNCING),
                    newStatus = SyncStatus.SYNCED,
                )
                database.syncQueueDao().updateStatuses(
                    fromStatuses = listOf(SyncStatus.SYNCING),
                    newStatus = SyncStatus.SYNCED,
                    attemptedAt = LocalDateTime.now().toString(),
                    errorMessage = null,
                )
                database.auditLogDao().insert(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        actorUserId = "system",
                        action = "SYNC_NOW",
                        targetEntity = "sync_queue",
                        targetEntityId = "batch",
                        createdAt = LocalDateTime.now().toString(),
                    )
                )
            }
            SyncExecutionResult(
                status = if (remoteResult.transport == RemoteSyncTransport.API) {
                    SyncExecutionStatus.SUCCESS
                } else {
                    SyncExecutionStatus.FALLBACK_SUCCESS
                },
                syncedCount = remoteResult.syncedCount,
                sourceLabel = if (remoteResult.transport == RemoteSyncTransport.API) "api_remota" else "fallback_local",
                message = remoteResult.message,
            )
        }.getOrElse { throwable ->
            database.withTransaction {
                database.sessionDao().updateSyncStatusForStatuses(
                    fromStatuses = listOf(SyncStatus.SYNCING),
                    newStatus = SyncStatus.ERROR,
                )
                database.syncQueueDao().updateStatuses(
                    fromStatuses = listOf(SyncStatus.SYNCING),
                    newStatus = SyncStatus.ERROR,
                    attemptedAt = LocalDateTime.now().toString(),
                    errorMessage = throwable.message,
                )
                database.auditLogDao().insert(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        actorUserId = "system",
                        action = "SYNC_ERROR",
                        targetEntity = "sync_queue",
                        targetEntityId = "batch",
                        createdAt = LocalDateTime.now().toString(),
                    )
                )
            }
            SyncExecutionResult(
                status = SyncExecutionStatus.ERROR,
                syncedCount = 0,
                sourceLabel = "erro",
                message = throwable.message ?: "Falha desconhecida durante a sincronização.",
            )
        }
    }
}

