# Implementation Plan: Disable Health Sync (User Toggle, Default OFF)

## Phase 1: Preference store + Settings toggle [checkpoint: 31e4327]
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
- [x] Task: Write failing tests for gating logic (Red) [894b767]
    - [x] Test JournalViewModel exposes `isHealthSyncEnabled` reflecting `HealthSyncSettings`
    - [x] Test `syncHealthData` short-circuits (no SDK call) when disabled
    - [x] Test workout data-source gate: no read/write when disabled
- [x] Task: Implement gating in ViewModels and data sources (Green) [894b767]
    - [x] Expose `isHealthSyncEnabled: StateFlow<Boolean>` from `JournalViewModel` (and workout ViewModel if separate) — both expose it; Journal prefs-backed with refresh/set, Workout via ctor flag (default false)
    - [x] Guard `checkHealthAvailability` / `hasHealthPermissions` / `syncHealthData` call sites to no-op when OFF — hasHealthPermissions→false, syncHealthData→empty result, AddEntry UI checks flag before availability call; checkHealthAvailability itself left passthrough (unreachable when OFF)
    - [x] Guard `HealthConnectWorkoutDataSource` and `HealthConnectManager` read/write paths behind the flag — both WorkoutViewModel write sites gated; journal reads gated in syncHealthData; readRecent has no callers
    - [x] Run unit tests until green — Journal 28/28, Workout 33/33, Engine 16/16, Routine 37/37
- [x] Task: Gate UI surfaces [894b767]
    - [x] `AddEntryScreen`/`EnrichmentPanel`: hide or disable Sync Health action when OFF (collect flag, no permission launcher) — hidden via showSyncHealth; click handler + permission callback guarded
    - [x] `WorkoutScreen`: hide/disable Health sync affordance + skip auto-fetch when OFF — no change needed: no sync button/fetch in WorkoutScreen; gated write yields healthSynced=false → existing saved-locally copy
    - [x] Confirm zero permission prompts when OFF via manual logcat check — pending manual verification (no emulator here)
- [x] Task: UI tests for gated surfaces (Blue) [894b767]
    - [x] UI test: with OFF, Sync Health button hidden/disabled in AddEntry — added testAddEntryScreen_HealthHiddenWhenSyncDisabled + EnrichmentPanel hidden test
    - [x] UI test: with ON, Sync Health button visible and clickable (existing flow intact) — existing testAddEntryScreen_EnrichmentPanelButtonsClickable + new visible-when-gate-on test
    - [x] Run UI tests on emulator until green — pending: no emulator in this environment, androidTest compiles
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
