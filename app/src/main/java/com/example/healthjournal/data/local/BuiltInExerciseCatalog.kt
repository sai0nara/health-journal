package com.example.healthjournal.data.local

/**
 * Versioned constants source for the built-in curated exercise catalog. Not a
 * migration: seeding is re-runnable at startup and idempotent (stable ids,
 * REPLACE upsert), so the curated rows can grow without versioned schema churn.
 * User-created exercises are seeded after the built-ins and never overwritten.
 */
object BuiltInExerciseCatalog {

    /** Bump when the curated set or mappings change; drives re-seed detection. */
    const val CATALOG_VERSION: Int = 1

    private fun exercise(
        id: String,
        name: String,
        category: String,
        vararg alternatives: String
    ) = ExerciseCatalogItem(
        id = id,
        name = name,
        muscleCategory = category,
        alternativeIds = alternatives.toList()
    )

    /** Stable, curated set grouped by muscle category with alternative movements. */
    val ITEMS: List<ExerciseCatalogItem> = listOf(
        // Chest
        exercise("barbell-bench-press", "Barbell Bench Press", "Chest", "dumbbell-bench-press", "machine-chest-press"),
        exercise("dumbbell-bench-press", "Dumbbell Bench Press", "Chest", "barbell-bench-press", "machine-chest-press"),
        exercise("machine-chest-press", "Machine Chest Press", "Chest", "barbell-bench-press", "dumbbell-bench-press"),
        exercise("incline-barbell-press", "Incline Barbell Press", "Chest", "incline-dumbbell-press"),
        exercise("incline-dumbbell-press", "Incline Dumbbell Press", "Chest", "incline-barbell-press"),
        exercise("cable-fly", "Cable Fly", "Chest", "pec-deck"),
        exercise("pec-deck", "Pec Deck", "Chest", "cable-fly"),
        // Back
        exercise("barbell-row", "Barbell Row", "Back", "dumbbell-row", "seated-cable-row"),
        exercise("dumbbell-row", "Dumbbell Row", "Back", "barbell-row", "seated-cable-row"),
        exercise("seated-cable-row", "Seated Cable Row", "Back", "barbell-row", "dumbbell-row"),
        exercise("lat-pulldown", "Lat Pulldown", "Back", "pull-up"),
        exercise("pull-up", "Pull-Up", "Back", "lat-pulldown"),
        exercise("t-bar-row", "T-Bar Row", "Back", "barbell-row", "seated-cable-row"),
        // Shoulders
        exercise("overhead-press", "Overhead Press", "Shoulders", "seated-dumbbell-press", "machine-shoulder-press"),
        exercise("seated-dumbbell-press", "Seated Dumbbell Press", "Shoulders", "overhead-press", "machine-shoulder-press"),
        exercise("machine-shoulder-press", "Machine Shoulder Press", "Shoulders", "overhead-press", "seated-dumbbell-press"),
        exercise("lateral-raise", "Lateral Raise", "Shoulders", "cable-lateral-raise"),
        exercise("cable-lateral-raise", "Cable Lateral Raise", "Shoulders", "lateral-raise"),
        // Quads
        exercise("barbell-squat", "Barbell Squat", "Quads", "leg-press", "hack-squat", "front-squat"),
        exercise("front-squat", "Front Squat", "Quads", "barbell-squat", "leg-press"),
        exercise("leg-press", "Leg Press", "Quads", "barbell-squat", "hack-squat"),
        exercise("hack-squat", "Hack Squat", "Quads", "barbell-squat", "leg-press"),
        exercise("leg-extension", "Leg Extension", "Quads", "hack-squat"),
        exercise("goblet-squat", "Goblet Squat", "Quads", "barbell-squat", "leg-press"),
        // Hamstrings / Glutes
        exercise("romanian-deadlift", "Romanian Deadlift", "Hamstrings", "stiff-leg-deadlift", "leg-curl"),
        exercise("stiff-leg-deadlift", "Stiff-Leg Deadlift", "Hamstrings", "romanian-deadlift"),
        exercise("leg-curl", "Leg Curl", "Hamstrings", "romanian-deadlift"),
        exercise("conventional-deadlift", "Conventional Deadlift", "Glutes", "trap-bar-deadlift", "romanian-deadlift"),
        exercise("trap-bar-deadlift", "Trap-Bar Deadlift", "Glutes", "conventional-deadlift"),
        exercise("hip-thrust", "Hip Thrust", "Glutes", "barbell-glute-bridge"),
        exercise("barbell-glute-bridge", "Barbell Glute Bridge", "Glutes", "hip-thrust"),
        // Arms
        exercise("barbell-curl", "Barbell Curl", "Biceps", "dumbbell-curl", "cable-curl"),
        exercise("dumbbell-curl", "Dumbbell Curl", "Biceps", "barbell-curl", "cable-curl"),
        exercise("cable-curl", "Cable Curl", "Biceps", "dumbbell-curl", "barbell-curl"),
        exercise("close-grip-bench", "Close-Grip Bench", "Triceps", "dips", "lying-triceps-extension"),
        exercise("dips", "Dips", "Triceps", "close-grip-bench", "lying-triceps-extension"),
        exercise("lying-triceps-extension", "Lying Triceps Extension", "Triceps", "cable-triceps-pushdown"),
        exercise("cable-triceps-pushdown", "Cable Triceps Pushdown", "Triceps", "lying-triceps-extension"),
        // Core
        exercise("plank", "Plank", "Core", "cable-crunch"),
        exercise("cable-crunch", "Cable Crunch", "Core", "plank"),
        // Calves
        exercise("standing-calf-raise", "Standing Calf Raise", "Calves", "seated-calf-raise", "leg-press-calf-raise"),
        exercise("seated-calf-raise", "Seated Calf Raise", "Calves", "standing-calf-raise"),
        exercise("leg-press-calf-raise", "Leg Press Calf Raise", "Calves", "standing-calf-raise")
    )
}

/**
 * Re-runnable catalog seeder: inserts the curated rows with REPLACE semantics
 * (idempotent, stable ids) and never touches user-created rows. Called at DB
 * build time and after restore.
 */
object ExerciseCatalogSeeder {

    suspend fun seed(dao: ExerciseCatalogDao) {
        dao.insertAll(BuiltInExerciseCatalog.ITEMS)
    }
}