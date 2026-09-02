# Medical Color System (Light & Dark) — Test Cases

> Verifies the light/dark palette values and role mapping at the JVM unit level,
> forbids hardcoded colors via a source audit, and confirms status-bar appearance
> and themed rendering at the instrumented level.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/ui/theme/MedicalColorSystemTest.kt` | light/dark palette values, role mapping, scheme selection, status-bar icon decision |
| JVM unit | `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` | walk `app/src/main/java`; reject absolute colors outside theme package |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/theme/StatusBarAppearanceTest.kt` | dark icons/transparent bar in light; light icons in dark; instant update on toggle |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/theme/ThemedRenderingTest.kt` | HistoryScreen renders under light and dark palettes |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenThemeTest.kt` | a representative screen renders in idle/success/error under both themes |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-1 | Light palette values | read `lightMedicalColorScheme` | roles hold the documented light hex values |
| T-2 | AC-1 | Dark palette values | read `darkMedicalColorScheme` | roles hold the documented dark hex values |
| T-3 | AC-1 | Role mapping | sample roles | each role maps to the intended semantic value in both modes |
| T-4 | AC-2 | Scheme selection | darkTheme true/false | correct scheme returned per flag |
| T-5 | AC-2 | Toggle re-renders | OS dark mode toggled | themed screen renders instantly in the new scheme |
| T-6 | AC-3 | Status bar icons (light) | light mode | dark icons; transparent bar |
| T-7 | AC-3 | Status bar icons (dark) | dark mode | light icons; transparent bar |
| T-8 | AC-4 | Hardcoded color audit | scan `app/src/main/java` | no absolute colors outside `ui/theme/` |

## Manual checks

- Toggling system dark mode updates the whole app (screens, dialogs, bottom sheets).
- Status bar and navigation bar remain legible in both modes on device.
- Charts and image-dialog scrims keep contrast in dark mode.

## Cross-references

- `Docs/prd/medical-color-system.md` — requirements under test.
- `Docs/psd/medical-color-system.md` — design the cases verify.
- [[ui-layer]] — the screens whose appearance is verified.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/ui/theme/MedicalColorSystemTest.kt` — palettes/roles/selection.
- `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` — audit.
- `app/src/androidTest/java/com/example/healthjournal/ui/theme/StatusBarAppearanceTest.kt` — status bar.
- `app/src/androidTest/java/com/example/healthjournal/ui/theme/ThemedRenderingTest.kt` — themed History.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RestoreScreenThemeTest.kt` — themed Restore.
- `Docs/prd/medical-color-system.md` — requirements.
- `Docs/psd/medical-color-system.md` — design.
