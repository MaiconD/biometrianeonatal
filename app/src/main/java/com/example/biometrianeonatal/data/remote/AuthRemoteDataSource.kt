package com.example.biometrianeonatal.data.remote

import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.UserRole
import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import java.util.UUID
import javax.inject.Inject

/**
 * Tipo `AuthenticatedRemoteSession` que organiza dados ou comportamento desta camada.
 */
data class AuthenticatedRemoteSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val user: RemoteAuthUserDto,
)

/**
 * Interface `AuthRemoteDataSource` que define um contrato reutilizado por outras camadas.
 */
interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String, hospitalId: String): AuthenticatedRemoteSession?
    suspend fun refresh(refreshToken: String): AuthTokensDto?
    suspend fun logout(refreshToken: String)
}

/**
 * Tipo `RetrofitAuthRemoteDataSource` que organiza dados ou comportamento desta camada.
 */
class RetrofitAuthRemoteDataSource @Inject constructor(
    private val appRuntimeConfig: AppRuntimeConfig,
    private val authApiService: AuthApiService,
) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String, hospitalId: String): AuthenticatedRemoteSession? {
        appRuntimeConfig.requireRemoteBackendConfigured()
        val response = authApiService.login(
            LoginRequestDto(
                email = email,
                password = password,
                hospitalId = hospitalId,
            ),
        )
        val accessToken = response.accessToken ?: response.token ?: return null
        val refreshToken = response.refreshToken ?: return null
        val expiresAtEpochMillis = response.expiresAtEpochMillis
            ?: (System.currentTimeMillis() + DEFAULT_ACCESS_TOKEN_TTL_MILLIS)
        return AuthenticatedRemoteSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMillis = expiresAtEpochMillis,
            user = response.user,
        )
    }

    override suspend fun refresh(refreshToken: String): AuthTokensDto? {
        appRuntimeConfig.requireRemoteBackendConfigured()
        return authApiService.refresh(RefreshTokenRequestDto(refreshToken))
            .let {
                AuthTokensDto(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                    expiresAtEpochMillis = it.expiresAtEpochMillis,
                )
            }
    }

    override suspend fun logout(refreshToken: String) {
        appRuntimeConfig.requireRemoteBackendConfigured()
        authApiService.logout(LogoutRequestDto(refreshToken))
    }

    private companion object {
        const val DEFAULT_ACCESS_TOKEN_TTL_MILLIS = 15 * 60 * 1000L
    }
}

/**
 * Tipo `LocalFallbackAuthRemoteDataSource` que organiza dados ou comportamento desta camada.
 */
class LocalFallbackAuthRemoteDataSource @Inject constructor(
    private val database: AppDatabase,
) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String, hospitalId: String): AuthenticatedRemoteSession? {
        val user = database.userDao().login(email, password, hospitalId) ?: return null
        return buildSession(
            userId = user.id,
            user = RemoteAuthUserDto(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role.name,
                hospitalId = user.hospitalId,
            ),
        )
    }

    override suspend fun refresh(refreshToken: String): AuthTokensDto? {
        val userId = parseUserIdFromToken(refreshToken, REFRESH_TOKEN_PREFIX) ?: return null
        val user = database.userDao().getById(userId) ?: return null
        val session = buildSession(
            userId = user.id,
            user = RemoteAuthUserDto(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role.name,
                hospitalId = user.hospitalId,
            ),
        )
        return AuthTokensDto(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAtEpochMillis = session.expiresAtEpochMillis,
        )
    }

    override suspend fun logout(refreshToken: String) = Unit

    private fun buildSession(userId: String, user: RemoteAuthUserDto): AuthenticatedRemoteSession {
        val now = System.currentTimeMillis()
        val expiresAtEpochMillis = now + ACCESS_TOKEN_TTL_MILLIS
        return AuthenticatedRemoteSession(
            accessToken = "$ACCESS_TOKEN_PREFIX:$userId:${UUID.randomUUID()}",
            refreshToken = "$REFRESH_TOKEN_PREFIX:$userId:${UUID.randomUUID()}",
            expiresAtEpochMillis = expiresAtEpochMillis,
            user = user,
        )
    }

    private fun parseUserIdFromToken(token: String, prefix: String): String? {
        val parts = token.split(':')
        return parts.takeIf { it.size >= 3 && it.firstOrNull() == prefix }?.getOrNull(1)
    }

    private companion object {
        const val ACCESS_TOKEN_TTL_MILLIS = 15 * 60 * 1000L
        const val ACCESS_TOKEN_PREFIX = "bn_access"
        const val REFRESH_TOKEN_PREFIX = "bn_refresh"
    }
}

/**
 * Tipo `FallbackAuthRemoteDataSource` que organiza dados ou comportamento desta camada.
 */
class FallbackAuthRemoteDataSource @Inject constructor(
    private val appRuntimeConfig: AppRuntimeConfig,
    private val primary: RetrofitAuthRemoteDataSource,
    private val fallback: LocalFallbackAuthRemoteDataSource,
) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String, hospitalId: String): AuthenticatedRemoteSession? {
        if (appRuntimeConfig.offlineDemoMode) {
            return fallback.login(email, password, hospitalId)
        }
        return runCatching {
            primary.login(email, password, hospitalId)
        }.getOrElse { throw it }
    }

    override suspend fun refresh(refreshToken: String): AuthTokensDto? {
        if (appRuntimeConfig.offlineDemoMode) {
            return fallback.refresh(refreshToken)
        }
        return runCatching {
            primary.refresh(refreshToken)
        }.getOrElse { throw it }
    }

    override suspend fun logout(refreshToken: String) {
        if (appRuntimeConfig.offlineDemoMode) {
            fallback.logout(refreshToken)
            return
        }
        runCatching {
            primary.logout(refreshToken)
        }.getOrElse { throw it }
    }
}

internal fun RemoteAuthUserDto.toUserRole(): UserRole {
    return runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.OPERADOR)
}

