package id.temankontrol.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.domain.model.ConsultationSummary
import id.temankontrol.app.domain.model.StatusFilter
import id.temankontrol.app.domain.repository.ConsultationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HistoryUiState(
    val filter: StatusFilter = StatusFilter.ALL,
    val searchQuery: String = "",
    val consultations: List<ConsultationSummary> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val consultationRepository: ConsultationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialFilterString: String? = savedStateHandle["filter"]
    private val initialFilter = try {
        if (initialFilterString != null) StatusFilter.valueOf(initialFilterString) else StatusFilter.ALL
    } catch (_: Exception) {
        StatusFilter.ALL
    }

    private val _currentFilter = MutableStateFlow(initialFilter)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        _currentFilter.flatMapLatest { filter ->
            consultationRepository.observeHistory(filter).map { filter to it }
        },
        _searchQuery
    ) { (filter, list), query ->
        val trimmed = query.trim()
        val filteredList = if (trimmed.isEmpty()) {
            list
        } else {
            list.filter { item ->
                item.title.contains(trimmed, ignoreCase = true) ||
                    (item.facilityName?.contains(trimmed, ignoreCase = true) == true)
            }
        }
        HistoryUiState(
            filter = filter,
            searchQuery = query,
            consultations = filteredList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(filter = initialFilter, isLoading = true)
    )

    fun setFilter(filter: StatusFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
