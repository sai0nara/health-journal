# Overview

> What the health-journal app is, how it is laid out, and how a user-facing action flows through the MVVM layers.

Last updated: 2026-09-01

## What this app is

A single-module Android health journal for personal event logging and health metric
tracking. UI is Jetpack Compose (Material 3), persistence is Room, and the code
follows MVVM with a repository layer. The module at `app/:app` (see the module build
file) contains everything; there are no separate sub-projects.

Source of the module build: `app/build.gradle.kts`.

## Layered flow

A user-facing action follows MVVM top-to-bottom:

1. A Compose screen in [[ui-layer]] reads state and forwards intents to a ViewModel.
2. A ViewModel in [[viewmodel-layer]] exposes `StateFlow` state and calls into repositories.
3. Repositories in [[data-layer]] read and write Room entities and DAOs locally, or
   delegate to [[sync-engine]] for Drive.
4. Media-heavy or long-running work flows through [[auth]], [[export-restore]], and
   [[domain-media]] as needed.

## The hard parts a newcomer gets wrong

- **Google Drive sync is implemented, not aspirational.** Production code under `sync/`
  ([[sync-engine]]) holds real Drive REST clients and a WorkManager worker, even
  though the roadmap header in `AGENTS.md` lists it as open. Trust the [[sync-engine]]
  page, not the roadmap line.
- **There are two test stacks.** `app/src/test` is the JVM unit stack; `app/src/androidTest`
  is the instrumented Compose/Room stack. Commands differ and Compose tests need a
  special flag. See [[unit-tests]] and [[instrumented]].

## Cross-references

- [[data-layer]] — the persistence layer behind every screen.
- [[sync-engine]] — where the Drive integration actually lives.
- [[unit-tests]] and [[instrumented]] — the two test stacks.
- [[google-drive]] and [[health-connect]] — the two neighbour systems.

## Sources

- `app/build.gradle.kts` — single-module build, SDK levels, runner, dependencies.
