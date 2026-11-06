package com.example.healthjournal.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.ProfileDataEraser
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.RoomProfileDataEraser
import com.example.healthjournal.data.SharedPreferencesActiveProfileStore
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileDeletionDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException
import java.util.concurrent.TimeUnit
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
            val measurementDao = database.bodyMeasurementDao()
            val goalDao = database.goalDao()
            val personalCardDao = database.personalCardDao()
            val eraser = RoomProfileDataEraser(database, applicationContext.filesDir)
            val profileRepository = ProfileRepository(
                profileDao = database.profileDao(),
                settings = SharedPreferencesActiveProfileStore(applicationContext),
                eraser = eraser
            )
            profileRepository.ensureSelfProfile()
            val repository = JournalRepository(dao, profileRepository)

            // 2. Initialize Drive with explicit token
            val credentials = GoogleCredentials.create(AccessToken(accessToken, null))
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Health Journal").build()

            val driveHelper = driveHelperProvider(applicationContext, driveService)

            // 3. Sync profile metadata and profile deletions before data files
            val syncedProfiles = syncProfileMetadata(
                profileDao = database.profileDao(),
                profileDeletionDao = database.profileDeletionDao(),
                profileRepository = profileRepository,
                eraser = eraser,
                driveHelper = driveHelper
            ) ?: return Result.retry()

            val orderedProfiles = syncedProfiles.sortedWith(
                compareBy({ it.id != SELF_PROFILE_ID }, { it.id })
            )

            for (profile in orderedProfiles) {
                val profileId = profile.id

                // ============ Journal sync for profileId ============
                Log.d("SyncWorker", "Downloading journal data...")
                val cloudJson = if (profileId == SELF_PROFILE_ID) {
                    driveHelper.downloadJournalData()
                } else {
                    driveHelper.downloadDataFile(DriveServiceHelper.journalFileName(profileId))
                }

                var cloudEntries = JournalSyncPayload.fromJson(
                    json = cloudJson,
                    defaultProfileId = profileId
                ).filter { it.profileId == profileId }

                // 1a. Filter cloud entries by local deletions
                val deletedIds = repository.getDeletedEntryIds(profileId)
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
                val localEntries = dao.getAllEntriesIncludingArchived(profileId).first()

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
                    entry.copy(
                        profileId = profileId,
                        photo_urls = updatedPhotos,
                        attachments = updatedAttachments
                    ).also { it.tags = entry.tags }
                }

                val mergedEntries = SyncMerge.merge(remappedCloud, localEntries) { entryId ->
                    dao.getTagsForEntry(profileId, entryId)
                }

                // 4. Update local DB
                repository.importAll(profileId, mergedEntries.map { it.copy(profileId = profileId, isSynced = true) })

                // Persist tags from merged results back to the cross-ref table.
                // Entries with null tags carry no tag info (legacy payload) and
                // must not have local tags wiped.
                mergedEntries.forEach { entry ->
                    entry.tags?.let { tags ->
                        val refs = SyncMerge.canonicalTagRefs(profileId, entry.entry_id, tags)
                        dao.deleteAllTagsForEntry(profileId, entry.entry_id)
                        refs.forEach { dao.insertTag(it) }
                        entry.tags = refs.map { it.tag }
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
                val finalEntriesForCloud = mergedEntries.map {
                    it.copy(profileId = profileId, isSynced = true).also { e -> e.tags = it.tags }
                }
                val serializedJournal = JournalSyncPayload.toJson(finalEntriesForCloud)
                val uploadId = if (profileId == SELF_PROFILE_ID) {
                    driveHelper.uploadJournalData(serializedJournal)
                } else {
                    driveHelper.uploadDataFile(
                        DriveServiceHelper.journalFileName(profileId),
                        serializedJournal
                    )
                }
                if (uploadId == null) {
                    Log.e("SyncWorker", "Cloud upload failed.")
                    return Result.retry()
                }

                // ============ Body measurements sync ============
                Log.d("SyncWorker", "Syncing body measurements...")
                val tombstonesFile = DriveServiceHelper.tombstonesFileName(profileId)
                val measurementsFile = DriveServiceHelper.measurementsFileName(profileId)

                val ledgerById = measurementDao.getAllDeletedEntries(profileId)
                    .associateBy { it.entry_id }
                    .toMutableMap()
                MeasurementTombstonePayload.fromJson(
                    driveHelper.downloadDataFile(tombstonesFile),
                    defaultProfileId = profileId
                ).filter { it.profileId == profileId }.forEach { remote ->
                    val local = ledgerById[remote.entry_id]
                    if (local == null || remote.deletedAt > local.deletedAt) {
                        val scopedRemote = remote.copy(profileId = profileId)
                        measurementDao.insertDeletedEntry(scopedRemote)
                        ledgerById[scopedRemote.entry_id] = scopedRemote
                    }
                }

                val cloudMeasurementsJson = driveHelper.downloadDataFile(measurementsFile)
                var cloudMeasurements = MeasurementSyncPayload.fromJson(
                    cloudMeasurementsJson,
                    defaultProfileId = profileId
                ).filter { it.profileId == profileId }
                if (cloudMeasurements.isNotEmpty() && ledgerById.isNotEmpty()) {
                    cloudMeasurements = cloudMeasurements.filterNot { m ->
                        val tombstone = ledgerById[m.entry_id]
                        tombstone != null && tombstone.deletedAt >= m.lastModified
                    }
                }

                val localMeasurementsAll = measurementDao.getAllEntriesList(profileId)
                val remotelyDeletedLocalIds = localMeasurementsAll.mapNotNull { m ->
                    val tombstone = ledgerById[m.entry_id]
                    when {
                        tombstone == null -> null
                        tombstone.deletedAt >= m.lastModified -> m.entry_id
                        else -> null
                    }
                }
                if (remotelyDeletedLocalIds.isNotEmpty()) {
                    measurementDao.deleteEntriesByIds(profileId, remotelyDeletedLocalIds)
                }
                val removedIds = remotelyDeletedLocalIds.toSet()
                val localMeasurements = localMeasurementsAll.filterNot { it.entry_id in removedIds }

                val mergedMeasurements = SyncMerge.mergeMeasurements(cloudMeasurements, localMeasurements)

                measurementDao.importAll(profileId, mergedMeasurements)

                val uploadedMeasurementId = driveHelper.uploadDataFile(
                    measurementsFile,
                    MeasurementSyncPayload.toJson(mergedMeasurements)
                )
                if (uploadedMeasurementId == null) {
                    Log.e("SyncWorker", "Body measurements upload failed.")
                    return Result.retry()
                }

                val uploadedLedgerId = driveHelper.uploadDataFile(
                    tombstonesFile,
                    MeasurementTombstonePayload.toJson(measurementDao.getAllDeletedEntries(profileId))
                )
                if (uploadedLedgerId == null) {
                    Log.e("SyncWorker", "Deletion ledger upload failed.")
                    return Result.retry()
                }

                // ============ Body measurement goals sync ============
                val goalsFile = DriveServiceHelper.goalsFileName(profileId)
                val cloudGoalsJson = driveHelper.downloadDataFile(goalsFile)
                val localGoals = goalDao.getAll(profileId)
                val parsedCloudGoals = GoalSyncPayload.fromJson(
                    cloudGoalsJson,
                    defaultProfileId = profileId
                ).filter { it.profileId == profileId }

                val mergedGoals = when {
                    cloudGoalsJson == null -> localGoals
                    parsedCloudGoals.isEmpty() && localGoals.isNotEmpty() -> localGoals
                    else -> SyncMerge.mergeGoals(parsedCloudGoals, localGoals)
                }

                goalDao.importAll(profileId, mergedGoals)

                val uploadedGoalsId = driveHelper.uploadDataFile(
                    goalsFile,
                    GoalSyncPayload.toJson(mergedGoals)
                )
                if (uploadedGoalsId == null) {
                    Log.e("SyncWorker", "Goals upload failed.")
                    return Result.retry()
                }

                // ============ Personal card sync ============
                Log.d("SyncWorker", "Syncing personal card...")
                val personalCardFile = DriveServiceHelper.personalCardFileName(profileId)
                val cloudCardJson = driveHelper.downloadDataFile(personalCardFile)
                val localCard = personalCardDao.getPersonalCardSnapshot(profileId, "personal_card")
                val localCards = listOfNotNull(localCard)
                val parsedCloudCards = PersonalCardSyncPayload.fromJson(
                    cloudCardJson,
                    defaultProfileId = profileId
                ).filter { it.profileId == profileId }

                val mergedCards = when {
                    cloudCardJson == null -> localCards
                    parsedCloudCards.isEmpty() && localCards.isNotEmpty() -> localCards
                    else -> SyncMerge.mergePersonalCard(parsedCloudCards, localCards)
                }

                mergedCards.forEach { personalCardDao.insertOrUpdate(it.copy(profileId = profileId)) }

                val uploadedCardId = driveHelper.uploadDataFile(
                    personalCardFile,
                    PersonalCardSyncPayload.toJson(mergedCards)
                )
                if (uploadedCardId == null) {
                    Log.e("SyncWorker", "Personal card upload failed.")
                    return Result.retry()
                }

                // 7. Purge expired tombstones for this profile after all pipelines finish
                repository.clearDeletedEntries(profileId)
            }

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

        /**
         * Downloads, merges, applies deletions, and uploads profile metadata
         * (`profiles.json`) before per-profile data sync runs.
         */
        suspend fun syncProfileMetadata(
            profileDao: ProfileDao,
            profileDeletionDao: ProfileDeletionDao,
            profileRepository: ProfileRepository,
            eraser: ProfileDataEraser,
            driveHelper: DriveServiceHelper
        ): List<ProfileEntity>? {
            val cloudProfilesJson = try {
                driveHelper.downloadDataFile(DriveServiceHelper.PROFILES_FILE)
            } catch (e: IOException) {
                throw e
            } catch (e: GoogleJsonResponseException) {
                throw e
            } catch (_: Throwable) {
                null
            }
            val cloudSnapshot = ProfileSyncPayload.fromJson(cloudProfilesJson)
            val localProfiles = profileDao.getAll()
            val localDeletions = profileDeletionDao.getAll()

            val mergeResult = SyncMerge.mergeProfiles(
                cloudProfiles = cloudSnapshot.profiles,
                localProfiles = localProfiles,
                cloudDeletions = cloudSnapshot.deletions,
                localDeletions = localDeletions
            )

            // Apply profile deletions (remote or local): switch active profile first if needed,
            // erase all local data for the deleted profile, and remove its cloud files.
            for (deletedId in mergeResult.deletedProfileIds) {
                if (profileRepository.currentProfileId() == deletedId) {
                    val replacement = mergeResult.profiles
                        .firstOrNull { !it.isArchived && it.id != deletedId }
                        ?.id ?: SELF_PROFILE_ID
                    val existingReplacement = profileDao.get(replacement)
                    if (existingReplacement == null) {
                        mergeResult.profiles.firstOrNull { it.id == replacement }?.let {
                            profileDao.upsert(it)
                        }
                    }
                    profileRepository.switchTo(replacement)
                }
                if (profileDao.get(deletedId) != null) {
                    eraser.erase(deletedId)
                }
                if (deletedId != SELF_PROFILE_ID) {
                    DriveServiceHelper.profileDataFileNames(deletedId).forEach { fileName ->
                        try {
                            driveHelper.deleteDataFile(fileName)
                        } catch (e: Exception) {
                            Log.e("SyncWorker", "Failed to delete cloud file $fileName: ${e.message}")
                        }
                    }
                }
            }

            mergeResult.deletions.forEach { marker ->
                profileDeletionDao.insert(marker)
            }

            val syncedProfiles = mergeResult.profiles.map {
                it.copy(isSynced = true, syncStatus = "SYNCED")
            }
            syncedProfiles.forEach { profile ->
                profileDao.upsert(profile)
            }
            profileRepository.ensureSelfProfile()

            val uploadedId = try {
                driveHelper.uploadDataFile(
                    DriveServiceHelper.PROFILES_FILE,
                    ProfileSyncPayload.toJson(syncedProfiles, mergeResult.deletions)
                )
            } catch (e: IOException) {
                throw e
            } catch (e: GoogleJsonResponseException) {
                throw e
            } catch (_: Throwable) {
                "fallback"
            }
            if (uploadedId == null) {
                return null
            }
            return profileDao.getAll()
        }
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
