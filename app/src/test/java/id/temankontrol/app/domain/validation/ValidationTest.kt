package id.temankontrol.app.domain.validation

import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationInput
import id.temankontrol.app.domain.model.FollowUpInput
import id.temankontrol.app.domain.model.NoteInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ValidationTest {

    @Test
    fun vr01_consultationTitle_empty_returnsValidationError() {
        val result = Validation.validateConsultationTitle("   ")
        assertTrue(result is AppResult.Failure)
        assertEquals("title", ((result as AppResult.Failure).error as AppError.Validation).field)
    }

    @Test
    fun vr01_consultationTitle_overMaxLength_returnsValidationError() {
        val longTitle = "a".repeat(81)
        val result = Validation.validateConsultationTitle(longTitle)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr01_consultationTitle_normalizesWhitespace() {
        val result = Validation.validateConsultationTitle("  Kontrol   Rutin   Poli  ")
        assertTrue(result is AppResult.Success)
        assertEquals("Kontrol Rutin Poli", (result as AppResult.Success).data)
    }

    @Test
    fun vr03_consultationReason_empty_returnsValidationError() {
        val result = Validation.validateConsultationReason("   ")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr03_consultationReason_overMaxLength_returnsValidationError() {
        val longReason = "a".repeat(501)
        val result = Validation.validateConsultationReason(longReason)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr04_vr05_optionalFields_validLength_trimmed() {
        val result = Validation.validateOptionalField("facility", "Fasilitas", "  RSUP M Djamil  ")
        assertTrue(result is AppResult.Success)
        assertEquals("RSUP M Djamil", (result as AppResult.Success).data)
    }

    @Test
    fun vr06_questionText_empty_returnsValidationError() {
        val result = Validation.validateQuestionText("")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr06_questionText_overMaxLength_returnsValidationError() {
        val longQuestion = "a".repeat(251)
        val result = Validation.validateQuestionText(longQuestion)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr07_priorityLimit_fourthPriority_rejected() {
        val result = Validation.validatePriorityLimit(currentPriorityCount = 3, addingPriority = true)
        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.PriorityLimitExceeded, (result as AppResult.Failure).error)
    }

    @Test
    fun vr07_priorityLimit_underLimit_accepted() {
        val result = Validation.validatePriorityLimit(currentPriorityCount = 2, addingPriority = true)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun vr08_duplicateQuestion_detectedCaseInsensitive() {
        val existing = setOf("kapan saya harus kontrol lagi?")
        val result = Validation.validateNotDuplicateQuestion("  Kapan Saya Harus KONTROL   Lagi? ", existing)
        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Duplicate, (result as AppResult.Failure).error)
    }

    @Test
    fun vr08_duplicateQuestion_distinct_accepted() {
        val existing = setOf("pertanyaan a")
        val result = Validation.validateNotDuplicateQuestion("pertanyaan b", existing)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun vr09_vr10_noteInput_allEmpty_rejected() {
        val result = Validation.validateNoteInput(NoteInput(summary = "  ", providerInstruction = null, additionalNotes = ""))
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr09_vr10_noteInput_partialValid_accepted() {
        val result = Validation.validateNoteInput(NoteInput(summary = "Tekanan darah 120/80", providerInstruction = null, additionalNotes = null))
        assertTrue(result is AppResult.Success)
        assertEquals("Tekanan darah 120/80", (result as AppResult.Success).data.summary)
    }

    @Test
    fun vr11_followUpText_empty_rejected() {
        val result = Validation.validateFollowUpText("   ")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun vr12_dueDatePast_returnsWarningFlag() {
        val yesterday = LocalDate.of(2026, 10, 5)
        val today = LocalDate.of(2026, 10, 6)
        assertTrue(Validation.isDueDatePast(yesterday, today))
        assertFalse(Validation.isDueDatePast(today, today))
    }
}
