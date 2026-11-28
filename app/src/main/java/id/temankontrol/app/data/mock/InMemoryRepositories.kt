package id.temankontrol.app.data.mock

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.*
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryConsultationRepository @Inject constructor(
    private val dataStore: InMemoryDataStore,
    private val scenarioManager: MockScenarioManager
) : ConsultationRepository {

    private suspend fun checkScenario() {
        if (scenarioManager.currentScenario.value == MockScenario.SLOW) {
            delay(1500)
        }
    }

    override fun observeUpcoming(limit: Int): Flow<List<ConsultationSummary>> {
        return combine(
            dataStore.consultations,
            dataStore.questions,
            dataStore.followUps
        ) { consultations, questions, followUps ->
            val startOfToday = dataStore.startOfToday()
            consultations
                .filter { it.status == ConsultationStatus.SCHEDULED && it.appointmentAt >= startOfToday }
                .sortedBy { it.appointmentAt }
                .take(limit)
                .map { c ->
                    val consultQuestions = questions.filter { it.consultationId == c.id }
                    val consultActions = followUps.filter { it.consultationId == c.id }
                    ConsultationSummary(
                        id = c.id,
                        title = c.title,
                        appointmentAt = c.appointmentAt,
                        facilityName = c.facilityName,
                        status = c.status,
                        questionCount = consultQuestions.size,
                        discussedCount = consultQuestions.count { it.isDiscussed },
                        openActionCount = consultActions.count { !it.isCompleted },
                        isPastDue = false
                    )
                }
        }
    }

    override fun observePastDueCount(): Flow<Int> {
        return dataStore.consultations.map { consultations ->
            val startOfToday = dataStore.startOfToday()
            consultations.count { it.status == ConsultationStatus.SCHEDULED && it.appointmentAt < startOfToday }
        }
    }

    override fun observeHistory(filter: StatusFilter): Flow<List<ConsultationSummary>> {
        return combine(
            dataStore.consultations,
            dataStore.questions,
            dataStore.followUps
        ) { consultations, questions, followUps ->
            val startOfToday = dataStore.startOfToday()
            consultations
                .filter { c ->
                    when (filter) {
                        StatusFilter.ALL -> true
                        StatusFilter.SCHEDULED -> c.status == ConsultationStatus.SCHEDULED
                        StatusFilter.COMPLETED -> c.status == ConsultationStatus.COMPLETED
                        StatusFilter.CANCELLED -> c.status == ConsultationStatus.CANCELLED
                    }
                }
                .sortedByDescending { it.appointmentAt }
                .map { c ->
                    val consultQuestions = questions.filter { it.consultationId == c.id }
                    val consultActions = followUps.filter { it.consultationId == c.id }
                    ConsultationSummary(
                        id = c.id,
                        title = c.title,
                        appointmentAt = c.appointmentAt,
                        facilityName = c.facilityName,
                        status = c.status,
                        questionCount = consultQuestions.size,
                        discussedCount = consultQuestions.count { it.isDiscussed },
                        openActionCount = consultActions.count { !it.isCompleted },
                        isPastDue = c.status == ConsultationStatus.SCHEDULED && c.appointmentAt < startOfToday
                    )
                }
        }
    }

    override fun observe(id: ConsultationId): Flow<Consultation?> {
        return dataStore.consultations.map { list ->
            list.find { it.id == id }
        }
    }

    override suspend fun create(input: ConsultationInput): AppResult<ConsultationId> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val validationResult = Validation.validateConsultationInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val newId = ConsultationId("c-" + UUID.randomUUID().toString().take(8))
        val now = Instant.now()
        val item = Consultation(
            id = newId,
            title = validated.title,
            appointmentAt = validated.appointmentAt,
            reason = validated.reason,
            facilityName = validated.facilityName,
            providerName = validated.providerName,
            status = ConsultationStatus.SCHEDULED,
            createdAt = now,
            updatedAt = now
        )

        dataStore.setConsultations(dataStore.consultations.value + item)
        return AppResult.Success(newId)
    }

    override suspend fun update(id: ConsultationId, input: ConsultationInput): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val validationResult = Validation.validateConsultationInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val list = dataStore.consultations.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        val existing = list[index]
        list[index] = existing.copy(
            title = validated.title,
            appointmentAt = validated.appointmentAt,
            reason = validated.reason,
            facilityName = validated.facilityName,
            providerName = validated.providerName,
            updatedAt = Instant.now()
        )

        dataStore.setConsultations(list)
        return AppResult.Success(Unit)
    }

    override suspend fun setStatus(id: ConsultationId, status: ConsultationStatus): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.consultations.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list[index] = list[index].copy(
            status = status,
            updatedAt = Instant.now()
        )

        dataStore.setConsultations(list)
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: ConsultationId): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.consultations.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list.removeAt(index)
        dataStore.setConsultations(list)

        // Cascade delete: questions, note, follow-up actions, attachments
        dataStore.setQuestions(dataStore.questions.value.filter { it.consultationId != id })
        val updatedNotes = dataStore.notes.value.toMutableMap()
        updatedNotes.remove(id)
        dataStore.setNotes(updatedNotes)
        dataStore.setFollowUps(dataStore.followUps.value.filter { it.consultationId != id })
        dataStore.setAttachments(dataStore.attachments.value.filter { it.consultationId != id })

        return AppResult.Success(Unit)
    }

    override suspend fun childCounts(id: ConsultationId): AppResult<ChildCounts> {
        val qCount = dataStore.questions.value.count { it.consultationId == id }
        val hasNote = dataStore.notes.value.containsKey(id)
        val aCount = dataStore.followUps.value.count { it.consultationId == id }
        return AppResult.Success(ChildCounts(qCount, hasNote, aCount))
    }
}

