package id.temankontrol.app.feature.consultation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationInput
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ConsultationFormUiState(
    val id: String? = null,
    val title: String = "",
    val reason: String = "",
    val facilityName: String = "",
    val providerName: String = "",
    val appointmentDate: LocalDate = LocalDate.now().plusDays(1),
    val appointmentTime: LocalTime = LocalTime.of(9, 0),
    val titleError: String? = null,
    val reasonError: String? = null,
    val facilityError: String? = null,
    val providerError: String? = null,
    val isPastDate: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val initialTitle: String = "",
    val initialReason: String = "",
    val initialFacility: String = "",
    val initialProvider: String = ""
)

@HiltViewModel
class ConsultationFormViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editId: String? = savedStateHandle["id"]

    private val _uiState = MutableStateFlow(ConsultationFormUiState(id = editId))
    val uiState: StateFlow<ConsultationFormUiState> = _uiState.asStateFlow()

    init {
        if (!editId.isNullOrBlank()) {
            loadExisting(editId)
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val consultation = consultationRepository.observe(ConsultationId(id)).firstOrNull()
            if (consultation != null) {
                val zonedDateTime = consultation.appointmentAt.atZone(ZoneId.systemDefault())
                val date = zonedDateTime.toLocalDate()
                val time = zonedDateTime.toLocalTime()
                val isPast = date.isBefore(LocalDate.now())

                _uiState.value = _uiState.value.copy(
                    title = consultation.title,
                    reason = consultation.reason,
                    facilityName = consultation.facilityName ?: "",
                    providerName = consultation.providerName ?: "",
                    appointmentDate = date,
                    appointmentTime = time,
                    isPastDate = isPast,
                    isLoading = false,
                    initialTitle = consultation.title,
                    initialReason = consultation.reason,
                    initialFacility = consultation.facilityName ?: "",
                    initialProvider = consultation.providerName ?: "",
                    isDirty = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value,
            titleError = null,
            isDirty = true
        )
    }

    fun onReasonChange(value: String) {
        _uiState.value = _uiState.value.copy(
            reason = value,
            reasonError = null,
            isDirty = true
        )
    }

    fun onFacilityChange(value: String) {
        _uiState.value = _uiState.value.copy(
            facilityName = value,
            facilityError = null,
            isDirty = true
        )
    }

    fun onProviderChange(value: String) {
        _uiState.value = _uiState.value.copy(
            providerName = value,
            providerError = null,
            isDirty = true
        )
    }

    fun onDateChange(date: LocalDate) {
        val isPast = date.isBefore(LocalDate.now())
        _uiState.value = _uiState.value.copy(
            appointmentDate = date,
            isPastDate = isPast,
            isDirty = true
        )
    }

    fun onTimeChange(time: LocalTime) {
        _uiState.value = _uiState.value.copy(
            appointmentTime = time,
            isDirty = true
        )
    }

    fun submit(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting) return

        val instant = state.appointmentDate
            .atTime(state.appointmentTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val input = ConsultationInput(
            title = state.title,
            appointmentAt = instant,
            reason = state.reason,
            facilityName = state.facilityName.ifBlank { null },
            providerName = state.providerName.ifBlank { null }
        )

        val validation = Validation.validateConsultationInput(input)
        if (validation is AppResult.Failure) {
            when (val err = validation.error) {
                is AppError.Validation -> {
                    when (err.field) {
                        "title" -> _uiState.value = _uiState.value.copy(titleError = err.message)
                        "reason" -> _uiState.value = _uiState.value.copy(reasonError = err.message)
                        "facilityName" -> _uiState.value = _uiState.value.copy(facilityError = err.message)
                        "providerName" -> _uiState.value = _uiState.value.copy(providerError = err.message)
                    }
                }
                else -> onError("Validasi gagal")
            }
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            if (state.id == null) {
                when (val result = consultationRepository.create(input)) {
                    is AppResult.Success -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, isDirty = false)
                        onSuccess(result.data.value)
                    }
                    is AppResult.Failure -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                        onError("Gagal menyimpan kunjungan")
                    }
                }
            } else {
                when (consultationRepository.update(ConsultationId(state.id), input)) {
                    is AppResult.Success -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, isDirty = false)
                        onSuccess(state.id)
                    }
                    is AppResult.Failure -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                        onError("Gagal memperbarui kunjungan")
                    }
                }
            }
        }
    }
}
