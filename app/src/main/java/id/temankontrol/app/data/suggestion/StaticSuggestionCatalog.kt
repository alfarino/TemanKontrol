package id.temankontrol.app.data.suggestion

import id.temankontrol.app.domain.model.QuestionSuggestion
import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.domain.repository.SuggestionCatalog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticSuggestionCatalog @Inject constructor() : SuggestionCatalog {

    private val suggestions: List<QuestionSuggestion> = listOf(
        // Gejala
        QuestionSuggestion("sug-g-1", SuggestionCategory.SYMPTOM, "Apa kemungkinan penyebab keluhan yang saya alami?"),
        QuestionSuggestion("sug-g-2", SuggestionCategory.SYMPTOM, "Keluhan apa yang perlu saya perhatikan atau segera laporkan?"),
        QuestionSuggestion("sug-g-3", SuggestionCategory.SYMPTOM, "Berapa lama biasanya keluhan ini membaik?"),
        QuestionSuggestion("sug-g-4", SuggestionCategory.SYMPTOM, "Apakah ada aktivitas yang sebaiknya saya hindari?"),

        // Pemeriksaan
        QuestionSuggestion("sug-p-1", SuggestionCategory.EXAM, "Pemeriksaan apa yang perlu saya jalani dan untuk apa?"),
        QuestionSuggestion("sug-p-2", SuggestionCategory.EXAM, "Apa yang perlu saya siapkan sebelum pemeriksaan?"),
        QuestionSuggestion("sug-p-3", SuggestionCategory.EXAM, "Kapan dan bagaimana saya mendapatkan hasilnya?"),
        QuestionSuggestion("sug-p-4", SuggestionCategory.EXAM, "Apa arti hasil pemeriksaan saya?"),

        // Perawatan
        QuestionSuggestion("sug-t-1", SuggestionCategory.TREATMENT, "Apa saja pilihan perawatan yang tersedia?"),
        QuestionSuggestion("sug-t-2", SuggestionCategory.TREATMENT, "Bagaimana cara menggunakan obat ini dengan benar?"),
        QuestionSuggestion("sug-t-3", SuggestionCategory.TREATMENT, "Efek samping apa yang perlu saya waspadai?"),
        QuestionSuggestion("sug-t-4", SuggestionCategory.TREATMENT, "Apa yang harus saya lakukan jika lupa minum obat?"),

        // Langkah Berikutnya
        QuestionSuggestion("sug-l-1", SuggestionCategory.NEXT_STEP, "Kapan saya perlu kontrol lagi?"),
        QuestionSuggestion("sug-l-2", SuggestionCategory.NEXT_STEP, "Siapa yang dapat saya hubungi jika ada pertanyaan?"),
        QuestionSuggestion("sug-l-3", SuggestionCategory.NEXT_STEP, "Tanda apa yang berarti saya harus segera kembali?"),
        QuestionSuggestion("sug-l-4", SuggestionCategory.NEXT_STEP, "Apa yang perlu saya bawa pada kunjungan berikutnya?")
    )

    override fun byCategory(category: SuggestionCategory): List<QuestionSuggestion> {
        return suggestions.filter { it.category == category }
    }
}
