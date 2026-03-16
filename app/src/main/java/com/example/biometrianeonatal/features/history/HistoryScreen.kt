package com.example.biometrianeonatal.features.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.designsystem.SectionTitle
import com.example.biometrianeonatal.core.designsystem.StatCard
import com.example.biometrianeonatal.core.designsystem.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSessionDetail: (String) -> Unit,
) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val completedCount = sessions.count { it.status == SessionLifecycleStatus.COMPLETED }
    val inProgressCount = sessions.count { it.status == SessionLifecycleStatus.IN_PROGRESS }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Histórico de coletas") },
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
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Linha do tempo offline", fontWeight = FontWeight.Bold)
                            Text(
                                "Visualize sessões já realizadas no dispositivo, mesmo sem backend configurado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Sessões",
                        value = sessions.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Concluídas",
                        value = completedCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Em andamento",
                        value = inProgressCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                SectionTitle(
                    title = "Últimas sessões",
                    subtitle = "Dados locais criptografados e ordenados da mais recente para a mais antiga.",
                )
            }
            if (sessions.isEmpty()) {
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
                            Text("Nenhuma sessão registrada", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Inicie uma coleta para que o histórico offline apareça aqui.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            items(sessions, key = { it.id }) { session ->
                Card(
                    onClick = { onOpenSessionDetail(session.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(session.babyName, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatSessionDateLabel(session.sessionDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Situação da sessão: ${session.status.toUiLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        StatusBadge(status = session.syncStatus)
                    }
                }
            }
        }
    }
}

private fun formatSessionDateLabel(rawValue: String): String {
    return rawValue.replace('T', ' ').take(16)
}

private fun SessionLifecycleStatus.toUiLabel(): String {
    return when (this) {
        SessionLifecycleStatus.DRAFT -> "Rascunho"
        SessionLifecycleStatus.IN_PROGRESS -> "Em andamento"
        SessionLifecycleStatus.COMPLETED -> "Concluída"
    }
}


