# Track sibling_profiles_20261031 — Specification

## Overview

Implement `Docs/prd/profiles-for-siblings.md`: one signed-in caregiver account
owns several sibling/dependent profiles, with a default Self profile preserving
all pre-existing data. Every health record belongs to exactly one profile; there
is no combined all-profile view.

The implementation keeps one Room database and adds a non-null profile owner to
the scoped tables. Repositories, DAOs, ViewModels, sync payloads, backup, restore,
and export all require an explicit profile. The active profile is a device-level
preference, surfaced in the top bar and changed only through a deliberate switch.

This is a single-track rollout in six phases: data foundation, profile
management UI, journal/personal card, measurements/goals/workouts, sync/export/
Health Connect, then docs and full verification.

## Functional Requirements

- FR-1: Add a `Profile` entity with a stable id, display name, relationship
  (`Self`, `Sibling`, `Child`, `Parent`, `Other`), optional accent color,
  archive state, timestamps, and sync state. The initial `Me` profile has the
  `Self` relationship; at most one profile may be `Self`. `Self` is an ordinary
  profile: it may be archived or deleted when another active profile exists, and
  a new `Self` may be created only while none exists. A maximum of 10
  non-archived profiles is enforced.
- FR-2: On upgrade, create the Self profile named `Me` and attach every existing
  journal entry, attachment, tag association, body measurement, goal, workout
  preset, workout session, personal card, and deletion tombstone to it. A fresh
  install creates the same empty Self profile.
- FR-3: The profile owner is non-null on every scoped table:
  `journal_entries`, `deleted_entries`, `body_measurements`, `goals`,
  `personal_card`, `workout_sessions`, and `workout_presets`. Tags follow their
  journal entry. The exercise catalog remains shared reference data.
- FR-4: All DAO and repository reads/writes accept or resolve an active profile
  and never return rows from another profile. `PersonalCard` is one row per
  profile; goals are keyed per profile; an unfinished workout session is unique
  per profile, not per device.
- FR-5: The active profile id is persisted in a device-level preference, restored
  at launch, and falls back to an active profile if the stored id is archived or
  missing. The active profile's name/avatar is visible in the top bar on every
  root screen.
- FR-6: A top-bar profile chip opens the switcher; a Manage Profiles screen
  supports create, edit, archive, restore, and permanent delete. Create/edit
  collects display name, relationship, and optional accent color; the avatar is
  generated from initials.
- FR-7: Switching with an unsaved entry or profile draft prompts to discard or
  cancel. Switching is blocked while a save is in progress or a workout session
  is active. After switching, root tabs reload for the new profile and any
  detail/edit screen closes.
- FR-8: History, Archive, Measurements, Workouts, Presets, Personal Card, search,
  filters, and empty states are scoped to the active profile. No screen offers an
  all-profile feed.
- FR-9: Health Connect import requires an explicit confirmation naming the
  active profile before writing. No imported value may be assigned to another
  profile.
- FR-10: Sync uploads all profiles under the signed-in account. Payloads carry
  profile ownership, tombstones remain profile-scoped, and merge/conflict paths
  never reassign a row. Profile deletion propagates so deleted profiles do not
  reappear from another device.
- FR-11: A full backup contains every profile, including archived profiles, and
  preserves profile boundaries. Restore atomically recreates profiles and data;
  an old backup without profile metadata restores into the Self profile.
- FR-12: Date-range PDF export defaults to the active profile. The user may
  select multiple active profiles; each profile's content is separated and
  labeled. Archived profiles are not offered for export. ZIP remains the
  all-profile full backup from FR-11 and ignores the PDF date range.
- FR-13: Unit system, theme, account, and the exercise catalog remain shared.
  Changing them under one profile affects every profile.
- FR-14: Profile create, switch, archive, restore, delete, and empty states ship
  in English and Russian, with accessible labels that do not rely on color alone.
- FR-15: Update `Docs/prd`, `Docs/psd`, and `Docs/tests` for this feature, plus
  the wiki pages that describe the affected data, UI, sync, and export paths.

## Non-Functional Requirements

