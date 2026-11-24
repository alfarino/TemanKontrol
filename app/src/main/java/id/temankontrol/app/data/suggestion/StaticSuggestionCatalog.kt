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
        QuestionSuggestion(
            "sug-g-1",
            SuggestionCategory.SYMPTOM,
            "Apa kemungkinan penyebab keluhan yang saya alami? Tolong jelaskan."
        ),
        QuestionSuggestion(
            "sug-g-2",
            SuggestionCategory.SYMPTOM,
            "Keluhan seperti apa yang perlu saya perhatikan, dan apa yang harus saya lakukan jika itu terjadi?"
        ),
        QuestionSuggestion(
            "sug-g-3",
            SuggestionCategory.SYMPTOM,
            "Bagaimana perkembangan keluhan ini biasanya, dan berapa lama saya perlu menunggu perbaikan?"
        ),
        QuestionSuggestion(
            "sug-g-4",
            SuggestionCategory.SYMPTOM,
            "Aktivitas atau kebiasaan apa saja yang sebaiknya saya sesuaikan selama masa ini, dan mengapa?"
        ),

        // Pemeriksaan
        QuestionSuggestion(
            "sug-p-1",
            SuggestionCategory.EXAM,
            "Pemeriksaan apa yang perlu saya jalani, dan apa tujuannya?"
        ),
        QuestionSuggestion(
            "sug-p-2",
            SuggestionCategory.EXAM,
            "Bagaimana saya harus mempersiapkan diri sebelum pemeriksaan?"
        ),
        QuestionSuggestion(
            "sug-p-3",
            SuggestionCategory.EXAM,
            "Bagaimana dan kapan saya akan mendapat hasilnya, dan siapa yang akan menjelaskannya?"
        ),
        QuestionSuggestion(
            "sug-p-4",
            SuggestionCategory.EXAM,
            "Tolong jelaskan arti hasil pemeriksaan saya dengan bahasa yang mudah saya pahami."
        ),

        // Perawatan
        QuestionSuggestion(
            "sug-t-1",
            SuggestionCategory.TREATMENT,
            "Apa saja pilihan perawatan yang tersedia, beserta manfaat dan risikonya masing-masing?"
        ),
        QuestionSuggestion(
            "sug-t-2",
            SuggestionCategory.TREATMENT,
            "Bagaimana cara yang benar menggunakan obat ini, dan apa yang terjadi jika saya salah menggunakannya?"
        ),
        QuestionSuggestion(
            "sug-t-3",
            SuggestionCategory.TREATMENT,
            "Efek samping apa yang mungkin muncul, dan bagaimana saya mengenalinya?"
        ),
        QuestionSuggestion(
            "sug-t-4",
            SuggestionCategory.TREATMENT,
            "Apa yang terjadi jika saya memilih untuk tidak menjalani perawatan ini?"
        ),

        // Langkah Berikutnya
        QuestionSuggestion(
            "sug-l-1",
            SuggestionCategory.NEXT_STEP,
            "Bagaimana rencana kontrol saya berikutnya, dan apa yang perlu dipantau sampai saat itu?"
        ),
        QuestionSuggestion(
            "sug-l-2",
            SuggestionCategory.NEXT_STEP,
            "Siapa yang bisa saya hubungi, dan bagaimana caranya, bila saya punya pertanyaan di rumah?"
        ),
        QuestionSuggestion(
            "sug-l-3",
            SuggestionCategory.NEXT_STEP,
            "Tanda-tanda apa yang berarti saya perlu segera kembali, dan ke mana saya harus pergi?"
        ),
        QuestionSuggestion(
            "sug-l-4",
            SuggestionCategory.NEXT_STEP,
            "Apa yang perlu saya siapkan atau bawa pada kunjungan berikutnya?"
        ),

        // Pemahaman (Ask Me 3 + Teach-back)
        QuestionSuggestion(
            "sug-u-1",
            SuggestionCategory.UNDERSTANDING,
            "Apa masalah utama saya saat ini, menurut tenaga kesehatan?"
        ),
        QuestionSuggestion(
            "sug-u-2",
            SuggestionCategory.UNDERSTANDING,
            "Apa yang perlu saya lakukan berikutnya, langkah demi langkah?"
        ),
        QuestionSuggestion(
            "sug-u-3",
            SuggestionCategory.UNDERSTANDING,
            "Mengapa langkah tersebut penting untuk saya?"
        ),
        QuestionSuggestion(
            "sug-u-4",
            SuggestionCategory.UNDERSTANDING,
            "Bagaimana penjelasan ini bisa saya sampaikan kepada keluarga di rumah?"
        )
    )

    override fun byCategory(category: SuggestionCategory): List<QuestionSuggestion> {
        return suggestions.filter { it.category == category }
    }
}
