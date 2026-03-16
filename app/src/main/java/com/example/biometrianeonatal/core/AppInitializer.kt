package com.example.biometrianeonatal.core

import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.sync.SyncWorkScheduler
import com.example.biometrianeonatal.data.seed.DatabaseSeeder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inicializador de infraestrutura que prepara massa demo e agenda sincronizacao automatica quando aplicavel.
 */
@Singleton
class AppInitializer @Inject constructor(
    private val appRuntimeConfig: AppRuntimeConfig,
    // Escopo isolado do ciclo de vida das telas para tarefas que precisam existir desde o bootstrap.
    private val database: AppDatabase,
    private val syncWorkScheduler: SyncWorkScheduler,
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            // No modo demo o app precisa nascer com dados locais para permitir navegação offline imediata.

    fun initialize() {
        applicationScope.launch {
            if (appRuntimeConfig.offlineDemoMode) {
        // O agendamento só ocorre quando há backend real configurado; em demo offline ele seria ruído.
                DatabaseSeeder(database).seedIfNeeded()
            }
        }
        if (appRuntimeConfig.hasConfiguredRemoteBackend) {
            syncWorkScheduler.scheduleAutomaticSync()
        }
    }
}

