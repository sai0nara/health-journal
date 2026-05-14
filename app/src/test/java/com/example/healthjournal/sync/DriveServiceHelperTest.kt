package com.example.healthjournal.sync

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class DriveServiceHelperTest {

    private val drive: Drive = mockk()
    private val driveFiles: Drive.Files = mockk()
    private val driveList: Drive.Files.List = mockk()
    private val driveCreate: Drive.Files.Create = mockk()
    private val driveUpdate: Drive.Files.Update = mockk()
    private val driveGet: Drive.Files.Get = mockk()
    private lateinit var helper: DriveServiceHelper

    @Before
    fun setup() {
        every { drive.files() } returns driveFiles
        helper = DriveServiceHelper(drive)
    }

    @Test
    fun `uploadJournalData creates new file in appDataFolder if not exists`() = runBlocking {
        val content = "test content"
        val fileList = FileList().setFiles(emptyList())

        every { driveFiles.list() } returns driveList
        every { driveList.setQ(any()) } returns driveList
        every { driveList.setSpaces(any()) } returns driveList
        every { driveList.setFields(any()) } returns driveList
        every { driveList.execute() } returns fileList

        val createdFile = File().setId("new_id")
        every { driveFiles.create(any(), any()) } returns driveCreate
        every { driveCreate.execute() } returns createdFile

        val result = helper.uploadJournalData(content)

        assertEquals("new_id", result)
        verify {
            driveList.setSpaces("appDataFolder") // Failing expectation: current code uses "drive"
            driveFiles.create(match { 
                it.parents?.contains("appDataFolder") == true 
            }, any())
        }
    }

    @Test
    fun `downloadJournalData returns content from appDataFolder`() = runBlocking {
        val fileId = "existing_id"
        val content = "downloaded content"
        val fileList = FileList().setFiles(listOf(File().setId(fileId)))

        every { driveFiles.list() } returns driveList
        every { driveList.setQ(any()) } returns driveList
        every { driveList.setSpaces(any()) } returns driveList
        every { driveList.setFields(any()) } returns driveList
        every { driveList.execute() } returns fileList

        every { driveFiles.get(fileId) } returns driveGet
        every { driveGet.executeMediaAsInputStream() } returns ByteArrayInputStream(content.toByteArray())

        val result = helper.downloadJournalData()

        assertEquals(content, result)
        verify {
            driveList.setSpaces("appDataFolder") // Failing expectation
        }
    }
}
