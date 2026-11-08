# Profiles for Siblings — Product Specification

> A single Room database stores all profile-owned health domains with an explicit
> `profileId` foreign/owner key; a `ProfileRepository` backed by
> `SharedPreferencesActiveProfileStore` drives reactive active-profile switching,
> top-bar chips, Drive sync file partitioning, full backup v2 and legacy v1
> restore, multi-profile PDF export, and confirmed Health Connect imports.

Last updated: 2026-11-06

## Overview

The sibling-profiles design implements `Docs/prd/profiles-for-siblings.md` inside
a single Room database rather than separate databases per profile. Every scoped
table (`journal_entries`, `deleted_entries`, `body_measurements`, `goals`,
`personal_card`, `workout_sessions`, `workout_presets`) carries a non-null
`profileId` column, while `profiles` and `profile_deletions` track profile
metadata and permanent deletion tombstones. Shared reference data (the exercise
catalog) and device-level preferences (unit system, theme, account) remain
global.

An `ActiveProfileProvider` interface exposes the active profile ID as a
`StateFlow<String>` and synchronous snapshot. Repositories subscribe to
`activeProfileId` via `flatMapLatest` for reactive UI reads and accept explicit
`profileId` parameters on writes, sync, backup, restore, and export paths so
background work and mid-form switches never attribute a record to the wrong
profile.

## Architecture

- **Single database with owner column:** `JournalDatabase` adds `ProfileEntity`
  and `ProfileDeletionEntity` alongside a forward migration that creates the
  default `Self` profile and backfills `profileId` across all scoped tables.
- **Active profile state:** `ProfileRepository` implements
  `ActiveProfileProvider`, persisting the selected profile ID through
  `SharedPreferencesActiveProfileStore` and falling back to an active profile if
  the stored ID is missing or archived.
- **Erasure boundary:** `RoomProfileDataEraser` atomically removes a deleted
  profile's rows across all scoped tables, cleans up sandboxed media files, and
  records a `ProfileDeletionEntity` tombstone for Drive sync.
- **UI switcher & guards:** `ProfileSwitcher` and `ProfileAvatar` render in root
  top bars; screens with unsaved drafts or active workout sessions gate or
  confirm profile switches before `MainActivity` pops back to the root graph.
- **Profile-safe Drive sync:** `SyncWorker` syncs `profiles.json` first via
  `ProfileSyncPayload` and `SyncMerge.mergeProfiles`, applies remote deletions
  through `ProfileDataEraser`, deletes per-profile cloud files via
  `DriveServiceHelper`, and then syncs each profile's data files using legacy
  filenames for `Self` and profile-suffixed filenames for sibling profiles.
- **Backup v2 & legacy v1 restore:** `BackupWriter` writes `profiles.json` into
  v2 archives and `FullBackupUseCase` exports all active and archived profiles
  with profile-scoped media paths. `BackupDataReader` defaults missing v1
  `profileId` fields to `Self`, and `RestoreRepository` replaces all profiles on
  v2 restore while scoping v1 restore to `Self` only.

## Data flow

1. At launch, `MainActivity` invokes `ProfileRepository.ensureSelfProfile()` to
   guarantee a `Self` profile and resolve a valid active profile ID.
2. Root screens observe `ProfileViewModel.uiState` to render `ProfileSwitcher` in
   the top bar; selecting another profile checks screen-level dirty/session
   guards before calling `ProfileViewModel.switchTo`.
3. Creating or editing a journal entry, measurement, goal, preset, workout
   session, or personal card captures the target `profileId` when the form or
   session starts and writes explicitly to that `profileId`.
4. Tapping Health Connect in `AddEntryScreen` opens a confirmation dialog naming
   the captured profile and passes that `targetProfileId` into
   `JournalViewModel.syncHealthData`.
5. `ExportScreen` and `ExportViewModel` offer active-profile selection chips for
   PDF reports (`PdfExportUseCase` renders a labeled section per selected
   profile) while keeping ZIP export as the all-profile `FullBackupUseCase`.
6. `SyncWorker` merges `profiles.json` and tombstones first, then iterates every
   local profile to merge journal, measurement, goal, and personal-card payloads
   keyed by `(profileId, recordId)`.

## Components

