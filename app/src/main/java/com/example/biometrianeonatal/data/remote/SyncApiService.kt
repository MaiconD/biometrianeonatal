package com.example.biometrianeonatal.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Tipo `SyncResponseDto` que organiza dados ou comportamento desta camada.
 */
data class SyncResponseDto(
    val syncedCount: Int,
)

/**
 * Interface `SyncApiService` que define um contrato reutilizado por outras camadas.
 */
interface SyncApiService {
    @POST("api/v1/sync")
    suspend fun sync(@Body payload: SyncBatchPayloadDto): SyncResponseDto
}

