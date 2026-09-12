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
}
