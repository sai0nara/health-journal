package com.example.healthjournal.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncDownloadTest {

    private lateinit var context: Context
    private lateinit var database: JournalDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
        
        // Mock the singleton database getter to return our in-memory DB
        mockkObject(JournalDatabase)
        every { JournalDatabase.getDatabase(any()) } returns database

        // Set up providers for DI
        SyncWorker.authManagerProvider = { 
            val mock = mockk<GoogleAuthManager>()
            coEvery { mock.getDriveAccessTokenSilent() } returns "mock_token"
            mock
        }
    }

    @After
    fun tearDown() {
        database.close()
        // Reset providers to default
        SyncWorker.authManagerProvider = { GoogleAuthManager(it) }
        SyncWorker.driveHelperProvider = { context, drive -> DriveServiceHelper(context, drive) }
        unmockkAll()
    }

    @Test
    fun testSyncWorker_MergesMultiplePhotosAndAttachments() = runBlocking {
        // 1. Prepare local data (empty)

        // 2. Prepare mock cloud data with multiple photos and attachments
        val cloudEntries = listOf(
            JournalEntry(
                description = "Multi-media Entry",
                timestamp = 3000,
                photo_urls = listOf("file:///remote/photo1.jpg", "file:///remote/photo2.jpg"),
                attachments = listOf(
                    AttachmentData("Report", "file:///remote/report.pdf", "application/pdf")
                ),
                bp_systolic = 120.0,
                bp_diastolic = 80.0
            )
        )
        val cloudJson = Gson().toJson(cloudEntries)

        // Mock DriveServiceHelper provider
        SyncWorker.driveHelperProvider = { context, drive ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns cloudJson
            coEvery { mock.uploadJournalData(any()) } returns "new_file_id"
            coEvery { mock.downloadDataFile(any()) } returns null
            coEvery { mock.uploadDataFile(any(), any()) } returns "measurements_file_id"
            coEvery { mock.findFileByName(any()) } returns "mock_cloud_id"
            coEvery { mock.downloadFile(any(), any()) } returns true
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // 3. Verify data in local database
        val localEntries = database.journalDao().getAllEntriesIncludingArchived().first()
        assertEquals(1, localEntries.size)
        val entry = localEntries[0]

        assertEquals(2, entry.photo_urls?.size)
        assertEquals(1, entry.attachments?.size)
        assertEquals(120.0, entry.bp_systolic!!, 0.1)
        assertEquals(80.0, entry.bp_diastolic!!, 0.1)

        // Verify URI re-mapping (should point to local filesDir)
        assertTrue(entry.photo_urls!![0].contains(context.filesDir.path))
        assertTrue(entry.attachments!![0].uri.contains(context.filesDir.path))
    }

    @Test
    fun testSyncWorker_PreservesCreationDateOnConflict() = runBlocking {
        // 1. Prepare local data: Created at 1000, Modified at 5000
        val entryId = "preserve_date_id"
        val localEntry = JournalEntry(
            entry_id = entryId,
            description = "Local Updated Content",
            timestamp = 1000, // Original creation
            lastModified = 5000 // Newer modification
        )
        database.journalDao().insertEntry(localEntry)

        // 2. Prepare mock cloud data: Created at 1000, Modified at 3000
        val cloudEntries = listOf(
            JournalEntry(
                entry_id = entryId,
                description = "Cloud Older Content",
                timestamp = 1000,
                lastModified = 3000 // Older modification
            )
        )
        val cloudJson = Gson().toJson(cloudEntries)

        // Mock DriveServiceHelper provider
        SyncWorker.driveHelperProvider = { _, _ ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns cloudJson
            coEvery { mock.uploadJournalData(any()) } returns "new_file_id"
            coEvery { mock.downloadDataFile(any()) } returns null
            coEvery { mock.uploadDataFile(any(), any()) } returns "measurements_file_id"
            coEvery { mock.findFileByName(any()) } returns "mock_cloud_id"
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // 3. Verify that local content was PRESERVED and date is still 1000
        val localEntries = database.journalDao().getAllEntriesIncludingArchived().first()
        val entry = localEntries.find { it.entry_id == entryId }
        assertNotNull(entry)
        assertEquals("Local Updated Content", entry?.description)
        assertEquals(1000L, entry?.timestamp) // Creation date PRESERVED
        assertEquals(5000L, entry?.lastModified)
    }

    @Test
    fun testSyncWorker_AuthFailureReturnsRetry() = runBlocking {
        // Token retrieval fails -> transient condition, periodic work must survive
        SyncWorker.authManagerProvider = {
            val mock = mockk<GoogleAuthManager>()
            coEvery { mock.getDriveAccessTokenSilent() } returns null
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun testSyncWorker_CloudUploadFailureReturnsRetry() = runBlocking {
        val cloudEntries = listOf(
            JournalEntry(entry_id = "upload_fail_id", description = "Entry")
        )
        val cloudJson = Gson().toJson(cloudEntries)

        SyncWorker.driveHelperProvider = { _, _ ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns cloudJson
            coEvery { mock.uploadJournalData(any()) } returns null
            coEvery { mock.downloadDataFile(any()) } returns null
            coEvery { mock.uploadDataFile(any(), any()) } returns "measurements_file_id"
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun testSyncWorker_HandlesPermanentDeletions() = runBlocking {
        // 1. Prepare local "deleted" record
        val deletedId = "deleted_id"
        val repository = com.example.healthjournal.data.JournalRepository(database.journalDao())
        repository.deleteEntries(listOf(deletedId))

        // 2. Prepare mock cloud data containing that ID
        val cloudEntries = listOf(
            JournalEntry(entry_id = deletedId, description = "Should be deleted")
        )
        val cloudJson = Gson().toJson(cloudEntries)

        val capturedUpload = slot<String>()
        SyncWorker.driveHelperProvider = { _, _ ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns cloudJson
            coEvery { mock.uploadJournalData(capture(capturedUpload)) } returns "new_file_id"
            coEvery { mock.downloadDataFile(any()) } returns null
            coEvery { mock.uploadDataFile(any(), any()) } returns "measurements_file_id"
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // 3. Verify that cloud JSON NO LONGER contains the deleted ID
        assertFalse(capturedUpload.captured.contains(deletedId))

        // 4. Verify local tombstone is RETAINED (grace period) so a stale cloud
        // copy in a later sync cycle cannot resurrect the deleted entry.
        assertTrue(repository.getDeletedEntryIds().contains(deletedId))
    }

    @Test
    fun testSyncWorker_PurgesExpiredTombstonesOnlyAfterBothPipelines() = runBlocking {
        val repository = com.example.healthjournal.data.JournalRepository(database.journalDao())
        val graceMs = com.example.healthjournal.data.JournalRepository.TOMBSTONE_GRACE_PERIOD_MS
        val now = System.currentTimeMillis()
        val expiredId = "expired_tombstone_id"
        val freshId = "fresh_tombstone_id"
        database.journalDao().insertDeletedEntry(
            com.example.healthjournal.data.local.DeletedEntry(expiredId, now - graceMs - 1)
        )
        database.journalDao().insertDeletedEntry(com.example.healthjournal.data.local.DeletedEntry(freshId))

        var tombstoneAtMeasurementsDownload: Boolean? = null
        var tombstoneAtMeasurementsUpload: Boolean? = null

        SyncWorker.driveHelperProvider = { _, _ ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns null
            coEvery { mock.uploadJournalData(any()) } returns "journal_file_id"
            coEvery { mock.downloadDataFile(any()) } coAnswers {
                tombstoneAtMeasurementsDownload =
                    repository.getDeletedEntryIds().contains(expiredId)
                null
            }
            coEvery { mock.uploadDataFile(any(), any()) } coAnswers {
                tombstoneAtMeasurementsUpload =
                    repository.getDeletedEntryIds().contains(expiredId)
                "measurements_file_id"
            }
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // Purge must not have run before or during the measurements pipeline:
        // the expired tombstone must still be present at both checkpoints.
        assertTrue(tombstoneAtMeasurementsDownload!!)
        assertTrue(tombstoneAtMeasurementsUpload!!)

        // After both pipelines complete, the expired tombstone is purged and
        // the fresh one is retained for its full grace period.
        val remaining = repository.getDeletedEntryIds()
        assertFalse(remaining.contains(expiredId))
        assertTrue(remaining.contains(freshId))
    }
}
