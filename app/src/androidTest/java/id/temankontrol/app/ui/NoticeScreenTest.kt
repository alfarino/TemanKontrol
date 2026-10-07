package id.temankontrol.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.temankontrol.app.feature.notice.NoticeContent
import id.temankontrol.app.ui.theme.TemanKontrolTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoticeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noticeScreen_rendersContentAndHandlesButtonClick() {
        var clicked = false

        composeTestRule.setContent {
            TemanKontrolTheme {
                NoticeContent(
                    onUnderstandClicked = { clicked = true }
                )
            }
        }

        // Verify title exists
        composeTestRule.onNodeWithText("Sebelum mulai").assertExists()

        // Verify button exists and click it
        composeTestRule.onNodeWithText("Saya Mengerti").performClick()

        assertTrue(clicked)
    }
}