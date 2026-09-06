# Medical Color System (Light & Dark) — Product Specification

> A fixed brand palette is mapped to two Material 3 schemes (`lightMedicalColorScheme`
> and `darkMedicalColorScheme`), selected by the OS dark-mode setting, with a
> Compose `SideEffect` driving status-bar icon appearance over an edge-to-edge
> transparent window; a source audit forbids hardcoded colors outside the theme
> package.

Last updated: 2026-09-02

## Overview

`Color.kt` defines the brand palette and two `ColorScheme` builders — light
("Medical Standard") and dark ("Eye-strain Reduction"). `Theme.kt`'s
`HealthJournalTheme` selects one via the OS setting (`isSystemInDarkTheme`) and
sets the status-bar icon appearance in a `SideEffect`, so both the palette and
the system bars react to a dark-mode toggle without an in-app override. Dynamic
color (Material You) is intentionally not used: a fixed medical palette applies
on all devices and Android versions.

## Architecture

- Single source of truth for colors is `ui/theme/Color.kt`; the scheme is
  injected through `MaterialTheme.colorScheme`, never read from a global.
- `HealthJournalTheme(darkTheme = isSystemInDarkTheme())` wraps the app; there is
  no custom `Typography` (Material 3 defaults are used).
- Status bar appearance is applied in a `SideEffect` via
  `WindowCompat.getInsetsController(...)` after `enableEdgeToEdge()`.
- No in-app theme state is persisted; the theme is derived from the OS at all
  times.

## Color mapping

`Color.kt` maps each semantic role to a light and a dark value (primary,
onPrimary, secondary, onSecondary, background, onBackground, surface,
onSurface, surfaceVariant, onSurfaceVariant, error, onError, plus scrim-like
roles). The exact hex values live in `Color.kt`; screens consume only the roles.

## Status bar & edge-to-edge

- `MainActivity` calls `enableEdgeToEdge()`, giving a transparent status bar.
- `StatusBarAppearanceEffect` sets `isAppearanceLightStatusBars` to true in light
  mode (dark icons) and false in dark mode (light icons), matching `Theme.kt`'s
  `statusBarIconsDark` helper.
- The same effect drives the transition instantly on theme toggle because it runs
  in a `SideEffect` keyed to the theme.

## Consumption

Screens and components read colors exclusively from `MaterialTheme.colorScheme`
roles (e.g. `primary` for titles and accents, `scrim` for image-dialog
backdrops, `onSurface` for text, `secondaryContainer` for card tints). A source
audit is enforced by a test that walks `app/src/main/java` and rejects absolute
`Color(0x...)`/`Color.*` usages outside the theme package.

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| OS dark-mode toggle while running | scheme and status-bar icons update in the same frame |
| Preview/edit-mode context | status-bar effect is skipped (no window) |
| Hardcoded color introduced in UI | audit test fails the build |
| Non-legible status bar | icon appearance tracks the scheme (dark icons on light, vice-versa) |

## Dependencies

- Compose Material 3 `ColorScheme`; `androidx.core` `WindowCompat`; the system UI
  edge-to-edge API.
- No color library; no dynamic-color dependency.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/theme/Theme.kt` — theme + status bar.
- `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` — palettes.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — `enableEdgeToEdge`.
- `Docs/prd/medical-color-system.md` — requirements.
- `Docs/tests/medical-color-system.md` — test cases.
