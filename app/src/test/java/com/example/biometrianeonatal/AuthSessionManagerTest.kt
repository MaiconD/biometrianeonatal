package com.example.biometrianeonatal

import com.example.biometrianeonatal.core.security.AuthSessionManager
import com.example.biometrianeonatal.core.security.SessionStore
import com.example.biometrianeonatal.data.remote.AuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.AuthTokensDto
import com.example.biometrianeonatal.data.remote.AuthenticatedRemoteSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionManagerTest {

    @Test
    fun getValidAccessToken_refreshes_when_expired() {
        val sessionStore = InMemorySessionStore().apply {
            saveCurrentUserId("user-01")
            saveAuthSession(
                accessToken = "expired-token",
                refreshToken = "refresh-01",
                expiresAtEpochMillis = System.currentTimeMillis() - 1_000L,
            )
        }
        val remoteDataSource = FakeAuthRemoteDataSource(
            refreshedTokens = AuthTokensDto(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                expiresAtEpochMillis = System.currentTimeMillis() + 60_000L,
            ),
        )
        val manager = AuthSessionManager(sessionStore, remoteDataSource)

        val token = manager.getValidAccessToken()

        assertEquals("new-access-token", token)
        assertEquals("new-access-token", sessionStore.getAccessToken())
        assertEquals("new-refresh-token", sessionStore.getRefreshToken())
        assertEquals(1, remoteDataSource.refreshCalls)
    }

    @Test
    fun getValidAccessToken_clears_session_when_refresh_fails() {
        val sessionStore = InMemorySessionStore().apply {
            saveCurrentUserId("user-01")
            saveAuthSession(
                accessToken = "expired-token",
                refreshToken = "refresh-01",
                expiresAtEpochMillis = System.currentTimeMillis() - 1_000L,
            )
        }
        val manager = AuthSessionManager(sessionStore, FakeAuthRemoteDataSource(refreshedTokens = null))

        val token = manager.getValidAccessToken()

        assertNull(token)
        assertNull(sessionStore.getCurrentUserId())
        assertNull(sessionStore.getAccessToken())
        assertNull(sessionStore.getRefreshToken())
        assertTrue(sessionStore.getAccessTokenExpiresAtEpochMillis() == null)
    }
}

private class InMemorySessionStore : SessionStore {
    private var currentUserId: String? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAtEpochMillis: Long? = null

    override fun getCurrentUserId(): String? = currentUserId

    override fun saveCurrentUserId(userId: String) {
        currentUserId = userId
    }

    override fun getAccessToken(): String? = accessToken

    override fun saveAccessToken(token: String) {
        accessToken = token
    }

    override fun getRefreshToken(): String? = refreshToken

    override fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    override fun getAccessTokenExpiresAtEpochMillis(): Long? = expiresAtEpochMillis

    override fun saveAccessTokenExpiresAtEpochMillis(value: Long) {
        expiresAtEpochMillis = value
    }

    override fun saveAuthSession(accessToken: String, refreshToken: String, expiresAtEpochMillis: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresAtEpochMillis = expiresAtEpochMillis
    }

    override fun clear() {
        currentUserId = null
        accessToken = null
        refreshToken = null
        expiresAtEpochMillis = null
    }
}

private class FakeAuthRemoteDataSource(
    private val refreshedTokens: AuthTokensDto?,
) : AuthRemoteDataSource {
    var refreshCalls: Int = 0
        private set

    override suspend fun login(email: String, password: String, hospitalId: String): AuthenticatedRemoteSession? = null

    override suspend fun refresh(refreshToken: String): AuthTokensDto? {
        refreshCalls += 1
        return refreshedTokens
    }

    override suspend fun logout(refreshToken: String) = Unit
}


