package com.example.healthjournal.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test-db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        JournalDatabase::class.java
    )

    @Test
    fun migrate8To9_CreatesEntryTagCrossRefTable() {
        helper.createDatabase(dbName, 8).apply {
            execSQL(
                "INSERT INTO journal_entries (entry_id, timestamp, lastModified, description, photo_urls, attachments, bp_systolic, bp_diastolic, heart_rate_avg, sleep_hours, ai_advice, isArchived, isSynced, syncStatus) " +
                    "VALUES ('migrated_id', 1000, 1000, 'Before migration', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'PENDING_SYNC')"
            )
            close()
        }

        helper.runMigrationsAndValidate(
            dbName,
            9,
            true,
            JournalDatabase.MIGRATION_8_9
        )
    }

    @Test
    fun migrate9To10_CreatesBodyMeasurementsTableAndPreservesJournalData() {
        helper.createDatabase("$dbName-9-10", 9).apply {
            execSQL(
                "INSERT INTO journal_entries (entry_id, timestamp, lastModified, description, photo_urls, attachments, bp_systolic, bp_diastolic, heart_rate_avg, sleep_hours, ai_advice, isArchived, isSynced, syncStatus) " +
                    "VALUES ('migrated_id_9_10', 1000, 1000, 'Before 9 to 10', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'PENDING_SYNC')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-9-10",
            10,
            true,
            JournalDatabase.MIGRATION_9_10
        )

        // Existing journal data must survive the migration untouched.
        db.query("SELECT entry_id FROM journal_entries WHERE entry_id = 'migrated_id_9_10'").use { cursor ->
            org.junit.Assert.assertTrue("Journal row lost during migration", cursor.moveToFirst())
        }

        // The new table must be usable: insert a partial measurement record.
        db.execSQL(
            "INSERT INTO body_measurements (entry_id, timestamp, lastModified, weight_kg, chest_cm, waist_cm, glute_cm, thigh_cm, calf_cm, bicep_cm, isSynced, syncStatus) " +
                "VALUES ('bm1', 2000, 2000, 78.5, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'PENDING_SYNC')"
        )
    }

    @Test
    fun migrate15To16_AddsIntervalStateColumnAndPreservesWorkoutData() {
        helper.createDatabase("$dbName-15-16", 15).apply {
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, notes, lastModified) " +
                    "VALUES ('migrated_ws_15_16', 'HIIT', 'ACTIVE', 1000, 0, '[{\"name\":\"Squat\",\"sets\":[]}]', 'Before 15 to 16', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-15-16",
            16,
            true,
            JournalDatabase.MIGRATION_15_16
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("intervalState column should be added", "intervalState" in columns)
            org.junit.Assert.assertTrue("setMatrix should survive", "setMatrix" in columns)
        }

        db.query("SELECT setMatrix FROM workout_sessions WHERE session_id = 'migrated_ws_15_16'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertTrue(cursor.getString(0).contains("Squat"))
        }

        db.execSQL(
            "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, intervalState, notes, lastModified) " +
                "VALUES ('ws_with_interval', 'HIIT', 'ACTIVE', 2000, 0, '{\"phase\":\"REST\",\"rounds\":2,\"intervals\":3}', '', 2000)"
        )
        db.query("SELECT intervalState FROM workout_sessions WHERE session_id = 'ws_with_interval'").use { cursor ->
            org.junit.Assert.assertTrue("intervalState should be readable", cursor.moveToFirst())
            org.junit.Assert.assertTrue(cursor.getString(0).contains("REST"))
        }
    }

    @Test
    fun migrate14To15_AddsSetMatrixColumnAndPreservesWorkoutData() {
        helper.createDatabase("$dbName-14-15", 14).apply {
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, notes, lastModified) " +
                    "VALUES ('migrated_ws', 'FITNESS', 'ACTIVE', 1000, 0, 'Before 14 to 15', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-14-15",
            15,
            true,
            JournalDatabase.MIGRATION_14_15
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("setMatrix column should be added", "setMatrix" in columns)
        }

        db.query("SELECT notes FROM workout_sessions WHERE session_id = 'migrated_ws'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Before 14 to 15", cursor.getString(0))
        }

        db.execSQL(
            "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, notes, lastModified) " +
                "VALUES ('ws_with_matrix', 'FITNESS', 'ACTIVE', 2000, 0, '[{\"name\":\"Squat\",\"sets\":[{\"kg\":60.0,\"reps\":10}]}]', '', 2000)"
        )
        db.query("SELECT setMatrix FROM workout_sessions WHERE session_id = 'ws_with_matrix'").use { cursor ->
            org.junit.Assert.assertTrue("setMatrix should be readable", cursor.moveToFirst())
            org.junit.Assert.assertTrue(cursor.getString(0).contains("Squat"))
        }
    }

    @Test
    fun migrate16To17_AddsPresetAndCatalogTablesAndPreservesWorkoutData() {
        helper.createDatabase("$dbName-16-17", 16).apply {
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, notes, lastModified) " +
                    "VALUES ('migrated_ws_16_17', 'FITNESS', 'ACTIVE', 1000, 0, '[{\"name\":\"Squat\",\"sets\":[]}]', 'Before 16 to 17', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-16-17",
            17,
            true,
            JournalDatabase.MIGRATION_16_17
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("intervalState should survive", "intervalState" in columns)
            org.junit.Assert.assertTrue("setMatrix should survive", "setMatrix" in columns)
        }

        db.query("SELECT setMatrix FROM workout_sessions WHERE session_id = 'migrated_ws_16_17'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertTrue(cursor.getString(0).contains("Squat"))
        }

        db.execSQL(
            "INSERT INTO workout_presets (id, name, scheduledDay, exercises, lastModified) " +
                "VALUES ('preset1', 'Leg Day', 'ANY', '[{\"exerciseId\":\"squat\",\"targetSets\":4,\"defaultReps\":8,\"defaultWeightKg\":60.0,\"restSeconds\":90}]', 2000)"
        )
        db.query("SELECT name FROM workout_presets WHERE id = 'preset1'").use { cursor ->
            org.junit.Assert.assertTrue("workout_presets should be usable", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Leg Day", cursor.getString(0))
        }

        db.execSQL(
            "INSERT INTO exercise_catalog (id, name, muscleCategory, alternativeIds, isUserAdded, lastModified) " +
                "VALUES ('squat', 'Barbell Squat', 'Quads', '[\"leg-press\"]', 0, 2000)"
        )
        db.query("SELECT muscleCategory FROM exercise_catalog WHERE id = 'squat'").use { cursor ->
            org.junit.Assert.assertTrue("exercise_catalog should be usable", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Quads", cursor.getString(0))
        }
    }

    @Test
    fun migrate17To18_AddsRoutineNameColumnAndPreservesWorkoutData() {
        helper.createDatabase("$dbName-17-18", 17).apply {
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, notes, lastModified) " +
                    "VALUES ('migrated_ws_17_18', 'FITNESS', 'ACTIVE', 1000, 0, '[{\"name\":\"Squat\",\"sets\":[]}]', 'Before 17 to 18', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-17-18",
            18,
            true,
            JournalDatabase.MIGRATION_17_18
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("routineName column should be added", "routineName" in columns)
            org.junit.Assert.assertTrue("setMatrix should survive", "setMatrix" in columns)
        }

        db.query("SELECT notes FROM workout_sessions WHERE session_id = 'migrated_ws_17_18'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Before 17 to 18", cursor.getString(0))
        }

        db.execSQL(
            "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, routineName, notes, lastModified) " +
                "VALUES ('ws_with_routine', 'FITNESS', 'ACTIVE', 2000, 0, '[{\"name\":\"Squat\",\"sets\":[]}]', 'Leg Day', '', 2000)"
        )
        db.query("SELECT routineName FROM workout_sessions WHERE session_id = 'ws_with_routine'").use { cursor ->
            org.junit.Assert.assertTrue("routineName should be readable", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Leg Day", cursor.getString(0))
        }
    }

    @Test
    fun migrate16To18_Chained_AddsRoutineNameAndPreservesWorkoutData() {
        helper.createDatabase("$dbName-16-18", 16).apply {
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, setMatrix, notes, lastModified) " +
                    "VALUES ('migrated_ws_16_18', 'FITNESS', 'ACTIVE', 1000, 0, '[{\"name\":\"Squat\",\"sets\":[]}]', 'Before 16 to 18', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-16-18",
            18,
            true,
            JournalDatabase.MIGRATION_16_17,
            JournalDatabase.MIGRATION_17_18
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("routineName column should be added", "routineName" in columns)
            org.junit.Assert.assertTrue("setMatrix should survive", "setMatrix" in columns)
        }

        db.query("SELECT notes FROM workout_sessions WHERE session_id = 'migrated_ws_16_18'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Before 16 to 18", cursor.getString(0))
        }
    }

    @Test
    fun migrate16To18_EmptyDatabase_OpensCleanWithRoutineNameColumn() {
        helper.createDatabase("$dbName-16-18-empty", 16).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-16-18-empty",
            18,
            true,
            JournalDatabase.MIGRATION_16_17,
            JournalDatabase.MIGRATION_17_18
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("routineName column should be added", "routineName" in columns)
        }

        db.query("SELECT COUNT(*) FROM workout_sessions").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate17To18_EmptyDatabase_OpensCleanWithRoutineNameColumn() {
        helper.createDatabase("$dbName-17-18-empty", 17).apply {
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-17-18-empty",
            18,
            true,
            JournalDatabase.MIGRATION_17_18
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertTrue("routineName column should be added", "routineName" in columns)
        }

        db.query("SELECT COUNT(*) FROM workout_sessions").use { cursor ->
            org.junit.Assert.assertTrue(cursor.moveToFirst())
            org.junit.Assert.assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate13To14_DropsOrphanedWorkoutSyncColumns() {
        // A real shipped-early v13 database carries isSynced and syncStatus on
        // workout_sessions; the exported 13.json snapshot only knows the clean
        // shape. Simulate that DB here, then assert the migration both removes
        // the columns and preserves the row data.
        helper.createDatabase("$dbName-13-14", 13).apply {
            execSQL("ALTER TABLE workout_sessions ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN syncStatus TEXT")
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, notes, lastModified, isSynced, syncStatus) " +
                    "VALUES ('early_v13', 'RUN', 'ACTIVE', 1000, 42, 'Before 13 to 14', 1000, 1, 'PENDING_SYNC')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-13-14",
            14,
            true,
            JournalDatabase.MIGRATION_13_14
        )

        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            org.junit.Assert.assertFalse("isSynced should be dropped", "isSynced" in columns)
            org.junit.Assert.assertFalse("syncStatus should be dropped", "syncStatus" in columns)
            org.junit.Assert.assertTrue(
                "workout_sessions data should survive the migration",
                "lastModified" in columns
            )
        }

        db.query("SELECT type, elapsedSeconds, notes FROM workout_sessions WHERE session_id = 'early_v13'").use { cursor ->
            org.junit.Assert.assertTrue("Previously inserted row lost", cursor.moveToFirst())
            org.junit.Assert.assertEquals("RUN", cursor.getString(0))
            org.junit.Assert.assertEquals(42L, cursor.getLong(1))
            org.junit.Assert.assertEquals("Before 13 to 14", cursor.getString(2))
        }
    }

    @Test
    fun migrate18To19_CanonicalizesTagValuesAndDedupesCrossRefs() {
        helper.createDatabase("$dbName-18-19", 18).apply {
            execSQL(
                "INSERT INTO journal_entries (entry_id, timestamp, lastModified, description, photo_urls, attachments, bp_systolic, bp_diastolic, heart_rate_avg, sleep_hours, ai_advice, isArchived, isSynced, syncStatus) " +
                    "VALUES ('mixed_tags', 1000, 1000, 'Mixed case tags', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'PENDING_SYNC')"
            )
            execSQL(
                "INSERT INTO journal_entries (entry_id, timestamp, lastModified, description, photo_urls, attachments, bp_systolic, bp_diastolic, heart_rate_avg, sleep_hours, ai_advice, isArchived, isSynced, syncStatus) " +
                    "VALUES ('other_tags', 2000, 2000, 'Already canonical', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 'PENDING_SYNC')"
            )
            execSQL(
                "INSERT INTO EntryTagCrossRef (entryId, tag) VALUES " +
                    "('mixed_tags', 'FITNESS'), ('mixed_tags', 'fitness'), ('mixed_tags', 'Fitness'), " +
                    "('mixed_tags', 'DOCTOR'), ('other_tags', 'sleep')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-18-19",
            19,
            true,
            JournalDatabase.MIGRATION_18_19
        )

        org.junit.Assert.assertEquals(listOf("doctor", "fitness"), tagsFor(db, "mixed_tags"))
        org.junit.Assert.assertEquals(listOf("sleep"), tagsFor(db, "other_tags"))
    }


    @Test
    fun migrate19To20_AddsWarmupColumnsAndPreservesRows() {
        helper.createDatabase("$dbName-19-20", 19).apply {
            execSQL(
                "INSERT INTO workout_presets (id, name, scheduledDay, exercises, lastModified) " +
                    "VALUES ('preset_warmup', 'Leg Day', 'ANY', '[]', 1000)"
            )
            execSQL(
                "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, notes, lastModified) " +
                    "VALUES ('ws_before_warmup', 'FITNESS', 'COMPLETED', 1000, 600, 'Before 19 to 20', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-19-20",
            20,
            true,
            JournalDatabase.MIGRATION_19_20
        )

        db.query("PRAGMA table_info(workout_presets)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            org.junit.Assert.assertTrue("warmupName column should be added", "warmupName" in columns)
            org.junit.Assert.assertTrue("warmupDurationSeconds column should be added", "warmupDurationSeconds" in columns)
        }
        db.query("PRAGMA table_info(workout_sessions)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            org.junit.Assert.assertTrue("warmupName column should be added", "warmupName" in columns)
            org.junit.Assert.assertTrue("warmupDurationSeconds column should be added", "warmupDurationSeconds" in columns)
            org.junit.Assert.assertTrue("warmupElapsedSeconds column should be added", "warmupElapsedSeconds" in columns)
            org.junit.Assert.assertTrue("warmupOutcome column should be added", "warmupOutcome" in columns)
        }

        db.query("SELECT name FROM workout_presets WHERE id = 'preset_warmup'").use { cursor ->
            org.junit.Assert.assertTrue("Preset row lost during migration", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Leg Day", cursor.getString(0))
        }
        db.query("SELECT notes FROM workout_sessions WHERE session_id = 'ws_before_warmup'").use { cursor ->
            org.junit.Assert.assertTrue("Session row lost during migration", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Before 19 to 20", cursor.getString(0))
        }

        db.execSQL(
            "INSERT INTO workout_presets (id, name, scheduledDay, exercises, warmupName, warmupDurationSeconds, lastModified) " +
                "VALUES ('preset_with_warmup', 'Push Day', 'ANY', '[]', 'Rowing', 300, 2000)"
        )
        db.query("SELECT warmupName, warmupDurationSeconds FROM workout_presets WHERE id = 'preset_with_warmup'").use { cursor ->
            org.junit.Assert.assertTrue("Warm-up columns should be usable", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Rowing", cursor.getString(0))
            org.junit.Assert.assertEquals(300, cursor.getInt(1))
        }

        db.execSQL(
            "INSERT INTO workout_sessions (session_id, type, status, startTimestamp, elapsedSeconds, warmupName, warmupDurationSeconds, warmupElapsedSeconds, warmupOutcome, notes, lastModified) " +
                "VALUES ('ws_with_warmup', 'FITNESS', 'COMPLETED', 2000, 600, 'Rowing', 300, 180, 'SKIPPED', '', 2000)"
        )
        db.query(
            "SELECT warmupName, warmupDurationSeconds, warmupElapsedSeconds, warmupOutcome " +
                "FROM workout_sessions WHERE session_id = 'ws_with_warmup'"
        ).use { cursor ->
            org.junit.Assert.assertTrue("Session warm-up columns should be usable", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Rowing", cursor.getString(0))
            org.junit.Assert.assertEquals(300, cursor.getInt(1))
            org.junit.Assert.assertEquals(180, cursor.getInt(2))
            org.junit.Assert.assertEquals("SKIPPED", cursor.getString(3))
        }
    }

    @Test
    fun migrate20To21_AddsVitalsColumnsAndPreservesRows() {
        helper.createDatabase("$dbName-20-21", 20).apply {
            execSQL(
                "INSERT INTO journal_entries (entry_id, timestamp, lastModified, description, bp_systolic, bp_diastolic, heart_rate_avg, isArchived, isSynced, syncStatus) " +
                    "VALUES ('entry_before_vitals', 1000, 1000, 'Before 20 to 21', 118.0, 78.0, 66, 0, 0, 'PENDING_SYNC')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "$dbName-20-21",
            21,
            true,
            JournalDatabase.MIGRATION_20_21
        )

        db.query("PRAGMA table_info(journal_entries)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            org.junit.Assert.assertTrue("blood_glucose_mmol column should be added", "blood_glucose_mmol" in columns)
            org.junit.Assert.assertTrue("body_temperature_c column should be added", "body_temperature_c" in columns)
            org.junit.Assert.assertTrue("spo2_percent column should be added", "spo2_percent" in columns)
        }

        db.query(
            "SELECT description, bp_systolic, bp_diastolic, heart_rate_avg, " +
                "blood_glucose_mmol, body_temperature_c, spo2_percent " +
                "FROM journal_entries WHERE entry_id = 'entry_before_vitals'"
        ).use { cursor ->
            org.junit.Assert.assertTrue("Journal row lost during migration", cursor.moveToFirst())
            org.junit.Assert.assertEquals("Before 20 to 21", cursor.getString(0))
            org.junit.Assert.assertEquals(118.0, cursor.getDouble(1), 0.001)
            org.junit.Assert.assertEquals(78.0, cursor.getDouble(2), 0.001)
            org.junit.Assert.assertEquals(66, cursor.getInt(3))
            org.junit.Assert.assertTrue("Existing row should keep null glucose", cursor.isNull(4))
            org.junit.Assert.assertTrue("Existing row should keep null temperature", cursor.isNull(5))
            org.junit.Assert.assertTrue("Existing row should keep null SpO2", cursor.isNull(6))
        }

        db.execSQL(
            "UPDATE journal_entries SET blood_glucose_mmol = 5.4, body_temperature_c = 36.6, " +
                "spo2_percent = 98 WHERE entry_id = 'entry_before_vitals'"
        )
        db.query(
            "SELECT blood_glucose_mmol, body_temperature_c, spo2_percent " +
                "FROM journal_entries WHERE entry_id = 'entry_before_vitals'"
        ).use { cursor ->
            org.junit.Assert.assertTrue("Vitals columns should be usable", cursor.moveToFirst())
            org.junit.Assert.assertEquals(5.4, cursor.getDouble(0), 0.001)
            org.junit.Assert.assertEquals(36.6, cursor.getDouble(1), 0.001)
            org.junit.Assert.assertEquals(98, cursor.getInt(2))
        }
    }

    private fun tagsFor(db: androidx.sqlite.db.SupportSQLiteDatabase, entryId: String): List<String> =
        db.query("SELECT tag FROM EntryTagCrossRef WHERE entryId = ? ORDER BY tag", arrayOf(entryId)).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
}
