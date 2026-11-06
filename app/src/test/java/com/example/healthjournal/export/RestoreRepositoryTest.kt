package com.example.healthjournal.export

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutPresetDao
import com.example.healthjournal.data.local.WorkoutSessionDao
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoreRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private data class Harness(
        val repo: RestoreRepository,
        val db: JournalDatabase,
        val journalDao: JournalDao,
        val fileName: String,
        val published: MutableList<List<JournalEntry>> = mutableListOf()
    )

    private fun mockRepository(filesDir: File, recordInsertAll: Boolean = false): Harness {
        val db = mockk<JournalDatabase>()
        val journalDao = mockk<JournalDao>()
        val bodyDao = mockk<BodyMeasurementDao>()
        val goalDao = mockk<GoalDao>()
        val personalCardDao = mockk<PersonalCardDao>()
        val workoutDao = mockk<WorkoutSessionDao>()
        val presetDao = mockk<WorkoutPresetDao>()
        coEvery { db.journalDao() } returns journalDao
        coEvery { db.bodyMeasurementDao() } returns bodyDao
        coEvery { db.goalDao() } returns goalDao
        coEvery { db.personalCardDao() } returns personalCardDao
        coEvery { db.workoutSessionDao() } returns workoutDao
        coEvery { db.workoutPresetDao() } returns presetDao

        coJustRun { journalDao.clearAllEntries(SELF_PROFILE_ID) }
        coJustRun { journalDao.clearAllDeletedEntries(SELF_PROFILE_ID) }
        coJustRun { journalDao.clearAllTags(SELF_PROFILE_ID) }
        coJustRun { journalDao.insertAllDeletedEntries(any()) }
        coJustRun { journalDao.insertAllTags(any()) }
        coJustRun { bodyDao.clearAll(SELF_PROFILE_ID) }
        coJustRun { bodyDao.replaceAll(any()) }
        coJustRun { goalDao.clear(SELF_PROFILE_ID) }
        coJustRun { goalDao.importAll(any(), any()) }
        coJustRun { personalCardDao.clearAll(SELF_PROFILE_ID) }
        coJustRun { personalCardDao.insertOrUpdate(any()) }
        coJustRun { workoutDao.clearAll(SELF_PROFILE_ID) }
        coJustRun { workoutDao.insertAll(any()) }
        coJustRun { presetDao.clearAll(SELF_PROFILE_ID) }
        coJustRun { presetDao.insertAll(any()) }

        val published = mutableListOf<List<JournalEntry>>()
        if (recordInsertAll) {
            coEvery { journalDao.insertAll(any()) } answers {
                published.add(firstArg())
            }
        } else {
            coJustRun { journalDao.insertAll(any()) }
        }

        val repo = RestoreRepository(db, filesDir, runInTransaction = { block -> block() })
        return Harness(repo, db, journalDao, fileName = "restore", published = published)
    }

    private val sampleData = BackupData(
        journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x")),
        bodyMeasurements = listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)),
        goals = listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)),
        personalCards = listOf(PersonalCard(id = "personal_card")),
        deletedEntries = listOf(DeletedEntry(entry_id = "eDel")),
        entryTags = listOf(EntryTagCrossRef("e1", "health")),
        workoutSessions = listOf(
            WorkoutSession(
                session_id = "w1",
                type = "RUN",
                startTimestamp = 10L,
                warmupName = "Rowing",
                warmupDurationSeconds = 300,
                warmupElapsedSeconds = 300,
                warmupOutcome = "COMPLETED"
            )
        ),
        presets = listOf(
            WorkoutPreset(id = "p1", name = "Leg day", warmupName = "Rowing", warmupDurationSeconds = 300)
        )
    )

    @Test
    fun restore_wipesAndInsertsAllEntitiesInTransaction() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val harness = mockRepository(filesDir)

        val result = harness.repo.restore(sampleData, mediaStagingDir = null)

        val journalDao = harness.db.journalDao()
        val bodyDao = harness.db.bodyMeasurementDao()
        val goalDao = harness.db.goalDao()
        val pcDao = harness.db.personalCardDao()
        val workoutDao = harness.db.workoutSessionDao()
        val presetDao = harness.db.workoutPresetDao()

        // Wipe
        coVerify { journalDao.clearAllEntries(SELF_PROFILE_ID) }
        coVerify { journalDao.clearAllDeletedEntries(SELF_PROFILE_ID) }
        coVerify { journalDao.clearAllTags(SELF_PROFILE_ID) }
        coVerify { bodyDao.clearAll(SELF_PROFILE_ID) }
        coVerify { goalDao.clear(SELF_PROFILE_ID) }
        coVerify { pcDao.clearAll(SELF_PROFILE_ID) }
        coVerify { workoutDao.clearAll(SELF_PROFILE_ID) }
        coVerify { presetDao.clearAll(SELF_PROFILE_ID) }
        // Insert
        coVerify { journalDao.insertAll(listOf(sampleData.journalEntries[0].copy(profileId = SELF_PROFILE_ID))) }
        coVerify { journalDao.insertAllDeletedEntries(listOf(sampleData.deletedEntries[0].copy(profileId = SELF_PROFILE_ID))) }
        coVerify { journalDao.insertAllTags(listOf(sampleData.entryTags[0])) }
        coVerify { bodyDao.replaceAll(listOf(sampleData.bodyMeasurements[0].copy(profileId = SELF_PROFILE_ID))) }
        coVerify { goalDao.importAll(SELF_PROFILE_ID, listOf(sampleData.goals[0])) }
        coVerify { pcDao.insertOrUpdate(sampleData.personalCards[0].copy(profileId = SELF_PROFILE_ID)) }
        coVerify { workoutDao.insertAll(listOf(sampleData.workoutSessions[0].copy(profileId = SELF_PROFILE_ID))) }
        coVerify { presetDao.insertAll(listOf(sampleData.presets[0].copy(profileId = SELF_PROFILE_ID))) }

        assertEquals(1, result.journalEntryCount)
        assertEquals(1, result.bodyMeasurementCount)
        assertEquals(1, result.goalCount)
        assertEquals(1, result.deletedEntryCount)
        assertEquals(1, result.tagCount)
        assertEquals(0, result.mediaFileCount)
        assertEquals(1, result.workoutCount)
        assertEquals(1, result.presetCount)
        assertEquals(7, result.totalRecords)
    }

    @Test
    fun restore_canonicalizesTagVariantsBeforeInsert() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val harness = mockRepository(filesDir)
        val data = BackupData(
            journalEntries = listOf(
                JournalEntry(entry_id = "e1", description = "x"),
                JournalEntry(entry_id = "e2", description = "y")
            ),
            entryTags = listOf(
                EntryTagCrossRef("e1", "FITNESS"),
                EntryTagCrossRef("e1", "fitness"),
                EntryTagCrossRef("e1", "Fitness"),
                EntryTagCrossRef("e2", "Sleep")
            )
        )

        val result = harness.repo.restore(data, mediaStagingDir = null)

        coVerify {
            harness.journalDao.insertAllTags(
                listOf(
                    EntryTagCrossRef("e1", "fitness"),
                    EntryTagCrossRef("e2", "sleep")
                )
            )
        }
        assertEquals(2, result.tagCount)
    }

    @Test
    fun restore_reimportsMedia_andRemapsPhotoAndAttachmentUris() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val staging = tempFolder.newFolder("stage")
        File(staging, "photo_a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(staging, "doc_1").writeBytes(byteArrayOf(9))

        val entry = JournalEntry(
            entry_id = "e1",
            description = "media",
            photo_urls = listOf("file:///data/0/pkg/files/photos/photo_a.jpg"),
            attachments = listOf(
                AttachmentData(name = "doc_1", uri = "file:///data/0/pkg/files/attachments/doc_1", mimeType = "text/plain")
            )
        )
        val data = BackupData(journalEntries = listOf(entry))

        val harness = mockRepository(filesDir, recordInsertAll = true)
        val result = harness.repo.restore(data, mediaStagingDir = staging)

        // Media copied into photos/ and attachments/
        val copiedPhoto = File(filesDir, "photos/photo_a.jpg")
        val copiedDoc = File(filesDir, "attachments/doc_1")
        assertTrue(copiedPhoto.exists())
        assertTrue(copiedDoc.exists())
        assertEquals(2, result.mediaFileCount)

        // URIs remapped to this device (captured via the publishing insertAll stub)
        val remapped = harness.published.single().single()
        assertEquals(listOf("file://${copiedPhoto.absolutePath}"), remapped.photo_urls)
        assertEquals("file://${copiedDoc.absolutePath}", remapped.attachments!![0].uri)
    }

    @Test
    fun restore_noStagingDir_skipsMediaImport() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val harness = mockRepository(filesDir)

        val result = harness.repo.restore(sampleData, mediaStagingDir = null)

        assertEquals(0, result.mediaFileCount)
    }

    @Test
    fun restore_transactionFailure_propagatesErrorAndLeavesDbToRollback() = runTest {
        val filesDir = tempFolder.newFolder("files")
        val db = mockk<JournalDatabase>()
        val journalDao = mockk<JournalDao>()
        coEvery { db.journalDao() } returns journalDao
        coJustRun { journalDao.clearAllEntries(SELF_PROFILE_ID) }

        val repo = RestoreRepository(db, filesDir, runInTransaction = { _ ->
            journalDao.clearAllEntries(SELF_PROFILE_ID)
            throw IOException("boom in transaction")
        })

        try {
            repo.restore(sampleData, mediaStagingDir = null)
            fail("Expected exception from transaction")
        } catch (e: IOException) {
            assertEquals("boom in transaction", e.message)
        }
    }

    @Test
    fun restore_v2ReplacesAllProfilesAndRemapsProfileScopedMedia() = runTest {
        val filesDir = tempFolder.newFolder("files_v2")
        val staging = tempFolder.newFolder("stage_v2")
        File(staging, "self_photo.jpg").writeBytes(byteArrayOf(1, 2))
        val sibStage = File(staging, "sib-1").apply { mkdirs() }
        File(sibStage, "sib_photo.jpg").writeBytes(byteArrayOf(3, 4))

        val db = mockk<JournalDatabase>(relaxed = true)
        val profileDao = mockk<ProfileDao>(relaxed = true)
        val journalDao = mockk<JournalDao>(relaxed = true)
        coEvery { db.profileDao() } returns profileDao
        coEvery { db.journalDao() } returns journalDao
        coEvery { profileDao.getAll() } returns listOf(
            ProfileEntity(id = SELF_PROFILE_ID, name = "Me", relationship = "SELF"),
            ProfileEntity(id = "old-sib", name = "Old", relationship = "SIBLING")
        )

        val insertedEntries = mutableListOf<List<JournalEntry>>()
        coEvery { journalDao.insertAll(any()) } answers {
            insertedEntries += firstArg<List<JournalEntry>>()
        }

        val v2Profiles = listOf(
            ProfileEntity(id = SELF_PROFILE_ID, name = "Me", relationship = "SELF"),
            ProfileEntity(id = "sib-1", name = "Alex", relationship = "SIBLING")
        )
        val v2Data = BackupData(
            profiles = v2Profiles,
            journalEntries = listOf(
                JournalEntry(
                    entry_id = "e-self",
                    profileId = SELF_PROFILE_ID,
                    description = "Self",
                    photo_urls = listOf("file:///old/photos/self_photo.jpg")
                ),
                JournalEntry(
                    entry_id = "e-sib",
                    profileId = "sib-1",
                    description = "Sibling",
                    photo_urls = listOf("file:///old/photos/sib-1/sib_photo.jpg")
                )
            )
        )

        val repo = RestoreRepository(db, filesDir, runInTransaction = { block -> block() })
        val result = repo.restore(v2Data, mediaStagingDir = staging)

        assertEquals(2, result.profileCount)
        coVerify { journalDao.clearAllEntries("old-sib") }
        coVerify { journalDao.clearAllEntries("sib-1") }
        coVerify { profileDao.delete("old-sib") }
        coVerify { profileDao.upsert(v2Profiles[0]) }
        coVerify { profileDao.upsert(v2Profiles[1]) }

        val byId = insertedEntries.single().associateBy { it.entry_id }
        assertTrue(File(filesDir, "photos/self_photo.jpg").exists())
        assertTrue(File(filesDir, "photos/sib-1/sib_photo.jpg").exists())
        assertEquals(
            listOf("file://${File(filesDir, "photos/self_photo.jpg").absolutePath}"),
            byId.getValue("e-self").photo_urls
        )
        assertEquals(
            listOf("file://${File(filesDir, "photos/sib-1/sib_photo.jpg").absolutePath}"),
            byId.getValue("e-sib").photo_urls
        )
    }

    @Test
    fun restore_v1BackupWipesOnlySelfAndPreservesOtherProfiles() = runTest {
        val filesDir = tempFolder.newFolder("files_v1")
        val harness = mockRepository(filesDir)

        val result = harness.repo.restore(sampleData, mediaStagingDir = null)

        assertEquals(1, result.profileCount)
        coVerify(exactly = 1) { harness.journalDao.clearAllEntries(SELF_PROFILE_ID) }
        coVerify(exactly = 0) { harness.journalDao.clearAllEntries("sib-1") }
    }
}
