package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.health.HealthConnectManager
import com.example.healthjournal.sync.SyncManager
import com.example.healthjournal.sync.SyncWorker
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    private lateinit var viewModel: JournalViewModel
    private val repository: JournalRepository = mockk()
    private val authManager: GoogleAuthManager = mockk()
    private val sessionManager: SessionManager = mockk()
    private val healthManager: HealthConnectManager = mockk()
    private val mediaService: com.example.healthjournal.media.MediaCompressionService = mockk()
    private val application: Application = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<CharSequence>(), any()) } returns mockk(relaxed = true)
        every { Toast.makeText(any(), any<Int>(), any()) } returns mockk(relaxed = true)

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)

        mockkObject(SyncManager)
        every { SyncManager.enqueuePeriodicSync(any()) } returns Unit

        // Mock signed in state for sync tests
        every { sessionManager.getUserEmail() } returns "test@example.com"
        every { application.applicationContext } returns application
        every { application.getString(any()) } returns "test_client_id"
        
        coEvery { repository.allEntries } returns flowOf(emptyList())
        coEvery { repository.archivedEntries } returns flowOf(emptyList())
        coEvery { repository.getEntriesSortedByDate(any()) } returns flowOf(emptyList())
        
        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun addEntryCallsRepository() = runTest {
        val description = "Test Description"
        coEvery { repository.insert(any()) } returns Unit
        
        viewModel.addEntry(description)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.insert(match { it.description == description }) }
    }

    @Test
    fun addEntryPreventsFutureDates() = runTest {
        val futureTimestamp = System.currentTimeMillis() + 100000
        coEvery { repository.insert(any()) } returns Unit
        
        viewModel.addEntry("Future", futureTimestamp)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun updateEntryCallsRepository() = runTest {
        val entry = JournalEntry(description = "Old")
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.getTagsForEntry(any()) } returns emptyList()
        coEvery { repository.removeTag(any(), any()) } returns Unit
        coEvery { repository.addTag(any(), any()) } returns Unit

        viewModel.updateEntry(entry.copy(description = "New"), emptySet())
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.insert(match { it.description == "New" }) }
    }

    @Test
    fun archiveEntryCallsRepositoryAndSync() = runTest {
        val entryId = "entry_to_archive"
        coEvery { repository.archiveEntry(entryId) } returns Unit
        
        viewModel.archiveEntry(entryId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.archiveEntry(entryId) }
        verify { SyncManager.enqueuePeriodicSync(any()) }
    }

    @Test
    fun restoreEntryCallsRepositoryAndSync() = runTest {
        val entryId = "entry_to_restore"
        coEvery { repository.restoreEntry(entryId) } returns Unit
        
        viewModel.restoreEntry(entryId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.restoreEntry(entryId) }
        verify { SyncManager.enqueuePeriodicSync(any()) }
    }

    @Test
    fun deleteEntriesCallsRepositoryAndSync() = runTest {
        val ids = listOf("id1", "id2")
        coEvery { repository.getEntriesByIds(ids) } returns emptyList()
        coEvery { repository.deleteEntries(ids) } returns Unit
        
        viewModel.deleteEntries(ids)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.getEntriesByIds(ids) }
        coVerify { repository.deleteEntries(ids) }
        verify { SyncManager.enqueuePeriodicSync(any()) }
    }

    @Test
    fun emptyArchiveCallsRepositoryAndSync() = runTest {
        coEvery { repository.getArchivedEntriesList() } returns emptyList()
        coEvery { repository.deleteAllArchived() } returns Unit
        
        viewModel.emptyArchive()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { repository.deleteAllArchived() }
        verify { SyncManager.enqueuePeriodicSync(any()) }
    }

    @Test
    fun deleteEntriesRemovesLocalAttachmentFiles() = runTest {
        // Simulate the app sandbox: photos/ and attachments/ under filesDir
        val fakeFilesDir = java.nio.file.Files.createTempDirectory("journal_files").toFile()
        every { application.filesDir } returns fakeFilesDir
        val photo = java.io.File(java.io.File(fakeFilesDir, "photos").apply { mkdirs() }, "photo1.jpg").apply { writeText("x") }
        val attachment = java.io.File(java.io.File(fakeFilesDir, "attachments").apply { mkdirs() }, "doc1.pdf").apply { writeText("y") }
        val entry = JournalEntry(
            description = "doomed",
            timestamp = 1000L,
            photo_urls = listOf("file://${photo.absolutePath}"),
            attachments = listOf(AttachmentData(name = "doc1.pdf", uri = "file://${attachment.absolutePath}", mimeType = "application/pdf"))
        )
        val ids = listOf(entry.entry_id)
        coEvery { repository.getEntriesByIds(ids) } returns listOf(entry)
        coEvery { repository.deleteEntries(ids) } returns Unit

        viewModel.deleteEntries(ids)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(photo.exists())
        assertFalse(attachment.exists())
        fakeFilesDir.deleteRecursively()
    }

    @Test
    fun deleteEntriesKeepsFilesOutsideFilesDir() = runTest {
        val fakeFilesDir = java.nio.file.Files.createTempDirectory("journal_files").toFile()
        every { application.filesDir } returns fakeFilesDir
        val outsideRoot = java.nio.file.Files.createTempDirectory("outside_root").toFile()
        val outside = java.io.File(outsideRoot, "keep.txt").apply { writeText("precious") }
        val entry = JournalEntry(
            description = "foreign uri",
            timestamp = 1000L,
            photo_urls = listOf("file://${outside.absolutePath}")
        )
        val ids = listOf(entry.entry_id)
        coEvery { repository.getEntriesByIds(ids) } returns listOf(entry)
        coEvery { repository.deleteEntries(ids) } returns Unit

        viewModel.deleteEntries(ids)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(outside.exists())
        fakeFilesDir.deleteRecursively()
        outsideRoot.deleteRecursively()
    }

    @Test
    fun signInSuccessUpdatesState() = runTest {
        // Overwrite default signed in state for this test
        every { sessionManager.getUserEmail() } returns null
        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)

        val context: Context = mockk()
        val email = "test@example.com"
        val credential = mockk<com.google.android.libraries.identity.googleid.GoogleIdTokenCredential>()
        every { credential.id } returns email
        
        coEvery { authManager.signIn(context) } returns credential
        every { sessionManager.saveUserEmail(email) } returns Unit
        every { sessionManager.getUserEmail() } returns email
        
        val onResolution = slot<(android.app.PendingIntent) -> Unit>()
        val onSuccess = slot<(String) -> Unit>()
        every { authManager.requestDriveAuthorization(capture(onResolution), capture(onSuccess)) } answers {
            onSuccess.captured.invoke("test_token")
        }
        
        viewModel.signIn(context) {}
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.isUserSignedIn.value)
        verify { sessionManager.saveUserEmail(email) }
    }

    @Test
    fun signOutClearsState() = runTest {
        coEvery { authManager.signOut() } returns Unit
        every { sessionManager.clearSession() } returns Unit
        
        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.isUserSignedIn.value)
        coVerify { 
            authManager.signOut()
        }
        verify {
            sessionManager.clearSession()
        }
    }

    @Test
    fun toggleEntryTag_marksEntryDirtyAndTriggersSync() = runTest {
        val entryId = "tagged_entry"
        coEvery { repository.getTagsForEntry(entryId) } returns listOf("DOCTOR")
        coEvery { repository.removeTag(entryId, "DOCTOR") } returns Unit
        coEvery { repository.addTag(entryId, any()) } returns Unit
        coEvery { repository.updateSyncStatus(entryId, "PENDING_SYNC") } returns Unit
        coEvery { repository.markEntryDirty(entryId) } returns Unit

        viewModel.toggleEntryTag(entryId, "DOCTOR")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.removeTag(entryId, "DOCTOR") }
        coVerify { repository.markEntryDirty(entryId) }
        verify { SyncManager.enqueuePeriodicSync(any()) }
    }

    @Test
    fun toggleEntryTag_addsMissingTagAndMarksDirty() = runTest {
        val entryId = "untagged_entry"
        coEvery { repository.getTagsForEntry(entryId) } returns emptyList()
        coEvery { repository.removeTag(any(), any()) } returns Unit
        coEvery { repository.addTag(entryId, "EXERCISES") } returns Unit
        coEvery { repository.updateSyncStatus(entryId, "PENDING_SYNC") } returns Unit
        coEvery { repository.markEntryDirty(entryId) } returns Unit

        viewModel.toggleEntryTag(entryId, "EXERCISES")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addTag(entryId, "EXERCISES") }
        coVerify { repository.markEntryDirty(entryId) }
    }

    @Test
    fun syncStatus_observesPeriodicAndManualWorkNames() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns MutableStateFlow(emptyList())

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") }
        verify { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") }
    }

    @Test
    fun syncStatus_reflectsManualWorkCompletion() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val workFlow = MutableStateFlow(listOf(workInfo(WorkInfo.State.SUCCEEDED)))
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns workFlow

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Synced", viewModel.syncStatus.value)
    }

    @Test
    fun syncStatus_failureEmitsSyncFailed() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val workFlow = MutableStateFlow(listOf(workInfo(WorkInfo.State.FAILED)))
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns workFlow

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sync Failed", viewModel.syncStatus.value)
    }

    @Test
    fun syncStatus_runningEmittedForEnqueuedWork() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val workFlow = MutableStateFlow(listOf(workInfo(WorkInfo.State.RUNNING)))
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns workFlow

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Syncing...", viewModel.syncStatus.value)
    }

    @Test
    fun syncStatus_authRequiredSurfacedFromProgress() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val info = workInfo(WorkInfo.State.ENQUEUED).also {
            every { it.progress } returns androidx.work.workDataOf(SyncWorker.KEY_AUTH_REQUIRED to true)
        }
        val workFlow = MutableStateFlow(listOf(info))
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns workFlow

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Re-authorization required", viewModel.syncStatus.value)
    }

    private fun workInfo(state: WorkInfo.State): WorkInfo {
        val info = mockk<WorkInfo>()
        every { info.state } returns state
        every { info.runAttemptCount } returns 0
        every { info.outputData } returns androidx.work.Data.EMPTY
        every { info.progress } returns androidx.work.Data.EMPTY
        return info
    }

    @Test
    fun getEntryByIdCallsRepository() = runTest {
        val entry = JournalEntry(description = "Test")
        coEvery { repository.getEntryById("123") } returns entry

        val result = viewModel.getEntryById("123")

        assertEquals(entry, result)
    }

    @Test
    fun allEntriesReflectsSearchAndSort() = runTest {
        val entries = listOf(JournalEntry(description = "Apple"))
        val searchQuery = "App"
        
        // Mock the search query call
        coEvery { repository.searchEntries(searchQuery, any()) } returns flowOf(entries)
        
        // Re-initialize to pick up flow changes
        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)

        // Start collecting
        val items = mutableListOf<List<JournalEntry>>()
        val collectJob = backgroundScope.launch {
            viewModel.allEntries.collect { items.add(it) }
        }
        
        viewModel.setSearchQuery(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(items.any { it.any { entry -> entry.description == "Apple" } })
        collectJob.cancel()
    }
}
