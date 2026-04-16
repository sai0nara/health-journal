package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class JournalViewModelTest {

    private lateinit var viewModel: JournalViewModel
    private val repository: JournalRepository = mockk()
    private val application: Application = mockk()
    private val sharedPreferences: SharedPreferences = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock WorkManager to avoid IllegalStateException in SyncManager.enqueueSync
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)

        // Mocking for SessionManager initialization inside ViewModel
        every { application.getSharedPreferences("health_journal_session", Context.MODE_PRIVATE) } returns sharedPreferences
        every { application.applicationContext } returns application
        
        coEvery { repository.allEntries } returns flowOf(emptyList())
        viewModel = JournalViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(WorkManager::class)
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
    fun allEntriesReflectsRepositoryFlow() = runTest {
        val entries = listOf(JournalEntry(description = "Entry 1"))
        coEvery { repository.allEntries } returns flowOf(entries)
        
        // Need to recreate viewModel since allEntries is a property initialized at creation
        viewModel = JournalViewModel(application, repository)
        
        // Start collecting so WhileSubscribed starts the underlying flow
        val collectJob = backgroundScope.launch {
            viewModel.allEntries.collect {}
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(entries, viewModel.allEntries.value)
        collectJob.cancel()
    }
}
