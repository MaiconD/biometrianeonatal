package com.example.biometrianeonatal.features.history

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.designsystem.SectionTitle
import com.example.biometrianeonatal.core.designsystem.StatusBadge
import com.example.biometrianeonatal.domain.model.SessionCaptureRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    onContinueCollection: (String, String) -> Unit,
) {
    val viewModel: HistoryDetailViewModel = hiltViewModel()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Detalhe da coleta") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (detail == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Sessão não encontrada", fontWeight = FontWeight.SemiBold)
                            Text(
                                "O registro local desta coleta não está mais disponível.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(detail?.babyName.orEmpty(), fontWeight = FontWeight.Bold)
                        Text(
                            "Sessão #${detail?.id?.takeLast(4).orEmpty()} • ${detail?.sessionDate?.replace('T', ' ')?.take(16).orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Operador: ${detail?.operatorName.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                SectionTitle(
                    title = "Metadados da sessão",
                    subtitle = "Leitura local criptografada para auditoria e demonstração offline.",
                )
            }
            item {
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
                        Text("Hospital: ${detail?.hospitalName.orEmpty()}")
                        Text("Equipamento: ${detail?.deviceId.orEmpty()}")
                        Text("Serial do sensor: ${detail?.sensorSerial.orEmpty()}")
                        Text("Situação da sessão: ${detail?.status?.toUiLabel().orEmpty()}")
                        Text(
                            "Progresso: ${detail?.progress?.completedCount ?: 0} de ${detail?.progress?.totalRequiredCount ?: 0} dedos concluídos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        detail?.syncStatus?.let { syncStatus ->
                            StatusBadge(status = syncStatus)
                        }
                        if (!detail?.notes.isNullOrBlank()) {
                            Text(
                                "Notas: ${detail?.notes.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (detail?.status == SessionLifecycleStatus.IN_PROGRESS && detail?.progress?.isComplete == false) {
                            Button(
                                onClick = {
                                    onContinueCollection(
                                        detail?.babyId.orEmpty(),
                                        detail?.id.orEmpty(),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Continuar coleta")
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle(
                    title = "Capturas persistidas",
                    subtitle = "Abra uma captura armazenada com verificação de integridade local.",
                )
            }
            if (detail?.captures.isNullOrEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Nenhuma captura aceita", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Esta sessão ainda não possui impressões persistidas na base local.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            items(detail?.captures.orEmpty(), key = { it.id }) { capture ->
                SessionCaptureCard(
                    capture = capture,
                    isOpening = uiState.openingCaptureId == capture.id,
                    onOpenCapture = { viewModel.openCapture(capture.id) },
                )
            }
            if (uiState.openArtifactErrorMessage != null || uiState.openedArtifact != null) {
                item {
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
                            Text("Pré-visualização segura", fontWeight = FontWeight.SemiBold)
                            uiState.openArtifactErrorMessage?.let { message ->
                                Text(
                                    message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .background(Color.Black, shape = MaterialTheme.shapes.large),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    uiState.openedArtifact?.mimeType?.startsWith("image/") == true -> {
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { context ->
                                                ImageView(context).apply {
                                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                                    adjustViewBounds = true
                                                }
                                            },
                                            update = { imageView ->
                                                imageView.setImageURI(uiState.openedArtifact?.contentUri?.toUri())
                                            },
                                        )
                                    }
                                    uiState.openedArtifact != null -> {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = null,
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCaptureCard(
    capture: SessionCaptureRecord,
    isOpening: Boolean,
    onOpenCapture: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(capture.fingerCode.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                    Text(
                        capture.capturedAt.replace('T', ' ').take(16),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onOpenCapture, enabled = !isOpening) {
                    Text(if (isOpening) "Abrindo..." else "Abrir")
                }
            }
            Text("Qualidade: ${capture.qualityScore}%")
            Text("Resolução: ${capture.resolution}")
            Text("FPS: ${capture.fps}")
        }
    }
}

private fun SessionLifecycleStatus.toUiLabel(): String {
    return when (this) {
        SessionLifecycleStatus.DRAFT -> "Rascunho"
        SessionLifecycleStatus.IN_PROGRESS -> "Em andamento"
        SessionLifecycleStatus.COMPLETED -> "Concluída"
    }
}



