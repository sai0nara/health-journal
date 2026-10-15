# Spec: Fix Personal Card crash on open

## Overview

Tapping the Personal card tile (Main dashboard) or the person icon (History top bar) crashes
the app. Both entry points navigate to the same `personal_card` destination (`MainActivity.kt`),
so the fault lies in the shared destination: `PersonalCardScreen`, `PersonalCardViewModel`
(+ factory), `PersonalCardRepository`/DAO, or the stored personal-card row. The reporter has
existing saved card data; no stack trace is available yet.

## Reproduction

1. App with a previously saved personal card.
2. Tap Main → Personal card tile, or History → person icon.
3. App crashes (expected: card screen opens showing saved data).

## Investigation plan (hypotheses)

1. Stored-row parse failure: `demographics` / medical-profile JSON or non-null migration default
   tripping on the existing row.
2. Null/empty field dereferenced at composition (no `!!` found statically — suspect safe-call
   chains or `first()`/date parsing).
3. Factory/repository construction (DAO, `UnitSettings` persistence lambda) throwing before first
   frame.
4. First step of the fix: capture the stack trace (logcat) to localize; add a regression test
   reproducing it.

## Functional Requirements

1. Personal card opens from both entry points without crashing, showing saved data.
2. Root cause fixed (not just a try/catch around composition) — e.g. defensive parse/default for
   legacy rows.
3. Regression test: failing-first test reproducing the crash with equivalent stored state, green
   after fix (TDD per `workflow.md`).

## Non-Functional Requirements

- No schema change unless required; if a migration is needed, versioned + exported per project
  convention.
- No changes to unit-system or sync behavior beyond what the crash demands.

## Acceptance Criteria

- [ ] Both entry points open the card on a device with existing card data, no crash.
- [ ] Fresh-install (empty card) path still works.
- [ ] `:app:testDebugUnitTest` green; new regression test included; UI test on emulator where
  possible.
- [ ] Root cause + stack trace recorded in plan.md implementation notes.

## Out of Scope

- Personal card feature changes; the Settings sign-in track; multi-device sync issues.
