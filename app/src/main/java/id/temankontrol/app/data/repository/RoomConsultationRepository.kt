package id.temankontrol.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.dao.ConsultationDao
import id.temankontrol.app.data.local.entity.ConsultationEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomConsultationRepository @Inject constructor(
    private val consultationDao: ConsultationDao
) : ConsultationRepository {

    private fun getStartOfTodayMillis(): Long {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    override fun observeUpcoming(limit: Int): Flow<List<ConsultationSummary>> {
        val startOfToday = getStartOfTodayMillis()
        return consultationDao.observeUpcoming(startOfToday, limit).map { list ->
            list.map { it.toDomain(startOfToday) }
        }
    }

    override fun observePastDueCount(): Flow<Int> {
        return consultationDao.observePastDueCount(getStartOfTodayMillis())
    }

    override fun observeHistory(filter: StatusFilter): Flow<List<ConsultationSummary>> {
        val startOfToday = getStartOfTodayMillis()
        val statusString = when (filter) {
            StatusFilter.ALL -> null
            StatusFilter.SCHEDULED -> ConsultationStatus.SCHEDULED.name
            StatusFilter.COMPLETED -> ConsultationStatus.COMPLETED.name
            StatusFilter.CANCELLED -> ConsultationStatus.CANCELLED.name
        }
        return consultationDao.observeHistory(statusString).map { list ->
            list.map { it.toDomain(startOfToday) }
        }
    }

    override fun observe(id: ConsultationId): Flow<Consultation?> {
        return consultationDao.observeById(id.value).map { it?.toDomain() }
    }

    override suspend fun create(input: ConsultationInput): AppResult<ConsultationId> {
        val validationResult = Validation.validateConsultationInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val newId = ConsultationId("c-" + UUID.randomUUID().toString().take(8))
        val now = Instant.now().toEpochMilli()
        val entity = ConsultationEntity(
            id = newId.value,
            title = validated.title,
            appointmentAt = validated.appointmentAt.toEpochMilli(),
            reason = validated.reason,
            facilityName = validated.facilityName,
            providerName = validated.providerName,
            status = ConsultationStatus.SCHEDULED.name,
            createdAt = now,
            updatedAt = now
        )

        return try {
            consultationDao.insert(entity)
            AppResult.Success(newId)
        } catch (e: SQLiteConstraintException) {
            AppResult.Failure(AppError.Duplicate)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun update(id: ConsultationId, input: ConsultationInput): AppResult<Unit> {
        val validationResult = Validation.validateConsultationInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val existing = consultationDao.getById(id.value)
            ?: return AppResult.Failure(AppError.NotFound)

        val updated = existing.copy(
            title = validated.title,
            appointmentAt = validated.appointmentAt.toEpochMilli(),
            reason = validated.reason,
            facilityName = validated.facilityName,
            providerName = validated.providerName,
            updatedAt = Instant.now().toEpochMilli()
        )

        return try {
            consultationDao.update(updated)
            AppResult.Success(Unit)
        } catch (e: SQLiteConstraintException) {
            AppResult.Failure(AppError.Duplicate)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun setStatus(id: ConsultationId, status: ConsultationStatus): AppResult<Unit> {
        return try {
            val rows = consultationDao.setStatus(id.value, status.name, Instant.now().toEpochMilli())
            if (rows > 0) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun delete(id: ConsultationId): AppResult<Unit> {
        return try {
            val rows = consultationDao.delete(id.value)
            if (rows > 0) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun childCounts(id: ConsultationId): AppResult<ChildCounts> {
        return try {
            val qCount = consultationDao.getQuestionCount(id.value)
            val hasNote = consultationDao.hasNote(id.value)
            val aCount = consultationDao.getActionCount(id.value)
            AppResult.Success(ChildCounts(qCount, hasNote, aCount))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
