package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.ActiveProfileStore
import com.example.healthjournal.data.ProfileDataEraser
import com.example.healthjournal.data.ProfileError
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeProfileDao
    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeProfileDao()
        repository = ProfileRepository(
            profileDao = dao,
            settings = FakeProfileStore(),
            eraser = FakeProfileDataEraser(),
            now = { 1_000L }
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProfileViewModel(repository, dispatcher)

    private suspend fun seedSelfAndSibling() {
        dao.upsert(
            ProfileEntity(
                id = SELF_PROFILE_ID,
                name = "Me",
                relationship = ProfileRelationship.SELF.name
            )
        )
        dao.upsert(
            ProfileEntity(
                id = "sibling",
                name = "Alex",
                relationship = ProfileRelationship.SIBLING.name
            )
        )
    }

    @Test
    fun state_loadsProfilesActiveProfileAndActiveCount() = runTest {
        seedSelfAndSibling()
        val vm = viewModel()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(listOf("Alex", "Me"), state.profiles.map { it.name })
        assertEquals("Me", state.activeProfile?.name)
        assertEquals(2, state.activeCount)
        assertFalse(state.isBusy)
    }

    @Test
    fun create_addsProfileAndClearsBusy() = runTest {
        dao.upsert(
            ProfileEntity(
                id = SELF_PROFILE_ID,
                name = "Me",
                relationship = ProfileRelationship.SELF.name
            )
        )
        val vm = viewModel()
        runCurrent()

        val result = vm.create("Sam", ProfileRelationship.CHILD, 0xFF00FF00.toInt())
        runCurrent()

        assertTrue(result.isSuccess)
        assertEquals(listOf("Me", "Sam"), vm.uiState.value.profiles.map { it.name })
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun failedCreate_setsProfileErrorAndClearsBusy() = runTest {
        val vm = viewModel()
        runCurrent()

        val result = vm.create("   ", ProfileRelationship.SIBLING, null)
        runCurrent()

        assertEquals(ProfileError.BlankName, result.exceptionOrNull())
        assertEquals(ProfileError.BlankName, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun busyState_isSetWhileWorkIsInFlight() = runTest {
        dao.upsertGate = CompletableDeferred()
        val vm = viewModel()
        runCurrent()

        val work = async { vm.create("Alex", ProfileRelationship.SIBLING, null) }
        runCurrent()
        assertTrue(vm.uiState.value.isBusy)

        dao.upsertGate?.complete(Unit)
        work.await()
        runCurrent()

        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun update_changesNameAndRelationship() = runTest {
        seedSelfAndSibling()
        val vm = viewModel()
        runCurrent()

        val result = vm.update("sibling", "Alexandra", ProfileRelationship.CHILD, null)
        runCurrent()

        assertTrue(result.isSuccess)
        val sibling = vm.uiState.value.profiles.first { it.id == "sibling" }
        assertEquals("Alexandra", sibling.name)
        assertEquals(ProfileRelationship.CHILD.name, sibling.relationship)
    }

    @Test
    fun archiveActiveProfile_switchesToFirstRemainingActive() = runTest {
        seedSelfAndSibling()
        val vm = viewModel()
        runCurrent()

        val result = vm.archive(SELF_PROFILE_ID)
        runCurrent()

        assertTrue(result.isSuccess)
        assertEquals("sibling", repository.currentProfileId())
        assertEquals(1, vm.uiState.value.activeCount)
        assertTrue(vm.uiState.value.profiles.first { it.id == SELF_PROFILE_ID }.isArchived)
    }

    @Test
    fun restore_archivedProfileWithoutSwitchingActive() = runTest {
        seedSelfAndSibling()
        repository.archive("sibling")
        val vm = viewModel()
        runCurrent()

        val result = vm.restore("sibling")
        runCurrent()

        assertTrue(result.isSuccess)
        assertEquals(2, vm.uiState.value.activeCount)
        assertFalse(vm.uiState.value.profiles.first { it.id == "sibling" }.isArchived)
        assertEquals(SELF_PROFILE_ID, repository.currentProfileId())
    }

    @Test
    fun deletePermanently_requiresExactTypedName() = runTest {
        seedSelfAndSibling()
        repository.archive("sibling")
        val vm = viewModel()
        runCurrent()

        val failure = vm.deletePermanently("sibling", "alex")
        val success = vm.deletePermanently("sibling", "Alex")
        runCurrent()

        assertEquals(ProfileError.NameMismatch, failure.exceptionOrNull())
        assertTrue(success.isSuccess)
        assertTrue(vm.uiState.value.profiles.none { it.id == "sibling" })
    }

    @Test
    fun switchTo_persistsActiveProfile() = runTest {
        seedSelfAndSibling()
        val vm = viewModel()
        runCurrent()

        val result = vm.switchTo("sibling")
        runCurrent()

        assertTrue(result.isSuccess)
        assertEquals("sibling", vm.uiState.value.activeProfile?.id)
    }

    @Test
    fun clearError_removesDisplayedError() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.create(" ", ProfileRelationship.SIBLING, null)

        vm.clearError()

        assertEquals(null, vm.uiState.value.error)
    }
}

private class FakeProfileStore : ActiveProfileStore {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(profileId: String) { value = profileId }
}

private class FakeProfileDataEraser : ProfileDataEraser {
    override suspend fun erase(profileId: String) = Unit
}

private class FakeProfileDao : ProfileDao {
    private val store = linkedMapOf<String, ProfileEntity>()
    private val feed = MutableStateFlow<List<ProfileEntity>>(emptyList())
    var upsertGate: CompletableDeferred<Unit>? = null

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
        upsertGate?.await()
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
