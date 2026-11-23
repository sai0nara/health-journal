# Implementation Plan: Fix Defects (Docs/Defects.md)

## Phase 1: Vitals Entry Text ("Vitals") & `#Health` Auto-Tag (Defect 1)

- [x] Task: Add `"Vitals"` description and `#Health` (`EntryKindTag.HEALTH`) tag when logging vitals in `VitalsViewModel`
    - [x] Write failing unit tests in `VitalsViewModelTest` verifying `logVital` creates a `JournalEntry` with `description = "Vitals"` and attaches `EntryKindTag.HEALTH` (`"health"`) via `JournalRepository.addTag` (Red)
    - [x] Update `EntryKindTag` (add `forVitals(): String = HEALTH`) and `VitalsViewModel.logVital` to set `description = "Vitals"` and call `journalRepository.addTag(profileId, entry.entry_id, EntryKindTag.forVitals())` (Green)
    - [x] Refactor and verify unit tests pass
- [x] Task: Apply `#Health` tag and fallback `"Vitals"` description when updating vitals on an existing entry in `JournalViewModel`
    - [x] Write failing unit tests in `JournalViewModelTest` verifying `updateVitals` adds `EntryKindTag.HEALTH` when the updated entry has vitals, sets `description = "Vitals"` if the existing description is blank, and preserves non-blank descriptions (Red)
    - [x] Implement description fallback and `repository.addTag(entry.profileId, entry.entry_id, EntryKindTag.forVitals())` in `JournalViewModel.updateVitals` (Green)
    - [x] Refactor and verify unit tests pass
- [x] Task: Conductor - User Manual Verification 'Phase 1: Vitals Entry Text ("Vitals") & `#Health` Auto-Tag (Defect 1)' (Protocol in workflow.md)

## Phase 2: Plain-Text HTML Stripping in Today Dashboard Recent Activity (Defect 2)

- [x] Task: Create pure HTML-to-plain-text summary sanitizer and apply it in `TodayDashboardViewModel`
    - [x] Write failing unit tests for stripping HTML tags (including `<p>Text</p><br>`, malformed `<p>Text</><br>`, nested formatting tags, HTML entities, and multi-line whitespace) and in `TodayDashboardViewModelTest` for `TodayActivityCategory.JOURNAL` summary formatting and `"Journal"` fallback when stripped text is empty (Red)
    - [x] Implement pure Kotlin plain-text extraction helper and wire it into `TodayDashboardViewModel.buildRecentActivity` (Green)
    - [x] Refactor and verify unit tests pass (>80% coverage)
- [x] Task: Conductor - User Manual Verification 'Phase 2: Plain-Text HTML Stripping in Today Dashboard Recent Activity (Defect 2)' (Protocol in workflow.md)

## Phase 3: History Calendar Panel & 'Apply' Button Visibility (Defect 3) & Verification

- [x] Task: Hide History `+` FAB while calendar panel is expanded and make `HistoryCalendarPanel` compact and scrollable
    - [x] Write failing Compose UI tests verifying that expanding the calendar panel on `HistoryScreen` hides the `+` FAB and keeps `calendar_apply_button` completely visible and clickable (Red)
    - [x] Update `HistoryScreen` to hide the `+` `FloatingActionButton` when `isCalendarExpanded` is `true`, and update `HistoryCalendarPanel` to use compact vertical spacing and vertical scrolling within bounded height so `'Apply'` is never obscured by the bottom bar (Green)
    - [x] Run instrumented UI tests on emulator and verify green, then shut down emulator with `adb emu kill` (Blue) — verified on connected device `SM-F936B`
- [x] Task: Update documentation (`Docs/Defects.md` / wiki pages if cited) and run full verification suite
    - [x] Verify if any `wiki/` or `Docs/` pages cite the modified files and update if needed
    - [x] Run `python3 wiki/lint.py` and `./gradlew :app:testDebugUnitTest` to confirm zero warnings or failures
- [x] Task: Conductor - User Manual Verification 'Phase 3: History Calendar Panel & 'Apply' Button Visibility (Defect 3) & Verification' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review findings R-1 to R-5 from Docs/Review.md
