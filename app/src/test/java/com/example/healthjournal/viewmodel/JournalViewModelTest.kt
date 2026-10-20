package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.healthjournal.R
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
        // Mirror the real English in values/strings.xml: the ViewModel must
        // select the right key per state, and unit tests assert the resolved
        // English end to end (no Robolectric, so resources are stubbed here).
        val syncEnglish = mapOf(
            R.string.sync_status_syncing to "Syncing...",
            R.string.sync_status_queued to "Sync Queued",
            R.string.sync_status_retrying to "Retrying Sync...",
            R.string.sync_status_synced to "Synced",
            R.string.sync_status_failed to "Sync Failed",
            R.string.sync_status_cancelled to "Sync Cancelled",
            R.string.sync_status_reauth to "Re-authorization required",
            R.string.sync_status_authenticated to "Authenticated & Authorized",
            R.string.sync_status_requested to "Sync Requested",
            R.string.sync_toast_syncing_drive to "Syncing with Google Drive...",
            R.string.sync_toast_drive_auth to "Drive Authorization Successful!",
            R.string.sync_toast_signed_out to "Signed out successfully"
        )
        syncEnglish.forEach { (key, text) -> every { application.getString(key) } returns text }
        val syncFormats = mapOf(
            R.string.sync_toast_failed_format to "Sync failed: %1\$s",
            R.string.sync_toast_signed_in_format to "Signed in as %1\$s",
            R.string.sync_toast_sign_in_failed_format to "Sign-in failed: %1\$s"
        )
        syncFormats.forEach { (key, template) ->
            every { application.getString(key, *anyVararg()) } answers {
                String.format(template, *it.invocation.args.drop(1).toTypedArray())
            }
        }
        
        coEvery { repository.allEntries } returns flowOf(emptyList())
        coEvery { repository.archivedEntries } returns flowOf(emptyList())
        coEvery { repository.allTags } returns flowOf(emptyList())
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
    fun addEntry_unionsParsedHashtagsWithManualTags() = runTest {
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.addTag(any(), any()) } returns Unit

        viewModel.addEntry("Evening #Recovery walk", tags = setOf("DOCTOR"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addTag(any(), "recovery") }
        coVerify { repository.addTag(any(), "DOCTOR") }
    }

    @Test
    fun addEntry_canonicalizesPresetAndHashtagToSingleTag() = runTest {
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.addTag(any(), any()) } returns Unit

        viewModel.addEntry("Morning #Fitness", tags = setOf("FITNESS"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.addTag(any(), "fitness") }
        coVerify(exactly = 1) { repository.addTag(any(), any()) }
    }

    @Test
    fun updateEntry_collapsesStoredCaseVariantsAndKeepsSystemTags() = runTest {
        val entry = JournalEntry(entry_id = "e2", description = "New #sleep text")
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.getTagsForEntry("e2") } returns
            listOf("FITNESS", "fitness", "Fitness", "DOCTOR", "stale")
        coEvery { repository.removeTag(any(), any()) } returns Unit
        coEvery { repository.addTag(any(), any()) } returns Unit

        viewModel.updateEntry(entry, setOf("DOCTOR"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.addTag("e2", "fitness") }
        coVerify(exactly = 1) { repository.addTag("e2", "doctor") }
        coVerify(exactly = 0) { repository.addTag("e2", "FITNESS") }
        coVerify(exactly = 0) { repository.addTag("e2", "Fitness") }
        coVerify(exactly = 0) { repository.addTag("e2", "DOCTOR") }
        coVerify(exactly = 0) { repository.addTag("e2", "stale") }
    }

    @Test
    fun updateEntry_reparsesHashtagsAndPreservesAutoTags() = runTest {
        val entry = JournalEntry(entry_id = "e1", description = "Old #stale text")
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.getTagsForEntry("e1") } returns listOf("Fitness", "DOCTOR", "stale")
        coEvery { repository.removeTag(any(), any()) } returns Unit
        coEvery { repository.addTag(any(), any()) } returns Unit

        viewModel.updateEntry(entry.copy(description = "New #sleep text"), setOf("DOCTOR"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addTag("e1", "sleep") }
        coVerify { repository.addTag("e1", "Fitness") }
        coVerify(exactly = 0) { repository.addTag("e1", "stale") }
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
    fun userEmailReflectsSavedSession() = runTest {
        assertEquals("test@example.com", viewModel.userEmail.value)
    }

    @Test
    fun signInSuccessUpdatesUserEmail() = runTest {
        // Overwrite default signed in state for this test
        every { sessionManager.getUserEmail() } returns null
        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        assertEquals(null, viewModel.userEmail.value)

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

        assertEquals(email, viewModel.userEmail.value)
    }

    @Test
    fun signOutClearsUserEmail() = runTest {
        coEvery { authManager.signOut() } returns Unit
        every { sessionManager.clearSession() } returns Unit

        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.userEmail.value)
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
        coEvery { repository.addTag(entryId, "exercises") } returns Unit
        coEvery { repository.updateSyncStatus(entryId, "PENDING_SYNC") } returns Unit
        coEvery { repository.markEntryDirty(entryId) } returns Unit

        viewModel.toggleEntryTag(entryId, "EXERCISES")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.addTag(entryId, "exercises") }
        coVerify { repository.markEntryDirty(entryId) }
    }

    @Test
    fun toggleTag_canonicalizesFilterSelection() = runTest {
        viewModel.toggleTag("FITNESS")

        assertEquals(setOf("fitness"), viewModel.selectedTags.value)
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

    @Test
    fun authRequired_surfacesWhileManualWorkBlockedOnAuthorization() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val info = workInfo(WorkInfo.State.ENQUEUED).also {
            every { it.progress } returns androidx.work.workDataOf(SyncWorker.KEY_AUTH_REQUIRED to true)
        }
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns MutableStateFlow(listOf(info))

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.authRequired.value)
    }

    @Test
    fun authRequired_clearsOnceManualWorkSucceeds() = runTest {
        val workManager = mockk<WorkManager>(relaxed = true)
        val blocked = workInfo(WorkInfo.State.ENQUEUED).also {
            every { it.progress } returns androidx.work.workDataOf(SyncWorker.KEY_AUTH_REQUIRED to true)
        }
        val workFlow = MutableStateFlow(listOf(blocked))
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_periodic") } returns MutableStateFlow(emptyList())
        every { workManager.getWorkInfosForUniqueWorkFlow("journal_sync_manual") } returns workFlow

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.authRequired.value)

        workFlow.value = listOf(workInfo(WorkInfo.State.SUCCEEDED))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.authRequired.value)
    }

    @Test
    fun requestDriveAuth_forwardsResolutionRequiredPendingIntent() = runTest {
        var capturedResolution: ((android.app.PendingIntent) -> Unit)? = null
        every { authManager.requestDriveAuthorization(any(), any()) } answers {
            capturedResolution = firstArg()
            secondArg<(String) -> Unit>()
        }

        viewModel = JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService, testDispatcher)
        var forwarded = false
        viewModel.requestDriveAuth { forwarded = true }

        capturedResolution?.invoke(mockk(relaxed = true))

        assertTrue(forwarded)
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

    @Test
    fun healthSync_defaultsToDisabled() = runTest {
        // Strict-mock Application has no stubbed prefs: unreadable resolves to OFF.
        assertFalse(viewModel.isHealthSyncEnabled.value)
    }

    @Test
    fun healthSync_initialEnabled_reflectsOptIn() = runTest {
        val enabled = JournalViewModel(
            application, repository, authManager, sessionManager,
            healthManager, mediaService, testDispatcher,
            healthSyncEnabledInitial = true
        )

        assertTrue(enabled.isHealthSyncEnabled.value)
    }

    @Test
    fun syncHealthData_whenDisabled_returnsEmptyWithoutCallingManager() = runTest {
        val result = viewModel.syncHealthData(1_700_000_000_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(HealthSyncResult(), result)
        coVerify(exactly = 0) { healthManager.getLatestBloodPressure(any(), any()) }
        coVerify(exactly = 0) { healthManager.getAverageHeartRate(any(), any()) }
        coVerify(exactly = 0) { healthManager.getSleepDurationHours(any(), any()) }
    }

    @Test
    fun syncHealthData_whenEnabled_fetchesFromManager() = runTest {
        coEvery { healthManager.getLatestBloodPressure(any(), any()) } returns (120.0 to 80.0)
        coEvery { healthManager.getAverageHeartRate(any(), any()) } returns 72L
        coEvery { healthManager.getSleepDurationHours(any(), any()) } returns 7.5f
        val enabled = JournalViewModel(
            application, repository, authManager, sessionManager,
            healthManager, mediaService, testDispatcher,
            healthSyncEnabledInitial = true
        )

        val result = enabled.syncHealthData(1_700_000_000_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(120.0, result.bpSystolic)
        assertEquals(80.0, result.bpDiastolic)
        assertEquals(72, result.heartRate)
        assertEquals(7.5f, result.sleepHours!!, 0.001f)
    }

    @Test
    fun hasHealthPermissions_whenDisabled_returnsFalseWithoutManagerCall() = runTest {
        assertFalse(viewModel.hasHealthPermissions())

        coVerify(exactly = 0) { healthManager.hasAllPermissions() }
    }

    @Test
    fun setHealthSyncEnabled_persistsAndReloadsAcrossInstances() = runTest {
        val store = mutableMapOf<String, Boolean>()
        val prefs: SharedPreferences = mockk()
        val editor: SharedPreferences.Editor = mockk(relaxed = true)
        every { application.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getBoolean(any(), any()) } answers {
            store[it.invocation.args[0] as String] ?: (it.invocation.args[1] as Boolean)
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[it.invocation.args[0] as String] = it.invocation.args[1] as Boolean
            editor
        }

        viewModel.setHealthSyncEnabled(true)
        assertTrue(viewModel.isHealthSyncEnabled.value)

        val reloaded = JournalViewModel(
            application, repository, authManager, sessionManager,
            healthManager, mediaService, testDispatcher
        )
        assertTrue(reloaded.isHealthSyncEnabled.value)
    }
}