@Singleton
class InMemoryQuestionRepository @Inject constructor(
    private val dataStore: InMemoryDataStore,
    private val scenarioManager: MockScenarioManager
) : QuestionRepository {

    private suspend fun checkScenario() {
        if (scenarioManager.currentScenario.value == MockScenario.SLOW) {
            delay(1500)
        }
    }

    override fun observeByConsultation(id: ConsultationId): Flow<List<Question>> {
        return dataStore.questions.map { list ->
            list.filter { it.consultationId == id }
                .sortedWith(
                    compareByDescending<Question> { it.isPriority }
                        .thenBy { it.createdAt }
                )
        }
    }

    override suspend fun get(questionId: String): AppResult<Question> {
        checkScenario()
        val question = dataStore.questions.value.find { it.id == questionId }
            ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(question)
    }

    override suspend fun create(id: ConsultationId, input: QuestionInput): AppResult<String> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val textValidation = Validation.validateQuestionText(input.text)
        if (textValidation is AppResult.Failure) return textValidation
        val validatedText = (textValidation as AppResult.Success).data

        val existingQuestions = dataStore.questions.value.filter { it.consultationId == id }

        // Duplicate check
        val normalizedExisting = existingQuestions.map { Validation.normalizeQuestionForDuplicateCheck(it.text) }.toSet()
        val duplicateCheck = Validation.validateNotDuplicateQuestion(validatedText, normalizedExisting)
        if (duplicateCheck is AppResult.Failure) return duplicateCheck

        // Priority limit check (<= 3)
        val currentPriorityCount = existingQuestions.count { it.isPriority }
        val priorityCheck = Validation.validatePriorityLimit(currentPriorityCount, input.isPriority)
        if (priorityCheck is AppResult.Failure) return priorityCheck

        val answerValidation = Validation.validateAnswerText(input.answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val qId = "q-" + UUID.randomUUID().toString().take(8)
        val now = Instant.now()
        val item = Question(
            id = qId,
            consultationId = id,
            text = validatedText,
            isPriority = input.isPriority,
            isDiscussed = false,
            createdAt = now,
            updatedAt = now,
            answer = validatedAnswer,
            answerUpdatedAt = if (validatedAnswer != null) now else null
        )

        dataStore.setQuestions(dataStore.questions.value + item)
        return AppResult.Success(qId)
    }

    override suspend fun update(questionId: String, input: QuestionInput): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val textValidation = Validation.validateQuestionText(input.text)
        if (textValidation is AppResult.Failure) return textValidation
        val validatedText = (textValidation as AppResult.Success).data

        val list = dataStore.questions.value.toMutableList()
        val index = list.indexOfFirst { it.id == questionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        val existing = list[index]
        val otherQuestions = list.filter { it.consultationId == existing.consultationId && it.id != questionId }

        val normalizedOthers = otherQuestions.map { Validation.normalizeQuestionForDuplicateCheck(it.text) }.toSet()
        val duplicateCheck = Validation.validateNotDuplicateQuestion(validatedText, normalizedOthers)
        if (duplicateCheck is AppResult.Failure) return duplicateCheck

        val currentPriorityCount = otherQuestions.count { it.isPriority }
        val priorityCheck = Validation.validatePriorityLimit(currentPriorityCount, input.isPriority)
        if (priorityCheck is AppResult.Failure) return priorityCheck

        val answerValidation = Validation.validateAnswerText(input.answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val now = Instant.now()
        list[index] = existing.copy(
            text = validatedText,
            isPriority = input.isPriority,
            updatedAt = now,
            answer = validatedAnswer,
            answerUpdatedAt = if (validatedAnswer != existing.answer) {
                if (validatedAnswer != null) now else null
            } else {
                existing.answerUpdatedAt
            }
        )

        dataStore.setQuestions(list)
        return AppResult.Success(Unit)
    }

    override suspend fun updateAnswer(questionId: String, answer: String?): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }
        val answerValidation = Validation.validateAnswerText(answer)
        if (answerValidation is AppResult.Failure) return answerValidation
        val validatedAnswer = (answerValidation as AppResult.Success).data

        val list = dataStore.questions.value.toMutableList()
        val index = list.indexOfFirst { it.id == questionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        val now = Instant.now()
        list[index] = list[index].copy(
            answer = validatedAnswer,
            answerUpdatedAt = if (validatedAnswer != null) now else null,
            updatedAt = now
        )
        dataStore.setQuestions(list)
        return AppResult.Success(Unit)
    }

    override suspend fun setDiscussed(questionId: String, discussed: Boolean): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.questions.value.toMutableList()
        val index = list.indexOfFirst { it.id == questionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list[index] = list[index].copy(
            isDiscussed = discussed,
            updatedAt = Instant.now()
        )

        dataStore.setQuestions(list)
        return AppResult.Success(Unit)
    }

    override suspend fun delete(questionId: String): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.questions.value.toMutableList()
        val index = list.indexOfFirst { it.id == questionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list.removeAt(index)
        dataStore.setQuestions(list)
        // Detach or remove question attachments
        dataStore.setAttachments(
            dataStore.attachments.value.filterNot { it.questionId == questionId }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun priorityCount(id: ConsultationId, excludeQuestionId: String?): AppResult<Int> {
        val count = dataStore.questions.value.count {
            it.consultationId == id && it.isPriority && it.id != excludeQuestionId
        }
        return AppResult.Success(count)
    }

    override suspend fun copyUnansweredQuestions(
        fromConsultationId: ConsultationId,
        toConsultationId: ConsultationId,
        questionIds: Set<String>?
    ): AppResult<CopyQuestionsResult> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }
        val all = dataStore.questions.value
        val sourceQuestions = all.filter { q ->
            q.consultationId == fromConsultationId && !q.isDiscussed && (questionIds == null || q.id in questionIds)
        }
        val targetNormalized = all.filter { it.consultationId == toConsultationId }
            .map { Validation.normalizeQuestionForDuplicateCheck(it.text) }
            .toMutableSet()

        val now = Instant.now()
        val copied = mutableListOf<Question>()
        var skipped = 0
        for (q in sourceQuestions) {
            val norm = Validation.normalizeQuestionForDuplicateCheck(q.text)
            if (norm in targetNormalized) {
                skipped++
            } else {
                targetNormalized.add(norm)
                copied.add(
                    Question(
                        id = "q-" + UUID.randomUUID().toString().take(8),
                        consultationId = toConsultationId,
                        text = q.text,
                        isPriority = q.isPriority,
                        isDiscussed = false,
                        createdAt = now,
                        updatedAt = now,
                        answer = null,
                        answerUpdatedAt = null
                    )
                )
            }
        }
        dataStore.setQuestions(all + copied)
        return AppResult.Success(CopyQuestionsResult(copied.size, skipped))
    }
}

