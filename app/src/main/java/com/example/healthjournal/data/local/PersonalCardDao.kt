package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalCardDao {

    @Query("SELECT * FROM personal_card WHERE profileId = :profileId AND id = :id LIMIT 1")
    fun getPersonalCard(profileId: String, id: String): Flow<PersonalCard?>

    @Query("SELECT * FROM personal_card WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getPersonalCardSnapshot(profileId: String, id: String): PersonalCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(card: PersonalCard)

    @Query("DELETE FROM personal_card WHERE profileId = :profileId AND id = :id")
    suspend fun deletePersonalCard(profileId: String, id: String)

    @Query("DELETE FROM personal_card WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String)

    @Query("SELECT * FROM personal_card WHERE profileId = :profileId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(profileId: String): List<PersonalCard>

    @Query("UPDATE personal_card SET syncStatus = :syncStatus WHERE profileId = :profileId AND id = :id")
    suspend fun updateSyncStatus(profileId: String, id: String, syncStatus: String)

    @Query(
        "UPDATE personal_card SET syncStatus = 'PENDING_SYNC', lastModified = :lastModified " +
            "WHERE profileId = :profileId AND id = :id"
    )
    suspend fun markEntryDirty(profileId: String, id: String, lastModified: Long)
}
