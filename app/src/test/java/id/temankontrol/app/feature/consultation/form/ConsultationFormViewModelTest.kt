package id.temankontrol.app.feature.consultation.form

import androidx.lifecycle.SavedStateHandle
import id.temankontrol.app.data.mock.InMemoryConsultationRepository
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.MockScenarioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ConsultationFormViewModelTest {

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
    fun initialState_hasDefaultValues() {
        val viewModel = ConsultationFormViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.value

        assertNull(state.id)
        assertEquals("", state.title)
        assertEquals("", state.reason)
        assertFalse(state.isDirty)
        assertFalse(state.isSubmitting)
        assertEquals(LocalDate.now().plusDays(1), state.appointmentDate)
    }

    @Test
    fun submit_emptyTitle_showsTitleError_doesNotCallSuccess() {
        val viewModel = ConsultationFormViewModel(repository, SavedStateHandle())
        var successCalled = false
        var errorCalled = false

        viewModel.onReasonChange("Alasan valid")
        viewModel.submit(
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertNotNull(viewModel.uiState.value.titleError)
    }

    @Test
    fun submit_emptyReason_showsReasonError_doesNotCallSuccess() {
        val viewModel = ConsultationFormViewModel(repository, SavedStateHandle())
        var successCalled = false

        viewModel.onTitleChange("Judul valid")
        viewModel.submit(
            onSuccess = { successCalled = true },
            onError = { }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertNotNull(viewModel.uiState.value.reasonError)
    }

    @Test
    fun submit_validInput_createsConsultationAndCallsSuccess() {
        val viewModel = ConsultationFormViewModel(repository, SavedStateHandle())
        var createdId: String? = null

        viewModel.onTitleChange("Kontrol Hipertensi")
        viewModel.onReasonChange("Cek tensi dan ambil obat")
        viewModel.onFacilityChange("Puskesmas")
        viewModel.onProviderChange("dr. Siti")

        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.submit(
            onSuccess = { id -> createdId = id },
            onError = { fail("Submission should not fail") }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdId)
        assertFalse(viewModel.uiState.value.isDirty)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun onDateChange_pastDate_setsIsPastDateTrue() {
        val viewModel = ConsultationFormViewModel(repository, SavedStateHandle())
        val pastDate = LocalDate.now().minusDays(2)

        viewModel.onDateChange(pastDate)

        assertTrue(viewModel.uiState.value.isPastDate)
        assertEquals(pastDate, viewModel.uiState.value.appointmentDate)
    }
}