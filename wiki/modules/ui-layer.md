# UI Layer

> The Jetpack Compose (Material 3) screens, reusable components, and theme that make up the app's interface.

Last updated: 2026-09-01

## What lives here

The `ui` package holds the entire interface: `ui/screens` for the full screens,
`ui/components` for reusable composables, and `ui/theme` for the Material 3 theme
and color system.

Screens are driven by the [[viewmodel-layer]]: they read state from a ViewModel's
`StateFlow` and forward user intentions (clicks, submits) back to it. Screens do not
touch repositories or DAOs directly.

## Key areas

- **Screens** — `AddEntryScreen`, `HistoryScreen`, `ArchiveScreen`,
  `MeasurementsScreen`, `PersonalCardScreen`, plus the export and restore screens in
  the [[export-restore]] feature.
- **Components** — reusable pieces such as `JournalEntryItem`, `RichTextToolbar`,
  `MeasurementEntrySheet`, `ParamTrendChart`, and `SharedSearchBar`.
- **Theme** — a custom material theme and the medical color system under `ui/theme`.

Long journal entries are truncated in the list with a "Show More" interaction point,
exercised by the truncation UI test on the instrumented stack.

## Cross-references

- [[viewmodel-layer]] — the state and logic behind every screen.
- [[data-layer]] — the persistence the screens ultimately read.
- [[instrumented]] — screens are covered by Compose UI tests.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — the main history feed screen.
- `app/src/main/java/com/example/healthjournal/ui/theme` — theme and color system sources.

Back to [[overview]]