| Component | File | Responsibility |
|---|---|---|
| Profile entities & DAOs | `app/src/main/java/com/example/healthjournal/data/local/ProfileEntity.kt` | profile row, relationship enum, and `SELF_PROFILE_ID` |
| Profile DAO | `app/src/main/java/com/example/healthjournal/data/local/ProfileDao.kt` | active/all queries, count, and upsert/delete ops |
| Deletion tombstone | `app/src/main/java/com/example/healthjournal/data/local/ProfileDeletionEntity.kt` | tombstone entity and `ProfileDeletionDao` |
| Active profile store | `app/src/main/java/com/example/healthjournal/data/ActiveProfileStore.kt` | `ActiveProfileProvider` and `SharedPreferencesActiveProfileStore` |
| Profile repository | `app/src/main/java/com/example/healthjournal/data/ProfileRepository.kt` | CRUD, active cap, archive/restore, and active selection |
| Profile data eraser | `app/src/main/java/com/example/healthjournal/data/ProfileDataEraser.kt` | transactional local data and media cleanup on permanent delete |
| Profile ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/ProfileViewModel.kt` | switcher state and Manage Profiles editor/delete flows |
| Switcher & avatar UI | `app/src/main/java/com/example/healthjournal/ui/components/ProfileSwitcher.kt` | top-bar chip, dropdown menu, and Manage Profiles entry |
| Manage Profiles screen | `app/src/main/java/com/example/healthjournal/ui/screens/ManageProfilesScreen.kt` | create, edit, archive, restore, and typed-name delete |
| Profile sync codec | `app/src/main/java/com/example/healthjournal/sync/ProfileSyncPayload.kt` | `profiles.json` serialization for profiles and deletions |
| Sync merge & worker | `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` | profile-keyed merges and tombstone propagation |
| Backup & restore | `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` | v2 all-profile backup and v1/v2 restore in `RestoreRepository` |
| PDF export | `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` | per-profile date-range sections in generated PDF reports |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Stored active profile ID is archived or missing | `ensureSelfProfile()` falls back to the first active profile (or creates `Self`) |
| Creating an 11th active profile or restoring when 10 are active | rejected with `ProfileError.ActiveLimit` |
| Creating a second `Self` profile | rejected with `ProfileError.SelfExists` |
| Archiving or deleting the last active profile | rejected with `ProfileError.LastActive` |
| Deleting a non-archived profile or mismatched typed name | rejected with `ProfileError.NotArchived` or `ProfileError.NameMismatch` |
| Switching profiles with unsaved draft or active workout | draft prompts to discard/cancel; active workout or in-flight save blocks switching |
| Active profile changes while a form, workout, or Health Connect dialog is open | captured `profileId` on form/session/dialog prevents cross-profile write |
| Remote sync delivers a profile deletion for the currently active profile | `SyncWorker` switches to a remaining active profile before erasing local rows |
| Legacy v1 backup restored onto a device with sibling profiles | `RestoreRepository` wipes and restores only `Self`, preserving sibling profiles |

## Dependencies

- Room persistence and SQLite schema migrations in `JournalDatabase`.
- Google Drive REST API (`DriveServiceHelper`) and WorkManager (`SyncWorker`,
  `RestoreWorker`).
- iText 7 (`PdfExportUseCase`) and Zip4j (`BackupWriter`, `BackupDataReader`).
- Health Connect (`HealthConnectManager`).

## Cross-references

- `Docs/prd/profiles-for-siblings.md` — the requirements this specification implements.
- `Docs/tests/profiles-for-siblings.md` — the test cases that verify this design.
- [[data-layer]] — profile-scoped Room entities, DAOs, and repositories.
- [[viewmodel-layer]] — profile-aware ViewModels and draft capture.
- [[ui-layer]] — top-bar switcher, Manage Profiles screen, and switch guards.
- [[sync-engine]] — profile metadata sync and per-profile Drive payloads.
- [[export-restore]] — backup v2, legacy v1 restore, and multi-profile PDF export.
- [[health-connect]] — explicit profile confirmation before importing vitals.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/ProfileEntity.kt` — profile entity and relationship enum.
- `app/src/main/java/com/example/healthjournal/data/local/ProfileDao.kt` — profile DAO.
- `app/src/main/java/com/example/healthjournal/data/local/ProfileDeletionEntity.kt` — deletion tombstone entity and DAO.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` — schema migration and DAO registry.
- `app/src/main/java/com/example/healthjournal/data/ActiveProfileStore.kt` — active profile preference store.
- `app/src/main/java/com/example/healthjournal/data/ProfileRepository.kt` — profile repository and validation rules.
- `app/src/main/java/com/example/healthjournal/data/ProfileDataEraser.kt` — local data and media erasure.
- `app/src/main/java/com/example/healthjournal/viewmodel/ProfileViewModel.kt` — profile management ViewModel.
- `app/src/main/java/com/example/healthjournal/ui/components/ProfileAvatar.kt` — initials avatar composable.
- `app/src/main/java/com/example/healthjournal/ui/components/ProfileSwitcher.kt` — top-bar switcher composable.
- `app/src/main/java/com/example/healthjournal/ui/screens/ManageProfilesScreen.kt` — Manage Profiles screen.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — draft capture and Health Connect target confirmation.
- `app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt` — PDF profile selection UI.
- `app/src/main/java/com/example/healthjournal/export/ExportViewModel.kt` — active-profile export state.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — multi-profile PDF generation.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — all-profile v2 backup.
- `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` — v2 all-profile and v1 Self-only restore.
- `app/src/main/java/com/example/healthjournal/sync/ProfileSyncPayload.kt` — profile metadata sync codec.
- `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` — composite-key sync merges.
- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — per-profile Drive sync orchestration.
- `Docs/prd/profiles-for-siblings.md` — requirements this specification implements.
- `Docs/tests/profiles-for-siblings.md` — test cases.
