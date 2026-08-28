package com.example.healthjournal.export

import android.net.Uri
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.PersonalCardDao
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FullBackupUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()

    private fun mockDatabase(
        version: Int = 12,
        journalDao: JournalDao = mockk(),
        bodyMeasurementDao: BodyMeasurementDao = mockk()
    ): JournalDatabase {
        val db = mockk<JournalDatabase>()
        val helper = mockk<androidx.sqlite.db.SupportSQLiteOpenHelper>()
        val sqLiteDatabase = mockk<androidx.sqlite.db.SupportSQLiteDatabase>()
        coEvery { db.journalDao() } returns journalDao
        coEvery { db.bodyMeasurementDao() } returns bodyMeasurementDao
        coEvery { db.openHelper } returns helper
        coEvery { helper.readableDatabase } returns sqLiteDatabase
        coEvery { sqLiteDatabase.version } returns version
        return db
    }

    private fun readText(zip: File, name: String): String? {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry(name) ?: return null
            return zf.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }

    @Test
    fun execute_producesFullBackupWithAllEntitiesAndManifest() = runTest {
        val journalDao = mockk<JournalDao>()
        val bodyDao = mockk<BodyMeasurementDao>()
        val db = mockDatabase(version = 12, journalDao = journalDao, bodyMeasurementDao = bodyDao)

        val journalRepo = mockk<JournalRepository>()
        coEvery { journalRepo.getAllEntriesInDateRange(0L, Long.MAX_VALUE) } returns
            listOf(JournalEntry(entry_id = "e1", description = "x"))
        coEvery { journalDao.getAllDeletedEntries() } returns listOf(DeletedEntry("eDel"))
        coEvery { journalDao.getAllTags() } returns listOf(EntryTagCrossRef("e1", "health"))

        val bodyRepo = mockk<BodyMeasurementRepository>()
        coEvery { bodyRepo.allEntries } returns flowOf(listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)))
        coEvery { bodyDao.getAllDeletedEntries() } returns listOf(DeletedEntry("mDel"))

        val goalsRepo = mockk<GoalsRepository>()
        coEvery { goalsRepo.getAll() } returns listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L))

        val pcRepo = mockk<PersonalCardRepository>()
        val personalCardDao = mockk<PersonalCardDao>()
        coEvery { db.personalCardDao() } returns personalCardDao
        coEvery { pcRepo.getPersonalCardSnapshot() } returns PersonalCard(id = "personal_card")

        val useCase = FullBackupUseCase(
            database = db,
            journalRepository = journalRepo,
            bodyMeasurementRepository = bodyRepo,
            goalsRepository = goalsRepo,
            personalCardRepository = pcRepo,
            filesDir = tempFolder.root,
            exportsDir = tempFolder.root,
            gson = gson
        )

        val zip = useCase.execute()

        assertTrue(zip.exists())

        val manifest = gson.fromJson(readText(zip, BackupWriter.MANIFEST_NAME), BackupManifest::class.java)
        assertEquals(BackupWriter.BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(12, manifest.schemaVersion)

        val entries = gson.fromJson(readText(zip, "data.json"), Array<JournalEntry>::class.java)
        assertEquals(1, entries.size)

        val measurements = gson.fromJson(readText(zip, BackupWriter.EntityFile.BODY_MEASUREMENTS), Array<BodyMeasurementEntry>::class.java)
        assertEquals(1, measurements.size)

        val goals = gson.fromJson(readText(zip, BackupWriter.EntityFile.GOALS), Array<GoalEntity>::class.java)
        assertEquals(1, goals.size)

        val cards = gson.fromJson(readText(zip, BackupWriter.EntityFile.PERSONAL_CARD), Array<PersonalCard>::class.java)
        assertEquals(1, cards.size)

        val deleted = gson.fromJson(readText(zip, BackupWriter.EntityFile.DELETED_ENTRIES), Array<DeletedEntry>::class.java)
        assertEquals(2, deleted.size)

        val tags = gson.fromJson(readText(zip, BackupWriter.EntityFile.ENTRY_TAGS), Array<EntryTagCrossRef>::class.java)
        assertEquals(1, tags.size)
    }

    @Test
    fun execute_schemaVersionReflectsDatabase() = runTest {
        val db = mockDatabase(version = 9)
        val journalRepo = mockk<JournalRepository>()
        coEvery { journalRepo.getAllEntriesInDateRange(0L, Long.MAX_VALUE) } returns emptyList()
        coEvery { db.journalDao().getAllDeletedEntries() } returns emptyList()
        coEvery { db.journalDao().getAllTags() } returns emptyList()
        val bodyRepo = mockk<BodyMeasurementRepository>()
        coEvery { bodyRepo.allEntries } returns flowOf(emptyList())
        coEvery { db.bodyMeasurementDao().getAllDeletedEntries() } returns emptyList()
        val goalsRepo = mockk<GoalsRepository>()
        coEvery { goalsRepo.getAll() } returns emptyList()
        val pcRepo = mockk<PersonalCardRepository>()
        coEvery { pcRepo.getPersonalCardSnapshot() } returns null
        coEvery { db.personalCardDao() } returns mockk()

        val useCase = FullBackupUseCase(
            database = db,
            journalRepository = journalRepo,
            bodyMeasurementRepository = bodyRepo,
            goalsRepository = goalsRepo,
            personalCardRepository = pcRepo,
            filesDir = tempFolder.root,
            exportsDir = tempFolder.root,
            gson = gson
        )

        val zip = useCase.execute()
        val manifest = gson.fromJson(readText(zip, BackupWriter.MANIFEST_NAME), BackupManifest::class.java)
        assertEquals(9, manifest.schemaVersion)
    }

    @Test
    fun execute_includesMediaFilesReferencedByEntries() = runTest {
        val filesDir = File(tempFolder.root, "files")
        val photosDir = File(filesDir, "photos")
        photosDir.mkdirs()
        val photo = File(photosDir, "photo_test.jpg")
        photo.writeBytes(byteArrayOf(9, 9, 9))

        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        coEvery { Uri.parse(any()) } returns mockUri
        coEvery { mockUri.scheme } returns "file"
        coEvery { mockUri.path } returns photo.absolutePath

        val journalDao = mockk<JournalDao>()
        val db = mockDatabase(journalDao = journalDao)
        coEvery { journalDao.getAllDeletedEntries() } returns emptyList()
        coEvery { journalDao.getAllTags() } returns emptyList()

        val entry = JournalEntry(entry_id = "e1", description = "x")
            .copy(photo_urls = listOf("file://${photo.absolutePath}"))

        val journalRepo = mockk<JournalRepository>()
        coEvery { journalRepo.getAllEntriesInDateRange(0L, Long.MAX_VALUE) } returns listOf(entry)

        val bodyRepo = mockk<BodyMeasurementRepository>()
        coEvery { bodyRepo.allEntries } returns flowOf(emptyList())
        coEvery { db.bodyMeasurementDao().getAllDeletedEntries() } returns emptyList()

        val goalsRepo = mockk<GoalsRepository>()
        coEvery { goalsRepo.getAll() } returns emptyList()
        val pcRepo = mockk<PersonalCardRepository>()
        coEvery { pcRepo.getPersonalCardSnapshot() } returns null
        coEvery { db.personalCardDao() } returns mockk()

        val useCase = FullBackupUseCase(
            database = db,
            journalRepository = journalRepo,
            bodyMeasurementRepository = bodyRepo,
            goalsRepository = goalsRepo,
            personalCardRepository = pcRepo,
            filesDir = filesDir,
            exportsDir = tempFolder.root,
            gson = gson
        )

        val zip = useCase.execute()

        ZipFile(zip).use { zf ->
            val bytes = zf.getInputStream(zf.getEntry("media/photo_test.jpg")).use { it.readBytes() }
            assertNotNull(bytes)
            assertTrue(bytes.contentEquals(byteArrayOf(9, 9, 9)))
        }
        unmockkStatic(Uri::class)
    }
}
