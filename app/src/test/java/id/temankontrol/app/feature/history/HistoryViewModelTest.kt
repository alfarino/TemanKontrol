package id.temankontrol.app.feature.history

import androidx.lifecycle.SavedStateHandle
import id.temankontrol.app.data.mock.InMemoryConsultationRepository
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.MockScenarioManager
import id.temankontrol.app.domain.model.ConsultationStatus
import id.temankontrol.app.domain.model.StatusFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scenarioManager: MockScenarioManager
    private lateinit var dataStore: InMemoryDataStore
    private lateinit var repository: InMemoryConsultationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scenarioManager = MockScenarioManager()
        dataStore = InMemoryDataStore(scenarioManager)
        repository = InMemoryConsultationRepository(dataStore, scenarioManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasAllFilterAndLoadsList() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.filter { !it.isLoading }.first()

        assertEquals(StatusFilter.ALL, state.filter)
        assertFalse(state.isLoading)
        assertTrue(state.consultations.isNotEmpty())
    }

    @Test
    fun setFilter_completed_returnsOnlyCompletedConsultations() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository, SavedStateHandle())
        viewModel.setFilter(StatusFilter.COMPLETED)

        val state = viewModel.uiState.filter { !it.isLoading && it.filter == StatusFilter.COMPLETED }.first()

        assertEquals(StatusFilter.COMPLETED, state.filter)
        assertTrue(state.consultations.all { it.status == ConsultationStatus.COMPLETED })
    }

    @Test
    fun setFilter_scheduled_returnsOnlyScheduledConsultations() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository, SavedStateHandle())
        viewModel.setFilter(StatusFilter.SCHEDULED)

        val state = viewModel.uiState.filter { !it.isLoading && it.filter == StatusFilter.SCHEDULED }.first()

        assertEquals(StatusFilter.SCHEDULED, state.filter)
        assertTrue(state.consultations.all { it.status == ConsultationStatus.SCHEDULED })
    }

    @Test
    fun setFilter_cancelled_returnsOnlyCancelledConsultations() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository, SavedStateHandle())
        viewModel.setFilter(StatusFilter.CANCELLED)

        val state = viewModel.uiState.filter { !it.isLoading && it.filter == StatusFilter.CANCELLED }.first()

        assertEquals(StatusFilter.CANCELLED, state.filter)
        assertTrue(state.consultations.all { it.status == ConsultationStatus.CANCELLED })
    }

    @Test
    fun setSearchQuery_filtersConsultationsByTitleOrFacility() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository, SavedStateHandle())
        val initial = viewModel.uiState.filter { !it.isLoading }.first()
        val sampleTitle = initial.consultations.first().title

        viewModel.setSearchQuery(sampleTitle)
        val filtered = viewModel.uiState.filter { !it.isLoading && it.searchQuery == sampleTitle }.first()
        assertTrue(filtered.consultations.isNotEmpty())
        assertTrue(filtered.consultations.all { it.title.contains(sampleTitle, ignoreCase = true) })

        viewModel.setSearchQuery("TIDAK_ADA_KUNJUNGAN_XYZ_999")
        val emptyResult = viewModel.uiState.filter { !it.isLoading && it.searchQuery == "TIDAK_ADA_KUNJUNGAN_XYZ_999" }.first()
        assertTrue(emptyResult.consultations.isEmpty())
    }
}