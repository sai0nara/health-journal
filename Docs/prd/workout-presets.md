# Workout Presets & Progressive Overload — Product Requirements

> Turns the Workout hub from a generic activity logger into a structured
> progressive-overload tracker: day-based presets, an in-session set/rep matrix
> with ordered completion, a searchable exercise catalog, and (planned) weight
> analytics with alternative-exercise weight conversion.

Last updated: 2026-11-09

## Overview

A user can compose named routines (e.g. "Leg Day") from a curated exercise
catalog, start any preset as an active session whose planned set rows are
prefilled with the preset's defaults, complete sets in order with inline
editing and quick-increment controls, swap a movement mid-routine, and finish
into the journal. The finished routine's history card names the routine and its
exercises instead of a bare tonnage figure.

## Goals / Non-goals

**Goals**

- Build, edit, and delete named presets with per-exercise defaults (sets, reps,
  weight, rest) and an optional scheduled day.
- Pick a preset and run it: planned set rows, ordered completion, automatic
  between-sets rest timer, inline weight/reps editing, and quick-increment
  controls that keep the screen on.
- Swap an exercise mid-routine via a searchable dropdown; the swapped-in
  movement starts from its catalog default plan.
- Finish a routine into the journal + Health Connect, with the routine name and
  per-exercise summaries on the history card, and offer a confirmation when the
  last planned set is completed.
- Recover an interrupted session to the last completed set.
- Configure one optional timed warm-up per routine and run it before the work
  sets, with a visible timer and skip control.
- (Planned, Phase 5) Per-exercise weight trend analytics (Epley 1RM, max volume)
  and alternative-movement weight conversion from logged history.

**Non-goals**

- GPS/route, heart-rate, guided HIIT, or Music sync (see out of scope).
- Replacing the manual (non-preset) v2 set-matrix or HIIT flows — presets are an
  additional entry path.
- Rack-aware weight recommendations beyond 1RM-based conversion.

## User stories

- As a lifter, I want a saved "Leg Day" routine I can start in two taps, so I
  don't rebuild the plan every session.
- As a lifter opening the Workout screen, I want saved Routines (and the Presets
  library entry button) displayed prominently at the top of the screen before the
  generic 10-activity list, so I can start my planned routine immediately
  without scrolling past unselected activities.
- As a lifter, I want my sets preloaded with planned weight/reps, so I only
  adjust when the plan changes.
- As a lifter, I want to swap a movement mid-session and get sane starting
  values, so I can adapt without mental math.
- As a lifter, I want to see which exercises and weights I did on a routine's
  history card, so past sessions read as a plan, not a tonnage total.
- As a lifter, I want a short warm-up timer attached to my routine, so I can
  start moving without tracking it separately.
- As a lifter editing a preset with warm-up and multiple exercises, I want the
  entire editor form (name, day, warm-up, exercise search, and exercise list) to
  scroll together as one full-height form with only the Cancel/Save row pinned
  at the bottom, so that warm-up and search controls do not pin the top of the
  screen and restrict scrolling to the bottom 20 %.

## Preset editor scrolling approaches

In the initial preset editor layout (`EditingContent` in
`app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt`),
the preset name field, day selector, warm-up toggle + warm-up name/duration
fields, exercises header, and catalog search input were placed as fixed children
of a non-scrollable outer `Column` above a `LazyColumn(Modifier.weight(1f))`,
with the Cancel/Save row below. With warm-up enabled (or catalog search results
expanded), the pinned upper controls consumed ~75–80 % of the viewport height,
leaving only ~20 % at the bottom for the exercise list to scroll.

