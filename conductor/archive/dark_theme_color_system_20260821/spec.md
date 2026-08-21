# Track Specification: Medical App Color System (Light & Dark)

## Overview
Replace the app's default light-only theme with a semantic color token system supporting Light ("Medical Standard") and Dark ("Eye-strain Reduction") themes. The app will automatically follow the OS theme preference, and the "invisible status bar" defect will be fixed as part of the same theming work.

## Functional Requirements

### FR1: Semantic Color Token Palettes
Define two complete color palettes mapped to Material 3 `ColorScheme` roles:

| Semantic Token | Light Theme | Dark Theme | Usage |
|---|---|---|---|
| Background | `#F8F9FA` | `#121212` | Main app background, behind cards |
| Surface | `#FFFFFF` | `#1E1E1E` | Cards, modals, archive list items |
| Primary | `#0A66C2` | `#4A90E2` | Key actions, active states, checkboxes |
| Secondary | `#20C997` | `#48D8A4` | Favorable actions, "Undo" toasts |
| Text Primary | `#212529` | `#E9ECEF` | Titles, reading text (`onBackground`/`onSurface`) |
| Text Secondary | `#6C757D` | `#A0AAB2` | Timestamps, archived dates (`onSurfaceVariant`) |
| Error/Destructive | `#DC3545` | `#EF5350` | "Delete All" buttons, hard warnings |

### FR2: System Theme Integration
- `HealthJournalTheme` selects the palette via `isSystemInDarkTheme()`.
- When the user toggles Dark Mode in OS settings, the app UI re-renders instantly without restart.
- No local persistence or in-app override is required (system-follow only).

### FR3: Fixed Brand Palette
- Dynamic color (Material You) is NOT used; the medical brand palette applies on all devices and Android versions.

### FR4: Status Bar Visibility Fix (Defect)
- Light mode: status bar icons/content rendered dark (visible against the off-white background).
- Dark mode: status bar icons/content rendered light.
- Status bar background matches the app background (transparent/edge-to-edge alignment), eliminating the invisible-status-bar defect.

### FR5: Full Semantic Token Migration
- Remove ALL hardcoded absolute colors (e.g., `Color(0xFF...)`, `Color.Blue`, `Color.White`) from screens and composables.
- All UI references colors exclusively via `MaterialTheme.colorScheme` roles.

## Non-Functional Requirements
- Dark theme uses elevated grays and slightly desaturated primaries to reduce eye strain and avoid visual vibration.
- Theme switch must be instantaneous (no process restart).
- Adheres to Material 3 standards and Product Guidelines (Visual Clarity, Clinical & Precise tone).

## Acceptance Criteria
1. OS in Light mode: off-white background, pure white cards, trust-blue primary, dark status bar icons clearly visible.
2. OS in Dark mode: deep charcoal background, elevated-gray surfaces, desaturated blue primary, light status bar icons clearly visible.
3. Toggling OS dark mode re-themes the running app instantly.
4. Grep/audit confirms zero hardcoded colors in UI code outside the theme definition files.
5. All new/existing unit tests pass (>80% coverage on new code); UI tests pass on emulator in BOTH light and dark modes.
6. Status bar is legible in both modes on a real device/emulator.

## Out of Scope
- In-app theme override setting (System/Light/Dark picker) — future track if desired.
- Material You dynamic color support.
- iOS (SwiftUI/UIKit) and Flutter implementations — this project is Android/Kotlin/Compose only.
- Any functional changes beyond theming and the status bar fix.
