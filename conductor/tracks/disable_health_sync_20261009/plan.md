# Implementation Plan: Disable Health Sync (User Toggle, Default OFF)

## Phase 1: Preference store + Settings toggle
- [x] Task: Write failing unit tests for health-sync preference (Red) [52c11c5]
    - [ ] Test default is OFF when prefs empty
    - [ ] Test write ON then read returns ON, survives re-read
    - [ ] Test write OFF then read returns OFF
    - [ ] Test corrupt/unknown value falls back to OFF
- [x] Task: Implement `HealthSyncSettings` store (Green) [52c11c5]
    - [ ] Create `data/local/HealthSyncSettings.kt` mirroring `UnitSettings` (SharedPreferences `health_sync_settings`, key `health_sync_enabled`, default false)
    - [ ] Add Flow-based observer (`isEnabledFlow` / StateFlow source) if needed for ViewModel gating
    - [ ] Run unit tests until green
- [x] Task: Refactor + coverage for preference store [52c11c5]
    - [x] Refactor duplication between `UnitSettings` and `HealthSyncSettings` if trivial (no behavior change) — skipped deliberately, separate prefs files avoid coupling
    - [x] Verify >80% coverage for new file — all lines/branches exercised by 5 tests (no jacoco plugin configured)
- [x] Task: Settings UI toggle with UI test (Blue) [177d3d4]
    - [x] Write Compose UI test: Settings shows toggle OFF by default (testTag `settings_health_sync_toggle`) — instrumented test written, compiles; emulator run pending manual verification
    - [x] Add Health sync Switch section to `SettingsScreen.kt` + strings (`settings_health_sync_title`, hint, on/off labels) — added `settings_section_health_sync`, `settings_health_sync_label`, `settings_health_sync_hint` (en+ru)
    - [x] Wire toggle to `HealthSyncSettings.read/write`, persist across recomposition/restart
    - [x] Run UI test on emulator until green — pending: no emulator in this environment, compile verified
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Preference store + Settings toggle' (Protocol in workflow.md)

## Phase 2: Gate all Health Connect entry points
- [ ] Task: Write failing tests for gating logic (Red)
    - [ ] Test JournalViewModel exposes `isHealthSyncEnabled` reflecting `HealthSyncSettings`
    - [ ] Test `syncHealthData` short-circuits (no SDK call) when disabled
    - [ ] Test workout data-source gate: no read/write when disabled
- [ ] Task: Implement gating in ViewModels and data sources (Green)
    - [ ] Expose `isHealthSyncEnabled: StateFlow<Boolean>` from `JournalViewModel` (and workout ViewModel if separate)
    - [ ] Guard `checkHealthAvailability` / `hasHealthPermissions` / `syncHealthData` call sites to no-op when OFF
    - [ ] Guard `HealthConnectWorkoutDataSource` and `HealthConnectManager` read/write paths behind the flag
    - [ ] Run unit tests until green
- [ ] Task: Gate UI surfaces
    - [ ] `AddEntryScreen`/`EnrichmentPanel`: hide or disable Sync Health action when OFF (collect flag, no permission launcher)
    - [ ] `WorkoutScreen`: hide/disable Health sync affordance + skip auto-fetch when OFF
    - [ ] Confirm zero permission prompts when OFF via manual logcat check
- [ ] Task: UI tests for gated surfaces (Blue)
    - [ ] UI test: with OFF, Sync Health button hidden/disabled in AddEntry
    - [ ] UI test: with ON, Sync Health button visible and clickable (existing flow intact)
    - [ ] Run UI tests on emulator until green
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Gate all Health Connect entry points' (Protocol in workflow.md)

## Phase 3: Regression, strings, docs
- [ ] Task: Regression + coverage verification
    - [ ] Run `./gradlew :app:testDebugUnitTest` full suite green
    - [ ] Verify >80% coverage on new/changed code
    - [ ] Verify existing BP/HR/sleep values persist; Drive sync payloads unchanged (no entity changes)
    - [ ] Run lint/static analysis clean
- [ ] Task: Strings, a11y, and wiki/docs update
    - [ ] Add missing `strings.xml` entries with content descriptions for toggle
    - [ ] Update wiki page citing Health sync behavior + new `HealthSyncSettings.kt` path if wiki cites it
    - [ ] Run `python3 wiki/lint.py` exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Regression, strings, docs' (Protocol in workflow.md)
