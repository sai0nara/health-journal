package com.example.healthjournal.pages

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.R
import com.example.healthjournal.util.targetString

/**
 * Page object for the Archive screen's deletion affordances. Exposes the
 * selector map (tests never see raw tags or string ids) and the actions a user
 * performs: select rows, search, tap the delete/empty controls, and answer the
 * confirmation gates.
 */
class ArchivePage(private val rule: ComposeTestRule) {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    val title = targetString(R.string.archive_title)
    val emptyState = targetString(R.string.archive_empty_state)
    val deleteCd = targetString(R.string.archive_cd_delete)
    val emptyAllCd = targetString(R.string.archive_cd_empty)
    val searchPlaceholder = targetString(R.string.archive_search_placeholder)

    fun entryTag(id: String) = "archive_entry_$id"

    fun entry(id: String) = rule.onNodeWithTag(entryTag(id))

    /**
     * Long press near the row's left edge (the text column) rather than the
     * node center: a row with photo thumbnails has no long-press target in its
     * middle, and the center would land on the photo strip instead.
     */
    fun longPressEntry(id: String) =
        entry(id).performTouchInput { longClick(percentOffset(0.05f, 0.5f)) }

    fun clickEntry(id: String) = entry(id).performClick()

    fun search(query: String) {
        rule.onNode(hasSetTextAction()).performTextInput(query)
    }

    fun tapDelete() = rule.onNodeWithContentDescription(deleteCd).performClick()

    fun tapEmptyAll() = rule.onNodeWithContentDescription(emptyAllCd).performClick()

    fun deleteDialogBody(count: Int) = context.getString(R.string.archive_delete_message, count)

    fun selectedCount(count: Int) = context.resources
        .getQuantityString(R.plurals.archive_selected_format, count, count)

    fun confirmDelete() = rule.onNodeWithText(targetString(R.string.common_delete)).performClick()

    fun cancelDelete() = rule.onNodeWithText(targetString(R.string.action_cancel)).performClick()

    fun confirmEmpty() = rule.onNodeWithText(targetString(R.string.archive_empty_confirm)).performClick()
}
