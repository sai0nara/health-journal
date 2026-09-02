# Personal Card — Product Requirements

> A single, standardized medical profile screen that consolidates demographics,
> medical profile, medical history, and emergency contacts, with metric/imperial
> unit support and save-blocking validation, synced to the cloud.

Last updated: 2026-09-02

## Overview

The Personal Card gives the user one place to record their identifying and
emergency medical details: demographics (name, date of birth, sex, height,
weight, race/ethnicity), a medical profile (blood type, allergies, medications,
adverse reactions), a medical history (hereditary diseases, chronic conditions,
surgical history), and emergency contacts. A single singleton row is stored
locally (Room table `personal_card`) and synced to Drive as its own snapshot,
so the profile is available across devices and survives backup/restore.

Height and weight support a metric/imperial unit toggle with inline validation;
the Date of Birth, height, and weight are validated together, and the Save
button is disabled while any is invalid.

## Goals / Non-goals

**Goals**

- Record a consolidated medical profile: demographics, medical profile, medical
  history, and emergency contacts.
- Support metric/imperial height and weight entry with conversion while typing.
- Validate date of birth (not future, age within range), height, and weight,
  and block save on any invalid field.
- Edit in a draft edit-mode and cancel without persisting changes.
- Sync the profile to Drive and include it in backup/restore.

**Non-goals**

- Appointment/history scheduling or clinical decision support.
- Body-measurement logging (that is the separate Body Measurements feature).
- Credentialed identity verification or sharing outside the device/Drive.

## User stories

- As a user, I want one place for my medical profile and emergency contacts so
  a caregiver can find them quickly.
- As a user, I want to enter my height and weight in my preferred units.
- As a user, I want invalid or incomplete critical fields to block saving.
- As a user, I want my profile on another device via Drive sync.

## Functional requirements

- FR-1: The card is reachable from the History screen via a profile button.
- FR-2: The screen shows four sections: Demographics, Medical Profile, Medical
  History, Emergency Contacts.
- FR-3: Each section is editable in a draft edit-mode with view-mode and
  add/remove dialogs for list fields.
- FR-4: Height and weight have a metric/imperial unit toggle that converts the
  displayed values.
- FR-5: Save is disabled while Date of Birth, height, or weight is invalid.
- FR-6: Saving persists the card locally and marks it pending-sync.
- FR-7: The card syncs to Drive as a singleton snapshot and merges last-write-wins.
- FR-8: The card is included in the full backup and restore.

## Non-functional requirements

- Offline-first persistence (local Room, then sync).
- Consistent with the medical color system and light/dark UI.
- Deterministic validation bounds shared by the ViewModel and its tests.

## Acceptance criteria

- AC-1: A fully valid card saves locally (pending-sync) and appears after reload.
- AC-2: An invalid date of birth, height, or weight disables Save with inline
  guidance.
- AC-3: Switching the unit system converts height/weight display without losing
  the entered metric value.
- AC-4: Cancelling edit mode discards draft changes.
- AC-5: The card converges across devices via Drive sync.
- AC-6: The card is present in a backup ZIP and restored on restore.

## Out of scope

- Identity verification or sharing to third parties.
- Body-measurement logging.

## Cross-references

- `Docs/psd/personal-card.md` — the specification that implements these requirements.
- `Docs/tests/personal-card.md` — the test cases that verify them.
- [[ui-layer]] — the Personal Card screen.
- [[data-layer]] — the `personal_card` entity and DAO.
- [[sync-engine]] — Drive sync of the card snapshot.
- [[export-restore]] — backup/restore inclusion.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` — the card UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` — draft/validation/save state.
- `app/src/main/java/com/example/healthjournal/data/PersonalCardRepository.kt` — persistence + sync-status.
- `app/src/main/java/com/example/healthjournal/data/local/PersonalCard.kt` — the singleton entity.
- `app/src/main/java/com/example/healthjournal/domain/validation/DemographicsValidator.kt` — field validation.
- `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` — Drive payload codec.
- `Docs/psd/personal-card.md` — specification.
- `Docs/tests/personal-card.md` — test cases.
