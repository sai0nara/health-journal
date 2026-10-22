# Track tag_identity_20261020 — Implementation Plan

## Phase 1: Failing tests (Red) [checkpoint: 3c76a3a]

- [x] Task: Canonical identity helper tests (3474205)
    - [x] Add `app/src/test/java/com/example/healthjournal/domain/TagIdentityTest.kt`: Locale.ROOT lowercasing (Cyrillic, Turkish dotted İ), case-insensitive dedupe, known-tag variants (`FITNESS`/`Fitness`/`fitness` → `fitness`), blank input dropped, idempotence
    - [x] Run `./gradlew :app:testDebugUnitTest` (JAVA_HOME JDK 21); confirm Red
- [x] Task: Save-path failing tests (JVM) (e0199da)
    - [x] `JournalViewModelTest`: `addEntry` with preset `FITNESS` plus `#Fitness` in the description records exactly one tag, `fitness`
    - [x] `JournalViewModelTest`: `updateEntry` collapses `FITNESS`/`fitness`/`Fitness` to one cross-ref and keeps system tags; `toggleEntryTag("EXERCISES")` stores `exercises`
    - [x] Run and confirm Red
- [x] Task: Migration failing test (instrumented) (2095148)
    - [x] Extend `data/local/MigrationTest.kt`: v18 fixture with `FITNESS`/`fitness`/`Fitness` on one entry and an unrelated tag on a second; run to v19 via `JournalDatabase.MIGRATION_18_19`; expect one `fitness` row and untouched second entry (Red = unresolved reference until Phase 2, matching the repo's migration-test pattern)
- [x] Task: Sync/restore ingestion failing tests (JVM) (c3b95b6)
    - [x] `RestoreRepositoryTest`: backup tags `FITNESS`/`Fitness`/`fitness` → one `fitness` cross-ref
    - [x] Cover sync persistence normalization as a pure helper (new JVM test) so `SyncWorker` has no untested normalization branch
    - [x] Run and confirm Red
- [x] Task: Picker + filter UI failing tests (instrumented) (c6ca769)
    - [x] `AddEntryScreenTest`: select the Fitness chip, type `#Fitness`, save; assert `addEntryCalledWith.tags == setOf("fitness")`
    - [x] New `ui/screens/TagFilterIdentityTest` (real in-memory Room + real `JournalViewModel`): preset Fitness chip filters entries tagged through hashtag, auto-tag, and manual selection
    - [x] Run on SM-F936B, one class per invocation (`-PcomposeRunner=true`); confirm Red
- [x] Task: Conductor - User Manual Verification 'Phase 1: Failing tests (Red)' (Protocol in workflow.md) 3c76a3a

## Phase 2: Implementation (Green + Refactor) [checkpoint: 62642c0]

- [x] Task: Canonical identity helper and all write paths (4a6a1d1)
    - [x] Add `domain/TagIdentity.kt` as the single canonicalization point; `JournalTag` exposes a derived canonical value, `EntryKindTag` constants become canonical
    - [x] Route `JournalRepository.addTag`/`removeTag`, `JournalViewModel.addEntry`/`updateEntry`/`toggleTag`/`toggleEntryTag` unions through it
    - [x] `TagSelectionRow`: selected state and toggle use the canonical value; display unchanged
    - [x] Update expectations that encoded old spellings: `JournalViewModelTest`, `BodyMeasurementViewModelTest`, `WorkoutViewModelTest`, `HashtagParserTest`, `SyncMergeTest`, `TagLabelResTest` comments
    - [x] Re-run Phase 1 JVM tests green; refactor and re-run
- [x] Task: Room migration v18→19 (04c8628)
    - [x] Implement `MIGRATION_18_19` (transactional Kotlin pass: canonicalize, `INSERT OR IGNORE` dedupe, delete variants, memberships preserved); bump `CURRENT_SCHEMA_VERSION` to 19; register in the builder; export schema 19
    - [x] Re-run `MigrationTest` + `TagSchemaTest` on device; green
- [x] Task: Sync + restore normalization (818b818)
    - [x] Apply canonicalization in `SyncWorker` tag persistence (`SyncWorker.kt:159`) and `RestoreRepository` tag import (`RestoreRepository.kt:69`); re-run JVM tests green
- [x] Task: Remove tags deselected on edit (bug found in manual verification) (c978e94)
    - [x] Drop system-tag stickiness in `updateEntry`: save exactly the selected chips plus re-parsed hashtags; update PRD/PSD/spec
    - [x] Red JVM test `updateEntry_removesDeselectedSystemTag`; instrumented `TagEditRemovalTest` green on device
- [x] Task: Instrumented picker/filter green (818b818)
    - [x] Re-run `AddEntryScreenTest` and `TagFilterIdentityTest` on device; adjust tag selectors (`entry_tag_fitness`) if needed
- [x] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md) 62642c0

## Phase 3: UI tests, docs, verification (Blue + gates)

- [~] Task: Device UI verification (Blue)
    - [ ] Run one class per invocation: `AddEntryScreenTest`, `TagFilterIdentityTest`, `HistoryScreenTest`, `ArchiveScreenTest`, `MigrationTest`, `TagSchemaTest`
    - [ ] Manual check: Fitness chip + `#Fitness` yields one chip; preset filter finds hashtag and workout entries; an upgraded v18 database keeps its tags filterable
- [ ] Task: Docs + wiki
    - [ ] Update `Docs/prd/hashtags.md` (FR-2/FR-3 canonical storage), `Docs/psd/hashtags.md`, `Docs/tests/hashtags.md` (new cases)
    - [ ] Update wiki pages citing changed sources (`wiki/modules/data-layer.md`, `ui-layer.md`, `viewmodel-layer.md` as applicable) and bump dates
- [ ] Task: Full verification
    - [ ] `./gradlew :app:testDebugUnitTest` green, `./gradlew build` green, `python3 wiki/lint.py` exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI tests, docs, verification (Blue + gates)' (Protocol in workflow.md)