| # | Approach | What it does | Viewport available for scrolling | Trade-offs | Recommendation |
|---|---|---|---|---|---|
| S1 | **Single unified `LazyColumn` + pinned bottom CTA row** | Place preset name, scheduled-day dropdown, warm-up section, exercises header + catalog search, and draft exercise rows as items inside one `LazyColumn(Modifier.weight(1f))`; keep only the `Cancel` / `Save Preset` row pinned at the bottom. | ≥ 80 % of screen height (entire area between top app bar and bottom CTA row) | Metadata and search scroll off-screen while deep in the exercise list (standard mobile form behavior) | **Adopt (Best Solution)** |
| S2 | **Bounded / overlay catalog search results inside S1** | Cap or overlay catalog search matches so a broad query does not push all draft exercises far down the list, and clear the query on selection. | ≥ 80 % of screen height | None | **Adopt alongside S1** |
| S3 | **Sticky "Exercises + Search" header inside `LazyColumn`** | Use `stickyHeader` to keep the search input pinned at the top once scrolled past warm-up. | ~65–70 % (reduced further when search results expand or IME opens) | Still pins ~100–200 dp of header over the exercise list, hurting small screens and keyboard editing | Reject (keep search as a normal scrolling item) |
| S4 | **Collapsible Warm-up accordion over split scroll** | Keep the split layout (`Column` + `LazyColumn`) but collapse warm-up into a summary chip when unfocused. | ~45–55 % when collapsed; drops back to ~20 % when warm-up is expanded or IME opens | Does not fix the root cause when editing warm-up or on compact screens | Reject |
| S5 | **Two-step wizard / tabs (Metadata tab vs Exercises tab)** | Split preset editing into two screens or tabs. | ≥ 80 % per tab | Extra navigation taps for a 3-field header; harder to review the full preset at a glance | Reject |
| S6 | **`Column` + `verticalScroll` (non-lazy)** | Replace `LazyColumn` with a regular vertically scrolling `Column`. | ≥ 80 % of screen height | Eagerly composes 4 `OutlinedTextField`s per exercise row; loses keyed lazy list semantics | Reject |

## Functional requirements

- FR-1: Preset library — list, create, edit, delete named presets; each preset
  holds exercises from a searchable catalog dropdown with target sets, default
  reps, default weight, and rest length. Editing has an explicit cancel that
  discards the draft without saving. Catalog search is a case-insensitive
  substring match on the exercise name; a blank query shows no results.
- FR-2: Preset validation — a preset requires a name and at least one exercise;
  per-exercise defaults must be usable (positive sets/reps/weight,
  non-negative rest). Validators enforce lower bounds only: there are no upper
  bounds on name length, sets, reps, weight, or rest, and no length constraint
  on the stored preset name.
- FR-3: Routine start — starting a preset builds a planned set matrix (all
  sets pending, prefilled with the preset's defaults, catalog names resolved)
  after the same 3-2-1 countdown as manual sessions; the countdown runs to zero
  with no skip/cancel affordance. It records the routine's name on the session.
  The optional scheduled day (`MONDAY`..`SUNDAY`, `ANY`) is a stored label on
  the preset; no scheduling engine consumes it. Exercises whose catalog name
  cannot be resolved (or with non-positive target sets) are skipped; if none
  resolve, an empty but ACTIVE session is still created.
- FR-4: Ordered set completion — a set can only be completed once every earlier
  set in the exercise is done; completing a set persists immediately, starts
  the rest timer with a heavy haptic, and fires a success haptic when the
  exercise's final set completes. Un-completing is allowed: unchecking a set
  persists it as pending and stops the rest period (re-checking restarts it).
  While paused, the session is frozen except Resume/Finish: the screen keeps
  the same controls, but set toggles, edits, swaps, and additions are ignored
  until resumed.
- FR-5: Set editing — each planned set's weight/reps edit inline with
  validation; the quick-increment pad adjusts the value in place, including
  while a field is focused, and updates the displayed text. Weight entry is
  free decimal text (no fixed granularity); the pad steps 1.25 kg in metric
  mode or 5 lb in imperial mode, plus a 1 kg / 2.5 lb dumbbell step.
- FR-6: Exercise swap — the dropdown replaces the exercise mid-routine and
  resets the row to the new movement's catalog default plan (sets, reps,
  weight, rest).
- FR-7: Session finish — the finished routine writes the journal entry (routine
  name + per-exercise name, set count, weight) and the Health Connect EXERCISE
  record. A Health Connect failure never blocks completion: the journal entry
  and summary are still written, with the failure recorded as not-synced.
- FR-8: Crash recovery — relaunching with an unfinished session resumes at the
  last persisted set state. Recovery offers exactly two actions: resume (back
  to active) or discard (marked discarded); there is no finish-from-recovery.
- FR-9: (Planned) Weight analytics — per-exercise trend chart of weight over
  time with Epley 1RM and max volume, in a bottom-sheet overlay. 1RM uses the
  Epley formula `weight × (1 + reps/30)` (a single rep returns the weight).
- FR-10: (Planned) Alternative conversion — convert a working weight to an
  alternative movement using the user's historical 1RM ratio, with a default
  coefficient fallback of 0.6 when history is insufficient.
