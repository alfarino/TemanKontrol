package id.temankontrol.app.data.suggestion

import id.temankontrol.app.domain.model.SuggestionCategory
import id.temankontrol.app.domain.validation.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticSuggestionCatalogTest {

    private val catalog = StaticSuggestionCatalog()

    @Test
    fun tc025_allCategoriesHaveOpenEndedQuestionsOnly() {
        val forbiddenTerms = listOf(
            "diagnosis", "resep", "rekam medis", "catatan dokter",
            "jawaban dokter", "rekomendasi pengobatan", "darurat", "dosis"
        )

        SuggestionCategory.entries.forEach { category ->
            val items = catalog.byCategory(category)
            assertTrue("Category $category must have 4-6 items", items.size in 4..6)

            items.forEach { suggestion ->
                assertFalse(
                    "Template '${suggestion.text}' must not look like a closed yes/no question",
                    Validation.isLikelyClosedQuestion(suggestion.text)
                )
                assertTrue(
                    "Template '${suggestion.text}' must be <= ${Validation.MAX_QUESTION_LENGTH} chars",
                    suggestion.text.length <= Validation.MAX_QUESTION_LENGTH
                )
                val lower = suggestion.text.lowercase()
                forbiddenTerms.forEach { term ->
                    assertFalse("Template contains forbidden term '$term': ${suggestion.text}", lower.contains(term))
                }
            }
        }
        assertEquals(5, SuggestionCategory.entries.size)
    }
}
