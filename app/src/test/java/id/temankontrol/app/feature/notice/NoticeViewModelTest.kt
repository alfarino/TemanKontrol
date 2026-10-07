package id.temankontrol.app.feature.notice

import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.InMemoryPreferencesRepository
import id.temankontrol.app.data.mock.MockScenarioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scenarioManager: MockScenarioManager
    private lateinit var dataStore: InMemoryDataStore
    private lateinit var preferencesRepo: InMemoryPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scenarioManager = MockScenarioManager()
        dataStore = InMemoryDataStore(scenarioManager)
        dataStore.setNoticeAccepted(false)
        preferencesRepo = InMemoryPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isAcceptedFalse() = runTest(testDispatcher) {
        val viewModel = NoticeViewModel(preferencesRepo)
        val state = viewModel.uiState.first()

        assertFalse(state.isAccepted)
    }

    @Test
    fun acceptNotice_updatesPreferenceAndInvokesCallback() = runTest(testDispatcher) {
        val viewModel = NoticeViewModel(preferencesRepo)
        var callbackInvoked = false

        viewModel.acceptNotice {
            callbackInvoked = true
        }
        testScheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        val acceptedPref = preferencesRepo.noticeAccepted.first()
        assertTrue(acceptedPref)
    }
}