package id.temankontrol.app.feature.question

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.AttachmentKind
import id.temankontrol.app.domain.model.AttachmentTarget
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.model.QuestionInput
import id.temankontrol.app.domain.model.QuestionSuggestion
import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.domain.repository.AttachmentRepository
import id.temankontrol.app.domain.repository.QuestionRepository
import id.temankontrol.app.domain.repository.SuggestionCatalog
import id.temankontrol.app.domain.validation.Validation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuestionUiState(
    val questions: List<Question> = emptyList(),
    val attachmentsByQuestion: Map<String, List<Attachment>> = emptyMap(),
    val priorityCount: Int = 0,
    val isLoading: Boolean = true,
    val editingQuestion: Question? = null,
    val inputText: String = "",
    val inputIsPriority: Boolean = false,
    val inputAnswer: String = "",
    val inputError: String? = null,
    val showClosedQuestionHint: Boolean = false,
    val dismissedClosedHintForText: String? = null,
    val showAddDialog: Boolean = false,
    val showSuggestionSheet: Boolean = false,
    val selectedCategory: SuggestionCategory = SuggestionCategory.SYMPTOM,
    val suggestions: List<QuestionSuggestion> = emptyList(),
    val expandedAttachmentQuestionIds: Set<String> = emptySet()
)

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val attachmentRepository: AttachmentRepository,
    private val suggestionCatalog: SuggestionCatalog,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val consultationIdString: String = checkNotNull(savedStateHandle["consultationId"])
    private val consultationId = ConsultationId(consultationIdString)

    private val _uiState = MutableStateFlow(QuestionUiState())
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    init {
        loadSuggestions(SuggestionCategory.SYMPTOM)
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                questionRepository.observeByConsultation(consultationId),
                attachmentRepository.observeByConsultation(consultationId)
            ) { questions, attachments ->
                val byQuestion = attachments
                    .filter { it.target == AttachmentTarget.QUESTION && it.questionId != null }
                    .groupBy { it.questionId!! }
                questions to byQuestion
            }.collect { (list, byQuestion) ->
                val pCount = list.count { it.isPriority }
                _uiState.value = _uiState.value.copy(
                    questions = list,
                    attachmentsByQuestion = byQuestion,
                    priorityCount = pCount,
                    isLoading = false
                )
            }
        }
    }

    fun setInputText(text: String) {
        val current = _uiState.value
        val isClosed = Validation.isLikelyClosedQuestion(text)
        val shouldShowHint = isClosed && current.dismissedClosedHintForText != text.trim()
        _uiState.value = current.copy(
            inputText = text,
            inputError = null,
            showClosedQuestionHint = shouldShowHint
        )
    }

    fun applyStarterChip(prefix: String) {
        val currentText = _uiState.value.inputText.trimStart()
        val newText = if (currentText.isEmpty()) {
            "$prefix "
        } else {
            val lowered = currentText.replaceFirstChar { it.lowercase() }
            "$prefix $lowered"
        }
        setInputText(newText.take(Validation.MAX_QUESTION_LENGTH))
    }

    fun dismissClosedQuestionHint() {
        val current = _uiState.value
        _uiState.value = current.copy(
            showClosedQuestionHint = false,
            dismissedClosedHintForText = current.inputText.trim()
        )
    }

    fun setInputPriority(priority: Boolean) {
        _uiState.value = _uiState.value.copy(inputIsPriority = priority, inputError = null)
    }

    fun setInputAnswer(answer: String) {
        _uiState.value = _uiState.value.copy(inputAnswer = answer, inputError = null)
    }

    fun toggleAttachmentSection(questionId: String) {
        val current = _uiState.value.expandedAttachmentQuestionIds.toMutableSet()
        if (!current.add(questionId)) {
            current.remove(questionId)
        }
        _uiState.value = _uiState.value.copy(expandedAttachmentQuestionIds = current)
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            editingQuestion = null,
            inputText = "",
            inputIsPriority = false,
            inputAnswer = "",
            inputError = null,
            showClosedQuestionHint = false,
            dismissedClosedHintForText = null,
            showAddDialog = true
        )
    }

    fun openEditDialog(question: Question) {
        _uiState.value = _uiState.value.copy(
            editingQuestion = question,
            inputText = question.text,
            inputIsPriority = question.isPriority,
            inputAnswer = question.answer ?: "",
            inputError = null,
            showClosedQuestionHint = Validation.isLikelyClosedQuestion(question.text),
            dismissedClosedHintForText = null,
            showAddDialog = true
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingQuestion = null,
            inputError = null,
            showClosedQuestionHint = false
        )
    }

    fun openSuggestionSheet() {
        _uiState.value = _uiState.value.copy(showSuggestionSheet = true)
    }

    fun closeSuggestionSheet() {
        _uiState.value = _uiState.value.copy(showSuggestionSheet = false)
    }

    fun selectCategory(category: SuggestionCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadSuggestions(category)
    }

    private fun loadSuggestions(category: SuggestionCategory) {
        val list = suggestionCatalog.byCategory(category)
        _uiState.value = _uiState.value.copy(suggestions = list)
    }

    fun applySuggestion(suggestionText: String) {
        _uiState.value = _uiState.value.copy(
            inputText = suggestionText,
            showSuggestionSheet = false,
            showClosedQuestionHint = false
        )
    }

    fun saveQuestion() {
        val state = _uiState.value
        val input = QuestionInput(
            text = state.inputText,
            isPriority = state.inputIsPriority,
            answer = state.inputAnswer.ifBlank { null }
        )

        viewModelScope.launch {
            if (state.editingQuestion == null) {
                when (val result = questionRepository.create(consultationId, input)) {
                    is AppResult.Success -> closeDialog()
                    is AppResult.Failure -> handleError(result.error)
                }
            } else {
                when (val result = questionRepository.update(state.editingQuestion.id, input)) {
                    is AppResult.Success -> closeDialog()
                    is AppResult.Failure -> handleError(result.error)
                }
            }
        }
    }

    private fun handleError(error: AppError) {
        val msg = when (error) {
            is AppError.Validation -> error.message
            is AppError.Duplicate -> "Pertanyaan ini sudah ada di kunjungan ini."
            else -> "Terjadi kesalahan."
        }
        _uiState.value = _uiState.value.copy(inputError = msg)
    }

    fun toggleDiscussed(questionId: String, discussed: Boolean) {
        viewModelScope.launch {
            questionRepository.setDiscussed(questionId, discussed)
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            questionRepository.delete(questionId)
        }
    }

    fun addPhotoToQuestion(questionId: String, caption: String?, uriString: String?) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "foto_${UUID.randomUUID().toString().take(6)}.jpg"
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
            // Keep question's attachment section expanded after adding
            val expanded = _uiState.value.expandedAttachmentQuestionIds.toMutableSet()
            expanded.add(questionId)
            _uiState.value = _uiState.value.copy(expandedAttachmentQuestionIds = expanded)
        }
    }

    fun addVoiceNoteToQuestion(questionId: String, durationMs: Long, caption: String?, uriString: String?) {
        viewModelScope.launch {
            val fileObj = uriString?.let { android.net.Uri.parse(it).path }?.let { java.io.File(it) }
            val fileName = fileObj?.name ?: "vn_${UUID.randomUUID().toString().take(6)}.m4a"
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
            val expanded = _uiState.value.expandedAttachmentQuestionIds.toMutableSet()
            expanded.add(questionId)
            _uiState.value = _uiState.value.copy(expandedAttachmentQuestionIds = expanded)
        }
    }

    fun updateAttachmentCaption(attachmentId: String, caption: String?) {
        viewModelScope.launch {
            attachmentRepository.updateCaption(attachmentId, caption)
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.delete(attachment.id)
        }
    }
}
