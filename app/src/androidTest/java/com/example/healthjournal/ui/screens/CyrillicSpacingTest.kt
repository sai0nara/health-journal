package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.util.FakeJournalViewModel
import com.example.healthjournal.util.HtmlParser
import com.example.healthjournal.ui.theme.HealthJournalTheme
import io.qameta.allure.kotlin.Feature
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests for word-spacing preservation in saved entries.
 * Captures the exact HTML persisted by the rich text editor so both
 * Latin and non-Latin (Cyrillic) input can be compared.
 */
@Feature("Add Entry")
class CyrillicSpacingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun typeAndSave(text: String): String {
        val viewModel = FakeJournalViewModel()
        var backCalled = false
        composeTestRule.setContent {
            HealthJournalTheme {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How are you feeling today?")
            .performTextInput(text)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Save Entry").performClick()
        composeTestRule.waitUntil(5000) { backCalled }
        return viewModel.addEntryCalledWith?.description ?: ""
    }

    @Test
    fun latinInput_preservesSpacesInSavedHtml() {
        val html = typeAndSave("hello world")
        assertTrue("Saved HTML was: $html", html.contains("hello world"))
        assertTrue(
            "Rendered text was: ${HtmlParser.parse(html).text}",
            HtmlParser.parse(html).text.contains("hello world")
        )
    }

    @Test
    fun cyrillicInput_preservesSpacesInSavedHtml() {
        val html = typeAndSave("привет мир")
        // Diagnostic evidence: compare both decode paths
        val viaAppParser = HtmlParser.parse(html).text
        val viaLibrary = com.mohamedrejeb.richeditor.model.RichTextState()
            .apply { setHtml(html) }
            .annotatedString.text
        println("CYR_TEST savedHtml=[$html]")
        println("CYR_TEST appParser=[$viaAppParser]")
        println("CYR_TEST libraryDecode=[$viaLibrary]")
        assertTrue(
            "Library decode lost spacing. html=[$html] appParser=[$viaAppParser] library=[$viaLibrary]",
            viaLibrary.contains("привет") && viaLibrary.contains("мир") && viaLibrary.contains(" ")
        )
    }
}
