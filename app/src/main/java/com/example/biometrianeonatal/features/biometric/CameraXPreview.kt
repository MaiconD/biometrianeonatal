package com.example.biometrianeonatal.features.biometric

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.biometrianeonatal.core.sensors.tabletcamera.TabletCameraCaptureCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Funcao de topo `CameraXBiometricPreview` usada como parte do fluxo principal do arquivo.
 */
@Composable
fun CameraXBiometricPreview(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coordinator = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CameraXPreviewEntryPoint::class.java,
        ).tabletCameraCaptureCoordinator()
    }
    var hasPermission by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraState by coordinator.observeState().collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, hasPermission, previewView) {
        coordinator.bindPreview(lifecycleOwner, previewView, hasPermission)
        onDispose { coordinator.unbindPreview(lifecycleOwner) }
    }

    Box(
        modifier = modifier.background(Color(0xFFEAF2F8)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !hasPermission -> {
                PreviewFallbackMessage(
                    title = "Preview bloqueado",
                    message = "Autorize a câmera para visualizar o preview local do dispositivo.",
                )
            }

            cameraState.lastErrorMessage != null -> {
                PreviewFallbackMessage(
                    title = "Preview indisponível",
                    message = cameraState.lastErrorMessage.orEmpty(),
                )
            }

            else -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView },
                )
            }
        }
    }
}

/**
 * Interface `CameraXPreviewEntryPoint` que define um contrato reutilizado por outras camadas.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CameraXPreviewEntryPoint {
    fun tabletCameraCaptureCoordinator(): TabletCameraCaptureCoordinator
}

@Composable
private fun PreviewFallbackMessage(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}



