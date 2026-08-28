package com.example.healthjournal.export

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoreRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private data class Harness(
        val repo: RestoreRepository,
        val db: JournalDatabase,
        val fileName: String,
        val published: MutableList<List<JournalEntry>> = mutableListOf()
    )

    private fun mockRepository(filesDir: File, recordInsertAll: Boolean = false): Harness {
        val db = mockk<JournalDatabase>()
        val journalDao = mockk<JournalDao>()
        val bodyDao = mockk<BodyMeasurementDao>()
        val goalDao = mockk<GoalDao>()
        val personalCardDao = mockk<PersonalCardDao>()
        coEvery { db.journalDao() } returns journalDao
        coEvery { db.bodyMeasurementDao() } returns bodyDao
        coEvery { db.goalDao() } returns goalDao
        coEvery { db.personalCardDao() } returns personalCardDao

        coJustRun { journalDao.clearAllEntries() }
        coJustRun { journalDao.clearAllDeletedEntries() }
        coJustRun { journalDao.clearAllTags() }
        coJustRun { journalDao.insertAllDeletedEntries(any()) }
        coJustRun { journalDao.insertAllTags(any()) }
        coJustRun { bodyDao.clearAll() }
        coJustRun { bodyDao.replaceAll(any()) }
        coJustRun { goalDao.clear() }
        coJustRun { goalDao.importAll(any()) }
        coJustRun { personalCardDao.clearAll() }
        coJustRun { personalCardDao.insertOrUpdate(any()) }

        val published = mutableListOf<List<JournalEntry>>()
        if (recordInsertAll) {
            coEvery { journalDao.insertAll(any()) } answers {
                published.add(firstArg())
            }
        } else {
            coJustRun { journalDao.insertAll(any()) }
        }

        val repo = RestoreRepository(db, filesDir, runInTransaction = { block -> block() })
        return Harness(repo, db, fileName = "restore", published = published)
    }

    private val sampleData = BackupData(
        journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x")),
        bodyMeasurements = listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)),
        goals = listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)),
        personalCards = listOf(PersonalCard(id = "personal_card")),
        deletedEntries = listOf(DeletedEntry(entry_id = "eDel")),
        entryTags = listOf(EntryTagCrossRef("e1", "health"))
    )

    @Test
    fun restore_wipesAndInsertsAllEntitiesInTransaction() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val harness = mockRepository(filesDir)

        val result = harness.repo.restore(sampleData, mediaStagingDir = null)

        val journalDao = harness.db.journalDao()
        val bodyDao = harness.db.bodyMeasurementDao()
        val goalDao = harness.db.goalDao()
        val pcDao = harness.db.personalCardDao()

        // Wipe
        coVerify { journalDao.clearAllEntries() }
        coVerify { journalDao.clearAllDeletedEntries() }
        coVerify { journalDao.clearAllTags() }
        coVerify { bodyDao.clearAll() }
        coVerify { goalDao.clear() }
        coVerify { pcDao.clearAll() }
        // Insert
        coVerify { journalDao.insertAll(listOf(sampleData.journalEntries[0])) }
        coVerify { journalDao.insertAllDeletedEntries(listOf(sampleData.deletedEntries[0])) }
        coVerify { journalDao.insertAllTags(listOf(sampleData.entryTags[0])) }
        coVerify { bodyDao.replaceAll(listOf(sampleData.bodyMeasurements[0])) }
        coVerify { goalDao.importAll(listOf(sampleData.goals[0])) }
        coVerify { pcDao.insertOrUpdate(sampleData.personalCards[0]) }

        assertEquals(1, result.journalEntryCount)
        assertEquals(1, result.bodyMeasurementCount)
        assertEquals(1, result.goalCount)
        assertEquals(1, result.deletedEntryCount)
        assertEquals(1, result.tagCount)
        assertEquals(0, result.mediaFileCount)
        assertEquals(5, result.totalRecords)
    }

    @Test
    fun restore_reimportsMedia_andRemapsPhotoAndAttachmentUris() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val staging = tempFolder.newFolder("stage")
        File(staging, "photo_a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(staging, "doc_1").writeBytes(byteArrayOf(9))

        val entry = JournalEntry(
            entry_id = "e1",
            description = "media",
            photo_urls = listOf("file:///data/0/pkg/files/photos/photo_a.jpg"),
            attachments = listOf(
                AttachmentData(name = "doc_1", uri = "file:///data/0/pkg/files/attachments/doc_1", mimeType = "text/plain")
            )
        )
        val data = BackupData(journalEntries = listOf(entry))

        val harness = mockRepository(filesDir, recordInsertAll = true)
        val result = harness.repo.restore(data, mediaStagingDir = staging)

        // Media copied into photos/ and attachments/
        val copiedPhoto = File(filesDir, "photos/photo_a.jpg")
        val copiedDoc = File(filesDir, "attachments/doc_1")
        assertTrue(copiedPhoto.exists())
        assertTrue(copiedDoc.exists())
        assertEquals(2, result.mediaFileCount)

        // URIs remapped to this device (captured via the publishing insertAll stub)
        val remapped = harness.published.single().single()
        assertEquals(listOf("file://${copiedPhoto.absolutePath}"), remapped.photo_urls)
        assertEquals("file://${copiedDoc.absolutePath}", remapped.attachments!![0].uri)
    }

    @Test
    fun restore_noStagingDir_skipsMediaImport() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val harness = mockRepository(filesDir)

        val result = harness.repo.restore(sampleData, mediaStagingDir = null)

        assertEquals(0, result.mediaFileCount)
    }

    @Test
    fun restore_transactionFailure_propagatesErrorAndLeavesDbToRollback() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val db = mockk<JournalDatabase>()
        val journalDao = mockk<JournalDao>()
        coEvery { db.journalDao() } returns journalDao
        coJustRun { journalDao.clearAllEntries() }

        val repo = RestoreRepository(db, filesDir, runInTransaction = { _ ->
            journalDao.clearAllEntries()
            throw IOException("boom in transaction")
        })

        try {
            repo.restore(sampleData, mediaStagingDir = null)
            fail("Expected exception from transaction")
        } catch (e: IOException) {
            assertEquals("boom in transaction", e.message)
        }
    }
}
