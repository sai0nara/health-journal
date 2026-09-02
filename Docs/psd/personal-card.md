# Personal Card — Product Specification

> A singleton `personal_card` Room row backed by a draft-style ViewModel holds
> demographics, medical profile, medical history, and emergency contacts; a
> metric/imperial unit toggle converts height and weight while typing, a
> validator gates Save, and the card syncs to Drive as a last-write-wins
> snapshot and rides the full backup/restore flow.

Last updated: 2026-09-02

## Overview

The Personal Card is a single-entity profile. The ViewModel keeps two copies of
the model — the saved originals and editable drafts — so edit mode can be
cancelled without loss. Height and weight are entered as text with a unit-system
toggle; `UnitConverter` converts between metric and imperial for display and
parses input back to metric for storage. A `DemographicsValidator` combines
three use cases (date of birth, height, weight); Save is enabled only when all
three pass. The singleton row rides the shared Drive sync snapshot pipeline and
is included in full backup and restore.

## Architecture

- MVVM without DI framework; `PersonalCardViewModel` is built via a manual
  `PersonalCardViewModelFactory`, using an IO dispatcher for persistence.
- Singleton entity: `id` is fixed to a constant; inserts force that id, so there
  is always at most one card (`@Insert` with `REPLACE` conflict).
- Draft/edit model: `PersonalCardUiState` holds saved originals + draft copies;
  `saveChanges` copies drafts to saved and marks the row dirty.
- Drive sync uses the shared snapshot pipeline with a dedicated `personal_card.json`
  file and last-write-wins merge (local keeps only if strictly newer).

## Data model

The `PersonalCard` entity (Room table `personal_card`, schema migration added in
a later version than the measurements table) serializes four complex fields as
JSON text via `JournalTypeConverters`:

- `demographics` — fullName, dateOfBirth (`yyyy-MM-dd`), sex, heightCm, weightKg, raceEthnicity.
- `medicalProfile` — bloodType (enum, or none), allergies, medications, adverseReactions.
- `medicalHistory` — hereditaryDiseases, chronicConditions, surgicalHistory.
- `emergencyContacts` — a list of name/relationship/phoneNumber.

Storage is always metric (`heightCm`, `weightKg`); the unit system is a
display-time preference only.

## Data flow

1. User opens the card from the History screen profile button.
2. ViewModel loads the single row and populates saved + draft copies.
3. User taps Edit; the draft fields become editable with the current unit system.
4. Keystrokes update drafts; height/weight text is sanitized and converted to
   metric; the validator re-runs and drives the Save enablement.
5. Add/remove dialogs edit the list fields (allergies, medications, reactions,
   history items, emergency contacts).
6. Save persists the row (insert-or-update), marks it dirty/pending-sync, and
   exits edit mode; Cancel reverts the drafts.
7. The next sync reads/merges/uploads `personal_card.json`; full backup writes it
   into the ZIP and restore replaces the local row.

## Components

| Component | File | Responsibility |
|---|---|---|
| Card screen | `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` | four view/edit sections + dialogs |
| ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` | drafts, unit toggle, validation, save |
| Repository | `app/src/main/java/com/example/healthjournal/data/PersonalCardRepository.kt` | persistence + sync status |
| DAO | `app/src/main/java/com/example/healthjournal/data/local/PersonalCardDao.kt` | Room ops (upsert, pending-sync, dirty) |
| Entity | `app/src/main/java/com/example/healthjournal/data/local/PersonalCard.kt` | model + enums |
| Validator | `app/src/main/java/com/example/healthjournal/domain/validation/DemographicsValidator.kt` | field rule orchestration |
| Unit conversion | `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt`, `app/src/main/java/com/example/healthjournal/data/local/UnitSystem.kt` | metric/imperial + input parsing |
| Sync codec | `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` | Drive payload JSON |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Blank Date of Birth / height / weight | treated as valid (fields optional) |
| Future date of birth | invalid; blocks save |
| Age over the upper bound | invalid; blocks save |
| Height/weight out of range (metric or imperial) | invalid; blocks save |
| Imperial input | converted to metric for storage; re-formatted on toggle |
| Empty cloud list vs non-empty local | local wins (protects against stale cloud) |
| Email/rapid double-save | `isSaving` guard prevents duplicate writes |
| Cancel after edits | drafts reverted; saved originals intact |

## Dependencies

- Room (`personal_card` entity + DAO); shared Drive snapshot sync pipeline;
  full backup/restore flow.
- Gson JSON serialization of the four complex fields via `JournalTypeConverters`.
- No DI framework; manual factories.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` — card UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` — state + operations.
- `app/src/main/java/com/example/healthjournal/data/PersonalCardRepository.kt` — persistence.
- `app/src/main/java/com/example/healthjournal/data/local/PersonalCardDao.kt` — DAO.
- `app/src/main/java/com/example/healthjournal/data/local/PersonalCard.kt` — entity/enums.
- `app/src/main/java/com/example/healthjournal/domain/validation/DemographicsValidator.kt` — validation.
- `app/src/main/java/com/example/healthjournal/domain/validation/ValidateDateOfBirthUseCase.kt` — DOB rules.
- `app/src/main/java/com/example/healthjournal/domain/validation/ValidateHeightUseCase.kt` — height rules.
- `app/src/main/java/com/example/healthjournal/domain/validation/ValidateWeightUseCase.kt` — weight rules.
- `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` — unit conversion.
- `app/src/main/java/com/example/healthjournal/data/local/UnitSystem.kt` — unit preference.
- `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` — sync codec.
- `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` — last-write-wins merge.
- `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` — Drive file constant.
- `Docs/prd/personal-card.md` — requirements.
- `Docs/tests/personal-card.md` — test cases.
