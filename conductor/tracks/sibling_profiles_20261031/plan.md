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

- [ ] Task: Confirm the schema baseline
    - [ ] Fetch and rebase the branch on `origin/main` after PR #49 is merged.
    - [ ] Confirm `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` reports version 21 and `app/schemas/.../21.json` exists.
    - [ ] If the baseline is still 20, stop; do not create a competing `20 → 21` migration.
    - [ ] Record the current schema version in the task notes and use `21 → 22` for this track.

- [ ] Task: Add the profile model, preference, and repository (Red/Green/Refactor)
    - [ ] Write failing `ProfileRepositoryTest` cases for: fresh `ensureSelfProfile()` creates `Me`; a stored active id is restored; an archived stored id falls back to an active profile; create rejects blank names and the 11th active profile; a second `Self` is rejected; archive of the active profile switches first; the last active profile cannot be archived; restore is blocked at 10 active profiles; permanent delete requires an archived profile and exact typed name; the last active profile cannot be deleted.
    - [ ] Run `./gradlew :app:testDebugUnitTest --tests "com.example.healthjournal.data.ProfileRepositoryTest"` and confirm the tests fail because the types do not exist.
    - [ ] Create `data/local/ProfileEntity.kt` with `id`, `name`, `relationship`, `accentColor`, `isArchived`, `createdAt`, `lastModified`, `isSynced`, and `syncStatus`; add `ProfileRelationship` (`SELF`, `SIBLING`, `CHILD`, `PARENT`, `OTHER`) and `SELF_PROFILE_ID`.
    - [ ] Create `data/local/ProfileDao.kt` with `observeAll()`, `observeActive()`, `get(id)`, `getSelf()`, `countActive()`, `upsert(profile)`, `setArchived(id, archived, lastModified)`, and `delete(id)`.
    - [ ] Create `data/local/ProfileDeletionEntity.kt` and `ProfileDeletionDao.kt` with a `profile_deletions` table keyed by `profileId` and `deletedAt`; expose `observeAll()`, `insert(marker)`, `get(profileId)`, and `delete(profileId)`.
    - [ ] Create `data/local/ActiveProfileSettings.kt` mirroring `UnitSettings.kt`: private `SharedPreferences` file, `read(context): String?`, `write(context, profileId)`.
    - [ ] Create `data/ActiveProfileProvider.kt` with `val activeProfileId: StateFlow<String>` and `fun currentProfileId(): String`.
    - [ ] Create `data/ProfileRepository.kt`: implements `ActiveProfileProvider`; owns a `Mutex`; exposes `profiles`, `activeProfiles`, `activeProfile`, `ensureSelfProfile()`, `create(name, relationship, color)`, `update(...)`, `archive(id)`, `restore(id)`, `deletePermanently(id, typedName)`, and `switchTo(id)`. Persist every active switch through `ActiveProfileSettings`.
    - [ ] Create `data/ProfileDataEraser.kt` with `suspend fun erase(profileId: String)`; implement `RoomProfileDataEraser` using `JournalDatabase.withTransaction` to delete all scoped rows and tags for that profile, insert the profile-deletion marker, and remove the profile row.
    - [ ] Run the tests to green; add tests for preference persistence and fallback.
    - [ ] Refactor `ProfileRepository` error handling into a sealed `ProfileError` (`BlankName`, `ActiveLimit`, `SelfExists`, `LastActive`, `NotArchived`, `NameMismatch`, `NotFound`).
    - [ ] Commit only the new profile files and their tests.

