# Plan: Move Sign-In from History Screen to Settings

## Phase 1: Settings account section (TDD)

- [x] Task: Add settings_* strings (en + ru)
    - [x] Add section title, Sign In, Sign Out, signed-in email label to values/strings.xml + values-ru/strings.xml
    - [x] Write failing test asserting new strings resolve (no hardcoded literals)
- [x] Task: Expose signed-in email for Settings UI
    - [x] Decide accessor: reactive userEmail on IJournalViewModel vs SessionManager read in composition; write failing unit test first
    - [x] Implement minimal accessor (auth logic otherwise untouched)
- [x] Task: Build account section in SettingsScreen
    - [x] Write failing Compose UI tests: signed-out shows Sign In (calls viewModel.signIn); signed-in shows email + Sign Out (calls viewModel.signOut); sync status shown
    - [x] Change SettingsScreen signature to accept IJournalViewModel; add resolution launcher; render section with testTags
    - [x] Refactor to match existing Settings section styling; rerun UI tests green
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: History cleanup + navigation wiring

- [x] Task: Remove Sign In from HistoryScreen
    - [x] Write failing test: signed-out History shows no Sign In button; status/Sync Now/pull-to-refresh still present
    - [x] Remove Sign In button block + unused authorizationLauncher and imports; keep sync-status text, overflow Sync Now, pull-to-refresh
- [x] Task: Wire shared ViewModel into settings destination
    - [x] Pass shared JournalViewModel to SettingsScreen in MainActivity.kt settings composable
    - [x] Update SettingsScreenTest/Preview call sites for new signature
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Test migration + full verification

- [x] Task: Migrate CloudSyncTest sign-in flow to Settings
    - [x] Rewrite testSignInTriggered against SettingsScreen; remove History-based sign-in assertions
    - [x] Run ./gradlew :app:testDebugUnitTest green; run Compose UI tests on emulator until green
- [x] Task: Retire history_sign_in string + final sweep
    - [x] Remove history_sign_in (en+ru) if unreferenced; grep for stale signIn references in History
    - [x] Verify Russian locale renders new section; verify coverage of touched files
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase: Review Fixes

- [~] Task: Apply review suggestions
