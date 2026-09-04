package com.example.healthjournal.export

import android.app.Application
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val schemaVersion = 12
    private val application: Application = mockk(relaxed = true)

    private val sampleData = BackupData(
        journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x")),
        bodyMeasurements = listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)),
        goals = listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)),
        personalCards = listOf(PersonalCard(id = "pc")),
        deletedEntries = listOf(DeletedEntry(entry_id = "d1")),
        entryTags = listOf(EntryTagCrossRef("e1", "health"))
    )

    private fun buildBackup(): File {
        val zip = File(tempFolder.root, "backup_${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out ->
            BackupWriter(Gson(), schemaVersion).writeBackup(out, sampleData, emptyList())
        }
        return zip
    }

    private fun encrypt(plain: File, passphrase: String): File {
        val enc = File(tempFolder.root, "enc_${System.nanoTime()}.zip")
        BackupEncryptor().encrypt(plain, enc, passphrase)
        return enc
    }

    private fun newViewModel(restoreResult: RestoreResult? = null, restoreError: RestoreError? = null): RestoreViewModel {
        return RestoreViewModel(
            application = application,
            dispatcher = dispatcher,
            backupReader = BackupReader(Gson()),
            restoreToCompletion = { _, _, _ ->
                if (restoreError != null) throw restoreError
                restoreResult ?: RestoreResult(0, 0, 0, 0, 0, 0)
            }
        )
    }

    private fun capturedViewModel(): Pair<RestoreViewModel, MutableList<Pair<String, String?>>> {
        val captured = mutableListOf<Pair<String, String?>>()
        val vm = RestoreViewModel(
            application = application,
            dispatcher = dispatcher,
            backupReader = BackupReader(Gson()),
            restoreToCompletion = { _, uri, pass ->
                captured.add(uri to pass)
                RestoreResult(0, 0, 0, 0, 0, 0)
            }
        )
        return vm to captured
    }

    private fun RestoreViewModel.state() = uiState.value

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        val vm = newViewModel()
        assertEquals(RestoreUiState.Idle, vm.state())
    }

    @Test
    fun selectBackup_plainBackup_showsConfirmationWithMetadata() = runTest {
        val vm = newViewModel()
        val backup = buildBackup()

        vm.selectBackup("file://${backup.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state()
        assertTrue(state is RestoreUiState.ConfirmationRequired)
        val conf = state as RestoreUiState.ConfirmationRequired
        assertEquals(schemaVersion, conf.schemaVersion)
        assertEquals(false, conf.isEncrypted)
    }

    @Test
    fun selectBackup_encryptedBackup_withoutPassphrase_requestsPassphrase() = runTest {
        val vm = newViewModel()
        val encrypted = encrypt(buildBackup(), "secret")

        vm.selectBackup("file://${encrypted.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)
    }

    @Test
    fun onPassphraseSubmitted_correctPassphrase_showsEncryptedConfirmation() = runTest {
        val vm = newViewModel()
        val encrypted = encrypt(buildBackup(), "secret")

        vm.selectBackup("file://${encrypted.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)

        vm.submitPassphrase("secret")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state()
        assertTrue(state is RestoreUiState.ConfirmationRequired)
        assertEquals(true, (state as RestoreUiState.ConfirmationRequired).isEncrypted)
    }

    @Test
    fun onPassphraseSubmitted_wrongPassphrase_returnsToPassphrasePrompt() = runTest {
        val vm = newViewModel()
        val encrypted = encrypt(buildBackup(), "secret")

        vm.selectBackup("file://${encrypted.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        vm.submitPassphrase("wrong")
        dispatcher.scheduler.advanceUntilIdle()

        // A wrong passphrase returns to PassphraseRequired so the user can retry
        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)
    }

    @Test
    fun selectBackup_missingFile_showsError() = runTest {
        val vm = newViewModel()
        val missing = File(tempFolder.root, "does_not_exist.zip")

        vm.selectBackup("file://${missing.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state() is RestoreUiState.Error)
    }

    @Test
    fun confirmRestore_success_transitionsToSuccess() = runTest {
        val result = RestoreResult(1, 1, 1, 1, 1, 0)
        val vm = newViewModel(restoreResult = result)
        val backup = buildBackup()

        vm.selectBackup("file://${backup.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmRestore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RestoreUiState.Success(result), vm.state())
    }

    @Test
    fun confirmRestore_error_transitionsToError() = runTest {
        val vm = newViewModel(restoreError = RestoreError.CorruptedFile("boom"))
        val backup = buildBackup()

        vm.selectBackup("file://${backup.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmRestore()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state()
        assertTrue(state is RestoreUiState.Error)
        assertTrue((state as RestoreUiState.Error).error is RestoreError.CorruptedFile)
    }

    @Test
    fun reset_returnsToIdle() = runTest {
        val vm = newViewModel()
        val backup = buildBackup()

        vm.selectBackup("file://${backup.absolutePath}")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.ConfirmationRequired)

        vm.reset()
        assertEquals(RestoreUiState.Idle, vm.state())
    }

    @Test
    fun confirmRestore_passesOriginalUriAndPassphraseToWorker() = runTest {
        // Select an encrypted backup; the content URI must be preserved and forwarded
        // to the restore worker (which copies it itself), rather than a cache temp path
        // that the ViewModel could delete before the async worker runs.
        val (vm, captured) = capturedViewModel()
        val encrypted = encrypt(buildBackup(), "secret")
        val originalUri = "file://${encrypted.absolutePath}"

        vm.selectBackup(originalUri)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)

        vm.submitPassphrase("secret")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.ConfirmationRequired)

        vm.confirmRestore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, captured.size)
        // The URI forwarded to the worker must be the original selection, not a cache temp file.
        assertEquals(originalUri, captured.single().first)
        // The passphrase must be forwarded with the URI.
        assertEquals("secret", captured.single().second)
    }

    @Test
    fun confirmRestore_wrongPassphrase_letsUserRetryPassphrase() = runTest {
        val vm = newViewModel(restoreError = RestoreError.WrongPassphrase())
        val encrypted = encrypt(buildBackup(), "secret")
        val originalUri = "file://${encrypted.absolutePath}"

        vm.selectBackup(originalUri)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)

        vm.submitPassphrase("secret")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.ConfirmationRequired)

        vm.confirmRestore()
        dispatcher.scheduler.advanceUntilIdle()
        // Failure transitions back to PassphraseRequired so the retry path works via submitPassphrase
        assertTrue(vm.state() is RestoreUiState.PassphraseRequired)

        // Retrying validates the manifest against the ORIGINAL uri (no silent no-op)
        vm.submitPassphrase("secret")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state() is RestoreUiState.ConfirmationRequired)
    }
}
