package com.example.biometrianeonatal.core.config

data class AppRuntimeConfig(
    val environmentName: String,
    val remoteBaseUrl: String,
    val offlineDemoMode: Boolean,
    val isDebugBuild: Boolean,
) {
    val hasConfiguredRemoteBackend: Boolean
        get() = !offlineDemoMode &&
            remoteBaseUrl.isNotBlank() &&
            !remoteBaseUrl.contains(".invalid", ignoreCase = true)

    fun requireRemoteBackendConfigured() {
        check(hasConfiguredRemoteBackend) {
            "Backend remoto não configurado para o ambiente '$environmentName'. " +
                "Defina REMOTE_BASE_URL válido e desative OFFLINE_DEMO_MODE para produção."
        }
    }
}
