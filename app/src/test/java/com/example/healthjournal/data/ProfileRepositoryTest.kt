package com.example.healthjournal.data

import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProfileRepositoryTest {

    private val dao = FakeProfileDao()
    private val store = FakeProfileStore()
    private val eraser = FakeProfileDataEraser()
    private var now = 1_000L
    private val repository = ProfileRepository(
        profileDao = dao,
        settings = store,
        eraser = eraser,
        now = { now }
    )

    @Test
    fun ensureSelfProfile_createsMeAndPersistsIt() = runTest {
        repository.ensureSelfProfile()

        val self = dao.get(SELF_PROFILE_ID)
        assertEquals("Me", self?.name)
        assertEquals(ProfileRelationship.SELF.name, self?.relationship)
        assertEquals(SELF_PROFILE_ID, store.read())
        assertEquals(SELF_PROFILE_ID, repository.activeProfileId.value)
    }

    @Test
    fun ensureSelfProfile_isIdempotent() = runTest {
        repository.ensureSelfProfile()
        now = 2_000L
        repository.ensureSelfProfile()

        assertEquals(1, dao.getAll().count { it.relationship == ProfileRelationship.SELF.name })
    }

    @Test
    fun storedActiveProfile_isRestored() = runTest {
        dao.upsert(profile("a", "Alex"))
        dao.upsert(profile("b", "Blake"))
        store.write("b")

        repository.ensureSelfProfile()

        assertEquals("b", repository.activeProfileId.value)
    }

    @Test
    fun archivedStoredProfile_fallsBackToFirstActive() = runTest {
        dao.upsert(profile("a", "Alex"))
        dao.upsert(profile("b", "Blake").copy(isArchived = true))
        store.write("b")

        repository.ensureSelfProfile()

        assertEquals("a", repository.activeProfileId.value)
        assertEquals("a", store.read())
    }

    @Test
    fun create_rejectsBlankName() = runTest {
        repository.ensureSelfProfile()

        val result = repository.create("   ", ProfileRelationship.SIBLING, null)

        assertEquals(ProfileError.BlankName, result.exceptionOrNull())
    }

    @Test
    fun create_rejectsSecondSelf() = runTest {
        repository.ensureSelfProfile()

        val result = repository.create("Another me", ProfileRelationship.SELF, null)

        assertEquals(ProfileError.SelfExists, result.exceptionOrNull())
    }

    @Test
    fun create_allowsSelfWhenNoneExists() = runTest {
        val result = repository.create("Me again", ProfileRelationship.SELF, null)

        assertTrue(result.isSuccess)
        assertEquals(ProfileRelationship.SELF.name, result.getOrThrow().relationship)
    }

    @Test
    fun create_rejectsEleventhActiveProfile() = runTest {
        repository.ensureSelfProfile()
        repeat(9) { index -> repository.create("Sibling $index", ProfileRelationship.SIBLING, null) }

        val result = repository.create("Sibling 10", ProfileRelationship.SIBLING, null)

        assertEquals(ProfileError.ActiveLimit, result.exceptionOrNull())
    }

    @Test
    fun create_startsEmptyAndDoesNotBecomeActive() = runTest {
        repository.ensureSelfProfile()

        val created = repository.create("Alex", ProfileRelationship.SIBLING, 0xFF00FF00.toInt()).getOrThrow()

        assertEquals(SELF_PROFILE_ID, repository.activeProfileId.value)
        assertFalse(created.isArchived)
        assertEquals(0xFF00FF00.toInt(), created.accentColor)
    }

    @Test
    fun archive_activeProfile_switchesToAnotherActive() = runTest {
        repository.ensureSelfProfile()
        val second = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()

        val result = repository.archive(SELF_PROFILE_ID)

        assertTrue(result.isSuccess)
        assertTrue(dao.get(SELF_PROFILE_ID)!!.isArchived)
        assertEquals(second.id, repository.activeProfileId.value)
        assertEquals(second.id, store.read())
    }

    @Test
    fun archive_lastActiveProfile_fails() = runTest {
        repository.ensureSelfProfile()

        val result = repository.archive(SELF_PROFILE_ID)

        assertEquals(ProfileError.LastActive, result.exceptionOrNull())
        assertFalse(dao.get(SELF_PROFILE_ID)!!.isArchived)
    }

    @Test
    fun restore_isBlockedAtTenActiveProfiles() = runTest {
        repository.ensureSelfProfile()
        val archived = repository.create("Archived", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(archived.id)
        repeat(9) { index -> repository.create("Sibling $index", ProfileRelationship.SIBLING, null) }

        val result = repository.restore(archived.id)

        assertEquals(ProfileError.ActiveLimit, result.exceptionOrNull())
        assertTrue(dao.get(archived.id)!!.isArchived)
    }

    @Test
    fun deletePermanently_requiresArchivedProfile() = runTest {
        repository.ensureSelfProfile()
        val active = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()

        val result = repository.deletePermanently(active.id, "Alex")

        assertEquals(ProfileError.NotArchived, result.exceptionOrNull())
        assertTrue(eraser.erased.isEmpty())
    }

    @Test
    fun deletePermanently_requiresExactTypedName() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(profile.id)
        val canonical = dao.get(profile.id)!!.name

        val result = repository.deletePermanently(profile.id, "  $canonical  ")

        assertEquals(ProfileError.NameMismatch, result.exceptionOrNull())
        assertTrue(eraser.erased.isEmpty())
    }

    @Test
    fun deletePermanently_erasesDataAndProfile() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(profile.id)

        val result = repository.deletePermanently(profile.id, "Alex")

        assertTrue(result.isSuccess)
        assertEquals(listOf(profile.id), eraser.erased)
        assertEquals(null, dao.get(profile.id))
    }

    @Test
    fun deletePermanently_allowsDeletingArchivedSelfWhenAnotherActiveExists() = runTest {
        repository.ensureSelfProfile()
        val sibling = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(SELF_PROFILE_ID)

        val result = repository.deletePermanently(SELF_PROFILE_ID, "Me")

        assertTrue(result.isSuccess)
        assertEquals(listOf(SELF_PROFILE_ID), eraser.erased)
        assertEquals(null, dao.get(SELF_PROFILE_ID))
        assertEquals(1, dao.countActive())
    }

    @Test
    fun deletePermanently_isIdempotent() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(profile.id)

        val first = repository.deletePermanently(profile.id, "Alex")
        val second = repository.deletePermanently(profile.id, "Alex")

        assertTrue(first.isSuccess)
        assertEquals(ProfileError.NotFound, second.exceptionOrNull())
        assertEquals(listOf(profile.id), eraser.erased)
    }

    @Test
    fun switchTo_rejectsArchivedProfile() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()
        repository.archive(profile.id)

        val result = repository.switchTo(profile.id)

        assertEquals(ProfileError.Archived, result.exceptionOrNull())
        assertEquals(SELF_PROFILE_ID, repository.activeProfileId.value)
    }

    @Test
    fun switchTo_persistsChoice() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()

        val result = repository.switchTo(profile.id)

        assertTrue(result.isSuccess)
        assertEquals(profile.id, store.read())
        assertEquals(profile.id, repository.activeProfileId.value)
    }

    @Test
    fun update_rejectsSecondSelf() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()

        val result = repository.update(profile.id, "Alex", ProfileRelationship.SELF, null)

        assertEquals(ProfileError.SelfExists, result.exceptionOrNull())
    }

    @Test
    fun update_changesMetadataWithoutChangingIdentity() = runTest {
        repository.ensureSelfProfile()
        val profile = repository.create("Alex", ProfileRelationship.SIBLING, null).getOrThrow()

        val result = repository.update(profile.id, "Alexandra", ProfileRelationship.SIBLING, 0xFF123456.toInt())

        assertTrue(result.isSuccess)
        val updated = dao.get(profile.id)!!
        assertEquals("Alexandra", updated.name)
        assertEquals(0xFF123456.toInt(), updated.accentColor)
    }

    private fun profile(id: String, name: String): ProfileEntity = ProfileEntity(
        id = id,
        name = name,
        relationship = ProfileRelationship.SIBLING.name,
        createdAt = now,
        lastModified = now
    )
}

