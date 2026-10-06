package id.temankontrol.app.feature.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationNote
import id.temankontrol.app.domain.model.NoteInput
import id.temankontrol.app.domain.repository.NoteRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteUiState(
    val summary: String = "",
    val providerInstruction: String = "",
    val additionalNotes: String = "",
    val summaryError: String? = null,
    val providerInstructionError: String? = null,
    val additionalNotesError: String? = null,
    val generalError: String? = null,
    val isExistingNote: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isDirty: Boolean = false
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val consultationIdString: String = checkNotNull(savedStateHandle["consultationId"])
    private val consultationId = ConsultationId(consultationIdString)

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val note = noteRepository.observe(consultationId).firstOrNull()
            if (note != null) {
                _uiState.value = _uiState.value.copy(
                    summary = note.summary ?: "",
                    providerInstruction = note.providerInstruction ?: "",
                    additionalNotes = note.additionalNotes ?: "",
                    isExistingNote = note.isNotEmpty(),
                    isLoading = false,
                    isDirty = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, isDirty = false)
            }
        }
    }

    fun onSummaryChange(text: String) {
        _uiState.value = _uiState.value.copy(summary = text, summaryError = null, generalError = null, isDirty = true)
    }

    fun onProviderInstructionChange(text: String) {
        _uiState.value = _uiState.value.copy(providerInstruction = text, providerInstructionError = null, generalError = null, isDirty = true)
    }

    fun onAdditionalNotesChange(text: String) {
        _uiState.value = _uiState.value.copy(additionalNotes = text, additionalNotesError = null, generalError = null, isDirty = true)
    }

    fun saveNote(onSuccess: () -> Unit) {
        val state = _uiState.value
        val input = NoteInput(
            summary = state.summary.ifBlank { null },
            providerInstruction = state.providerInstruction.ifBlank { null },
            additionalNotes = state.additionalNotes.ifBlank { null }
        )

        val validation = Validation.validateNoteInput(input)
        if (validation is AppResult.Failure) {
            when (val err = validation.error) {
                is AppError.Validation -> {
                    when (err.field) {
                        "note" -> _uiState.value = _uiState.value.copy(generalError = err.message)
                        "summary" -> _uiState.value = _uiState.value.copy(summaryError = err.message)
                        "providerInstruction" -> _uiState.value = _uiState.value.copy(providerInstructionError = err.message)
                        "additionalNotes" -> _uiState.value = _uiState.value.copy(additionalNotesError = err.message)
                    }
                }
                else -> _uiState.value = _uiState.value.copy(generalError = "Validasi gagal")
            }
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true)
        viewModelScope.launch {
            when (noteRepository.upsert(consultationId, input)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, isDirty = false)
                    onSuccess()
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, generalError = "Gagal menyimpan catatan")
                }
            }
        }
    }

    fun deleteNote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (noteRepository.delete(consultationId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isDirty = false)
                    onSuccess()
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(generalError = "Gagal menghapus catatan")
                }
            }
        }
    }
}
