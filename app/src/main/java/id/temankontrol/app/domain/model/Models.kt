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
    NEXT_STEP,
    UNDERSTANDING
}

enum class AttachmentKind {
    PHOTO,
    VOICE
}

enum class AttachmentTarget {
    QUESTION,
    ADDITIONAL_NOTE
}

enum class AppLockTimeout(val label: String, val minutes: Int) {
    IMMEDIATE("Langsung", 0),
    ONE_MINUTE("1 menit", 1),
    FIVE_MINUTES("5 menit", 5)
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
    val openActionCount: Int,
    val isPastDue: Boolean = false
)

data class Question(
    val id: String,
    val consultationId: ConsultationId,
    val text: String,
    val isPriority: Boolean,
    val isDiscussed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val answer: String? = null,
    val answerUpdatedAt: Instant? = null
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
    val isPriority: Boolean = false,
    val answer: String? = null
)

data class Attachment(
    val id: String,
    val consultationId: ConsultationId,
    val questionId: String? = null,
    val target: AttachmentTarget = AttachmentTarget.ADDITIONAL_NOTE,
    val kind: AttachmentKind,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long? = null,
    val caption: String? = null,
    val uriString: String? = null,
    val createdAt: Instant
)

data class ReminderSettings(
    val visitReminderEnabled: Boolean = true,
    val visitReminderHour: Int = 19,
    val visitThirtyMinBeforeEnabled: Boolean = true,
    val visitAtTimeEnabled: Boolean = true,
    val customLeadMinutes: Int? = null,
    val alarmSoundModeEnabled: Boolean = true,
    val prepareReminderEnabled: Boolean = true,
    val followUpReminderEnabled: Boolean = true,
    val followUpReminderHour: Int = 8,
    val backupReminderEnabled: Boolean = true,
    val showTitleOnLockScreen: Boolean = false,
    val hasNotificationPermission: Boolean = true
)

data class AppLockSettings(
    val enabled: Boolean = false,
    val timeout: AppLockTimeout = AppLockTimeout.ONE_MINUTE,
    val secureScreen: Boolean = true,
    val hasDeviceCredential: Boolean = true
)

data class BackupSummaryInfo(
    val lastBackupAt: Instant? = null,
    val hasUnsavedChangesSinceBackup: Boolean = true,
    val consultationCount: Int = 0,
    val questionCount: Int = 0,
    val noteCount: Int = 0,
    val followUpCount: Int = 0,
    val attachmentCount: Int = 0,
    val estimatedSizeBytes: Long = 0L
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

@kotlinx.serialization.Serializable
enum class SyncEntityType {
    CONSULTATION,
    QUESTION,
    NOTE,
    FOLLOW_UP,
    ATTACHMENT
}

@kotlinx.serialization.Serializable
data class SyncTombstone(
    val entityType: SyncEntityType,
    val entityId: String,
    val deletedAtMillis: Long
)

@kotlinx.serialization.Serializable
data class AccountSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val lastSyncAtMillis: Long? = null,
    val serverBaseUrl: String = "https://api.temankontrol.unand.tech"
)

data class SyncSummaryResult(
    val pushedRecords: Int,
    val pulledRecords: Int,
    val uploadedFiles: Int,
    val downloadedFiles: Int,
    val syncedAtMillis: Long
)
