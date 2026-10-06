package id.temankontrol.app.data.mock

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationInput
import id.temankontrol.app.domain.model.FollowUpInput
import id.temankontrol.app.domain.model.QuestionInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class InMemoryRepositoriesTest {

    private lateinit var scenarioManager: MockScenarioManager
    private lateinit var dataStore: InMemoryDataStore
    private lateinit var consultationRepo: InMemoryConsultationRepository
    private lateinit var questionRepo: InMemoryQuestionRepository
    private lateinit var noteRepo: InMemoryNoteRepository
    private lateinit var followUpRepo: InMemoryFollowUpRepository

    @Before
    fun setUp() {
        scenarioManager = MockScenarioManager()
        dataStore = InMemoryDataStore(scenarioManager)
        consultationRepo = InMemoryConsultationRepository(dataStore, scenarioManager)
        questionRepo = InMemoryQuestionRepository(dataStore, scenarioManager)
        noteRepo = InMemoryNoteRepository(dataStore, scenarioManager)
        followUpRepo = InMemoryFollowUpRepository(dataStore, scenarioManager)
    }

    @Test
    fun testPriorityLimitExceeded() = runBlocking {
        val consultId = ConsultationId("c-test-p")
        // Create 3 priority questions
        val q1 = questionRepo.create(consultId, QuestionInput("Pertanyaan 1", isPriority = true))
        assertTrue(q1.isSuccess())
        val q2 = questionRepo.create(consultId, QuestionInput("Pertanyaan 2", isPriority = true))
        assertTrue(q2.isSuccess())
        val q3 = questionRepo.create(consultId, QuestionInput("Pertanyaan 3", isPriority = true))
        assertTrue(q3.isSuccess())

        // 4th priority question must fail
        val q4 = questionRepo.create(consultId, QuestionInput("Pertanyaan 4", isPriority = true))
        assertTrue(q4.isFailure())
        assertEquals(AppError.PriorityLimitExceeded, (q4 as id.temankontrol.app.core.common.AppResult.Failure).error)

        // 4th non-priority question must succeed
        val q5 = questionRepo.create(consultId, QuestionInput("Pertanyaan 5", isPriority = false))
        assertTrue(q5.isSuccess())
    }

    @Test
    fun testDuplicateQuestionRejected() = runBlocking {
        val consultId = ConsultationId("c-test-dup")
        val res1 = questionRepo.create(consultId, QuestionInput("Apa efek samping obat?", isPriority = false))
        assertTrue(res1.isSuccess())

        // Duplicate with same text (case-insensitive & trimmed)
        val res2 = questionRepo.create(consultId, QuestionInput("  apa efek samping obat?  ", isPriority = false))
        assertTrue(res2.isFailure())
        assertEquals(AppError.Duplicate, (res2 as id.temankontrol.app.core.common.AppResult.Failure).error)
    }

    @Test
    fun testCascadeDelete() = runBlocking {
        // Create consultation
        val createRes = consultationRepo.create(
            ConsultationInput(
                title = "Konsultasi Cascade",
                appointmentAt = Instant.now().plus(2, ChronoUnit.DAYS),
                reason = "Alasan kontrol cascade"
            )
        )
        assertTrue(createRes.isSuccess())
        val cId = (createRes as id.temankontrol.app.core.common.AppResult.Success).data

        // Add question & follow-up
        questionRepo.create(cId, QuestionInput("Pertanyaan cascade", false))
        followUpRepo.create(cId, FollowUpInput("Tindak lanjut cascade", null))

        // Check counts
        val counts = consultationRepo.childCounts(cId)
        assertTrue(counts.isSuccess())
        assertEquals(1, (counts as id.temankontrol.app.core.common.AppResult.Success).data.questionCount)
        assertEquals(1, counts.data.actionCount)

        // Delete consultation
        val delRes = consultationRepo.delete(cId)
        assertTrue(delRes.isSuccess())

        // Verify child records are gone
        val questions = questionRepo.observeByConsultation(cId).first()
        assertTrue(questions.isEmpty())
        val followUps = followUpRepo.observeByConsultation(cId).first()
        assertTrue(followUps.isEmpty())
    }
}
