# Main Page with Tiles — Product Specification

> A stateless dashboard destination becomes the launch screen: a 2-column
> grid of eight static tiles wired to the existing nav graph, with the
> History menu trimmed to actions only.

Last updated: 2026-09-29

## Overview

The design adds one destination and changes one line of nav config. No
ViewModel, no repository, no I/O on the dashboard: `MainScreen` is a pure
function of `(onTileClick) -> Unit`, so cold launch stays instant and
offline. All navigation targets are the existing routes, untouched —
sections already return via `popBackStack()`, which now lands on Main
because Main is the only entry. The History overflow menu keeps its
action entries (Sync, Sort, About) and drops the three entries whose
destinations have tiles. See `Docs/prd/main-page-tiles.md` for the
requirements.

## Architecture

- MVVM without DI framework: unchanged — Main adds no ViewModel, so no
  factory or repository wiring.
- Navigation Compose stays the single mechanism: one new `composable("main")`,
  `startDestination` flipped `"history"` → `"main"`, tile tap calls
  `navController.navigate(route)`.
- Tile labels resolve through `stringResource` with `values-ru` parity,
  reusing the localization contract (`LocalizationParityTest` covers all
  keys, including the new `main_tile_*` set).
- Seams for testing: JVM key-presence test (catalogs), Compose UI tests
  (`MainScreen` in isolation via `onTileClick` recording; History menu
  assertions), manual device passes for locale/theme/layout.

## Data flow

1. Cold launch lands on `"main"`; `MainScreen` renders eight tiles from a
   compile-time list (route + labelRes + icon), no data loading.
2. Tile tap invokes `onTileClick(route)` → `navController.navigate(route)`
   to the existing destination.
3. System Back inside any section calls its existing `onBack`
   (`popBackStack()`), returning to Main.
4. History overflow menu offers Sync/Sort/About only; Archive, Workouts,
   Settings are reachable exclusively via their tiles.

## Components

| Component | File | Responsibility |
|---|---|---|
| Dashboard UI | `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` | static 2-column tile grid, testTags, content descriptions |
| Nav graph | `app/src/main/java/com/example/healthjournal/MainActivity.kt` | `"main"` destination, `startDestination` |
| Trimmed menu | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | actions-only overflow (Sync/Sort/About) |
| Tile labels | `app/src/main/res/values/strings.xml` | `main_tile_*` English labels |
| Tile labels RU | `app/src/main/res/values-ru/strings.xml` | `main_tile_*` Russian labels |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Device locale ≠ `ru` | English labels, unchanged from pre-feature strings |
| New section added later | add route + tile entry + two labels; parity test forces both catalogs |
| Small screen / large font | labels wrap inside cards; grid scrolls; nothing clips |
| Deep link / notification into a section | Back still lands on Main (sole back-stack root) |
| TalkBack | tile announced by label; decorative icons skipped; order matches grid |

## Dependencies

- No new libraries or platform services; Navigation Compose and the
  existing string-catalog setup only.
- No schema migration; no sync/backup payload change.

## Cross-references

- `Docs/prd/main-page-tiles.md` — the requirements this specification implements.
- `Docs/tests/main-page-tiles.md` — the test cases that verify this design.
- [[ui-layer]] — the Compose navigation whose entry point this changes.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` — dashboard UI.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — nav graph and start destination.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — trimmed overflow menu.
- `app/src/main/res/values/strings.xml` — English tile labels.
- `app/src/main/res/values-ru/strings.xml` — Russian tile labels.
- `app/src/test/java/com/example/healthjournal/localization/MainTilesKeysTest.kt` — key-presence contract.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` — tile UI coverage.
- `Docs/prd/main-page-tiles.md` — requirements this specification implements.
- `Docs/tests/main-page-tiles.md` — test cases.
