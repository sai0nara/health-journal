package com.example.healthjournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the ExerciseCatalogItem entity defaults: a catalog row is a
 * built-in or user-added movement with a muscle category and mapped
 * alternative-movement ids, generated identity and a lastModified timestamp.
 */
class ExerciseCatalogItemTest {

    @Test
    fun freshCatalogItem_userAdded_hasGeneratedId() {
        val item = ExerciseCatalogItem(name = "Goblet Squat", muscleCategory = "Quads", isUserAdded = true)

        assertTrue(item.id.isNotBlank())
        assertEquals("Goblet Squat", item.name)
        assertEquals("Quads", item.muscleCategory)
        assertTrue(item.isUserAdded)
        assertTrue(item.alternativeIds.isEmpty())
    }

    @Test
    fun freshCatalogItem_defaultsToBuiltIn() {
        val item = ExerciseCatalogItem(name = "Barbell Squat", muscleCategory = "Quads")

        assertFalse("Default catalog rows are built-in", item.isUserAdded)
        assertTrue(item.alternativeIds.isEmpty())
    }

    @Test
    fun catalogItem_keepsConfiguredAlternativeMappings() {
        val item = ExerciseCatalogItem(
            name = "Barbell Bench Press",
            muscleCategory = "Chest",
            alternativeIds = listOf("dumbbell-press", "machine-press")
        )

        assertEquals(listOf("dumbbell-press", "machine-press"), item.alternativeIds)
    }

    @Test
    fun freshCatalogItem_timestampDefaultsToNow() {
        val before = System.currentTimeMillis()
        val item = ExerciseCatalogItem(name = "Romanian Deadlift", muscleCategory = "Hamstrings")
        val after = System.currentTimeMillis()

        assertTrue(item.lastModified in before..after)
    }
}