package com.example.healthjournal.export

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseFileSwapperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeFile(dir: File, name: String, content: String): File {
        return File(dir, name).apply { writeText(content) }
    }

    private fun readText(f: File): String = f.readText()

    @Test
    fun swap_replacesDbShmAndWalWithStagedCopies() {
        val liveDir = tempFolder.newFolder("live")
        val stagedDir = tempFolder.newFolder("staged")

        val dbFile = writeFile(liveDir, "journal_database", "OLD-DB")
        val shmFile = writeFile(liveDir, "journal_database-shm", "OLD-SHM")
        val walFile = writeFile(liveDir, "journal_database-wal", "OLD-WAL")

        val stagedDb = writeFile(stagedDir, "journal_database", "NEW-DB")
        val stagedShm = writeFile(stagedDir, "journal_database-shm", "NEW-SHM")
        val stagedWal = writeFile(stagedDir, "journal_database-wal", "NEW-WAL")

        val swapper = DatabaseFileSwapper(dbFile)
        val snapshot = swapper.swap(stagedDb, stagedShm, stagedWal)

        assertEquals("NEW-DB", readText(dbFile))
        assertEquals("NEW-SHM", readText(shmFile))
        assertEquals("NEW-WAL", readText(walFile))

        // Rollback after swap
        swapper.rollback(snapshot)
        assertEquals("OLD-DB", readText(dbFile))
        assertEquals("OLD-SHM", readText(shmFile))
        assertEquals("OLD-WAL", readText(walFile))
    }

    @Test
    fun swap_backsUpOriginalForRollback() {
        val liveDir = tempFolder.newFolder("live")
        val stagedDir = tempFolder.newFolder("staged")
        val dbFile = writeFile(liveDir, "journal_database", "ORIGINAL")

        val swapper = DatabaseFileSwapper(dbFile)
        val snapshot = swapper.swap(writeFile(stagedDir, "journal_database", "NEW"), null, null)

        assertTrue(snapshot.databaseBackup.exists())
        assertEquals("ORIGINAL", readText(snapshot.databaseBackup))
    }

    @Test
    fun swap_noWalOrShm_handlesTheirAbsence() {
        val liveDir = tempFolder.newFolder("live")
        val stagedDir = tempFolder.newFolder("staged")
        val dbFile = writeFile(liveDir, "journal_database", "OLD")

        val swapper = DatabaseFileSwapper(dbFile)
        val snapshot = swapper.swap(writeFile(stagedDir, "journal_database", "NEW"), null, null)

        assertEquals("NEW", readText(dbFile))
        swapper.rollback(snapshot)
        assertEquals("OLD", readText(dbFile))
    }

    @Test
    fun swap_readOnlyGuard_rollsBackOnFailure() {
        // Simulate a failure by providing a directory in place of the staged db
        val liveDir = tempFolder.newFolder("live")
        val stagedDir = tempFolder.newFolder("staged")
        val badStaged = tempFolder.newFolder("bad_staged")
        val dbFile = writeFile(liveDir, "journal_database", "KEEP")

        val swapper = DatabaseFileSwapper(dbFile)
        var thrown = false
        try {
            swapper.swap(badStaged, null, null) // staged is a directory
            thrown = true
        } catch (e: Exception) {
            // expected
        }
        assertFalse("Should throw when staged file is not a file", thrown)
        // Original preserved
        assertEquals("KEEP", readText(dbFile))
    }
}
