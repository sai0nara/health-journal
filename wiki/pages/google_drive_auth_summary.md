# Summary: Google Drive Authorization (2026)

This page summarizes the modern patterns for Drive Auth on Android as of 2026, extracted from [[google_drive_auth_research_2026]].

## Key Architecture
- **AuthN:** [[credential_manager]] for identity.
- **AuthZ:** [[google_identity_services]] (`AuthorizationClient`) for scopes.

## Implementation Flow
1. Foreground: Get `serverAuthCode` via `AuthorizationRequest.requestOfflineAccess()`.
2. Foreground: Exchange for `refreshToken`.
3. Background: [[sync_worker]] refreshes token and initializes `Drive` service.

## Critical Requirements
- [[google_cloud_console]] configuration (SHA-1, Scopes).
- [[app_data_folder]] usage for private app data.
