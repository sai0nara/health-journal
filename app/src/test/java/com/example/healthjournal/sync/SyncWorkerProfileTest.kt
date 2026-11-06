package com.example.healthjournal.sync

import com.example.healthjournal.data.FakeProfileDao
import com.example.healthjournal.data.FakeProfileDataEraser
import com.example.healthjournal.data.FakeProfileDeletionDao
import com.example.healthjournal.data.InMemoryActiveProfileStore
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncWorkerProfileTest {

    private lateinit var profileDao: FakeProfileDao
    private lateinit var deletionDao: FakeProfileDeletionDao
    private lateinit var store: InMemoryActiveProfileStore
    private lateinit var eraser: FakeProfileDataEraser
    private lateinit var profileRepository: ProfileRepository
    private lateinit var driveHelper: DriveServiceHelper

    @Before
    fun setup() {
        profileDao = FakeProfileDao()
        deletionDao = FakeProfileDeletionDao()
        store = InMemoryActiveProfileStore(SELF_PROFILE_ID)
        eraser = FakeProfileDataEraser(profileDao, deletionDao)
        profileRepository = ProfileRepository(profileDao, store, eraser)
        driveHelper = mockk(relaxed = true)
    }

    @Test
    fun syncProfiles_roundTripsAndMergesMetadataBeforeDataFiles() = runBlocking {
        profileRepository.ensureSelfProfile()
        val localSib = ProfileEntity(
            id = "sib-local",
            name = "Alex Local",
            relationship = ProfileRelationship.SIBLING.name,
            createdAt = 100L,
            lastModified = 1_000L
        )
        profileDao.upsert(localSib)

        val cloudSibUpdated = localSib.copy(name = "Alex Cloud", lastModified = 2_000L)
        val cloudNewSib = ProfileEntity(
            id = "sib-cloud",
            name = "Sam",
            relationship = ProfileRelationship.SIBLING.name,
            createdAt = 300L,
            lastModified = 3_000L
        )
        val cloudProfilesJson = ProfileSyncPayload.toJson(
            profiles = listOf(cloudSibUpdated, cloudNewSib),
            deletions = emptyList()
        )
        val uploadedProfilesSlot = slot<String>()

        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.PROFILES_FILE) } returns cloudProfilesJson
        coEvery {
            driveHelper.uploadDataFile(DriveServiceHelper.PROFILES_FILE, capture(uploadedProfilesSlot))
        } returns "profiles-file-id"

        val syncedProfiles = SyncWorker.syncProfileMetadata(
            profileDao = profileDao,
            profileDeletionDao = deletionDao,
            profileRepository = profileRepository,
            eraser = eraser,
            driveHelper = driveHelper
        )

        assertTrue(syncedProfiles != null)
        val byId = syncedProfiles!!.associateBy { it.id }
        assertTrue(SELF_PROFILE_ID in byId)
        assertEquals("Alex Cloud", byId.getValue("sib-local").name)
        assertEquals("Sam", byId.getValue("sib-cloud").name)

        val uploadedSnapshot = ProfileSyncPayload.fromJson(uploadedProfilesSlot.captured)
        assertEquals(setOf(SELF_PROFILE_ID, "sib-local", "sib-cloud"), uploadedSnapshot.profiles.map { it.id }.toSet())
    }

    @Test
    fun syncProfiles_remoteDeletion_switchesActiveProfile_erasesLocalData_andDeletesCloudFiles() = runBlocking {
        profileRepository.ensureSelfProfile()
        val activeSibling = ProfileEntity(
            id = "sib-del",
            name = "To Be Deleted",
            relationship = ProfileRelationship.SIBLING.name,
            isArchived = false,
            createdAt = 100L,
            lastModified = 1_000L
        )
        profileDao.upsert(activeSibling)
        profileRepository.switchTo("sib-del")
        assertEquals("sib-del", profileRepository.currentProfileId())

        val cloudJson = ProfileSyncPayload.toJson(
            profiles = emptyList(),
            deletions = listOf(ProfileDeletionEntity(profileId = "sib-del", deletedAt = 5_000L))
        )
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.PROFILES_FILE) } returns cloudJson
        coEvery { driveHelper.uploadDataFile(DriveServiceHelper.PROFILES_FILE, any()) } returns "profiles-file-id"
        coEvery { driveHelper.deleteDataFile(any()) } returns true

        val syncedProfiles = SyncWorker.syncProfileMetadata(
            profileDao = profileDao,
            profileDeletionDao = deletionDao,
            profileRepository = profileRepository,
            eraser = eraser,
            driveHelper = driveHelper
        )

        assertTrue(syncedProfiles != null)
        assertEquals(SELF_PROFILE_ID, profileRepository.currentProfileId())
        assertTrue("sib-del" in eraser.erasedIds)
        assertFalse(profileDao.getAll().any { it.id == "sib-del" })
        for (fileName in DriveServiceHelper.profileDataFileNames("sib-del")) {
            coVerify { driveHelper.deleteDataFile(fileName) }
        }
    }

    @Test
    fun syncProfiles_localDeletion_deletesPerProfileCloudFilesAndPublishesTombstone() = runBlocking {
        profileRepository.ensureSelfProfile()
        deletionDao.insert(ProfileDeletionEntity(profileId = "sib-gone", deletedAt = 4_000L))

        val uploadedSlot = slot<String>()
        coEvery { driveHelper.downloadDataFile(DriveServiceHelper.PROFILES_FILE) } returns null
        coEvery {
            driveHelper.uploadDataFile(DriveServiceHelper.PROFILES_FILE, capture(uploadedSlot))
        } returns "profiles-file-id"
        coEvery { driveHelper.deleteDataFile(any()) } returns true

        val syncedProfiles = SyncWorker.syncProfileMetadata(
            profileDao = profileDao,
            profileDeletionDao = deletionDao,
            profileRepository = profileRepository,
            eraser = eraser,
            driveHelper = driveHelper
        )

        assertEquals(listOf(SELF_PROFILE_ID), syncedProfiles?.map { it.id })
        val snapshot = ProfileSyncPayload.fromJson(uploadedSlot.captured)
        assertEquals(listOf("sib-gone"), snapshot.deletions.map { it.profileId })
        for (fileName in DriveServiceHelper.profileDataFileNames("sib-gone")) {
            coVerify { driveHelper.deleteDataFile(fileName) }
        }
    }
}
