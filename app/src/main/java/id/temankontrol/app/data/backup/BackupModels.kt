package id.temankontrol.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupCounts(
    val consultations: Int,
    val questions: Int,
    val notes: Int,
    val followUps: Int,
    val attachments: Int
)

@Serializable
data class BackupManifest(
    val formatVersion: Int = 1,
    val appVersion: String,
    val dbSchemaVersion: Int,
    val createdAt: Long,
    val counts: BackupCounts,
    val sha256Data: String
)

@Serializable
data class BackupConsultation(
    val id: String,
    val title: String,
    val appointmentAt: Long,
    val reason: String,
    val facilityName: String? = null,
    val providerName: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupQuestion(
    val id: String,
    val consultationId: String,
    val text: String,
    val textNormalized: String,
    val isPriority: Boolean,
    val isDiscussed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val answer: String? = null,
    val answerUpdatedAt: Long? = null
)

@Serializable
data class BackupNote(
    val consultationId: String,
    val summary: String? = null,
    val providerInstruction: String? = null,
    val additionalNotes: String? = null,
    val updatedAt: Long
)

@Serializable
data class BackupFollowUp(
    val id: String,
    val consultationId: String,
    val text: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupAttachment(
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
    val uriString: String? = null,
    val createdAt: Long
)

@Serializable
data class BackupPayload(
    val consultations: List<BackupConsultation>,
    val questions: List<BackupQuestion>,
    val notes: List<BackupNote>,
    val followUps: List<BackupFollowUp>,
    val attachments: List<BackupAttachment> = emptyList()
)
