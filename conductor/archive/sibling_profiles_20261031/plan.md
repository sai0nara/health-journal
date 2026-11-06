# Track sibling_profiles_20261031 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a default Self profile and multiple sibling/dependent profiles, with
every health record, sync payload, backup, restore, PDF export, and Health
Connect import attributed to exactly one profile.

**Architecture:** Keep one Room database. Add a `profiles` table, a profile
deletion ledger, and a non-null `profileId` owner on every scoped table.
Repositories resolve the active profile from a shared `ActiveProfileProvider`
and expose profile-scoped flows; sync/backup/export call explicit-profile
methods. A top-bar `ProfileSwitcher` and a `ManageProfilesScreen` drive the
active-profile preference. The ZIP format remains the all-profile full backup;
PDF export selects active profiles.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room with exported schemas,
Coroutines/Flow, MVVM with manual ViewModel factories, WorkManager, Google Drive
sync, iText PDF, WorkManager restore.

**Spec:** `conductor/tracks/sibling_profiles_20261031/spec.md`

**Prerequisite:** PR #49 (manual vitals) must be merged first. It takes the
schema to 21. This plan assumes the profile migration is `21 → 22`; if the
schema is not 21 when implementation starts, stop and rebase before changing
`JournalDatabase.kt`.

## Global Constraints

- Export `JAVA_HOME` before every Gradle command:
  `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home`.
- Follow red → green → refactor → UI test for every task. Do not write production
  code before its failing test exists.
- Run `./gradlew build` and `./gradlew :app:testDebugUnitTest` after each task.
  Run instrumented tests only on a connected device or emulator; shut the
  emulator down afterwards. Use:
  `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.TestClass>`.
- Run a single JVM test with:
  `./gradlew :app:testDebugUnitTest --tests "com.example.healthjournal.data.ProfileRepositoryTest"`.
- Run `python3 wiki/lint.py` at the end of every phase; exit 0 is required.
- Commit only the files named in the task. Never stage `opencode.json`, drafts,
  or unrelated working-tree changes.
- Use `SELF_PROFILE_ID = "self"` for the initial `Me` profile and for legacy
  rows/payloads that have no profile owner.
- Every scoped query and write takes an explicit profile id at the DAO boundary;
  no `getAll()` without a profile may remain.
- The exercise catalog, unit system, theme, account, and device settings stay
  shared.
- Existing sync domains are journal, measurements, goals, and personal card.
  Workouts and presets remain backup-only in this track; do not add a new
  workout sync feature.
- ZIP is the all-profile full backup. PDF is the active/selected-profile report.
- All new user-facing strings go in both `values/strings.xml` and
  `values-ru/strings.xml`.
- At the end of each phase, run `python3 wiki/lint.py` and leave it at exit 0.

## Phase 1: Data foundation

- [x] Task: Confirm the schema baseline
    - [x] Fetch and rebase the branch on `origin/main` after PR #49 is merged.
    - [x] Confirm `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` reports version 21 and `app/schemas/.../21.json` exists.
    - [x] If the baseline is still 20, stop; do not create a competing `20 → 21` migration.
    - [x] Record the current schema version in the task notes and use `21 → 22` for this track.

