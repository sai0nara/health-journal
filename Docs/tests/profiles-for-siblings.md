# Profiles for Siblings — Test Cases

> Maps the PRD's acceptance criteria and the PSD's isolation invariants to
> automated JVM unit and instrumented tests covering database migration, DAO
> isolation, repository/ViewModel scoping, UI switching, Drive sync, backup v2 /
> v1 restore, PDF export, and Health Connect confirmation.

Last updated: 2026-11-06

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/data/ProfileRepositoryTest.kt` | `ensureSelfProfile`, create/update/archive/restore/delete rules, active cap, fallback selection |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/ProfileViewModelTest.kt` | switcher state, editor validation, archive/restore, typed-name delete confirmation |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | active-profile journal queries, explicit `profileId` writes, tag scoping |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/PersonalCardRepositoryTest.kt` | per-profile card isolation and pending-sync scoping |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` | per-profile workout sessions and single-unfinished-session invariant per profile |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/PresetRepositoryTest.kt` | per-profile presets with shared exercise catalog |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | captured `profileId` on entry save/edit and explicit `targetProfileId` on Health Connect sync |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` | reload on profile switch and captured `profileId` on card save |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` | profile-scoped measurements, goal updates, and captured `profileId` on save |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModelTest.kt` | profile-scoped trends and goal progress |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` | captured session `profileId` across active-profile switches |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` | captured preset `profileId` on create and edit |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/ProfileSyncPayloadTest.kt` | `profiles.json` round-trip and legacy empty-payload defaults |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/SyncMergeProfileTest.kt` | profile LWW merge, deletion tombstones, and Self preservation |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/SyncWorkerProfileTest.kt` | remote deletion erasure, active-profile fallback, and Drive file cleanup |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/SyncMergeTest.kt` | `(profileId, entry_id)` journal merge and cross-profile tombstone isolation |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncMergeTest.kt` | `(profileId, entry_id)` measurement merge and tombstone isolation |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/GoalSyncMergeTest.kt` | `(profileId, parameterId)` goal merge and per-profile freshness pruning |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/PersonalCardSyncMergeTest.kt` | `(profileId, id)` personal-card merge isolation |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` | v2 backup across active and archived profiles with profile-scoped media paths |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` | v2 all-profile replacement and v1 Self-only restore preserving sibling profiles |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/PdfExportUseCaseTest.kt` | active-profile default and multi-profile labeled PDF sections |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | schema 21→22 migration creates `Me` (`self`) and preserves all existing rows |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` | DAO-level isolation and `RoomProfileDataEraser` cleanup |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/WorkoutDaoTest.kt` | per-profile workout session and preset DAO isolation |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ManageProfilesScreenTest.kt` | create, edit, archive, restore, and typed-name delete UI flows |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` | dirty-draft switch guard, captured profile on save, and Health Connect confirmation dialog |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenTest.kt` | PDF active-profile selector, archived profile exclusion, and ZIP full-backup toggle |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` | end-to-end v2 multi-profile and v1 legacy restore against Room |

## Test cases

| ID |Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Upgrade migration preserves data under `Me` | v21 database with journal, measurement, goal, workout, preset, and card rows | Run `MIGRATION_21_22` | `Me` (`self`) profile exists; all pre-existing rows belong to `self` |
| T-2 | Profile creation and active limit | Fewer than 10 active profiles | Create sibling profile with name, relationship, and color; attempt 11th active profile | Profile appears in switcher and starts empty; 11th active profile fails with limit error |
| T-3 | Active profile persistence and fallback | Active profile set to sibling A | Restart app; archive A | Active profile survives restart; archiving A switches to another active profile |
| T-4 | Cross-profile data isolation | Profiles A and B exist | Log journal entry, measurement, goal, workout, preset, and card under A; switch to B | B shows empty states across all screens; switching back to A restores A's data |
| T-5 | Draft and session switch guards | Unsaved entry draft or active workout under A | Attempt to switch to B from top bar | Unsaved draft prompts to discard or cancel; active workout blocks switching |
| T-6 | Health Connect explicit target confirmation | Profile A (`Alex`) is active; Health Connect enabled | Tap Health Connect on Add Entry; switch active profile state while dialog is open; confirm Import | Dialog names `Alex`; confirmed import and saved entry apply only to `Alex` |
| T-7 | Multi-profile Drive sync and deletion | Profiles `Me` and `Alex` synced to Drive | Sync both profiles; archive and permanently delete `Alex`; sync again | `Me` uses legacy filenames; `Alex` uses profile-suffixed filenames; deleting `Alex` writes tombstone and removes `Alex`'s local and Drive files |
| T-8 | Full backup v2 and legacy v1 restore | Active and archived profiles with data and media | Export ZIP backup (v2) and restore; then restore a legacy v1 ZIP backup | v2 restore recreates all profiles and media boundaries; v1 restore replaces only `Me` while leaving sibling profiles intact |
| T-9 | PDF export profile selection | Active profiles `Me` and `Alex`, archived profile `Sam` | Open Export screen in PDF mode; select `Me` and `Alex`; generate PDF | `Me` is selected by default; `Sam` is absent; generated PDF contains labeled sections for `Me` and `Alex` in order |
| T-10 | Russian localization and accessibility | App strings in `values` and `values-ru` | Run `LocalizationParityTest` and `HardcodedStringAuditTest` | Every profile, switcher, export, and Health Connect string is localized in English and Russian |

