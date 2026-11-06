# Profiles for Siblings — Product Requirements

> Let one caregiver manage separate, complete health profiles for siblings from a
> single signed-in account, without mixing their journals, measurements, goals,
> workouts, or medical cards.

Last updated: 2026-11-06

## Overview

The app is used by one signed-in account, but one adult may track more than one
sibling. Every journal entry, body measurement, goal, workout, and personal
card belongs to an explicit profile owner: a default Self profile keeps existing data, and additional dependent
profiles can be created for siblings or other family members.

Profiles are an organizational boundary, not a security boundary. They prevent
accidental mixing of health records; they do not protect one sibling's data from
another person using the unlocked device or signed-in account.

Profile ownership lives on
`app/src/main/java/com/example/healthjournal/data/local/ProfileEntity.kt`,
`app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`,
`app/src/main/java/com/example/healthjournal/data/local/PersonalCard.kt`, and the
repositories and screens that scope queries to the active profile.

## Goals / Non-goals

**Goals**

- Manage several sibling/dependent profiles from one caregiver account.
- Keep every health record attached to exactly one profile.
- Preserve all existing data by migrating it to a default Self profile.
- Make the active profile obvious and switching deliberate.
- Prevent cross-profile attribution during Health Connect import, export, backup,
  restore, and sync.
- Allow a profile to be archived and later permanently deleted without silent
  data loss.

**Non-goals**

- Multiple caregiver accounts, invitations, roles, or shared access.
- Per-profile PINs, biometric locks, or a claim that profiles provide privacy
  between device users.
- A combined all-siblings feed, dashboard, or comparison view.
- Profile photo uploads; profiles use initials and an optional accent color.
- Per-profile app settings such as units or theme.
- Clinical identity verification or provider-facing records.

## User stories

- As a caregiver, I want separate profiles for each sibling so their health
  records do not get mixed together.
- As a caregiver, I want to switch profiles quickly and always see whose data I
  am viewing.
- As a caregiver, I want to add a new sibling profile and start with an empty
  journal rather than inheriting someone else's history.
- As a caregiver, I want existing data to remain available after the update
  without manually reassigning it.
- As a caregiver, I want Health Connect imports to ask which profile they belong
  to.
- As a caregiver, I want a full backup of every profile, but a PDF export of only
  the profile I choose.
- As a caregiver, I want archived profiles to be recoverable before I permanently
  delete them.

## Functional requirements

- FR-1: On upgrade, the app creates a default Self profile named `Me` and assigns
  every existing journal entry, attachment, tag, body measurement, goal, workout
  preset, workout session, and personal card to it. On a fresh install, the same
  empty Self profile is created.
- FR-2: The app supports up to 10 non-archived profiles. The initial `Me`
  profile has the `Self` relationship and additional profiles use `Sibling`,
  `Child`, `Parent`, or `Other`. At most one profile may be `Self`; because
  `Self` is an ordinary profile, it may be archived or deleted when another
  active profile exists, and a new `Self` may be created only while none exists.
- FR-3: A profile has a non-blank display name, a relationship, and an optional
  accent color. The avatar is generated from the name's initials; profile photo
  upload is not supported.
- FR-4: Profiles can be created and edited from Manage Profiles. Editing a
  profile's name, relationship, or color must not move or relink its data.
- FR-5: The active profile is shown in the top app bar on every root screen and
  is persisted across launches. If the persisted profile is no longer active,
  the app falls back to another active profile.
- FR-6: Switching profiles is explicit. All root screens, lists, search results,
  and filters show only the active profile's data; there is no all-profiles view.
- FR-7: Switching while an entry or profile edit has unsaved changes prompts the
  user to discard or cancel. Switching is blocked while a save is in progress.
  After a switch, root tabs reload for the new profile and any open detail or
  edit screen closes.
- FR-8: A new profile starts with an empty state and no inherited entries,
  measurements, goals, workouts, cards, tags, or attachments.
- FR-9: The following data belongs to one profile: journal entries and their
  photos/attachments, tags, body measurements, goals, workout presets, workout
  sessions, and the personal card. The built-in exercise catalog, unit system,
  theme, account, and other device-level settings remain shared.
- FR-10: Health Connect import is always initiated for the active profile and
  must show the target profile name before writing. Imported data is never
  silently assigned to another profile or split across profiles.
- FR-11: A full backup includes every profile, including archived profiles, and
  preserves profile boundaries. Restore recreates profiles and their data
  without merging them.
- FR-12: Date-range PDF export defaults to the active profile. The user may
  select multiple active profiles; each selected profile's data is separated and
  labeled. Archived profiles are not offered for export. The ZIP format remains
  the full backup described in FR-11 and is not filtered by the PDF date range.
