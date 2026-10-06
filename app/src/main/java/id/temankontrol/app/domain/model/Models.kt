package id.temankontrol.app.domain.model

import java.time.Instant
import java.time.LocalDate

@JvmInline
value class ConsultationId(val value: String)

enum class ConsultationStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}

enum class StatusFilter {
    ALL,
    SCHEDULED,
    COMPLETED,
    CANCELLED
}

enum class SuggestionCategory {
    SYMPTOM,
    EXAM,
    TREATMENT,
    NEXT_STEP
}

data class Consultation(
    val id: ConsultationId,
    val title: String,
    val appointmentAt: Instant,
    val reason: String,
    val facilityName: String?,
    val providerName: String?,
    val status: ConsultationStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ConsultationSummary(
    val id: ConsultationId,
    val title: String,
    val appointmentAt: Instant,
    val facilityName: String?,
    val status: ConsultationStatus,
    val questionCount: Int,
    val discussedCount: Int,
    val openActionCount: Int
)

data class Question(
    val id: String,
    val consultationId: ConsultationId,
    val text: String,
    val isPriority: Boolean,
    val isDiscussed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ConsultationNote(
    val consultationId: ConsultationId,
    val summary: String?,
    val providerInstruction: String?,
    val additionalNotes: String?,
    val updatedAt: Instant
) {
    fun isNotEmpty(): Boolean =
        !summary.isNullOrBlank() || !providerInstruction.isNullOrBlank() || !additionalNotes.isNullOrBlank()
}

data class FollowUpAction(
    val id: String,
    val consultationId: ConsultationId,
    val text: String,
    val dueDate: LocalDate?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class OpenFollowUp(
    val action: FollowUpAction,
    val consultationTitle: String
)

data class QuestionSuggestion(
    val id: String,
    val category: SuggestionCategory,
    val text: String
)

data class ChildCounts(
    val questionCount: Int,
    val hasNote: Boolean,
    val actionCount: Int
)

// Inputs for create/update operations
data class ConsultationInput(
    val title: String,
    val appointmentAt: Instant,
    val reason: String,
    val facilityName: String? = null,
    val providerName: String? = null
)

data class QuestionInput(
    val text: String,
    val isPriority: Boolean = false
)

data class NoteInput(
    val summary: String? = null,
    val providerInstruction: String? = null,
    val additionalNotes: String? = null
)

data class FollowUpInput(
    val text: String,
    val dueDate: LocalDate? = null
)
