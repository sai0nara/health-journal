# Specification: Workout Presets & Progressive Overload

## Overview

A structured progressive-overload tracker on top of the Workout hub (v2 shipped this branch): named day-based presets (e.g. "Leg Day") with pre-configured exercises/sets/rest, a preset-driven execution flow with completion checkboxes and RPE, historical weight analytics (Vico trend charts, Epley 1-RM estimation), and real-time weight conversion between alternative movements based on the user's logged 1RM history. Replaces the v2 manual in-session set-matrix flow; completes entries via the existing journal + Health Connect write path. Schema builds on v2 (16 → 17). GPS, heart-rate, preset HIIT guidance, and Music sync remain out of scope.

## Functional Requirements

- **Preset library (Flow A):** Workout hub → Presets section → create/edit named presets; preset holds exercises (from a searchable dropdown) each with default target sets, reps, weight, and a rest-timer length between sets.
- **Exercise catalog:** built-in curated, muscle-category-grouped list with pre-mapped alternative movements + user-added exercises; searchable dropdown for preset building and in-session exercise changes.
- **Execution (Flow B):** pick a preset → "Start Routine" → interactive exercise cards; per-set fields Weight, Reps, RPE (1–10, optional) and a completion checkbox; auto-start rest timer after a set is completed; tap the exercise dropdown to swap exercises mid-routine.
- **Weight analytics (Flow C):** per-exercise trend chart (weight over time, estimated 1RM via Epley, max volume) with a bottom-sheet overlay; "Alternative" action converts to a target exercise and shows an equivalent starting weight.
- **Alternative-weight conversion:** 1RM-based equivalence between mapped movement pairs, personalized from the user's historical 1RMs with a default coefficient fallback when history is insufficient.
- **Mobile UX:** heavy haptic on set completion + success-pattern haptic when an exercise's sets are all done; inline numeric quick-increment controls (+2.5 kg / +5 lb); screen-awake flag across the active routine; offline-first set persistence.

## Non-Functional Requirements

- Tech-stack change (Charting → Vico) documented in tech-stack.md BEFORE implementation.
- New Room entities (preset, preset-exercise cross-ref, exercise catalog, alternative mappings, logged-set extension for RPE) with indexed queries; extend backups to serialize/restore these so the Drive full-backup round-trip still holds.
- MVI/UiState machine extended (replacing the v2 "Active"-state set-matrix editing with a routine-execution state); immutable state + keyed lazy lists; timer/heavy math off the main thread.
- >80% coverage on new code; UI test per new user-facing flow.
- Wiki lint exits 0; affected vault pages updated with the code.

## Acceptance Criteria

- Create/edit/delete a preset with exercises, sets, reps, rest; pick it and start.
- Execute sets with weight/reps/RPE/completion; rest auto-starts; haptics on completion; swappable exercise via dropdown.
- Trend chart + Epley 1RM + volume show per selected exercise; alternative conversion shows a sensible target weight from history (or default coefficient).
- Presets/catalog/mappings round-trip a Drive full backup + restore.
- Journal entry + Health Connect record still written on routine finish.
- Kill mid-set → relaunch resumes at the last completed set.

## Out of Scope

- GPS/location routes; heart-rate/sensor integration; foreground service/notification; music/air-play sync; automated/guided HIIT plans; band/relative-strength comparisons; SQLCipher encryption.