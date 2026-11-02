package com.example.healthjournal.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.JournalViewModel
import io.mockk.every
import io.mockk.mockk
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Edits an existing entry through the real [JournalViewModel] and database:
 * deselecting the Fitness chip must drop the stored tag on save, whichever
 * source (preset, hashtag, workout auto-tag) put it there.
 */
@Feature("Tag editing")
class TagEditRemovalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    private lateinit var app: Application
    private lateinit var db: JournalDatabase
    private lateinit var repository: JournalRepository
    private lateinit var viewModel: JournalViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, JournalDatabase::class.java).build()
        repository = JournalRepository(db.journalDao())
        val session = mockk<SessionManager>()
        every { session.getUserEmail() } returns null
        viewModel = JournalViewModel(
            application = app,
            repository = repository,
            authManager = mockk(relaxed = true),
            sessionManager = session,
            healthManager = mockk(relaxed = true),
            mediaService = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun editingEntry_unselectingFitnessChip_removesStoredTag() {
        runBlocking {
            db.journalDao().insertEntry(
                JournalEntry(entry_id = "e_edit", description = "Workout logged", timestamp = 1000)
            )
            repository.addTag("e_edit", "fitness")
            repository.addTag("e_edit", "recovery")
        }
        var wentBack = false

        composeTestRule.setContent {
            HealthJournalTheme {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { wentBack = true },
                    entryId = "e_edit"
                )
            }
        }

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Fitness") and isSelectable() and isSelected())
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("Fitness") and isSelectable())
            .performScrollTo()
            .performClick()
            .assertIsNotSelected()

        composeTestRule.onNodeWithText("Update Entry").performScrollTo().performClick()

        composeTestRule.waitUntil(10_000) {
            runBlocking { repository.getTagsForEntry("e_edit") } == listOf("recovery")
        }
        assertTrue("Screen should navigate back after save", wentBack)
        assertEquals(listOf("recovery"), runBlocking { repository.getTagsForEntry("e_edit") })
    }
}
