# Implementation Plan: Workout Tracking v2

**Prerequisite:** v1 workspace (this branch), schema 12–13 already present; v2
adds migration 13→14. Work happens on this branch on top of v1, then merges to
main as one PR or a stacked PR after v1 merges.

> **Deviation (Phase 1 verification):** the 13→14 slot was consumed by the
> checkout fix for the orphaned `isSynced`/`syncStatus` columns (early v13 build
> shipped them; the v1 review-fix removed them from the entity without a version
> bump). Phase 2 set/rep persistence therefore uses **MIGRATION_14_15**, and
> Phase 3 interval-state persistence (cheap parallel-history column, no row
> rewriting) uses **MIGRATION_15_16**.

## Phase 1: Domain + Activity Catalog [checkpoint: 833c818]
- [x] Task: Write failing unit tests for the expanded WorkoutType catalog (10 types, labels, per-type target kind, Health Connect mapping, MET calorie values) (37ad11e)
- [x] Task: Implement the 10-type catalog with per-type target semantics and MET calorie estimation; green tests (37ad11e)
- [x] Task: Write failing unit tests for HIIT interval-phase domain logic (manual advance, round counting, haptic cue trigger) (87afe8a)
- [x] Task: Implement HIIT interval-phase domain logic; green tests (87afe8a)
- [x] Task: Conductor - User Manual Verification 'Domain + Activity Catalog' (Protocol in workflow.md) [833c818: report in git note, user confirmed YES after 2 fix rounds]

## Phase 2: Set/Rep Matrix (Data + Domain) [checkpoint: fb69b5b]
- [x] Task: Write failing unit tests for the set/reps model (exercises, sets validated, tonnage calculation) (48cc0a9)
- [x] Task: Implement exercises/sets model + tonnage use-case; green tests (48cc0a9)
- [x] Task: Write failing unit tests for Room persistence of sets (schema 14→15 migration, TypeConverter or table) (fd158cc)
- [x] Task: Implement schema 14→15 migration + persistence; update exported schema JSON; green tests (fd158cc)
- [x] Task: Conductor - User Manual Verification 'Set/Rep Matrix' (Protocol in workflow.md) [fb69b5b: report in git note, user confirmed user_version=15 + setMatrix column]

## Phase 3: Session Engine (MVI) + ViewModel [checkpoint: d952499]
- [x] Task: Write failing ViewModel tests for Active-state extensions: countdown, HIIT phase/round advance, set-matrix adds/rest timer, summary composition, crash-recovery of sets + intervals (e661529)
- [x] Task: Implement ViewModel countdown + HIIT interval control + set-matrix operations + recovery restore; green tests (e661529)
- [x] Task: Rerun unit suite to green; verify >80% coverage on new code (e661529: 409 JVM 0 failures; 22/22 on-device; every new engine behavior has dedicated unit + UI tests — repo has no JaCoCo, evidence is test enumeration as in Phases 1-2)
- [x] Task: Conductor - User Manual Verification 'Session Engine (MVI) + ViewModel' (Protocol in workflow.md) [d952499: report in git note, user confirmed countdown 3-2-1 works on device; set-matrix/HIIT UI deferred to Phase 4]

## Phase 4: UI + Navigation + Polish [checkpoint: de617cf]
- [x] Task: Write failing Compose UI tests for: catalog discovery (10 types), configuration per type, countdown, HIIT phase/round UI, set-matrix editing, summary (tonnage, rounds, laps), manual-log dialog extras (9 new tests; red on-device before implementation)
- [x] Task: Implement Workout UI updates (M3, semantic tokens) + History entry polish; wire per-type config/manual-log (HIIT phase/round UI + Next interval; Fitness set-matrix editor with rest timer + inline errors; summary tonnage + rounds/intervals; manual-log laps/movements extras)
- [x] Task: Run instrumented UI tests until green on device (47256c6: WorkoutScreenTest 27/27 on SM-F936B)
- [x] Task: Conductor - User Manual Verification 'UI + Navigation + Polish' (Protocol in workflow.md) [de617cf: report in git note, user confirmed YES — Fitness sets/tonnage, HIIT phases/rounds, Swimming laps + Calisthenics movements extras all behaved as described]

## Phase 5: Integration & Health Connect
- [x] Task: End-to-end tests: 10-type session → summary → journal entry → Health Connect EXERCISE write (all 10 mappings); verify crash-recovery mid-set and mid-interval (2b49569: allTenTypes + crashRecovery_midSet + crashRecovery_midInterval; green-over-verified wiring, see task git note)
- [x] Task: Verify Health Connect write for new types with granted/denied permission paths; airplane-mode logging pass (2b49569: denied-path for 5 new types unsynced-but-complete; writeFailure fake proves airplane-mode write never breaks finish/manual-log)
- [x] Task: Update affected wiki pages (agent owns the vault) and run wiki lint (exit 0); commit wiki + code together (wiki: ui-layer, data-layer, health-connect integration, unit-tests, instrumented refreshed for WorkoutScreen/JournalDatabase v16/converters/health-write; Docs: health-connect prd/psd/tests + photos-media + entry-logging + body-measurements dates/content; lint exit 0)
- [ ] Task: Conductor - User Manual Verification 'Integration & Health Connect' (Protocol in workflow.md)

## Phase 6: Review Fixes
- [ ] Task: Code review of v2 changes; apply suggestions; record SHA (protocol in workflow.md)