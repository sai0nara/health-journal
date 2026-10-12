package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.domain.HashtagParser
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.HtmlEntities
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HashtagEditorHtmlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editorPlainText_preservesHashForParser() {
        var captured: RichTextState? = null
        composeTestRule.setContent {
            HealthJournalTheme {
                val state = rememberRichTextState()
                captured = state
                RichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.onNodeWithText("", substring = true).performTextInput("#hashtag morning")
        composeTestRule.waitForIdle()

        var plain = ""
        composeTestRule.runOnUiThread {
            plain = captured!!.annotatedString.text
        }

        assertTrue("editor plain text must keep a literal hash, got: $plain", plain.contains("#"))
        assertEquals(setOf("hashtag"), HashtagParser.extractHashtags(plain))
    }

    @Test
    fun editorHtml_encodesHashAsEntity() {
        var captured: RichTextState? = null
        composeTestRule.setContent {
            HealthJournalTheme {
                val state = rememberRichTextState()
                captured = state
                RichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.onNodeWithText("", substring = true).performTextInput("#hashtag morning")
        composeTestRule.waitForIdle()

        var html = ""
        composeTestRule.runOnUiThread {
            html = HtmlEntities.decodeNonAsciiNamedEntities(captured!!.toHtml())
        }

        // Documents why parsing must use plain text, never HTML.
        assertTrue(
            "editor HTML encodes '#' as '&num;', got: $html",
            html.contains("&num;hashtag")
        )
        assertTrue(HashtagParser.extractHashtags(html).isEmpty())
    }
}
