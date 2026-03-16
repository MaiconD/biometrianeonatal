package com.example.biometrianeonatal.data.remote

/**
 * Tipo `LoginRequestDto` que organiza dados ou comportamento desta camada.
 */
data class LoginRequestDto(
    val email: String,
    val password: String,
    val hospitalId: String,
)

/**
 * Tipo `RefreshTokenRequestDto` que organiza dados ou comportamento desta camada.
 */
data class RefreshTokenRequestDto(
    val refreshToken: String,
)

/**
 * Tipo `LogoutRequestDto` que organiza dados ou comportamento desta camada.
 */
data class LogoutRequestDto(
    val refreshToken: String,
)

/**
 * Tipo `RemoteAuthUserDto` que organiza dados ou comportamento desta camada.
 */
data class RemoteAuthUserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val hospitalId: String,
)

/**
 * Tipo `AuthTokensDto` que organiza dados ou comportamento desta camada.
 */
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
)

/**
 * Tipo `AuthLoginResponseDto` que organiza dados ou comportamento desta camada.
 */
data class AuthLoginResponseDto(
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val user: RemoteAuthUserDto,
)

/**
 * Tipo `RefreshTokenResponseDto` que organiza dados ou comportamento desta camada.
 */
data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
)