- [x] Task: Add the profile model, preference, and repository (Red/Green/Refactor)
    - [x] Write failing `ProfileRepositoryTest` cases for: fresh `ensureSelfProfile()` creates `Me`; a stored active id is restored; an archived stored id falls back to an active profile; create rejects blank names and the 11th active profile; a second `Self` is rejected; archive of the active profile switches first; the last active profile cannot be archived; restore is blocked at 10 active profiles; permanent delete requires an archived profile and exact typed name; the last active profile cannot be deleted.
    - [x] Run `./gradlew :app:testDebugUnitTest --tests "com.example.healthjournal.data.ProfileRepositoryTest"` and confirm the tests fail because the types do not exist.
    - [x] Create `data/local/ProfileEntity.kt` with `id`, `name`, `relationship`, `accentColor`, `isArchived`, `createdAt`, `lastModified`, `isSynced`, and `syncStatus`; add `ProfileRelationship` (`SELF`, `SIBLING`, `CHILD`, `PARENT`, `OTHER`) and `SELF_PROFILE_ID`.
    - [x] Create `data/local/ProfileDao.kt` with `observeAll()`, `observeActive()`, `get(id)`, `getSelf()`, `countActive()`, `upsert(profile)`, `setArchived(id, archived, lastModified)`, and `delete(id)`.
    - [x] Create `data/local/ProfileDeletionEntity.kt` and `ProfileDeletionDao.kt` with a `profile_deletions` table keyed by `profileId` and `deletedAt`; expose `observeAll()`, `insert(marker)`, `get(profileId)`, and `delete(profileId)`.
    - [x] Create `data/ActiveProfileStore.kt` mirroring `UnitSettings.kt`: an `ActiveProfileStore` interface plus a `SharedPreferences` implementation with `read()` and `write(profileId)`.
    - [x] Add `data/ActiveProfileProvider.kt` with `val activeProfileId: StateFlow<String>` and `fun currentProfileId(): String`.
    - [x] Create `data/ProfileRepository.kt`: implements `ActiveProfileProvider`; owns a `Mutex`; exposes `profiles`, `activeProfiles`, `activeProfile`, `ensureSelfProfile()`, `create(name, relationship, color)`, `update(...)`, `archive(id)`, `restore(id)`, `deletePermanently(id, typedName)`, and `switchTo(id)`. Persist every active switch through `ActiveProfileStore`.
    - [x] Create `data/ProfileDataEraser.kt` with `suspend fun erase(profileId: String)`; implement `RoomProfileDataEraser` using `JournalDatabase.withTransaction` to delete all scoped rows and tags for that profile, insert the profile-deletion marker, and remove the profile row.
    - [x] Run the tests to green; add tests for preference persistence and fallback.
    - [x] Refactor `ProfileRepository` error handling into a sealed `ProfileError` (`BlankName`, `ActiveLimit`, `SelfExists`, `LastActive`, `NotArchived`, `NameMismatch`, `NotFound`).
    - [x] Commit only the new profile files and their tests.

- [x] Task: Add profile ownership to entities and the `21 → 22` migration (Red/Green)
    - [x] Write failing `MigrationTest` cases: `migrate21To22_AddsProfilesAndBackfillsEveryScopedTable` and `migrate20To22_ChainsVitalsAndProfilesMigration`. Fresh-install `Self` creation is covered by `ProfileRepositoryTest.ensureSelfProfile_*`.
    - [x] Seed v21 rows in journal entries, tags, measurements, goals, personal card, workout sessions, presets, and tombstones; assert every migrated row has `profileId = "self"` and that goals/cards/tombstones keep their original keys.
    - [x] Run the migration test on an emulator; confirm it fails because the database is still 21.
    - [x] Add `profileId: String` to `JournalEntry`, `BodyMeasurementEntry`, `GoalEntity`, `PersonalCard`, `WorkoutSession`, `WorkoutPreset`, and `DeletedEntry`.
    - [x] Change `GoalEntity` primary key to `(profileId, parameterId)`, `PersonalCard` to `(profileId, id)`, and `DeletedEntry` to `(profileId, entry_id)`.
    - [x] Add `(profileId, timestamp)` and `(profileId, isArchived)` indices where applicable; change preset index to `(profileId, name)` and add profile/status/start indices for sessions.
    - [x] Add `ProfileEntity` and `ProfileDeletionEntity` to the `@Database` entity list and add DAO accessors.
    - [x] Bump `JournalDatabase` version and `CURRENT_SCHEMA_VERSION` to 22 and add `MIGRATION_21_22`.
    - [x] Migration steps: create `profiles`; insert `('self', 'Me', 'SELF', ..., 0)`; create `profile_deletions`; add `profileId TEXT NOT NULL DEFAULT 'self'` to journal entries, measurements, workout sessions, and presets; recreate `goals`, `personal_card`, and `deleted_entries` with the new composite keys and copy every row with `'self'`; add the new indices.
    - [x] Register `MIGRATION_21_22` in `addMigrations`.
    - [x] Fix every constructor and fixture compile error; use `SELF_PROFILE_ID` for migration fixtures and explicit profile ids for production writes.
    - [x] Run the migration test on an emulator and `./gradlew :app:testDebugUnitTest` to green.
    - [x] Generate `app/schemas/com.example.healthjournal.data.local.JournalDatabase/22.json` with the migration.
    - [x] Commit the entity, database, and migration-test changes.

