package com.example.healthjournal.data

import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed class ProfileError(message: String) : Exception(message) {
    object BlankName : ProfileError("Profile name must not be blank")
    object ActiveLimit : ProfileError("At most 10 profiles can be active")
    object SelfExists : ProfileError("A Self profile already exists")
    object LastActive : ProfileError("The last active profile cannot be removed")
    object NotArchived : ProfileError("A profile must be archived before deletion")
    object NameMismatch : ProfileError("The typed name does not match")
    object Archived : ProfileError("The profile is archived")
    object NotFound : ProfileError("Profile not found")
}

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val settings: ActiveProfileStore,
    private val eraser: ProfileDataEraser,
    private val now: () -> Long = System::currentTimeMillis
) : ActiveProfileProvider {

    private val mutex = Mutex()
    private val _activeProfileId = MutableStateFlow(
        settings.read()?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
    )

    override val activeProfileId: StateFlow<String> = _activeProfileId

    override fun currentProfileId(): String = _activeProfileId.value

    val profiles = profileDao.observeAll()
    val activeProfiles = profileDao.observeActive()
    val activeProfile = combine(profiles, _activeProfileId) { all, activeId ->
        all.firstOrNull { it.id == activeId && !it.isArchived }
            ?: all.firstOrNull { !it.isArchived }
    }

    suspend fun ensureSelfProfile(): ProfileEntity = mutex.withLock {
        val self = profileDao.getSelf() ?: createSelfProfile()
        val stored = settings.read()?.let { profileDao.get(it) }
        val active = stored?.takeIf { !it.isArchived } ?: profileDao.getFirstActive()
        if (active != null) {
            setActive(active.id)
        } else {
            setActive(self.id)
        }
        profileDao.getSelf() ?: self
    }

    suspend fun create(
        name: String,
        relationship: ProfileRelationship,
        accentColor: Int?
    ): Result<ProfileEntity> = mutex.withLock {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@withLock Result.failure(ProfileError.BlankName)
        if (relationship == ProfileRelationship.SELF && profileDao.getSelf() != null) {
            return@withLock Result.failure(ProfileError.SelfExists)
        }
        if (profileDao.countActive() >= MAX_ACTIVE_PROFILES) {
            return@withLock Result.failure(ProfileError.ActiveLimit)
        }
        val timestamp = now()
        val profile = ProfileEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            relationship = relationship.name,
            accentColor = accentColor,
            createdAt = timestamp,
            lastModified = timestamp
        )
        profileDao.upsert(profile)
        Result.success(profile)
    }

    suspend fun update(
        id: String,
        name: String,
        relationship: ProfileRelationship,
        accentColor: Int?
    ): Result<Unit> = mutex.withLock {
        val existing = profileDao.get(id) ?: return@withLock Result.failure(ProfileError.NotFound)
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@withLock Result.failure(ProfileError.BlankName)
        val self = profileDao.getSelf()
        if (relationship == ProfileRelationship.SELF && self != null && self.id != id) {
            return@withLock Result.failure(ProfileError.SelfExists)
        }
        profileDao.upsert(
            existing.copy(
                name = cleanName,
                relationship = relationship.name,
                accentColor = accentColor,
                lastModified = now(),
                syncStatus = "PENDING_SYNC"
            )
        )
        Result.success(Unit)
    }

    suspend fun archive(id: String): Result<Unit> = mutex.withLock {
        val existing = profileDao.get(id) ?: return@withLock Result.failure(ProfileError.NotFound)
        if (existing.isArchived) return@withLock Result.success(Unit)
        if (_activeProfileId.value == id) {
            val fallback = profileDao.observeActive().first().firstOrNull { it.id != id }
                ?: return@withLock Result.failure(ProfileError.LastActive)
            setActive(fallback.id)
        } else if (profileDao.countActive() == 0) {
            return@withLock Result.failure(ProfileError.LastActive)
        }
        profileDao.setArchived(id, true, now())
        Result.success(Unit)
    }

    suspend fun restore(id: String): Result<Unit> = mutex.withLock {
        val existing = profileDao.get(id) ?: return@withLock Result.failure(ProfileError.NotFound)
        if (!existing.isArchived) return@withLock Result.success(Unit)
        if (profileDao.countActive() >= MAX_ACTIVE_PROFILES) {
            return@withLock Result.failure(ProfileError.ActiveLimit)
        }
        profileDao.setArchived(id, false, now())
        Result.success(Unit)
    }

    suspend fun deletePermanently(id: String, typedName: String): Result<Unit> = mutex.withLock {
        val existing = profileDao.get(id) ?: return@withLock Result.failure(ProfileError.NotFound)
        if (!existing.isArchived) return@withLock Result.failure(ProfileError.NotArchived)
        if (typedName != existing.name) return@withLock Result.failure(ProfileError.NameMismatch)
        if (_activeProfileId.value == id) return@withLock Result.failure(ProfileError.LastActive)
        if (profileDao.countActive() == 0) return@withLock Result.failure(ProfileError.LastActive)
        eraser.erase(id)
        profileDao.delete(id)
        Result.success(Unit)
    }

    suspend fun switchTo(id: String): Result<Unit> = mutex.withLock {
        val profile = profileDao.get(id) ?: return@withLock Result.failure(ProfileError.NotFound)
        if (profile.isArchived) return@withLock Result.failure(ProfileError.Archived)
        setActive(id)
        Result.success(Unit)
    }

    private suspend fun createSelfProfile(): ProfileEntity {
        val timestamp = now()
        val self = ProfileEntity(
            id = SELF_PROFILE_ID,
            name = "Me",
            relationship = ProfileRelationship.SELF.name,
            createdAt = timestamp,
            lastModified = timestamp
        )
        profileDao.upsert(self)
        return self
    }

    private fun setActive(id: String) {
        _activeProfileId.value = id
        settings.write(id)
    }

    companion object {
        const val MAX_ACTIVE_PROFILES = 10
    }
}
