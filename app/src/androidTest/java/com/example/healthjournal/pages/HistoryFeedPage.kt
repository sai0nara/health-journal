package com.example.healthjournal.pages

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.healthjournal.R
import com.example.healthjournal.util.targetString

/**
 * Page object for the History feed rows: the row-level gestures (swipe to
 * archive, spring back, long press) and the undo affordance. Rows are located
 * by their visible description text, mirroring what the user sees.
 */
class HistoryFeedPage(private val rule: ComposeTestRule) {

    val undoLabel = targetString(R.string.history_undo)
    val emptyLabel = targetString(R.string.history_empty)

    fun swipeLeft(entryText: String) =
        rule.onNodeWithText(entryText).performTouchInput { swipeLeft() }

    fun swipeRight(entryText: String) =
        rule.onNodeWithText(entryText).performTouchInput { swipeRight() }

    fun longPress(entryText: String) =
        rule.onNodeWithText(entryText).performTouchInput { longClick() }
}
