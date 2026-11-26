package id.temankontrol.app.data.repository

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.dao.NoteDao
import id.temankontrol.app.data.local.entity.ConsultationNoteEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.NoteRepository
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val preferencesRepository: PreferencesRepository
) : NoteRepository {

    override fun observe(id: ConsultationId): Flow<ConsultationNote?> {
        return noteDao.observe(id.value).map { it?.toDomain() }
    }

    override suspend fun upsert(id: ConsultationId, input: NoteInput): AppResult<Unit> {
        val validationResult = Validation.validateNoteInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val entity = ConsultationNoteEntity(
            consultationId = id.value,
            summary = validated.summary,
            providerInstruction = validated.providerInstruction,
            additionalNotes = validated.additionalNotes,
            updatedAt = Instant.now().toEpochMilli()
        )

        return try {
            noteDao.upsert(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun delete(id: ConsultationId): AppResult<Unit> {
        return try {
            val rows = noteDao.delete(id.value)
            if (rows > 0) {
                preferencesRepository.recordSyncTombstone(
                    SyncEntityType.NOTE,
                    id.value,
                    Instant.now().toEpochMilli()
                )
                preferencesRepository.markDataChanged()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
