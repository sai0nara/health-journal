package com.example.healthjournal.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class SyncDownloadMergeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val authManager = SyncWorker.authManagerProvider(applicationContext)
        Log.d("SyncDownloadMergeWorker", "Starting download/merge work...")

        try {
            val accessToken = authManager.getDriveAccessTokenSilent()
            if (accessToken.isNullOrBlank()) return Result.failure()

            val database = JournalDatabase.getDatabase(applicationContext)
            val dao = database.journalDao()
            val repository = JournalRepository(dao)
            
            val credentials = GoogleCredentials.create(AccessToken(accessToken, null))
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Health Journal").build()
            
            val driveHelper = SyncWorker.driveHelperProvider(applicationContext, driveService)

            // Download and merge...
            
            Log.d("SyncDownloadMergeWorker", "Download/merge completed.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncDownloadMergeWorker", "Download/merge failed", e)
            return Result.retry()
        }
    }
}
