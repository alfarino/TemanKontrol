package id.temankontrol.app.data.mock

import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.QuestionInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AttachmentAndAnswerTest {

    private lateinit var scenarioManager: MockScenarioManager
    private lateinit var dataStore: InMemoryDataStore
    private lateinit var questionRepo: InMemoryQuestionRepository
    private lateinit var attachmentRepo: InMemoryAttachmentRepository

    @Before
    fun setUp() {
        scenarioManager = MockScenarioManager()
        dataStore = InMemoryDataStore(scenarioManager)
        questionRepo = InMemoryQuestionRepository(dataStore, scenarioManager)
        attachmentRepo = InMemoryAttachmentRepository(dataStore)
    }

    @Test
    fun updateAnswer_persistsAndClearsProperly() = runBlocking {
        val cId = ConsultationId("c-ans-test")
        val createRes = questionRepo.create(cId, QuestionInput("Bagaimana hasil pemeriksaan saya?"))
        assertTrue(createRes.isSuccess())
        val qId = createRes.getOrNull()!!

        val updRes = questionRepo.updateAnswer(qId, "  Hasil normal, kontrol ulang 3 bulan.  ")
        assertTrue(updRes.isSuccess())

        val fetched = questionRepo.get(qId).getOrNull()
        assertEquals("Hasil normal, kontrol ulang 3 bulan.", fetched?.answer)
        assertNotNull(fetched?.answerUpdatedAt)
    }

    @Test
    fun copyUnansweredQuestions_copiesOnlyUndiscussedAndSkipsDuplicates() = runBlocking {
        val fromId = ConsultationId("c-from")
        val toId = ConsultationId("c-to")

        val q1 = questionRepo.create(fromId, QuestionInput("Mengapa tekanan darah naik?", isPriority = true)).getOrNull()!!
        val q2 = questionRepo.create(fromId, QuestionInput("Bagaimana efek samping obat ini?")).getOrNull()!!
        questionRepo.setDiscussed(q2, true)

        // Pre-populate duplicate in target
        questionRepo.create(toId, QuestionInput("  mengapa tekanan darah naik? "))
        val q3 = questionRepo.create(fromId, QuestionInput("Apa langkah selanjutnya?")).getOrNull()!!

        val copyRes = questionRepo.copyUnansweredQuestions(fromId, toId)
        assertTrue(copyRes.isSuccess())
        val summary = copyRes.getOrNull()!!
        assertEquals(1, summary.copiedCount)
        assertEquals(1, summary.skippedDuplicateCount)

        val targetList = questionRepo.observeByConsultation(toId).first()
        assertEquals(2, targetList.size)
        assertTrue(targetList.any { it.text == "Apa langkah selanjutnya?" && !it.isDiscussed })
        assertNotNull(q1)
        assertNotNull(q3)
    }

    @Test
    fun attachments_attachToQuestionAndAdditionalNoteAndDelete() = runBlocking {
        val cId = ConsultationId("c-att-test")
        val photoRes = attachmentRepo.addAttachment(
            consultationId = cId,
            questionId = "q-10",
            target = AttachmentTarget.QUESTION,
            kind = AttachmentKind.PHOTO,
            fileName = "foto.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 100_000L,
            caption = "Hasil foto"
        )
        val voiceRes = attachmentRepo.addAttachment(
            consultationId = cId,
            questionId = null,
            target = AttachmentTarget.ADDITIONAL_NOTE,
            kind = AttachmentKind.VOICE,
            fileName = "vn.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 64_000L,
            durationMs = 16_000L,
            caption = "Penjelasan tambahan"
        )
        assertTrue(photoRes.isSuccess())
        assertTrue(voiceRes.isSuccess())

        val list = attachmentRepo.observeByConsultation(cId).first()
        assertEquals(2, list.size)
        assertEquals(1, list.count { it.target == AttachmentTarget.QUESTION && it.questionId == "q-10" })
        assertEquals(1, list.count { it.target == AttachmentTarget.ADDITIONAL_NOTE && it.questionId == null })

        attachmentRepo.delete(photoRes.getOrNull()!!)
        val afterDelete = attachmentRepo.observeByConsultation(cId).first()
        assertEquals(1, afterDelete.size)
    }
}
