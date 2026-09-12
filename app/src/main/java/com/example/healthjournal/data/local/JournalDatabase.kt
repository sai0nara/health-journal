package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JournalEntry::class, DeletedEntry::class, EntryTagCrossRef::class, BodyMeasurementEntry::class, GoalEntity::class, PersonalCard::class, WorkoutSession::class], version = 16, exportSchema = true)
@androidx.room.TypeConverters(JournalTypeConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    abstract fun bodyMeasurementDao(): BodyMeasurementDao

    abstract fun goalDao(): GoalDao

    abstract fun personalCardDao(): PersonalCardDao

    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        /** Current Room database schema version; single-sourced for restore validation. */
        const val CURRENT_SCHEMA_VERSION: Int = 16

        // v1 -> v2: add isSynced
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v2 -> v3: photo_url -> photo_urls + attachments, add lastModified
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entries_new` (" +
                        "`entry_id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                        "`photo_urls` TEXT NOT NULL, `attachments` TEXT NOT NULL, " +
                        "`steps` INTEGER, `heart_rate_avg` INTEGER, `sleep_hours` REAL, " +
                        "`ai_advice` TEXT, `isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`entry_id`))"
                )
                database.execSQL(
                    "INSERT INTO `journal_entries_new` (`entry_id`, `timestamp`, `lastModified`, `description`, `photo_urls`, `attachments`, `steps`, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isSynced`) " +
                        "SELECT `entry_id`, `timestamp`, `timestamp`, `description`, '[]', '[]', `steps`, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isSynced` FROM `journal_entries`"
                )
                database.execSQL("DROP TABLE `journal_entries`")
                database.execSQL("ALTER TABLE `journal_entries_new` RENAME TO `journal_entries`")
            }
        }

        // v3 -> v4: steps -> bp_systolic + bp_diastolic
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entries_new` (" +
                        "`entry_id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                        "`photo_urls` TEXT NOT NULL, `attachments` TEXT NOT NULL, " +
                        "`bp_systolic` REAL, `bp_diastolic` REAL, `heart_rate_avg` INTEGER, " +
                        "`sleep_hours` REAL, `ai_advice` TEXT, `isSynced` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`entry_id`))"
                )
                database.execSQL(
                    "INSERT INTO `journal_entries_new` (`entry_id`, `timestamp`, `lastModified`, `description`, `photo_urls`, `attachments`, `bp_systolic`, `bp_diastolic`, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isSynced`) " +
                        "SELECT `entry_id`, `timestamp`, `lastModified`, `description`, `photo_urls`, `attachments`, NULL, NULL, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isSynced` FROM `journal_entries`"
                )
                database.execSQL("DROP TABLE `journal_entries`")
                database.execSQL("ALTER TABLE `journal_entries_new` RENAME TO `journal_entries`")
            }
        }

        // v4 -> v6: add isArchived + deleted_entries table
        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_entries` (`entry_id` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`entry_id`))"
                )
            }
        }

        // v6 -> v8: make list/archive/sync columns nullable, add syncStatus
        val MIGRATION_6_8 = object : Migration(6, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entries_new` (" +
                        "`entry_id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                        "`photo_urls` TEXT, `attachments` TEXT, `bp_systolic` REAL, " +
                        "`bp_diastolic` REAL, `heart_rate_avg` INTEGER, `sleep_hours` REAL, " +
                        "`ai_advice` TEXT, `isArchived` INTEGER, `isSynced` INTEGER, " +
                        "`syncStatus` TEXT, PRIMARY KEY(`entry_id`))"
                )
                database.execSQL(
                    "INSERT INTO `journal_entries_new` (`entry_id`, `timestamp`, `lastModified`, `description`, `photo_urls`, `attachments`, `bp_systolic`, `bp_diastolic`, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isArchived`, `isSynced`, `syncStatus`) " +
                        "SELECT `entry_id`, `timestamp`, `lastModified`, `description`, `photo_urls`, `attachments`, `bp_systolic`, `bp_diastolic`, `heart_rate_avg`, `sleep_hours`, `ai_advice`, `isArchived`, `isSynced`, 'PENDING_SYNC' FROM `journal_entries`"
                )
                database.execSQL("DROP TABLE `journal_entries`")
                database.execSQL("ALTER TABLE `journal_entries_new` RENAME TO `journal_entries`")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `EntryTagCrossRef` (`entryId` TEXT NOT NULL, `tag` TEXT NOT NULL, PRIMARY KEY(`entryId`, `tag`))"
                )
            }
        }

        // v9 -> v10: add body_measurements table for body-composition tracking
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_measurements` (" +
                        "`entry_id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `weight_kg` REAL, " +
                        "`chest_cm` REAL, `waist_cm` REAL, `glute_cm` REAL, " +
                        "`thigh_cm` REAL, `calf_cm` REAL, `bicep_cm` REAL, " +
                        "`isSynced` INTEGER, `syncStatus` TEXT, PRIMARY KEY(`entry_id`))"
                )
            }
        }

        // v10 -> v11: add goals table for Body Analytics per-parameter targets
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`parameterId` TEXT NOT NULL, `target` REAL NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, PRIMARY KEY(`parameterId`))"
                )
            }
        }

        // v11 -> v12: add personal_card table for medical profile
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `personal_card` (" +
                        "`id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, `isSynced` INTEGER, " +
                        "`syncStatus` TEXT, `demographics` TEXT NOT NULL, " +
                        "`medicalProfile` TEXT NOT NULL, `medicalHistory` TEXT NOT NULL, " +
                        "`emergencyContacts` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        // v12 -> v13: add workout_sessions table for workout tracking
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                        "`session_id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `startTimestamp` INTEGER NOT NULL, " +
                        "`endTimestamp` INTEGER, `elapsedSeconds` INTEGER NOT NULL, " +
                        "`targetDistanceM` REAL, `targetDurationMin` REAL, " +
                        "`calories` REAL, `notes` TEXT NOT NULL, " +
                        "`lastModified` INTEGER NOT NULL, PRIMARY KEY(`session_id`))"
                )
            }
        }

        // v13 -> v14: drop the orphaned isSynced/syncStatus columns that an
        // early workout build shipped on workout_sessions and that were later
        // declared dead; the schema changed at v13 without a version bump, so
        // this migration realigns on-disk DBs with the recompiled identity.
        // Recreate-and-copy, matching the pattern used by the older migrations,
        // because ALTER TABLE ... DROP COLUMN needs SQLite >= 3.35 and would
        // crash app startup on API 26-31 devices on the shipped-early build.
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val columns = database.query("PRAGMA table_info(`workout_sessions`)").use { cursor ->
                    buildSet {
                        while (cursor.moveToNext()) {
                            add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                        }
                    }
                }
                val hasOrphans = "isSynced" in columns || "syncStatus" in columns
                if (hasOrphans) {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `workout_sessions_new` (" +
                            "`session_id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                            "`status` TEXT NOT NULL, `startTimestamp` INTEGER NOT NULL, " +
                            "`endTimestamp` INTEGER, `elapsedSeconds` INTEGER NOT NULL, " +
                            "`targetDistanceM` REAL, `targetDurationMin` REAL, " +
                            "`calories` REAL, `notes` TEXT NOT NULL, " +
                            "`lastModified` INTEGER NOT NULL, PRIMARY KEY(`session_id`))"
                    )
                    database.execSQL(
                        "INSERT OR IGNORE INTO `workout_sessions_new` (`session_id`, `type`, `status`, `startTimestamp`, `endTimestamp`, `elapsedSeconds`, `targetDistanceM`, `targetDurationMin`, `calories`, `notes`, `lastModified`) " +
                            "SELECT `session_id`, `type`, `status`, `startTimestamp`, `endTimestamp`, `elapsedSeconds`, `targetDistanceM`, `targetDurationMin`, `calories`, `notes`, `lastModified` FROM `workout_sessions`"
                    )
                    database.execSQL("DROP TABLE `workout_sessions`")
                    database.execSQL("ALTER TABLE `workout_sessions_new` RENAME TO `workout_sessions`")
                }
            }
        }

        // v14 -> v15: add setMatrix column holding the strength set/reps matrix
        // (exercises with sets) as Gson JSON, keeping sets atomic with the session
        // row for crash recovery.
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `setMatrix` TEXT")
            }
        }

        // v15 -> v16: add intervalState column holding the manual HIIT interval
        // phase/round tracker as Gson JSON, persisted for crash recovery just
        // like the strength set matrix.
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `intervalState` TEXT")
            }
        }

        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_database"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_6, MIGRATION_6_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
