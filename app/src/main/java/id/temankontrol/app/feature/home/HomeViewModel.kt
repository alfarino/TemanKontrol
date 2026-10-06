package id.temankontrol.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.domain.model.ConsultationSummary
import id.temankontrol.app.domain.model.OpenFollowUp
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.repository.FollowUpRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val upcomingConsultations: List<ConsultationSummary> = emptyList(),
    val openFollowUps: List<OpenFollowUp> = emptyList(),
    val pastDueCount: Int = 0,
    val isLoading: Boolean = false,
    val isTotalEmpty: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    private val followUpRepository: FollowUpRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        consultationRepository.observeUpcoming(limit = 3),
        followUpRepository.observeOpen(limit = 5),
        consultationRepository.observePastDueCount()
    ) { upcoming, openActions, pastDue ->
        HomeUiState(
            upcomingConsultations = upcoming,
            openFollowUps = openActions,
            pastDueCount = pastDue,
            isLoading = false,
            isTotalEmpty = upcoming.isEmpty() && openActions.isEmpty() && pastDue == 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun toggleActionCompletion(actionId: String, completed: Boolean) {
        viewModelScope.launch {
            followUpRepository.setCompleted(actionId, completed)
        }
    }
}
