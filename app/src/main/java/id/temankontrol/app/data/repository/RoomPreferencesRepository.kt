package id.temankontrol.app.data.repository

import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.preferences.UserPreferencesDataStore
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPreferencesRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : PreferencesRepository {

    override val noticeAccepted: Flow<Boolean> = dataStore.noticeAccepted

    override suspend fun setNoticeAccepted(): AppResult<Unit> {
        return dataStore.setNoticeAccepted()
    }
}
