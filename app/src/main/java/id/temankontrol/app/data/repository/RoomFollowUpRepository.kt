package id.temankontrol.app.data.repository

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.dao.FollowUpDao
import id.temankontrol.app.data.local.entity.FollowUpActionEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.data.reminder.ReminderScheduler
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.FollowUpRepository
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFollowUpRepository @Inject constructor(
    private val followUpDao: FollowUpDao,
    private val reminderScheduler: ReminderScheduler,
    private val preferencesRepository: PreferencesRepository
) : FollowUpRepository {

    override fun observeByConsultation(id: ConsultationId): Flow<List<FollowUpAction>> {
        return followUpDao.observeByConsultation(id.value).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeOpen(limit: Int): Flow<List<OpenFollowUp>> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return followUpDao.observeOpen(todayEpochDay, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun get(actionId: String): AppResult<FollowUpAction> {
        val action = followUpDao.getById(actionId)
            ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(action.toDomain())
    }

    override suspend fun create(id: ConsultationId, input: FollowUpInput): AppResult<String> {
        val validationResult = Validation.validateFollowUpInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val actionId = "f-" + UUID.randomUUID().toString().take(8)
        val now = Instant.now().toEpochMilli()

        val entity = FollowUpActionEntity(
            id = actionId,
            consultationId = id.value,
            text = validated.text,
            dueDate = validated.dueDate?.toEpochDay(),
            isCompleted = false,
            completedAt = null,
            createdAt = now,
            updatedAt = now
        )

        return try {
            followUpDao.insert(entity)
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
            AppResult.Success(actionId)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun update(actionId: String, input: FollowUpInput): AppResult<Unit> {
        val validationResult = Validation.validateFollowUpInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val existing = followUpDao.getById(actionId)
            ?: return AppResult.Failure(AppError.NotFound)

        val updated = existing.copy(
            text = validated.text,
            dueDate = validated.dueDate?.toEpochDay(),
            updatedAt = Instant.now().toEpochMilli()
        )

        return try {
            followUpDao.update(updated)
            preferencesRepository.markDataChanged()
            reminderScheduler.rescheduleAll()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun setCompleted(actionId: String, completed: Boolean): AppResult<Unit> {
        val now = Instant.now().toEpochMilli()
        val completedAt = if (completed) now else null
        return try {
            val rows = followUpDao.setCompleted(actionId, completed, completedAt, now)
            if (rows > 0) {
                preferencesRepository.markDataChanged()
                reminderScheduler.rescheduleAll()
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun delete(actionId: String): AppResult<Unit> {
        return try {
            val rows = followUpDao.delete(actionId)
            if (rows > 0) {
                preferencesRepository.recordSyncTombstone(
                    SyncEntityType.FOLLOW_UP,
                    actionId,
                    Instant.now().toEpochMilli()
                )
                preferencesRepository.markDataChanged()
                reminderScheduler.rescheduleAll()
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
