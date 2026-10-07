package id.temankontrol.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val noticeAcceptedKey = booleanPreferencesKey("notice_accepted")

    val noticeAccepted: Flow<Boolean> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences[noticeAcceptedKey] ?: false
        }

    suspend fun setNoticeAccepted(): AppResult<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[noticeAcceptedKey] = true
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
