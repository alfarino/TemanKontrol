package id.temankontrol.app.data.repository

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.dao.AttachmentDao
import id.temankontrol.app.data.local.entity.AttachmentEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.data.media.MediaAttachmentManager
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.repository.AttachmentRepository
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.model.SyncEntityType
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val mediaManager: MediaAttachmentManager,
    private val preferencesRepository: PreferencesRepository
) : AttachmentRepository {

    companion object {
        const val MAX_PHOTOS_PER_CONSULTATION = 10
        const val MAX_VOICES_PER_CONSULTATION = 5
    }

    override fun observeByConsultation(id: ConsultationId): Flow<List<Attachment>> {
        return attachmentDao.observeByConsultation(id.value).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAll(): Flow<List<Attachment>> {
        return attachmentDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addAttachment(
        consultationId: ConsultationId,
        questionId: String?,
        target: AttachmentTarget,
        kind: AttachmentKind,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        durationMs: Long?,
        caption: String?,
        uriString: String?
    ): AppResult<String> {
        val captionValidation = Validation.validateCaptionText(caption)
        if (captionValidation is AppResult.Failure) return captionValidation
        val validatedCaption = (captionValidation as AppResult.Success).data

        return try {
            val currentCount = attachmentDao.countByConsultationAndKind(consultationId.value, kind.name)
            val limit = if (kind == AttachmentKind.PHOTO) MAX_PHOTOS_PER_CONSULTATION else MAX_VOICES_PER_CONSULTATION
            if (currentCount >= limit) {
                return AppResult.Failure(
                    AppError.Validation(
                        "attachment",
                        "Batas lampiran tercapai ($MAX_PHOTOS_PER_CONSULTATION foto, $MAX_VOICES_PER_CONSULTATION catatan suara per kunjungan)."
                    )
                )
            }

            val attId = "att-" + UUID.randomUUID().toString().take(8)
            val entity = AttachmentEntity(
                id = attId,
                consultationId = consultationId.value,
                questionId = questionId,
                target = target.name,
                kind = kind.name,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                caption = validatedCaption,
                uriString = uriString,
                createdAt = Instant.now().toEpochMilli()
            )
            attachmentDao.insert(entity)
            AppResult.Success(attId)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun updateCaption(attachmentId: String, caption: String?): AppResult<Unit> {
        val captionValidation = Validation.validateCaptionText(caption)
        if (captionValidation is AppResult.Failure) return captionValidation
        val validatedCaption = (captionValidation as AppResult.Success).data

        return try {
            val rows = attachmentDao.updateCaption(attachmentId, validatedCaption)
            if (rows > 0) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun delete(attachmentId: String): AppResult<Unit> {
        return try {
            val existing = attachmentDao.getById(attachmentId)
            val rows = attachmentDao.delete(attachmentId)
            if (rows > 0) {
                existing?.let { mediaManager.deleteFile(it.consultationId, it.fileName) }
                preferencesRepository.recordSyncTombstone(
                    SyncEntityType.ATTACHMENT,
                    attachmentId,
                    Instant.now().toEpochMilli()
                )
                preferencesRepository.markDataChanged()
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
