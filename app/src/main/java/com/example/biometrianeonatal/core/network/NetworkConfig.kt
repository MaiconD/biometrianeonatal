package com.example.biometrianeonatal.core.network

import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.CipherSuite
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fabrica de clientes HTTP e Retrofit com timeouts e politica TLS compativeis com o ambiente.
 */
object NetworkConfig {
    const val DEFAULT_BASE_URL = "https://example.invalid/"

    fun createOkHttpClient(
        appRuntimeConfig: AppRuntimeConfig,
        authInterceptor: Interceptor? = null,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (appRuntimeConfig.isDebugBuild) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder().apply {
            authInterceptor?.let(::addInterceptor)
            addInterceptor(logging)
            connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            retryOnConnectionFailure(true)
            connectionSpecs(listOf(RESTRICTED_TLS_SPEC))
        }.build()
    }

    fun createRetrofit(
        baseUrl: String = DEFAULT_BASE_URL,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val RESTRICTED_TLS_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .cipherSuites(
            CipherSuite.TLS_AES_128_GCM_SHA256,
            CipherSuite.TLS_AES_256_GCM_SHA384,
            CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
        )
        .build()

    private const val TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 30L
}

