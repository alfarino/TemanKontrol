package id.temankontrol.app.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.BackupSummaryInfo
import id.temankontrol.app.domain.model.StatusFilter
import id.temankontrol.app.domain.repository.AttachmentRepository
import id.temankontrol.app.domain.repository.BackupRepository
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.repository.FollowUpRepository
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class RestorePreview(
    val uriString: String,
    val fileName: String,
    val createdAt: Instant?,
    val appVersion: String,
    val isEncrypted: Boolean,
    val requiresPasswordPrompt: Boolean,
    val consultationCount: Int,
    val questionCount: Int,
    val attachmentCount: Int
)

data class BackupUiState(
    val summary: BackupSummaryInfo = BackupSummaryInfo(),
    val encryptWithPassword: Boolean = true,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isProcessing: Boolean = false,
    val exportSuccessFileName: String? = null,
    val exportSavedLocallyWarning: Boolean = false,
    val exportErrorMessage: String? = null,
    val restorePreview: RestorePreview? = null,
    val restorePassword: String = "",
    val restoreErrorMessage: String? = null,
    val restoreSuccessMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val followUpRepository: FollowUpRepository,
    private val attachmentRepository: AttachmentRepository,
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        observeSummary()
    }

    private fun observeSummary() {
        viewModelScope.launch {
            combine(
                consultationRepository.observeHistory(StatusFilter.ALL),
                followUpRepository.observeOpen(limit = 500),
                attachmentRepository.observeAll(),
                preferencesRepository.lastBackupAt
            ) { consultations, openFollowUps, attachments, lastBackup ->
                val totalQuestions = consultations.sumOf { it.questionCount }
                val attachmentBytes = attachments.sumOf { it.sizeBytes }
                val estBytes = 12_000L + (consultations.size * 1_500L) + attachmentBytes
                BackupSummaryInfo(
                    lastBackupAt = lastBackup,
                    hasUnsavedChangesSinceBackup = true,
                    consultationCount = consultations.size,
                    questionCount = totalQuestions,
                    noteCount = consultations.size,
                    followUpCount = openFollowUps.size,
                    attachmentCount = attachments.size,
                    estimatedSizeBytes = estBytes
                )
            }.collect { info ->
                _uiState.value = _uiState.value.copy(summary = info)
            }
        }
    }

    fun setEncryptWithPassword(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            encryptWithPassword = enabled,
            passwordError = null,
            confirmError = null
        )
    }

    fun setPassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null)
    }

    fun setConfirmPassword(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmError = null)
    }

    fun validateBeforeExport(): Boolean {
        val state = _uiState.value
        if (!state.encryptWithPassword) return true
        val res = Validation.validateBackupPassword(state.password, state.confirmPassword)
        if (res is AppResult.Failure) {
            val err = res.error as? AppError.Validation
            if (err?.field == "password") {
                _uiState.value = state.copy(passwordError = err.message)
            } else if (err?.field == "confirmPassword") {
                _uiState.value = state.copy(confirmError = err.message)
            }
            return false
        }
        return true
    }

    fun exportToUri(uriString: String) {
        if (!validateBeforeExport()) return
        val pwd = if (_uiState.value.encryptWithPassword) _uiState.value.password else null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                exportSuccessFileName = null,
                exportErrorMessage = null,
                restoreSuccessMessage = null
            )
            when (val res = backupRepository.exportToUri(uriString, pwd)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        exportSuccessFileName = res.data.fileName,
                        exportSavedLocallyWarning = res.data.isLocalDeviceStorage,
                        password = "",
                        confirmPassword = ""
                    )
                }
                is AppResult.Failure -> {
                    val msg = (res.error as? AppError.BackupError)?.message
                        ?: "Gagal membuat berkas cadangan."
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        exportErrorMessage = msg
                    )
                }
            }
        }
    }

    fun inspectRestoreUri(uriString: String, fileName: String, password: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                restoreErrorMessage = null,
                restoreSuccessMessage = null
            )
            when (val res = backupRepository.inspectFromUri(uriString, password)) {
                is AppResult.Success -> {
                    val data = res.data
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        restorePreview = RestorePreview(
                            uriString = uriString,
                            fileName = fileName,
                            createdAt = if (data.createdAtMillis > 0L) Instant.ofEpochMilli(data.createdAtMillis) else null,
                            appVersion = data.appVersion,
                            isEncrypted = data.isEncrypted,
                            requiresPasswordPrompt = data.requiresPasswordPrompt,
                            consultationCount = data.consultationCount,
                            questionCount = data.questionCount,
                            attachmentCount = data.attachmentCount
                        ),
                        restorePassword = password.orEmpty()
                    )
                }
                is AppResult.Failure -> {
                    val msg = (res.error as? AppError.BackupError)?.message
                        ?: "Cadangan tidak bisa dibuka."
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        restoreErrorMessage = msg
                    )
                }
            }
        }
    }

    fun setRestorePassword(pwd: String) {
        _uiState.value = _uiState.value.copy(restorePassword = pwd, restoreErrorMessage = null)
    }

    fun dismissRestorePreview() {
        _uiState.value = _uiState.value.copy(
            restorePreview = null,
            restorePassword = "",
            restoreErrorMessage = null
        )
    }

    fun confirmRestore() {
        val preview = _uiState.value.restorePreview ?: return
        val pwd = if (preview.isEncrypted) _uiState.value.restorePassword else null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, restoreErrorMessage = null)
            when (val res = backupRepository.restoreFromUri(preview.uriString, pwd)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        restorePreview = null,
                        restorePassword = "",
                        restoreSuccessMessage = "Pemulihan selesai. Seluruh data kunjungan dan lampiran berhasil dipulihkan."
                    )
                }
                is AppResult.Failure -> {
                    val msg = (res.error as? AppError.BackupError)?.message
                        ?: "Gagal memulihkan cadangan."
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        restoreErrorMessage = msg
                    )
                }
            }
        }
    }
}
