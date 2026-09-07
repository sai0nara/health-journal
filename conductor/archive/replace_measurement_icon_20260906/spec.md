# Specification: Replace Body Measurement Action Icon

## Overview
Replace the History top-bar 'View Measurements' action icon (`Icons.Default.MonitorWeight`) with the new chart artwork (`Docs/Images/461d7f12-ebd2-444c-85c2-8f1c6c929a51.jpeg`, white line-art chart on dark, 957x1024), as illustrated by the toolbar-button reference.

## Functional Requirements
- **New asset:** Add the chart artwork as a bundled drawable (`drawable-nodpi/measurements_chart.png`) with its dark background made transparent, keeping the white line-art.
- **Replacement:** The History top-bar 'View Measurements' `IconButton` renders the new drawable instead of `Icons.Default.MonitorWeight`, keeping the same content description ('View Measurements'), navigation behavior, and placement.
- **Theme-aware:** The icon is tinted with `MaterialTheme.colorScheme.onSurface` so it adapts automatically to light and dark theme.
- **Unchanged:** The 'Add body measurements' FAB keeps its current icon; no other screens or actions change.

## Non-Functional Requirements
- Single bundled local asset (offline-first; no network fetch).
- Artwork trimmed of empty margins and centered; line detail must stay legible at 24dp action-icon size.
- No new dependencies (reuse the stock-JDK image pipeline used for the app icon).

## Acceptance Criteria
- History top bar shows the new chart glyph for 'View Measurements'; tapping it still opens the Measurements screen.
- Icon is clearly visible in both light and dark theme.
- UI tests cover the replaced action (existence + navigation callback).
- Wiki lint passes (exit 0); any affected wiki page updated and committed together.

## Out of Scope
- Changing the 'Add body measurements' FAB icon.
- Launcher icon or app-logo changes.
- Modifying navigation, behavior, or other screens.
