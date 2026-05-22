package com.example.healthjournal.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val authManager = GoogleAuthManager(applicationContext)

        try {
            // 1. Get Access Token Silently
            val accessToken = authManager.getDriveAccessTokenSilent()
            
            if (accessToken == null) {
                Log.e("SyncWorker", "Silent authorization failed. User interaction might be required.")
                return Result.failure()
            }

            val database = JournalDatabase.getDatabase(applicationContext)
            val dao = database.journalDao()
            val repository = JournalRepository(dao)
            
            // 2. Initialize Drive with explicit token
            val credentials = GoogleCredentials.create(AccessToken(accessToken, null))
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Health Journal").build()
            
            val driveHelper = DriveServiceHelper(driveService)

            // 1. Download cloud data
            val cloudJson = driveHelper.downloadJournalData()
            val cloudEntries: List<JournalEntry> = if (cloudJson != null) {
                val type = object : TypeToken<List<JournalEntry>>() {}.type
                Gson().fromJson(cloudJson, type)
            } else {
                emptyList()
            }

            // 2. Get local data
            val localEntries = dao.getAllEntries().first()

            // 3. Merge (latest timestamp wins)
            val allEntriesMap = mutableMapOf<String, JournalEntry>()
            
            // Add all cloud entries first
            cloudEntries.forEach { allEntriesMap[it.entry_id] = it }
            
            // Overwrite or add local entries if they are newer
            localEntries.forEach { local ->
                val existing = allEntriesMap[local.entry_id]
                if (existing == null || local.timestamp > existing.timestamp) {
                    allEntriesMap[local.entry_id] = local
                }
            }

            val mergedEntries = allEntriesMap.values.toList().map { it.copy(isSynced = true) }

            // 4. Update local DB
            repository.importAll(mergedEntries)

            // 5. Upload merged data back to cloud
            driveHelper.uploadJournalData(Gson().toJson(mergedEntries))
            
            Log.d("SyncWorker", "Bidirectional sync successful")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            return Result.retry()
        }
    }
}

object SyncManager {
    fun enqueueSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "journal_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
