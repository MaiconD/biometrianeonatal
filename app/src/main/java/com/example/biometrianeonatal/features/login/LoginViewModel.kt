package com.example.biometrianeonatal.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrianeonatal.domain.model.Hospital
import com.example.biometrianeonatal.domain.usecase.auth.LoginUseCase
import com.example.biometrianeonatal.domain.usecase.auth.ObserveHospitalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado imutavel do formulario de login com credenciais, hospital e feedback de erro.
 */
data class LoginUiState(
    val email: String = "operador@utfpr.edu.br",
    val password: String = "123456",
    val selectedHospitalId: String = "hospital-pb",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * ViewModel do login que carrega hospitais e coordena a autenticacao.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    observeHospitalsUseCase: ObserveHospitalsUseCase,
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val hospitals: StateFlow<List<Hospital>> = observeHospitalsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun updateHospital(hospitalId: String) {
        _uiState.value = _uiState.value.copy(selectedHospitalId = hospitalId, errorMessage = null)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val state = _uiState.value
            val user = loginUseCase(
                email = state.email.trim(),
                password = state.password,
                hospitalId = state.selectedHospitalId,
            )
            if (user != null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Credenciais inválidas para a unidade selecionada.",
                )
            }
        }
    }
}

