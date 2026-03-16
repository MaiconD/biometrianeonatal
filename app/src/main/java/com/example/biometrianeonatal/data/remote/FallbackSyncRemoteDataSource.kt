package com.example.biometrianeonatal.data.remote

import com.example.biometrianeonatal.core.config.AppRuntimeConfig

class FallbackSyncRemoteDataSource(
    private val appRuntimeConfig: AppRuntimeConfig,
    private val primary: SyncRemoteDataSource,
    private val fallback: SyncRemoteDataSource,
) : SyncRemoteDataSource {
    override suspend fun pushBatch(payload: SyncBatchPayloadDto): RemoteSyncResult {
        if (appRuntimeConfig.offlineDemoMode) {
            return fallback.pushBatch(payload)
        }
        return runCatching {
            primary.pushBatch(payload)
        }.getOrElse { throw it }
    }
}