- FR-11: Finish prompt — completing the final pending set of a preset routine
  shows a confirmation with Finish and Keep going. Keep going dismisses only
  the prompt (session stays active, rest timer continues, nothing is saved);
  Finish runs the normal finish flow. The prompt appears only on the transition
  to all planned sets complete, never on load/recovery, and never for manual strength or
  HIIT sessions. Manual Finish remains immediate and bypasses the prompt.
- FR-12: Warm-up — a preset may define one optional warm-up name and a
  duration in whole minutes (1–60). A preset with warm-up enabled requires a
  non-blank name and a valid duration; disabling it stores no warm-up. Starting
  such a preset runs the normal countdown, then the warm-up timer, then the
  planned sets. The user can skip at any time; Finish and interrupted-session
  recovery also end the warm-up as skipped. The warm-up is not a strength set:
  it contributes no tonnage, PR/analytics input, finish-prompt trigger, or
  separate Health Connect record. The session stores its name, planned duration,
  actual elapsed duration, and `COMPLETED`/`SKIPPED` outcome; the summary and
  history description show one localized warm-up line when configured. Full backup and
  restore round-trip preset and session warm-up fields, and records without
  them restore with nulls.
- FR-13: Preset editor full-form scrolling (Planned) — the preset create/edit
  form (`EditingContent` in
  `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt`)
  places all form sections — preset name field, scheduled-day dropdown, warm-up
  toggle and warm-up name/duration fields, exercise validation error, exercises
  section header, catalog search input and matching results, and draft exercise
  rows — inside a single unified `LazyColumn` (`preset_draft_list`) occupying
  `Modifier.weight(1f)` between the top app bar and the pinned bottom
  `Cancel` / `Save Preset` action bar (≥ 80 % of the screen height in portrait
  with the soft keyboard hidden). Warm-up and the Exercises header/search are
  never pinned above the scroll container; scrolling down moves them off the
  top of the viewport so draft exercise rows can be viewed and edited across the
  full form height.
- FR-14: Workout hub routine placement (Planned) — in the Workout hub idle state
  (`IdleContent` in
  `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt`),
  the Routines section (and the "Presets" library button `presets_entry`) is
  positioned at the top of the scrollable catalog (`workout_catalog`), before
  the generic 10-activity list ("Choose a workout") and recent session history.
  When saved presets exist for the active profile, each routine card
  (`routine_start_<presetId>`) renders at the very top of the list so a routine
  can be started in a single tap without scrolling. When no presets exist, the
  "Presets" button remains at the top as the primary call-to-action to create or
  manage presets, followed by the generic activity list.

## Non-functional requirements

- Offline-first local persistence (Room), consistent with the rest of the app.
- >80% coverage on new code; a UI test per user-facing flow.
- Keyed lazy lists and timer/math off the main thread.
- Schema migrations are explicit and validated; routine name and warm-up
  columns are added via versioned migrations with schema export.
- Medical color system / Material 3; haptics for set/exercise completion;
  keep-screen-on during an active routine.

## Acceptance criteria

- AC-1: A preset can be created, edited, started, and deleted.
- AC-2: Starting a preset runs sets in order; a skipped set blocks the ones
  after it (UI disabled + state-machine guard).
- AC-3: Swapping an exercise resets weight/reps/sets/rest to the catalog
  default plan.
- AC-4: The quick pad updates a set even when its field is focused.
- AC-5: Finishing a routine shows the routine name and per-exercise name/sets/
  weight on the journal card.
- AC-6: An unfinished session survives a process kill at the last completed set.
- AC-7: (Planned) A weight trend chart and alternative conversion render from
  logged history, with a sensible fallback for a fresh user.
- AC-8: Completing the last planned set of a preset routine shows the finish
  prompt; Keep going leaves it active, Finish reaches Summary, unchecking and
  re-checking the set shows it again, and manual strength, HIIT, recovered, and
  partially complete sessions do not show it.
- AC-9: A preset warm-up can be configured, edited, disabled, started, skipped,
  and completed. Completing or skipping it records the planned and actual
  duration plus the outcome, shows the warm-up in the summary and one localized
  history line, and leaves tonnage, set count, finish-prompt state, and Health Connect
  records unchanged. A pre-warm-up preset or backup still opens and restores
  with no warm-up.
- AC-10: (Planned) In the preset create/edit form with warm-up enabled and
  multiple exercises added, the single scrollable form container spans the full
  height between the top app bar and the pinned bottom `Cancel` / `Save Preset`
  bar (≥ 80 % of the screen height with the keyboard closed); scrolling down
  scrolls the preset name, scheduled day, warm-up section, and exercise search
  off the top of the viewport so every draft exercise row and its
  Sets/Reps/Weight/Rest inputs can be scrolled into view and edited while
  `Cancel` and `Save Preset` remain pinned and reachable at the bottom.
