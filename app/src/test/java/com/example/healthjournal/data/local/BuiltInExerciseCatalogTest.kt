package com.example.healthjournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for the built-in curated exercise catalog seed: it is a versioned
 * constants source (not a migration), covers a meaningful minimum set of
 * movements, every alternative mapping resolves to a real catalog row, and
 * seeding it twice is idempotent.
 */
class BuiltInExerciseCatalogTest {

    @Test
    fun seed_source_isVersioned() {
        assertTrue("Seed must carry a catalog version", BuiltInExerciseCatalog.CATALOG_VERSION > 0)
    }

    @Test
    fun seed_coversMinimumCatalogSize() {
        assertTrue(
            "Curated catalog must cover a useful minimum set, got ${BuiltInExerciseCatalog.ITEMS.size}",
            BuiltInExerciseCatalog.ITEMS.size >= 20
        )
    }

    @Test
    fun seed_hasUniqueStableIds() {
        val ids = BuiltInExerciseCatalog.ITEMS.map { it.id }
        assertEquals("Stable IDs must be unique", ids.size, ids.distinct().size)
    }

    @Test
    fun seed_everyAlternativeId_resolvesToACatalogRow() {
        val knownIds = BuiltInExerciseCatalog.ITEMS.map { it.id }.toSet()

        for (item in BuiltInExerciseCatalog.ITEMS) {
            for (alternativeId in item.alternativeIds) {
                assertTrue(
                    "Alternative '$alternativeId' on '${item.name}' must resolve to a real row",
                    alternativeId in knownIds
                )
            }
        }
    }

    @Test
    fun seed_everyRow_isBuiltIn() {
        assertTrue(BuiltInExerciseCatalog.ITEMS.all { !it.isUserAdded })
    }

    @Test
    fun seeding_isIdempotent() = runBlocking {
        val dao = FakeExerciseCatalogDao()

        ExerciseCatalogSeeder.seed(dao)
        val afterFirst = dao.visibleItems()

        ExerciseCatalogSeeder.seed(dao)
        val afterSecond = dao.visibleItems()

        assertEquals("Seeding twice must not change the catalog", afterFirst, afterSecond)
        assertEquals(BuiltInExerciseCatalog.ITEMS.size, afterFirst.size)
    }

    @Test
    fun seeding_preservesUserAddedExercises() = runBlocking {
        val dao = FakeExerciseCatalogDao()
        val userItem = ExerciseCatalogItem(
            name = "Goblet Squat",
            muscleCategory = "Quads",
            isUserAdded = true
        )
        dao.upsertExercise(userItem)

        ExerciseCatalogSeeder.seed(dao)

        assertTrue(
            "User-added exercises must survive seeding",
            dao.visibleItems().any { it.id == userItem.id && it.isUserAdded }
        )
    }
}