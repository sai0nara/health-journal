package com.example.healthjournal.export

import android.content.Context
import com.example.healthjournal.R
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfExportUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var repository: JournalRepository
    private lateinit var imageResizer: ImageResizer
    private lateinit var exportsDir: File
    private lateinit var useCase: PdfExportUseCase

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        imageResizer = mockk(relaxed = true)
        exportsDir = tempFolder.newFolder("exports")

        every { context.getString(R.string.pdf_report_title) } returns "Health Journal Report"
        every { context.getString(R.string.export_pdf_profile_section, any()) } answers {
            val arg = secondArg<Array<Any?>>().firstOrNull() ?: ""
            "Profile: $arg"
        }
        every { context.getString(R.string.export_pdf_no_entries) } returns "No entries in selected period."
        every { repository.currentProfileId() } returns SELF_PROFILE_ID

        useCase = PdfExportUseCase(
            context = context,
            repository = repository,
            exportsDir = exportsDir,
            imageResizer = imageResizer
        )
    }

    @Test
    fun execute_defaultsToActiveProfileOnly() = runTest {
        every { repository.currentProfileId() } returns "profile-a"
        coEvery {
            repository.getAllEntriesInDateRange("profile-a", 1000L, 5000L)
        } returns listOf(
            JournalEntry(
                entry_id = "e-a",
                profileId = "profile-a",
                timestamp = 2000L,
                description = "Alex headache note"
            )
        )
        coEvery {
            repository.getAllEntriesInDateRange("profile-b", any(), any())
        } returns listOf(
            JournalEntry(
                entry_id = "e-b",
                profileId = "profile-b",
                timestamp = 2000L,
                description = "Sam fever note"
            )
        )

        val pdfFile = useCase.execute(startDate = 1000L, endDate = 5000L)
        val text = extractPdfText(pdfFile)

        coVerify(exactly = 1) { repository.getAllEntriesInDateRange("profile-a", 1000L, 5000L) }
        coVerify(exactly = 0) { repository.getAllEntriesInDateRange("profile-b", any(), any()) }
        assertTrue("Expected Alex entry in PDF text: $text", text.contains("Alex headache note"))
        assertFalse("Unexpected Sam entry in PDF text: $text", text.contains("Sam fever note"))
    }

    @Test
    fun execute_multipleSelectedProfiles_producesTwoLabeledSectionsInOrder() = runTest {
        coEvery {
            repository.getAllEntriesInDateRange("profile-a", 1000L, 5000L)
        } returns listOf(
            JournalEntry(
                entry_id = "e-a",
                profileId = "profile-a",
                timestamp = 2500L,
                description = "Alex morning checkup"
            )
        )
        coEvery {
            repository.getAllEntriesInDateRange("profile-b", 1000L, 5000L)
        } returns listOf(
            JournalEntry(
                entry_id = "e-b",
                profileId = "profile-b",
                timestamp = 3000L,
                description = "Sam evening checkup"
            )
        )

        val pdfFile = useCase.execute(
            profileIds = listOf("profile-a", "profile-b"),
            startDate = 1000L,
            endDate = 5000L,
            profileNames = mapOf("profile-a" to "Alex", "profile-b" to "Sam")
        )
        val text = extractPdfText(pdfFile)

        val alexHeaderIndex = text.indexOf("Profile: Alex")
        val alexEntryIndex = text.indexOf("Alex morning checkup")
        val samHeaderIndex = text.indexOf("Profile: Sam")
        val samEntryIndex = text.indexOf("Sam evening checkup")

        assertTrue("Missing 'Profile: Alex' header in: $text", alexHeaderIndex >= 0)
        assertTrue("Missing Alex entry in: $text", alexEntryIndex > alexHeaderIndex)
        assertTrue("Missing 'Profile: Sam' header after Alex in: $text", samHeaderIndex > alexEntryIndex)
        assertTrue("Missing Sam entry after Sam header in: $text", samEntryIndex > samHeaderIndex)
    }

    @Test
    fun execute_excludesArchivedJournalEntriesFromPdf() = runTest {
        coEvery {
            repository.getAllEntriesInDateRange(SELF_PROFILE_ID, 1000L, 5000L)
        } returns listOf(
            JournalEntry(
                entry_id = "active-1",
                profileId = SELF_PROFILE_ID,
                timestamp = 2000L,
                description = "Visible active entry",
                isArchived = false
            ),
            JournalEntry(
                entry_id = "archived-1",
                profileId = SELF_PROFILE_ID,
                timestamp = 3000L,
                description = "Hidden archived entry",
                isArchived = true
            )
        )

        val pdfFile = useCase.execute(
            profileIds = listOf(SELF_PROFILE_ID),
            startDate = 1000L,
            endDate = 5000L,
            profileNames = mapOf(SELF_PROFILE_ID to "Me")
        )
        val text = extractPdfText(pdfFile)

        assertTrue(text.contains("Visible active entry"))
        assertFalse(text.contains("Hidden archived entry"))
    }

    private fun extractPdfText(file: File): String {
        PdfDocument(PdfReader(file)).use { pdfDoc ->
            return (1..pdfDoc.numberOfPages).joinToString("\n") { page ->
                PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page))
            }
        }
    }
}
