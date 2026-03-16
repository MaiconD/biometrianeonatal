package com.example.biometrianeonatal.core.sensors

import java.time.Instant
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

class SensorCaptureResult(
    val sessionId: String,
    val fingerCode: String,
    val qualityScore: Int,
    val resolution: String,
    val fps: Int,
    val imageBytes: ByteArray,
    val templateBase64: String,
)

class FakeSensorAdapter : SensorCapturePort {
    private val runtimeSnapshot = MutableStateFlow(
        SensorRuntimeSnapshot(
            connectionState = SensorConnectionState.READY,
            capabilities = describeCapabilities(),
            deviceMetadata = SensorDeviceMetadata(
                name = "InfantID Sensor (Demo)",
                version = "2.4.0-demo",
                serial = "DEMO-SENSOR-001",
                manufacturer = "InfantID / Demo Local",
            ),
            selectedSource = SensorCaptureSource.DEMO_LOCAL,
            availableSources = listOf(
                SensorCaptureSourceOption(
                    source = SensorCaptureSource.DEMO_LOCAL,
                    label = "Demo local",
                    isAvailable = true,
                )
            ),
            availableUsbDevices = emptyList(),
            usbPermissionRequired = false,
            isCaptureReady = true,
        )
    )

    override fun observeRuntimeSnapshot(): Flow<SensorRuntimeSnapshot> = runtimeSnapshot

    override fun getSelectedCaptureSource(): SensorCaptureSource = SensorCaptureSource.DEMO_LOCAL

    override suspend fun selectCaptureSource(source: SensorCaptureSource) = Unit

    override suspend fun requestUsbPermission() = Unit

    override suspend fun selectUsbDevice(deviceId: Int) = Unit

    override fun describeCapabilities(): SensorCapabilities {
        return SensorCapabilities(
            transport = "demo-local",
            supportsLivePreview = true,
            supportsVideoStream = false,
            supportsUsbOtg = false,
            supportsNativeProcessing = false,
        )
    }

    override suspend fun capture(sessionId: String, fingerCode: String): SensorCaptureResult {
        runtimeSnapshot.value = runtimeSnapshot.value.copy(connectionState = SensorConnectionState.CAPTURING)
        val quality = Random.nextInt(from = 78, until = 99)
        val fps = if (quality > 90) 10 else 8
        val resolution = if (quality > 90) "640x480" else "1280x720"
        val captureInstant = Instant.now().toEpochMilli()
        val captureNonce = Random.nextLong()
        val template = Base64.getEncoder().encodeToString(
            "$sessionId|$fingerCode|$quality|$fps|$resolution".toByteArray()
        )
        val imageBytes = buildString {
            append("FAKE_CAPTURE\n")
            append("session=")
            append(sessionId)
            append('\n')
            append("finger=")
            append(fingerCode)
            append('\n')
            append("quality=")
            append(quality)
            append('\n')
            append("fps=")
            append(fps)
            append('\n')
            append("resolution=")
            append(resolution)
            append('\n')
            append("capturedAt=")
            append(captureInstant)
            append('\n')
            append("nonce=")
            append(captureNonce)
        }.toByteArray(StandardCharsets.UTF_8)
        return SensorCaptureResult(
            sessionId = sessionId,
            fingerCode = fingerCode,
            qualityScore = quality,
            resolution = resolution,
            fps = fps,
            imageBytes = imageBytes,
            templateBase64 = template,
        ).also {
            runtimeSnapshot.value = runtimeSnapshot.value.copy(
                connectionState = SensorConnectionState.PREVIEW_ACTIVE,
                isCaptureReady = true,
            )
        }
    }
}

