package com.example.biometrianeonatal.core.sensors.tabletcamera

import android.content.Context
import android.graphics.BitmapFactory
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Tipo `TabletCameraRuntimeState` que organiza dados ou comportamento desta camada.
 */
data class TabletCameraRuntimeState(
    val hasPermission: Boolean = false,
    val isPreviewBound: Boolean = false,
    val isCaptureReady: Boolean = false,
    val lastErrorMessage: String? = null,
)

/**
 * Tipo `TabletCameraCapturePayload` que organiza dados ou comportamento desta camada.
 */
data class TabletCameraCapturePayload(
    val imageBytes: ByteArray,
    val width: Int,
    val height: Int,
)

/**
 * Tipo `TabletCameraCaptureCoordinator` que organiza dados ou comportamento desta camada.
 */
@Singleton
class TabletCameraCaptureCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val runtimeState = MutableStateFlow(TabletCameraRuntimeState())

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var boundLifecycleOwner: LifecycleOwner? = null

    fun observeState(): StateFlow<TabletCameraRuntimeState> = runtimeState.asStateFlow()

    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        hasPermission: Boolean,
    ) {
        if (!hasPermission) {
            runtimeState.value = TabletCameraRuntimeState(
                hasPermission = false,
                isPreviewBound = false,
                isCaptureReady = false,
                lastErrorMessage = "Permissão de câmera pendente.",
            )
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val provider = cameraProviderFuture.get()
                    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val captureUseCase = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(rotation)
                        .build()
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        captureUseCase,
                    )
                    cameraProvider = provider
                    imageCapture = captureUseCase
                    boundLifecycleOwner = lifecycleOwner
                    runtimeState.value = TabletCameraRuntimeState(
                        hasPermission = true,
                        isPreviewBound = true,
                        isCaptureReady = true,
                        lastErrorMessage = null,
                    )
                }.onFailure { error ->
                    imageCapture = null
                    cameraProvider = null
                    boundLifecycleOwner = null
                    runtimeState.value = TabletCameraRuntimeState(
                        hasPermission = true,
                        isPreviewBound = false,
                        isCaptureReady = false,
                        lastErrorMessage = error.message ?: "Falha ao inicializar a câmera do tablet.",
                    )
                }
            },
            mainExecutor,
        )
    }

    fun unbindPreview(lifecycleOwner: LifecycleOwner? = null) {
        if (lifecycleOwner != null && boundLifecycleOwner != null && boundLifecycleOwner != lifecycleOwner) {
            return
        }
        runCatching { cameraProvider?.unbindAll() }
        imageCapture = null
        cameraProvider = null
        boundLifecycleOwner = null
        runtimeState.value = runtimeState.value.copy(
            isPreviewBound = false,
            isCaptureReady = false,
        )
    }

    suspend fun takePicture(): TabletCameraCapturePayload {
        val captureUseCase = imageCapture
            ?: throw IllegalStateException("Câmera do tablet ainda não está pronta para captura.")
        val outputFile = File.createTempFile("tablet-capture-", ".jpg", context.cacheDir)
        return suspendCancellableCoroutine { continuation ->
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            captureUseCase.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        runCatching {
                            val bytes = outputFile.readBytes()
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            TabletCameraCapturePayload(
                                imageBytes = bytes,
                                width = bounds.outWidth,
                                height = bounds.outHeight,
                            )
                        }.onSuccess { payload ->
                            outputFile.delete()
                            runtimeState.value = runtimeState.value.copy(
                                isPreviewBound = true,
                                isCaptureReady = true,
                                lastErrorMessage = null,
                            )
                            continuation.resume(payload)
                        }.onFailure { error ->
                            outputFile.delete()
                            runtimeState.value = runtimeState.value.copy(
                                lastErrorMessage = error.message ?: "Falha ao ler a captura da câmera do tablet.",
                            )
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        outputFile.delete()
                        runtimeState.value = runtimeState.value.copy(
                            lastErrorMessage = exception.message ?: "Falha na captura da câmera do tablet.",
                            isCaptureReady = false,
                        )
                        continuation.resumeWithException(exception)
                    }
                },
            )
            continuation.invokeOnCancellation {
                outputFile.delete()
            }
        }
    }
}


