package id.temankontrol.app.feature.followup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.FollowUpInput
import id.temankontrol.app.domain.repository.FollowUpRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FollowUpUiState(
    val actions: List<FollowUpAction> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingAction: FollowUpAction? = null,
    val inputText: String = "",
    val inputDueDate: LocalDate? = null,
    val inputError: String? = null,
    val isDueDatePast: Boolean = false
)

@HiltViewModel
class FollowUpViewModel @Inject constructor(
    private val followUpRepository: FollowUpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val consultationIdString: String = checkNotNull(savedStateHandle["consultationId"])
    private val consultationId = ConsultationId(consultationIdString)

    private val _uiState = MutableStateFlow(FollowUpUiState())
    val uiState: StateFlow<FollowUpUiState> = _uiState.asStateFlow()

    init {
        observeActions()
    }

    private fun observeActions() {
        viewModelScope.launch {
            followUpRepository.observeByConsultation(consultationId).collect { list ->
                _uiState.value = _uiState.value.copy(
                    actions = list,
                    isLoading = false
                )
            }
        }
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingAction = null,
            inputText = "",
            inputDueDate = null,
            inputError = null,
            isDueDatePast = false
        )
    }

    fun openEditDialog(action: FollowUpAction) {
        val isPast = Validation.isDueDatePast(action.dueDate)
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingAction = action,
            inputText = action.text,
            inputDueDate = action.dueDate,
            inputError = null,
            isDueDatePast = isPast
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingAction = null, inputError = null)
    }

    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, inputError = null)
    }

    fun onDueDateChanged(date: LocalDate?) {
        val isPast = Validation.isDueDatePast(date)
        _uiState.value = _uiState.value.copy(inputDueDate = date, isDueDatePast = isPast)
    }

    fun saveAction() {
        val state = _uiState.value
        val input = FollowUpInput(
            text = state.inputText,
            dueDate = state.inputDueDate
        )

        val validation = Validation.validateFollowUpInput(input)
        if (validation is AppResult.Failure) {
            when (val err = validation.error) {
                is AppError.Validation -> _uiState.value = _uiState.value.copy(inputError = err.message)
                else -> _uiState.value = _uiState.value.copy(inputError = "Validasi gagal")
            }
            return
        }

        viewModelScope.launch {
            if (state.editingAction == null) {
                when (followUpRepository.create(consultationId, input)) {
                    is AppResult.Success -> closeDialog()
                    is AppResult.Failure -> _uiState.value = _uiState.value.copy(inputError = "Gagal membuat tindak lanjut")
                }
            } else {
                when (followUpRepository.update(state.editingAction.id, input)) {
                    is AppResult.Success -> closeDialog()
                    is AppResult.Failure -> _uiState.value = _uiState.value.copy(inputError = "Gagal memperbarui tindak lanjut")
                }
            }
        }
    }

    fun toggleCompleted(actionId: String, completed: Boolean) {
        viewModelScope.launch {
            followUpRepository.setCompleted(actionId, completed)
        }
    }

    fun deleteAction(actionId: String) {
        viewModelScope.launch {
            followUpRepository.delete(actionId)
        }
    }
}
