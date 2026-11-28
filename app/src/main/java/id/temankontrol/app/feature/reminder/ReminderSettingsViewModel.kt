package id.temankontrol.app.feature.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.data.reminder.ReminderScheduler
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _hasPermission = MutableStateFlow(true)

    val uiState: StateFlow<ReminderSettings> = combine(
        preferencesRepository.reminderSettings,
        _hasPermission
    ) { settings, hasPermission ->
        settings.copy(hasNotificationPermission = hasPermission)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderSettings()
    )

    fun setVisitReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(visitReminderEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setVisitReminderHour(hour: Int) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(visitReminderHour = hour.coerceIn(6, 21)) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setVisitThirtyMinBeforeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(visitThirtyMinBeforeEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setVisitAtTimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(visitAtTimeEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setCustomLeadMinutes(minutes: Int?) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(customLeadMinutes = minutes) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setAlarmSoundModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(alarmSoundModeEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun triggerTestAlarm() {
        viewModelScope.launch {
            reminderScheduler.triggerTestAlarmNow()
        }
    }

    fun setPrepareReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(prepareReminderEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setFollowUpReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(followUpReminderEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setFollowUpReminderHour(hour: Int) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(followUpReminderHour = hour.coerceIn(6, 21)) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(backupReminderEnabled = enabled) }
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
        }
    }

    fun setShowTitleOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings { it.copy(showTitleOnLockScreen = show) }
            preferencesRepository.markDataChanged()
        }
    }

    fun setHasNotificationPermission(granted: Boolean) {
        val wasGranted = _hasPermission.value
        _hasPermission.value = granted
        if (granted && !wasGranted) {
            viewModelScope.launch {
                reminderScheduler.rescheduleAll()
            }
        }
    }
}
