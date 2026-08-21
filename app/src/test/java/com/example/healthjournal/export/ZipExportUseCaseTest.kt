package com.example.healthjournal.export

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class ZipExportUseCaseTest {

    private val repository: JournalRepository = mockk()
    private val gson = Gson()

    @Test
    fun testZipExport_CreatesValidZipWithDataJson() = runBlocking {
        val tempDir = Files.createTempDirectory("zip_test").toFile()
        val exportsDir = File(tempDir, "exports")
        exportsDir.mkdirs()
        
val entries = listOf(
            JournalEntry(description = "Test Entry 1"),
            JournalEntry(description = "Test Entry 2")
        )

        coEvery { repository.getAllEntriesInDateRange(any(), any()) } returns entries

        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        val resultFile = useCase.execute(0L, Long.MAX_VALUE)

        assertTrue("Result file should exist", resultFile.exists())

        val zipFile = ZipFile(resultFile)
        val dataEntry = zipFile.getEntry("data.json")
        assertNotNull("ZIP should contain data.json", dataEntry)

        val content = zipFile.getInputStream(dataEntry).bufferedReader().use { it.readText() }
        assertTrue("data.json should contain entry descriptions", content.contains("Test Entry 1"))

        zipFile.close()
    }

    @Test
    fun testZipExport_FiltersByDateRange() = runBlocking {
        val tempDir = Files.createTempDirectory("zip_range_test").toFile()
        val exportsDir = File(tempDir, "exports")
        exportsDir.mkdirs()

        val startDate = 1000L
        val endDate = 2000L

        coEvery { repository.getAllEntriesInDateRange(startDate, endDate) } returns emptyList()

        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        val resultFile = useCase.execute(startDate, endDate)

        assertTrue("Result file should exist", resultFile.exists())

        val zipFile = ZipFile(resultFile)
        val content = zipFile.getInputStream(zipFile.getEntry("data.json")).bufferedReader().use { it.readText() }
        assertTrue("data.json should be an empty array for out-of-range entries", content.contains("[]"))

        zipFile.close()
    }

    @Test
    fun testZipExport_IncludesArchivedEntries() = runBlocking {
        val tempDir = Files.createTempDirectory("zip_archived_test").toFile()
        val exportsDir = File(tempDir, "exports")
        exportsDir.mkdirs()

        val archived = JournalEntry(entry_id = "archived_1", description = "Archived entry", isArchived = true)

        coEvery { repository.getAllEntriesInDateRange(any(), any()) } returns listOf(archived)

        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        val resultFile = useCase.execute(0L, Long.MAX_VALUE)

        val zipFile = ZipFile(resultFile)
        val content = zipFile.getInputStream(zipFile.getEntry("data.json")).bufferedReader().use { it.readText() }
        assertTrue("data.json should include archived entries", content.contains("Archived entry"))

        zipFile.close()
    }

    @Test
    fun testZipExport_CopiesMediaFiles() = runBlocking {
        val tempDir = Files.createTempDirectory("zip_media_test").toFile()
        val exportsDir = File(tempDir, "exports")
        val internalDir = File(tempDir, "internal")
        exportsDir.mkdirs()
        internalDir.mkdirs()

        val mediaFile = File(internalDir, "test_photo.jpg")
        mediaFile.writeText("fake photo content")

        val entries = listOf(
            JournalEntry(
                description = "Entry with photo",
                attachments = listOf(
                    com.example.healthjournal.data.local.AttachmentData(
                        name = "test_photo",
                        uri = "file://${mediaFile.absolutePath}",
                        mimeType = "image/jpeg"
                    )
                )
            )
        )

        coEvery { repository.getAllEntriesInDateRange(any(), any()) } returns entries

        // Mock android.net.Uri.parse() since it's not available in unit tests
        mockkStatic(Uri::class)
        mockkStatic(Log::class)
        mockkStatic(MimeTypeMap::class)
        val mockUri = mockk<Uri>()
        val mockMimeTypeMap = mockk<MimeTypeMap>()
        every { Uri.parse("file://${mediaFile.absolutePath}") } returns mockUri
        every { mockUri.scheme } returns "file"
        every { mockUri.path } returns mediaFile.absolutePath
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { MimeTypeMap.getSingleton() } returns mockMimeTypeMap
        every { mockMimeTypeMap.getExtensionFromMimeType("image/jpeg") } returns "jpg"

        try {
            val useCase = ZipExportUseCase(repository, exportsDir, gson)
            val resultFile = useCase.execute(0L, Long.MAX_VALUE)

            val zipFile = ZipFile(resultFile)
            val mediaEntry = zipFile.getEntry("media/test_photo.jpg")
            assertNotNull("ZIP should contain media file", mediaEntry)

            val content = zipFile.getInputStream(mediaEntry).bufferedReader().use { it.readText() }
            assertTrue("Media content should match", content == "fake photo content")

            zipFile.close()
        } finally {
            unmockkStatic(Uri::class)
            unmockkStatic(Log::class)
            unmockkStatic(MimeTypeMap::class)
        }
    }
}
