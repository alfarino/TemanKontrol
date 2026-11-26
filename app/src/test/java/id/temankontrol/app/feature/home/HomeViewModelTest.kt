package id.temankontrol.app.feature.home

import id.temankontrol.app.data.mock.InMemoryConsultationRepository
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.InMemoryFollowUpRepository
import id.temankontrol.app.data.mock.InMemoryPreferencesRepository
import id.temankontrol.app.data.mock.MockScenario
import id.temankontrol.app.data.mock.MockScenarioManager
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scenarioManager: MockScenarioManager
    private lateinit var dataStore: InMemoryDataStore
    private lateinit var consultationRepo: InMemoryConsultationRepository
    private lateinit var followUpRepo: InMemoryFollowUpRepository
    private lateinit var preferencesRepo: InMemoryPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scenarioManager = MockScenarioManager()
        dataStore = InMemoryDataStore(scenarioManager)
        consultationRepo = InMemoryConsultationRepository(dataStore, scenarioManager)
        followUpRepo = InMemoryFollowUpRepository(dataStore, scenarioManager)
        preferencesRepo = InMemoryPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun normalScenario_loadsUpcomingAndFollowUps() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(consultationRepo, followUpRepo, preferencesRepo)
        val state = viewModel.uiState.filter { !it.isLoading }.first()

        assertFalse(state.isLoading)
        assertFalse(state.isTotalEmpty)
        assertTrue(state.upcomingConsultations.isNotEmpty())
        assertTrue(state.openFollowUps.isNotEmpty())
    }

    @Test
    fun emptyScenario_showsTotalEmptyTrue() = runTest(testDispatcher) {
        dataStore.loadScenario(MockScenario.EMPTY)
        val viewModel = HomeViewModel(consultationRepo, followUpRepo, preferencesRepo)
        val state = viewModel.uiState.filter { !it.isLoading }.first()

        assertFalse(state.isLoading)
        assertTrue(state.isTotalEmpty)
        assertEquals(0, state.upcomingConsultations.size)
        assertEquals(0, state.openFollowUps.size)
        assertEquals(0, state.pastDueCount)
    }

    @Test
    fun toggleActionCompletion_updatesActionState() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(consultationRepo, followUpRepo, preferencesRepo)
        val state = viewModel.uiState.filter { !it.isLoading }.first()

        val firstAction = state.openFollowUps.firstOrNull()
        assertNotNull(firstAction)

        val targetId = firstAction!!.action.id
        viewModel.toggleActionCompletion(targetId, true)
        testScheduler.advanceUntilIdle()

        val updatedAction = followUpRepo.get(targetId)
        assertTrue((updatedAction as id.temankontrol.app.core.common.AppResult.Success).data.isCompleted)
    }
}
