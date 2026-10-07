package id.temankontrol.app.data.local

import id.temankontrol.app.data.local.dao.ConsultationDao
import id.temankontrol.app.data.local.dao.FollowUpDao
import id.temankontrol.app.data.local.dao.NoteDao
import id.temankontrol.app.data.local.dao.QuestionDao
import id.temankontrol.app.data.local.entity.ConsultationEntity
import id.temankontrol.app.data.local.entity.ConsultationNoteEntity
import id.temankontrol.app.data.local.entity.FollowUpActionEntity
import id.temankontrol.app.data.local.entity.QuestionEntity
import id.temankontrol.app.data.mock.SampleData
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: AppDatabase,
    private val consultationDao: ConsultationDao,
    private val questionDao: QuestionDao,
    private val noteDao: NoteDao,
    private val followUpDao: FollowUpDao
) {
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        if (consultationDao.countAll() == 0) {
            seedSampleData()
        }
    }

    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        clearAllData()
        val consultations = SampleData.getSampleConsultations()
        val questions = SampleData.getSampleQuestions()
        val notes = SampleData.getSampleNotes()
        val followUps = SampleData.getSampleFollowUps()

        consultations.forEach { c ->
            consultationDao.insert(
                ConsultationEntity(
                    id = c.id.value,
                    title = c.title,
                    appointmentAt = c.appointmentAt.toEpochMilli(),
                    reason = c.reason,
                    facilityName = c.facilityName,
                    providerName = c.providerName,
                    status = c.status.name,
                    createdAt = c.createdAt.toEpochMilli(),
                    updatedAt = c.updatedAt.toEpochMilli()
                )
            )
        }

        questions.forEach { q ->
            questionDao.insert(
                QuestionEntity(
                    id = q.id,
                    consultationId = q.consultationId.value,
                    text = q.text,
                    textNormalized = Validation.normalizeQuestionForDuplicateCheck(q.text),
                    isPriority = q.isPriority,
                    isDiscussed = q.isDiscussed,
                    createdAt = q.createdAt.toEpochMilli(),
                    updatedAt = q.updatedAt.toEpochMilli()
                )
            )
        }

        notes.forEach { n ->
            noteDao.upsert(
                ConsultationNoteEntity(
                    consultationId = n.consultationId.value,
                    summary = n.summary,
                    providerInstruction = n.providerInstruction,
                    additionalNotes = n.additionalNotes,
                    updatedAt = n.updatedAt.toEpochMilli()
                )
            )
        }

        followUps.forEach { f ->
            followUpDao.insert(
                FollowUpActionEntity(
                    id = f.id,
                    consultationId = f.consultationId.value,
                    text = f.text,
                    dueDate = f.dueDate?.toEpochDay(),
                    isCompleted = f.isCompleted,
                    completedAt = f.completedAt?.toEpochMilli(),
                    createdAt = f.createdAt.toEpochMilli(),
                    updatedAt = f.updatedAt.toEpochMilli()
                )
            )
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
