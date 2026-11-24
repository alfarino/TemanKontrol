package id.temankontrol.app.feature.question

import androidx.lifecycle.SavedStateHandle
import id.temankontrol.app.data.mock.InMemoryAttachmentRepository
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.InMemoryQuestionRepository
import id.temankontrol.app.data.mock.MockScenarioManager
import id.temankontrol.app.data.suggestion.StaticSuggestionCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var questionRepo: InMemoryQuestionRepository
    private lateinit var attachmentRepo: InMemoryAttachmentRepository
    private lateinit var catalog: StaticSuggestionCatalog
    private lateinit var viewModel: QuestionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val scenarioManager = MockScenarioManager()
        val dataStore = InMemoryDataStore(scenarioManager)
        questionRepo = InMemoryQuestionRepository(dataStore, scenarioManager)
        attachmentRepo = InMemoryAttachmentRepository(dataStore)
        catalog = StaticSuggestionCatalog()
        viewModel = QuestionViewModel(
            questionRepository = questionRepo,
            attachmentRepository = attachmentRepo,
            suggestionCatalog = catalog,
            savedStateHandle = SavedStateHandle(mapOf("consultationId" to "c-001"))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun closedQuestion_triggersSoftHintAndCanBeDismissed() = runTest(testDispatcher) {
        viewModel.openAddDialog()
        viewModel.setInputText("Apakah saya boleh minum kopi?")
        assertTrue(viewModel.uiState.value.showClosedQuestionHint)

        viewModel.dismissClosedQuestionHint()
        assertFalse(viewModel.uiState.value.showClosedQuestionHint)
    }

    @Test
    fun starterChip_prependsOpenStarterAndClearsHint() = runTest(testDispatcher) {
        viewModel.openAddDialog()
        viewModel.applyStarterChip("Bagaimana")
        assertEquals("Bagaimana ", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.showClosedQuestionHint)
    }

    @Test
    fun saveFourPriorityQuestionsWithAnswer_succeedsWithoutCap() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val initialCount = viewModel.uiState.value.questions.size

        for (i in 1..4) {
            viewModel.openAddDialog()
            viewModel.setInputText("Mengapa langkah ke-$i penting?")
            viewModel.setInputPriority(true)
            viewModel.setInputAnswer("Penjelasan tercatat #$i")
            viewModel.saveQuestion()
            testScheduler.advanceUntilIdle()
        }

        val state = viewModel.uiState.value
        assertEquals(initialCount + 4, state.questions.size)
        assertTrue(state.priorityCount >= 4)
        assertTrue(state.questions.any { it.answer == "Penjelasan tercatat #4" })
    }
}