- [ ] Task: Add profile ownership to entities and the `21 → 22` migration (Red/Green)
    - [ ] Write failing `MigrationTest` cases: `migrate21To22_CreatesSelfProfileAndBackfillsScopedTables`; `migrate21To22_PreservesEveryRowAndKey`; `migrate21To22_ChainedFrom20`; `migrate21To22_FreshDatabaseHasSelfProfile`.
    - [ ] Seed v21 rows in journal entries, tags, measurements, goals, personal card, workout sessions, presets, and tombstones; assert every migrated row has `profileId = "self"` and that goals/cards/tombstones keep their original keys.
    - [ ] Run the migration test on an emulator; confirm it fails because the database is still 21.
    - [ ] Add `profileId: String` to `JournalEntry`, `BodyMeasurementEntry`, `GoalEntity`, `PersonalCard`, `WorkoutSession`, `WorkoutPreset`, and `DeletedEntry`.
    - [ ] Change `GoalEntity` primary key to `(profileId, parameterId)`, `PersonalCard` to `(profileId, id)`, and `DeletedEntry` to `(profileId, entry_id)`.
    - [ ] Add `(profileId, timestamp)` and `(profileId, isArchived)` indices where applicable; change preset index to `(profileId, name)` and add profile/status/start indices for sessions.
    - [ ] Add `ProfileEntity` and `ProfileDeletionEntity` to the `@Database` entity list and add DAO accessors.
    - [ ] Bump `JournalDatabase` version and `CURRENT_SCHEMA_VERSION` to 22 and add `MIGRATION_21_22`.
    - [ ] Migration steps: create `profiles`; insert `('self', 'Me', 'SELF', ..., 0)`; create `profile_deletions`; add `profileId TEXT NOT NULL DEFAULT 'self'` to journal entries, measurements, workout sessions, and presets; recreate `goals`, `personal_card`, and `deleted_entries` with the new composite keys and copy every row with `'self'`; add the new indices.
    - [ ] Register `MIGRATION_21_22` in `addMigrations`.
    - [ ] Fix every constructor and fixture compile error; use `SELF_PROFILE_ID` for migration fixtures and explicit profile ids for production writes.
    - [ ] Run the migration test on an emulator and `./gradlew :app:testDebugUnitTest` to green.
    - [ ] Generate and commit `app/schemas/com.example.healthjournal.data.local.JournalDatabase/22.json` with the migration.
    - [ ] Commit the entity, database, and migration-test changes.

- [ ] Task: Scope every DAO (Red/Green)
    - [ ] Write failing tests in `JournalDaoTest`, `BodyMeasurementDaoTest`, `GoalDaoTest`, `PersonalCardDaoTest`, and new workout DAO tests: profile A rows are absent from profile B reads; by-id reads return null for a foreign profile; tag lists join through `journal_entries`; two profiles can store the same goal parameter, card, and unfinished session; tombstones are profile-scoped; `clearAll` and `removeDeletedEntriesBefore` affect only one profile.
    - [ ] Run the DAO tests and confirm the failures are missing profile predicates.
    - [ ] Add `profileId` as the first parameter to every scoped read/write in `JournalDao`, `BodyMeasurementDao`, `GoalDao`, `PersonalCardDao`, `WorkoutSessionDao`, and `WorkoutPresetDao`.
    - [ ] Replace tag queries with joins against `journal_entries`; do not add an owner column to `EntryTagCrossRef`.
    - [ ] Change `GoalDao.deleteById(profileId, parameterId)` and `PersonalCardDao` methods to require both profile and card id.
    - [ ] Add `WorkoutSessionDao.insertSessionIfNoUnfinished(profileId, session): Boolean` as a `@Transaction` that selects an unfinished session for that profile and inserts only when none exists.
    - [ ] Keep `ExerciseCatalogDao` shared and unchanged.
    - [ ] Add the new DAO methods to both unit-test and androidTest fakes.
    - [ ] Run the DAO tests and the full unit suite to green.
    - [ ] Commit the DAO scoping and tests.

