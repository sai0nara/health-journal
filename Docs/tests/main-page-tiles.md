# Main Page with Tiles — Test Cases

> Maps the PRD's requirements and the PSD's contract to concrete
> verification. The "Automated coverage" table cites the real test files;
> keep it in sync when coverage moves.

Last updated: 2026-09-29

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/MainTilesKeysTest.kt` | `main_tile_*` keys exist in both catalogs |
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` | full-catalog RU parity incl. new tile keys |
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` | no literals in `MainScreen` (routes exempt as machine constants) |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` | 8 tiles in order, tile tap reports route |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | overflow shows actions only, no nav entries |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Tile keys present | — | run `MainTilesKeysTest` | 8 keys in both catalogs |
| T-2 | Eight tiles in order | — | render `MainScreen` | `main_tile_history…settings` all displayed |
| T-3 | Tile tap navigates | `onTileClick` recorder | tap Workouts tile | recorder receives `"workout"` |
| T-4 | Cold launch lands on tiles | fresh install | launch app | tile grid visible, 8 tiles |
| T-5 | Section round trip | on Main | tap each tile, press Back | correct section each time; Back returns to Main |
| T-6 | History menu trimmed | on History | open overflow | Sync/Sort/About only; no Archive/Workouts/Settings |
| T-7 | Russian tiles | `ru` device | open Main | all 8 labels Russian |
| T-8 | English unchanged | non-`ru` device | open Main + History | labels exactly as specified; menu unchanged |
| T-9 | Dark mode + small screen | dark theme, large fonts | open Main | themed, readable, no clipped tiles |
| T-10 | Full verification | — | JVM suite, androidTest compile, wiki lint | all green, lint exits 0 |

## Manual checks

- Cold launch on a fresh install lands on the tile dashboard.
- Every tile reaches its section; system Back from each returns to Main.
- `ru` device: tiles Russian; dark mode themed; largest font scale shows no clipping.
- Kill-and-relaunch stays on Main; no mixed-language strings.

## Cross-references

- `Docs/prd/main-page-tiles.md` — the requirements under test.
- `Docs/psd/main-page-tiles.md` — the design the cases verify.
- [[ui-layer]] — the Compose navigation covered by the round-trip checks.

## Sources

- `app/src/test/java/com/example/healthjournal/localization/MainTilesKeysTest.kt` — key presence.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` — tile UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — trimmed menu.
- `Docs/prd/main-page-tiles.md` — requirements under test.
- `Docs/psd/main-page-tiles.md` — design the cases verify.
