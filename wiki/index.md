# Vault Index

> The catalog of every page in the LLM Wiki. Each page appears here exactly once.

Last updated: 2026-09-01

## Service

- [[overview]] — what the app is and how a user-facing action flows through MVVM.

## Modules

- [[data-layer]] — Room, repositories, DAOs, and entities under `data/`.
- [[ui-layer]] — Compose screens, components, and theme.
- [[viewmodel-layer]] — the StateFlow ViewModels under `viewmodel/`.
- [[sync-engine]] — the Google Drive sync engine under `sync/`.
- [[export-restore]] — the export/backup/encrypt/zip/restore pipeline under `export/`.
- [[auth]] — Credential Manager auth and Drive token handling under `auth/`.
- [[domain-media]] — pure domain logic and the image compression service.

## Tests

- [[unit-tests]] — the JVM/MockK stack under `app/src/test`.
- [[instrumented]] — the Compose and Room instrumented stack under `app/src/androidTest`.

## Integrations

- [[google-drive]] — how this repo uses Google Drive and its auth (neighbour system).
- [[health-connect]] — how this repo uses Health Connect for wearable data.

## Meta

Governance documents live in `wiki/meta/spec.md` and `wiki/meta/schema.md`; by
design they are referenced by backticked path rather than wikilink, so they are not
listed here.
