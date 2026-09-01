# Google Drive Integration

> How this repo authorizes against and uses Google Drive: the auth flow, the app-data folder, and the sync client.

Last updated: 2026-09-01

## Relationship to Google

This vault records only *how this repo uses* Google Drive; Google owns the definitive
detail. The dependency runs **outward**: this app holds a Drive REST client (see
[[sync-engine]]) and calls Google's API. This repo does not host or implement any of
Drive's internals.

For authoritative API guidance, see Google's own documentation; the Drive service and
identity client libraries this app depends on are declared in `app/build.gradle.kts`.

## The auth flow (durable summary)

The modern pattern this app follows for Drive access:

1. **Identity** — Android Credential Manager identifies the user.
2. **Scopes** — Offline/refresh access is requested so a **refresh token** can be
   stored for the background worker.
3. **Background** — the worker refreshes the access token and initializes the Drive
   service (the [[sync-engine]] worker does this).

Two non-negotiable pitfalls, noted as durable gotchas: the certificate fingerprint
registered in Google Cloud Console must match this app's signing cert, and the OAuth
consent screen must include the Drive app-data scope or requests fail.

## Where the app data lives

Drive gives apps a private **app-data folder** referenced by the `drive.appdata`
scope, rather than a user-visible folder. This is what the sync code targets and is
why the scope must be requested.

## Direction of the dependency

Outbound to Google Drive. State it plainly: **the app is the client; Google Drive is
the service.**

## Cross-references

- [[auth]] — the on-device auth that produces the token.
- [[sync-engine]] — the consumer of the token and the Drive client.
- [[overview]] — where the app's layered flow is drawn.

## Sources

- `app/src/main/java/com/example/healthjournal/auth/GoogleAuthManager.kt` — the auth/token flow.
- `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` — the Drive REST client.
- `wiki/sources/google_drive_auth_research_2026.md` — migrated research note behind this page.

Back to [[overview]]
