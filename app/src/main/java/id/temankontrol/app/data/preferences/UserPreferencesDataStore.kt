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
import id.temankontrol.app.domain.model.AccountSession
import id.temankontrol.app.domain.model.AppLockSettings
import id.temankontrol.app.domain.model.AppLockTimeout
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.model.SyncEntityType
import id.temankontrol.app.domain.model.SyncTombstone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val NOTICE_ACCEPTED = booleanPreferencesKey("notice_accepted")
        val REMINDER_VISIT_ENABLED = booleanPreferencesKey("reminder_visit_enabled")
        val REMINDER_VISIT_HOUR = intPreferencesKey("reminder_visit_hour")
        val REMINDER_VISIT_30M_ENABLED = booleanPreferencesKey("reminder_visit_30m_enabled")
        val REMINDER_VISIT_AT_TIME_ENABLED = booleanPreferencesKey("reminder_visit_at_time_enabled")
        val REMINDER_CUSTOM_LEAD_MINUTES = intPreferencesKey("reminder_custom_lead_minutes")
        val REMINDER_ALARM_SOUND_MODE = booleanPreferencesKey("reminder_alarm_sound_mode")
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
        val HAS_UNSYNCED_CLOUD_CHANGES = booleanPreferencesKey("has_unsynced_cloud_changes")
        val LAST_DATA_CHANGE_MILLIS = longPreferencesKey("last_data_change_millis")
        val ACCOUNT_SESSION_JSON = stringPreferencesKey("account_session_json")
        val LOCAL_DATA_OWNER_USER_ID = stringPreferencesKey("local_data_owner_user_id")
        val SYNC_TOMBSTONES_JSON = stringPreferencesKey("sync_tombstones_json")
    }

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { emit(emptyPreferences()) }

    val noticeAccepted: Flow<Boolean> = safeData.map { it[Keys.NOTICE_ACCEPTED] ?: false }

    val reminderSettings: Flow<ReminderSettings> = safeData.map { prefs ->
        val customLead = prefs[Keys.REMINDER_CUSTOM_LEAD_MINUTES]?.takeIf { it > 0 }
        ReminderSettings(
            visitReminderEnabled = prefs[Keys.REMINDER_VISIT_ENABLED] ?: true,
            visitReminderHour = prefs[Keys.REMINDER_VISIT_HOUR] ?: 19,
            visitThirtyMinBeforeEnabled = prefs[Keys.REMINDER_VISIT_30M_ENABLED] ?: true,
            visitAtTimeEnabled = prefs[Keys.REMINDER_VISIT_AT_TIME_ENABLED] ?: true,
            customLeadMinutes = customLead,
            alarmSoundModeEnabled = prefs[Keys.REMINDER_ALARM_SOUND_MODE] ?: true,
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

    val hasUnsyncedCloudChanges: Flow<Boolean> = safeData.map { prefs ->
        prefs[Keys.HAS_UNSYNCED_CLOUD_CHANGES] ?: false
    }

    val lastDataChangeMillis: Flow<Long> = safeData.map { prefs ->
        prefs[Keys.LAST_DATA_CHANGE_MILLIS] ?: 0L
    }

    val accountSession: Flow<AccountSession?> = safeData.map { prefs ->
        prefs[Keys.ACCOUNT_SESSION_JSON]?.let { raw ->
            runCatching { json.decodeFromString<AccountSession>(raw) }.getOrNull()
        }
    }

    val localDataOwnerUserId: Flow<String?> = safeData.map { prefs ->
        prefs[Keys.LOCAL_DATA_OWNER_USER_ID]
    }

    val pendingTombstones: Flow<List<SyncTombstone>> = safeData.map { prefs ->
        prefs[Keys.SYNC_TOMBSTONES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<SyncTombstone>>(raw) }.getOrNull()
        }.orEmpty()
    }

    suspend fun setNoticeAccepted(): AppResult<Unit> = safeEdit { prefs ->
        prefs[Keys.NOTICE_ACCEPTED] = true
    }

    suspend fun updateReminderSettings(transform: (ReminderSettings) -> ReminderSettings): AppResult<Unit> =
        safeEdit { prefs ->
            val current = ReminderSettings(
                visitReminderEnabled = prefs[Keys.REMINDER_VISIT_ENABLED] ?: true,
                visitReminderHour = prefs[Keys.REMINDER_VISIT_HOUR] ?: 19,
                visitThirtyMinBeforeEnabled = prefs[Keys.REMINDER_VISIT_30M_ENABLED] ?: true,
                visitAtTimeEnabled = prefs[Keys.REMINDER_VISIT_AT_TIME_ENABLED] ?: true,
                customLeadMinutes = prefs[Keys.REMINDER_CUSTOM_LEAD_MINUTES]?.takeIf { it > 0 },
                alarmSoundModeEnabled = prefs[Keys.REMINDER_ALARM_SOUND_MODE] ?: true,
                prepareReminderEnabled = prefs[Keys.REMINDER_PREPARE_ENABLED] ?: true,
                followUpReminderEnabled = prefs[Keys.REMINDER_FOLLOWUP_ENABLED] ?: true,
                followUpReminderHour = prefs[Keys.REMINDER_FOLLOWUP_HOUR] ?: 8,
                backupReminderEnabled = prefs[Keys.REMINDER_BACKUP_ENABLED] ?: true,
                showTitleOnLockScreen = prefs[Keys.NOTIF_SHOW_TITLE] ?: false
            )
            val updated = transform(current)
            prefs[Keys.REMINDER_VISIT_ENABLED] = updated.visitReminderEnabled
            prefs[Keys.REMINDER_VISIT_HOUR] = updated.visitReminderHour
            prefs[Keys.REMINDER_VISIT_30M_ENABLED] = updated.visitThirtyMinBeforeEnabled
            prefs[Keys.REMINDER_VISIT_AT_TIME_ENABLED] = updated.visitAtTimeEnabled
            prefs[Keys.REMINDER_CUSTOM_LEAD_MINUTES] = updated.customLeadMinutes ?: 0
            prefs[Keys.REMINDER_ALARM_SOUND_MODE] = updated.alarmSoundModeEnabled
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
        val now = System.currentTimeMillis()
        prefs[Keys.DATA_CHANGED_SINCE_BACKUP] = true
        prefs[Keys.HAS_UNSYNCED_CLOUD_CHANGES] = true
        prefs[Keys.LAST_DATA_CHANGE_MILLIS] = now
    }

    suspend fun markCloudChangesSynced(snapshotChangeMillis: Long): AppResult<Unit> = safeEdit { prefs ->
        val latest = prefs[Keys.LAST_DATA_CHANGE_MILLIS] ?: 0L
        // Clear pending flag only if no newer local edit occurred while sync was running
        if (latest <= snapshotChangeMillis) {
            prefs[Keys.HAS_UNSYNCED_CLOUD_CHANGES] = false
        }
    }

    suspend fun saveAccountSession(session: AccountSession?): AppResult<Unit> = safeEdit { prefs ->
        if (session == null) {
            prefs.remove(Keys.ACCOUNT_SESSION_JSON)
        } else {
            prefs[Keys.ACCOUNT_SESSION_JSON] = json.encodeToString(session)
        }
    }

    suspend fun saveLocalDataOwnerUserId(userId: String?): AppResult<Unit> = safeEdit { prefs ->
        if (userId.isNullOrBlank()) {
            prefs.remove(Keys.LOCAL_DATA_OWNER_USER_ID)
        } else {
            prefs[Keys.LOCAL_DATA_OWNER_USER_ID] = userId
        }
    }

    suspend fun clearAllSyncTombstones(): AppResult<Unit> = safeEdit { prefs ->
        prefs.remove(Keys.SYNC_TOMBSTONES_JSON)
    }

    suspend fun resetAccountScopedPreferences(): AppResult<Unit> = safeEdit { prefs ->
        prefs.remove(Keys.ACCOUNT_SESSION_JSON)
        prefs.remove(Keys.LOCAL_DATA_OWNER_USER_ID)
        prefs.remove(Keys.SYNC_TOMBSTONES_JSON)
        prefs.remove(Keys.LAST_BACKUP_AT)
        prefs.remove(Keys.DATA_CHANGED_SINCE_BACKUP)
        prefs.remove(Keys.HAS_UNSYNCED_CLOUD_CHANGES)
        prefs.remove(Keys.LAST_DATA_CHANGE_MILLIS)
        prefs.remove(Keys.REMINDER_VISIT_ENABLED)
        prefs.remove(Keys.REMINDER_VISIT_HOUR)
        prefs.remove(Keys.REMINDER_VISIT_30M_ENABLED)
        prefs.remove(Keys.REMINDER_VISIT_AT_TIME_ENABLED)
        prefs.remove(Keys.REMINDER_CUSTOM_LEAD_MINUTES)
        prefs.remove(Keys.REMINDER_ALARM_SOUND_MODE)
        prefs.remove(Keys.REMINDER_PREPARE_ENABLED)
        prefs.remove(Keys.REMINDER_FOLLOWUP_ENABLED)
        prefs.remove(Keys.REMINDER_FOLLOWUP_HOUR)
        prefs.remove(Keys.REMINDER_BACKUP_ENABLED)
        prefs.remove(Keys.NOTIF_SHOW_TITLE)
    }

    suspend fun updateLastSyncAt(epochMillis: Long): AppResult<Unit> = safeEdit { prefs ->
        val current = prefs[Keys.ACCOUNT_SESSION_JSON]?.let { raw ->
            runCatching { json.decodeFromString<AccountSession>(raw) }.getOrNull()
        }
        if (current != null) {
            prefs[Keys.ACCOUNT_SESSION_JSON] = json.encodeToString(current.copy(lastSyncAtMillis = epochMillis))
        }
    }

    suspend fun recordSyncTombstone(
        entityType: SyncEntityType,
        entityId: String,
        deletedAtMillis: Long
    ): AppResult<Unit> = safeEdit { prefs ->
        val existing = prefs[Keys.SYNC_TOMBSTONES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<SyncTombstone>>(raw) }.getOrNull()
        }.orEmpty()
        val updated = existing.filterNot { it.entityType == entityType && it.entityId == entityId } +
            SyncTombstone(entityType, entityId, deletedAtMillis)
        prefs[Keys.SYNC_TOMBSTONES_JSON] = json.encodeToString(updated)
    }

    suspend fun clearSyncTombstones(entityIds: Set<String>): AppResult<Unit> = safeEdit { prefs ->
        val existing = prefs[Keys.SYNC_TOMBSTONES_JSON]?.let { raw ->
            runCatching { json.decodeFromString<List<SyncTombstone>>(raw) }.getOrNull()
        }.orEmpty()
        val remaining = existing.filterNot { it.entityId in entityIds }
        prefs[Keys.SYNC_TOMBSTONES_JSON] = json.encodeToString(remaining)
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
