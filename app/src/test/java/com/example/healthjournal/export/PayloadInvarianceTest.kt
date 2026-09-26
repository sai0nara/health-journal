package com.example.healthjournal.export

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * P0 automation for Russian-localization payload invariance (R6/P7/T6).
 *
 * One independent test per case ID from
 * `qa-artifacts/2026-09-26-russian-localization/test-suite.md`; each test
 * builds its own temp dirs and its own mocked repository, so no mutable
 * state is shared between tests. No UI selectors: these cases assert the
 * data layer (ZIP `data.json` bytes + deserialized entities), which is the
 * JVM-observable contract for "user content is never translated".
 *
 * AAA shape per test: Arrange (entries + fakes) / Act (export + read back)
 * / Assert (UI-visible text + backend bytes where each applies; no HTTP
 * layer exists in this app).
 */
class PayloadInvarianceTest {

    private val gson = Gson()

    // ---------- helpers (pure, no shared state) ----------

    private fun repositoryReturning(entries: List<JournalEntry>): JournalRepository {
        val repository: JournalRepository = mockk()
        coEvery { repository.getAllEntriesInDateRange(any(), any()) } returns entries
        return repository
    }

    private fun exportDataJson(entries: List<JournalEntry>): File {
        val tempDir = Files.createTempDirectory("payload_invariance").toFile()
        val exportsDir = File(tempDir, "exports")
        exportsDir.mkdirs()
        val useCase = ZipExportUseCase(repositoryReturning(entries), exportsDir, gson)
        return runBlocking { useCase.execute(0L, Long.MAX_VALUE) }
    }

    private fun readDataJsonBytes(zip: File): ByteArray {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry("data.json")
                ?: error("ZIP has no data.json: ${zip.absolutePath}")
            return zf.getInputStream(entry).use { it.readBytes() }
        }
    }

    private fun catalogValue(catalog: String, key: String): String {
        val candidates = listOf(File("src/main/res"), File("app/src/main/res"))
        val resDir = candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${System.getProperty("user.dir")}")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "$catalog/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.attributes.getNamedItem("name").nodeValue == key) {
                return node.textContent
            }
        }
        error("Key $key not found in $catalog/strings.xml")
    }

    private fun descriptionsOf(dataJsonBytes: ByteArray): List<String> {
        val roundTripped = gson.fromJson(
            dataJsonBytes.toString(Charsets.UTF_8),
            Array<JournalEntry>::class.java
        )
        return roundTripped.map { it.description }
    }

    // ---------- TC-018: user content + ZIP byte-identical ----------

    @Test
    fun tc018_userContentAndZipPayload_areByteIdentical() {
        // Arrange: entries carrying English user data (names, medications, journal text)
        val entries = listOf(
            JournalEntry(entry_id = "tc018-1", description = "Morning run with Alex — took Aspirin 100mg"),
            JournalEntry(entry_id = "tc018-2", description = "Call Dr. Smith about Metformin refill")
        )

        // Act: export and read the raw payload bytes
        val payloadBytes = readDataJsonBytes(exportDataJson(entries))

        // Assert: backend bytes equal the source serialization exactly (no translation/mutation),
        // and the UI-visible text round-trips verbatim
        assertTrue(
            "ZIP data.json bytes must equal the source serialization",
            payloadBytes.contentEquals(gson.toJson(entries).toByteArray(Charsets.UTF_8))
        )
        assertEquals(
            entries.map { it.description },
            descriptionsOf(payloadBytes)
        )
    }

    // ---------- TC-019: translated/mutated payload rejected (T6 illegal) ----------

    @Test
    fun tc019_chromeCollidingUserText_isNeverTranslated_evenUnderRuLocale() {
        // Arrange: user text that collides with app chrome ("Settings" is a real
        // values/ key whose RU twin is "Настройки"); device simulated as ru
        val chromeKey = "settings_title"
        val userText = catalogValue("values", chromeKey)
        val ruTwin = catalogValue("values-ru", chromeKey)
        val entries = listOf(JournalEntry(entry_id = "tc019-1", description = "Reminder: open $userText after lunch"))
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale("ru", "RU"))
        try {
            // Act: export under the RU locale
            val payloadBytes = readDataJsonBytes(exportDataJson(entries))
            val payloadText = payloadBytes.toString(Charsets.UTF_8)

            // Assert: payload keeps the user's literal; the RU chrome twin never
            // leaks into user content (T6 corrupted state is never entered)
            assertTrue("payload must keep user literal", payloadText.contains(userText))
            assertFalse("RU chrome twin must not leak into user content", payloadText.contains(ruTwin))
            assertEquals(listOf(entries.single().description), descriptionsOf(payloadBytes))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    // ---------- TC-033: multi-byte Unicode + emoji intact ----------

    @Test
    fun tc033_unicodeAndEmojiContent_surviveExportUnchanged() {
        // Arrange: multi-byte Cyrillic + CJK + emoji (surrogate pairs) + combining mark
        val original = "Привет мир — 小心 \uD83D\uDE04 training e\u0301ndurance \uD83C\uDFC3"
        val entries = listOf(JournalEntry(entry_id = "tc033-1", description = original))

        // Act: export and read back
        val payloadBytes = readDataJsonBytes(exportDataJson(entries))

        // Assert: stored bytes unchanged and the string (incl. surrogate pairs) is exact
        assertTrue(
            "payload bytes must equal the source serialization",
            payloadBytes.contentEquals(gson.toJson(entries).toByteArray(Charsets.UTF_8))
        )
        val roundTripped = descriptionsOf(payloadBytes).single()
        assertEquals(original, roundTripped)
        assertEquals(original.codePointCount(0, original.length), roundTripped.codePointCount(0, roundTripped.length))
    }

    // ---------- TC-034: SQL-injection user text stays inert ----------

    @Test
    fun tc034_sqlInjectionUserText_staysInertLiteral() {
        // Arrange: classic injection payloads as plain journal text
        val payload1 = "'; DROP TABLE entries;--"
        val payload2 = "' UNION SELECT * FROM journal_entries --"
        val entries = listOf(
            JournalEntry(entry_id = "tc034-1", description = payload1),
            JournalEntry(entry_id = "tc034-2", description = payload2)
        )

        // Act: export (serialization boundary) and deserialize
        val payloadBytes = readDataJsonBytes(exportDataJson(entries))

        // Assert: literals preserved exactly (never executed/interpreted),
        // entry count unchanged, no exception thrown
        assertEquals(listOf(payload1, payload2), descriptionsOf(payloadBytes))
        assertTrue(
            "payload bytes must equal the source serialization",
            payloadBytes.contentEquals(gson.toJson(entries).toByteArray(Charsets.UTF_8))
        )
    }
}
