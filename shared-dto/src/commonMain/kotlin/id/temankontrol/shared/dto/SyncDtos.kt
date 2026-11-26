package id.temankontrol.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncConsultationDto(
    val id: String,
    val title: String,
    val appointmentAtMillis: Long,
    val reason: String,
    val facilityName: String? = null,
    val providerName: String? = null,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null
)

@Serializable
data class SyncQuestionDto(
    val id: String,
    val consultationId: String,
    val text: String,
    val isPriority: Boolean,
    val isDiscussed: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val answer: String? = null,
    val answerUpdatedAtMillis: Long? = null,
    val deletedAtMillis: Long? = null
)

@Serializable
data class SyncNoteDto(
    val consultationId: String,
    val summary: String? = null,
    val providerInstruction: String? = null,
    val additionalNotes: String? = null,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null
)

@Serializable
data class SyncFollowUpDto(
    val id: String,
    val consultationId: String,
    val text: String,
    val dueDateEpochDay: Long? = null,
    val isCompleted: Boolean,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null
)

@Serializable
data class SyncAttachmentMetaDto(
    val id: String,
    val consultationId: String,
    val questionId: String? = null,
    val target: String,
    val kind: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long? = null,
    val caption: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null
)

@Serializable
data class SyncReminderSettingsDto(
    val visitReminderEnabled: Boolean,
    val visitReminderHour: Int,
    val visitThirtyMinBeforeEnabled: Boolean,
    val visitAtTimeEnabled: Boolean,
    val customLeadMinutes: Int? = null,
    val alarmSoundModeEnabled: Boolean,
    val prepareReminderEnabled: Boolean,
    val followUpReminderEnabled: Boolean,
    val followUpReminderHour: Int,
    val backupReminderEnabled: Boolean,
    val showTitleOnLockScreen: Boolean,
    val updatedAtMillis: Long
)

@Serializable
data class SyncPushRequest(
    val consultations: List<SyncConsultationDto> = emptyList(),
    val questions: List<SyncQuestionDto> = emptyList(),
    val notes: List<SyncNoteDto> = emptyList(),
    val followUps: List<SyncFollowUpDto> = emptyList(),
    val attachments: List<SyncAttachmentMetaDto> = emptyList(),
    val reminderSettings: SyncReminderSettingsDto? = null
)

@Serializable
data class SyncPushResponse(
    val acceptedCount: Int,
    val skippedConflictsCount: Int,
    val serverTimestampMillis: Long
)

@Serializable
data class SyncPullResponse(
    val serverTimestampMillis: Long,
    val consultations: List<SyncConsultationDto> = emptyList(),
    val questions: List<SyncQuestionDto> = emptyList(),
    val notes: List<SyncNoteDto> = emptyList(),
    val followUps: List<SyncFollowUpDto> = emptyList(),
    val attachments: List<SyncAttachmentMetaDto> = emptyList(),
    val reminderSettings: SyncReminderSettingsDto? = null
)
