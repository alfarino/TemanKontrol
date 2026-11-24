package id.temankontrol.app.domain.repository

import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ConsultationRepository {
    fun observeUpcoming(limit: Int = 3): Flow<List<ConsultationSummary>>
    fun observePastDueCount(): Flow<Int>
    fun observeHistory(filter: StatusFilter): Flow<List<ConsultationSummary>>
    fun observe(id: ConsultationId): Flow<Consultation?>
    suspend fun create(input: ConsultationInput): AppResult<ConsultationId>
    suspend fun update(id: ConsultationId, input: ConsultationInput): AppResult<Unit>
    suspend fun setStatus(id: ConsultationId, status: ConsultationStatus): AppResult<Unit>
    suspend fun delete(id: ConsultationId): AppResult<Unit>
    suspend fun childCounts(id: ConsultationId): AppResult<ChildCounts>
}

interface QuestionRepository {
    fun observeByConsultation(id: ConsultationId): Flow<List<Question>>
    suspend fun get(questionId: String): AppResult<Question>
    suspend fun create(id: ConsultationId, input: QuestionInput): AppResult<String>
    suspend fun update(questionId: String, input: QuestionInput): AppResult<Unit>
    suspend fun updateAnswer(questionId: String, answer: String?): AppResult<Unit>
    suspend fun setDiscussed(questionId: String, discussed: Boolean): AppResult<Unit>
    suspend fun delete(questionId: String): AppResult<Unit>
    suspend fun priorityCount(id: ConsultationId, excludeQuestionId: String? = null): AppResult<Int>
    suspend fun copyUnansweredQuestions(
        fromConsultationId: ConsultationId,
        toConsultationId: ConsultationId,
        questionIds: Set<String>? = null
    ): AppResult<CopyQuestionsResult>
}

data class CopyQuestionsResult(
    val copiedCount: Int,
    val skippedDuplicateCount: Int
)

interface AttachmentRepository {
    fun observeByConsultation(id: ConsultationId): Flow<List<Attachment>>
    fun observeAll(): Flow<List<Attachment>>
    suspend fun addAttachment(
        consultationId: ConsultationId,
        questionId: String?,
        target: AttachmentTarget,
        kind: AttachmentKind,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        durationMs: Long? = null,
        caption: String? = null,
        uriString: String? = null
    ): AppResult<String>
    suspend fun updateCaption(attachmentId: String, caption: String?): AppResult<Unit>
    suspend fun delete(attachmentId: String): AppResult<Unit>
}

interface NoteRepository {
    fun observe(id: ConsultationId): Flow<ConsultationNote?>
    suspend fun upsert(id: ConsultationId, input: NoteInput): AppResult<Unit>
    suspend fun delete(id: ConsultationId): AppResult<Unit>
}

interface FollowUpRepository {
    fun observeByConsultation(id: ConsultationId): Flow<List<FollowUpAction>>
    fun observeOpen(limit: Int = 5): Flow<List<OpenFollowUp>>
    suspend fun get(actionId: String): AppResult<FollowUpAction>
    suspend fun create(id: ConsultationId, input: FollowUpInput): AppResult<String>
    suspend fun update(actionId: String, input: FollowUpInput): AppResult<Unit>
    suspend fun setCompleted(actionId: String, completed: Boolean): AppResult<Unit>
    suspend fun delete(actionId: String): AppResult<Unit>
}

interface PreferencesRepository {
    val noticeAccepted: Flow<Boolean>
    val reminderSettings: Flow<ReminderSettings>
    val appLockSettings: Flow<AppLockSettings>
    val lastBackupAt: Flow<java.time.Instant?>
    val dataChangedSinceBackup: Flow<Boolean>

    suspend fun setNoticeAccepted(): AppResult<Unit>
    suspend fun updateReminderSettings(transform: (ReminderSettings) -> ReminderSettings): AppResult<Unit>
    suspend fun updateAppLockSettings(transform: (AppLockSettings) -> AppLockSettings): AppResult<Unit>
    suspend fun recordBackupCreated(at: java.time.Instant): AppResult<Unit>
    suspend fun markDataChanged(): AppResult<Unit>
}

interface SuggestionCatalog {
    fun byCategory(category: SuggestionCategory): List<QuestionSuggestion>
}

data class BackupInspectResult(
    val isEncrypted: Boolean,
    val formatVersion: Int,
    val appVersion: String,
    val createdAtMillis: Long,
    val consultationCount: Int,
    val questionCount: Int,
    val attachmentCount: Int,
    val requiresPasswordPrompt: Boolean
)

data class BackupExportResult(
    val fileName: String,
    val byteCount: Long,
    val isLocalDeviceStorage: Boolean
)

interface BackupRepository {
    suspend fun exportToUri(uriString: String, password: String?): AppResult<BackupExportResult>
    suspend fun inspectFromUri(uriString: String, password: String?): AppResult<BackupInspectResult>
    suspend fun restoreFromUri(uriString: String, password: String?): AppResult<Unit>
}
