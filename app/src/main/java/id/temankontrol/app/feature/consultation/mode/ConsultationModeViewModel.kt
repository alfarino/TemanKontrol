package id.temankontrol.app.feature.consultation.mode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.repository.AttachmentRepository
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ConsultationModeUiState(
    val consultation: Consultation? = null,
    val questions: List<Question> = emptyList(),
    val attachmentsByQuestion: Map<String, List<Attachment>> = emptyMap(),
    val currentIndex: Int = 0,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val draftAnswer: String = ""
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val discussedCount: Int get() = questions.count { it.isDiscussed }
}

@HiltViewModel
class ConsultationModeViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val questionRepository: QuestionRepository,
    private val attachmentRepository: AttachmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val consultationIdString: String = checkNotNull(savedStateHandle["consultationId"])
    private val consultationId = ConsultationId(consultationIdString)

    private val _index = MutableStateFlow(0)
    private val _finished = MutableStateFlow(false)
    private val _draftAnswers = MutableStateFlow<Map<String, String>>(emptyMap())

    val uiState: StateFlow<ConsultationModeUiState> = combine(
        consultationRepository.observe(consultationId),
        questionRepository.observeByConsultation(consultationId),
        attachmentRepository.observeByConsultation(consultationId),
        _index,
        _finished,
        _draftAnswers
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val consultation = values[0] as Consultation?
        @Suppress("UNCHECKED_CAST")
        val questions = values[1] as List<Question>
        @Suppress("UNCHECKED_CAST")
        val attachments = values[2] as List<Attachment>
        val index = values[3] as Int
        val finished = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val drafts = values[5] as Map<String, String>

        val safeIndex = if (questions.isEmpty()) 0 else index.coerceIn(0, questions.lastIndex)
        val currentQ = questions.getOrNull(safeIndex)
        val byQuestion = attachments
            .filter { it.target == AttachmentTarget.QUESTION && it.questionId != null }
            .groupBy { it.questionId!! }

        ConsultationModeUiState(
            consultation = consultation,
            questions = questions,
            attachmentsByQuestion = byQuestion,
            currentIndex = safeIndex,
            isFinished = finished,
            isLoading = false,
            draftAnswer = currentQ?.let { drafts[it.id] ?: it.answer.orEmpty() } ?: ""
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConsultationModeUiState(isLoading = true)
    )

    fun onAnswerDraftChange(questionId: String, text: String) {
        val next = _draftAnswers.value.toMutableMap()
        next[questionId] = text
        _draftAnswers.value = next
    }

    fun saveCurrentAnswer(questionId: String) {
        val answerText = _draftAnswers.value[questionId] ?: return
        viewModelScope.launch {
            questionRepository.updateAnswer(questionId, answerText.ifBlank { null })
        }
    }

    fun toggleDiscussed(questionId: String, discussed: Boolean) {
        viewModelScope.launch {
            questionRepository.setDiscussed(questionId, discussed)
        }
    }

    fun nextQuestion() {
        val state = uiState.value
        state.currentQuestion?.let { saveCurrentAnswer(it.id) }
        if (state.currentIndex < state.questions.lastIndex) {
            _index.value = state.currentIndex + 1
        } else {
            _finished.value = true
        }
    }

    fun previousQuestion() {
        val state = uiState.value
        state.currentQuestion?.let { saveCurrentAnswer(it.id) }
        if (state.isFinished) {
            _finished.value = false
        } else if (state.currentIndex > 0) {
            _index.value = state.currentIndex - 1
        }
    }

    fun addPhoto(questionId: String, caption: String?, uriString: String?) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "mode_foto_${UUID.randomUUID().toString().take(6)}.jpg"
            val sizeBytes = fileObj?.takeIf { it.exists() }?.length() ?: 320_000L
            attachmentRepository.addAttachment(
                consultationId = consultationId,
                questionId = questionId,
                target = AttachmentTarget.QUESTION,
                kind = AttachmentKind.PHOTO,
                fileName = fileName,
                mimeType = "image/jpeg",
                sizeBytes = sizeBytes,
                caption = caption,
                uriString = uriString
            )
        }
    }

    fun addVoiceNote(questionId: String, durationMs: Long, caption: String?, uriString: String?) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "mode_vn_${UUID.randomUUID().toString().take(6)}.m4a"
            val sizeBytes = fileObj?.takeIf { it.exists() }?.length() ?: (durationMs / 1000L).coerceAtLeast(1L) * 4_000L
            attachmentRepository.addAttachment(
                consultationId = consultationId,
                questionId = questionId,
                target = AttachmentTarget.QUESTION,
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

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.delete(attachment.id)
        }
    }

    fun updateAttachmentCaption(attachmentId: String, caption: String?) {
        viewModelScope.launch {
            attachmentRepository.updateCaption(attachmentId, caption)
        }
    }

    fun markConsultationCompleted(onDone: () -> Unit) {
        viewModelScope.launch {
            consultationRepository.setStatus(consultationId, ConsultationStatus.COMPLETED)
            onDone()
        }
    }
}
