package com.example.biometrianeonatal.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class SyncResponseDto(
    val syncedCount: Int,
)

interface SyncApiService {
    @POST("api/v1/sync")
    suspend fun sync(@Body payload: SyncBatchPayloadDto): SyncResponseDto
}

