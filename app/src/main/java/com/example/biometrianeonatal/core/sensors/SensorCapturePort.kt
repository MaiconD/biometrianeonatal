package com.example.biometrianeonatal.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Enumeracao `SensorCaptureSource` usada para restringir valores validos do dominio.
 */
enum class SensorCaptureSource {
    DEMO_LOCAL,
    TABLET_CAMERA,
    USB_SENSOR,
}

/**
 * Tipo `SensorCaptureSourceOption` que organiza dados ou comportamento desta camada.
 */
data class SensorCaptureSourceOption(
    val source: SensorCaptureSource,
    val label: String,
    val isAvailable: Boolean,
    val detailMessage: String? = null,
)

/**
 * Tipo `SensorUsbDeviceOption` que organiza dados ou comportamento desta camada.
 */
data class SensorUsbDeviceOption(
    val deviceId: Int,
    val label: String,
    val isSelected: Boolean,
    val hasPermission: Boolean,
)

/**
 * Enumeracao `SensorConnectionState` usada para restringir valores validos do dominio.
 */
enum class SensorConnectionState {
    DISCONNECTED,
    WAITING_PERMISSION,
    READY,
    PREVIEW_ACTIVE,
    CAPTURING,
    ERROR,
}

/**
 * Tipo `SensorDeviceMetadata` que organiza dados ou comportamento desta camada.
 */
data class SensorDeviceMetadata(
    val name: String,
    val version: String,
    val serial: String,
    val manufacturer: String,
)

/**
 * Tipo `SensorCapabilities` que organiza dados ou comportamento desta camada.
 */
data class SensorCapabilities(
    val transport: String,
    val supportsLivePreview: Boolean,
    val supportsVideoStream: Boolean,
    val supportsUsbOtg: Boolean,
    val supportsNativeProcessing: Boolean,
)

/**
 * Tipo `SensorRuntimeSnapshot` que organiza dados ou comportamento desta camada.
 */
data class SensorRuntimeSnapshot(
    val connectionState: SensorConnectionState,
    val capabilities: SensorCapabilities,
    val deviceMetadata: SensorDeviceMetadata,
    val selectedSource: SensorCaptureSource,
    val availableSources: List<SensorCaptureSourceOption>,
    val availableUsbDevices: List<SensorUsbDeviceOption>,
    val usbPermissionRequired: Boolean,
    val isCaptureReady: Boolean,
    val lastErrorMessage: String? = null,
)

/**
 * Interface `SensorCapturePort` que define um contrato reutilizado por outras camadas.
 */
interface SensorCapturePort {
    fun observeRuntimeSnapshot(): Flow<SensorRuntimeSnapshot>

    fun getSelectedCaptureSource(): SensorCaptureSource

    suspend fun selectCaptureSource(source: SensorCaptureSource)

    suspend fun requestUsbPermission()

    suspend fun selectUsbDevice(deviceId: Int)

    fun describeCapabilities(): SensorCapabilities

    suspend fun capture(sessionId: String, fingerCode: String): SensorCaptureResult
}
