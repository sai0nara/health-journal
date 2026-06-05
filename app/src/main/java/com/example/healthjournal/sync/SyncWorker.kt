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
import androidx.work.workDataOf
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
        val authManager = authManagerProvider(applicationContext)
        Log.d("SyncWorker", "Starting sync work...")

        try {
            // 1. Get Access Token Silently
            val accessToken = authManager.getDriveAccessTokenSilent()
            
            if (accessToken.isNullOrBlank()) {
                Log.e("SyncWorker", "Silent authorization failed. No token.")
                return Result.failure(workDataOf("error_message" to "Auth failed (No token)"))
            }
            Log.d("SyncWorker", "Token obtained: ${accessToken.take(5)}...")

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
            
            val driveHelper = driveHelperProvider(applicationContext, driveService)

            // 1. Download cloud data
            Log.d("SyncWorker", "Downloading cloud data...")
            val cloudJson = driveHelper.downloadJournalData()
            
            var cloudEntries: List<JournalEntry> = if (cloudJson != null) {
                try {
                    val type = object : TypeToken<List<JournalEntry>>() {}.type
                    Gson().fromJson<List<JournalEntry>>(cloudJson, type) ?: emptyList()
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to parse cloud JSON", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

            // 1a. Filter cloud entries by local deletions
            val deletedIds = repository.getDeletedEntryIds()
            if (deletedIds.isNotEmpty()) {
                Log.d("SyncWorker", "Removing ${deletedIds.size} deleted entries from cloud list.")
                cloudEntries = cloudEntries.filterNot { it.entry_id in deletedIds }
            }

            // 1b. Download missing files from cloud
            cloudEntries.forEach { entry ->
                // Photos
                entry.photo_urls?.forEach { url ->
                    val filename = url.substringAfterLast("/")
                    val localFile = java.io.File(applicationContext.filesDir, "photos/$filename")
                    if (!localFile.exists()) {
                        Log.d("SyncWorker", "Downloading missing photo: $filename")
                        driveHelper.findFileByName(filename)?.let { driveHelper.downloadFile(it, localFile) }
                    }
                }
                // Attachments
                entry.attachments?.forEach { att ->
                    val filename = att.uri.substringAfterLast("/")
                    val localFile = java.io.File(applicationContext.filesDir, "attachments/$filename")
                    if (!localFile.exists()) {
                        Log.d("SyncWorker", "Downloading missing attachment: $filename")
                        driveHelper.findFileByName(filename)?.let { driveHelper.downloadFile(it, localFile) }
                    }
                }
            }

            // 2. Get local data (including archived)
            val localEntries = dao.getAllEntriesIncludingArchived().first()

            // 3. Merge (latest timestamp wins)
            val allEntriesMap = mutableMapOf<String, JournalEntry>()
            cloudEntries.forEach { entry ->
                // Remap URIs to this device
                val updatedPhotos = entry.photo_urls?.map { url ->
                    val filename = url.substringAfterLast("/")
                    java.io.File(applicationContext.filesDir, "photos/$filename").toURI().toString()
                } ?: emptyList()
                val updatedAttachments = entry.attachments?.map { att ->
                    val filename = att.uri.substringAfterLast("/")
                    att.copy(uri = java.io.File(applicationContext.filesDir, "attachments/$filename").toURI().toString())
                } ?: emptyList()
                val updatedEntry = entry.copy(photo_urls = updatedPhotos, attachments = updatedAttachments)
                allEntriesMap[updatedEntry.entry_id] = updatedEntry 
            }
            
            localEntries.forEach { local ->
                val existing = allEntriesMap[local.entry_id]
                if (existing == null || local.lastModified > existing.lastModified) {
                    allEntriesMap[local.entry_id] = local
                }
            }

            val mergedEntries = allEntriesMap.values.toList()

            // 4. Update local DB
            repository.importAll(mergedEntries.map { it.copy(isSynced = true) })

            // 5. Upload new local files to cloud
            mergedEntries.forEach { entry ->
                // Photos
                entry.photo_urls?.forEach { urlString ->
                    try {
                        val uri = android.net.Uri.parse(urlString)
                        val localFile = java.io.File(uri.path ?: "")
                        if (localFile.exists()) driveHelper.uploadFile(localFile, "image/jpeg")
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Failed to upload photo for entry ${entry.entry_id}", e)
                    }
                }
                // Attachments
                entry.attachments?.forEach { att ->
                    try {
                        val uri = android.net.Uri.parse(att.uri)
                        val localFile = java.io.File(uri.path ?: "")
                        if (localFile.exists()) driveHelper.uploadFile(localFile, att.mimeType)
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Failed to upload attachment ${att.name}", e)
                    }
                }
            }

            // 6. Upload merged JSON back to cloud
            val finalEntriesForCloud = mergedEntries.map { it.copy(isSynced = true) }
            val uploadId = driveHelper.uploadJournalData(Gson().toJson(finalEntriesForCloud))
            if (uploadId == null) {
                Log.e("SyncWorker", "Cloud upload failed.")
                return Result.failure(workDataOf("error_message" to "Cloud upload failed (Null ID)"))
            }

            // 7. Clear local deleted tombstones after successful sync
            if (deletedIds.isNotEmpty()) {
                repository.clearDeletedEntries(deletedIds)
            }

            Log.d("SyncWorker", "Bidirectional sync completed successfully.")
            return Result.success()
        } catch (e: Throwable) {
            Log.e("SyncWorker", "Sync FATAL failure!", e)
            val errorMsg = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "No message"}"
            return Result.failure(workDataOf("error_message" to errorMsg))
        }
    }

    companion object {
        var authManagerProvider: (Context) -> GoogleAuthManager = { GoogleAuthManager(it) }
        var driveHelperProvider: (Context, Drive) -> DriveServiceHelper = { context, drive -> DriveServiceHelper(context, drive) }
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
