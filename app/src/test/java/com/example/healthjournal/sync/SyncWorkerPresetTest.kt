package com.example.healthjournal.sync

import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncWorkerPresetTest {

    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var driveHelper: DriveServiceHelper

    private val pushExercise = PresetExercise(
        exerciseId = "bench-press",
        targetSets = 3,
        defaultReps = 8,
        defaultWeightKg = 60.0,
        restSeconds = 90
    )

    @Before
    fun setup() {
        presetDao = FakeWorkoutPresetDao()
        driveHelper = mockk(relaxed = true)
    }

    @Test
    fun firstSync_nullCloudFile_uploadsLocalPresets() = runBlocking {
        val local = WorkoutPreset(
            id = "p-local",
            name = "Push Day",
            scheduledDay = ScheduledDay.MONDAY.name,
            exercises = listOf(pushExercise),
            lastModified = 1_000L,
            profileId = SELF_PROFILE_ID
        )
        presetDao.upsertPreset(local)

        val uploadedSlot = slot<String>()
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE) } returns null
        coEvery {
            driveHelper.uploadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE, capture(uploadedSlot))
        } returns "presets-file-id"

        val ok = SyncWorker.syncWorkoutPresets(
            profileId = SELF_PROFILE_ID,
            presetDao = presetDao,
            ledgerById = emptyMap(),
            driveHelper = driveHelper
        )

        assertTrue(ok)
        val uploaded = PresetSyncPayload.fromJson(uploadedSlot.captured)
        assertEquals(listOf(local), uploaded)
    }

    @Test
    fun bidirectionalMerge_importsCloudPresetsAndResolvesLww() = runBlocking {
        val sibId = "sib-1"
        val presetsFile = DriveServiceHelper.presetsFileName(sibId)
        val localOlder = WorkoutPreset(
            id = "p-shared",
            name = "Old Name",
            lastModified = 1_000L,
            profileId = sibId
        )
        val localNew = WorkoutPreset(
            id = "p-local-only",
            name = "Local Only",
            lastModified = 1_500L,
            profileId = sibId
        )
        presetDao.upsertPreset(localOlder)
        presetDao.upsertPreset(localNew)

        val cloudNewer = WorkoutPreset(
            id = "p-shared",
            name = "Cloud Updated",
            lastModified = 2_000L,
            profileId = sibId
        )
        val cloudOnly = WorkoutPreset(
            id = "p-cloud-only",
            name = "Cloud Only",
            lastModified = 2_500L,
            profileId = sibId
        )

        val uploadedSlot = slot<String>()
        coEvery { driveHelper.downloadDataFile(presetsFile) } returns PresetSyncPayload.toJson(
            listOf(cloudNewer, cloudOnly)
        )
        coEvery {
            driveHelper.uploadDataFile(presetsFile, capture(uploadedSlot))
        } returns "sib-presets-id"

        val ok = SyncWorker.syncWorkoutPresets(
            profileId = sibId,
            presetDao = presetDao,
            ledgerById = emptyMap(),
            driveHelper = driveHelper
        )

        assertTrue(ok)
        val localAfter = presetDao.getAllPresets(sibId).first().associateBy { it.id }
        assertEquals(setOf("p-shared", "p-local-only", "p-cloud-only"), localAfter.keys)
        assertEquals("Cloud Updated", localAfter.getValue("p-shared").name)

        val uploadedById = PresetSyncPayload.fromJson(uploadedSlot.captured, defaultProfileId = sibId)
            .associateBy { it.id }
        assertEquals(setOf("p-shared", "p-local-only", "p-cloud-only"), uploadedById.keys)
    }

    @Test
    fun localTombstone_suppressesStaleCloudPreset_butPreservesNewerReEdit() = runBlocking {
        val staleDeleted = WorkoutPreset(
            id = "p-deleted",
            name = "Deleted Routine",
            lastModified = 1_000L,
            profileId = SELF_PROFILE_ID
        )
        val recreatedAfterDelete = WorkoutPreset(
            id = "p-recreated",
            name = "Recreated Routine",
            lastModified = 3_000L,
            profileId = SELF_PROFILE_ID
        )
        val ledger = mapOf(
            "p-deleted" to DeletedEntry("p-deleted", deletedAt = 2_000L, profileId = SELF_PROFILE_ID),
            "p-recreated" to DeletedEntry("p-recreated", deletedAt = 2_000L, profileId = SELF_PROFILE_ID)
        )

        val uploadedSlot = slot<String>()
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE) } returns
            PresetSyncPayload.toJson(listOf(staleDeleted, recreatedAfterDelete))
        coEvery {
            driveHelper.uploadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE, capture(uploadedSlot))
        } returns "presets-file-id"

        val ok = SyncWorker.syncWorkoutPresets(
            profileId = SELF_PROFILE_ID,
            presetDao = presetDao,
            ledgerById = ledger,
            driveHelper = driveHelper
        )

        assertTrue(ok)
        val localIds = presetDao.getAllPresets(SELF_PROFILE_ID).first().map { it.id }
        assertEquals(listOf("p-recreated"), localIds)
        val uploadedIds = PresetSyncPayload.fromJson(uploadedSlot.captured).map { it.id }
        assertEquals(listOf("p-recreated"), uploadedIds)
    }

    @Test
    fun remoteTombstone_deletesLocalPresetAndOmitsFromUpload() = runBlocking {
        val localStale = WorkoutPreset(
            id = "p-gone",
            name = "Should Be Erased",
            lastModified = 1_000L,
            profileId = SELF_PROFILE_ID
        )
        presetDao.upsertPreset(localStale)
        val ledger = mapOf(
            "p-gone" to DeletedEntry("p-gone", deletedAt = 2_000L, profileId = SELF_PROFILE_ID)
        )

        val uploadedSlot = slot<String>()
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE) } returns "[]"
        coEvery {
            driveHelper.uploadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE, capture(uploadedSlot))
        } returns "presets-file-id"

        val ok = SyncWorker.syncWorkoutPresets(
            profileId = SELF_PROFILE_ID,
            presetDao = presetDao,
            ledgerById = ledger,
            driveHelper = driveHelper
        )

        assertTrue(ok)
        assertTrue(presetDao.getAllPresets(SELF_PROFILE_ID).first().isEmpty())
        assertTrue(PresetSyncPayload.fromJson(uploadedSlot.captured).isEmpty())
    }

    @Test
    fun uploadFailure_returnsFalse() = runBlocking {
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE) } returns "[]"
        coEvery { driveHelper.uploadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE, any()) } returns null

        val ok = SyncWorker.syncWorkoutPresets(
            profileId = SELF_PROFILE_ID,
            presetDao = presetDao,
            ledgerById = emptyMap(),
            driveHelper = driveHelper
        )

        assertFalse(ok)
        coVerify { driveHelper.uploadDataFile(DriveServiceHelper.WORKOUT_PRESETS_FILE, "[]") }
    }
}
