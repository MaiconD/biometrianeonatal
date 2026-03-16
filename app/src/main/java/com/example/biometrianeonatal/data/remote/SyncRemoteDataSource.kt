package com.example.biometrianeonatal.data.remote

enum class RemoteSyncTransport {
    API,
    FALLBACK,
}

data class RemoteSyncResult(
    val syncedCount: Int,
    val transport: RemoteSyncTransport,
    val message: String? = null,
)

interface SyncRemoteDataSource {
    suspend fun pushBatch(payload: SyncBatchPayloadDto): RemoteSyncResult
}

