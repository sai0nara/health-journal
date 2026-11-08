package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDeletionDao {

    @Query("SELECT * FROM profile_deletions ORDER BY deletedAt ASC")
    fun observeAll(): Flow<List<ProfileDeletionEntity>>

    @Query("SELECT * FROM profile_deletions ORDER BY deletedAt ASC")
    suspend fun getAll(): List<ProfileDeletionEntity>

    @Query("SELECT * FROM profile_deletions WHERE profileId = :profileId LIMIT 1")
    suspend fun get(profileId: String): ProfileDeletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(marker: ProfileDeletionEntity)

    @Query("DELETE FROM profile_deletions WHERE profileId = :profileId")
    suspend fun delete(profileId: String)
}
