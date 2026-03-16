package com.example.biometrianeonatal.core.sensors.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tipo `UsbSensorDevice` que organiza dados ou comportamento desta camada.
 */
data class UsbSensorDevice(
    val deviceId: Int,
    val deviceName: String,
    val manufacturerName: String?,
    val productName: String?,
    val serialNumber: String?,
    val hasPermission: Boolean,
)

/**
 * Tipo `UsbSensorState` que organiza dados ou comportamento desta camada.
 */
data class UsbSensorState(
    val devices: List<UsbSensorDevice> = emptyList(),
    val selectedDeviceId: Int? = null,
    val isPermissionPending: Boolean = false,
    val lastStatusMessage: String? = null,
) {
    val selectedDevice: UsbSensorDevice?
        get() = devices.firstOrNull { it.deviceId == selectedDeviceId }
}

/**
 * Interface `UsbSensorDiscovery` que define um contrato reutilizado por outras camadas.
 */
interface UsbSensorDiscovery {
    fun observeState(): Flow<UsbSensorState>
    fun currentState(): UsbSensorState
    fun snapshotConnectedDevices(): List<UsbSensorDevice>
    suspend fun selectDevice(deviceId: Int)
    suspend fun requestPermissionForSelectedOrFirst()
}

/**
 * Tipo `AndroidUsbSensorDiscovery` que organiza dados ou comportamento desta camada.
 */
@Singleton
class AndroidUsbSensorDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : UsbSensorDiscovery {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val state = MutableStateFlow(UsbSensorState())
    private var selectedDeviceId: Int? = null

    init {
        registerUsbPermissionReceiver()
        refreshState()
    }

    override fun observeState(): Flow<UsbSensorState> = state.asStateFlow()

    override fun currentState(): UsbSensorState = state.value

    override fun snapshotConnectedDevices(): List<UsbSensorDevice> {
        val manager = usbManager ?: return emptyList()
        return manager.deviceList.values.map { device ->
            UsbSensorDevice(
                deviceId = device.deviceId,
                deviceName = device.deviceName,
                manufacturerName = runCatching { device.manufacturerName }.getOrNull(),
                productName = runCatching { device.productName }.getOrNull(),
                serialNumber = runCatching { device.serialNumber }.getOrNull(),
                hasPermission = manager.hasPermission(device),
            )
        }.sortedBy { it.deviceName }
    }

    override suspend fun selectDevice(deviceId: Int) {
        selectedDeviceId = deviceId
        refreshState(lastStatusMessage = null)
    }

    override suspend fun requestPermissionForSelectedOrFirst() {
        val manager = usbManager ?: return
        val targetDevice = resolveTargetDevice() ?: run {
            refreshState(lastStatusMessage = "Nenhum leitor USB disponível para autorização.")
            return
        }
        if (manager.hasPermission(targetDevice)) {
            refreshState(lastStatusMessage = "Permissão USB já concedida para o dispositivo selecionado.")
            return
        }
        selectedDeviceId = targetDevice.deviceId
        state.value = state.value.copy(isPermissionPending = true, lastStatusMessage = "Solicitando permissão USB...")
        manager.requestPermission(targetDevice, buildPermissionPendingIntent())
    }

    private fun registerUsbPermissionReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(
            usbPermissionReceiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun buildPermissionPendingIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun resolveTargetDevice(): UsbDevice? {
        val manager = usbManager ?: return null
        val devices = manager.deviceList.values.toList()
        return devices.firstOrNull { it.deviceId == selectedDeviceId } ?: devices.firstOrNull()
    }

    private fun refreshState(lastStatusMessage: String? = state.value.lastStatusMessage) {
        val devices = snapshotConnectedDevices()
        val effectiveSelectedId = devices.firstOrNull { it.deviceId == selectedDeviceId }?.deviceId
            ?: devices.firstOrNull()?.deviceId
        selectedDeviceId = effectiveSelectedId
        state.value = UsbSensorState(
            devices = devices.map { device ->
                if (device.deviceId == effectiveSelectedId) device else device
            },
            selectedDeviceId = effectiveSelectedId,
            isPermissionPending = false,
            lastStatusMessage = lastStatusMessage,
        )
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    selectedDeviceId = device?.deviceId ?: selectedDeviceId
                    refreshState(
                        lastStatusMessage = if (permissionGranted) {
                            "Permissão USB concedida para o leitor selecionado."
                        } else {
                            "Permissão USB negada para o leitor selecionado."
                        }
                    )
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (selectedDeviceId == null) {
                        selectedDeviceId = device?.deviceId
                    }
                    refreshState(
                        lastStatusMessage = device?.let {
                            "Leitor USB conectado: ${it.productName ?: it.deviceName}."
                        } ?: "Leitor USB conectado."
                    )
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (selectedDeviceId == device?.deviceId) {
                        selectedDeviceId = null
                    }
                    refreshState(
                        lastStatusMessage = device?.let {
                            "Leitor USB removido: ${it.productName ?: it.deviceName}."
                        } ?: "Leitor USB removido."
                    )
                }
            }
        }
    }

    private companion object {
        const val ACTION_USB_PERMISSION = "com.example.biometrianeonatal.USB_PERMISSION"
    }
}


