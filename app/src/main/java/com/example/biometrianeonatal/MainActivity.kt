package com.example.biometrianeonatal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.biometrianeonatal.core.BiometriaNeonatalApp
import com.example.biometrianeonatal.di.AppGraphDependencies
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appGraphDependencies: AppGraphDependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiometriaNeonatalApp(appGraphDependencies)
        }
    }
}
