package com.example.healthjournal.util

import com.example.healthjournal.data.ActiveProfileStore
import com.example.healthjournal.data.ProfileDataEraser
import com.example.healthjournal.data.local.ProfileDao
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProfileDao : ProfileDao {
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

class FakeProfileStore : ActiveProfileStore {
    private var value: String? = null
    override fun read(): String? = value
    override fun write(profileId: String) { value = profileId }
}

class FakeProfileDataEraser : ProfileDataEraser {
    override suspend fun erase(profileId: String) = Unit
}
