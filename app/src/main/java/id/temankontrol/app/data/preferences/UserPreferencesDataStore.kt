package id.temankontrol.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.AppLockSettings
import id.temankontrol.app.domain.model.AppLockTimeout
import id.temankontrol.app.domain.model.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val NOTICE_ACCEPTED = booleanPreferencesKey("notice_accepted")
        val REMINDER_VISIT_ENABLED = booleanPreferencesKey("reminder_visit_enabled")
        val REMINDER_VISIT_HOUR = intPreferencesKey("reminder_visit_hour")
        val REMINDER_PREPARE_ENABLED = booleanPreferencesKey("reminder_prepare_enabled")
        val REMINDER_FOLLOWUP_ENABLED = booleanPreferencesKey("reminder_followup_enabled")
        val REMINDER_FOLLOWUP_HOUR = intPreferencesKey("reminder_followup_hour")
        val REMINDER_BACKUP_ENABLED = booleanPreferencesKey("reminder_backup_enabled")
        val NOTIF_SHOW_TITLE = booleanPreferencesKey("notif_show_title")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
        val SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val DATA_CHANGED_SINCE_BACKUP = booleanPreferencesKey("data_changed_since_backup")
    }

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { emit(emptyPreferences()) }

    val noticeAccepted: Flow<Boolean> = safeData.map { it[Keys.NOTICE_ACCEPTED] ?: false }

    val reminderSettings: Flow<ReminderSettings> = safeData.map { prefs ->
        ReminderSettings(
            visitReminderEnabled = prefs[Keys.REMINDER_VISIT_ENABLED] ?: true,
            visitReminderHour = prefs[Keys.REMINDER_VISIT_HOUR] ?: 19,
            prepareReminderEnabled = prefs[Keys.REMINDER_PREPARE_ENABLED] ?: true,
            followUpReminderEnabled = prefs[Keys.REMINDER_FOLLOWUP_ENABLED] ?: true,
            followUpReminderHour = prefs[Keys.REMINDER_FOLLOWUP_HOUR] ?: 8,
            backupReminderEnabled = prefs[Keys.REMINDER_BACKUP_ENABLED] ?: true,
            showTitleOnLockScreen = prefs[Keys.NOTIF_SHOW_TITLE] ?: false,
            hasNotificationPermission = true
        )
    }

    val appLockSettings: Flow<AppLockSettings> = safeData.map { prefs ->
        val timeoutName = prefs[Keys.APP_LOCK_TIMEOUT] ?: AppLockTimeout.ONE_MINUTE.name
        val timeout = runCatching { AppLockTimeout.valueOf(timeoutName) }.getOrDefault(AppLockTimeout.ONE_MINUTE)
        AppLockSettings(
            enabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            timeout = timeout,
            secureScreen = prefs[Keys.SECURE_SCREEN] ?: true,
            hasDeviceCredential = true
        )
    }

    val lastBackupAt: Flow<Instant?> = safeData.map { prefs ->
        prefs[Keys.LAST_BACKUP_AT]?.let { Instant.ofEpochMilli(it) }
    }

    val dataChangedSinceBackup: Flow<Boolean> = safeData.map { prefs ->
        prefs[Keys.DATA_CHANGED_SINCE_BACKUP] ?: true
    }

    suspend fun setNoticeAccepted(): AppResult<Unit> = safeEdit { prefs ->
        prefs[Keys.NOTICE_ACCEPTED] = true
    }

    suspend fun updateReminderSettings(transform: (ReminderSettings) -> ReminderSettings): AppResult<Unit> =
        safeEdit { prefs ->
            val current = ReminderSettings(
                visitReminderEnabled = prefs[Keys.REMINDER_VISIT_ENABLED] ?: true,
                visitReminderHour = prefs[Keys.REMINDER_VISIT_HOUR] ?: 19,
                prepareReminderEnabled = prefs[Keys.REMINDER_PREPARE_ENABLED] ?: true,
                followUpReminderEnabled = prefs[Keys.REMINDER_FOLLOWUP_ENABLED] ?: true,
                followUpReminderHour = prefs[Keys.REMINDER_FOLLOWUP_HOUR] ?: 8,
                backupReminderEnabled = prefs[Keys.REMINDER_BACKUP_ENABLED] ?: true,
                showTitleOnLockScreen = prefs[Keys.NOTIF_SHOW_TITLE] ?: false
            )
            val updated = transform(current)
            prefs[Keys.REMINDER_VISIT_ENABLED] = updated.visitReminderEnabled
            prefs[Keys.REMINDER_VISIT_HOUR] = updated.visitReminderHour
            prefs[Keys.REMINDER_PREPARE_ENABLED] = updated.prepareReminderEnabled
            prefs[Keys.REMINDER_FOLLOWUP_ENABLED] = updated.followUpReminderEnabled
            prefs[Keys.REMINDER_FOLLOWUP_HOUR] = updated.followUpReminderHour
            prefs[Keys.REMINDER_BACKUP_ENABLED] = updated.backupReminderEnabled
            prefs[Keys.NOTIF_SHOW_TITLE] = updated.showTitleOnLockScreen
        }

    suspend fun updateAppLockSettings(transform: (AppLockSettings) -> AppLockSettings): AppResult<Unit> =
        safeEdit { prefs ->
            val timeoutName = prefs[Keys.APP_LOCK_TIMEOUT] ?: AppLockTimeout.ONE_MINUTE.name
            val current = AppLockSettings(
                enabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
                timeout = runCatching { AppLockTimeout.valueOf(timeoutName) }.getOrDefault(AppLockTimeout.ONE_MINUTE),
                secureScreen = prefs[Keys.SECURE_SCREEN] ?: true
            )
            val updated = transform(current)
            prefs[Keys.APP_LOCK_ENABLED] = updated.enabled
            prefs[Keys.APP_LOCK_TIMEOUT] = updated.timeout.name
            prefs[Keys.SECURE_SCREEN] = updated.secureScreen
        }

    suspend fun recordBackupCreated(at: Instant): AppResult<Unit> = safeEdit { prefs ->
        prefs[Keys.LAST_BACKUP_AT] = at.toEpochMilli()
        prefs[Keys.DATA_CHANGED_SINCE_BACKUP] = false
    }

    suspend fun markDataChanged(): AppResult<Unit> = safeEdit { prefs ->
        prefs[Keys.DATA_CHANGED_SINCE_BACKUP] = true
    }

    private suspend fun safeEdit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit): AppResult<Unit> {
        return try {
            dataStore.edit { block(it) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
