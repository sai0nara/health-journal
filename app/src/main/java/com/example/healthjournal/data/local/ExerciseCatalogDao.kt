package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCatalogDao {

    /** Reactive feed of the full catalog, grouped for muscle-category browsing. */
    @Query("SELECT * FROM exercise_catalog ORDER BY muscleCategory ASC, name ASC")
    fun getAllExercises(): Flow<List<ExerciseCatalogItem>>

    /** Search across exercise names for the preset-building dropdown. */
    @Query("SELECT * FROM exercise_catalog WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<ExerciseCatalogItem>

    @Query("SELECT * FROM exercise_catalog WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): ExerciseCatalogItem?

    /** Rows that reference ids in the given alternative-movement mappings. */
    @Query("SELECT * FROM exercise_catalog WHERE id IN (:alternativeIds)")
    suspend fun getExercisesByIds(alternativeIds: List<String>): List<ExerciseCatalogItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(item: ExerciseCatalogItem)

    @Query("DELETE FROM exercise_catalog WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: String)

    /** Bulk insert for full-backup restore and catalog seeding (identity-preserving). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseCatalogItem>)

    @Query("DELETE FROM exercise_catalog")
    suspend fun clearAll()
}