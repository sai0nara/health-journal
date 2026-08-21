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
}