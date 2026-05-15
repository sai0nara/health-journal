package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.sync.SyncManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val application: Application = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)

        mockkObject(SyncManager)
        every { SyncManager.enqueueSync(any()) } returns Unit

        every { sessionManager.getUserEmail() } returns null
        every { application.applicationContext } returns application
        
        coEvery { repository.allEntries } returns flowOf(emptyList())
        viewModel = JournalViewModel(application, repository, authManager, sessionManager)
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
        
        coVerify { repository.insert(match { it.description == description }) }
    }

    @Test
    fun addEntryPreventsFutureDates() = runTest {
        val futureTimestamp = System.currentTimeMillis() + 100000
        coEvery { repository.insert(any()) } returns Unit
        
        viewModel.addEntry("Future", futureTimestamp)
        
        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun updateEntryCallsRepository() = runTest {
        val entry = JournalEntry(description = "Old")
        coEvery { repository.insert(any()) } returns Unit
        
        viewModel.updateEntry(entry.copy(description = "New"))
        
        coVerify { repository.insert(match { it.description == "New" }) }
    }

    @Test
    fun signInSuccessUpdatesState() = runTest {
        val context: Context = mockk()
        val email = "test@example.com"
        val credential = mockk<com.google.android.libraries.identity.googleid.GoogleIdTokenCredential>()
        every { credential.id } returns email
        
        coEvery { authManager.signIn(context) } returns credential
        every { sessionManager.saveUserEmail(email) } returns Unit
        
        val onResolution = slot<(android.app.PendingIntent) -> Unit>()
        val onSuccess = slot<(String) -> Unit>()
        every { authManager.requestDriveAuthorization(eq(email), capture(onResolution), capture(onSuccess)) } answers {
            onSuccess.captured.invoke("token")
        }

        viewModel.signIn(context) {}
        
        assertTrue(viewModel.isUserSignedIn.value)
        verify { sessionManager.saveUserEmail(email) }
    }

    @Test
    fun signOutClearsState() = runTest {
        coEvery { authManager.signOut() } returns Unit
        every { sessionManager.clearSession() } returns Unit
        
        viewModel.signOut()
        
        assertFalse(viewModel.isUserSignedIn.value)
        coVerify { 
            authManager.signOut()
        }
        verify {
            sessionManager.clearSession()
        }
    }

    @Test
    fun getEntryByIdCallsRepository() = kotlinx.coroutines.runBlocking {
        val entry = JournalEntry(description = "Test")
        coEvery { repository.getEntryById("123") } returns entry

        val result = viewModel.getEntryById("123")

        assertEquals(entry, result)
    }
}