- FR-13: All profiles, including archived profiles, sync under the signed-in
  Drive account. A sync, restore, or conflict-resolution path must never
  reassign a record to a different profile.
- FR-14: Archiving a profile removes it from the switcher and all daily screens
  but retains its data. If the active profile is archived, the app switches to
  another active profile first.
- FR-15: Archived profiles are managed from Manage Profiles, where the user can
  restore them or permanently delete them. Restoring is blocked while the
  active-profile limit is reached.
- FR-16: Permanent deletion requires typing the profile name, deletes that
  profile's local and cloud data, and cannot be undone.
- FR-17: The last active profile cannot be archived or permanently deleted. A
  profile must be archived before it can be permanently deleted.
- FR-18: An old backup or Drive payload that predates profiles is restored into
  the Self profile. A backup created after this feature restores every profile
  it contains.

## Non-functional requirements

- **Isolation invariant:** no query, cache, draft, export, backup, log, or sync
  upload may read or write health data across profile boundaries.
- **Offline-first:** profile creation, switching, editing, archiving, and all
  health writes work locally; sync follows when the account is available.
- **Migration safety:** the upgrade is forward-only, runs once, preserves every
  existing row, and leaves the database usable if the app is interrupted.
- **Backward compatibility:** existing backup and Drive payloads without profile
  metadata remain readable and restore into the Self profile.
- **Privacy:** profile names and health data are not written to logs or analytics.
  The UI states that profiles are organizational, not an authentication boundary.
- **Correct attribution:** a failed save or interrupted switch must never leave a
  draft attached to a different profile.
- **Performance:** switching loads only the active profile's data; it does not
  copy or scan the entire database.
- **Accessibility and localization:** profile identity is conveyed by name and
  relationship, not color alone; all new UI strings ship in English and Russian.

## Acceptance criteria

- AC-1: After upgrading an existing install, all prior journal entries,
  measurements, goals, workouts, and the personal card appear under `Me` with no
  data loss.
- AC-2: A caregiver can create a sibling profile with a name, relationship, and
  color; it appears in the switcher and starts empty.
- AC-3: The active profile name is visible on every root screen, and the selection
  survives an app restart.
- AC-4: A record created under profile A is absent from every list, search, and
  export while profile B is active, and reappears when A is active again.
- AC-5: Changing units or theme under one profile is reflected under every
  profile.
- AC-6: A Health Connect import names the active profile before writing and
  produces no rows under any other profile.
- AC-7: A full backup contains all profiles; restoring it preserves every
  profile and its data boundaries. An old profile-less backup restores into
  `Me`.
- AC-8: A PDF export contains only the active profile by default, and only the
  explicitly selected active profiles when multiple are chosen; a ZIP export is
  the all-profile full backup from AC-7.
- AC-9: Archiving a profile removes it from the switcher but keeps its data
  recoverable; restoring it brings the profile and data back.
- AC-10: Permanently deleting an archived profile requires its typed name and
  removes its data; the last active profile cannot be archived or deleted.
- AC-11: Switching profiles with an unsaved draft prompts first and never writes
  the draft to the new profile.
- AC-12: No screen or export offers a combined all-profile list.

## Out of scope

- Multiple caregiver accounts, sharing, invitations, roles, or remote access.
- Profile-level authentication, PINs, biometric gates, or encryption keys.
- Combined cross-profile dashboards, feeds, comparisons, or analytics.
- Profile photo uploads or custom image avatars.
- Per-profile unit systems, themes, or notification settings.
- Per-profile Health Connect accounts or attribution of device-level sensor data.
- Clinician portals, provider sharing, or insurance workflows.
- Reminders, notifications, or background profile-specific jobs.

## Cross-references

- `Docs/psd/profiles-for-siblings.md` — the specification, added when the feature
  is built.
- `Docs/tests/profiles-for-siblings.md` — the test cases, added when the feature
  is built.
- [[data-layer]] — the entities and DAOs that gain a profile owner.
- [[ui-layer]] — the top-bar switcher and profile-scoped screens.
- [[sync-engine]] — profile-safe Drive payloads and conflict handling.
- [[export-restore]] — backup and restore across profiles.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — entries currently have no profile owner.
- `app/src/main/java/com/example/healthjournal/data/local/PersonalCard.kt` — the personal card is currently a singleton row.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` — the entity registry and migration chain that will gain profile ownership.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — repository-wide reads that must become profile-scoped.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — UI state currently reads all entries.
- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — Drive sync entry point that must preserve profile boundaries.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — full backup and restore.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — PDF export scope.
- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — Health Connect reads that must target the active profile.
- `app/src/main/java/com/example/healthjournal/data/local/UnitSettings.kt` — the shared unit preference that stays account-wide.
- `Docs/prd/personal-card.md` — the existing single-card requirements this feature extends.
