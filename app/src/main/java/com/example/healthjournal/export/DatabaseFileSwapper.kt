package com.example.healthjournal.export

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Performs an atomic file-level replacement of a Room database (journal_database plus
 * its -shm and -wal siblings) from staged staging files, with a backup that supports
 * rollback on failure.
 *
 * This is a pure file utility used by the restore file-swap path. It does not hold any
 * Room database reference; callers are responsible for closing the database singleton
 * (see JournalDatabase.closeInstance()) before swapping and reopening afterwards.
 */
class DatabaseFileSwapper(
    private val dbFile: File,
) {

    /** Snapshot of the original database files, usable for rollback. */
    class SwapSnapshot internal constructor(
        val dbFile: File,
        val databaseBackup: File,
        val shmBackup: File?,
        val walBackup: File?,
    )

    /**
     * Backs up the current database files, then replaces them with the staged copies.
     *
     * @param stagedDb  the staged journal_database file
     * @param stagedShm the staged -shm file, or null if none
     * @param stagedWal the staged -wal file, or null if none
     * @return a [SwapSnapshot] capturing the original files for rollback
     * @throws IllegalStateException if a staged file is not an actual file
     */
    fun swap(
        stagedDb: File,
        stagedShm: File? = null,
        stagedWal: File? = null,
    ): SwapSnapshot {
        requireFile(stagedDb, "staged db")
        stagedShm?.let { requireFile(it, "staged -shm") }
        stagedWal?.let { requireFile(it, "staged -wal") }

        val shmFile = sibling("-shm")
        val walFile = sibling("-wal")

        val dbBackup = Files.createTempFile("swap-db-", ".bak").toFile()
            .apply { delete(); writeBytes(dbFile.readBytes()) }

        fun backupOf(f: File): File? {
            if (!f.exists()) return null
            return Files.createTempFile("swap-side-", ".bak").toFile()
                .apply { delete(); writeBytes(f.readBytes()) }
        }

        val shmBackup = backupOf(shmFile)
        val walBackup = backupOf(walFile)

        val snapshot = SwapSnapshot(dbFile, dbBackup, shmBackup, walBackup)
        try {
            copy(stagedDb, dbFile)
            if (stagedShm != null) copy(stagedShm, shmFile)
            if (stagedWal != null) copy(stagedWal, walFile)
        } catch (e: Exception) {
            rollback(snapshot)
            throw e
        }
        return snapshot
    }

    /** Restores the original database files captured in [snapshot]. */
    fun rollback(snapshot: SwapSnapshot) {
        copy(snapshot.databaseBackup, snapshot.dbFile)
        val shmFile = sibling("-shm")
        val walFile = sibling("-wal")
        if (snapshot.shmBackup != null) copy(snapshot.shmBackup, shmFile) else shmFile.delete()
        if (snapshot.walBackup != null) copy(snapshot.walBackup, walFile) else walFile.delete()
        snapshot.databaseBackup.delete()
        snapshot.shmBackup?.delete()
        snapshot.walBackup?.delete()
    }

    private val name: String
        get() = dbFile.name

    private fun sibling(suffix: String): File =
        File(dbFile.parentFile, name.replace(Regex("\\.db$"), "") + suffix)

    private fun requireFile(f: File, label: String) {
        if (!f.isFile) throw IllegalArgumentException("$label is not a file: ${f.path}")
    }

    private fun copy(from: File, to: File) {
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