@Singleton
class InMemoryNoteRepository @Inject constructor(
    private val dataStore: InMemoryDataStore,
    private val scenarioManager: MockScenarioManager
) : NoteRepository {

    private suspend fun checkScenario() {
        if (scenarioManager.currentScenario.value == MockScenario.SLOW) {
            delay(1500)
        }
    }

    override fun observe(id: ConsultationId): Flow<ConsultationNote?> {
        return dataStore.notes.map { it[id] }
    }

    override suspend fun upsert(id: ConsultationId, input: NoteInput): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val validationResult = Validation.validateNoteInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val map = dataStore.notes.value.toMutableMap()
        map[id] = ConsultationNote(
            consultationId = id,
            summary = validated.summary,
            providerInstruction = validated.providerInstruction,
            additionalNotes = validated.additionalNotes,
            updatedAt = Instant.now()
        )

        dataStore.setNotes(map)
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: ConsultationId): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val map = dataStore.notes.value.toMutableMap()
        map.remove(id)
        dataStore.setNotes(map)
        return AppResult.Success(Unit)
    }
}

@Singleton
class InMemoryFollowUpRepository @Inject constructor(
    private val dataStore: InMemoryDataStore,
    private val scenarioManager: MockScenarioManager
) : FollowUpRepository {

    private suspend fun checkScenario() {
        if (scenarioManager.currentScenario.value == MockScenario.SLOW) {
            delay(1500)
        }
    }

    override fun observeByConsultation(id: ConsultationId): Flow<List<FollowUpAction>> {
        return dataStore.followUps.map { list ->
            list.filter { it.consultationId == id }
                .sortedWith(
                    compareBy<FollowUpAction> { it.isCompleted }
                        .thenBy(nullsLast()) { it.dueDate }
                        .thenByDescending { it.updatedAt }
                )
        }
    }

    override fun observeOpen(limit: Int): Flow<List<OpenFollowUp>> {
        return combine(
            dataStore.followUps,
            dataStore.consultations
        ) { followUps, consultations ->
            val consultMap = consultations.associateBy { it.id }

            followUps.filter { !it.isCompleted }
                .sortedWith(
                    compareBy<FollowUpAction> { action ->
                        if (Validation.isDueDatePast(action.dueDate)) 0 else 1
                    }
                    .thenBy(nullsLast()) { it.dueDate }
                    .thenBy { it.createdAt }
                )
                .take(limit)
                .map { action ->
                    val consultTitle = consultMap[action.consultationId]?.title ?: "Konsultasi"
                    OpenFollowUp(
                        action = action,
                        consultationTitle = consultTitle
                    )
                }
        }
    }

    override suspend fun get(actionId: String): AppResult<FollowUpAction> {
        checkScenario()
        val action = dataStore.followUps.value.find { it.id == actionId }
            ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(action)
    }

    override suspend fun create(id: ConsultationId, input: FollowUpInput): AppResult<String> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val validationResult = Validation.validateFollowUpInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val actionId = "f-" + UUID.randomUUID().toString().take(8)
        val now = Instant.now()
        val item = FollowUpAction(
            id = actionId,
            consultationId = id,
            text = validated.text,
            dueDate = validated.dueDate,
            isCompleted = false,
            completedAt = null,
            createdAt = now,
            updatedAt = now
        )

        dataStore.setFollowUps(dataStore.followUps.value + item)
        return AppResult.Success(actionId)
    }

    override suspend fun update(actionId: String, input: FollowUpInput): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val validationResult = Validation.validateFollowUpInput(input)
        if (validationResult is AppResult.Failure) return validationResult
        val validated = (validationResult as AppResult.Success).data

        val list = dataStore.followUps.value.toMutableList()
        val index = list.indexOfFirst { it.id == actionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list[index] = list[index].copy(
            text = validated.text,
            dueDate = validated.dueDate,
            updatedAt = Instant.now()
        )

        dataStore.setFollowUps(list)
        return AppResult.Success(Unit)
    }

    override suspend fun setCompleted(actionId: String, completed: Boolean): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.followUps.value.toMutableList()
        val index = list.indexOfFirst { it.id == actionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        val now = Instant.now()
        list[index] = list[index].copy(
            isCompleted = completed,
            completedAt = if (completed) now else null,
            updatedAt = now
        )

        dataStore.setFollowUps(list)
        return AppResult.Success(Unit)
    }

    override suspend fun delete(actionId: String): AppResult<Unit> {
        checkScenario()
        if (scenarioManager.currentScenario.value == MockScenario.ERROR) {
            return AppResult.Failure(AppError.Storage)
        }

        val list = dataStore.followUps.value.toMutableList()
        val index = list.indexOfFirst { it.id == actionId }
        if (index == -1) return AppResult.Failure(AppError.NotFound)

        list.removeAt(index)
        dataStore.setFollowUps(list)
        return AppResult.Success(Unit)
    }
}