- [x] Task: Scope every DAO (Red/Green)
    - [x] Write failing tests in `JournalDaoTest`, `BodyMeasurementDaoTest`, `GoalDaoTest`, `PersonalCardDaoTest`, and `WorkoutDaoTest`: profile A rows are absent from profile B reads; by-id reads return null for a foreign profile; tag lists join through `journal_entries`; two profiles can store the same goal parameter and card, and each can own an unfinished session; tombstones are profile-scoped; `clearAll` and `removeDeletedEntriesBefore` affect only one profile.
    - [x] Run the DAO tests and confirm the failures are missing profile predicates.
    - [x] Add `profileId` as the first parameter to every scoped read/write in `JournalDao`, `BodyMeasurementDao`, `GoalDao`, `PersonalCardDao`, `WorkoutSessionDao`, and `WorkoutPresetDao`.
    - [x] Replace tag queries with joins against `journal_entries`; do not add an owner column to `EntryTagCrossRef`.
    - [x] Change `GoalDao.deleteById(profileId, parameterId)` and `PersonalCardDao` methods to require both profile and card id.
    - [x] Add `WorkoutSessionDao.insertSessionIfNoUnfinished(profileId, session): Boolean` as a `@Transaction` that selects an unfinished session for that profile and inserts only when none exists.
    - [x] Keep `ExerciseCatalogDao` shared and unchanged.
    - [x] Add the new DAO methods to both unit-test and androidTest fakes.
    - [x] Run the DAO tests and the full unit suite to green.
    - [x] Commit the DAO scoping and tests.

- [x] Task: Make repositories active-profile aware (Red/Green)
    - [x] Write failing tests for `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`, `PersonalCardRepository`, `WorkoutRepository`, and `PresetRepository`: a switch reloads each flow for the new profile; writes stamp the active profile; explicit-profile methods return only that profile; a stale active id falls back.
    - [x] Run the repository tests and confirm the missing-profile failures.
    - [x] Add an `ActiveProfileProvider` constructor parameter to each repository.
    - [x] Replace eager `dao.getAll*()` flows with `activeProfile.activeProfileId.flatMapLatest { id -> dao.getAll*(id) }`.
    - [x] Stamp `profileId` at the repository write boundary; make `getEntryById`, archive/restore/delete, tags, goal updates, card updates, session updates, preset updates, and pending-sync reads resolve the current profile.
    - [x] Add explicit-profile methods for sync, backup, and PDF: `getEntriesInDateRange(profileId, ...)`, `getPendingSyncEntries(profileId)`, `getDeletedEntryIds(profileId)`, measurement list/pending/deleted reads, goals list, card snapshot, and session/preset lists.
    - [x] Update all repository tests and fakes to construct the provider.
    - [x] Run the full unit suite to green.
    - [x] Commit the repository changes and tests.

- [x] Task: Wire profile bootstrap into `MainActivity`
    - [x] Construct `ProfileRepository`, `ProfileDataEraser`, and `ActiveProfileStore` before the existing repositories.
    - [x] Pass the provider into every repository constructor.
    - [x] Call `profileRepository.ensureSelfProfile()` in the existing `lifecycleScope.launch` bootstrap before the UI relies on profile data.
    - [x] Run `./gradlew build`; fix every construction and factory compile error.
    - [x] Commit the wiring.

- [x] Task: Conductor - User Manual Verification 'Phase 1: Data foundation' (Protocol in workflow.md)
    - [x] Install the debug build over an existing install; verify every old entry, measurement, goal, card, workout, and preset is visible under `Me`.
    - [x] Wipe the app and verify a fresh install starts with an empty `Me`.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**


## Phase 2: Profile management UI and active-profile preference

- [x] Task: Add `ProfileViewModel` and state (Red/Green)
    - [x] Write failing `ProfileViewModelTest` cases for loading profiles, active profile, create/edit, archive/restore, typed delete, busy state, and error messages.
    - [x] Run the test and confirm it fails.
    - [x] Create `viewmodel/ProfileViewModel.kt` with `ProfileUiState(profiles, activeProfile, activeCount, isBusy, error)` and factories that take `ProfileRepository`.
    - [x] Expose actions returning `Result` so screens can render `ProfileError` without string parsing.
    - [x] Run the test to green; commit.

