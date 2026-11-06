package com.example.healthjournal.ui.screens

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.MainActivity
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.RoomProfileDataEraser
import com.example.healthjournal.data.SharedPreferencesActiveProfileStore
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = JournalDatabase.getDatabase(context)
        val profileRepository = ProfileRepository(
            profileDao = database.profileDao(),
            settings = SharedPreferencesActiveProfileStore(context),
            eraser = RoomProfileDataEraser(database)
        )
        runBlocking {
            database.profileDao().getAll().forEach { database.profileDao().delete(it.id) }
            profileRepository.ensureSelfProfile()
            val alex = profileRepository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
            val sam = profileRepository.create("Sam", ProfileRelationship.SIBLING, null).getOrThrow()
            profileRepository.archive(sam.id)
            profileRepository.switchTo(SELF_PROFILE_ID)
            // Keep reference to alex for assertions
            alexProfileId = alex.id
            samProfileId = sam.id
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("TEST_MODE", true)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    private var alexProfileId: String = ""
    private var samProfileId: String = ""

    private fun navigateToExportScreen() {
        composeTestRule.onNodeWithText("Export Data").performClick()
    }

    @Test
    fun testExportScreen_RendersCorrectly() {
        navigateToExportScreen()

        // Check if screen elements are present
        composeTestRule.onNodeWithText("Export Data").assertExists()
        composeTestRule.onNodeWithText("Choose the date range and format for your export.").assertExists()
        composeTestRule.onNodeWithText("PDF (Medical Report)").assertExists()
        composeTestRule.onNodeWithText("ZIP (Raw Data & Media)").assertExists()
        composeTestRule.onNodeWithText("Generate Export").assertExists()
    }

    @Test
    fun testPdfProfileSelector_defaultsToActive_excludesArchived_andHidesForZip() {
        navigateToExportScreen()

        // Active profile (Me) is selected by default; active sibling (Alex) is unselected; archived (Sam) is absent
        composeTestRule.onNodeWithTag("export_profile_$SELF_PROFILE_ID")
            .assertExists()
            .assertIsSelected()
        composeTestRule.onNodeWithTag("export_profile_$alexProfileId")
            .assertExists()
            .assertIsNotSelected()
        composeTestRule.onNodeWithTag("export_profile_$samProfileId")
            .assertDoesNotExist()

        // Selecting Alex adds it to the PDF selection (both Me and Alex selected)
        composeTestRule.onNodeWithTag("export_profile_$alexProfileId").performClick()
        composeTestRule.onNodeWithTag("export_profile_$SELF_PROFILE_ID").assertIsSelected()
        composeTestRule.onNodeWithTag("export_profile_$alexProfileId").assertIsSelected()

        // Switching to ZIP hides the PDF profile selector
        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithTag("export_profile_$SELF_PROFILE_ID").assertDoesNotExist()
        composeTestRule.onNodeWithTag("export_profile_$alexProfileId").assertDoesNotExist()
    }

    @Test
    fun testDateRangePicker_hiddenForZip_showsFullBackupNotice() {
        navigateToExportScreen()

        // Date range picker visible with PDF (default)
        composeTestRule.onNodeWithText("Start Date").assertExists()
        composeTestRule.onNodeWithText("End Date").assertExists()

        // Switch to ZIP - picker hidden, clear full-backup notice shown
        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithText("Start Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("End Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("ZIP exports your full backup", substring = true).assertExists()
    }

    @Test
    fun testEncryptToggle_onlyShownForZip() {
        navigateToExportScreen()

        // Encrypt option not present for PDF
        composeTestRule.onNodeWithText("Encrypt backup").assertDoesNotExist()

        // Switch to ZIP - encrypt toggle appears
        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithText("Encrypt backup").assertExists()

        // Passphrase field hidden until toggle is enabled
        composeTestRule.onNodeWithText("Enter passphrase").assertDoesNotExist()

        // Enable toggle - passphrase field appears
        composeTestRule.onNodeWithTag("encrypt_backup").performClick()
        composeTestRule.onNodeWithText("Enter passphrase").assertExists()
    }
}
