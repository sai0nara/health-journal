# Review Register

## 2026-11-23 — Track `fix_defects_20261122` (Defects.md items 1–3)

Scope: uncommitted working-tree changes for the track (no task commits recorded).
Spec/plan: `conductor/tracks/fix_defects_20261122/spec.md`, `conductor/tracks/fix_defects_20261122/plan.md`.

### Summary
All three defects are fixed and every test passes. One high-severity gap remains: the
new HTML-to-text helper does not decode the punctuation codes the rich-text editor
saves, so `#` and `!` still show up as raw code in Recent Activity.

### Verification checks
| Check | Result |
|---|---|
| Plan compliance | Partial — FR-1/2/3 implemented; the plan's docs task is marked done but no doc was updated |
| Style compliance | Pass (minor duplication, see R-4/R-5) |
| New tests | Yes — `HtmlPlainTextTest`, plus additions in `VitalsViewModelTest`, `JournalViewModelTest`, `TodayDashboardViewModelTest`, `HistoryScreenCalendarTest` |
| Test coverage | Partial — editor punctuation codes and Apply-vs-bottom-bar position not asserted |
| Test results | Passed — `./gradlew :app:testDebugUnitTest`; instrumented calendar tests on SM-F936B |

### Findings

| ID | Severity | Status | Finding |
|---|---|---|---|
| R-1 | High | Fixed | Editor punctuation codes show as raw code in Recent Activity (`HtmlPlainText` now decodes HTML5 ASCII punctuation entities including `&num;` and `&excl;`) |
| R-2 | Medium | Fixed | Docs not updated for changed behaviour (`Docs/psd/hashtags.md` and `Docs/Defects.md` updated) |
| R-3 | Medium | Fixed | Calendar UI test does not prove Apply clears the bottom bar (`HistoryScreenCalendarTest` now asserts `applyBounds.bottom <= barBounds.top`) |
| R-4 | Low | Fixed | "Vitals" description hardcoded in two places (`EntryKindTag.VITALS_DESCRIPTION` shared across `VitalsViewModel` and `JournalViewModel`) |
| R-5 | Low | Fixed | Hand-rolled "has vitals" check duplicates an existing helper (`EntryVitals.hasAny()` shared across `JournalViewModel` and `VitalsFieldsEditor`) |

#### R-1 (High) — Editor punctuation codes show as raw code in Recent Activity
- **File:** `app/src/main/java/com/example/healthjournal/util/HtmlPlainText.kt` (`decodeEntities`)
- **Context:** The rich-text editor saves `#` as `&num;` and keeps codes such as
  `&excl;` (see `Docs/psd/hashtags.md` and
  `app/src/main/java/com/example/healthjournal/util/HtmlEntities.kt`).
  `HtmlPlainText` only decodes `&nbsp; &lt; &gt; &quot; &apos; &amp;` and numeric
  codes, so "Run #Fitness!" shows as `Run &num;Fitness&excl;` — the same kind of
  defect as Defects.md #2.
- **Suggestion:** Decode HTML5 names for printable ASCII punctuation (`num`, `excl`,
  `quest`, `period`, `comma`, `colon`, `semi`, `lpar`, `rpar`, `lsqb`, `rsqb`,
  `lcub`, `rcub`, `percnt`, `dollar`, `ast`, `plus`, `equals`, `sol`, `bsol`,
  `commat`, `Hat`, `grave`, `verbar`, `lowbar`, `hyphen`) via a single named-entity
  regex plus lookup map, keeping `&amp;` last. Add a test:
  `toPlainText("<p>Run &num;Fitness&excl;</p>") == "Run #Fitness!"`.

#### R-2 (Medium) — Docs not updated for changed behaviour
- **Files:** `Docs/psd/hashtags.md` (Architecture, Data flow step 3, Components table), `Docs/Defects.md`
- **Context:** The PSD lists only workout and measurement as auto-tag sources;
  vitals now also receive the Health tag. `Docs/Defects.md` still lists all three
  defects as open. AGENTS.md's "agent owns the vault" rule requires updating pages
  that cite changed source.
- **Suggestion:** Add vitals logging (Vitals screen, or adding vitals to an existing
  entry) as a Health auto-tag source, add a Components row for
  `app/src/main/java/com/example/healthjournal/viewmodel/VitalsViewModel.kt`, bump
  `Last updated`, and mark the three Defects.md items fixed with a reference to this
  track.

#### R-3 (Medium) — Calendar UI test does not prove Apply clears the bottom bar
- **File:** `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenCalendarTest.kt` (`calendarExpanded_hidesAddEntryFab_andKeepsApplyButtonVisibleAboveBottomBar`)
- **Context:** `assertIsDisplayed()` only checks the button is on screen; it would
  still pass if the bottom bar covered it, which is the exact defect. This is the
  weakened-assertion pattern the QA rules forbid.
- **Suggestion:** Tag the test's `NavigationBar` (e.g. `test_bottom_bar`) and assert
  `onNodeWithTag("calendar_apply_button").getBoundsInRoot().bottom <=
  onNodeWithTag("test_bottom_bar").getBoundsInRoot().top`.

#### R-4 (Low) — "Vitals" description hardcoded in two places
- **Files:** `app/src/main/java/com/example/healthjournal/viewmodel/VitalsViewModel.kt` (`logVital`), `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` (`updateVitals`)
- **Context:** The same literal appears twice and is English-only text saved into
  entries while the app has a Russian locale. Saving text matches the existing
  measurement-summary approach, but it should have a single source of truth.
- **Suggestion:** Add `const val VITALS_DESCRIPTION = "Vitals"` to `EntryKindTag`
  (`app/src/main/java/com/example/healthjournal/domain/HashtagParser.kt`) and use it
  in both places.

#### R-5 (Low) — Hand-rolled "has vitals" check duplicates an existing helper
- **File:** `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` (`updateVitals`)
- **Context:** The six-field null check repeats logic already provided by
  `JournalEntry.hasEditableVitals()`.
- **Suggestion:** Build the updated entry first and derive the flag from
  `updated.hasEditableVitals()`.

### Resolution (2026-11-23)
All five findings (R-1 to R-5) have been resolved:
- **R-1:** `HtmlPlainText.decodeEntities` now decodes HTML5 printable ASCII punctuation entities (including `&num;` and `&excl;`) and double-encoded `&amp;...;` sequences safely, verified in `HtmlPlainTextTest` and `TodayDashboardViewModelTest`.
- **R-2:** Updated `Docs/psd/hashtags.md` (with `VitalsViewModel` and `JournalViewModel.updateVitals` auto-tagging) and marked all three items in `Docs/Defects.md` fixed under `fix_defects_20261122`.
- **R-3:** `HistoryScreenCalendarTest` now tags the bottom navigation bar (`test_bottom_bar`) and asserts `applyBounds.bottom <= barBounds.top`.
- **R-4:** Extracted `EntryKindTag.VITALS_DESCRIPTION` and referenced it from `VitalsViewModel` and `JournalViewModel`.
- **R-5:** Added `EntryVitals.hasAny()` in `ValidateVitals.kt` and reused it in both `JournalViewModel.updateVitals` and `JournalEntry.hasEditableVitals()`.
