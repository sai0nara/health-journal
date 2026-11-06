package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY isArchived ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE relationship = 'SELF' LIMIT 1")
    suspend fun getSelf(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC LIMIT 1")
    suspend fun getFirstActive(): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM profiles ORDER BY isArchived ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query(
        "UPDATE profiles SET isArchived = :archived, lastModified = :lastModified, " +
            "syncStatus = 'PENDING_SYNC' WHERE id = :id"
    )
    suspend fun setArchived(id: String, archived: Boolean, lastModified: Long)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)
}
