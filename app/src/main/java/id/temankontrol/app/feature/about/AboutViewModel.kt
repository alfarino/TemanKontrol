package id.temankontrol.app.feature.about

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.BuildConfig
import id.temankontrol.app.data.mock.InMemoryDataStore
import id.temankontrol.app.data.mock.MockScenario
import id.temankontrol.app.data.mock.MockScenarioManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class AboutUiState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val isDebug: Boolean = BuildConfig.DEBUG,
    val activeScenario: MockScenario = MockScenario.NORMAL
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val scenarioManager: MockScenarioManager,
    private val dataStore: InMemoryDataStore
) : ViewModel() {

    val currentScenario: StateFlow<MockScenario> = scenarioManager.currentScenario

    fun selectScenario(scenario: MockScenario) {
        dataStore.loadScenario(scenario)
    }
}
