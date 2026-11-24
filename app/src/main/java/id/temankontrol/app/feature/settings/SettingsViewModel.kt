package id.temankontrol.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.domain.model.AppLockSettings
import id.temankontrol.app.domain.model.AppLockTimeout
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SettingsUiState(
    val lastBackupAt: Instant? = null,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val appLockSettings: AppLockSettings = AppLockSettings()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.lastBackupAt,
        preferencesRepository.reminderSettings,
        preferencesRepository.appLockSettings
    ) { lastBackup, reminder, lock ->
        SettingsUiState(
            lastBackupAt = lastBackup,
            reminderSettings = reminder,
            appLockSettings = lock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAppLockSettings { it.copy(enabled = enabled) }
        }
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        viewModelScope.launch {
            preferencesRepository.updateAppLockSettings { it.copy(timeout = timeout) }
        }
    }

    fun setSecureScreen(secure: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAppLockSettings { it.copy(secureScreen = secure) }
        }
    }
}
