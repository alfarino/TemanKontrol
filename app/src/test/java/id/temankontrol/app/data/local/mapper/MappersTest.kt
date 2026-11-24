package id.temankontrol.app.data.local.mapper

import id.temankontrol.app.data.local.entity.*
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.ConsultationStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MappersTest {

    @Test
    fun consultationEntityToDomain() {
        val now = 1775500000000L
        val entity = ConsultationEntity(
            id = "c-123",
            title = "Kontrol Rutin",
            appointmentAt = now,
            reason = "Cek tensi",
            facilityName = "Klinik Sehat",
            providerName = "dr. Budi",
            status = "SCHEDULED",
            createdAt = now - 1000,
            updatedAt = now
        )

        val domain = entity.toDomain()

        assertEquals("c-123", domain.id.value)
        assertEquals("Kontrol Rutin", domain.title)
        assertEquals(Instant.ofEpochMilli(now), domain.appointmentAt)
        assertEquals("Cek tensi", domain.reason)
        assertEquals("Klinik Sehat", domain.facilityName)
        assertEquals("dr. Budi", domain.providerName)
        assertEquals(ConsultationStatus.SCHEDULED, domain.status)
        assertEquals(Instant.ofEpochMilli(now - 1000), domain.createdAt)
        assertEquals(Instant.ofEpochMilli(now), domain.updatedAt)
    }

    @Test
    fun consultationSummaryTupleToDomain_isPastDueWhenBeforeToday() {
        val todayStart = 1775500000000L
        val pastTuple = ConsultationSummaryTuple(
            id = "c-past",
            title = "Kunjungan Kemarin",
            appointmentAt = todayStart - 1000,
            facilityName = "RSUD",
            status = "SCHEDULED",
            questionCount = 3,
            discussedCount = 1,
            openActionCount = 2
        )

        val pastDomain = pastTuple.toDomain(todayStart)
        assertTrue(pastDomain.isPastDue)
        assertEquals(3, pastDomain.questionCount)
        assertEquals(1, pastDomain.discussedCount)
        assertEquals(2, pastDomain.openActionCount)

        val futureTuple = pastTuple.copy(
            id = "c-future",
            appointmentAt = todayStart + 1000
        )
        val futureDomain = futureTuple.toDomain(todayStart)
        assertFalse(futureDomain.isPastDue)

        val completedPastTuple = pastTuple.copy(
            id = "c-completed",
            status = "COMPLETED"
        )
        val completedDomain = completedPastTuple.toDomain(todayStart)
        assertFalse(completedDomain.isPastDue)
    }

    @Test
    fun questionEntityToDomain() {
        val now = 1775500000000L
        val entity = QuestionEntity(
            id = "q-1",
            consultationId = "c-1",
            text = "Bagaimana cara minum obat?",
            textNormalized = "bagaimana cara minum obat?",
            isPriority = true,
            isDiscussed = false,
            createdAt = now,
            updatedAt = now,
            answer = "Diminum sesudah makan pagi.",
            answerUpdatedAt = now + 500
        )

        val domain = entity.toDomain()
        assertEquals("q-1", domain.id)
        assertEquals("c-1", domain.consultationId.value)
        assertEquals("Bagaimana cara minum obat?", domain.text)
        assertTrue(domain.isPriority)
        assertFalse(domain.isDiscussed)
        assertEquals("Diminum sesudah makan pagi.", domain.answer)
        assertEquals(Instant.ofEpochMilli(now + 500), domain.answerUpdatedAt)
    }

    @Test
    fun attachmentEntityToDomain() {
        val now = 1775500000000L
        val entity = AttachmentEntity(
            id = "att-1",
            consultationId = "c-1",
            questionId = "q-1",
            target = "QUESTION",
            kind = "VOICE",
            fileName = "penjelasan.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 128000L,
            durationMs = 32000L,
            caption = "Rekaman dokter",
            uriString = null,
            createdAt = now
        )

        val domain = entity.toDomain()
        assertEquals("att-1", domain.id)
        assertEquals("c-1", domain.consultationId.value)
        assertEquals("q-1", domain.questionId)
        assertEquals(AttachmentTarget.QUESTION, domain.target)
        assertEquals(AttachmentKind.VOICE, domain.kind)
        assertEquals(32000L, domain.durationMs)
        assertEquals("Rekaman dokter", domain.caption)
    }

    @Test
    fun consultationNoteEntityToDomain() {
        val now = 1775500000000L
        val entity = ConsultationNoteEntity(
            consultationId = "c-1",
            summary = "Kondisi stabil",
            providerInstruction = "Minum obat teratur",
            additionalNotes = "Kontrol 1 bulan lagi",
            updatedAt = now
        )

        val domain = entity.toDomain()
        assertEquals("c-1", domain.consultationId.value)
        assertEquals("Kondisi stabil", domain.summary)
        assertEquals("Minum obat teratur", domain.providerInstruction)
        assertEquals("Kontrol 1 bulan lagi", domain.additionalNotes)
    }

    @Test
    fun followUpActionEntityToDomain() {
        val now = 1775500000000L
        val epochDay = 20500L
        val entity = FollowUpActionEntity(
            id = "f-1",
            consultationId = "c-1",
            text = "Beli obat",
            dueDate = epochDay,
            isCompleted = true,
            completedAt = now,
            createdAt = now - 5000,
            updatedAt = now
        )

        val domain = entity.toDomain()
        assertEquals("f-1", domain.id)
        assertEquals("c-1", domain.consultationId.value)
        assertEquals("Beli obat", domain.text)
        assertEquals(LocalDate.ofEpochDay(epochDay), domain.dueDate)
        assertTrue(domain.isCompleted)
        assertEquals(Instant.ofEpochMilli(now), domain.completedAt)
    }

    @Test
    fun openFollowUpTupleToDomain() {
        val actionEntity = FollowUpActionEntity(
            id = "f-1",
            consultationId = "c-1",
            text = "Cek tensi",
            dueDate = null,
            isCompleted = false,
            completedAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val tuple = OpenFollowUpTuple(
            action = actionEntity,
            consultationTitle = "Kontrol Hipertensi"
        )

        val domain = tuple.toDomain()
        assertEquals("f-1", domain.action.id)
        assertEquals("Kontrol Hipertensi", domain.consultationTitle)
    }
}