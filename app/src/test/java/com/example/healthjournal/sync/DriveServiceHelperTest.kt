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

    @Test
    fun `deleteDataFile deletes existing file in appDataFolder`() = runBlocking {
        val fileId = "to_delete_id"
        val fileList = FileList().setFiles(listOf(File().setId(fileId)))
        val driveDelete: Drive.Files.Delete = mockk()

        every { driveFiles.list() } returns driveList
        every { driveList.setQ(any()) } returns driveList
        every { driveList.setSpaces(any()) } returns driveList
        every { driveList.setFields(any()) } returns driveList
        every { driveList.execute() } returns fileList
        every { driveFiles.delete(fileId) } returns driveDelete
        every { driveDelete.execute() } returns null

        val deleted = helper.deleteDataFile("health_journal_data_sib-1.json")

        assertEquals(true, deleted)
        verify { driveFiles.delete(fileId) }
    }

    @Test
    fun `perProfileFileNames keep Self on legacy names and suffix non-Self profiles`() {
        assertEquals(
            DriveServiceHelper.JOURNAL_DATA_FILE,
            DriveServiceHelper.journalFileName("self")
        )
        assertEquals(
            "health_journal_data_sib-1.json",
            DriveServiceHelper.journalFileName("sib-1")
        )
        assertEquals(
            DriveServiceHelper.MEASUREMENTS_DATA_FILE,
            DriveServiceHelper.measurementsFileName("self")
        )
        assertEquals(
            "body_measurements_sib-1.json",
            DriveServiceHelper.measurementsFileName("sib-1")
        )
        assertEquals(
            DriveServiceHelper.MEASUREMENTS_GOALS_FILE,
            DriveServiceHelper.goalsFileName("self")
        )
        assertEquals(
            "body_measurements_goals_sib-1.json",
            DriveServiceHelper.goalsFileName("sib-1")
        )
        assertEquals(
            DriveServiceHelper.MEASUREMENTS_TOMBSTONES_FILE,
            DriveServiceHelper.tombstonesFileName("self")
        )
        assertEquals(
            "body_measurements_tombstones_sib-1.json",
            DriveServiceHelper.tombstonesFileName("sib-1")
        )
        assertEquals(
            DriveServiceHelper.PERSONAL_CARD_FILE,
            DriveServiceHelper.personalCardFileName("self")
        )
        assertEquals(
            "personal_card_sib-1.json",
            DriveServiceHelper.personalCardFileName("sib-1")
        )
    }
}
