package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalCardDao {

    @Query("SELECT * FROM personal_card WHERE id = :id LIMIT 1")
    fun getPersonalCard(id: String): Flow<PersonalCard?>

    @Query("SELECT * FROM personal_card WHERE id = :id LIMIT 1")
    suspend fun getPersonalCardSnapshot(id: String): PersonalCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(card: PersonalCard)

    @Query("DELETE FROM personal_card WHERE id = :id")
    suspend fun deletePersonalCard(id: String)

    @Query("SELECT * FROM personal_card WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(): List<PersonalCard>

    @Query("UPDATE personal_card SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String)

    @Query("UPDATE personal_card SET syncStatus = 'PENDING_SYNC', lastModified = :lastModified WHERE id = :id")
    suspend fun markEntryDirty(id: String, lastModified: Long)
}
