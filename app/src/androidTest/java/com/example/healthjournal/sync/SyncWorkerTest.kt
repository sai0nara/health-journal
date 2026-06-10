package com.example.healthjournal.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.healthjournal.auth.GoogleAuthManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncWorkerTest {

    private lateinit var context: Context
    private val authManager: GoogleAuthManager = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SyncWorker.authManagerProvider = { authManager }
    }

    @Test
    fun testSyncWorker_FailsWhenNoToken() = runBlocking {
        every { authManager.getDriveAccessTokenSilent() } returns null

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
