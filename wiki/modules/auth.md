# Auth

> How the app authenticates the user and obtains the tokens the Google Drive sync engine uses.

Last updated: 2026-09-01

## What lives here

The `auth` package handles authentication and session state. It is the on-device
piece of the [[google-drive]] integration: it produces the identity and the token the
[[sync-engine]] uses to talk to Drive.

- `GoogleAuthManager` — the entry point for Google identity and the Drive token flow.
- `SessionManager` — holds the current authenticated session.

The underlying auth technology is Android Credential Manager (identity) combined with
the Google identity/play-services auth libraries, declared in the module build file.
The Drive scope authorization steps and their pitfalls are documented on the
[[google-drive]] page rather than transcribed here.

## Direction of the dependency

This package is the client side of a Google identity/Drive authorization. The details
of authorizing against Google's services belong to Google; this vault only records
how this repo consumes them, and points outward for the authoritative flow.

## Cross-references

- [[google-drive]] — the neighbour system and its auth flow.
- [[sync-engine]] — the consumer of the token this package produces.
- [[unit-tests]] — auth and token logic is exercised by drive-helper JVM tests.

## Sources

- `app/src/main/java/com/example/healthjournal/auth/GoogleAuthManager.kt` — Google identity and token flow.

Back to [[overview]]
