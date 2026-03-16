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

@Singleton
class AppInitializer @Inject constructor(
    private val appRuntimeConfig: AppRuntimeConfig,
    private val database: AppDatabase,
    private val syncWorkScheduler: SyncWorkScheduler,
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        applicationScope.launch {
            if (appRuntimeConfig.offlineDemoMode) {
                DatabaseSeeder(database).seedIfNeeded()
            }
        }
        if (appRuntimeConfig.hasConfiguredRemoteBackend) {
            syncWorkScheduler.scheduleAutomaticSync()
        }
    }
}

