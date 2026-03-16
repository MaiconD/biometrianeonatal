package com.example.biometrianeonatal.data.remote

/**
 * Tipo `RetrofitSyncRemoteDataSource` que organiza dados ou comportamento desta camada.
 */
class RetrofitSyncRemoteDataSource(
    private val apiService: SyncApiService,
) : SyncRemoteDataSource {
    override suspend fun pushBatch(payload: SyncBatchPayloadDto): RemoteSyncResult {
        return RemoteSyncResult(
            syncedCount = apiService.sync(payload).syncedCount,
            transport = RemoteSyncTransport.API,
            message = "Sincronização enviada para a API remota.",
        )
    }
}

