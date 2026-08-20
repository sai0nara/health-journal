package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JournalEntry::class, DeletedEntry::class, EntryTagCrossRef::class], version = 9, exportSchema = true)
@androidx.room.TypeConverters(JournalTypeConverters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

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

        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_database"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_6, MIGRATION_6_8, MIGRATION_8_9
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
