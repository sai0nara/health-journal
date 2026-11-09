# Track preset_editor_scroll_20261109 — Implementation Plan

## Technical Notes

- Target file: `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt`
- Test file: `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt`
- Documentation files:
  - `Docs/prd/workout-presets.md`
  - `Docs/psd/workout-presets.md`
  - `Docs/tests/workout-presets.md`
- Work strictly follows TDD: Red → Green → Refactor → Verification.

## Phase 1: Failing Test & Spec Alignment [checkpoint: f5d1328]

- [x] Task: Update PSD and Tests documentation with preset editor scrolling design (f5d1328)
    - [x] Update `Docs/psd/workout-presets.md` with unified LazyColumn architecture and edge cases
    - [x] Update `Docs/tests/workout-presets.md` with new test case for full-form scrolling
- [x] Task: Write failing Compose UI test for full-form scrolling behavior (f5d1328)
    - [x] In `PresetLibraryScreenTest.kt`, add test verifying that with multiple exercises and warm-up enabled, scrolling to a lower exercise scrolls the top header items off-screen while `save_preset_button` remains displayed
    - [x] Run test and confirm expected failure or baseline assertion

## Phase 2: Implementation & Verification [checkpoint: f5d1328]

- [x] Task: Refactor `EditingContent` in `PresetLibraryScreen.kt` to unified `LazyColumn` (f5d1328)
    - [x] Move header items (name, day dropdown, warm-up section, exercise error, exercises header + search) into keyed `item` slots of `LazyColumn`
    - [x] Ensure `preset_draft_list` encompasses all scrolling items and occupies `Modifier.weight(1f)`
    - [x] Ensure bottom CTA row with `Cancel` and `Save Preset` remains pinned
    - [x] Run `PresetLibraryScreenTest` and verify all tests pass
- [x] Task: Run JVM unit tests and wiki lint (f5d1328)
    - [x] Run `./gradlew :app:testDebugUnitTest`
    - [x] Run `python3 wiki/lint.py` and confirm 0 errors
