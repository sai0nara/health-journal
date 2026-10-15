# Plan: Fix Personal Card crash on open

## Phase 1: Localize the crash (TDD)

- [~] Task: Capture stack trace + write failing regression test
    - [ ] Reproduce via existing-data state; capture logcat stack trace; record root cause in plan notes
    - [x] Write failing JVM and/or Compose test seeding the same stored state and opening the card destination
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Fix + harden

- [x] Task: Implement root-cause fix (minimal code to pass)
    - [x] Fix stored-row parse/default, null handling, or construction fault — no blanket try/catch
    - [x] Rerun full :app:testDebugUnitTest green; refactor for clarity, keep green
- [x] Task: Verify fresh-install path + add UI test
    - [x] UI test: card opens with empty state and with saved state; run on emulator until green
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Implementation notes

- **Hypothesis H1 (proven at codec level, device logcat still needed):** Gson
  allocates Kotlin data classes without constructors, so explicit JSON nulls
  land in non-null fields. `PersonalCardSyncPayload.fromJson` passed cloud
  cards straight into LWW merge + `insertOrUpdate`; the next open then NPEs
  dereferencing e.g. `demographics.fullName.isEmpty()` (`PersonalCardScreen.kt`)
  or `card.demographics.heightCm` (`PersonalCardViewModel.kt` init).
- **Evidence:** `PersonalCardSyncPayloadTest.payload_nullPoisonedJson_sanitizesToDefaults`
  failed pre-fix (`AssertionError`: `fullName` null) and passes post-fix.
- **Fix:** `sanitized()` extensions in `PersonalCard.kt`, applied at both decode
  boundaries (`PersonalCardSyncPayload.fromJson`, all four `JournalTypeConverters.to*`).
  Full suite: 608 JVM tests green. No schema change.

## Phase: Review Fixes

- [~] Task: Apply review suggestions