- [ ] Task: Make repositories active-profile aware (Red/Green)
    - [ ] Write failing tests for `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`, `PersonalCardRepository`, `WorkoutRepository`, and `PresetRepository`: a switch reloads each flow for the new profile; writes stamp the active profile; explicit-profile methods return only that profile; a stale active id falls back.
    - [ ] Run the repository tests and confirm the missing-profile failures.
    - [ ] Add an `ActiveProfileProvider` constructor parameter to each repository.
    - [ ] Replace eager `dao.getAll*()` flows with `activeProfile.activeProfileId.flatMapLatest { id -> dao.getAll*(id) }`.
    - [ ] Stamp `profileId` at the repository write boundary; make `getEntryById`, archive/restore/delete, tags, goal updates, card updates, session updates, preset updates, and pending-sync reads resolve the current profile.
    - [ ] Add explicit-profile methods for sync, backup, and PDF: `getEntriesInDateRange(profileId, ...)`, `getPendingSyncEntries(profileId)`, `getDeletedEntryIds(profileId)`, measurement list/pending/deleted reads, goals list, card snapshot, and session/preset lists.
    - [ ] Update all repository tests and fakes to construct the provider.
    - [ ] Run the full unit suite to green.
    - [ ] Commit the repository changes and tests.

- [ ] Task: Wire profile bootstrap into `MainActivity`
    - [ ] Construct `ProfileRepository`, `ProfileDataEraser`, and `ActiveProfileSettings` before the existing repositories.
    - [ ] Pass the provider into every repository constructor.
    - [ ] Call `profileRepository.ensureSelfProfile()` in the existing `lifecycleScope.launch` bootstrap before the UI relies on profile data.
    - [ ] Run `./gradlew build`; fix every construction and factory compile error.
    - [ ] Commit the wiring.

- [ ] Task: Conductor - User Manual Verification 'Phase 1: Data foundation' (Protocol in workflow.md)
    - [ ] Install the debug build over an existing install; verify every old entry, measurement, goal, card, workout, and preset is visible under `Me`.
    - [ ] Wipe the app and verify a fresh install starts with an empty `Me`.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 2: Profile management UI and active-profile preference

- [ ] Task: Add `ProfileViewModel` and state (Red/Green)
    - [ ] Write failing `ProfileViewModelTest` cases for loading profiles, active profile, create/edit, archive/restore, typed delete, busy state, and error messages.
    - [ ] Run the test and confirm it fails.
    - [ ] Create `viewmodel/ProfileViewModel.kt` with `ProfileUiState(profiles, activeProfile, activeCount, isBusy, error)` and factories that take `ProfileRepository`.
    - [ ] Expose actions returning `Result` so screens can render `ProfileError` without string parsing.
    - [ ] Run the test to green; commit.

- [ ] Task: Build the avatar and switcher components (Red/Green + UI test)
    - [ ] Write failing `ProfileAvatarTest` and `ProfileSwitcherTest` for initials, accent color, active check mark, profile list, and Manage Profiles action.
    - [ ] Implement `ui/components/ProfileAvatar.kt` and `ui/components/ProfileSwitcher.kt`.
    - [ ] Use a Material 3 dropdown anchored to the top-bar chip; show the active name and initials; include an explicit Manage Profiles item.
    - [ ] Keep the chip disabled while an export, save, or workout operation is running.
    - [ ] Run the Compose tests on an emulator; commit.

- [ ] Task: Create Manage Profiles (Red/Green + UI test)
    - [ ] Write failing `ManageProfilesScreenTest` for the empty/one-profile state, create, edit, archive, restore, typed-name delete, archived section, and 10-profile limit.
    - [ ] Implement `ui/screens/ManageProfilesScreen.kt` with active and archived sections, create/edit forms, relationship options, color choices, and archive/restore/delete confirmations.
    - [ ] Show the privacy note that profiles are organizational, not a security boundary.
    - [ ] Run the Compose tests on an emulator; commit.

