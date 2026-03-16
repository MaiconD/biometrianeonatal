package com.example.biometrianeonatal

import android.app.Application
import com.example.biometrianeonatal.core.AppInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BiometriaNeonatalApplication : Application() {
    @Inject
    lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        appInitializer.initialize()
    }
}

