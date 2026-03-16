package com.example.biometrianeonatal.data.remote

/**
 * Enumeracao `RemoteSyncTransport` usada para restringir valores validos do dominio.
 */
enum class RemoteSyncTransport {
    API,
    FALLBACK,
}

/**
 * Tipo `RemoteSyncResult` que organiza dados ou comportamento desta camada.
 */
data class RemoteSyncResult(
    val syncedCount: Int,
    val transport: RemoteSyncTransport,
    val message: String? = null,
)

/**
 * Interface `SyncRemoteDataSource` que define um contrato reutilizado por outras camadas.
 */
interface SyncRemoteDataSource {
    suspend fun pushBatch(payload: SyncBatchPayloadDto): RemoteSyncResult
}