internal class FakeProfileStore(initial: String? = null) : ActiveProfileStore {
    private var value: String? = initial
    override fun read(): String? = value
    override fun write(profileId: String) { value = profileId }
}

internal typealias InMemoryActiveProfileStore = FakeProfileStore

internal class FakeProfileDataEraser(
    private val profileDao: ProfileDao? = null,
    private val deletionDao: com.example.healthjournal.data.local.ProfileDeletionDao? = null
) : ProfileDataEraser {
    val erased = mutableListOf<String>()
    val erasedIds: List<String> get() = erased
    override suspend fun erase(profileId: String) {
        erased += profileId
        deletionDao?.insert(com.example.healthjournal.data.local.ProfileDeletionEntity(profileId))
        profileDao?.delete(profileId)
    }
}

internal class FakeProfileDeletionDao : com.example.healthjournal.data.local.ProfileDeletionDao {
    private val store = linkedMapOf<String, com.example.healthjournal.data.local.ProfileDeletionEntity>()
    private val feed = MutableStateFlow<List<com.example.healthjournal.data.local.ProfileDeletionEntity>>(emptyList())

    override fun observeAll(): Flow<List<com.example.healthjournal.data.local.ProfileDeletionEntity>> = feed
    override suspend fun getAll(): List<com.example.healthjournal.data.local.ProfileDeletionEntity> =
        store.values.sortedBy { it.deletedAt }
    override suspend fun get(profileId: String): com.example.healthjournal.data.local.ProfileDeletionEntity? =
        store[profileId]
    override suspend fun insert(marker: com.example.healthjournal.data.local.ProfileDeletionEntity) {
        store[marker.profileId] = marker
        feed.value = store.values.sortedBy { it.deletedAt }
    }
    override suspend fun delete(profileId: String) {
        store.remove(profileId)
        feed.value = store.values.sortedBy { it.deletedAt }
    }
}