- [ ] Task: Add the profile chip to top bars and the Manage Profiles route
    - [ ] Add `profileSwitcher: @Composable ((String) -> Unit) -> Unit = {}` to `MainScreen`, `HistoryScreen`, `MeasurementsScreen`, `WorkoutScreen`, `PresetLibraryScreen`, `ArchiveScreen`, `ExportScreen`, `SettingsScreen`, `PersonalCardScreen`, and `AddEntryScreen`.
    - [ ] Compose the supplied switcher in each screen's `actions`; hide it in `ArchiveScreen` selection mode and `PersonalCardScreen` while saving.
    - [ ] Add the `manage_profiles` route in `MainActivity` and pass a switcher lambda that uses `ProfileViewModel`, switches the profile, and pops detail routes back to `main`.
    - [ ] Add the switcher to `AddEntryScreen` so a draft switch can be prompted.
    - [ ] Run existing screen tests to confirm the default empty slot keeps them source-compatible.
    - [ ] Run `./gradlew build`; commit.

- [ ] Task: Implement switch guards (Red/Green + UI test)
    - [ ] Write failing tests for discarding an Add Entry draft, personal-card edit, preset edit, active workout, and generating export.
    - [ ] In `AddEntryScreen`, snapshot initial form state, show a discard/cancel dialog on switch, call the switch only after confirmation, and disable the chip while a save is in progress.
    - [ ] In `PersonalCardScreen` and `PresetLibraryScreen`, guard dirty edits and disable switching while saving.
    - [ ] In `WorkoutScreen`, block switching while a workout is active, paused, counting down, or recovering.
    - [ ] In `ExportScreen`, disable the chip while `ExportState.Generating`.
    - [ ] Ensure an accepted switch closes the detail/edit route and reloads root tabs.
    - [ ] Run the Compose tests on an emulator; commit.

- [ ] Task: Conductor - User Manual Verification 'Phase 2: Profile management UI and active-profile preference' (Protocol in workflow.md)
    - [ ] Create a sibling profile with a name, relationship, and color; verify it appears in the switcher.
    - [ ] Switch between `Me` and the sibling; verify the active name is visible on every root screen and survives an app restart.
    - [ ] Open Add Entry, type text, switch, cancel, then retry and discard; verify the draft never lands under the other profile.
    - [ ] Start a workout and verify switching is blocked; finish it and verify switching works.
    - [ ] Change units and theme under A, switch to B, and verify both settings remain shared.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 3: Journal and personal card scoping

- [ ] Task: Capture the target profile in journal writes (Red/Green)
    - [ ] Write failing `JournalViewModelTest` cases: `addEntry` stamps the captured profile; editing a foreign entry is rejected; a switch during an open form does not move the draft; tags are written under the same profile.
    - [ ] Run the test and confirm the failures.
    - [ ] Add an `ActiveProfileProvider`/`ProfileRepository` dependency to `JournalViewModel`.
    - [ ] Add a `profileId` parameter to `addEntry` and `updateEntry`; require `updateEntry` to match the existing entry's profile.
    - [ ] Capture the active profile id when Add Entry opens; pass it through Save/Update.
    - [ ] Expose `activeProfile: StateFlow<Profile?>` for the Health Connect confirmation and profile labels.
    - [ ] Run `JournalViewModelTest` to green; commit.

- [ ] Task: Scope History, Archive, and Add Entry (Red/Green + UI test)
    - [ ] Write failing `HistoryScreenTest` and `AddEntryScreenTest` cases: entries from profile A are absent under B; search and tag filters are scoped; an edit opened under A cannot be saved under B.
    - [ ] Verify the repository flow makes History and Archive reload on switch; add explicit empty-state copy naming the active profile.
    - [ ] Ensure `AddEntryScreen` clears/initializes form state from `(entryId, activeProfileId)`.
    - [ ] Run the Compose tests on an emulator; commit.

