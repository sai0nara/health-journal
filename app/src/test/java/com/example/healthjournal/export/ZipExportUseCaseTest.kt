package com.example.healthjournal.export

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
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
        
        coEvery { repository.allEntries } returns flowOf(entries)
        
        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        val resultFile = useCase.execute()
        
        assertTrue("Result file should exist", resultFile.exists())
        
        val zipFile = ZipFile(resultFile)
        val dataEntry = zipFile.getEntry("data.json")
        assertNotNull("ZIP should contain data.json", dataEntry)
        
        val content = zipFile.getInputStream(dataEntry).bufferedReader().use { it.readText() }
        assertTrue("data.json should contain entry descriptions", content.contains("Test Entry 1"))
        
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
                        name = "Photo",
                        uri = "file://${mediaFile.absolutePath}",
                        mimeType = "image/jpeg"
                    )
                )
            )
        )
        
        coEvery { repository.allEntries } returns flowOf(entries)
        
        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        val resultFile = useCase.execute()
        
        val zipFile = ZipFile(resultFile)
        val mediaEntry = zipFile.getEntry("media/test_photo.jpg")
        assertNotNull("ZIP should contain media file", mediaEntry)
        
        val content = zipFile.getInputStream(mediaEntry).bufferedReader().use { it.readText() }
        assertTrue("Media content should match", content == "fake photo content")
        
        zipFile.close()
    }
}
