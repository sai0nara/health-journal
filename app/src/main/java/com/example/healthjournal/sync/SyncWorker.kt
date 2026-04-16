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
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.local.JournalDatabase
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val email = sessionManager.getUserEmail() ?: return Result.failure()

        try {
            val database = JournalDatabase.getDatabase(applicationContext)
            val repository = database.journalDao()
            val entries = repository.getAllEntries().first()
            
            val json = Gson().toJson(entries)
            
            val account = android.accounts.Account(email, "com.google")
            val driveService = DriveServiceHelper.createDriveService(applicationContext, account)
            val driveHelper = DriveServiceHelper(driveService)
            
            driveHelper.uploadJournalData(json)
            
            Log.d("SyncWorker", "Sync successful")
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
