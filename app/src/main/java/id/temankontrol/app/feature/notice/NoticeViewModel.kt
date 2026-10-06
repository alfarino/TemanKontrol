package id.temankontrol.app.feature.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoticeUiState(
    val isAccepted: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<NoticeUiState> = preferencesRepository.noticeAccepted
        .map { NoticeUiState(isAccepted = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NoticeUiState()
        )

    fun acceptNotice(onAccepted: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setNoticeAccepted()
            onAccepted()
        }
    }
}
