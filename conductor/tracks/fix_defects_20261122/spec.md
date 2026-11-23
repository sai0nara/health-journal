# Specification: Fix Defects from Docs/Defects.md

## Overview
Resolve three user-facing defects documented in `Docs/Defects.md`:
1. Vitals entries logged into the journal lack description text and the `#Health` tag.
2. Journal entries in the Today Dashboard's Recent Activity section render raw HTML tags (e.g., `<p>Text</p><br>`) instead of clean plain text.
3. On the History screen, expanding the calendar filter panel causes the `'Apply'` button to be partially or completely obscured behind the `+` Floating Action Button (FAB) and/or bottom navigation bar.

## Functional Requirements

### FR-1: Vitals Entry Text ("Vitals") and `#Health` Tag in Journal
- **New Vital Log (`VitalsViewModel.logVital`):**
  - Set `description = "Vitals"` on the created `JournalEntry` (instead of `""`).
  - Persist the canonical `#Health` tag (`EntryKindTag.forVitals()` / `EntryKindTag.HEALTH`) for the created entry via `journalRepository.addTag(profileId, entry.entry_id, EntryKindTag.HEALTH)`.
- **Existing Entry Vitals Update (`JournalViewModel.updateVitals`):**
  - When updating vitals on an existing `JournalEntry`, if the entry's `description` is blank (empty or whitespace-only) and at least one vital value is present, set `description = "Vitals"`.
  - If the updated entry has at least one vital value (`hasAnyVitals()`), ensure the `#Health` tag (`EntryKindTag.HEALTH`) is added to the entry via `repository.addTag(entry.profileId, entry.entry_id, EntryKindTag.HEALTH)`.

### FR-2: Clean Plain-Text Summary for Recent Activity on Main Screen
- **HTML Stripping & Whitespace Normalization:**
  - Introduce a pure, JVM-testable HTML-to-plain-text utility (e.g., converting block/break tags `<br>`, `</p>`, `</div>`, `</li>` to spaces, stripping remaining HTML tags, decoding common HTML entities such as `&nbsp;`, `&amp;`, `&lt;`, `&gt;`, `&quot;`, `&#39;`, and collapsing consecutive whitespace/newlines into a single space).
  - In `TodayDashboardViewModel.buildRecentActivity`, format `TodayActivityCategory.JOURNAL` summaries using this plain-text sanitizer before falling back to `"Journal"` if the stripped result is empty.
  - Ensure entries like `<p>Text</p><br>` or malformed closing tags like `<p>Text</><br>` render cleanly as `"Text"` in Recent Activity on `MainScreen`.

### FR-3: History Calendar Panel & 'Apply' Button Visibility
- **Hide FAB When Calendar Panel Is Expanded:**
  - In `HistoryScreen`, only render the `+` `FloatingActionButton` when `!isCalendarExpanded`, preventing the FAB from overlapping the bottom-right `'Apply'` button of the calendar panel.
- **Compact & Scrollable Calendar Panel:**
  - Constrain `HistoryCalendarPanel` (or its container in `HistoryScreen`) so it fits comfortably within the viewport above the bottom navigation bar, using compact vertical spacing and `verticalScroll(rememberScrollState())` (or bounded max height) so that on small screens or landscape/compact viewports the `'Apply'` button is always reachable and never clipped behind the bottom bar.

## Non-Functional Requirements
- **JVM & Instrumented Testability:** HTML-to-plain-text stripping and ViewModel logic must be pure Kotlin so they can be tested in JVM unit tests without requiring Android framework `Html.fromHtml` stubs.
- **Zero Regressions:** Existing calendar filtering, rich-text journal rendering in `HistoryScreen`, and vitals trend chart tracking must continue to pass all unit and UI tests.

## Acceptance Criteria
- [ ] Logging any vital from the Vitals screen creates a journal entry visible in History with description `"Vitals"` and tag chip `#Health`.
- [ ] Adding/editing vitals on an entry in History adds the `#Health` tag (and sets description to `"Vitals"` if the entry description was blank).
- [ ] A journal entry saved from the rich text editor (e.g., `<p>Text</p><br>` or `<p>Text</><br>`) displays as `"Text"` without HTML markup in the Recent Activity section on the Today Dashboard (`MainScreen`).
- [ ] Opening the calendar panel on `HistoryScreen` hides the `+` FAB and keeps the `'Apply'` button completely visible and clickable above the bottom navigation bar.

## Out of Scope
- Migrating or altering historical database rows via a Room schema migration.
- Changing how rich text is rendered inside full `JournalEntryItem` cards on `HistoryScreen`.
