package com.example.healthjournal.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [ExerciseCatalogDao] for unit tests: behaves like Room for
 * upserts, by-id reads, name search, and bulk seeding.
 */
class FakeExerciseCatalogDao : ExerciseCatalogDao {

    private val store = linkedMapOf<String, ExerciseCatalogItem>()
    private val feed = MutableStateFlow<List<ExerciseCatalogItem>>(emptyList())

    override fun getAllExercises(): Flow<List<ExerciseCatalogItem>> = feed

    override suspend fun searchByName(query: String): List<ExerciseCatalogItem> =
        store.values.filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }

    override suspend fun getExerciseById(exerciseId: String): ExerciseCatalogItem? =
        store[exerciseId]

    override suspend fun getExercisesByIds(alternativeIds: List<String>): List<ExerciseCatalogItem> =
        alternativeIds.mapNotNull { store[it] }

    override suspend fun upsertExercise(item: ExerciseCatalogItem) {
        store[item.id] = item
        emit()
    }

    override suspend fun deleteExerciseById(exerciseId: String) {
        store.remove(exerciseId)
        emit()
    }

    override suspend fun insertAll(items: List<ExerciseCatalogItem>) {
        items.forEach { store[it.id] = it }
        emit()
    }

    override suspend fun clearAll() {
        store.clear()
        emit()
    }

    fun visibleItems(): List<ExerciseCatalogItem> = store.values.toList()

    private fun emit() {
        feed.value = store.values.sortedWith(compareBy({ it.muscleCategory }, { it.name }))
    }
}