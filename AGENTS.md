# Health Journal

Android health journal app for personal event logging and health metric
tracking. Kotlin + Jetpack Compose (Material 3), Room persistence, MVVM with a
repository pattern, Kotlin Coroutines/Flow, Jetpack Navigation. No DI framework —
ViewModels and their dependencies are constructed manually (with per-ViewModel
`Factory` classes reading DAOs off the singleton `JournalDatabase`).

## Branches & PRs

- Development happens on feature branches named `feat/<topic>`; finished work is
  merged via a GitHub PR (use `gh`), not by committing to `main`.
- Do not commit unless the user asks. Never stage unrelated working-tree changes
  (e.g. drafts, `AGENTS.md` edits, deletions of legacy docs) — stage only the
  files your change touches.

## Driving the project: `conductor/`

`conductor/` is the project-management layer. Before starting work, read the
track workflow (`conductor/workflow.md`) and current track status
(`conductor/tracks.md` — one `[x]`/`[~]`/`[ ]` line per track). Each track has its
own plan under `conductor/tracks/<track_id>/plan.md` (or `conductor/archive/` for
finished ones). Work follows TDD: red (failing test) → green → refactor → UI test.
Aim for >80% coverage and UI tests on every user-facing feature.

## Building & testing

`JAVA_HOME` must point at a JDK 21; every Gradle command needs it:

```sh
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home
```

- Build: `./gradlew build`
- Install debug build on a device/emulator: `./gradlew installDebug`
- JVM unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests (require a connected device/emulator): `./gradlew connectedAndroidTest`
- Allure report: `./gradlew :app:allureServe`
- `wiki/lint.py` and related scripts use stock `python3` + `git`; no test framework.

## Documentation

- `Docs/<type>/` is the documented-feature set, one feature = a lower-kebab-case
  slug in three files: `Docs/prd/<slug>.md` (requirements), `Docs/psd/<slug>.md`
  (design/spec), `Docs/tests/<slug>.md` (test cases). The catalog is
  `Docs/index.md`; a feature must list all three paths there or the wiki lint's
  `MISSING` check fails. A not-yet-built feature is PRD-only and marked `— planned`
  in the index. Templates live in `Docs/_templates/`.
- Legacy root docs (`Docs/PRD.md`, `Docs/Plan.md`, `Docs/Stories.md`) have been
  retired — the per-feature PRD/PSD docs replaced them. `Docs/Review.md` and
  `Docs/Defects.md` are the review/defect registers.

## Code layout

Under `app/src/main/java/com/example/healthjournal/`:

- `ui/` — Compose screens, components, theme. `theme/` has the light/dark
  semantic color system (`Theme.kt`, `Color.kt`).
- `viewmodel/` — UI state (StateFlow) and business orchestration.
- `data/` — repositories (`JournalRepository`, `BodyMeasurementRepository`,
  `GoalsRepository`, `PersonalCardRepository`) over Room `local/` DAOs/entities.
- `domain/` — pure validation/formatting (e.g. `ValidateMeasurements`,
  `validation/` for demographics, `UtcToLocalDate`).
- `sync/` — Google Drive sync engine (`SyncWorker`, `SyncMerge`, payload codecs,
  tombstones) plus the restore worker.
- `export/` — PDF/ZIP export, full backup, and restore (`PdfExportUseCase`,
  `ZipExportUseCase`, `FullBackupUseCase`, `RestoreCoordinator`, `RestoreViewModel`).
- `health/` — Health Connect integration (`HealthConnectManager`).
- `media/` — attachment compression (`MediaCompressionService`).
- `auth/` — Drive auth (`GoogleAuthManager` uses CredentialManager; `SessionManager`).
- `util/` — HTML entity/parser helpers (`HtmlEntities`, `HtmlParser`).

Database migrations live in `data/local/JournalDatabase.kt`. Schema is exported;
the goals/tables, body measurements, and personal card were added via versioned
migrations with `exportSchema = true`.

## Wiki (LLMwiki)

The operational contract for the vault lives in `wiki/meta/schema.md`; the full
contract and rationale is `wiki/meta/spec.md`. Read the operational schema
before modifying code or wiki content.

Three rules govern every change:
1. **Agent owns the vault** — when a change touches source that a wiki page
   cites, update the page and commit both together.
2. **Cite, don't copy** — pages explain durable structure and cite mutable
   values to the owning file via a backticked relative path such as
   `` `app/src/main/java/.../File.kt` ``; never transcribe a value into a page.
3. **The lint exits 0 before the turn ends** — run it before finishing.

Commands:
- Lint: `python3 wiki/lint.py` (or `wiki/lint.sh` from the repo root)
- Hook self-tests: `python3 wiki/hooks/test_hooks.py`
- Lint self-tests: `python3 wiki/test_lint.py`

Content boundary: the wiki describes this repo's architecture and behavior.
No secrets, personal data, or issue-tracker references go in wiki pages. New
pages must follow the structure in `wiki/meta/schema.md`. The hooks registered
in `.opencode/plugins/wiki.js` run at the end of each turn: they name pages
affected by changed code and report vault health. The `Docs/` product docs are
covered by the same lint (freshness + path checks) but are exempt from the
wikilink/navigation checks.

## Roadmap

Described in Trello board https://trello.com/b/xlytOSCh/health-journal
