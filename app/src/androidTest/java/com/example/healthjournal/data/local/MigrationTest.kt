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
        // A v13 database built by an early workout build carried isSynced and
        // syncStatus on workout_sessions; those columns were later declared
        // dead and removed from the entity. The 13->14 migration must drop
        // them so the persisted schema matches the recompiled v14 identity.
        helper.createDatabase("$dbName-13-14", 13)

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
                columns.contains("lastModified")
            )
        }
    }
}