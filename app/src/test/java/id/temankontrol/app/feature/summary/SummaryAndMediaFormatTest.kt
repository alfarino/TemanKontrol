package id.temankontrol.app.feature.summary

import id.temankontrol.app.core.designsystem.component.formatDuration
import id.temankontrol.app.core.designsystem.component.formatSizeKb
import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationNote
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.Question
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SummaryAndMediaFormatTest {

    @Test
    fun buildPlainTextSummary_respectsTogglesAndIncludesDisclaimer() {
        val now = Instant.parse("2026-11-17T08:00:00Z")
        val state = SummaryPreviewUiState(
            consultation = Consultation(
                id = ConsultationId("c-1"),
                title = "Kontrol Hipertensi",
                appointmentAt = now,
                reason = "Evaluasi tensi",
                facilityName = "RS Sehat",
                providerName = "dr. Budi",
                status = ConsultationStatus.COMPLETED,
                createdAt = now,
                updatedAt = now
            ),
            questions = listOf(
                Question(
                    id = "q-1",
                    consultationId = ConsultationId("c-1"),
                    text = "Bagaimana target tensi saya?",
                    isPriority = true,
                    isDiscussed = true,
                    createdAt = now,
                    updatedAt = now,
                    answer = "Di bawah 130/80 mmHg."
                )
            ),
            note = ConsultationNote(
                consultationId = ConsultationId("c-1"),
                summary = "Tensi membaik",
                providerInstruction = "Kurangi garam",
                additionalNotes = null,
                updatedAt = now
            ),
            followUps = listOf(
                FollowUpAction(
                    id = "f-1",
                    consultationId = ConsultationId("c-1"),
                    text = "Cek lab bulan depan",
                    dueDate = null,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            ),
            includeProvider = false,
            includeQuestions = true,
            includeNotes = true,
            includeFollowUps = true,
            isLoading = false
        )

        val disclaimer = "Dibuat oleh pengguna dengan Teman Kontrol. Bukan dokumen medis resmi."
        val text = state.buildPlainTextSummary(disclaimer)

        assertTrue(text.contains("Kontrol Hipertensi"))
        assertFalse(text.contains("dr. Budi"))
        assertTrue(text.contains("Bagaimana target tensi saya?"))
        assertTrue(text.contains("Di bawah 130/80 mmHg."))
        assertTrue(text.contains("Tensi membaik"))
        assertTrue(text.contains("Cek lab bulan depan"))
        assertTrue(text.contains(disclaimer))
    }

    @Test
    fun mediaFormatters_formatDurationAndSizeCorrectly() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("01:22", formatDuration(82_000L))
        assertEquals("500 B", formatSizeKb(500L))
        assertEquals("279 KB", formatSizeKb(285_400L))
    }
}
