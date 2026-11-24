package id.temankontrol.app.feature.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.temankontrol.app.core.designsystem.component.DateUtils
import id.temankontrol.app.domain.model.Consultation
import id.temankontrol.app.domain.model.ConsultationId
import id.temankontrol.app.domain.model.ConsultationNote
import id.temankontrol.app.domain.model.FollowUpAction
import id.temankontrol.app.domain.model.Question
import id.temankontrol.app.domain.repository.ConsultationRepository
import id.temankontrol.app.domain.repository.FollowUpRepository
import id.temankontrol.app.domain.repository.NoteRepository
import id.temankontrol.app.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SummaryPreviewUiState(
    val consultation: Consultation? = null,
    val questions: List<Question> = emptyList(),
    val note: ConsultationNote? = null,
    val followUps: List<FollowUpAction> = emptyList(),
    val includeProvider: Boolean = true,
    val includeQuestions: Boolean = true,
    val includeNotes: Boolean = true,
    val includeFollowUps: Boolean = true,
    val isLoading: Boolean = true
) {
    fun buildPlainTextSummary(disclaimer: String): String {
        val c = consultation ?: return ""
        return buildString {
            appendLine("=== RINGKASAN KONSULTASI ===")
            appendLine(c.title)
            appendLine("Tanggal: ${DateUtils.formatRelativeDateTime(c.appointmentAt)}")
            if (!c.facilityName.isNullOrBlank()) appendLine("Fasilitas: ${c.facilityName}")
            if (includeProvider && !c.providerName.isNullOrBlank()) {
                appendLine("Tenaga Kesehatan: ${c.providerName}")
            }
            appendLine("Alasan: ${c.reason}")
            appendLine()

            if (includeQuestions && questions.isNotEmpty()) {
                appendLine("--- Pertanyaan & Jawaban ---")
                questions.forEachIndexed { idx, q ->
                    val mark = if (q.isDiscussed) "[✓]" else "[ ]"
                    appendLine("${idx + 1}. $mark ${q.text}")
                    if (!q.answer.isNullOrBlank()) {
                        appendLine("   Jawaban dicatat: ${q.answer}")
                    }
                }
                appendLine()
            }

            if (includeNotes && note != null && note.isNotEmpty()) {
                appendLine("--- Hasil Konsultasi (Catatan Pengguna) ---")
                if (!note.summary.isNullOrBlank()) appendLine("Ringkasan: ${note.summary}")
                if (!note.providerInstruction.isNullOrBlank()) appendLine("Arahan: ${note.providerInstruction}")
                if (!note.additionalNotes.isNullOrBlank()) appendLine("Catatan Tambahan: ${note.additionalNotes}")
                appendLine()
            }

            if (includeFollowUps && followUps.isNotEmpty()) {
                appendLine("--- Tindak Lanjut ---")
                followUps.forEach { f ->
                    val status = if (f.isCompleted) "[Selesai]" else "[Terbuka]"
                    val due = f.dueDate?.let { " (Tenggat: ${DateUtils.formatLocalDate(it)})" } ?: ""
                    appendLine("- $status ${f.text}$due")
                }
                appendLine()
            }

            appendLine(disclaimer)
        }.trim()
    }
}

@HiltViewModel
class SummaryPreviewViewModel @Inject constructor(
    consultationRepository: ConsultationRepository,
    questionRepository: QuestionRepository,
    noteRepository: NoteRepository,
    followUpRepository: FollowUpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val consultationId = ConsultationId(checkNotNull(savedStateHandle["consultationId"]))

    private val _includeProvider = MutableStateFlow(true)
    private val _includeQuestions = MutableStateFlow(true)
    private val _includeNotes = MutableStateFlow(true)
    private val _includeFollowUps = MutableStateFlow(true)

    val uiState: StateFlow<SummaryPreviewUiState> = combine(
        consultationRepository.observe(consultationId),
        questionRepository.observeByConsultation(consultationId),
        noteRepository.observe(consultationId),
        followUpRepository.observeByConsultation(consultationId),
        combine(_includeProvider, _includeQuestions, _includeNotes, _includeFollowUps) { p, q, n, f ->
            listOf(p, q, n, f)
        }
    ) { consultation, questions, note, followUps, flags ->
        SummaryPreviewUiState(
            consultation = consultation,
            questions = questions,
            note = note,
            followUps = followUps,
            includeProvider = flags[0],
            includeQuestions = flags[1],
            includeNotes = flags[2],
            includeFollowUps = flags[3],
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryPreviewUiState(isLoading = true)
    )

    fun setIncludeProvider(v: Boolean) { _includeProvider.value = v }
    fun setIncludeQuestions(v: Boolean) { _includeQuestions.value = v }
    fun setIncludeNotes(v: Boolean) { _includeNotes.value = v }
    fun setIncludeFollowUps(v: Boolean) { _includeFollowUps.value = v }
}
