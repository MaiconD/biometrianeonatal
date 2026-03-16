package com.example.biometrianeonatal.core.security

import com.example.biometrianeonatal.data.remote.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthSessionManager @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRemoteDataSource: AuthRemoteDataSource,
) {
    fun getCurrentAccessToken(): String? = sessionStore.getAccessToken()

    fun getCurrentRefreshToken(): String? = sessionStore.getRefreshToken()

    fun getValidAccessToken(): String? {
        val accessToken = sessionStore.getAccessToken()
        val expiresAtEpochMillis = sessionStore.getAccessTokenExpiresAtEpochMillis()
        if (accessToken.isNullOrBlank() || expiresAtEpochMillis == null) return null
        if (System.currentTimeMillis() < expiresAtEpochMillis - TOKEN_EXPIRY_SKEW_MILLIS) {
            return accessToken
        }
        return refreshAccessTokenIfNeeded()
    }

    fun saveAuthSession(accessToken: String, refreshToken: String, expiresAtEpochMillis: Long) {
        sessionStore.saveAuthSession(accessToken, refreshToken, expiresAtEpochMillis)
    }

    fun clearAuthSession() {
        sessionStore.clear()
    }

    fun refreshAccessTokenIfNeeded(): String? {
        val refreshToken = sessionStore.getRefreshToken() ?: return null
        val refreshedTokens = runBlocking {
            authRemoteDataSource.refresh(refreshToken)
        } ?: return null.also { sessionStore.clear() }

        sessionStore.saveAuthSession(
            accessToken = refreshedTokens.accessToken,
            refreshToken = refreshedTokens.refreshToken,
            expiresAtEpochMillis = refreshedTokens.expiresAtEpochMillis,
        )
        return refreshedTokens.accessToken
    }

    private companion object {
        const val TOKEN_EXPIRY_SKEW_MILLIS = 30_000L
    }
}

class AuthTokenInterceptor @Inject constructor(
    private val authSessionManager: AuthSessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authSessionManager.getValidAccessToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}

