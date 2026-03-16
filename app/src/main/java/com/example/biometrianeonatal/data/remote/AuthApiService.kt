package com.example.biometrianeonatal.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthLoginResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): RefreshTokenResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto)
}

