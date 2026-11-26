package id.temankontrol.app.data.repository

import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.preferences.UserPreferencesDataStore
import id.temankontrol.app.domain.model.AccountSession
import id.temankontrol.app.domain.model.AppLockSettings
import id.temankontrol.app.domain.model.ReminderSettings
import id.temankontrol.app.domain.model.SyncEntityType
import id.temankontrol.app.domain.model.SyncTombstone
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPreferencesRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : PreferencesRepository {

    override val noticeAccepted: Flow<Boolean> = dataStore.noticeAccepted
    override val reminderSettings: Flow<ReminderSettings> = dataStore.reminderSettings
    override val appLockSettings: Flow<AppLockSettings> = dataStore.appLockSettings
    override val lastBackupAt: Flow<Instant?> = dataStore.lastBackupAt
    override val dataChangedSinceBackup: Flow<Boolean> = dataStore.dataChangedSinceBackup
    override val accountSession: Flow<AccountSession?> = dataStore.accountSession
    override val pendingTombstones: Flow<List<SyncTombstone>> = dataStore.pendingTombstones

    override suspend fun setNoticeAccepted(): AppResult<Unit> = dataStore.setNoticeAccepted()

    override suspend fun updateReminderSettings(
        transform: (ReminderSettings) -> ReminderSettings
    ): AppResult<Unit> = dataStore.updateReminderSettings(transform)

    override suspend fun updateAppLockSettings(
        transform: (AppLockSettings) -> AppLockSettings
    ): AppResult<Unit> = dataStore.updateAppLockSettings(transform)

    override suspend fun recordBackupCreated(at: Instant): AppResult<Unit> =
        dataStore.recordBackupCreated(at)

    override suspend fun markDataChanged(): AppResult<Unit> = dataStore.markDataChanged()

    override suspend fun saveAccountSession(session: AccountSession?): AppResult<Unit> =
        dataStore.saveAccountSession(session)

    override suspend fun updateLastSyncAt(epochMillis: Long): AppResult<Unit> =
        dataStore.updateLastSyncAt(epochMillis)

    override suspend fun recordSyncTombstone(
        entityType: SyncEntityType,
        entityId: String,
        deletedAtMillis: Long
    ): AppResult<Unit> = dataStore.recordSyncTombstone(entityType, entityId, deletedAtMillis)

    override suspend fun clearSyncTombstones(entityIds: Set<String>): AppResult<Unit> =
        dataStore.clearSyncTombstones(entityIds)
}
