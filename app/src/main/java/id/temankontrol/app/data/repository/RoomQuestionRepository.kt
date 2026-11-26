package id.temankontrol.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.data.local.dao.AttachmentDao
import id.temankontrol.app.data.local.dao.QuestionDao
import id.temankontrol.app.data.local.entity.QuestionEntity
import id.temankontrol.app.data.local.mapper.toDomain
import id.temankontrol.app.data.media.MediaAttachmentManager
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.CopyQuestionsResult
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.domain.repository.QuestionRepository
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomQuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val attachmentDao: AttachmentDao,
    private val mediaManager: MediaAttachmentManager,
    private val preferencesRepository: PreferencesRepository
) : QuestionRepository {

    override fun observeByConsultation(id: ConsultationId): Flow<List<Question>> {
        return questionDao.observeByConsultation(id.value).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun get(questionId: String): AppResult<Question> {
        val question = questionDao.getById(questionId)
            ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(question.toDomain())
    }

    override suspend fun create(id: ConsultationId, input: QuestionInput): AppResult<String> {
        val textValidation = Validation.validateQuestionText(input.text)
        if (textValidation is AppResult.Failure) return textValidation
        val validatedText = (textValidation as AppResult.Success).data

        val answerValidation = Validation.validateAnswerText(input.answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val currentPriorityCount = questionDao.priorityCount(id.value, null)
        val priorityCheck = Validation.validatePriorityLimit(currentPriorityCount, input.isPriority)
        if (priorityCheck is AppResult.Failure) return priorityCheck

        val normalizedText = Validation.normalizeQuestionForDuplicateCheck(validatedText)
        val qId = "q-" + UUID.randomUUID().toString().take(8)
        val now = Instant.now().toEpochMilli()

        val entity = QuestionEntity(
            id = qId,
            consultationId = id.value,
            text = validatedText,
            textNormalized = normalizedText,
            isPriority = input.isPriority,
            isDiscussed = false,
            createdAt = now,
            updatedAt = now,
            answer = validatedAnswer,
            answerUpdatedAt = if (validatedAnswer != null) now else null
        )

        return try {
            questionDao.insert(entity)
            AppResult.Success(qId)
        } catch (e: SQLiteConstraintException) {
            AppResult.Failure(AppError.Duplicate)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun update(questionId: String, input: QuestionInput): AppResult<Unit> {
        val textValidation = Validation.validateQuestionText(input.text)
        if (textValidation is AppResult.Failure) return textValidation
        val validatedText = (textValidation as AppResult.Success).data

        val answerValidation = Validation.validateAnswerText(input.answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val existing = questionDao.getById(questionId)
            ?: return AppResult.Failure(AppError.NotFound)

        val currentPriorityCount = questionDao.priorityCount(existing.consultationId, questionId)
        val priorityCheck = Validation.validatePriorityLimit(currentPriorityCount, input.isPriority)
        if (priorityCheck is AppResult.Failure) return priorityCheck

        val normalizedText = Validation.normalizeQuestionForDuplicateCheck(validatedText)
        val now = Instant.now().toEpochMilli()
        val answerUpdatedAt = when {
            validatedAnswer == null -> null
            validatedAnswer != existing.answer -> now
            else -> existing.answerUpdatedAt ?: now
        }

        val updated = existing.copy(
            text = validatedText,
            textNormalized = normalizedText,
            isPriority = input.isPriority,
            updatedAt = now,
            answer = validatedAnswer,
            answerUpdatedAt = answerUpdatedAt
        )

        return try {
            questionDao.update(updated)
            AppResult.Success(Unit)
        } catch (e: SQLiteConstraintException) {
            AppResult.Failure(AppError.Duplicate)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun updateAnswer(questionId: String, answer: String?): AppResult<Unit> {
        val answerValidation = Validation.validateAnswerText(answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val now = Instant.now().toEpochMilli()
        return try {
            val rows = questionDao.updateAnswer(
                id = questionId,
                answer = validatedAnswer,
                answerUpdatedAt = if (validatedAnswer != null) now else null,
                updatedAt = now
            )
            if (rows > 0) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun setDiscussed(questionId: String, discussed: Boolean): AppResult<Unit> {
        return try {
            val rows = questionDao.setDiscussed(questionId, discussed, Instant.now().toEpochMilli())
            if (rows > 0) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun delete(questionId: String): AppResult<Unit> {
        return try {
            val qAttachments = attachmentDao.getByQuestionId(questionId)
            val rows = questionDao.delete(questionId)
            if (rows > 0) {
                val nowMs = Instant.now().toEpochMilli()
                qAttachments.forEach {
                    mediaManager.deleteFile(it.consultationId, it.fileName)
                    preferencesRepository.recordSyncTombstone(SyncEntityType.ATTACHMENT, it.id, nowMs)
                }
                preferencesRepository.recordSyncTombstone(SyncEntityType.QUESTION, questionId, nowMs)
                preferencesRepository.markDataChanged()
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.NotFound)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun priorityCount(id: ConsultationId, excludeQuestionId: String?): AppResult<Int> {
        return try {
            val count = questionDao.priorityCount(id.value, excludeQuestionId)
            AppResult.Success(count)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }

    override suspend fun copyUnansweredQuestions(
        fromConsultationId: ConsultationId,
        toConsultationId: ConsultationId,
        questionIds: Set<String>?
    ): AppResult<CopyQuestionsResult> {
        return try {
            val sourceQuestions = questionDao.observeByConsultation(fromConsultationId.value).first()
                .filter { !it.isDiscussed && (questionIds == null || it.id in questionIds) }
            val existingTarget = questionDao.observeByConsultation(toConsultationId.value).first()
                .map { it.textNormalized }
                .toMutableSet()

            val now = Instant.now().toEpochMilli()
            var copied = 0
            var skipped = 0
            for (q in sourceQuestions) {
                if (q.textNormalized in existingTarget) {
                    skipped++
                } else {
                    existingTarget.add(q.textNormalized)
                    val newId = "q-" + UUID.randomUUID().toString().take(8)
                    questionDao.insert(
                        QuestionEntity(
                            id = newId,
                            consultationId = toConsultationId.value,
                            text = q.text,
                            textNormalized = q.textNormalized,
                            isPriority = q.isPriority,
                            isDiscussed = false,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    copied++
                }
            }
            AppResult.Success(CopyQuestionsResult(copied, skipped))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage)
        }
    }
}