@Singleton
class InMemoryPreferencesRepository @Inject constructor(
    private val dataStore: InMemoryDataStore
) : PreferencesRepository {

    private val _reminderSettings = kotlinx.coroutines.flow.MutableStateFlow(ReminderSettings())
    private val _appLockSettings = kotlinx.coroutines.flow.MutableStateFlow(AppLockSettings())
    private val _lastBackupAt = kotlinx.coroutines.flow.MutableStateFlow<Instant?>(null)
    private val _dataChanged = kotlinx.coroutines.flow.MutableStateFlow(true)
    private val _hasUnsyncedCloudChanges = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val _lastDataChangeMillis = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private val _accountSession = kotlinx.coroutines.flow.MutableStateFlow<AccountSession?>(null)
    private val _localDataOwnerUserId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _tombstones = kotlinx.coroutines.flow.MutableStateFlow<List<SyncTombstone>>(emptyList())

    override val noticeAccepted: Flow<Boolean> = dataStore.noticeAccepted
    override val reminderSettings: Flow<ReminderSettings> = _reminderSettings
    override val appLockSettings: Flow<AppLockSettings> = _appLockSettings
    override val lastBackupAt: Flow<Instant?> = _lastBackupAt
    override val dataChangedSinceBackup: Flow<Boolean> = _dataChanged
    override val hasUnsyncedCloudChanges: Flow<Boolean> = _hasUnsyncedCloudChanges
    override val lastDataChangeMillis: Flow<Long> = _lastDataChangeMillis
    override val accountSession: Flow<AccountSession?> = _accountSession
    override val localDataOwnerUserId: Flow<String?> = _localDataOwnerUserId
    override val pendingTombstones: Flow<List<SyncTombstone>> = _tombstones

    override suspend fun setNoticeAccepted(): AppResult<Unit> {
        dataStore.setNoticeAccepted(true)
        return AppResult.Success(Unit)
    }

    override suspend fun updateReminderSettings(
        transform: (ReminderSettings) -> ReminderSettings
    ): AppResult<Unit> {
        _reminderSettings.value = transform(_reminderSettings.value)
        return AppResult.Success(Unit)
    }

    override suspend fun updateAppLockSettings(
        transform: (AppLockSettings) -> AppLockSettings
    ): AppResult<Unit> {
        _appLockSettings.value = transform(_appLockSettings.value)
        return AppResult.Success(Unit)
    }

    override suspend fun recordBackupCreated(at: Instant): AppResult<Unit> {
        _lastBackupAt.value = at
        _dataChanged.value = false
        return AppResult.Success(Unit)
    }

    override suspend fun markDataChanged(): AppResult<Unit> {
        _dataChanged.value = true
        _hasUnsyncedCloudChanges.value = true
        _lastDataChangeMillis.value = System.currentTimeMillis()
        return AppResult.Success(Unit)
    }

    override suspend fun markCloudChangesSynced(snapshotChangeMillis: Long): AppResult<Unit> {
        if (_lastDataChangeMillis.value <= snapshotChangeMillis) {
            _hasUnsyncedCloudChanges.value = false
        }
        return AppResult.Success(Unit)
    }

    override suspend fun saveAccountSession(session: AccountSession?): AppResult<Unit> {
        _accountSession.value = session
        return AppResult.Success(Unit)
    }

    override suspend fun saveLocalDataOwnerUserId(userId: String?): AppResult<Unit> {
        _localDataOwnerUserId.value = userId
        return AppResult.Success(Unit)
    }

    override suspend fun updateLastSyncAt(epochMillis: Long): AppResult<Unit> {
        _accountSession.value = _accountSession.value?.copy(lastSyncAtMillis = epochMillis)
        return AppResult.Success(Unit)
    }

    override suspend fun recordSyncTombstone(
        entityType: SyncEntityType,
        entityId: String,
        deletedAtMillis: Long
    ): AppResult<Unit> {
        _tombstones.value = _tombstones.value.filterNot { it.entityType == entityType && it.entityId == entityId } +
            SyncTombstone(entityType, entityId, deletedAtMillis)
        return AppResult.Success(Unit)
    }

    override suspend fun clearSyncTombstones(entityIds: Set<String>): AppResult<Unit> {
        _tombstones.value = _tombstones.value.filterNot { it.entityId in entityIds }
        return AppResult.Success(Unit)
    }

    override suspend fun clearAllSyncTombstones(): AppResult<Unit> {
        _tombstones.value = emptyList()
        return AppResult.Success(Unit)
    }

    override suspend fun resetAccountScopedPreferences(): AppResult<Unit> {
        _accountSession.value = null
        _localDataOwnerUserId.value = null
        _tombstones.value = emptyList()
        _lastBackupAt.value = null
        _dataChanged.value = true
        _hasUnsyncedCloudChanges.value = false
        _lastDataChangeMillis.value = 0L
        _reminderSettings.value = ReminderSettings()
        return AppResult.Success(Unit)
    }
}

