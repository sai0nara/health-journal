# Spec: Refactor Body Measurements Card View

## Overview

Refactor `MeasurementCard` in `MeasurementsScreen.kt` to display all recorded parameters in a structured grid layout, replacing the current single-line summary that truncates at 2 lines.

## Problem

`MeasurementCard` uses `toSummary()` which concatenates all non-null fields into a single string (e.g. "78.5 kg · Waist 85 cm · Chest 100 cm · Glute 102 cm · Thighs 55 cm · Calves 37 cm · Biceps 33 cm"). With `maxLines = 2` and ellipsis, entries with many fields are truncated — the user cannot see all their data.

## Proposed Layout

```
┌──────────────────────────────────┐
│  78.5 kg              ☁️  🗑️   │  ← weight (primary, bold)
│  Chest 98 cm · Waist 85 cm      │  ← 2-up grid row
│  Glute 102 cm · Thigh 55 cm     │
│  Calves 37 cm · Biceps 33 cm    │
│  3 Sep 2026                      │  ← date (bottom)
└──────────────────────────────────┘
```

- **Weight** on its own line (titleMedium, SemiBold) as the primary metric
- **Body circumferences** in a 2-column `FlowRow` grid, each as `label value unit`
- **Date** moved to bottom-left
- **Sync icon + delete** stay right-aligned (vertically centered)
- Empty fields (`null`) are omitted from the grid

## Acceptance Criteria

- [ ] All non-null measurement parameters are visible without truncation
- [ ] Weight is visually prominent as the primary metric
- [ ] Body circumferences are displayed in a compact 2-column grid
- [ ] Date is shown at the bottom of the card
- [ ] Sync status icon and delete button remain accessible
- [ ] Cards with only weight look clean (no empty grid rows)
- [ ] Cards with only circumferences (no weight) look clean
- [ ] Existing UI tests pass

## Out of Scope

- No changes to data layer, entity, or `toSummary()`
- No changes to the chart or tab pager
- No new components or screens
