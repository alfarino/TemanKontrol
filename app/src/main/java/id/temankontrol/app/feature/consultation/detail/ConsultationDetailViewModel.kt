package id.temankontrol.app.feature.consultation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.*
import id.temankontrol.app.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsultationDetailUiState(
    val consultation: Consultation? = null,
    val questions: List<Question> = emptyList(),
    val note: ConsultationNote? = null,
    val followUps: List<FollowUpAction> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val otherScheduledConsultations: List<ConsultationSummary> = emptyList(),
    val childCounts: ChildCounts = ChildCounts(0, false, 0),
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false
) {
    val attachmentsByQuestion: Map<String, List<Attachment>>
        get() = attachments.filter { it.target == AttachmentTarget.QUESTION && it.questionId != null }
            .groupBy { it.questionId!! }

    val additionalNoteAttachments: List<Attachment>
        get() = attachments.filter { it.target == AttachmentTarget.ADDITIONAL_NOTE }

    val unansweredQuestions: List<Question>
        get() = questions.filter { !it.isDiscussed }
}

@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val questionRepository: QuestionRepository,
    private val noteRepository: NoteRepository,
    private val followUpRepository: FollowUpRepository,
    private val attachmentRepository: AttachmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val consultationIdString: String = checkNotNull(savedStateHandle["id"])
    private val consultationId = ConsultationId(consultationIdString)

    val uiState: StateFlow<ConsultationDetailUiState> = combine(
        consultationRepository.observe(consultationId),
        questionRepository.observeByConsultation(consultationId),
        noteRepository.observe(consultationId),
        followUpRepository.observeByConsultation(consultationId),
        attachmentRepository.observeByConsultation(consultationId),
        consultationRepository.observeHistory(StatusFilter.SCHEDULED)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val consultation = values[0] as Consultation?
        @Suppress("UNCHECKED_CAST")
        val questions = values[1] as List<Question>
        @Suppress("UNCHECKED_CAST")
        val note = values[2] as ConsultationNote?
        @Suppress("UNCHECKED_CAST")
        val followUps = values[3] as List<FollowUpAction>
        @Suppress("UNCHECKED_CAST")
        val attachments = values[4] as List<Attachment>
        @Suppress("UNCHECKED_CAST")
        val scheduled = values[5] as List<ConsultationSummary>

        if (consultation == null) {
            ConsultationDetailUiState(isLoading = false, isNotFound = true)
        } else {
            ConsultationDetailUiState(
                consultation = consultation,
                questions = questions,
                note = note,
                followUps = followUps,
                attachments = attachments,
                otherScheduledConsultations = scheduled.filter { it.id != consultationId },
                childCounts = ChildCounts(questions.size, note != null, followUps.size),
                isLoading = false,
                isNotFound = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConsultationDetailUiState(isLoading = true)
    )

    fun setStatus(status: ConsultationStatus) {
        viewModelScope.launch {
            consultationRepository.setStatus(consultationId, status)
        }
    }

    fun toggleQuestionDiscussed(questionId: String, discussed: Boolean) {
        viewModelScope.launch {
            questionRepository.setDiscussed(questionId, discussed)
        }
    }

    fun saveInlineAnswer(questionId: String, answer: String?) {
        viewModelScope.launch {
            val cleaned = answer?.trim()?.ifBlank { null }
            questionRepository.updateAnswer(questionId, cleaned)
            if (cleaned != null) {
                questionRepository.setDiscussed(questionId, true)
            }
        }
    }

    fun toggleFollowUpCompleted(actionId: String, completed: Boolean) {
        viewModelScope.launch {
            followUpRepository.setCompleted(actionId, completed)
        }
    }

    fun copyQuestionsToConsultation(
        targetConsultationId: String,
        questionIds: Set<String>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = questionRepository.copyUnansweredQuestions(
                fromConsultationId = consultationId,
                toConsultationId = ConsultationId(targetConsultationId),
                questionIds = questionIds
            )) {
                is AppResult.Success -> {
                    val data = res.data
                    val msg = if (data.skippedDuplicateCount > 0) {
                        "Dibawa ${data.copiedCount} pertanyaan, dilewati ${data.skippedDuplicateCount} (sudah ada)."
                    } else {
                        "${data.copiedCount} pertanyaan dibawa ke kunjungan tujuan."
                    }
                    onResult(msg)
                }
                is AppResult.Failure -> {
                    onResult("Gagal membawa pertanyaan.")
                }
            }
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.delete(attachment.id)
        }
    }

    fun addPhotoAttachment(
        questionId: String?,
        target: AttachmentTarget,
        caption: String?,
        uriString: String?
    ) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "foto_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
            val sizeBytes = fileObj?.takeIf { it.exists() }?.length() ?: 320_000L
            attachmentRepository.addAttachment(
                consultationId = consultationId,
                questionId = questionId,
                target = target,
                kind = AttachmentKind.PHOTO,
                fileName = fileName,
                mimeType = "image/jpeg",
                sizeBytes = sizeBytes,
                caption = caption,
                uriString = uriString
            )
        }
    }

    fun addVoiceAttachment(
        questionId: String?,
        target: AttachmentTarget,
        durationMs: Long,
        caption: String?,
        uriString: String?
    ) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "vn_${java.util.UUID.randomUUID().toString().take(6)}.m4a"
            val sizeBytes = fileObj?.takeIf { it.exists() }?.length() ?: (durationMs / 1000L).coerceAtLeast(1L) * 4_000L
            attachmentRepository.addAttachment(
                consultationId = consultationId,
                questionId = questionId,
                target = target,
                kind = AttachmentKind.VOICE,
                fileName = fileName,
                mimeType = "audio/mp4",
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                caption = caption,
                uriString = uriString
            )
        }
    }

    fun updateAttachmentCaption(attachmentId: String, caption: String?) {
        viewModelScope.launch {
            attachmentRepository.updateCaption(attachmentId, caption)
        }
    }

    fun deleteConsultation(onDeleted: () -> Unit) {
        viewModelScope.launch {
            consultationRepository.delete(consultationId)
            onDeleted()
        }
    }
}
