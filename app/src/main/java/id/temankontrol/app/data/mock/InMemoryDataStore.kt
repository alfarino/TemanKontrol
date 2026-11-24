package id.temankontrol.app.data.mock

import id.temankontrol.app.domain.model.Attachment
import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationNote
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryDataStore @Inject constructor(
    private val scenarioManager: MockScenarioManager
) {
    private val _consultations = MutableStateFlow<List<Consultation>>(emptyList())
    val consultations: StateFlow<List<Consultation>> = _consultations.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _notes = MutableStateFlow<Map<ConsultationId, ConsultationNote>>(emptyMap())
    val notes: StateFlow<Map<ConsultationId, ConsultationNote>> = _notes.asStateFlow()

    private val _followUps = MutableStateFlow<List<FollowUpAction>>(emptyList())
    val followUps: StateFlow<List<FollowUpAction>> = _followUps.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments.asStateFlow()

    private val _noticeAccepted = MutableStateFlow(false)
    val noticeAccepted: StateFlow<Boolean> = _noticeAccepted.asStateFlow()

    init {
        loadScenario(MockScenario.NORMAL)
    }

    fun loadScenario(scenario: MockScenario) {
        scenarioManager.setScenario(scenario)
        when (scenario) {
            MockScenario.NORMAL, MockScenario.SLOW -> {
                _consultations.value = SampleData.getSampleConsultations()
                _questions.value = SampleData.getSampleQuestions()
                _notes.value = SampleData.getSampleNotes().associateBy { it.consultationId }
                _followUps.value = SampleData.getSampleFollowUps()
                _attachments.value = SampleData.getSampleAttachments()
                _noticeAccepted.value = true
            }
            MockScenario.EMPTY -> {
                _consultations.value = emptyList()
                _questions.value = emptyList()
                _notes.value = emptyMap()
                _followUps.value = emptyList()
                _attachments.value = emptyList()
                _noticeAccepted.value = false
            }
            MockScenario.ERROR -> {
                // Keep empty to trigger empty or error
                _consultations.value = emptyList()
                _questions.value = emptyList()
                _notes.value = emptyMap()
                _followUps.value = emptyList()
                _attachments.value = emptyList()
                _noticeAccepted.value = true
            }
        }
    }

    fun startOfToday(): Instant {
        return LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }

    fun setConsultations(list: List<Consultation>) { _consultations.value = list }
    fun setQuestions(list: List<Question>) { _questions.value = list }
    fun setNotes(map: Map<ConsultationId, ConsultationNote>) { _notes.value = map }
    fun setFollowUps(list: List<FollowUpAction>) { _followUps.value = list }
    fun setAttachments(list: List<Attachment>) { _attachments.value = list }
    fun setNoticeAccepted(accepted: Boolean) { _noticeAccepted.value = accepted }
}
