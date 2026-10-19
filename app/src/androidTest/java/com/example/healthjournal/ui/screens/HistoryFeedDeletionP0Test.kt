package com.example.healthjournal.ui.screens

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.healthjournal.R
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.pages.ArchivePage
import com.example.healthjournal.pages.HistoryFeedPage
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.RecordingHapticFeedback
import com.example.healthjournal.util.targetString
import com.example.healthjournal.viewmodel.JournalViewModel
import io.mockk.every
import io.mockk.mockk
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * P0 automation for the irreversible-deletion paths of the History feed and
 * Archive: selection + confirmed delete, empty archive, sandbox media cleanup,
 * and the guards that keep a deleted entry from being resurrected.
 *
 * Runs the real [JournalViewModel] over an in-memory Room database, so each
 * assertion checks the persisted effect — rows, tombstones and files — instead
 * of a mocked call log. Haptics are captured through a recording
 * LocalHapticFeedback because device vibration is not observable.
 */
@Feature("History Feed")
class HistoryFeedDeletionP0Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    private lateinit var app: Application
    private lateinit var db: JournalDatabase
    private lateinit var dao: JournalDao
    private lateinit var repository: JournalRepository
    private lateinit var viewModel: JournalViewModel
    private val haptics = RecordingHapticFeedback()

    private val archive by lazy { ArchivePage(composeTestRule) }
    private val history by lazy { HistoryFeedPage(composeTestRule) }

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, JournalDatabase::class.java).build()
        dao = db.journalDao()
        repository = JournalRepository(dao)
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
        File(app.filesDir, "p0-test").deleteRecursively()
        File(app.cacheDir, "p0-test").deleteRecursively()
    }

    // --- TC-34 -----------------------------------------------------------------

    @Test
    @Step("TC-34 Selection survives filtering and the confirmation counts hidden rows")
    fun testTC34_selectionSurvivesFilterAndCountsHiddenRows() {
        seedArchived(archivedEntry("Z1", "Alpha"), archivedEntry("Z2", "Bravo"), archivedEntry("Z3", "Charlie"))
        setArchiveContent()

        // Select Z1 by long press, then add Z2; the selection count reflects both.
        awaitEntryTag("Z1")
        archive.longPressEntry("Z1")
        archive.clickEntry("Z2")
        composeTestRule.onNodeWithText(archive.selectedCount(2)).assertExists()

        // Filter Z1's list down so Z2 is no longer visible: the selection persists.
        archive.search("Alpha")
        waitFor { composeTestRule.onAllNodesWithTag(archive.entryTag("Z2")).fetchSemanticsNodes().isEmpty() }
        composeTestRule.onNodeWithTag(archive.entryTag("Z1")).assertExists()
        composeTestRule.onNodeWithText(archive.selectedCount(2)).assertExists()

        // The confirmation counts the hidden selection, and deleting removes both.
        val hapticsBeforeDialog = haptics.events.size
        archive.tapDelete()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(2)).assertExists()
        archive.confirmDelete()

        waitFor { entryById("Z1") == null && entryById("Z2") == null }
        assertEquals(setOf("Z1", "Z2"), tombstoneIds())
        assertTrue("Z3 must survive a filtered delete", entryById("Z3") != null)

        // Confirmation clears the selection and fires no extra haptic.
        waitFor { composeTestRule.onAllNodesWithText(archive.selectedCount(2)).fetchSemanticsNodes().isEmpty() }
        composeTestRule.onNodeWithText(archive.title).assertExists()
        assertEquals(hapticsBeforeDialog, haptics.events.size)
    }

    // --- TC-37 -----------------------------------------------------------------

    @Test
    @Step("TC-37 Confirmed selection delete removes rows and writes a tombstone per row")
    fun testTC37_confirmedDeleteWritesTombstonesForEverySelectedRow() {
        seedArchived(archivedEntry("Z1", "Alpha"), archivedEntry("Z2", "Bravo"))
        setArchiveContent()

        awaitEntryTag("Z1")
        archive.longPressEntry("Z1")
        archive.clickEntry("Z2")
        archive.tapDelete()

        // The dialog states the exact contract before anything is removed.
        composeTestRule.onNodeWithText(targetString(R.string.archive_delete_title)).assertExists()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(2)).assertExists()
        composeTestRule.onNodeWithText(targetString(R.string.action_cancel)).assertExists()
        composeTestRule.onNodeWithText(targetString(R.string.common_delete)).assertExists()

        archive.confirmDelete()

        waitFor { entryById("Z1") == null && entryById("Z2") == null }
        assertEquals(setOf("Z1", "Z2"), tombstoneIds())
        composeTestRule.onNodeWithText(targetString(R.string.archive_snackbar_deleted)).assertExists()
        composeTestRule.onNodeWithText(archive.emptyState).assertExists()
    }

    // --- TC-38 -----------------------------------------------------------------

    @Test
    @Step("TC-38 Confirmed delete cleans sandbox media and ignores foreign URIs")
    fun testTC38_confirmedDeleteRemovesSandboxMediaBestEffort() {
        val photo = sandboxFile("p0-test", "tc38", "photo.jpg")
        val attachment = sandboxFile("p0-test", "tc38", "doc.txt")
        val foreign = File(app.cacheDir, "p0-test/tc38/foreign.txt").apply {
            parentFile?.mkdirs()
            writeText("keep me")
        }
        // Never created on disk: the deletion must stay best-effort on missing files.
        val preDeleted = File(app.filesDir, "p0-test/tc38/already-gone.jpg")

        seedArchived(
            archivedEntry(
                "Z1",
                "Alpha",
                photoUrls = listOf(
                    "file://${photo.absolutePath}",
                    "content://media/external/images/7",
                    "file://${preDeleted.absolutePath}"
                ),
                attachments = listOf(
                    AttachmentData(name = "doc.txt", uri = "file://${attachment.absolutePath}", mimeType = "text/plain")
                )
            )
        )
        setArchiveContent()

        awaitEntryTag("Z1")
        archive.longPressEntry("Z1")
        archive.tapDelete()
        archive.confirmDelete()

        waitFor { tombstoneIds().contains("Z1") }
        assertTrue("row must be removed", entryById("Z1") == null)
        assertTrue("sandbox photo must be deleted", !photo.exists())
        assertTrue("sandbox attachment must be deleted", !attachment.exists())
        assertTrue("a file outside filesDir must never be touched", foreign.exists())
    }

    // --- TC-39 -----------------------------------------------------------------

    @Test
    @Step("TC-39 Cancel and dismiss are pure: no row is removed and no tombstone is written")
    fun testTC39_cancelOrDismissLeavesArchiveUntouched() {
        seedArchived(archivedEntry("Z1", "Alpha"))
        setArchiveContent()

        // Cancel via the explicit button.
        awaitEntryTag("Z1")
        archive.longPressEntry("Z1")
        archive.tapDelete()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(1)).assertExists()
        archive.cancelDelete()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(1)).assertDoesNotExist()

        assertTrue("row must survive cancel", entryById("Z1") != null)
        assertTrue("cancel must not write tombstones", tombstoneIds().isEmpty())
        composeTestRule.onNodeWithText(targetString(R.string.archive_snackbar_deleted)).assertDoesNotExist()

        // Dismiss via system back.
        archive.tapDelete()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(1)).assertExists()
        Espresso.pressBack()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(1)).assertDoesNotExist()

        assertTrue("row must survive dismiss", entryById("Z1") != null)
        assertTrue("dismiss must not write tombstones", tombstoneIds().isEmpty())
    }

    // --- TC-40 -----------------------------------------------------------------

    @Test
    @Step("TC-40 Empty archive ignores the search filter and tombstones every row")
    fun testTC40_emptyAllDeletesRowsHiddenByTheFilter() {
        seedArchived(archivedEntry("Z1", "Alpha"), archivedEntry("Z2", "Bravo"), archivedEntry("Z3", "Charlie"))
        setArchiveContent()

        awaitEntryTag("Z1")
        archive.search("Alpha")
        waitFor { composeTestRule.onAllNodesWithTag(archive.entryTag("Z2")).fetchSemanticsNodes().isEmpty() }

        archive.tapEmptyAll()
        composeTestRule.onNodeWithText(targetString(R.string.archive_empty_title)).assertExists()
        composeTestRule.onNodeWithText(targetString(R.string.archive_empty_body)).assertExists()
        composeTestRule.onNodeWithText(targetString(R.string.archive_empty_confirm)).assertExists()
        composeTestRule.onNodeWithText(targetString(R.string.archive_keep_entries)).assertExists()

        archive.confirmEmpty()

        waitFor { tombstoneIds().size == 3 }
        assertEquals(setOf("Z1", "Z2", "Z3"), tombstoneIds())
        assertTrue("filtered-out rows must also be gone", entryById("Z3") == null)
        waitFor { composeTestRule.onAllNodesWithText(archive.emptyState).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithText(targetString(R.string.archive_snackbar_emptied)).assertExists()
    }

    // --- TC-41 -----------------------------------------------------------------

    @Test
    @Step("TC-41 Empty archive cleans sandbox media for every entry")
    fun testTC41_emptyAllRemovesSandboxMediaForEveryEntry() {
        val photo = sandboxFile("p0-test", "tc41", "photo.jpg")
        val foreign = File(app.cacheDir, "p0-test/tc41/foreign.txt").apply {
            parentFile?.mkdirs()
            writeText("keep me")
        }
        seedArchived(
            archivedEntry("Z1", "Alpha"),
            archivedEntry("Z2", "Bravo", photoUrls = listOf("file://${photo.absolutePath}")),
            archivedEntry("Z3", "Charlie", photoUrls = listOf("file://${foreign.absolutePath}"))
        )
        setArchiveContent()

        awaitEntryTag("Z1")
        archive.tapEmptyAll()
        archive.confirmEmpty()

        waitFor { tombstoneIds().size == 3 }
        assertTrue("sandbox photo must be cleaned", !photo.exists())
        assertTrue("a file outside filesDir must never be touched", foreign.exists())
        assertTrue("row must be removed", entryById("Z2") == null)
    }

    // --- TC-44 -----------------------------------------------------------------

    @Test
    @Step("TC-44 History exposes no delete path: swipe archives, undo restores, nothing else")
    fun testTC44_historyFeedOffersOnlySwipeArchiveAndUndo() {
        runBlocking {
            dao.insertEntry(JournalEntry(entry_id = "Z1", description = "Feed Alpha"))
            dao.insertEntry(JournalEntry(entry_id = "Z2", description = "Feed Bravo"))
        }
        setHistoryContent()

        // Swipe left archives: the row is flagged, never removed, and no tombstone appears.
        awaitText("Feed Alpha")
        history.swipeLeft("Feed Alpha")
        waitFor { entryById("Z1")?.isArchived == true }
        assertTrue("swipe must keep the row", entryById("Z1") != null)
        assertTrue("swipe must not tombstone", tombstoneIds().isEmpty())
        assertTrue("archiving must produce haptic feedback", haptics.events.contains(HapticFeedbackType.LongPress))
        composeTestRule.onNodeWithText(history.undoLabel).assertExists()

        // Swipe right springs back: no archive, no haptic.
        val hapticsAfterLeftSwipe = haptics.events.size
        history.swipeRight("Feed Bravo")
        composeTestRule.waitForIdle()
        assertEquals(false, entryById("Z2")?.isArchived)
        assertEquals(hapticsAfterLeftSwipe, haptics.events.size)

        // Long press unlocks nothing: no selection mode, no delete affordance.
        history.longPress("Feed Bravo")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(archive.deleteCd).assertDoesNotExist()
        composeTestRule.onNodeWithText(archive.selectedCount(1)).assertDoesNotExist()
        assertTrue("no gesture may tombstone from History", tombstoneIds().isEmpty())
    }

    // --- TC-45 -----------------------------------------------------------------

    @Test
    @Step("TC-45 Delete always goes through confirmation; no direct-delete control exists")
    fun testTC45_deleteRequiresConfirmationAndHasNoShortcut() {
        seedArchived(archivedEntry("Z1", "Alpha"))
        setArchiveContent()

        // Outside selection mode there is no per-entry delete affordance at all.
        awaitEntryTag("Z1")
        composeTestRule.onNodeWithContentDescription(archive.deleteCd).assertDoesNotExist()

        archive.longPressEntry("Z1")
        // Exactly one delete control, and it only opens the confirmation gate.
        assertEquals(
            1,
            composeTestRule.onAllNodesWithText(archive.selectedCount(1)).fetchSemanticsNodes().size
        )
        assertEquals(
            1,
            composeTestRule.onAllNodesWithContentDescription(archive.deleteCd).fetchSemanticsNodes().size
        )
        archive.tapDelete()
        composeTestRule.onNodeWithText(archive.deleteDialogBody(1)).assertExists()
        assertTrue("opening the dialog must not delete", entryById("Z1") != null)
        assertTrue("opening the dialog must not tombstone", tombstoneIds().isEmpty())

        archive.cancelDelete()
        assertTrue("cancelling must not delete", entryById("Z1") != null)
        assertTrue("cancelling must not tombstone", tombstoneIds().isEmpty())
    }

    // --- TC-46 -----------------------------------------------------------------

    @Test
    @Step("TC-46 A tombstoned entry cannot be resurrected by search, restore or re-archive")
    fun testTC46_tombstonedEntryCannotBeResurrected() {
        runBlocking {
            dao.insertEntry(archivedEntry("Z1", "Ghost"))
            dao.insertEntry(JournalEntry(entry_id = "Z2", description = "Survivor"))
            repository.deleteEntries(listOf("Z1"))
        }

        val showArchive = mutableStateOf(false)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                HealthJournalTheme {
                    if (showArchive.value) {
                        ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
                    } else {
                        HistoryScreen(viewModel = viewModel, onAddEntryClick = {}, onEntryClick = {})
                    }
                }
            }
        }

        // History, unfiltered: the survivor is there, the tombstoned entry is not.
        awaitText("Survivor")

        // History, searched by its exact title: filtered out, only the query text remains.
        viewModel.setSearchQuery("Ghost")
        waitFor("search applies") { viewModel.allEntries.value.isEmpty() }
        composeTestRule.onNodeWithText(history.emptyLabel).assertExists()
        composeTestRule.onNodeWithText("Survivor").assertDoesNotExist()
        assertEquals(
            1,
            composeTestRule.onAllNodesWithText("Ghost").fetchSemanticsNodes().size
        )
        composeTestRule.onNodeWithText(history.undoLabel).assertDoesNotExist()

        // Archive: not listed.
        composeTestRule.runOnUiThread { showArchive.value = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(archive.emptyState).assertExists()
        assertTrue(
            composeTestRule.onAllNodesWithTag(archive.entryTag("Z1")).fetchSemanticsNodes().isEmpty()
        )

        // Restore and re-archive are no-ops; the tombstone stays.
        viewModel.restoreEntry("Z1")
        viewModel.archiveEntry("Z1")
        composeTestRule.waitForIdle()
        runBlocking { kotlinx.coroutines.delay(300) }
        assertTrue("restore/re-archive must not recreate the row", entryById("Z1") == null)
        assertEquals(setOf("Z1"), tombstoneIds())
    }

    // --- helpers ---------------------------------------------------------------

    private fun seedArchived(vararg entries: JournalEntry) = runBlocking {
        entries.forEach { dao.insertEntry(it) }
    }

    private fun archivedEntry(
        id: String,
        description: String,
        photoUrls: List<String> = emptyList(),
        attachments: List<AttachmentData> = emptyList()
    ) = JournalEntry(
        entry_id = id,
        description = description,
        isArchived = true,
        photo_urls = photoUrls,
        attachments = attachments
    )

    private fun entryById(id: String): JournalEntry? = runBlocking { dao.getEntryById(id) }

    private fun tombstoneIds(): Set<String> =
        runBlocking { dao.getAllDeletedEntries().map { it.entry_id }.toSet() }

    private fun waitFor(message: String = "condition", condition: () -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 10_000, condition = condition)
    }

    private fun awaitEntryTag(id: String) = waitFor("archive row '$id' visible") {
        composeTestRule.onAllNodesWithTag(archive.entryTag(id)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun awaitText(text: String) = waitFor("text '$text' visible") {
        composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun setArchiveContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                HealthJournalTheme {
                    ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
                }
            }
        }
    }

    private fun setHistoryContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                HealthJournalTheme {
                    HistoryScreen(viewModel = viewModel, onAddEntryClick = {}, onEntryClick = {})
                }
            }
        }
    }

    private fun sandboxFile(vararg segments: String): File {
        val file = File(app.filesDir, segments.joinToString("/"))
        file.parentFile?.mkdirs()
        file.writeText("p0-test")
        return file
    }
}