- [x] Task: Build the avatar and switcher components (Red/Green + UI test)
    - [x] Write failing `ProfileAvatarTest` and `ProfileSwitcherTest` for initials, accent color, active check mark, profile list, and Manage Profiles action.
    - [x] Implement `ui/components/ProfileAvatar.kt` and `ui/components/ProfileSwitcher.kt`.
    - [x] Use a Material 3 dropdown anchored to the top-bar chip; show the active name and initials; include an explicit Manage Profiles item.
    - [x] Keep the chip disabled while an export, save, or workout operation is running.
    - [x] Run the Compose tests on an emulator; commit.

- [x] Task: Create Manage Profiles (Red/Green + UI test)
    - [x] Write failing `ManageProfilesScreenTest` for the empty/one-profile state, create, edit, archive, restore, typed-name delete, archived section, and 10-profile limit.
    - [x] Implement `ui/screens/ManageProfilesScreen.kt` with active and archived sections, create/edit forms, relationship options, color choices, and archive/restore/delete confirmations.
    - [x] Show the privacy note that profiles are organizational, not a security boundary.
    - [x] Run the Compose tests on an emulator; commit.

- [x] Task: Add the profile chip to top bars and the Manage Profiles route
    - [x] Add `profileSwitcher: @Composable (enabled: Boolean, onSwitch: (String) -> Unit) -> Unit = { _, _ -> }` to `MainScreen`, `HistoryScreen`, `MeasurementsScreen`, `WorkoutScreen`, `PresetLibraryScreen`, `ArchiveScreen`, `ExportScreen`, `SettingsScreen`, `PersonalCardScreen`, and `AddEntryScreen`.
    - [x] Compose the supplied switcher in each screen's `actions`; hide it in `ArchiveScreen` selection mode and `PersonalCardScreen` while saving.
    - [x] Add the `manage_profiles` route in `MainActivity` and pass a switcher lambda that uses `ProfileViewModel`, switches the profile, and pops detail routes back to `main`.
    - [x] Add the switcher to `AddEntryScreen` so a draft switch can be prompted.
    - [x] Run existing screen tests to confirm the default empty slot keeps them source-compatible.
    - [x] Run `./gradlew build`; commit.

- [x] Task: Implement switch guards (Red/Green + UI test)
    - [x] Write failing tests for discarding an Add Entry draft, personal-card edit, preset edit, active workout, and generating export.
    - [x] In `AddEntryScreen`, snapshot initial form state, show a discard/cancel dialog on switch, call the switch only after confirmation, and disable the chip while a save is in progress.
    - [x] In `PersonalCardScreen` and `PresetLibraryScreen`, guard dirty edits and disable switching while saving.
    - [x] In `WorkoutScreen`, block switching while a workout is active, paused, counting down, or recovering.
    - [x] In `ExportScreen`, disable the chip while `ExportState.Generating`.
    - [x] Ensure an accepted switch closes the detail/edit route and reloads root tabs.
    - [x] Run the Compose tests on an emulator; commit.

- [x] Task: Conductor - User Manual Verification 'Phase 2: Profile management UI and active-profile preference' (Protocol in workflow.md)
    - [x] Create a sibling profile with a name, relationship, and color; verify it appears in the switcher.
    - [x] Switch between `Me` and the sibling; verify the active name is visible on every root screen and survives an app restart.
    - [x] Open Add Entry, type text, switch, cancel, then retry and discard; verify the draft never lands under the other profile.
    - [x] Start a workout and verify switching is blocked; finish it and verify switching works.
    - [x] Change units and theme under A, switch to B, and verify both settings remain shared.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 3: Journal and personal card scoping

- [x] Task: Capture the target profile in journal writes (Red/Green)
    - [x] Write failing `JournalViewModelTest` cases: `addEntry` stamps the captured profile; editing a foreign entry is rejected; a switch during an open form does not move the draft; tags are written under the same profile.
    - [x] Run the test and confirm the failures.
    - [x] Add an `ActiveProfileProvider`/`ProfileRepository` dependency to `JournalViewModel`.
    - [x] Add a `profileId` parameter to `addEntry` and `updateEntry`; require `updateEntry` to match the existing entry's profile.
    - [x] Capture the active profile id when Add Entry opens; pass it through Save/Update.
    - [x] Expose `activeProfile: StateFlow<Profile?>` for the Health Connect confirmation and profile labels.
    - [x] Run `JournalViewModelTest` to green.

