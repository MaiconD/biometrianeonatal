package com.example.biometrianeonatal.core.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import com.example.biometrianeonatal.core.sensors.nativeprocessing.NativeImageProcessor
import com.example.biometrianeonatal.core.sensors.tabletcamera.TabletCameraCaptureCoordinator
import com.example.biometrianeonatal.core.sensors.usb.UsbSensorDevice
import com.example.biometrianeonatal.core.sensors.usb.UsbSensorDiscovery
import java.nio.charset.StandardCharsets
import java.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tipo `AdaptiveSensorCapturePort` que organiza dados ou comportamento desta camada.
 */
@Singleton
class AdaptiveSensorCapturePort @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRuntimeConfig: AppRuntimeConfig,
    private val fakeSensorAdapter: FakeSensorAdapter,
    private val tabletCameraCaptureCoordinator: TabletCameraCaptureCoordinator,
    private val usbSensorDiscovery: UsbSensorDiscovery,
    private val nativeImageProcessor: NativeImageProcessor,
) : SensorCapturePort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val selectedSource = MutableStateFlow(defaultSource())
    private val runtimeSnapshot = MutableStateFlow(buildSnapshot())

    init {
        scope.launch {
            tabletCameraCaptureCoordinator.observeState().collect {
                runtimeSnapshot.value = buildSnapshot()
            }
        }
        scope.launch {
            usbSensorDiscovery.observeState().collect {
                runtimeSnapshot.value = buildSnapshot()
            }
        }
    }

    override fun observeRuntimeSnapshot(): Flow<SensorRuntimeSnapshot> = runtimeSnapshot.asStateFlow()

    override fun getSelectedCaptureSource(): SensorCaptureSource = selectedSource.value

    override suspend fun selectCaptureSource(source: SensorCaptureSource) {
        val availableSource = buildAvailableSources().firstOrNull { it.source == source }
        if (availableSource?.isAvailable == true) {
            selectedSource.value = source
        }
        runtimeSnapshot.value = buildSnapshot()
    }

    override suspend fun requestUsbPermission() {
        usbSensorDiscovery.requestPermissionForSelectedOrFirst()
        runtimeSnapshot.value = buildSnapshot()
    }

    override suspend fun selectUsbDevice(deviceId: Int) {
        usbSensorDiscovery.selectDevice(deviceId)
        runtimeSnapshot.value = buildSnapshot()
    }

    override fun describeCapabilities(): SensorCapabilities = buildSnapshot().capabilities

    override suspend fun capture(sessionId: String, fingerCode: String): SensorCaptureResult {
        runtimeSnapshot.value = buildSnapshot(connectionStateOverride = SensorConnectionState.CAPTURING)
        val captureResult = when (getSelectedCaptureSource()) {
            SensorCaptureSource.TABLET_CAMERA -> captureFromTabletCamera(sessionId, fingerCode)
            SensorCaptureSource.DEMO_LOCAL,
            SensorCaptureSource.USB_SENSOR,
            -> captureFromFallback(sessionId, fingerCode)
        }
        return captureResult.also {
            runtimeSnapshot.value = buildSnapshot(
                connectionStateOverride = when (getSelectedCaptureSource()) {
                    SensorCaptureSource.TABLET_CAMERA -> SensorConnectionState.PREVIEW_ACTIVE
                    else -> SensorConnectionState.READY
                }
            )
        }
    }

    private fun defaultSource(): SensorCaptureSource {
        val available = buildAvailableSources()
        return when {
            available.any { it.source == SensorCaptureSource.TABLET_CAMERA && it.isAvailable } -> SensorCaptureSource.TABLET_CAMERA
            available.any { it.source == SensorCaptureSource.DEMO_LOCAL && it.isAvailable } -> SensorCaptureSource.DEMO_LOCAL
            available.any { it.source == SensorCaptureSource.USB_SENSOR && it.isAvailable } -> SensorCaptureSource.USB_SENSOR
            else -> SensorCaptureSource.DEMO_LOCAL
        }
    }

    private fun buildSnapshot(
        connectionStateOverride: SensorConnectionState? = null,
    ): SensorRuntimeSnapshot {
        val availableSources = buildAvailableSources()
        val effectiveSource = availableSources.firstOrNull {
            it.source == selectedSource.value && it.isAvailable
        }?.source ?: defaultSource().also { selectedSource.value = it }
        val usbState = usbSensorDiscovery.currentState()
        val usbDevices = usbState.devices
        val selectedUsbDevice = usbState.selectedDevice
        val tabletCameraState = tabletCameraCaptureCoordinator.observeState().value
        val capabilities = when (effectiveSource) {
            SensorCaptureSource.DEMO_LOCAL -> SensorCapabilities(
                transport = "demo-local",
                supportsLivePreview = false,
                supportsVideoStream = false,
                supportsUsbOtg = false,
                supportsNativeProcessing = nativeImageProcessor.isNativeAvailable,
            )
            SensorCaptureSource.TABLET_CAMERA -> SensorCapabilities(
                transport = "camerax-tablet",
                supportsLivePreview = true,
                supportsVideoStream = true,
                supportsUsbOtg = false,
                supportsNativeProcessing = nativeImageProcessor.isNativeAvailable,
            )
            SensorCaptureSource.USB_SENSOR -> SensorCapabilities(
                transport = "usb-uvc-prep",
                supportsLivePreview = false,
                supportsVideoStream = true,
                supportsUsbOtg = true,
                supportsNativeProcessing = nativeImageProcessor.isNativeAvailable,
            )
        }
        val metadata = when (effectiveSource) {
            SensorCaptureSource.DEMO_LOCAL -> SensorDeviceMetadata(
                name = "InfantID Sensor (Demo)",
                version = "2.4.0-demo",
                serial = "DEMO-SENSOR-001",
                manufacturer = "InfantID / Demo Local",
            )
            SensorCaptureSource.TABLET_CAMERA -> SensorDeviceMetadata(
                name = "Câmera do tablet",
                version = "CameraX",
                serial = Build.MODEL ?: "tablet-camera",
                manufacturer = Build.MANUFACTURER ?: "Android",
            )
            SensorCaptureSource.USB_SENSOR -> selectedUsbDevice.toMetadata()
        }
        val state = connectionStateOverride ?: when {
            !availableSources.any { it.source == effectiveSource && it.isAvailable } -> SensorConnectionState.ERROR
            effectiveSource == SensorCaptureSource.TABLET_CAMERA && tabletCameraState.lastErrorMessage != null -> SensorConnectionState.ERROR
            effectiveSource == SensorCaptureSource.TABLET_CAMERA && tabletCameraState.isPreviewBound -> SensorConnectionState.PREVIEW_ACTIVE
            effectiveSource == SensorCaptureSource.TABLET_CAMERA && tabletCameraState.hasPermission -> SensorConnectionState.READY
            effectiveSource == SensorCaptureSource.USB_SENSOR && usbDevices.isNotEmpty() && selectedUsbDevice?.hasPermission != true -> SensorConnectionState.WAITING_PERMISSION
            effectiveSource == SensorCaptureSource.USB_SENSOR && usbDevices.isEmpty() -> SensorConnectionState.DISCONNECTED
            else -> SensorConnectionState.READY
        }
        val unavailableReason = availableSources.firstOrNull { it.source == effectiveSource && !it.isAvailable }?.detailMessage
        return SensorRuntimeSnapshot(
            connectionState = state,
            capabilities = capabilities,
            deviceMetadata = metadata,
            selectedSource = effectiveSource,
            availableSources = availableSources,
            availableUsbDevices = usbDevices.map { usbDevice ->
                SensorUsbDeviceOption(
                    deviceId = usbDevice.deviceId,
                    label = usbDevice.productName ?: usbDevice.deviceName,
                    isSelected = usbDevice.deviceId == usbState.selectedDeviceId,
                    hasPermission = usbDevice.hasPermission,
                )
            },
            usbPermissionRequired = effectiveSource == SensorCaptureSource.USB_SENSOR && selectedUsbDevice?.hasPermission != true,
            isCaptureReady = when (effectiveSource) {
                SensorCaptureSource.DEMO_LOCAL -> true
                SensorCaptureSource.TABLET_CAMERA -> tabletCameraState.isCaptureReady
                SensorCaptureSource.USB_SENSOR -> selectedUsbDevice?.hasPermission == true
            },
            lastErrorMessage = when {
                state == SensorConnectionState.ERROR && effectiveSource == SensorCaptureSource.TABLET_CAMERA -> {
                    tabletCameraState.lastErrorMessage ?: unavailableReason
                }
                state == SensorConnectionState.ERROR -> unavailableReason
                else -> null
            },
        )
    }

    private fun buildAvailableSources(): List<SensorCaptureSourceOption> {
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val usbState = usbSensorDiscovery.currentState()
        val usbDevices = usbState.devices
        val selectedUsbDevice = usbState.selectedDevice
        val demoAvailable = appRuntimeConfig.offlineDemoMode || appRuntimeConfig.isDebugBuild
        val tabletCameraState = tabletCameraCaptureCoordinator.observeState().value
        return listOf(
            SensorCaptureSourceOption(
                source = SensorCaptureSource.DEMO_LOCAL,
                label = "Demo local",
                isAvailable = demoAvailable,
                detailMessage = if (demoAvailable) null else "Demo desabilitado neste ambiente.",
            ),
            SensorCaptureSourceOption(
                source = SensorCaptureSource.TABLET_CAMERA,
                label = "Câmera do tablet",
                isAvailable = hasCamera,
                detailMessage = when {
                    !hasCamera -> "Dispositivo sem câmera disponível."
                    tabletCameraState.lastErrorMessage != null -> tabletCameraState.lastErrorMessage
                    !tabletCameraState.hasPermission -> "Permissão será solicitada ao abrir o preview."
                    !tabletCameraState.isPreviewBound -> "Abra o preview para armar a captura real."
                    else -> "Captura real CameraX pronta."
                },
            ),
            SensorCaptureSourceOption(
                source = SensorCaptureSource.USB_SENSOR,
                label = "Leitor USB/UVC",
                isAvailable = usbDevices.isNotEmpty(),
                detailMessage = when {
                    usbDevices.isEmpty() -> "Nenhum leitor USB/UVC detectado no momento."
                    selectedUsbDevice == null -> "Selecione um leitor USB para continuar."
                    !selectedUsbDevice.hasPermission -> usbState.lastStatusMessage
                        ?: "Autorize o leitor USB selecionado para habilitar a captura."
                    else -> usbState.lastStatusMessage
                        ?: "Leitor USB autorizado e pronto para integração futura."
                },
            ),
        )
    }

    private fun buildCaptureResolution(defaultResolution: String): String {
        return when (getSelectedCaptureSource()) {
            SensorCaptureSource.DEMO_LOCAL -> defaultResolution
            SensorCaptureSource.TABLET_CAMERA -> "CameraX/$defaultResolution"
            SensorCaptureSource.USB_SENSOR -> "USB-UVC/$defaultResolution"
        }
    }

    private suspend fun captureFromFallback(sessionId: String, fingerCode: String): SensorCaptureResult {
        val baseCapture = fakeSensorAdapter.capture(sessionId, fingerCode)
        val processedBytes = nativeImageProcessor.processCapture(baseCapture.imageBytes)
        val sourceTag = getSelectedCaptureSource().name
        return SensorCaptureResult(
            sessionId = baseCapture.sessionId,
            fingerCode = baseCapture.fingerCode,
            qualityScore = baseCapture.qualityScore,
            resolution = buildCaptureResolution(baseCapture.resolution),
            fps = baseCapture.fps,
            imageBytes = processedBytes + "\nsource=$sourceTag".toByteArray(StandardCharsets.UTF_8),
            templateBase64 = baseCapture.templateBase64,
        )
    }

    private suspend fun captureFromTabletCamera(sessionId: String, fingerCode: String): SensorCaptureResult {
        val payload = tabletCameraCaptureCoordinator.takePicture()
        val processedBytes = nativeImageProcessor.processCapture(payload.imageBytes)
        val resolution = when {
            payload.width > 0 && payload.height > 0 -> "${payload.width}x${payload.height}"
            else -> "JPEG"
        }
        val template = Base64.getEncoder().encodeToString(
            "$sessionId|$fingerCode|TABLET_CAMERA|$resolution".toByteArray(StandardCharsets.UTF_8)
        )
        return SensorCaptureResult(
            sessionId = sessionId,
            fingerCode = fingerCode,
            qualityScore = 92,
            resolution = buildCaptureResolution(resolution),
            fps = 30,
            imageBytes = processedBytes,
            templateBase64 = template,
        )
    }

    private fun UsbSensorDevice?.toMetadata(): SensorDeviceMetadata {
        return SensorDeviceMetadata(
            name = this?.productName ?: this?.deviceName ?: "Leitor USB/UVC",
            version = "USB/UVC",
            serial = this?.serialNumber ?: this?.deviceId?.toString().orEmpty(),
            manufacturer = this?.manufacturerName ?: "USB Host",
        )
    }
}