internal class FakeProfileDao : ProfileDao {
    private val store = linkedMapOf<String, ProfileEntity>()
    private val feed = MutableStateFlow<List<ProfileEntity>>(emptyList())

    override fun observeAll(): Flow<List<ProfileEntity>> = feed
    override fun observeActive(): Flow<List<ProfileEntity>> =
        feed.map { profiles -> profiles.filterNot { it.isArchived } }

    override suspend fun get(id: String): ProfileEntity? = store[id]
    override suspend fun getSelf(): ProfileEntity? =
        store.values.firstOrNull { it.relationship == ProfileRelationship.SELF.name }
    override suspend fun getFirstActive(): ProfileEntity? =
        store.values.filterNot { it.isArchived }.minByOrNull { it.name.lowercase() }
    override suspend fun countActive(): Int = store.values.count { !it.isArchived }
    override suspend fun getAll(): List<ProfileEntity> = store.values.toList()

    override suspend fun upsert(profile: ProfileEntity) {
        store[profile.id] = profile
        emit()
    }

    override suspend fun setArchived(id: String, archived: Boolean, lastModified: Long) {
        store[id]?.let { store[id] = it.copy(isArchived = archived, lastModified = lastModified) }
        emit()
    }

    override suspend fun delete(id: String) {
        store.remove(id)
        emit()
    }

    private fun emit() {
        feed.value = store.values.sortedWith(compareBy({ it.isArchived }, { it.name.lowercase() }))
    }
}
