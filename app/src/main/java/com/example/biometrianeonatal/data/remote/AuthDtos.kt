package com.example.biometrianeonatal.data.remote

data class LoginRequestDto(
    val email: String,
    val password: String,
    val hospitalId: String,
)

data class RefreshTokenRequestDto(
    val refreshToken: String,
)

data class LogoutRequestDto(
    val refreshToken: String,
)

data class RemoteAuthUserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val hospitalId: String,
)

data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
)

data class AuthLoginResponseDto(
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val user: RemoteAuthUserDto,
)

data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
)