@Singleton
class InMemoryAttachmentRepository @Inject constructor(
    private val dataStore: InMemoryDataStore
) : AttachmentRepository {

    override fun observeByConsultation(id: ConsultationId): Flow<List<Attachment>> {
        return dataStore.attachments.map { list ->
            list.filter { it.consultationId == id }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeAll(): Flow<List<Attachment>> = dataStore.attachments

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
        val captionResult = Validation.validateCaptionText(caption)
        if (captionResult is AppResult.Failure) return captionResult
        val validatedCaption = (captionResult as AppResult.Success).data

        val attId = "att-" + UUID.randomUUID().toString().take(8)
        val item = Attachment(
            id = attId,
            consultationId = consultationId,
            questionId = questionId,
            target = target,
            kind = kind,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            caption = validatedCaption,
            uriString = uriString,
            createdAt = Instant.now()
        )
        dataStore.setAttachments(dataStore.attachments.value + item)
        return AppResult.Success(attId)
    }

    override suspend fun updateCaption(attachmentId: String, caption: String?): AppResult<Unit> {
        val captionResult = Validation.validateCaptionText(caption)
        if (captionResult is AppResult.Failure) return captionResult
        val validatedCaption = (captionResult as AppResult.Success).data

        val list = dataStore.attachments.value.toMutableList()
        val idx = list.indexOfFirst { it.id == attachmentId }
        if (idx == -1) return AppResult.Failure(AppError.NotFound)
        list[idx] = list[idx].copy(caption = validatedCaption)
        dataStore.setAttachments(list)
        return AppResult.Success(Unit)
    }

    override suspend fun delete(attachmentId: String): AppResult<Unit> {
        val list = dataStore.attachments.value.toMutableList()
        val idx = list.indexOfFirst { it.id == attachmentId }
        if (idx == -1) return AppResult.Failure(AppError.NotFound)
        list.removeAt(idx)
        dataStore.setAttachments(list)
        return AppResult.Success(Unit)
    }
}
