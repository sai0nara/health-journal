package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.allEntries } returns flowOf(emptyList())
        viewModel = JournalViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        viewModel = JournalViewModel(repository)
        
        // Start collecting so WhileSubscribed starts the underlying flow
        val collectJob = backgroundScope.launch {
            viewModel.allEntries.collect {}
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(entries, viewModel.allEntries.value)
        collectJob.cancel()
    }
}
