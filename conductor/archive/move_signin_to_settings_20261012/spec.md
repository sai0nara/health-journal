# Spec: Move Sign-In from History Screen to Settings

## Overview

Relocate Google Sign-In from the History feed (`HistoryScreen.kt`) to the Settings screen
(`SettingsScreen.kt`). History keeps its signed-in sync UI (status text, overflow Sync Now,
pull-to-refresh); only the signed-out Sign In button moves. Settings gains a full account
section: Sign In when signed out, account email + sync status + Sign Out when signed in.
Sign-out exists in `JournalViewModel`/`GoogleAuthManager` today but has no UI; this track
surfaces it for the first time.

## Functional Requirements

1. **History cleanup** (`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`):
   remove the signed-out full-width Sign In button block; remove the now-unused
   `authorizationLauncher` (StartIntentSenderForResult) from History. Keep unchanged:
   sync-status text, overflow Sync Now item, pull-to-refresh → `syncNow()`, and all feed behavior.
2. **Settings account section**
   (`app/src/main/java/com/example/healthjournal/ui/screens/SettingsScreen.kt`): new section
   (above or below existing Units/Health-sync sections) that observes
   `IJournalViewModel.isUserSignedIn`:
   - Signed out: Sign In button calling `viewModel.signIn()` with a PendingIntent resolution
     launcher (moved from History).
   - Signed in: signed-in account email, sync status (`viewModel.syncStatus`), Sign Out button
     calling `viewModel.signOut()`.
   - Stable testTags for UI tests (e.g. sign-in button, sign-out button, account email).
3. **Navigation wiring** (`app/src/main/java/com/example/healthjournal/MainActivity.kt`): pass the
   shared `JournalViewModel` into the `"settings"` destination (Settings is reached via the Main
   dashboard tile, so auth state is shared app-wide).
4. **Strings**: add `settings_*` strings (en + ru) for the section title, Sign In/Out labels;
   reuse or retire `history_sign_in` accordingly (no hardcoded literals).
5. **Tests**: migrate `CloudSyncTest.testSignInTriggered` from History to Settings; extend
   `SettingsScreenTest` (signed-out shows Sign In, signed-in shows email + Sign Out, taps call
   through); update any History tests asserting the old button. Follow TDD
   (red → green → refactor → UI test) per `workflow.md`.

## Non-Functional Requirements

- No auth-logic changes: `GoogleAuthManager` (CredentialManager) and `SessionManager` untouched
  unless exposing the email reactively requires a minimal accessor.
- Material 3 + existing Settings patterns (section title style, testTags); light/dark themed via
  `HealthJournalTheme`.
- Offline-first unaffected: pull-to-refresh `syncNow()` still safely no-ops when signed out.

## Acceptance Criteria

- [ ] Signed out: History shows NO Sign In button; Settings shows Sign In; tapping it triggers
  `viewModel.signIn` and Drive sync works end-to-end.
- [ ] Signed in: Settings shows account email + Sign Out; Sign Out clears session
  (`isUserSignedIn=false`) and returns Settings to signed-out state.
- [ ] History signed-in UI (status, Sync Now, pull-to-refresh) behaves exactly as before.
- [ ] `./gradlew :app:testDebugUnitTest` green; Compose UI tests for the new section pass; Russian
  locale renders new strings.

## Out of Scope

- Auto-redirect/nagging unsigned users toward Settings; multi-account support; Drive scope or
  token-flow changes; moving Sync Now/status off History.