## Manual checks

- Sign in with Google Drive on two devices, create a sibling profile, sync, and verify profile-suffixed files in Drive `appDataFolder`.
- Export a multi-profile PDF report and inspect section headers and embedded photos in a PDF viewer.
- Grant Health Connect permissions on a physical device and verify the profile confirmation dialog before initial permission grant and on subsequent quick-sync imports.

## Cross-references

- `Docs/prd/profiles-for-siblings.md` — the requirements under test.
- `Docs/psd/profiles-for-siblings.md` — the design the cases verify.
- [[unit-tests]] — JVM unit test conventions.
- [[instrumented]] — instrumented test conventions.

## Sources

- `app/src/test/java/com/example/healthjournal/data/ProfileRepositoryTest.kt` — profile repository unit tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/ProfileViewModelTest.kt` — profile ViewModel unit tests.
- `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` — journal repository scoping tests.
- `app/src/test/java/com/example/healthjournal/data/PersonalCardRepositoryTest.kt` — personal card repository scoping tests.
- `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` — workout repository scoping tests.
- `app/src/test/java/com/example/healthjournal/data/PresetRepositoryTest.kt` — preset repository scoping tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — journal and Health Connect target unit tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` — personal card ViewModel scoping tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` — measurement ViewModel scoping tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModelTest.kt` — analytics ViewModel scoping tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` — workout ViewModel scoping tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` — preset ViewModel scoping tests.
- `app/src/test/java/com/example/healthjournal/sync/ProfileSyncPayloadTest.kt` — profile sync payload unit tests.
- `app/src/test/java/com/example/healthjournal/sync/SyncMergeProfileTest.kt` — profile sync merge unit tests.
- `app/src/test/java/com/example/healthjournal/sync/SyncWorkerProfileTest.kt` — profile sync worker unit tests.
- `app/src/test/java/com/example/healthjournal/sync/SyncMergeTest.kt` — journal composite-key merge unit tests.
- `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncMergeTest.kt` — measurement composite-key merge unit tests.
- `app/src/test/java/com/example/healthjournal/sync/GoalSyncMergeTest.kt` — goal composite-key merge unit tests.
- `app/src/test/java/com/example/healthjournal/sync/PersonalCardSyncMergeTest.kt` — personal card composite-key merge unit tests.
- `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` — v2 full backup unit tests.
- `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` — v2 and v1 restore repository unit tests.
- `app/src/test/java/com/example/healthjournal/export/PdfExportUseCaseTest.kt` — multi-profile PDF export unit tests.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — migration 21→22 instrumented tests.
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — DAO scoping and eraser instrumented tests.
- `app/src/androidTest/java/com/example/healthjournal/data/local/WorkoutDaoTest.kt` — workout and preset DAO scoping tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ManageProfilesScreenTest.kt` — Manage Profiles Compose tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` — AddEntry profile switch and Health Connect Compose tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenTest.kt` — Export screen PDF profile selector Compose tests.
- `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` — end-to-end backup/restore instrumented tests.
- `Docs/prd/profiles-for-siblings.md` — requirements under test.
- `Docs/psd/profiles-for-siblings.md` — design the cases verify.
