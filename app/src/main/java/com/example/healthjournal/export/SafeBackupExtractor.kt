package com.example.healthjournal.export

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Safely extracts a plain full-backup ZIP into a target directory before the
 * database/media are swapped in, defending against two classic archive attacks:
 *
 *  - **Zip-Slip / path traversal**: any entry whose resolved canonical path
 *    escapes [targetDir] (via `..` segments, absolute paths, or symlink tricks)
 *    is rejected with [RestoreError.CorruptedFile].
 *  - **Zip bomb / excessive expansion**: cumulative uncompressed bytes exceeding
 *    [maxUncompressedBytes] are rejected with [RestoreError.InsufficientStorage].
 *
 * Extraction is streamed with a bounded buffer; the calling repository performs
 * the atomic staged swap and rollback.
 */
class SafeBackupExtractor(
    private val maxUncompressedBytes: Long = DEFAULT_MAX_UNCOMPRESSED_BYTES
) {

    /**
     * Extracts [backupZip]'s entries under [targetDir], returning the list of
     * entry names written.
     *
     * @throws RestoreError.CorruptedFile for unsafe (escaping) entries.
     * @throws RestoreError.InsufficientStorage if expansion exceeds the limit.
     */
    fun extract(backupZip: File, targetDir: File): List<String> {
        targetDir.mkdirs()
        val canonicalTarget = targetDir.canonicalFile
        val canonicalTargetPath = canonicalTarget.path + File.separator
        var totalUncompressed = 0L
        val written = mutableListOf<String>()

        try {
            ZipFile(backupZip).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val resolved = resolveSafePath(entry, targetDir, canonicalTarget, canonicalTargetPath)
                        ?: throw RestoreError.CorruptedFile("Unsafe archive entry rejected: ${entry.name}")

                    if (entry.isDirectory) {
                        resolved.mkdirs()
                        continue
                    }

                    resolved.parentFile?.mkdirs()
                    zf.getInputStream(entry).use { input ->
                        resolved.outputStream().use { out ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                out.write(buffer, 0, read)
                                totalUncompressed += read
                                if (totalUncompressed > maxUncompressedBytes) {
                                    throw RestoreError.InsufficientStorage()
                                }
                            }
                        }
                    }
                    written.add(entry.name)
                }
            }
        } catch (e: RestoreError.CorruptedFile) {
            throw e
        } catch (e: RestoreError.InsufficientStorage) {
            throw e
        } catch (e: IOException) {
            throw RestoreError.CorruptedFile("Failed to extract backup archive.", e)
        }

        return written
    }

    /**
     * Resolves [entry]'s name to a canonical [File] inside [targetDir], or null
     * if the name escapes the target directory (zip-slip / absolute path).
     */
    private fun resolveSafePath(
        entry: ZipEntry,
        targetDir: File,
        canonicalTarget: File,
        canonicalTargetPath: String
    ): File? {
        val name = entry.name
        if (name.startsWith("/")) return null
        // Reject any entry that, once canonicalized, is not under the target dir.
        val resolved = File(targetDir, name)
        val canonical = try {
            resolved.canonicalFile
        } catch (e: IOException) {
            return null
        }
        val canonicalPath = canonical.path
        if (canonicalPath != canonicalTarget.path && !canonicalPath.startsWith(canonicalTargetPath)) {
            return null
        }
        return resolved
    }

    companion object {
        private const val BUFFER_SIZE = 8192
        const val DEFAULT_MAX_UNCOMPRESSED_BYTES = 256L * 1024 * 1024 // 256 MB
    }
}
