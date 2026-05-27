package com.example.healthjournal.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.healthjournal.auth.GoogleAuthManager
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
    fun testSyncWorker_MergesCloudAndLocalData() = runBlocking {
        // 1. Prepare local data
        val localEntry = JournalEntry(description = "Local Entry", timestamp = 1500)
        database.journalDao().insertEntry(localEntry)

        // 2. Prepare mock cloud data (one newer, one older, one same)
        val cloudEntries = listOf(
            JournalEntry(entry_id = localEntry.entry_id, description = "Updated Local Entry", timestamp = 2000), // Newer
            JournalEntry(description = "Cloud Only Entry", timestamp = 1000) // New
        )
        val cloudJson = Gson().toJson(cloudEntries)

        // Mock DriveServiceHelper provider
        SyncWorker.driveHelperProvider = { _, _ ->
            val mock = mockk<DriveServiceHelper>()
            coEvery { mock.downloadJournalData() } returns cloudJson
            coEvery { mock.uploadJournalData(any()) } returns "new_file_id"
            mock
        }

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // 3. Verify data in local database
        val localEntries = database.journalDao().getAllEntries().first()
        
        // Should have 2 entries: the updated local one and the cloud-only one
        assertEquals(2, localEntries.size)
        assertTrue(localEntries.any { it.description == "Updated Local Entry" })
        assertTrue(localEntries.any { it.description == "Cloud Only Entry" })
    }
}
