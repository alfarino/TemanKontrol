package id.temankontrol.app.data.local

import id.temankontrol.app.data.local.dao.AttachmentDao
import id.temankontrol.app.data.local.dao.ConsultationDao
import id.temankontrol.app.data.local.dao.FollowUpDao
import id.temankontrol.app.data.local.dao.NoteDao
import id.temankontrol.app.data.local.dao.QuestionDao
import id.temankontrol.app.data.local.entity.AttachmentEntity
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
    private val followUpDao: FollowUpDao,
    private val attachmentDao: AttachmentDao
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
        val attachments = SampleData.getSampleAttachments()

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
                    updatedAt = q.updatedAt.toEpochMilli(),
                    answer = q.answer,
                    answerUpdatedAt = q.answerUpdatedAt?.toEpochMilli()
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

        attachments.forEach { a ->
            attachmentDao.insert(
                AttachmentEntity(
                    id = a.id,
                    consultationId = a.consultationId.value,
                    questionId = a.questionId,
                    target = a.target.name,
                    kind = a.kind.name,
                    fileName = a.fileName,
                    mimeType = a.mimeType,
                    sizeBytes = a.sizeBytes,
                    durationMs = a.durationMs,
                    caption = a.caption,
                    uriString = a.uriString,
                    createdAt = a.createdAt.toEpochMilli()
                )
            )
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }

    /**
     * Dataset uji D-100 (PRD §21.4): 100 kunjungan × 8 pertanyaan × 4 tindak lanjut.
     * Dipakai untuk pengukuran performa scroll, cold start, dan query (debug-only).
     */
    suspend fun seedLargeD100() = withContext(Dispatchers.IO) {
        clearAllData()
        val now = java.time.Instant.now()
        val today = java.time.LocalDate.now()
        for (i in 1..100) {
            val cId = "c-d100-$i"
            val apptAt = now.plus((i - 30).toLong(), java.time.temporal.ChronoUnit.DAYS)
            val status = when {
                i % 3 == 0 -> "COMPLETED"
                i % 7 == 0 -> "CANCELLED"
                else -> "SCHEDULED"
            }
            consultationDao.insert(
                ConsultationEntity(
                    id = cId,
                    title = "Kunjungan Uji Performa #$i — Kontrol Rutin",
                    appointmentAt = apptAt.toEpochMilli(),
                    reason = "Evaluasi gejala, pemeriksaan berkala, dan pemantauan tindak lanjut ke-$i.",
                    facilityName = "RS Uji Performa $i",
                    providerName = "dr. Simulasi $i",
                    status = status,
                    createdAt = now.toEpochMilli(),
                    updatedAt = now.toEpochMilli()
                )
            )
            for (qIdx in 1..8) {
                val qText = "Bagaimana perkembangan keluhan ke-$qIdx pada kunjungan $i?"
                questionDao.insert(
                    QuestionEntity(
                        id = "q-d100-$i-$qIdx",
                        consultationId = cId,
                        text = qText,
                        textNormalized = Validation.normalizeQuestionForDuplicateCheck(qText),
                        isPriority = qIdx <= 3,
                        isDiscussed = status == "COMPLETED",
                        createdAt = now.toEpochMilli() + qIdx,
                        updatedAt = now.toEpochMilli() + qIdx,
                        answer = if (status == "COMPLETED") "Penjelasan tercatat untuk item $qIdx." else null,
                        answerUpdatedAt = if (status == "COMPLETED") now.toEpochMilli() else null
                    )
                )
            }
            for (fIdx in 1..4) {
                followUpDao.insert(
                    FollowUpActionEntity(
                        id = "f-d100-$i-$fIdx",
                        consultationId = cId,
                        text = "Tindak lanjut uji ke-$fIdx untuk kunjungan $i",
                        dueDate = today.plusDays((fIdx - 2).toLong()).toEpochDay(),
                        isCompleted = status == "COMPLETED" && fIdx <= 2,
                        completedAt = if (status == "COMPLETED" && fIdx <= 2) now.toEpochMilli() else null,
                        createdAt = now.toEpochMilli() + fIdx,
                        updatedAt = now.toEpochMilli() + fIdx
                    )
                )
            }
        }
    }
}