- [x] Task: Scope History, Archive, and Add Entry (Red/Green + UI test)
    - [x] Write failing `HistoryScreenTest` and `AddEntryScreenTest` cases: entries from profile A are absent under B; search and tag filters are scoped; an edit opened under A cannot be saved under B.
    - [x] Verify the repository flow makes History and Archive reload on switch; add explicit empty-state copy naming the active profile.
    - [x] Ensure `AddEntryScreen` clears/initializes form state from `(entryId, activeProfileId)`.
    - [x] Run the Compose tests on an emulator.

- [x] Task: Scope the personal card (Red/Green + UI test)
    - [x] Write failing `PersonalCardRepositoryTest`, `PersonalCardViewModelTest`, and `PersonalCardScreenTest` cases: one card per profile; profile A's card is absent under B; saving under A cannot update B.
    - [x] Make `PersonalCardRepository` use `(activeProfile, PERSONAL_CARD_ID)` for every read/write and add an explicit-profile snapshot for sync/backup.
    - [x] Update `PersonalCardViewModel` to capture the target profile at edit start.
    - [x] Run unit and Compose tests.

- [x] Task: Verify journal media ownership
    - [x] Add a test that two profiles can reference attachments with the same filename without overwriting each other's URI/entry association.
    - [x] If filenames collide, scope stored media names by profile in the journal save path; otherwise record the existing uniqueness guarantee in the test.
    - [x] Run tests.

- [x] Task: Conductor - User Manual Verification 'Phase 3: Journal and personal card scoping' (Protocol in workflow.md)
    - [x] Create entries under profile A; switch to B and verify History, Archive, search, tags, and the personal card are empty for B.
    - [x] Create entries and a card under B; switch back and verify A's data returns unchanged.
    - [x] Open an A entry for edit, switch with a dirty draft, discard, and verify no B write occurs.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 4: Measurements, goals, workouts, and presets scoping

- [x] Task: Scope measurements and goals (Red/Green + UI test)
    - [x] Write failing `BodyMeasurementViewModelTest`, `BodyAnalyticsViewModel`/goals tests, and `MeasurementsScreenTest` cases: measurements and goals are per profile; the same parameter can have different targets; clearing a goal affects only the active profile.
    - [x] Update `BodyMeasurementRepository` and `GoalsRepository` writes/reads to stamp/filter the active profile.
    - [x] Update the ViewModels to reload on switch and show active-profile empty states.
    - [x] Run unit and Compose tests; commit.

- [x] Task: Scope workouts and enforce one active session per profile (Red/Green + UI test)
    - [x] Write failing `WorkoutRepositoryTest` and `WorkoutViewModelTest` cases: profile A and B can each have an unfinished session; recovery on B does not resume A; a session started under A cannot be saved under B.
    - [x] Use `insertSessionIfNoUnfinished(profileId, session)` and profile-scoped session queries.
    - [x] Capture the target profile when a session starts; guard switching while a session is active.
    - [x] Update `WorkoutViewModelFactory` and `MainActivity` wiring.
    - [x] Run unit and Compose tests; commit.

- [x] Task: Scope presets (Red/Green + UI test)
    - [x] Write failing `PresetRepositoryTest`, `PresetViewModelTest`, and `PresetLibraryScreenTest` cases: presets are per profile; the same preset name may exist under two profiles; catalog search remains shared.
    - [x] Update `PresetRepository` and `PresetViewModel` to use the active profile; keep `ExerciseCatalogDao` shared.
    - [x] Guard preset edits on switch.
    - [x] Run unit and Compose tests; commit.

- [x] Task: Conductor - User Manual Verification 'Phase 4: Measurements, goals, workouts, and presets scoping' (Protocol in workflow.md)
    - [x] Add measurements, goals, a workout session, and a preset under A; switch to B and verify all are empty.
    - [x] Create a same-named preset and a same-parameter goal under B; switch back and verify A's values are unchanged.
    - [x] Start a workout under A; verify B cannot resume it and switching is blocked.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 5: Sync, backup/restore, PDF export, and Health Connect

- [x] Task: Sync profile metadata and profile deletions (Red/Green)
    - [x] Write failing `ProfileSyncPayloadTest`, `SyncMergeProfileTest`, and `SyncWorkerProfileTest` cases: profile metadata round-trips; newer `lastModified` wins; a deletion marker removes the profile and its data; legacy payloads have no profiles and default to Self.
    - [x] Add `ProfileSyncPayload` and a `profiles.json` Drive file; upload/download/merge profile metadata before data files.
    - [x] Add `DriveServiceHelper.deleteDataFile(fileName)` and delete every per-profile file when a profile deletion is applied.
    - [x] Apply a remote deletion through `ProfileDataEraser`; if the deleted profile is active, switch to a remaining active profile first.
    - [x] Run the JVM sync tests; commit.

