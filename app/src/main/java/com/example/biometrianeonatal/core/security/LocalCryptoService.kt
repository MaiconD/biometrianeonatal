package com.example.biometrianeonatal.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface LocalCryptoService {
    fun encrypt(value: String): String
    fun decryptIfNeeded(value: String): String
}

class AndroidKeystoreCryptoService : LocalCryptoService {
    private val keyAlias = "biometria_neonatal_local_v1"
    private val keyStoreProvider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val encryptedPrefix = "enc:v1:"

    override fun encrypt(value: String): String {
        if (value.isBlank()) return value
        if (value.startsWith(encryptedPrefix)) return value

        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, payload, iv.size, encryptedBytes.size)
        return encryptedPrefix + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decryptIfNeeded(value: String): String {
        if (!value.startsWith(encryptedPrefix)) return value

        return runCatching {
            val payload = Base64.decode(value.removePrefix(encryptedPrefix), Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val encryptedBytes = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
        }.getOrElse { "" }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