- [ ] Task: Scope the personal card (Red/Green + UI test)
    - [ ] Write failing `PersonalCardRepositoryTest`, `PersonalCardViewModelTest`, and `PersonalCardScreenTest` cases: one card per profile; profile A's card is absent under B; saving under A cannot update B.
    - [ ] Make `PersonalCardRepository` use `(activeProfile, PERSONAL_CARD_ID)` for every read/write and add an explicit-profile snapshot for sync/backup.
    - [ ] Update `PersonalCardViewModel` to capture the target profile at edit start.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Verify journal media ownership
    - [ ] Add a test that two profiles can reference attachments with the same filename without overwriting each other's URI/entry association.
    - [ ] If filenames collide, scope stored media names by profile in the journal save path; otherwise record the existing uniqueness guarantee in the test.
    - [ ] Run tests; commit.

- [ ] Task: Conductor - User Manual Verification 'Phase 3: Journal and personal card scoping' (Protocol in workflow.md)
    - [ ] Create entries under profile A; switch to B and verify History, Archive, search, tags, and the personal card are empty for B.
    - [ ] Create entries and a card under B; switch back and verify A's data returns unchanged.
    - [ ] Open an A entry for edit, switch with a dirty draft, discard, and verify no B write occurs.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 4: Measurements, goals, workouts, and presets scoping

- [ ] Task: Scope measurements and goals (Red/Green + UI test)
    - [ ] Write failing `BodyMeasurementViewModelTest`, `BodyAnalyticsViewModel`/goals tests, and `MeasurementsScreenTest` cases: measurements and goals are per profile; the same parameter can have different targets; clearing a goal affects only the active profile.
    - [ ] Update `BodyMeasurementRepository` and `GoalsRepository` writes/reads to stamp/filter the active profile.
    - [ ] Update the ViewModels to reload on switch and show active-profile empty states.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Scope workouts and enforce one active session per profile (Red/Green + UI test)
    - [ ] Write failing `WorkoutRepositoryTest` and `WorkoutViewModelTest` cases: profile A and B can each have an unfinished session; recovery on B does not resume A; a session started under A cannot be saved under B.
    - [ ] Use `insertSessionIfNoUnfinished(profileId, session)` and profile-scoped session queries.
    - [ ] Capture the target profile when a session starts; guard switching while a session is active.
    - [ ] Update `WorkoutViewModelFactory` and `MainActivity` wiring.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Scope presets (Red/Green + UI test)
    - [ ] Write failing `PresetRepositoryTest`, `PresetViewModelTest`, and `PresetLibraryScreenTest` cases: presets are per profile; the same preset name may exist under two profiles; catalog search remains shared.
    - [ ] Update `PresetRepository` and `PresetViewModel` to use the active profile; keep `ExerciseCatalogDao` shared.
    - [ ] Guard preset edits on switch.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Conductor - User Manual Verification 'Phase 4: Measurements, goals, workouts, and presets scoping' (Protocol in workflow.md)
    - [ ] Add measurements, goals, a workout session, and a preset under A; switch to B and verify all are empty.
    - [ ] Create a same-named preset and a same-parameter goal under B; switch back and verify A's values are unchanged.
    - [ ] Start a workout under A; verify B cannot resume it and switching is blocked.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 5: Sync, backup/restore, PDF export, and Health Connect

- [ ] Task: Sync profile metadata and profile deletions (Red/Green)
    - [ ] Write failing `ProfileSyncPayloadTest`, `SyncMergeProfileTest`, and `SyncWorkerProfileTest` cases: profile metadata round-trips; newer `lastModified` wins; a deletion marker removes the profile and its data; legacy payloads have no profiles and default to Self.
    - [ ] Add `ProfileSyncPayload` and a `profiles.json` Drive file; upload/download/merge profile metadata before data files.
    - [ ] Add `DriveServiceHelper.deleteDataFile(fileName)` and delete every per-profile file when a profile deletion is applied.
    - [ ] Apply a remote deletion through `ProfileDataEraser`; if the deleted profile is active, switch to a remaining active profile first.
    - [ ] Run the JVM sync tests; commit.

