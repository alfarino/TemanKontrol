package id.temankontrol.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.temankontrol.app.data.local.dao.*
import id.temankontrol.app.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var consultationDao: ConsultationDao
    private lateinit var questionDao: QuestionDao
    private lateinit var noteDao: NoteDao
    private lateinit var followUpDao: FollowUpDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var reminderLogDao: ReminderLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        consultationDao = db.consultationDao()
        questionDao = db.questionDao()
        noteDao = db.noteDao()
        followUpDao = db.followUpDao()
        attachmentDao = db.attachmentDao()
        reminderLogDao = db.reminderLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveConsultation() = runBlocking {
        val consultation = ConsultationEntity(
            id = "c-test-1",
            title = "Pemeriksaan Gula Darah",
            appointmentAt = 1775500000000L,
            reason = "Cek rutin",
            facilityName = "Puskesmas Sejahtera",
            providerName = "dr. Siti",
            status = "SCHEDULED",
            createdAt = 1775000000000L,
            updatedAt = 1775000000000L
        )
        consultationDao.insert(consultation)

        val retrieved = consultationDao.getById("c-test-1")
        assertNotNull(retrieved)
        assertEquals("Pemeriksaan Gula Darah", retrieved?.title)
        assertEquals("SCHEDULED", retrieved?.status)
    }

    @Test
    fun cascadeDelete_removesQuestionsNotesAndFollowUps() = runBlocking {
        val cId = "c-cascade"
        val now = 1775500000000L

        // Insert consultation
        consultationDao.insert(
            ConsultationEntity(
                id = cId,
                title = "Konsultasi Cascade",
                appointmentAt = now,
                reason = "Alasan kontrol",
                facilityName = null,
                providerName = null,
                status = "SCHEDULED",
                createdAt = now,
                updatedAt = now
            )
        )

        // Insert question
        questionDao.insert(
            QuestionEntity(
                id = "q-cascade-1",
                consultationId = cId,
                text = "Pertanyaan 1",
                textNormalized = "pertanyaan 1",
                isPriority = true,
                isDiscussed = false,
                createdAt = now,
                updatedAt = now
            )
        )

        // Insert note
        noteDao.upsert(
            ConsultationNoteEntity(
                consultationId = cId,
                summary = "Ringkasan cascade",
                providerInstruction = null,
                additionalNotes = null,
                updatedAt = now
            )
        )

        // Insert follow-up
        followUpDao.insert(
            FollowUpActionEntity(
                id = "f-cascade-1",
                consultationId = cId,
                text = "Aksi cascade",
                dueDate = null,
                isCompleted = false,
                completedAt = null,
                createdAt = now,
                updatedAt = now
            )
        )

        // Verify initial presence
        assertEquals(1, consultationDao.getQuestionCount(cId))
        assertTrue(consultationDao.hasNote(cId))
        assertEquals(1, consultationDao.getActionCount(cId))

        // Delete parent consultation
        consultationDao.delete(cId)

        // Verify cascade deletion
        assertNull(consultationDao.getById(cId))
        assertNull(questionDao.getById("q-cascade-1"))
        assertNull(noteDao.getByConsultationId(cId))
        assertNull(followUpDao.getById("f-cascade-1"))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateQuestionNormalizedText_throwsConstraintException(): Unit = runBlocking {
        val cId = "c-unique"
        val now = 1775500000000L

        consultationDao.insert(
            ConsultationEntity(
                id = cId,
                title = "Konsultasi Unik",
                appointmentAt = now,
                reason = "Alasan tes unik",
                facilityName = null,
                providerName = null,
                status = "SCHEDULED",
                createdAt = now,
                updatedAt = now
            )
        )

        questionDao.insert(
            QuestionEntity(
                id = "q-unique-1",
                consultationId = cId,
                text = "Bolehkah makan telur?",
                textNormalized = "bolehkah makan telur?",
                isPriority = false,
                isDiscussed = false,
                createdAt = now,
                updatedAt = now
            )
        )

        // Inserting exact duplicate normalized text in same consultation must fail
        questionDao.insert(
            QuestionEntity(
                id = "q-unique-2",
                consultationId = cId,
                text = "Bolehkah Makan Telur?",
                textNormalized = "bolehkah makan telur?",
                isPriority = false,
                isDiscussed = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @Test
    fun noteUpsert_updatesExistingRecord() = runBlocking {
        val cId = "c-note"
        val now = 1775500000000L

        consultationDao.insert(
            ConsultationEntity(
                id = cId,
                title = "Konsultasi Note",
                appointmentAt = now,
                reason = "Alasan tes note",
                facilityName = null,
                providerName = null,
                status = "SCHEDULED",
                createdAt = now,
                updatedAt = now
            )
        )

        noteDao.upsert(
            ConsultationNoteEntity(
                consultationId = cId,
                summary = "Versi pertama",
                providerInstruction = null,
                additionalNotes = null,
                updatedAt = now
            )
        )

        var note = noteDao.getByConsultationId(cId)
        assertEquals("Versi pertama", note?.summary)

        // Upsert updated version
        noteDao.upsert(
            ConsultationNoteEntity(
                consultationId = cId,
                summary = "Versi diperbarui",
                providerInstruction = "Instruksi dokter",
                additionalNotes = "Catatan tambahan",
                updatedAt = now + 1000
            )
        )

        note = noteDao.getByConsultationId(cId)
        assertEquals("Versi diperbarui", note?.summary)
        assertEquals("Instruksi dokter", note?.providerInstruction)
    }

    @Test
    fun followUpSetCompleted_updatesStatusAndCompletedAt() = runBlocking {
        val cId = "c-followup"
        val now = 1775500000000L

        consultationDao.insert(
            ConsultationEntity(
                id = cId,
                title = "Konsultasi FollowUp",
                appointmentAt = now,
                reason = "Alasan tes followup",
                facilityName = null,
                providerName = null,
                status = "SCHEDULED",
                createdAt = now,
                updatedAt = now
            )
        )

        val fId = "f-test-1"
        followUpDao.insert(
            FollowUpActionEntity(
                id = fId,
                consultationId = cId,
                text = "Tindak lanjut penting",
                dueDate = 20500L,
                isCompleted = false,
                completedAt = null,
                createdAt = now,
                updatedAt = now
            )
        )

        val initial = followUpDao.getById(fId)
        assertNotNull(initial)
        assertFalse(initial!!.isCompleted)

        val completedTime = now + 5000
        followUpDao.setCompleted(fId, true, completedTime, completedTime)

        val updated = followUpDao.getById(fId)
        assertNotNull(updated)
        assertTrue(updated!!.isCompleted)
        assertEquals(completedTime, updated.completedAt)
    }

    @Test
    fun attachmentAndQuestionAnswer_cascadeDeletesWithQuestionAndConsultation() = runBlocking {
        val cId = "c-att-room"
        val now = 1775500000000L
        consultationDao.insert(
            ConsultationEntity(
                id = cId,
                title = "Tes Lampiran",
                appointmentAt = now,
                reason = "Alasan tes lampiran",
                facilityName = null,
                providerName = null,
                status = "SCHEDULED",
                createdAt = now,
                updatedAt = now
            )
        )
        questionDao.insert(
            QuestionEntity(
                id = "q-att-1",
                consultationId = cId,
                text = "Bagaimana cara minum obat?",
                textNormalized = "bagaimana cara minum obat?",
                isPriority = true,
                isDiscussed = false,
                createdAt = now,
                updatedAt = now
            )
        )
        questionDao.updateAnswer("q-att-1", "Minum 1 kali sehari sesudah makan.", now, now)
        assertEquals("Minum 1 kali sehari sesudah makan.", questionDao.getById("q-att-1")?.answer)

        attachmentDao.insert(
            AttachmentEntity(
                id = "att-q",
                consultationId = cId,
                questionId = "q-att-1",
                target = "QUESTION",
                kind = "PHOTO",
                fileName = "foto.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 12345L,
                durationMs = null,
                caption = "Foto pertanyaan",
                uriString = null,
                createdAt = now
            )
        )
        attachmentDao.insert(
            AttachmentEntity(
                id = "att-note",
                consultationId = cId,
                questionId = null,
                target = "ADDITIONAL_NOTE",
                kind = "VOICE",
                fileName = "vn.m4a",
                mimeType = "audio/mp4",
                sizeBytes = 24000L,
                durationMs = 6000L,
                caption = "VN tambahan",
                uriString = null,
                createdAt = now
            )
        )

        // Delete question -> question attachment removed, additional note attachment stays
        questionDao.delete("q-att-1")
        assertNull(attachmentDao.getById("att-q"))
        assertNotNull(attachmentDao.getById("att-note"))

        // Delete consultation -> additional note attachment cascades
        consultationDao.delete(cId)
        assertNull(attachmentDao.getById("att-note"))
    }

    @Test
    fun reminderLog_deduplicatesByKey() = runBlocking {
        assertFalse(reminderLogDao.exists("VISIT:c-1:1000"))
        reminderLogDao.insert(ReminderLogEntity(key = "VISIT:c-1:1000", firedAt = 1000L))
        assertTrue(reminderLogDao.exists("VISIT:c-1:1000"))
        reminderLogDao.deleteOlderThan(2000L)
        assertFalse(reminderLogDao.exists("VISIT:c-1:1000"))
    }
}