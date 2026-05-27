package com.example.healthjournal.sync

import android.content.Context
import android.util.Log
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class DriveServiceHelperTest {

    private val context: Context = mockk()
    private val drive: Drive = mockk()
    private val driveFiles: Drive.Files = mockk()
    private val driveList: Drive.Files.List = mockk()
    private val driveCreate: Drive.Files.Create = mockk()
    private val driveUpdate: Drive.Files.Update = mockk()
    private val driveGet: Drive.Files.Get = mockk()
    private lateinit var helper: DriveServiceHelper

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { drive.files() } returns driveFiles
        helper = DriveServiceHelper(context, drive)
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
            driveList.setSpaces("appDataFolder")
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
            driveList.setSpaces("appDataFolder")
        }
    }
}
