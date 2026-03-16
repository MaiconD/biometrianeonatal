package com.example.biometrianeonatal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.biometrianeonatal.core.BiometriaNeonatalApp
import com.example.biometrianeonatal.di.AppGraphDependencies
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity Android que hospeda a arvore Compose e inicia a navegacao principal.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appGraphDependencies: AppGraphDependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // A Activity apenas hospeda o grafo Compose; regras de navegação e sessão ficam fora dela.
            BiometriaNeonatalApp(appGraphDependencies)
        }
    }
}
