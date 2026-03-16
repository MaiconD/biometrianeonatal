package com.example.biometrianeonatal.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Contrato para persistencia segura da sessao autenticada no dispositivo.
 */
interface SessionStore {
    fun getCurrentUserId(): String?
    fun saveCurrentUserId(userId: String)
    fun getAccessToken(): String?
    fun saveAccessToken(token: String)
    fun getRefreshToken(): String?
    fun saveRefreshToken(token: String)
    fun getAccessTokenExpiresAtEpochMillis(): Long?
    fun saveAccessTokenExpiresAtEpochMillis(value: Long)
    fun saveAuthSession(accessToken: String, refreshToken: String, expiresAtEpochMillis: Long)
    fun clear()
}

/**
 * Implementacao da sessao com EncryptedSharedPreferences para proteger tokens e usuario atual.
 */
class SecureSessionStore(
    context: Context,
) : SessionStore {
    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getCurrentUserId(): String? {
        return preferences.getString(KEY_CURRENT_USER_ID, null)
    }

    override fun saveCurrentUserId(userId: String) {
        preferences.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    override fun getAccessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun saveAccessToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override fun getRefreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    override fun saveRefreshToken(token: String) {
        preferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    override fun getAccessTokenExpiresAtEpochMillis(): Long? {
        val value = preferences.getLong(KEY_ACCESS_TOKEN_EXPIRES_AT_EPOCH_MILLIS, -1L)
        return value.takeIf { it > 0L }
    }

    override fun saveAccessTokenExpiresAtEpochMillis(value: Long) {
        preferences.edit().putLong(KEY_ACCESS_TOKEN_EXPIRES_AT_EPOCH_MILLIS, value).apply()
    }

    override fun saveAuthSession(accessToken: String, refreshToken: String, expiresAtEpochMillis: Long) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_ACCESS_TOKEN_EXPIRES_AT_EPOCH_MILLIS, expiresAtEpochMillis)
            .apply()
    }

    override fun clear() {
        preferences.edit()
            .remove(KEY_CURRENT_USER_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_TOKEN_EXPIRES_AT_EPOCH_MILLIS)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "biometria_neonatal_session"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_TOKEN_EXPIRES_AT_EPOCH_MILLIS = "access_token_expires_at_epoch_millis"
    }
}

