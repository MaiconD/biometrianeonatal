package com.example.biometrianeonatal

import android.app.Application
import com.example.biometrianeonatal.core.AppInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Classe `Application` que inicializa o Hilt e dispara o bootstrap global do app.
 */
@HiltAndroidApp
class BiometriaNeonatalApplication : Application() {
    @Inject
    lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        // Centraliza o bootstrap do processo Android antes da primeira tela ser exibida.
        appInitializer.initialize()
    }
}