- AC-11: (Planned) On the Workout screen in the idle state, the "Routines"
  header, saved routine cards, and the "Presets" button (`presets_entry`) appear
  at the top of `workout_catalog` above "Choose a workout" and all 10 activity
  buttons (`Run`, `Walk`, etc.); starting a routine (`routine_start_<presetId>`)
  or navigating to presets (`presets_entry`) does not require scrolling past
  the generic activity catalog.

## Grooming decisions (Preset editor scrolling)

| ID | Topic | Decision |
|---|---|---|
| G1 | Scroll container boundary | **Resolved (S1):** Single `LazyColumn` (`preset_draft_list`) holds header fields, warm-up, exercise search, and draft exercise rows; only the top app bar and bottom `Cancel` / `Save Preset` row stay pinned (`FR-13`, `AC-10`). |
| G2 | Catalog search results while scrolling | **Resolved (S2):** Catalog search input and results live inside the `LazyColumn` above draft exercises (with bounded height or overlay) and collapse on selection so they never pin the viewport. |
| G3 | Soft keyboard (IME) interaction | **Resolved:** Focusing any header, warm-up, search, or exercise field scrolls the focused field into view inside the unified `LazyColumn` while keeping the bottom action bar or keyboard-accessible scroll intact. |
| V1 | "Scrolling available only for 20% of the screen at the bottom. Warm-up and Exercises pinned" | **Resolved:** Replaced with measurable layout requirement in `FR-13` and `AC-10` — unified `LazyColumn` occupies ≥ 80 % of portrait viewport height (full space between `TopAppBar` and bottom CTA row), with 0 dp of pinned form content above the scroll container. |
| V2 | "Suggest best solution" | **Resolved:** Selected **S1 + S2** (single unified `LazyColumn` + pinned bottom CTA row with bounded/collapsible search results). |

## Grooming decisions (Workout hub routine placement)

| ID | Topic | Decision |
|---|---|---|
| G4 | Section ordering on Workout screen | **Resolved:** In `IdleContent`, order sections top-to-bottom: (1) Routines section (`R.string.workout_routines`) with routine cards if presets exist, (2) "Presets" library button (`presets_entry`), (3) "Choose a workout" header (`workout_choose`) + 10 generic activity buttons, (4) "Recent" workouts history list (`workout_recent`). (`FR-14`, `AC-11`). |
| G5 | Empty routines state on Workout hub | **Resolved:** When active profile has no presets, show "Routines" section header and the "Presets" button prominently at the top, directing users to create their first preset before the generic catalog. |

## Out of scope

- GPS/location routes, heart-rate/sensor integration, foreground service/
  notification, music/air-play sync, automated/guided HIIT plans, band/relative-
  strength comparisons, SQLCipher encryption.

## Cross-references

- `Docs/psd/workout-presets.md` — the specification that implements these
  requirements.
- `Docs/tests/workout-presets.md` — the test cases that verify them.
- [[ui-layer]] — the preset library and routine-execution screens.
- [[data-layer]] — preset, catalog, and session entities/DAOs.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` — preset library UI.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — routine execution, pad, swap UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/PresetViewModel.kt` — preset CRUD/validation state.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` — routine start/completion/swap/finish.
- `app/src/main/java/com/example/healthjournal/data/PresetRepository.kt` — preset persistence.
- `app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt` — preset entity.
- `app/src/main/java/com/example/healthjournal/data/local/ExerciseCatalogItem.kt` — catalog entity.
- `app/src/main/java/com/example/healthjournal/data/local/BuiltInExerciseCatalog.kt` — curated catalog + default plans.
- `app/src/main/java/com/example/healthjournal/domain/StrengthExercise.kt` — planned set row model + tonnage.
- `app/src/main/java/com/example/healthjournal/domain/WorkoutPreset.kt` — scheduled-day enum + preset exercise model.
- `app/src/main/java/com/example/healthjournal/domain/ValidatePreset.kt` — preset validation.
- `app/src/main/java/com/example/healthjournal/domain/WarmupHistoryFormatter.kt` — locale-aware persisted warm-up history line.
- `app/src/main/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCase.kt` — Epley 1RM.
- `app/src/main/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCase.kt` — conversion.
- `Docs/psd/workout-presets.md` — specification.
- `Docs/tests/workout-presets.md` — test cases.