- [ ] Task: Make data sync profile-safe (Red/Green)
    - [ ] Write failing `SyncMergeTest`, payload codec tests, `MeasurementSyncMergeTest`, `GoalSyncMergeTest`, and `PersonalCardSyncMergeTest` cases for composite keys, legacy missing `profileId` → Self, and no cross-profile reassignment.
    - [ ] Add `profileId` to journal, measurement, goal, personal-card, and tombstone payloads; normalize missing values to `SELF_PROFILE_ID`.
    - [ ] Key journal merges by `(profileId, entry_id)`, measurements by `(profileId, entry_id)`, goals by `(profileId, parameterId)`, and cards by `(profileId, id)`.
    - [ ] Keep Self on the legacy filenames; write non-Self profiles to UUID-suffixed filenames so old clients never ingest sibling data.
    - [ ] Refactor `SyncWorker` to iterate all profiles, sync each domain per profile, and prune tombstones per profile.
    - [ ] Do not add workout/preset sync.
    - [ ] Run the JVM sync tests; commit.

- [ ] Task: Extend full backup to v2 with all profiles (Red/Green)
    - [ ] Write failing `BackupWriterTest`, `BackupDataReaderTest`, `FullBackupUseCaseTest`, and `ManifestValidatorTest` cases: v2 includes `profiles.json`; all archived/active profiles and their rows are present; v1 still reads; media paths are profile-scoped.
    - [ ] Add `profiles: List<ProfileEntity>` to `BackupData`; bump `BACKUP_FORMAT_VERSION` to 2; write `profiles.json` and include it in the manifest contents.
    - [ ] Add `profileId` to every backed-up entity; keep v1 readers tolerant by defaulting missing owners to Self.
    - [ ] Scope media entry names by profile/entry to prevent same-name collisions.
    - [ ] Update `FullBackupUseCase` to read every profile, including archived, and every domain.
    - [ ] Run the JVM backup tests; commit.

- [ ] Task: Restore v2 and legacy v1 backups safely (Red/Green)
    - [ ] Write failing `RestoreRepositoryTest`, `RestoreIntegrationTest`, `RestoreCoordinatorTest`, and `RestoreViewModelTest` cases: v2 replaces all profiles; v1 clears and restores only Self while preserving other profiles; profile count is shown; media URIs remap per profile.
    - [ ] Pass the backup format version and profile metadata through `RestoreCoordinator`, `RestoreRepository`, `RestoreWorker`, and `RestoreUiState`.
    - [ ] For v2, wipe and recreate all profiles and rows in one transaction; for v1, wipe only Self.
    - [ ] Re-import media into profile-scoped paths and remap every URI.
    - [ ] After restore, call `ProfileRepository.ensureSelfProfile()` and re-sync.
    - [ ] Run the JVM restore tests and the device `RestoreIntegrationTest`; commit.

- [ ] Task: Add profile selection to PDF export (Red/Green + UI test)
    - [ ] Write failing `PdfExportUseCaseTest` and `ExportScreenTest` cases: default is active; selecting A+B produces two labeled sections; archived profiles are absent; ZIP still exports all profiles and ignores the PDF date range.
    - [ ] Add active-profile options and selection state to `ExportViewModel`/`ExportScreen`; keep ZIP UI as the all-profile full backup.
    - [ ] Change `PdfExportUseCase.execute(profileIds, startDate, endDate)` to read each selected profile explicitly and add a profile heading before its entries.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Require an explicit Health Connect target (Red/Green + UI test)
    - [ ] Write failing `JournalViewModelTest` and `AddEntryScreenTest` cases: import shows the active profile name; confirming writes only to the captured profile; switching while the dialog is open cannot redirect the import.
    - [ ] Add `syncHealthData(timestamp, targetProfileId)` and a confirmation dialog before both the permission callback and quick-sync path.
    - [ ] Capture the target profile id when the dialog opens and pass it through the import.
    - [ ] Run unit and Compose tests; commit.