- **Isolation invariant:** no query, cache, draft, export, backup, log, or sync
  upload may read or write health data across profile boundaries.
- **Offline-first:** profile operations and all health writes work locally; sync
  follows when the account and network are available.
- **Migration safety:** the upgrade is forward-only, idempotent, and atomic. An
  interruption must leave the previous database usable; no existing row may be
  lost or duplicated.
- **Backward compatibility:** existing backup ZIPs and Drive payloads without
  profile metadata remain readable and restore into the Self profile.
- **Privacy:** profile names and health data are not written to logs or
  analytics. The UI states profiles are organizational, not an authentication
  boundary.
- **Performance:** switching reloads only the active profile's data; it does not
  copy or scan the entire database.
- **Correct attribution:** failed saves, interrupted switches, and process death
  must never attach a draft or session to a different profile.
- **Quality gates:** TDD red/green/refactor per task, UI tests on every
  user-facing path, >80% coverage on new code, `./gradlew build` green, and
  `python3 wiki/lint.py` exit 0.

## Acceptance Criteria

- AC-1: Upgrading an existing install places every prior record under `Me` with
  no data loss; a fresh install starts with an empty `Me`.
- AC-2: A profile created with name, relationship, and color appears in the
  switcher and starts empty; an 11th active profile is rejected.
- AC-3: The active profile name is visible on every root screen and survives an
  app restart.
- AC-4: Records created under profile A are absent from every list, search, and
  export under profile B and return when A is active again.
- AC-5: Unit and theme changes under one profile apply to every profile.
- AC-6: A Health Connect import names the active profile before writing and
  creates no rows under any other profile.
- AC-7: A full backup contains all profiles; restore preserves every profile and
  its boundaries; an old profile-less backup lands in `Me`.
- AC-8: A PDF export contains only the active profile by default, and only the
  selected active profiles when multiple are chosen; a ZIP export is the
  all-profile full backup from AC-7.
- AC-9: Archiving removes a profile from the switcher while retaining its data;
  restoring brings it back; the last active profile cannot be archived.
- AC-10: Permanent deletion requires the typed profile name, removes the profile
  and its data locally and from cloud, and cannot target the last active profile.
- AC-11: Switching with an unsaved draft prompts first and never writes the draft
  to the new profile; an active workout blocks switching.
- AC-12: No screen, backup, or export silently combines data from two profiles.

## Implementation Decisions

- One database, not a database per profile. The active profile is a
  `SharedPreferences`-backed preference, matching `UnitSettings`.
- `PersonalCard` becomes keyed by profile; goals use a profile-scoped primary
  key; workout sessions/presets and deletion tombstones carry the profile id.
- A shared `ProfileSwitcher` composable sits in root-screen top bars and opens a
  Manage Profiles route. Profile updates are reactive through a Room-backed
  `ProfileRepository`.
- Repository and ViewModel factories take the active-profile source; screens do
  not pass profile ids ad hoc through navigation arguments.
- Exercise catalog rows remain shared; preset and session usage is profile-scoped.
- Profile deletion uses a sync-visible deletion marker so another device cannot
  resurrect the profile and its records.

## Out of Scope

- Multiple caregiver accounts, sharing, invitations, roles, or remote access.
- Profile-level authentication, PINs, biometric gates, or encryption keys.
- Combined cross-profile dashboards, feeds, comparisons, or analytics.
- Profile photo uploads.
- Per-profile unit systems, themes, or notification settings.
- Per-profile Health Connect accounts or automatic sensor-data attribution.
- Clinician portals, provider sharing, or insurance workflows.
- Reminders, notifications, or background profile-specific jobs.
- Changes to the exercise catalog's sharing model.

## Cross-references

- `Docs/prd/profiles-for-siblings.md` — the product requirements this track
  implements.
- `Docs/psd/profiles-for-siblings.md` — added with this track.
- `Docs/tests/profiles-for-siblings.md` — added with this track.
- [[data-layer]] — profile ownership on Room entities and DAOs.
- [[ui-layer]] — top-bar switcher and profile-scoped screens.
- [[sync-engine]] — profile-safe Drive payloads and tombstones.
- [[export-restore]] — full backup and restore across profiles.
