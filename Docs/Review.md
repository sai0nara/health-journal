# Code Review Register

## Review: Workout Presets & Progressive Overload (Phases 1–4)
**Date:** 2026-09-18  
**Scope:** `feat/workout-tracking` (`5c1891c..HEAD`)  
**Status:** Findings Resolved

### Summary
The implementation for Phases 1–4 is high quality, well-tested across unit and Compose UI suites, and addresses 22 manual verification findings; one high-severity schema version constant discrepancy and minor doc updates were identified and fixed.

### Verification Checks
- [x] **Plan Compliance**: Partial - Phases 1–4 are fully implemented with defect fixes; Phases 5–7 remain as planned.
- [x] **Style Compliance**: Pass - Follows Material 3 semantic tokens, manual ViewModel factory patterns without DI, and repository encapsulation.
- [x] **New Tests**: Yes - Comprehensive JVM unit tests, Room migration tests, and Jetpack Compose UI tests added.
- [x] **Test Coverage**: Yes - Extensive coverage across domain use cases, DAOs, repositories, ViewModels, and UI interactions.
- [x] **Test Results**: Passed - All JVM unit tests passed (`:app:testDebugUnitTest`).

### Findings

#### 1. [High] `JournalDatabase.CURRENT_SCHEMA_VERSION` is out of sync with `@Database(version = 18)`
- **File**: `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` (Lines L31-L33)
- **Context**: The database version was bumped to `18` via `MIGRATION_17_18` (adding `routineName` to `workout_sessions`), but `CURRENT_SCHEMA_VERSION` was left at `17`. Since `RestoreWorker` and `RestoreIntegrationTest` rely on `CURRENT_SCHEMA_VERSION` for backup/restore schema compatibility validation, this mismatch will cause restore operations to reject valid backups or misidentify schema versions.
- **Suggestion**:
```diff
-        /** Current Room database schema version; single-sourced for restore validation. */
-        const val CURRENT_SCHEMA_VERSION: Int = 17
+        /** Current Room database schema version; single-sourced for restore validation. */
+        const val CURRENT_SCHEMA_VERSION: Int = 18
```
- **Resolution**: `FIXED` — constant bumped to `18`; `RestoreIntegrationTest` (4 tests) green on device.

#### 2. [Low] `wiki/modules/data-layer.md` out of date regarding database version
- **File**: `wiki/modules/data-layer.md` (Lines L22-L26)
- **Context**: The documentation states the schema is at version 16, omitting migrations 16→17 (presets and exercise catalog) and 17→18 (`routineName`).
- **Suggestion**:
```diff
-The schema is at version 16, added incrementally via the
-versioned migrations in `JournalDatabase.kt` (13→14 drops orphaned sync columns,
-14→15 adds the set matrix, 15→16 adds the interval state); the exported JSON schema
-lives under `app/schemas/`.
+The schema is at version 18, added incrementally via the
+versioned migrations in `JournalDatabase.kt` (13→14 drops orphaned sync columns,
+14→15 adds the set matrix, 15→16 adds the interval state, 16→17 adds workout presets
+and exercise catalog, 17→18 adds routineName to workout sessions); the exported JSON schema
+lives under `app/schemas/`.
```
- **Resolution**: `FIXED` — wiki page updated; lint exit 0 (modulo expected UNTRACKED notices for the new uncommitted workout-presets docs).
