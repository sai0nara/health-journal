# Restore from Backup — Test Cases

> Maps the PRD's acceptance criteria and the PSD's edge cases to verification.
> Coverage splits into pure-JVM unit tests (the whole pipeline is coordinator-
> driven and runs without Android), and instrumented tests for the screen and
> the end-to-end database swap with real Room + MediaStore.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/export/RestoreViewModelTest.kt` | MVI transitions: picker→validating→confirm/passphrase→processing→success/error |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/RestoreCoordinatorTest.kt` | orchestration: plain success, encrypted success, wrong passphrase, version mismatch, unsupported archive, zip-slip, restore failure, staging cleanup |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` | atomic wipe+reinsert; media re-import + URI remap; no-op without staging; rollback on failure |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/SafeBackupExtractorTest.kt` | normal extraction; traversal / absolute-path entries rejected; per-entry and cumulative expansion caps |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupEncryptorTest.kt` | AES-256 container readable round-trip; wrong passphrase fails to open |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupReaderTest.kt` | plain vs encrypted manifest read; detection of the outer container |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` | staged archive data mapped to local DTOs |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` | real-DB atomic replace incl. media re-import; encrypted success; wrong passphrase leaves data intact; version mismatch leaves data intact |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenTest.kt` | idle/validating/confirmation/passphrase/error states render and buttons dispatch |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenThemeTest.kt` | light + dark rendering of key screen states |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-1 | Restore a plain archive | valid plain backup; app has existing data | all data replaced atomically; counts reported; media accessible |
| T-2 | AC-2 | Restore an encrypted archive | valid encrypted backup; correct passphrase | passphrase gate shown; restore succeeds |
| T-3 | AC-3 | Wrong passphrase | encrypted backup; wrong passphrase | `WrongPassphrase`; current data unchanged; retry offered |
| T-4 | AC-4 | Newer/unknown schema | archive with higher schema version | `VersionMismatch`; data unchanged |
| T-5 | AC-5 | Corrupt / foreign file | non-backup or truncated `.zip` | `CorruptedFile`/`UnsupportedFormat`; data unchanged |
| T-6 | AC-6 | Zip-slip / zip-bomb | archive with `../` entry or over-limit expansion | extraction rejected; no partial writes; data unchanged |
| T-7 | AC-7 | Post-restore sync | successful restore; Drive configured | manual sync enqueued after worker success |
| T-8 | AC-8 | Media remap | archive with photos/attachments | files reappear via remapped local URIs |
| T-9 | NFR-atomicity | Swap failure rolls back | injected failure during the transaction | Room rolls back; data intact |
| T-10 | FR-5 | Process death mid-restore | kill/restart app while worker runs | WorkManager resumes; swap stays atomic |

## Manual checks

- Pick a backup through the real SAF dialog: the MIME filter (`.zip`) behaves on
  at least one emulator and one physical device.
- Restore on a device then observe the Drive re-sync in the sync log.
- Restore a large archive (photos included) and confirm storage headroom
  guidance when the device is nearly full.
- Restore twice back-to-back: two successful replaces on the same install.

## Sources

- `app/src/test/java/com/example/healthjournal/export/RestoreViewModelTest.kt` — ViewModel transitions.
- `app/src/test/java/com/example/healthjournal/export/RestoreCoordinatorTest.kt` — pipeline orchestration.
- `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` — atomic swap + media.
- `app/src/test/java/com/example/healthjournal/export/SafeBackupExtractorTest.kt` — unzip guards.
- `app/src/test/java/com/example/healthjournal/export/BackupEncryptorTest.kt` — container cryptography.
- `app/src/test/java/com/example/healthjournal/export/BackupReaderTest.kt` — manifest/detection.
- `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` — DTO mapping.
- `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` — end-to-end database replace.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenTest.kt` — screen states + callbacks.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenThemeTest.kt` — light/dark rendering.
- `Docs/prd/restore-from-backup.md` — requirements under test.
- `Docs/psd/restore-from-backup.md` — design the cases verify.