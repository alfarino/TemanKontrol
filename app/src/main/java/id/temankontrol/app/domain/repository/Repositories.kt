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
    suspend fun setDiscussed(questionId: String, discussed: Boolean): AppResult<Unit>
    suspend fun delete(questionId: String): AppResult<Unit>
    suspend fun priorityCount(id: ConsultationId, excludeQuestionId: String? = null): AppResult<Int>
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
    suspend fun setNoticeAccepted(): AppResult<Unit>
}

interface SuggestionCatalog {
    fun byCategory(category: SuggestionCategory): List<QuestionSuggestion>
}
