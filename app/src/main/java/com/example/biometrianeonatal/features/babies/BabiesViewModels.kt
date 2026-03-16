package com.example.biometrianeonatal.features.babies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.core.database.GuardianRelation
import com.example.biometrianeonatal.core.database.Sex
import com.example.biometrianeonatal.domain.model.BabyDraft
import com.example.biometrianeonatal.domain.model.BabyListItem
import com.example.biometrianeonatal.domain.model.BabyProfileSummary
import com.example.biometrianeonatal.domain.model.GuardianDraft
import com.example.biometrianeonatal.domain.model.Hospital
import com.example.biometrianeonatal.domain.usecase.auth.ObserveHospitalsUseCase
import com.example.biometrianeonatal.domain.usecase.babies.DeleteBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabiesUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabySummaryUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveGuardiansUseCase
import com.example.biometrianeonatal.domain.usecase.babies.SaveBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.SaveGuardiansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado do formulario de recem-nascido com valores digitados, erros e flags de salvamento.
 */
data class BabyFormUiState(
    val babyId: String? = null,
    val hospitalId: String = "hospital-pb",
    val name: String = "",
    val nameError: String? = null,
    val birthDate: String = "",
    val birthDateError: String? = null,
    val birthTime: String = "",
    val birthTimeError: String? = null,
    val sex: Sex = Sex.NAO_INFORMADO,
    val weightGrams: String = "",
    val weightError: String? = null,
    val heightCm: String = "",
    val heightError: String? = null,
    val medicalRecord: String = "",
    val observations: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * ViewModel que observa a listagem de bebes e executa exclusoes quando solicitadas.
 */
@HiltViewModel
class BabiesListViewModel @Inject constructor(
    observeBabiesUseCase: ObserveBabiesUseCase,
    private val deleteBabyUseCase: DeleteBabyUseCase,
) : ViewModel() {
    // A lista é mantida como StateFlow para atualização automática da UI sempre que o repositório mudar.
    val babies: StateFlow<List<BabyListItem>> = observeBabiesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun deleteBaby(babyId: String) {
        viewModelScope.launch {
            deleteBabyUseCase(babyId)
        }
    }
}

/**
 * ViewModel do cadastro/edicao de bebe com carregamento inicial, validacao e persistencia.
 */
@HiltViewModel
class BabyFormViewModel @Inject constructor(
    observeHospitalsUseCase: ObserveHospitalsUseCase,
    private val observeBabyUseCase: ObserveBabyUseCase,
    private val saveBabyUseCase: SaveBabyUseCase,
    private val deleteBabyUseCase: DeleteBabyUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Quando presente, indica que a tela está em modo edição e precisa hidratar o formulário existente.
    private val babyId: String? = savedStateHandle["babyId"]

    private val _uiState = MutableStateFlow(BabyFormUiState(babyId = babyId, isEditMode = babyId != null))
    val uiState: StateFlow<BabyFormUiState> = _uiState.asStateFlow()

    val hospitals: StateFlow<List<Hospital>> = observeHospitalsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        if (babyId != null) {
            viewModelScope.launch {
                // Carrega uma única vez o rascunho salvo e normaliza os campos para o formato exibido na UI.
                observeBabyUseCase(babyId).first()?.let { draft ->
                    _uiState.value = BabyFormUiState(
                        babyId = draft.id,
                        hospitalId = draft.hospitalId,
                        name = draft.name,
                        birthDate = toDisplayBirthDate(draft.birthDate),
                        birthTime = toDisplayBirthTime(draft.birthTime),
                        sex = draft.sex,
                        weightGrams = normalizeWeightInput(draft.weightGrams),
                        heightCm = normalizeHeightInput(draft.heightCm),
                        medicalRecord = draft.medicalRecord,
                        observations = draft.observations,
                        isEditMode = true,
                    )
                }
            }
        }
    }

    fun updateHospital(value: String) {
        _uiState.value = _uiState.value.copy(hospitalId = value, errorMessage = null)
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = null, errorMessage = null)
    }

    fun updateBirthDate(value: String) {
        _uiState.value = _uiState.value.copy(
            birthDate = toDisplayBirthDate(value),
            birthDateError = null,
            errorMessage = null,
        )
    }

    fun updateBirthTime(value: String) {
        _uiState.value = _uiState.value.copy(
            birthTime = toDisplayBirthTime(value),
            birthTimeError = null,
            errorMessage = null,
        )
    }

    fun updateSex(value: Sex) {
        _uiState.value = _uiState.value.copy(sex = value, errorMessage = null)
    }

    fun updateWeight(value: String) {
        _uiState.value = _uiState.value.copy(
            weightGrams = normalizeWeightInput(value),
            weightError = null,
            errorMessage = null,
        )
    }

    fun updateHeight(value: String) {
        _uiState.value = _uiState.value.copy(
            heightCm = normalizeHeightInput(value),
            heightError = null,
            errorMessage = null,
        )
    }

    fun updateMedicalRecord(value: String) {
        _uiState.value = _uiState.value.copy(medicalRecord = value, errorMessage = null)
    }

    fun updateObservations(value: String) {
        _uiState.value = _uiState.value.copy(observations = value, errorMessage = null)
    }

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        // Centraliza todas as regras síncronas de validação antes de disparar escrita no repositório.
        val validatedState = state.copy(
            nameError = if (state.name.isBlank()) "Informe o nome do bebê." else null,
            birthDateError = when {
                state.birthDate.isBlank() -> "Selecione a data de nascimento."
                !isValidBirthDate(state.birthDate) -> "Selecione uma data de nascimento válida."
                else -> null
            },
            birthTimeError = when {
                state.birthTime.isBlank() -> "Selecione a hora do nascimento."
                !isValidBirthTime(state.birthTime) -> "Selecione um horário válido."
                else -> null
            },
            weightError = when {
                state.weightGrams.isBlank() -> null
                state.weightGrams.toIntOrNull() == null -> "Peso deve conter apenas números."
                else -> null
            },
            heightError = when {
                state.heightCm.isBlank() -> null
                state.heightCm.toIntOrNull() == null -> "Altura deve conter apenas números."
                else -> null
            },
        )

        val hasValidationError = listOf(
            validatedState.nameError,
            validatedState.birthDateError,
            validatedState.birthTimeError,
            validatedState.weightError,
            validatedState.heightError,
        ).any { it != null }

        if (hasValidationError) {
            _uiState.value = validatedState.copy(errorMessage = "Preencha os campos destacados para salvar.")
            return
        }

        viewModelScope.launch {
            _uiState.value = validatedState.copy(isSaving = true, errorMessage = null)
            val currentState = _uiState.value
            // A conversão para o formato de armazenamento acontece aqui para manter a UI sempre amigável ao usuário.
            val babyId = saveBabyUseCase(
                BabyDraft(
                    id = currentState.babyId,
                    hospitalId = currentState.hospitalId,
                    name = currentState.name,
                    birthDate = toStorageBirthDate(currentState.birthDate),
                    birthTime = toDisplayBirthTime(currentState.birthTime),
                    sex = currentState.sex,
                    weightGrams = currentState.weightGrams,
                    heightCm = currentState.heightCm,
                    medicalRecord = currentState.medicalRecord,
                    observations = currentState.observations,
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSaved(babyId)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val babyId = _uiState.value.babyId ?: return
        viewModelScope.launch {
            deleteBabyUseCase(babyId)
            onDeleted()
        }
    }
}

/**
 * Estado imutavel `BabySummaryUiState` consumido pela interface Compose.
 */
data class BabySummaryUiState(
    val summary: BabyProfileSummary? = null,
    val guardians: List<GuardianDraft> = emptyList(),
)

/**
 * ViewModel `BabySummaryViewModel` que expoe estado reativo e acoes da respectiva feature.
 */
@HiltViewModel
class BabySummaryViewModel @Inject constructor(
    observeBabySummaryUseCase: ObserveBabySummaryUseCase,
    observeGuardiansUseCase: ObserveGuardiansUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val babyId: String = checkNotNull(savedStateHandle["babyId"])
    private val guardiansFlow = observeGuardiansUseCase(babyId)
    private val summaryUiState = MutableStateFlow(BabySummaryUiState())

    val uiState: StateFlow<BabySummaryUiState> = combine(
        observeBabySummaryUseCase(babyId),
        guardiansFlow,
        summaryUiState,
    ) { summary, guardians, _ ->
        BabySummaryUiState(
            summary = summary,
            guardians = guardians,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BabySummaryUiState(),
    )
}

/**
 * Estado imutavel `GuardiansUiState` consumido pela interface Compose.
 */
data class GuardiansUiState(
    val guardians: List<GuardianDraft> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showAddEditForm: Boolean = false,
    val editingGuardianIndex: Int = -1,
    val editingGuardianDraft: GuardianDraft? = null,
)

/**
 * ViewModel `GuardiansViewModel` que expoe estado reativo e acoes da respectiva feature.
 */
@HiltViewModel
class GuardiansViewModel @Inject constructor(
    private val observeGuardiansUseCase: ObserveGuardiansUseCase,
    private val saveGuardiansUseCase: SaveGuardiansUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val babyId: String = checkNotNull(savedStateHandle["babyId"])

    private val _uiState = MutableStateFlow(GuardiansUiState())
    val uiState: StateFlow<GuardiansUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedGuardians = observeGuardiansUseCase(babyId).first()
            val guardians = savedGuardians.map(::formatGuardianDraftForUi)
            _uiState.value = _uiState.value.copy(
                guardians = guardians,
            )
        }
    }

    fun startAddGuardian() {
        _uiState.value = _uiState.value.copy(
            showAddEditForm = true,
            editingGuardianIndex = -1,
            editingGuardianDraft = GuardianDraft(relation = GuardianRelation.RESPONSAVEL),
            errorMessage = null,
        )
    }

    fun startEditGuardian(index: Int) {
        val guardian = _uiState.value.guardians.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(
            showAddEditForm = true,
            editingGuardianIndex = index,
            editingGuardianDraft = guardian,
            errorMessage = null,
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            showAddEditForm = false,
            editingGuardianIndex = -1,
            editingGuardianDraft = null,
            errorMessage = null,
        )
    }

    fun updateEditingGuardian(draft: GuardianDraft) {
        _uiState.value = _uiState.value.copy(editingGuardianDraft = draft)
    }

    fun saveEditingGuardian() {
        val draft = _uiState.value.editingGuardianDraft ?: return
        val sanitized = sanitizeGuardianDraft(draft)
        
        if (sanitized.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Informe o nome completo.")
            return
        }
        if (sanitized.document.isNotBlank() && !isValidCpf(sanitized.document)) {
             _uiState.value = _uiState.value.copy(errorMessage = "CPF inválido.")
            return
        }
        if (sanitized.signatureBase64.isNullOrBlank()) {
             _uiState.value = _uiState.value.copy(errorMessage = "A assinatura é obrigatória.")
            return           
        }

        val currentList = _uiState.value.guardians.toMutableList()
        val index = _uiState.value.editingGuardianIndex

        if (index >= 0 && index < currentList.size) {
            currentList[index] = sanitized
        } else {
            currentList.add(sanitized)
        }

        // Optimistic update
        _uiState.value = _uiState.value.copy(
            guardians = currentList,
            showAddEditForm = false,
            editingGuardianDraft = null,
            editingGuardianIndex = -1,
        )
        
        saveRequests(currentList)
    }
    
    fun removeGuardian(index: Int) {
         val currentList = _uiState.value.guardians.toMutableList()
         if (index in currentList.indices) {
             currentList.removeAt(index)
             _uiState.value = _uiState.value.copy(guardians = currentList)
             saveRequests(currentList)
         }
    }

    private fun saveRequests(guardians: List<GuardianDraft>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching {
                 val normalizedGuardians = guardians.map(::sanitizeGuardianDraft)
                 saveGuardiansUseCase(babyId, normalizedGuardians)
            }.onSuccess {
                val persistedGuardians = observeGuardiansUseCase(babyId).first()
                _uiState.value = _uiState.value.copy(
                    guardians = persistedGuardians.map(::formatGuardianDraftForUi),
                    isSaving = false,
                )
            }.onFailure {
                 _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Erro ao salvar alterações: ${it.message}"
                )
            }
        }
    }
}

private fun sanitizeGuardianDraft(draft: GuardianDraft): GuardianDraft {
    return draft.copy(
        name = draft.name.trim(),
        document = draft.document.filter(Char::isDigit).take(11),
        phone = draft.phone.filter(Char::isDigit).take(11),
        addressCity = draft.addressCity.trim(),
        addressState = normalizeStateCode(draft.addressState),
        addressLine = draft.addressLine.trim(),
    )
}

