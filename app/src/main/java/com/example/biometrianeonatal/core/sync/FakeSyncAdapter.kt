package com.example.biometrianeonatal.core.sync

import com.example.biometrianeonatal.data.remote.SyncBatchPayloadDto
import com.example.biometrianeonatal.data.remote.RemoteSyncResult
import com.example.biometrianeonatal.data.remote.RemoteSyncTransport
import com.example.biometrianeonatal.data.remote.SyncRemoteDataSource
import kotlinx.coroutines.delay

class FakeSyncAdapter : SyncRemoteDataSource {
    override suspend fun pushBatch(payload: SyncBatchPayloadDto): RemoteSyncResult {
        delay(900)
        return RemoteSyncResult(
            syncedCount = payload.babies.size + payload.guardians.size + payload.sessions.size + payload.fingerprints.size,
            transport = RemoteSyncTransport.FALLBACK,
            message = "Sincronização simulada em ambiente local.",
        )
    }
}

