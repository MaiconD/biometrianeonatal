package com.example.biometrianeonatal.core.database

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/**
 * Provedor da senha do banco criptografado persistida com EncryptedSharedPreferences.
 */
class DatabasePassphraseProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences by lazy {
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

    fun getOrCreatePassphrase(): ByteArray {
        val stored = preferences.getString(KEY_PASSPHRASE, null)
        if (!stored.isNullOrBlank()) {
            return if (stored.startsWith(TEXT_PREFIX)) {
                stored.removePrefix(TEXT_PREFIX).toByteArray(StandardCharsets.UTF_8)
            } else {
                Base64.decode(stored, Base64.NO_WRAP)
            }
        }
        return createAndPersistTextPassphrase().toByteArray(StandardCharsets.UTF_8)
    }

    fun getOrCreatePassphraseText(): String {
        val stored = preferences.getString(KEY_PASSPHRASE, null)
        if (!stored.isNullOrBlank()) {
            if (stored.startsWith(TEXT_PREFIX)) {
                return stored.removePrefix(TEXT_PREFIX)
            }
            return createAndPersistTextPassphrase()
        }
        return createAndPersistTextPassphrase()
    }

    private fun createAndPersistTextPassphrase(): String {
        val randomBytes = ByteArray(PASSPHRASE_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val textPassphrase = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            .replace('+', '-')
            .replace('/', '_')
        preferences.edit()
            .putString(KEY_PASSPHRASE, TEXT_PREFIX + textPassphrase)
            .apply()
        return textPassphrase
    }

    private companion object {
        const val FILE_NAME = "biometria_neonatal_db_passphrase"
        const val KEY_PASSPHRASE = "database_passphrase"
        const val PASSPHRASE_LENGTH_BYTES = 32
        const val TEXT_PREFIX = "text:v1:"
    }
}