- [ ] Task: Conductor - User Manual Verification 'Phase 5: Sync, backup/restore, PDF export, and Health Connect' (Protocol in workflow.md)
    - [ ] Sync two profiles to Drive; verify sibling data is absent from a legacy-looking Self payload and present in its own file.
    - [ ] Restore a v2 backup and verify all profiles and boundaries return; restore a v1 backup and verify only `Me` changes.
    - [ ] Export a PDF for A, then A+B; verify sections and no archived profiles. Export ZIP and verify every profile is present.
    - [ ] Import Health Connect data under A after confirming the dialog; verify no row appears under B.
    - [ ] Delete an archived profile and verify its local and cloud data are removed.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

## Phase 6: Docs and full verification

- [ ] Task: Add localized strings and accessibility labels
    - [ ] Add every profile/switcher/manage/delete/export/Health Connect string to `values/strings.xml` and `values-ru/strings.xml`.
    - [ ] Add content descriptions for the avatar, chip, relationship, and destructive actions; do not rely on color alone.
    - [ ] Run `LocalizationParityTest`, `HardcodedStringAuditTest`, and the Compose UI tests; commit.

- [ ] Task: Write the feature PSD and test-case docs
    - [ ] Create `Docs/psd/profiles-for-siblings.md` from `Docs/_templates/psd.md`; document the single-database model, active-profile provider, top-bar/switcher, profile-scoped repositories, sync file naming, backup v2/v1 restore, PDF selection, and Health Connect confirmation.
    - [ ] Create `Docs/tests/profiles-for-siblings.md` from `Docs/_templates/tests.md`; list the migration, DAO isolation, repository, ViewModel, sync, backup/restore, export, Health Connect, and Compose tests.
    - [ ] Update `Docs/prd/profiles-for-siblings.md` status from planned to shipped and keep its `Last updated` accurate.
    - [ ] Update `Docs/index.md` to list the PRD, PSD, and tests paths and remove `— planned`.
    - [ ] Run `python3 wiki/lint.py`; commit.

- [ ] Task: Update the wiki pages affected by the code
    - [ ] Update `wiki/modules/data-layer.md` for the profile table, ownership, migration, and scoped DAOs/repositories.
    - [ ] Update `wiki/modules/viewmodel-layer.md` for active-profile injection and per-screen scoping.
    - [ ] Update `wiki/modules/ui-layer.md` for the top-bar chip and Manage Profiles.
    - [ ] Update `wiki/modules/sync-engine.md` for profile metadata, filenames, deletion propagation, and legacy fallback.
    - [ ] Update `wiki/modules/export-restore.md` for full backup v2, v1 restore-to-Self, and PDF profile selection.
    - [ ] Update `wiki/integrations/health-connect.md` for the confirmation and target capture.
    - [ ] Cite paths; do not copy mutable values.
    - [ ] Run `python3 wiki/lint.py` and commit the wiki changes with the code.

- [ ] Task: Full verification and README-free cleanup
    - [ ] Run `./gradlew build` and `./gradlew :app:testDebugUnitTest`; both must pass.
    - [ ] Run the affected instrumented suites: migration, DAO isolation, profile UI, history, add entry, personal card, measurements, workout, presets, export, restore, and Health Connect.
    - [ ] Run `python3 wiki/lint.py`; exit 0 required.
    - [ ] Run `git diff --check`; fix whitespace.
    - [ ] Confirm no `opencode.json`, draft, or unrelated file is staged.
    - [ ] Commit only the intended verification fixes.

- [ ] Task: Conductor - User Manual Verification 'Phase 6: Docs and full verification' (Protocol in workflow.md)
    - [ ] Walk every acceptance criterion in `spec.md` against the built app.
    - [ ] Verify upgrade, fresh install, create, switch, archive, restore, delete, sync, backup/restore, PDF selection, Health Connect, and localization.
    - [ ] Verify `python3 wiki/lint.py` exits 0.
    - [ ] Ask: **Does this meet your expectations? Please confirm with yes or provide feedback on what needs to be changed.**

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
