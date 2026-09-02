# Medical Color System (Light & Dark) — Product Requirements

> A semantic light and dark color system that follows the OS theme, replaces all
> hardcoded colors with Material role tokens, and keeps the status bar legible in
> both modes.

Last updated: 2026-09-02

## Overview

The app ships with two complete Material 3 color schemes — a light "Medical
Standard" palette and a dark "Eye-strain Reduction" palette — selected by the OS
dark-mode setting (`isSystemInDarkTheme`). All screens and components read colors
exclusively from `MaterialTheme.colorScheme` roles; hardcoded absolute colors are
removed from the UI code. The status bar is transparent (edge-to-edge) with icon
appearance following the active theme so it stays legible in both modes.

## Goals / Non-goals

**Goals**

- Provide a complete light and dark semantic color system.
- Follow the OS light/dark setting and re-render on toggle.
- Eliminate hardcoded colors from screens/components in favour of semantic roles.
- Keep the status bar legible in both light and dark modes.

**Non-goals**

- An in-app theme override/picker (system-follow only).
- Dynamic color (Material You) — not used; a fixed brand palette applies everywhere.
- iOS/Flutter or any non-theming changes.

## User stories

- As a user, I want the app to follow my device's light/dark preference so it is
  comfortable in any environment.
- As a user, I want consistent, accessible colours across the app.

## Functional requirements

- FR-1: A light and a dark color scheme map a consistent brand palette to
  Material 3 roles (primary, secondary, background, surface, text, error).
- FR-2: The active scheme follows the OS dark-mode setting and re-renders on
  toggle.
- FR-3: Dynamic color (Material You) is not used.
- FR-4: The status bar is transparent/edge-to-edge with dark icons in light mode
  and light icons in dark mode.
- FR-5: UI code references `MaterialTheme.colorScheme` roles; hardcoded absolute
  colors are removed.

## Non-functional requirements

- Accessibility: sufficient contrast in both palettes for text roles.
- Consistency: a single shared token source (`ui/theme/Color.kt`).

## Acceptance criteria

- AC-1: Light and dark schemes expose the documented role values.
- AC-2: Toggling the OS dark mode re-renders the UI instantly.
- AC-3: The status bar is transparent with correct icon appearance in both modes.
- AC-4: No hardcoded absolute colors remain in screen/component code (enforced by
  an audit test).

## Out of scope

- In-app theme override and dynamic color.

## Cross-references

- `Docs/psd/medical-color-system.md` — the specification that implements these requirements.
- `Docs/tests/medical-color-system.md` — the test cases that verify them.
- [[ui-layer]] — how the theme is consumed by screens.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/theme/Theme.kt` — theme selection + status bar.
- `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` — the light/dark palettes.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — edge-to-edge setup.
- `Docs/psd/medical-color-system.md` — specification.
- `Docs/tests/medical-color-system.md` — test cases.
