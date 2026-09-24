# Track personal_card_verify_20260923 — Specification

## Overview

Verify and harden the personal-card demographics + units scope
(`Docs/prd/personal-card.md` FR-4, AC-3, AC-7): height/weight conversion both
ways, ft+in entry, converted read-only rows, and preference persistence. Fix
any gaps found; leave medical lists, contacts, and sync/backup untouched.

## Functional Requirements

- FR-1: Height/weight entry converts in both unit systems without value loss.
- FR-2: Imperial height splits into ft+in fields parsing back to metric cm.
- FR-3: Read-only rows render in the preferred units.
- FR-4: The unit preference persists across launches.
- FR-5: Validation blocks out-of-range height/weight in both systems with
  display-unit messages.
- FR-6: Fix defects found during verification (conversion, validation, or
  persistence).

## Non-Functional Requirements

- Reliability: no crash on partial/malformed ft/in input; round trips within
  display rounding.
- Coverage: >80% on touched code; UI tests on touched surfaces (per
  workflow).

## Acceptance Criteria

- AC-1: `./gradlew :app:testDebugUnitTest` green.
- AC-2: PersonalCardScreenTest + related UI classes green in isolation on
  device.
- AC-3: Manual device pass: ft/in entry, toggle, relaunch, validation bounds.
- AC-4: Wiki lint exits 0.
- AC-5: All defects found are fixed with regression tests.

## Out of Scope

- Medical profile/history lists, emergency contacts, sync/backup flows.
- Storage migration or new preferences.
