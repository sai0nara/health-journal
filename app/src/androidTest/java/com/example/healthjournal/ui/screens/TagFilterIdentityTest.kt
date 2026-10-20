package com.example.healthjournal.ui.screens

import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.JournalTag
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.domain.EntryKindTag
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.JournalViewModel
import io.mockk.every
import io.mockk.mockk
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Filtering by a preset tag chip must match the same concept written by a
 * hashtag, a system auto-tag or the preset itself. Runs the real
 * [JournalViewModel] over an in-memory Room database so the assertions cover
 * the stored tag values rather than a mocked call log.
 */
@Feature("Tag filtering")
class TagFilterIdentityTest {

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
    fun presetFitnessChip_matchesEntriesTaggedFromEverySource() {
        runBlocking {
            db.journalDao().insertEntry(
                JournalEntry(entry_id = "e_hashtag", description = "Run #Fitness", timestamp = 3000)
            )
            db.journalDao().insertEntry(
                JournalEntry(entry_id = "e_auto", description = "Workout logged", timestamp = 2000)
            )
            db.journalDao().insertEntry(
                JournalEntry(entry_id = "e_preset", description = "Gym session", timestamp = 1000)
            )
            repository.addTag("e_hashtag", "fitness")
            repository.addTag("e_auto", EntryKindTag.forWorkout())
            repository.addTag("e_preset", JournalTag.FITNESS.name)
        }

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {}, onEntryClick = {})
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Run #Fitness").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Auto-tagged entry not rendered before filtering",
            composeTestRule.onAllNodesWithText("Workout logged").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "Preset-tagged entry not rendered before filtering",
            composeTestRule.onAllNodesWithText("Gym session").fetchSemanticsNodes().isNotEmpty()
        )

        composeTestRule.onNode(hasText("Fitness") and isSelectable())
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Workout logged").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Gym session").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Run #Fitness").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
