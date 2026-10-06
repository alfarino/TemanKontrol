package id.temankontrol.app.data.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class MockScenario(val displayName: String) {
    NORMAL("Normal"),
    EMPTY("Kosong"),
    ERROR("Error"),
    SLOW("Lambat (1500 ms)")
}

@Singleton
class MockScenarioManager @Inject constructor() {
    private val _currentScenario = MutableStateFlow(MockScenario.NORMAL)
    val currentScenario: StateFlow<MockScenario> = _currentScenario.asStateFlow()

    fun setScenario(scenario: MockScenario) {
        _currentScenario.value = scenario
    }
}
