package com.example.biometrianeonatal.features.biometric

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biometrianeonatal.domain.model.SessionCaptureProgress

private val fingerOptions = listOf(
    "POLEGAR_DIREITO" to "Polegar direito",
    "INDICADOR_DIREITO" to "Indicador direito",
    "POLEGAR_ESQUERDO" to "Polegar esquerdo",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricSessionScreen(
    onBack: () -> Unit,
    onSessionStarted: (String) -> Unit,
) {
    val viewModel: BiometricViewModel = hiltViewModel()
    val sessionContext by viewModel.sessionContext.collectAsStateWithLifecycle()
    val sensorRuntime by viewModel.sensorRuntime.collectAsStateWithLifecycle()
    val babyProfileSummary by viewModel.babyProfileSummary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Sessão de Coleta") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Informações do paciente", fontWeight = FontWeight.Bold)
                    Text(sessionContext?.babyName ?: "Carregando...")
                    Text(
                        sessionContext?.babyAgeLabel ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            babyProfileSummary?.latestSessionProgress?.takeIf { it.completedFingerCodes.isNotEmpty() }?.let { progress ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Digitais já cadastradas", fontWeight = FontWeight.Bold)
                        progress.completedFingerCodes.forEach { code ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = fingerOptions.find { it.first == code }?.second ?: code,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Fonte de captura", fontWeight = FontWeight.SemiBold)
                    CaptureSourceSelector(
                        selectedSourceName = sensorRuntime.selectedSource.name,
                        options = sensorRuntime.availableSources,
                        onSourceSelected = { viewModel.selectCaptureSource(it.source) },
                    )
                    UsbDeviceSelector(
                        sensorRuntime = sensorRuntime,
                        onSelectDevice = viewModel::selectUsbDevice,
                        onRequestPermission = viewModel::requestUsbPermission,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Equipamento", fontWeight = FontWeight.Bold)
                    Text("${sessionContext?.sensorName ?: sensorRuntime.sensorName} • v${sessionContext?.sensorVersion ?: sensorRuntime.sensorVersion}")
                    Text(
                        sessionContext?.sensorStatusLabel ?: sensorRuntime.connectionState,
                        color = if ((sessionContext?.sensorStatusLabel ?: sensorRuntime.connectionState).contains("Erro", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFF2E7D32)
                        },
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Transporte: ${sessionContext?.sensorTransport ?: sensorRuntime.transport}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HardwareStatusBanner(sensorRuntime = sensorRuntime)
                }
            }
            Button(
                onClick = { viewModel.startSession(onSessionStarted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Iniciar Coleta")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureBiometricScreen(
    onBack: () -> Unit,
    onReview: () -> Unit,
    onFinish: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preview by viewModel.pendingCapture.collectAsStateWithLifecycle()
    val sessionProgress by viewModel.sessionProgress.collectAsStateWithLifecycle()
    val sensorRuntime by viewModel.sensorRuntime.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Captura Biométrica") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Hardware: ${sensorRuntime.sensorName} • ${sensorRuntime.connectionState}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HardwareStatusBanner(sensorRuntime = sensorRuntime)
            
            CollectionProgressCard(
                progress = sessionProgress,
                activeFinger = uiState.activeFinger,
            )
            
            Text("Selecione o dedo", fontWeight = FontWeight.SemiBold)
            FingerSelectionGrid(
                progress = sessionProgress,
                activeFinger = uiState.activeFinger,
                onSelectFinger = viewModel::updateFinger,
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Informações da captura", fontWeight = FontWeight.Bold)
                    Text("Qualidade: ${preview?.qualityScore ?: 82}%")
                    LinearProgressIndicator(
                        progress = { ((preview?.qualityScore ?: 82) / 100f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("FPS: ${preview?.fps ?: 10}")
                    Text(
                        "Dedo: ${fingerOptions.firstOrNull { it.first == uiState.activeFinger }?.second ?: uiState.activeFinger}",
                    )
                }
            }
            Button(
                onClick = { viewModel.capture(onReview) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isCapturing && sensorRuntime.isCaptureReady,
            ) {
                Text(if (uiState.isCapturing) "Capturando..." else "Capturar")
            }

            if (sessionProgress.isComplete) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Finalizar Coleta")
                }
            }

            if (!sensorRuntime.isCaptureReady && !uiState.isCapturing) {
                Text(
                    text = sensorRuntime.lastErrorMessage
                        ?: sensorRuntime.availableSources.firstOrNull { it.source == sensorRuntime.selectedSource }?.detailMessage
                        ?: "Prepare a fonte selecionada antes de capturar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.captureErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HardwareStatusBanner(
    sensorRuntime: com.example.biometrianeonatal.domain.model.SensorRuntimeInfo,
) {
    val bannerColor = when {
        sensorRuntime.connectionState.contains("Erro", ignoreCase = true) -> MaterialTheme.colorScheme.errorContainer
        sensorRuntime.connectionState.contains("Aguardando", ignoreCase = true) -> MaterialTheme.colorScheme.tertiaryContainer
        sensorRuntime.isCaptureReady -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Status do hardware", fontWeight = FontWeight.SemiBold)
            Text(sensorRuntime.connectionState)
            sensorRuntime.lastErrorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UsbDeviceSelector(
    sensorRuntime: com.example.biometrianeonatal.domain.model.SensorRuntimeInfo,
    onSelectDevice: (com.example.biometrianeonatal.domain.model.UsbDeviceOption) -> Unit,
    onRequestPermission: () -> Unit,
) {
    if (sensorRuntime.selectedSource != com.example.biometrianeonatal.domain.model.CaptureSource.USB_SENSOR && sensorRuntime.usbDevices.isEmpty()) {
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sensorRuntime.usbDevices.isNotEmpty()) {
            Text("Leitores USB detectados", fontWeight = FontWeight.SemiBold)
            sensorRuntime.usbDevices.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { device ->
                        AssistChip(
                            onClick = { onSelectDevice(device) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (device.isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            label = {
                                Text(
                                    if (device.hasPermission) device.label else "${device.label} (sem acesso)",
                                )
                            },
                        )
                    }
                }
            }
        }
        if (sensorRuntime.selectedSource == com.example.biometrianeonatal.domain.model.CaptureSource.USB_SENSOR && sensorRuntime.usbPermissionRequired) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Autorizar leitor USB")
            }
        }
    }
}

@Composable
private fun CaptureSourceSelector(
    selectedSourceName: String,
    options: List<com.example.biometrianeonatal.domain.model.CaptureSourceOption>,
    onSourceSelected: (com.example.biometrianeonatal.domain.model.CaptureSourceOption) -> Unit,
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    AssistChip(
                        onClick = { onSourceSelected(option) },
                        enabled = option.isAvailable,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedSourceName == option.source.name) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                        label = { Text(option.label) },
                    )
                }
            }
        }
        options.firstOrNull { it.source.name == selectedSourceName }?.detailMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { detailMessage ->
                Text(
                    text = detailMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureReviewScreen(
    onBack: () -> Unit,
    onContinueCollection: () -> Unit,
    onSessionCompleted: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preview by viewModel.pendingCapture.collectAsStateWithLifecycle()
    val sessionProgress by viewModel.sessionProgress.collectAsStateWithLifecycle()
    val willCompleteSession = preview?.fingerCode?.let { fingerCode ->
        if (fingerCode in sessionProgress.completedFingerCodes) {
            sessionProgress.isComplete
        } else {
            sessionProgress.completedCount + 1 >= sessionProgress.totalRequiredCount
        }
    } == true

    LaunchedEffect(preview?.imagePath) {
        if (preview != null) {
            viewModel.loadReviewArtifact()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Revisão da Digital") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.isLoadingReviewArtifact -> {
                        Text("Carregando imagem...", color = Color.White)
                    }
                    uiState.reviewArtifact?.mimeType?.startsWith("image/") == true -> {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                ImageView(context).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    adjustViewBounds = true
                                }
                            },
                            update = { imageView ->
                                imageView.setImageURI(uiState.reviewArtifact?.contentUri?.toUri())
                            },
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(0.5f),
                        )
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Informações da Captura", fontWeight = FontWeight.Bold)
                    Text("Qualidade: ${preview?.qualityScore ?: 0}%")
                    Text("Dedo: ${fingerOptions.firstOrNull { it.first == preview?.fingerCode }?.second ?: "-"}")
                    Text("Resolução: ${preview?.resolution ?: "-"}")
                    Text("Fonte: ${preview?.captureSource?.name ?: "-"}")
                    uiState.reviewArtifactErrorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            CollectionProgressCard(
                progress = sessionProgress,
                activeFinger = preview?.fingerCode,
            )
            Button(
                onClick = {
                    viewModel.acceptCapture(
                        onContinueCollection = onContinueCollection,
                        onSessionCompleted = onSessionCompleted,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = preview != null,
            ) {
                Text(if (willCompleteSession) {
                    "Aceitar e finalizar sessão"
                } else {
                    "Aceitar e continuar coleta"
                })
            }
            Button(
                onClick = {
                    viewModel.discardCapture()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Refazer captura")
            }
        }
    }
}

@Composable
private fun CollectionProgressCard(
    progress: SessionCaptureProgress,
    activeFinger: String?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Progresso da sessão", fontWeight = FontWeight.Bold)
            Text(
                text = "${progress.completedCount} de ${progress.totalRequiredCount} dedos concluídos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = {
                    if (progress.totalRequiredCount == 0) 0f
                    else progress.completedCount.toFloat() / progress.totalRequiredCount.toFloat()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CaptureGuidanceCard(
    progress: SessionCaptureProgress,
    activeFinger: String,
    selectedSourceName: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Orientação da coleta", fontWeight = FontWeight.Bold)
            Text(
                text = if (progress.isComplete) {
                    "Todos os dedos obrigatórios do demo já foram coletados. Você ainda pode recapturar um dedo específico."
                } else {
                    "Posicione o ${fingerOptions.firstOrNull { it.first == activeFinger }?.second ?: activeFinger} no sensor e mantenha o dispositivo estável."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Fonte ativa: $selectedSourceName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            progress.nextSuggestedFingerCode
                ?.takeIf { it != activeFinger }
                ?.let { nextFingerCode ->
                    Text(
                        text = "Depois desta etapa, o próximo dedo sugerido é ${fingerOptions.firstOrNull { it.first == nextFingerCode }?.second ?: nextFingerCode}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}

@Composable
private fun FingerSelectionGrid(
    progress: SessionCaptureProgress,
    activeFinger: String,
    onSelectFinger: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fingerOptions.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { (code, label) ->
                    val isCompleted = code in progress.completedFingerCodes
                    val isActive = activeFinger == code
                    val isSuggested = progress.nextSuggestedFingerCode == code
                    AssistChip(
                        onClick = { onSelectFinger(code) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                isCompleted && isActive -> MaterialTheme.colorScheme.secondaryContainer
                                isCompleted -> MaterialTheme.colorScheme.secondaryContainer
                                isActive -> MaterialTheme.colorScheme.primaryContainer
                                isSuggested -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                        ),
                        label = {
                            Text(
                                buildString {
                                    append(label)
                                    when {
                                        isCompleted -> append(" ✓")
                                        isSuggested -> append(" •")
                                    }
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}
