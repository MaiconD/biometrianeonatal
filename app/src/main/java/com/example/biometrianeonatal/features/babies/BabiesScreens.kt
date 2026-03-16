package com.example.biometrianeonatal.features.babies

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biometrianeonatal.core.database.GuardianRelation
import com.example.biometrianeonatal.core.database.SessionLifecycleStatus
import com.example.biometrianeonatal.core.database.Sex
import com.example.biometrianeonatal.core.database.UserRole
import com.example.biometrianeonatal.core.designsystem.AppDropdownField
import com.example.biometrianeonatal.core.designsystem.AppPickerField
import com.example.biometrianeonatal.core.designsystem.AppTextField
import com.example.biometrianeonatal.core.designsystem.FormSectionCard
import com.example.biometrianeonatal.core.designsystem.SectionTitle
import com.example.biometrianeonatal.core.designsystem.SignaturePad
import com.example.biometrianeonatal.core.designsystem.SingleChoiceTileGroup
import com.example.biometrianeonatal.core.designsystem.StatusBadge
import com.example.biometrianeonatal.core.security.AccessPolicy
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.GuardianDraft
import com.example.biometrianeonatal.domain.model.Hospital
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/**
 * Tela Compose de listagem dos bebes com busca, acoes permitidas e atalhos de fluxo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabiesListScreen(
    userRole: UserRole?,
    onBack: () -> Unit,
    onNewBaby: () -> Unit,
    onViewSummary: (String) -> Unit,
    onCollect: (String) -> Unit,
    onEditBaby: (String) -> Unit,
) {
    // ViewModel responsável por fornecer a lista de bebês
    val viewModel: BabiesListViewModel = hiltViewModel()
    // Estado reativo da lista de bebês
    val babies by viewModel.babies.collectAsStateWithLifecycle()
    // Permissões do usuário para ações na tela
    val canCreateOrEditBaby = AccessPolicy.canCreateOrEditBaby(userRole)
    val canManageGuardians = AccessPolicy.canManageGuardians(userRole)
    val canCollectBiometrics = AccessPolicy.canCollectBiometrics(userRole)
    // Estado do campo de busca
    var query by remember { mutableStateOf("") }

    // Filtra bebês conforme a busca
    val filteredBabies = remember(babies, query) {
        babies.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.hospitalName.contains(query, ignoreCase = true)
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
                title = { Text("Bebês") },
                actions = {
                    // Botão para adicionar novo bebê, se permitido
                    if (canCreateOrEditBaby) {
                        IconButton(onClick = onNewBaby) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        }
                    }
                },
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
                // Campo de busca
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar por bebê ou hospital") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                )
            }
            // Exibe aviso se o usuário só pode consultar
            if (!canCreateOrEditBaby && !canCollectBiometrics && !canManageGuardians) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Modo leitura", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Seu perfil permite apenas consultar os cadastros de recém-nascidos.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            // Exibe mensagem se não houver bebês
            if (filteredBabies.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Nenhum cadastro encontrado", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Cadastre um recém-nascido ou ajuste a busca para localizar registros ativos.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            // Lista de cartões de bebês
            items(filteredBabies, key = { it.id }) { baby ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = baby.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Nascimento: ${baby.birthDate} às ${baby.birthTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Unidade: ${baby.hospitalName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusBadge(status = baby.status)
                        }
                        // Botões de ação conforme permissão
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canCreateOrEditBaby) {
                                TextButton(onClick = { onEditBaby(baby.id) }) {
                                    Text("Editar")
                                }
                            }
                            if (canCollectBiometrics) {
                                Button(
                                    onClick = { onCollect(baby.id) },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Coletar")
                                }
                                TextButton(onClick = { onViewSummary(baby.id) }) {
                                    Text("Resumo")
                                }
                            } else {
                                Button(
                                    onClick = { onViewSummary(baby.id) },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Ver resumo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tela Compose `BabySummaryScreen` responsavel por uma etapa do fluxo apresentado ao usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabySummaryScreen(
    userRole: UserRole?,
    onBack: () -> Unit,
    onManageGuardians: (String) -> Unit,
    onStartCollection: (String) -> Unit,
    onResumeCollection: (String, String) -> Unit,
    onOpenLatestSession: (String) -> Unit,
) {
    val viewModel: BabySummaryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary
    val baby = summary?.baby
    val canViewSensitiveGuardianData = AccessPolicy.canViewSensitiveGuardianData(userRole)
    val canManageGuardians = AccessPolicy.canManageGuardians(userRole)
    val canCollectBiometrics = AccessPolicy.canCollectBiometrics(userRole)
    val canViewHistory = AccessPolicy.canViewHistory(userRole)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Resumo do bebê") },
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
                    Text(baby?.name ?: "Carregando...", fontWeight = FontWeight.Bold)
                    Text("Nascimento: ${baby?.birthDate.orEmpty()} às ${baby?.birthTime.orEmpty()}")
                    Text("Sexo: ${baby?.sex?.toDisplayLabel().orEmpty()}")
                    Text("Peso: ${baby?.weightGrams.orEmpty()} g")
                    Text("Altura: ${baby?.heightCm.orEmpty()} cm")
                    Text(
                        text = "Prontuário: ${baby?.medicalRecord?.maskMedicalRecord() ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    baby?.observations
                        ?.takeIf { it.isNotBlank() }
                        ?.let { observations ->
                            Text(
                                text = "Observações: $observations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }

            CollectionReadinessCard(
                summary = summary,
                canManageGuardians = canManageGuardians,
                canCollectBiometrics = canCollectBiometrics,
                canViewHistory = canViewHistory,
                onManageGuardians = onManageGuardians,
                onStartCollection = onStartCollection,
                onResumeCollection = onResumeCollection,
                onOpenLatestSession = onOpenLatestSession,
            )

            LatestSessionOverviewCard(summary = summary)

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
                    Text("Responsáveis", fontWeight = FontWeight.SemiBold)
                    
                    if (uiState.guardians.isEmpty()) {
                        Text(
                            "Nenhum responsável ativo vinculado a este cadastro.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        uiState.guardians.forEachIndexed { index, guardian ->
                            GuardianSummaryCard(
                                title = guardian.relation.toDisplayLabel(index),
                                draft = guardian,
                                canViewSensitiveGuardianData = canViewSensitiveGuardianData,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionReadinessCard(
    summary: BabyProfileSummary?,
    canManageGuardians: Boolean,
    canCollectBiometrics: Boolean,
    canViewHistory: Boolean,
    onManageGuardians: (String) -> Unit,
    onStartCollection: (String) -> Unit,
    onResumeCollection: (String, String) -> Unit,
    onOpenLatestSession: (String) -> Unit,
) {
    val containerColor = when {
        summary == null -> MaterialTheme.colorScheme.surfaceVariant
        summary.shouldResumeCollection -> MaterialTheme.colorScheme.tertiaryContainer
        summary.isReadyForCollection -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Prontidão para coleta", fontWeight = FontWeight.SemiBold)

            if (summary == null) {
                Text(
                    text = "Consolidando dados clínicos e biométricos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val readinessMessage = when {
                !summary.hasGuardians -> "Cadastre responsáveis para liberar a biometria offline."
                summary.shouldResumeCollection -> "Há uma sessão em andamento pronta para retomada imediata."
                summary.hasLatestSession -> "Cadastro pronto para nova coleta ou auditoria da sessão anterior."
                else -> "Cadastro pronto para iniciar a primeira coleta biométrica."
            }
            Text(text = readinessMessage, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Responsáveis ativos: ${summary.guardiansCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            summary.consentAcceptedAt
                ?.takeIf { it.isNotBlank() }
                ?.let { consentAcceptedAt ->
                    Text(
                        text = "Último aceite registrado: ${formatDateTimeLabel(consentAcceptedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            if (canCollectBiometrics) {
                Button(
                    onClick = {
                        summary.baby.id?.let { babyId ->
                            if (summary.shouldResumeCollection) {
                                summary.latestSessionId?.let { sessionId ->
                                    onResumeCollection(babyId, sessionId)
                                }
                            } else {
                                onStartCollection(babyId)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = summary.isReadyForCollection &&
                        (!summary.shouldResumeCollection || !summary.latestSessionId.isNullOrBlank()),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (summary.shouldResumeCollection) "Retomar coleta" else "Iniciar coleta biométrica")
                }
            }

            if (canManageGuardians) {
                TextButton(
                    onClick = { summary.baby.id?.let(onManageGuardians) },
                ) {
                    Text(if (summary.hasGuardians) "Atualizar responsáveis" else "Cadastrar responsáveis")
                }
            }

            if (canViewHistory && summary.hasLatestSession) {
                TextButton(
                    onClick = { summary.latestSessionId?.let(onOpenLatestSession) },
                ) {
                    Text("Abrir última sessão")
                }
            }
        }
    }
}

@Composable
private fun LatestSessionOverviewCard(
    summary: BabyProfileSummary?,
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
            Text("Última sessão biométrica", fontWeight = FontWeight.SemiBold)

            when {
                summary == null -> {
                    Text(
                        text = "Consolidando histórico local da última sessão...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                !summary.hasLatestSession -> {
                    Text(
                        text = "Nenhuma sessão biométrica foi registrada para este bebê até o momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = "Data: ${formatDateTimeLabel(summary.latestSessionDate.orEmpty())}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Situação: ${summary.latestSessionStatus?.toUiLabel().orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    summary.latestSessionSyncStatus?.let { syncStatus ->
                        StatusBadge(status = syncStatus)
                    }
                    Text(
                        text = "Digitais aceitas: ${summary.latestSessionProgress.completedCount} de ${summary.latestSessionProgress.totalRequiredCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (summary.latestSessionProgress.totalRequiredCount == 0) 0f
                            else summary.latestSessionProgress.completedCount.toFloat() /
                                summary.latestSessionProgress.totalRequiredCount.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    summary.latestSessionProgress.nextSuggestedFingerCode
                        ?.takeIf { !summary.latestSessionProgress.isComplete }
                        ?.let { fingerCode ->
                            Text(
                                text = "Próximo dedo sugerido: ${fingerCode.toFingerDisplayLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    if (summary.latestSessionProgress.isComplete) {
                        Text(
                            text = "Sessão concluída e pronta para auditoria local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tela Compose `BabyRegistrationScreen` responsavel por uma etapa do fluxo apresentado ao usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyRegistrationScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onManageGuardians: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val formViewModel: BabyFormViewModel = hiltViewModel()
    val uiState by formViewModel.uiState.collectAsStateWithLifecycle()
    val hospitals by formViewModel.hospitals.collectAsStateWithLifecycle()
    var isBirthDatePickerVisible by remember { mutableStateOf(false) }
    var isBirthTimePickerVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val selectedHospital = hospitals.firstOrNull { it.id == uiState.hospitalId }
    val selectedHospitalLabel = selectedHospital?.toUiLabel().orEmpty().ifBlank { uiState.hospitalId }
    val hospitalOptions = hospitals.map(Hospital::toUiLabel).ifEmpty {
        if (selectedHospitalLabel.isBlank()) emptyList() else listOf(selectedHospitalLabel)
    }

    if (isBirthDatePickerVisible) {
        BirthDatePickerModal(
            currentValue = uiState.birthDate,
            onDateSelected = formViewModel::updateBirthDate,
            onDismiss = { isBirthDatePickerVisible = false },
        )
    }

    if (isBirthTimePickerVisible) {
        BirthTimePickerModal(
            currentValue = uiState.birthTime,
            onTimeSelected = formViewModel::updateBirthTime,
            onDismiss = { isBirthTimePickerVisible = false },
        )
    }

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Excluir cadastro") },
            text = { Text("Tem certeza que deseja excluir este cadastro? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        formViewModel.delete {
                            onDelete()
                        }
                        showDeleteConfirmation = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(if (uiState.isEditMode) "Editar Recém-Nascido" else "Cadastro de Recém-Nascido") },
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { formViewModel.save(onSaved) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isSaving,
                    ) {
                        Text(
                            when {
                                uiState.isSaving -> "Salvando..."
                                uiState.isEditMode -> "Atualizar"
                                else -> "Salvar"
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(
                title = if (uiState.isEditMode) "Atualize os dados do recém-nascido" else "Preencha os dados clínicos iniciais",
            )

            FormSectionCard(
                title = "Dados clínicos",
            ) {
                uiState.errorMessage?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (hospitalOptions.isNotEmpty()) {
                    AppDropdownField(
                        value = selectedHospitalLabel,
                        label = "Unidade hospitalar",
                        options = hospitalOptions,
                        onOptionSelected = { label ->
                            hospitals.firstOrNull { it.toUiLabel() == label }?.let { hospital ->
                                formViewModel.updateHospital(hospital.id)
                            }
                        },
                    )
                }

                AppTextField(
                    value = uiState.name,
                    onValueChange = formViewModel::updateName,
                    label = "Nome do recém-nascido",
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppPickerField(
                        value = uiState.birthDate,
                        label = "Data de nascimento",
                        supportingText = uiState.birthDateError,
                        isError = uiState.birthDateError != null,
                        onClick = { isBirthDatePickerVisible = true },
                        modifier = Modifier.weight(1f),
                    )
                    AppPickerField(
                        value = uiState.birthTime,
                        label = "Hora do nascimento",
                        supportingText = uiState.birthTimeError,
                        isError = uiState.birthTimeError != null,
                        onClick = { isBirthTimePickerVisible = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Sexo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SingleChoiceTileGroup(
                        options = Sex.entries.toList(),
                        selectedOption = uiState.sex,
                        onSelectOption = formViewModel::updateSex,
                        optionLabel = { option -> option.toDisplayLabel() },
                        itemsPerRow = 2,
                        cornerRadiusDp = 14,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppTextField(
                        value = uiState.weightGrams,
                        onValueChange = formViewModel::updateWeight,
                        label = "Peso (g)",
                        modifier = Modifier.weight(1f),
                        isError = uiState.weightError != null,
                        supportingText = uiState.weightError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    AppTextField(
                        value = uiState.heightCm,
                        onValueChange = formViewModel::updateHeight,
                        label = "Altura (cm)",
                        modifier = Modifier.weight(1f),
                        isError = uiState.heightError != null,
                        supportingText = uiState.heightError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                AppTextField(
                    value = uiState.medicalRecord,
                    onValueChange = formViewModel::updateMedicalRecord,
                    label = "Prontuário hospitalar",
                    singleLine = true,
                )
                AppTextField(
                    value = uiState.observations,
                    onValueChange = formViewModel::updateObservations,
                    label = "Observações",
                )
            }

            if (onManageGuardians != null || onDelete != null) {
                FormSectionCard(
                    title = "Gerenciamento"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onManageGuardians != null) {
                            OutlinedButton(
                                onClick = onManageGuardians,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Gerenciar responsáveis")
                            }
                        }
                        if (onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            ) {
                                Text("Excluir cadastro")
                            }
                        }
                    }
                }
            }

        }
    }
}

/**
 * Tela Compose `GuardiansScreen` responsavel por uma etapa do fluxo apresentado ao usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardiansScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: GuardiansViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddEditForm) {
        uiState.editingGuardianDraft?.let { draft ->
            GuardianFormScreen(
                draft = draft,
                onUpdate = viewModel::updateEditingGuardian,
                onSave = viewModel::saveEditingGuardian,
                onCancel = viewModel::cancelEdit,
                isSaving = uiState.isSaving,
                errorMessage = uiState.errorMessage,
            )
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    },
                    title = { Text("Gerenciar Responsáveis") },
                )
            },
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(
                    onClick = viewModel::startAddGuardian,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Adicionar")
                }
            },
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = onSaved,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Concluir")
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.guardians.isEmpty()) {
                   item {
                       Text(
                           "Nenhum responsável cadastrado.",
                           modifier = Modifier.padding(16.dp),
                           style = MaterialTheme.typography.bodyMedium,
                           color = MaterialTheme.colorScheme.onSurfaceVariant
                       )
                   }
                }
                itemsIndexed(uiState.guardians) { index, guardian ->
                    GuardianListCard(
                        guardian = guardian,
                        onEdit = { viewModel.startEditGuardian(index) },
                        onDelete = { viewModel.removeGuardian(index) },
                        canRemove = uiState.guardians.size > 0 // Allow removing even if only one? Usually keep one but Logic handles it
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianListCard(
    guardian: GuardianDraft,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(guardian.name.ifBlank { "Nome não informado" }, fontWeight = FontWeight.Bold)
                Text(guardian.relation.toDisplayLabel(), style = MaterialTheme.typography.bodyMedium)
                if (guardian.signatureBase64 != null) {
                    Text("Assinado digitalmente", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                } else {
                     Text("Assinatura pendente", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, "Editar")
                }
                if (canRemove) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, "Remover", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuardianFormScreen(
    draft: GuardianDraft,
    onUpdate: (GuardianDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
) {
    val cpfDigits = draft.document.filter(Char::isDigit)
    val cpfError = when {
        draft.document.isBlank() -> null
        cpfDigits.length < 11 -> "Informe os 11 dígitos do CPF."
        !isValidCpf(draft.document) -> "CPF inválido."
        else -> null
    }
    val phoneDigits = draft.phone.filter(Char::isDigit)
    val phoneError = when {
        draft.phone.isBlank() -> null
        phoneDigits.length < 10 -> "Informe DDD e número."
        !isValidPhoneNumber(draft.phone) -> "Telefone inválido."
        else -> null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(if (draft.id == null) "Novo Responsável" else "Editar Responsável") },
            )
        },
        bottomBar = {
             Surface(shadowElevation = 8.dp) {
                Row(modifier = Modifier.padding(16.dp).navigationBarsPadding().fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f)) { 
                        Text(if (isSaving) "Salvando..." else "Salvar") 
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             Text("Vínculo", fontWeight = FontWeight.SemiBold)
            SingleChoiceTileGroup(
                options = GuardianRelation.entries.toList(),
                selectedOption = draft.relation,
                onSelectOption = { relation -> onUpdate(draft.copy(relation = relation)) },
                optionLabel = { relation -> relation.toDisplayLabel() },
                itemsPerRow = 2,
                cornerRadiusDp = 14,
            )

            AppTextField(
                value = draft.name,
                onValueChange = { onUpdate(draft.copy(name = it)) },
                label = "Nome completo",
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            AppTextField(
                value = draft.document,
                onValueChange = { onUpdate(draft.copy(document = normalizeDocumentInput(it))) },
                label = "Documento (CPF)",
                singleLine = true,
                isError = cpfError != null,
                supportingText = cpfError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            AppTextField(
                value = draft.phone,
                onValueChange = { onUpdate(draft.copy(phone = normalizePhoneInput(it))) },
                label = "Telefone",
                singleLine = true,
                isError = phoneError != null,
                supportingText = phoneError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            
            Text("Endereço", fontWeight = FontWeight.SemiBold)
            AppTextField(
                value = draft.addressCity,
                onValueChange = { onUpdate(draft.copy(addressCity = it)) },
                label = "Cidade",
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            AppDropdownField(
                value = draft.addressState,
                label = "UF",
                options = BrazilianStateOptions,
                onOptionSelected = { onUpdate(draft.copy(addressState = it)) },
            )
            AppTextField(
                value = draft.addressLine,
                onValueChange = { onUpdate(draft.copy(addressLine = it)) },
                label = "Logradouro, Nº, Bairro",
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            
             Text("Assinatura (Obrigatório)", fontWeight = FontWeight.SemiBold)
             Text("Responsável deve assinar abaixo para validar o cadastro.", style = MaterialTheme.typography.bodySmall)

             Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(200.dp)
                     .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                     .clip(RoundedCornerShape(8.dp))
             ) {
                 if (draft.signatureBase64 == null) {
                     SignaturePad(
                         modifier = Modifier.fillMaxSize(),
                         onSignatureChanged = { signature ->
                             if (signature != null) {
                                 onUpdate(draft.copy(signatureBase64 = signature))
                             }
                         }
                     )
                     Text("Assine aqui", modifier = Modifier.align(Alignment.Center), color = Color.LightGray)
                 } else {
                     Column(
                         modifier = Modifier.fillMaxSize(),
                         horizontalAlignment = Alignment.CenterHorizontally,
                         verticalArrangement = Arrangement.Center
                     ) {
                         Text("Assinatura Capturada", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                         TextButton(onClick = { onUpdate(draft.copy(signatureBase64 = null)) }) {
                             Text("Limpar / Assinar Novamente")
                         }
                     }
                 }
             }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianSummaryCard(
    title: String,
    draft: GuardianDraft,
    canViewSensitiveGuardianData: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                text = if (canViewSensitiveGuardianData) draft.name.ifBlank { "Não informado" } else draft.name.maskPersonName(),
                style = MaterialTheme.typography.bodyMedium,
            )
            
            if (draft.signatureBase64 != null) {
                Text(
                    text = "Assinado digitalmente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Assinatura pendente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Text(
                text = "CPF: ${draft.document.maskDocument()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Telefone: ${draft.phone.maskPhone()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Cidade/UF: ${listOf(draft.addressCity, draft.addressState).filter { it.isNotBlank() }.joinToString("/").ifBlank { "Não informado" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Endereço: ${draft.addressLine.maskAddressLine()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerModal(
    currentValue: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = currentValue.toBirthDatePickerMillis(),
    )

    // Force PT-BR locale for the DatePicker
    val configuration = LocalConfiguration.current
    val ptBrLocale = remember { Locale.Builder().setLanguage("pt").setRegion("BR").build() }
    
    // Create a NEW Configuration to ensure the locale is applied without mutating the original reference in a way that might be ignored
    val ptBrConfiguration = remember(configuration) {
        Configuration(configuration).apply {
            setLocale(ptBrLocale)
            setLayoutDirection(ptBrLocale)
        }
    }
    
    CompositionLocalProvider(
        LocalConfiguration provides ptBrConfiguration
    ) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toBirthDateDisplayValue()
                            ?.let(onDateSelected)
                        onDismiss()
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
                modifier = Modifier.heightIn(max = 550.dp), // Limit height for smaller screens
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthTimePickerModal(
    currentValue: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Force PT-BR locale for Time Picker labels
    val configuration = LocalConfiguration.current
    val ptBrLocale = remember { Locale.Builder().setLanguage("pt").setRegion("BR").build() }

    val ptBrConfiguration = remember(configuration) {
        Configuration(configuration).apply {
            setLocale(ptBrLocale)
            setLayoutDirection(ptBrLocale)
        }
    }

    CompositionLocalProvider(LocalConfiguration provides ptBrConfiguration) {
        val (initialHour, initialMinute) = remember(currentValue) {
            val parts = currentValue.split(':')
            val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            h to m
        }
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(formatBirthTime(timePickerState.hour, timePickerState.minute))
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Selecione a hora", style = MaterialTheme.typography.titleMedium)
                    // Use TimeInput for simpler text-based entry instead of the clock dial
                    TimeInput(state = timePickerState)
                }
            },
        )
    }
}

private fun Hospital.toUiLabel(): String {
    val cityLabel = listOf(city, state).filter { it.isNotBlank() }.joinToString("/")
    return if (cityLabel.isBlank()) name else "$name • $cityLabel"
}

private fun String.toBirthDatePickerMillis(): Long? {
    val storageDate = toStorageBirthDate(this)
    if (storageDate.isBlank()) return null
    val parsedDate = runCatching { LocalDate.parse(storageDate) }.getOrNull() ?: return null
    return parsedDate
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}

private fun Long.toBirthDateDisplayValue(): String {
    val selectedDate = Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    return toDisplayBirthDate(selectedDate.toString())
}

private fun GuardianRelation.toDisplayLabel(index: Int? = null): String {
    return when (this) {
        GuardianRelation.MAE -> "Mãe"
        GuardianRelation.PAI -> "Pai"
        GuardianRelation.RESPONSAVEL -> index?.let { "Responsável ${it + 1}" } ?: "Responsável"
    }
}

private fun Sex.toDisplayLabel(): String {
    return when (this) {
        Sex.MASCULINO -> "Masculino"
        Sex.FEMININO -> "Feminino"
        Sex.NAO_INFORMADO -> "Não informado"
    }
}

private fun String.maskPersonName(): String {
    if (isBlank()) return "Não informado"
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "Não informado"
        parts.size == 1 -> parts.first().first() + "***"
        else -> "${parts.first().first()}*** ${parts.last().first()}***"
    }
}

private fun String.maskDocument(): String {
    val digits = filter(Char::isDigit)
    return when {
        digits.isBlank() -> "Não informado"
        digits.length < 4 -> "***"
        else -> "***.***.***-${digits.takeLast(2)}"
    }
}

private fun String.maskPhone(): String {
    val digits = filter(Char::isDigit)
    return when {
        digits.isBlank() -> "Não informado"
        digits.length < 4 -> "(**) ****"
        else -> "(**) *****-${digits.takeLast(4)}"
    }
}

private fun String.maskAddressLine(): String {
    if (isBlank()) return "Não informado"
    return trim().split(',').firstOrNull()?.let { "$it, ***" } ?: "Endereço protegido"
}

private fun String.maskMedicalRecord(): String {
    if (isBlank()) return "Não informado"
    return if (length <= 4) "****" else "${take(2)}***${takeLast(2)}"
}

private fun String.toFingerDisplayLabel(): String {
    return when (this) {
        "POLEGAR_DIREITO" -> "Polegar direito"
        "INDICADOR_DIREITO" -> "Indicador direito"
        "POLEGAR_ESQUERDO" -> "Polegar esquerdo"
        else -> replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }
}

private fun formatDateTimeLabel(rawValue: String): String {
    return rawValue.replace('T', ' ').take(16).ifBlank { "-" }
}

private fun SessionLifecycleStatus.toUiLabel(): String {
    return when (this) {
        SessionLifecycleStatus.DRAFT -> "Rascunho"
        SessionLifecycleStatus.IN_PROGRESS -> "Em andamento"
        SessionLifecycleStatus.COMPLETED -> "Concluída"
    }
}



