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
}