- [x] Task: Make data sync profile-safe (Red/Green)
    - [x] Write failing `SyncMergeTest`, payload codec tests, `MeasurementSyncMergeTest`, `GoalSyncMergeTest`, and `PersonalCardSyncMergeTest` cases for composite keys, legacy missing `profileId` → Self, and no cross-profile reassignment.
    - [x] Add `profileId` to journal, measurement, goal, personal-card, and tombstone payloads; normalize missing values to `SELF_PROFILE_ID`.
    - [x] Key journal merges by `(profileId, entry_id)`, measurements by `(profileId, entry_id)`, goals by `(profileId, parameterId)`, and cards by `(profileId, id)`.
    - [x] Keep Self on the legacy filenames; write non-Self profiles to UUID-suffixed filenames so old clients never ingest sibling data.
    - [x] Refactor `SyncWorker` to iterate all profiles, sync each domain per profile, and prune tombstones per profile.
    - [x] Do not add workout/preset sync.
    - [x] Run the JVM sync tests; commit.

- [x] Task: Extend full backup to v2 with all profiles (Red/Green)
    - [x] Write failing `BackupWriterTest`, `BackupDataReaderTest`, `FullBackupUseCaseTest`, and `ManifestValidatorTest` cases: v2 includes `profiles.json`; all archived/active profiles and their rows are present; v1 still reads; media paths are profile-scoped.
    - [x] Add `profiles: List<ProfileEntity>` to `BackupData`; bump `BACKUP_FORMAT_VERSION` to 2; write `profiles.json` and include it in the manifest contents.
    - [x] Add `profileId` to every backed-up entity; keep v1 readers tolerant by defaulting missing owners to Self.
    - [x] Scope media entry names by profile/entry to prevent same-name collisions.
    - [x] Update `FullBackupUseCase` to read every profile, including archived, and every domain.
    - [x] Run the JVM backup tests; commit.

- [x] Task: Restore v2 and legacy v1 backups safely (Red/Green)
    - [x] Write failing `RestoreRepositoryTest`, `RestoreIntegrationTest`, `RestoreCoordinatorTest`, and `RestoreViewModelTest` cases: v2 replaces all profiles; v1 clears and restores only Self while preserving other profiles; profile count is shown; media URIs remap per profile.
    - [x] Pass the backup format version and profile metadata through `RestoreCoordinator`, `RestoreRepository`, `RestoreWorker`, and `RestoreUiState`.
    - [x] For v2, wipe and recreate all profiles and rows in one transaction; for v1, wipe only Self.
    - [x] Re-import media into profile-scoped paths and remap every URI.
    - [x] After restore, call `ProfileRepository.ensureSelfProfile()` and re-sync.
    - [x] Run the JVM restore tests and the device `RestoreIntegrationTest`; commit.

- [x] Task: Add profile selection to PDF export (Red/Green + UI test)
    - [x] Write failing `PdfExportUseCaseTest` and `ExportScreenTest` cases: default is active; selecting A+B produces two labeled sections; archived profiles are absent; ZIP still exports all profiles and ignores the PDF date range.
    - [x] Add active-profile options and selection state to `ExportViewModel`/`ExportScreen`; keep ZIP UI as the all-profile full backup.
    - [x] Change `PdfExportUseCase.execute(profileIds, startDate, endDate)` to read each selected profile explicitly and add a profile heading before its entries.
    - [x] Run unit and Compose tests; commit.

- [x] Task: Require an explicit Health Connect target (Red/Green + UI test)
    - [x] Write failing `JournalViewModelTest` and `AddEntryScreenTest` cases: import shows the active profile name; confirming writes only to the captured profile; switching while the dialog is open cannot redirect the import.
    - [x] Add `syncHealthData(timestamp, targetProfileId)` and a confirmation dialog before both the permission callback and quick-sync path.
    - [x] Capture the target profile id when the dialog opens and pass it through the import.
    - [x] Run unit and Compose tests; commit.

