package com.example.healthjournal.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.PeriodicWorkRequestBuilder
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
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
                // Surface the blocker to the UI: silent auth only fails when the
                // user must (re-)grant Drive consent, so retrying silently forever
                // is not enough — the user needs to re-authorize from the app.
                setProgress(workDataOf(KEY_AUTH_REQUIRED to true))
                // Transient: token may be available on the next run (30-60 min
                // lifetime). failure() would permanently kill periodic sync.
                return Result.retry()
            }
            setProgress(workDataOf(KEY_AUTH_REQUIRED to false))
            Log.d("SyncWorker", "Token obtained successfully")

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
                    val rawEntries = Gson().fromJson<List<JournalEntry>>(cloudJson, type) ?: emptyList()
                    // Fix nulls from old cloud data
                    rawEntries.map { entry ->
                        // Fix nulls from old cloud data (preserve tags through copy)
                        entry.copy(
                            isSynced = entry.isSynced ?: true,
                            syncStatus = entry.syncStatus ?: "SYNCED",
                            attachments = entry.attachments?.map { att ->
                                att.copy(syncStatus = att.syncStatus ?: "SYNCED", isLocalOnly = att.isLocalOnly ?: false)
                            } ?: emptyList(),
                            photo_urls = entry.photo_urls ?: emptyList()
                        ).also { it.tags = entry.tags }
                    }
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

            // 3. Merge (latest timestamp wins), preserving tags per LWW
            val remappedCloud = cloudEntries.map { entry ->
                // Remap URIs to this device
                val updatedPhotos = entry.photo_urls?.map { url ->
                    val filename = url.substringAfterLast("/")
                    java.io.File(applicationContext.filesDir, "photos/$filename").toURI().toString()
                } ?: emptyList()
                val updatedAttachments = entry.attachments?.map { att ->
                    val filename = att.uri.substringAfterLast("/")
                    att.copy(uri = java.io.File(applicationContext.filesDir, "attachments/$filename").toURI().toString())
                } ?: emptyList()
                entry.copy(photo_urls = updatedPhotos, attachments = updatedAttachments)
                    .also { it.tags = entry.tags }
            }

            val mergedEntries = SyncMerge.merge(remappedCloud, localEntries) { entryId ->
                dao.getTagsForEntry(entryId)
            }

            // 4. Update local DB
            repository.importAll(mergedEntries.map { it.copy(isSynced = true) })
            
            // Persist tags from merged results back to the cross-ref table.
            // Entries with null tags carry no tag info (legacy payload) and
            // must not have local tags wiped.
            mergedEntries.forEach { entry ->
                entry.tags?.let { tags ->
                    dao.deleteAllTagsForEntry(entry.entry_id)
                    tags.forEach { tag -> dao.insertTag(EntryTagCrossRef(entry.entry_id, tag)) }
                }
            }

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
            val finalEntriesForCloud = mergedEntries.map { it.copy(isSynced = true).also { e -> e.tags = it.tags } }
            val uploadId = driveHelper.uploadJournalData(Gson().toJson(finalEntriesForCloud))
            if (uploadId == null) {
                Log.e("SyncWorker", "Cloud upload failed.")
                // Transient network/API failure: keep periodic work alive.
                return Result.retry()
            }

            // ============ Body measurements sync ============
            // Same pipeline, sibling cloud file: download -> filter local
            // tombstones -> LWW merge -> persist -> upload.
            Log.d("SyncWorker", "Syncing body measurements...")
            val measurementDao = database.bodyMeasurementDao()

            // Sync the cross-device deletion ledger FIRST so deletions made on
            // other devices are visible to this run's filtering. The ledger is
            // shared by both entity types (one deleted_entries table).
            val ledgerById = measurementDao.getAllDeletedEntries().associateBy { it.entry_id }
                .toMutableMap()
            MeasurementTombstonePayload.fromJson(
                driveHelper.downloadDataFile(DriveServiceHelper.MEASUREMENTS_TOMBSTONES_FILE)
            ).forEach { remote ->
                // Newest deletion wins when both sides tombstoned the same row.
                val local = ledgerById[remote.entry_id]
                if (local == null || remote.deletedAt > local.deletedAt) {
                    measurementDao.insertDeletedEntry(remote)
                    ledgerById[remote.entry_id] = remote
                }
            }

            val cloudMeasurementsJson = driveHelper.downloadDataFile(DriveServiceHelper.MEASUREMENTS_DATA_FILE)
            var cloudMeasurements = MeasurementSyncPayload.fromJson(cloudMeasurementsJson)
            if (cloudMeasurements.isNotEmpty() && ledgerById.isNotEmpty()) {
                // Same LWW rule as the local-row sweep below: a tombstone only
                // removes a cloud copy when the deletion is at least as recent
                // as the copy's last edit. Filtering by tombstone presence alone
                // would re-delete edits that another device legitimately won
                // back after its own LWW evaluation, causing endless
                // delete/re-upload flapping between devices.
                cloudMeasurements = cloudMeasurements.filterNot { m ->
                    val tombstone = ledgerById[m.entry_id]
                    tombstone != null && tombstone.deletedAt >= m.lastModified
                }
            }

            // Drop local rows that were deleted on another device, unless the
            // local edit is newer than the deletion (LWW: the edit wins and is
            // re-uploaded below). importAll only upserts, so removal is explicit.
            val localMeasurementsAll = measurementDao.getAllEntriesList()
            val remotelyDeletedLocalIds = localMeasurementsAll.mapNotNull { m ->
                val tombstone = ledgerById[m.entry_id]
                when {
                    tombstone == null -> null
                    tombstone.deletedAt >= m.lastModified -> m.entry_id
                    else -> null
                }
            }
            if (remotelyDeletedLocalIds.isNotEmpty()) {
                measurementDao.deleteEntriesByIds(remotelyDeletedLocalIds)
            }
            val removedIds = remotelyDeletedLocalIds.toSet()
            val localMeasurements = localMeasurementsAll.filterNot { it.entry_id in removedIds }

            val mergedMeasurements = SyncMerge.mergeMeasurements(cloudMeasurements, localMeasurements)

            measurementDao.importAll(mergedMeasurements)

            val uploadedMeasurementId = driveHelper.uploadDataFile(
                DriveServiceHelper.MEASUREMENTS_DATA_FILE,
                MeasurementSyncPayload.toJson(mergedMeasurements)
            )
            if (uploadedMeasurementId == null) {
                Log.e("SyncWorker", "Body measurements upload failed.")
                return Result.retry()
            }

            // Publish the merged deletion ledger for other devices.
            val uploadedLedgerId = driveHelper.uploadDataFile(
                DriveServiceHelper.MEASUREMENTS_TOMBSTONES_FILE,
                MeasurementTombstonePayload.toJson(measurementDao.getAllDeletedEntries())
            )
            if (uploadedLedgerId == null) {
                Log.e("SyncWorker", "Deletion ledger upload failed.")
                return Result.retry()
            }

            // ============ Body measurement goals sync ============
            // Sibling cloud file; full-snapshot merge, no tombstones needed.
            Log.d("SyncWorker", "Syncing body measurement goals...")
            val goalDao = database.goalDao()

            val cloudGoalsJson = driveHelper.downloadDataFile(DriveServiceHelper.MEASUREMENTS_GOALS_FILE)
            val localGoals = goalDao.getAll()

            // When the cloud file doesn't exist yet (first sync on this
            // device), preserve local goals and push them up rather than
            // pruning them into an empty cloud snapshot.
            val mergedGoals = if (cloudGoalsJson == null) {
                localGoals
            } else {
                SyncMerge.mergeGoals(GoalSyncPayload.fromJson(cloudGoalsJson), localGoals)
            }

            goalDao.importAll(mergedGoals)

            val uploadedGoalsId = driveHelper.uploadDataFile(
                DriveServiceHelper.MEASUREMENTS_GOALS_FILE,
                GoalSyncPayload.toJson(mergedGoals)
            )
            if (uploadedGoalsId == null) {
                Log.e("SyncWorker", "Goals upload failed.")
                return Result.retry()
            }

            // 7. Purge expired tombstones — only after ALL sync pipelines are
            // done, since both share the deleted_entries table. Young tombstones
            // are kept so a stale cloud copy in a later sync cycle cannot
            // resurrect a deleted entry.
            repository.clearDeletedEntries()

            Log.d("SyncWorker", "Bidirectional sync completed successfully.")
            return Result.success()
        } catch (e: Throwable) {
            Log.e("SyncWorker", "Sync FATAL failure!", e)
            val errorMsg = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "No message"}"
            // Transient errors (network, Drive API) must keep periodic work alive.
            return if (e is IOException || e is GoogleJsonResponseException) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error_message" to errorMsg))
            }
        }
    }

    companion object {
        const val KEY_AUTH_REQUIRED = "auth_required"
        var authManagerProvider: (Context) -> GoogleAuthManager = { GoogleAuthManager(it) }
        var driveHelperProvider: (Context, Drive) -> DriveServiceHelper = { context, drive -> DriveServiceHelper(context, drive) }
    }
}

object SyncManager {
    const val PERIODIC_WORK_NAME = "journal_sync_periodic"
    const val MANUAL_WORK_NAME = "journal_sync_manual"

    fun enqueuePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun triggerManualSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}

