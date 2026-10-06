package id.temankontrol.app.feature.question

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.common.AppError
import id.temankontrol.app.core.common.AppResult
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.model.QuestionInput
import id.temankontrol.app.domain.model.QuestionSuggestion
import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.domain.repository.QuestionRepository
import id.temankontrol.app.domain.repository.SuggestionCatalog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionUiState(
    val questions: List<Question> = emptyList(),
    val priorityCount: Int = 0,
    val isLoading: Boolean = true,
    val editingQuestion: Question? = null,
    val inputText: String = "",
    val inputIsPriority: Boolean = false,
    val inputError: String? = null,
    val showAddDialog: Boolean = false,
    val showSuggestionSheet: Boolean = false,
    val selectedCategory: SuggestionCategory = SuggestionCategory.SYMPTOM,
    val suggestions: List<QuestionSuggestion> = emptyList()
)

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val suggestionCatalog: SuggestionCatalog,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val consultationIdString: String = checkNotNull(savedStateHandle["consultationId"])
    private val consultationId = ConsultationId(consultationIdString)

    private val _uiState = MutableStateFlow(QuestionUiState())
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    init {
        loadSuggestions(SuggestionCategory.SYMPTOM)
        observeQuestions()
    }

    private fun observeQuestions() {
        viewModelScope.launch {
            questionRepository.observeByConsultation(consultationId).collect { list ->
                val pCount = list.count { it.isPriority }
                _uiState.value = _uiState.value.copy(
                    questions = list,
                    priorityCount = pCount,
                    isLoading = false
                )
            }
        }
    }

    fun setInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, inputError = null)
    }

    fun setInputPriority(priority: Boolean) {
        _uiState.value = _uiState.value.copy(inputIsPriority = priority, inputError = null)
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            editingQuestion = null,
            inputText = "",
            inputIsPriority = false,
            inputError = null,
            showAddDialog = true
        )
    }

    fun openEditDialog(question: Question) {
        _uiState.value = _uiState.value.copy(
            editingQuestion = question,
            inputText = question.text,
            inputIsPriority = question.isPriority,
            inputError = null,
            showAddDialog = true
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingQuestion = null, inputError = null)
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
            showSuggestionSheet = false
        )
    }

    fun saveQuestion() {
        val state = _uiState.value
        val input = QuestionInput(
            text = state.inputText,
            isPriority = state.inputIsPriority
        )

        viewModelScope.launch {
            if (state.editingQuestion == null) {
                when (val result = questionRepository.create(consultationId, input)) {
                    is AppResult.Success -> {
                        closeDialog()
                    }
                    is AppResult.Failure -> {
                        handleError(result.error)
                    }
                }
            } else {
                when (val result = questionRepository.update(state.editingQuestion.id, input)) {
                    is AppResult.Success -> {
                        closeDialog()
                    }
                    is AppResult.Failure -> {
                        handleError(result.error)
                    }
                }
            }
        }
    }

    private fun handleError(error: AppError) {
        val msg = when (error) {
            is AppError.Validation -> error.message
            is AppError.PriorityLimitExceeded -> "Maksimal 3 pertanyaan prioritas."
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
}