- [x] Task: Conductor - User Manual Verification 'Phase 5: Sync, backup/restore, PDF export, and Health Connect' (Protocol in workflow.md)
    - [x] Sync two profiles to Drive; verify sibling data is absent from a legacy-looking Self payload and present in its own file.
    - [x] Restore a v2 backup and verify all profiles and boundaries return; restore a v1 backup and verify only `Me` changes.
    - [x] Export a PDF for A, then A+B; verify sections and no archived profiles. Export ZIP and verify every profile is present.
    - [x] Import Health Connect data under A after confirming the dialog; verify no row appears under B.
    - [x] Delete an archived profile and verify its local and cloud data are removed.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 6: Docs and full verification

- [x] Task: Add localized strings and accessibility labels
    - [x] Add every profile/switcher/manage/delete/export/Health Connect string to `values/strings.xml` and `values-ru/strings.xml`.
    - [x] Add content descriptions for the avatar, chip, relationship, and destructive actions; do not rely on color alone.
    - [x] Run `LocalizationParityTest`, `HardcodedStringAuditTest`, and the Compose UI tests; commit.

- [x] Task: Write the feature PSD and test-case docs
    - [x] Create `Docs/psd/profiles-for-siblings.md` from `Docs/_templates/psd.md`; document the single-database model, active-profile provider, top-bar/switcher, profile-scoped repositories, sync file naming, backup v2/v1 restore, PDF selection, and Health Connect confirmation.
    - [x] Create `Docs/tests/profiles-for-siblings.md` from `Docs/_templates/tests.md`; list the migration, DAO isolation, repository, ViewModel, sync, backup/restore, export, Health Connect, and Compose tests.
    - [x] Update `Docs/prd/profiles-for-siblings.md` status from planned to shipped and keep its `Last updated` accurate.
    - [x] Update `Docs/index.md` to list the PRD, PSD, and tests paths and remove `— planned`.
    - [x] Run `python3 wiki/lint.py`; commit.

- [x] Task: Update the wiki pages affected by the code
    - [x] Update `wiki/modules/data-layer.md` for the profile table, ownership, migration, and scoped DAOs/repositories.
    - [x] Update `wiki/modules/viewmodel-layer.md` for active-profile injection and per-screen scoping.
    - [x] Update `wiki/modules/ui-layer.md` for the top-bar chip and Manage Profiles.
    - [x] Update `wiki/modules/sync-engine.md` for profile metadata, filenames, deletion propagation, and legacy fallback.
    - [x] Update `wiki/modules/export-restore.md` for full backup v2, v1 restore-to-Self, and PDF profile selection.
    - [x] Update `wiki/integrations/health-connect.md` for the confirmation and target capture.
    - [x] Cite paths; do not copy mutable values.
    - [x] Run `python3 wiki/lint.py` and commit the wiki changes with the code.

- [x] Task: Full verification and README-free cleanup
    - [x] Run `./gradlew build` and `./gradlew :app:testDebugUnitTest`; both must pass.
    - [x] Run the affected instrumented suites: migration, DAO isolation, profile UI, history, add entry, personal card, measurements, workout, presets, export, restore, and Health Connect.
    - [x] Run `python3 wiki/lint.py`; exit 0 required.
    - [x] Run `git diff --check`; fix whitespace.
    - [x] Confirm no `opencode.json`, draft, or unrelated file is staged.
    - [x] Commit only the intended verification fixes.

- [x] Task: Conductor - User Manual Verification 'Phase 6: Docs and full verification' (Protocol in workflow.md)
    - [x] Walk every acceptance criterion in `spec.md` against the built app.
    - [x] Verify upgrade, fresh install, create, switch, archive, restore, delete, sync, backup/restore, PDF selection, Health Connect, and localization.
    - [x] Verify `python3 wiki/lint.py` exits 0.
    - [x] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Risks and sequencing notes

- **Schema collision:** PR #49 is unmerged and also changes the schema. Do not
  create the profile migration until PR #49 lands; renumber and regenerate the
  schema JSON if needed.
- **Restore is the highest-risk path:** a v1 backup must not wipe sibling data.
  Test the v1 Self-only restore and the v2 all-profile restore separately.
- **Sync backward compatibility:** keep Self on legacy filenames and never put
  sibling records in the files an old client reads.
- **Profile deletion:** archive first, typed name second, remote deletion third.
  Do not leave orphaned media, tags, or tombstones.
- **Switch guards:** the chip must exist on Add Entry, otherwise AC-11 is
  unreachable.

## Phase: Review Fixes
- [x] Task: Apply review suggestions 0cbcca8
