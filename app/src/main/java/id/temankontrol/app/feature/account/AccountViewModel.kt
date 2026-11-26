package id.temankontrol.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.remote.CloudApiException
import id.temankontrol.app.data.remote.TemanKontrolApiClient
import id.temankontrol.app.data.reminder.ReminderScheduler
import id.temankontrol.app.data.sync.OfflineFirstSyncEngine
import id.temankontrol.app.domain.model.AccountSession
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.shared.dto.ResetPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val session: AccountSession? = null,
    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val syncEngine: OfflineFirstSyncEngine,
    private val apiClient: TemanKontrolApiClient,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    private val _busyMessage = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _status = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountUiState> = combine(
        preferencesRepository.accountSession,
        _busy,
        _busyMessage,
        _error,
        _status
    ) { session, busy, busyMsg, err, status ->
        AccountUiState(
            session = session,
            isBusy = busy,
            busyMessage = busyMsg,
            errorMessage = err,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountUiState()
    )

    fun clearFeedback() {
        _error.value = null
        _status.value = null
    }

    fun login(
        email: String,
        password: String,
        serverUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _busyMessage.value = "Masuk & memulihkan data akun Anda…"
            _error.value = null
            _status.value = null
            when (val res = syncEngine.login(email, password, serverUrl.trim())) {
                is AppResult.Success -> {
                    reminderScheduler.rescheduleAll()
                    val s = res.data
                    _status.value = "Berhasil masuk! Tersinkron (${s.pushedRecords} unggah, ${s.pulledRecords} pulih)."
                    _busy.value = false
                    _busyMessage.value = null
                    onSuccess()
                }
                is AppResult.Failure -> {
                    _error.value = extractMessage(res.error, "Gagal masuk ke akun.")
                    _busy.value = false
                    _busyMessage.value = null
                }
            }
        }
    }

    fun register(
        displayName: String,
        email: String,
        password: String,
        privacyAccepted: Boolean,
        serverUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _busyMessage.value = "Mendaftarkan akun & menyinkronkan data lokal…"
            _error.value = null
            _status.value = null
            when (val res = syncEngine.register(email, password, displayName, privacyAccepted, serverUrl.trim())) {
                is AppResult.Success -> {
                    reminderScheduler.rescheduleAll()
                    val s = res.data
                    _status.value = "Akun dibuat & ${s.pushedRecords} data lokal diamankan."
                    _busy.value = false
                    _busyMessage.value = null
                    onSuccess()
                }
                is AppResult.Failure -> {
                    _error.value = extractMessage(res.error, "Gagal mendaftarkan akun.")
                    _busy.value = false
                    _busyMessage.value = null
                }
            }
        }
    }

    fun syncNow(forceFullRestore: Boolean = false) {
        viewModelScope.launch {
            _busy.value = true
            _busyMessage.value = if (forceFullRestore) {
                "Memulihkan data akun Anda…"
            } else {
                "Menyinkronkan data ke api.temankontrol.unand.tech…"
            }
            _error.value = null
            _status.value = null
            when (val res = syncEngine.syncNow(forceFullRestore)) {
                is AppResult.Success -> {
                    reminderScheduler.rescheduleAll()
                    val s = res.data
                    _status.value = "Tersinkron: ${s.pushedRecords} unggah, ${s.pulledRecords} unduh, ${s.uploadedFiles} file naik, ${s.downloadedFiles} file pulih."
                }
                is AppResult.Failure -> {
                    _error.value = extractMessage(res.error, "Gagal menyinkronkan data.")
                }
            }
            _busy.value = false
            _busyMessage.value = null
        }
    }

    fun requestOtp(email: String, serverUrl: String, onDispatched: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val res = apiClient.forgotPassword(email.trim(), serverUrl.trim())
                onDispatched(res.message)
            } catch (e: CloudApiException) {
                _error.value = e.message
            } catch (_: Exception) {
                _error.value = "Gagal menghubungi server untuk kode OTP."
            } finally {
                _busy.value = false
            }
        }
    }

    fun confirmResetPassword(
        email: String,
        otpCode: String,
        newPassword: String,
        serverUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                apiClient.resetPassword(
                    ResetPasswordRequest(email.trim(), otpCode.trim(), newPassword),
                    serverUrl.trim()
                )
                _status.value = "Kata sandi berhasil diperbarui. Silakan masuk."
                onSuccess()
            } catch (e: CloudApiException) {
                _error.value = e.message
            } catch (_: Exception) {
                _error.value = "Gagal mereset kata sandi."
            } finally {
                _busy.value = false
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            syncEngine.logout()
            _busy.value = false
            onLoggedOut()
        }
    }

    fun deleteAccountPermanently(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _busyMessage.value = "Menghapus akun & seluruh data secara permanen…"
            _error.value = null
            when (val res = syncEngine.deleteAccountPermanently(password)) {
                is AppResult.Success -> {
                    reminderScheduler.rescheduleAll()
                    _busy.value = false
                    _busyMessage.value = null
                    onDeleted()
                }
                is AppResult.Failure -> {
                    _error.value = extractMessage(res.error, "Gagal menghapus akun.")
                    _busy.value = false
                    _busyMessage.value = null
                }
            }
        }
    }

    private fun extractMessage(err: AppError, fallback: String): String = when (err) {
        is AppError.Validation -> err.message
        else -> fallback
    }
}
