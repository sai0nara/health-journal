# Specification: Disable Health Sync (User Toggle, Default OFF)

## Overview
Add a user-facing setting to disable Health Connect sync. When OFF, the app makes no Health Connect calls and requests no health permissions. Default is OFF (privacy-first opt-in). Existing synced values in journal entries and workouts are preserved. Manifest permission declarations are kept; only runtime requests/calls are gated.

## Functional Requirements
1. Settings toggle persisted locally (follow `UnitSettings` SharedPreferences pattern, e.g. `HealthSyncSettings`): boolean `healthSyncEnabled`, default `false`.
2. `SettingsScreen` gains a Health sync section: Switch with testTag `settings_health_sync_toggle`, label + hint explaining opt-in behavior.
3. When toggle OFF:
   - `AddEntryScreen` hides or disables the Sync Health action in `EnrichmentPanel` (no `checkHealthAvailability`, no `hasHealthPermissions`, no permission launcher, no `syncHealthData`).
   - Workout Health Connect paths (`HealthConnectWorkoutDataSource`, `HealthConnectManager` reads for BP/HR/sleep, write ExerciseSession) are not invoked.
   - No Health Connect permission prompts are triggered from any screen.
4. When toggle ON: current behavior fully preserved (availability check, permission flow, `syncHealthData`, workout read/write).
5. ViewModels expose `isHealthSyncEnabled: StateFlow<Boolean>` (or equivalent) sourced from the persisted setting; UI collects it to gate sync entry points.
6. Migration: all installs (new + existing) default to OFF — existing users who previously synced must explicitly opt back in. No data migration; stored BP/HR/sleep values untouched.
7. Manifest (`AndroidManifest.xml`, `health_permissions.xml`) unchanged.

## Non-Functional Requirements
- Privacy: zero Health Connect SDK calls and zero permission dialogs when disabled.
- Performance: setting read is cheap, cached in memory via Flow; no blocking I/O on main thread.
- UX: toggle state survives process death/restart; disabled state clearly communicates that Sync Health is off.

## Acceptance Criteria
- [ ] Fresh install shows Health sync OFF in Settings.
- [ ] Toggling ON then restarting the app preserves ON; same for OFF.
- [ ] With OFF: Sync Health action not actionable (hidden or disabled) in AddEntry; no permission dialog appears; logcat shows no HealthConnectManager calls.
- [ ] With ON: Sync Health flow works as today (permission → fetch BP/HR/sleep → populate fields).
- [ ] With OFF: Workout screens do not read from or write to Health Connect.
- [ ] Existing entries keep their BP/HR/sleep values after update; Drive sync payloads unchanged.
- [ ] Unit tests for settings persistence + gating logic; Compose UI test for toggle and Sync Health visibility.

## Out of Scope
- Removing Health Connect dependencies, manifest entries, or `HealthConnectManager` code.
- Clearing or backfilling historical health data.
- Changes to Google Drive sync, export, or restore.
- Automatic/background health sync scheduling.
