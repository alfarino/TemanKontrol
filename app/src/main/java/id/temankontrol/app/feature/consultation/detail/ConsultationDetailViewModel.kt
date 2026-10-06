package id.temankontrol.app.feature.consultation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val childCounts: ChildCounts = ChildCounts(0, false, 0),
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false
)

@HiltViewModel
class ConsultationDetailViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val questionRepository: QuestionRepository,
    private val noteRepository: NoteRepository,
    private val followUpRepository: FollowUpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val consultationIdString: String = checkNotNull(savedStateHandle["id"])
    private val consultationId = ConsultationId(consultationIdString)

    val uiState: StateFlow<ConsultationDetailUiState> = combine(
        consultationRepository.observe(consultationId),
        questionRepository.observeByConsultation(consultationId),
        noteRepository.observe(consultationId),
        followUpRepository.observeByConsultation(consultationId)
    ) { consultation, questions, note, followUps ->
        if (consultation == null) {
            ConsultationDetailUiState(isLoading = false, isNotFound = true)
        } else {
            ConsultationDetailUiState(
                consultation = consultation,
                questions = questions,
                note = note,
                followUps = followUps,
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

    fun toggleFollowUpCompleted(actionId: String, completed: Boolean) {
        viewModelScope.launch {
            followUpRepository.setCompleted(actionId, completed)
        }
    }

    fun deleteConsultation(onDeleted: () -> Unit) {
        viewModelScope.launch {
            consultationRepository.delete(consultationId)
            onDeleted()
        }
    }
}
