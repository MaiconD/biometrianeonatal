package com.example.biometrianeonatal.core.sensors

import kotlinx.coroutines.flow.Flow

enum class SensorCaptureSource {
    DEMO_LOCAL,
    TABLET_CAMERA,
    USB_SENSOR,
}

data class SensorCaptureSourceOption(
    val source: SensorCaptureSource,
    val label: String,
    val isAvailable: Boolean,
    val detailMessage: String? = null,
)

data class SensorUsbDeviceOption(
    val deviceId: Int,
    val label: String,
    val isSelected: Boolean,
    val hasPermission: Boolean,
)

enum class SensorConnectionState {
    DISCONNECTED,
    WAITING_PERMISSION,
    READY,
    PREVIEW_ACTIVE,
    CAPTURING,
    ERROR,
}

data class SensorDeviceMetadata(
    val name: String,
    val version: String,
    val serial: String,
    val manufacturer: String,
)

data class SensorCapabilities(
    val transport: String,
    val supportsLivePreview: Boolean,
    val supportsVideoStream: Boolean,
    val supportsUsbOtg: Boolean,
    val supportsNativeProcessing: Boolean,
)

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

interface SensorCapturePort {
    fun observeRuntimeSnapshot(): Flow<SensorRuntimeSnapshot>

    fun getSelectedCaptureSource(): SensorCaptureSource

    suspend fun selectCaptureSource(source: SensorCaptureSource)

    suspend fun requestUsbPermission()

    suspend fun selectUsbDevice(deviceId: Int)

    fun describeCapabilities(): SensorCapabilities

    suspend fun capture(sessionId: String, fingerCode: String): SensorCaptureResult
}
