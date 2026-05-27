# Research: Google Drive Authorization on Android (2026)

## Overview
This note summarizes the best practices for authorizing an Android app to access Google Drive, specifically the `appDataFolder`, using modern APIs available in 2026.

## Authentication (AuthN)
- **Tool:** Credential Manager
- **Purpose:** Identifies who the user is using Passkeys, Google Accounts, or Passwords.
- **Key Insight:** Do not use Credential Manager to request Drive scopes. It only provides identity (ID Token).

## Authorization (AuthZ)
- **Tool:** AuthorizationClient (Google Identity Services)
- **Purpose:** Requests specific OAuth scopes (e.g., `https://www.googleapis.com/auth/drive.appdata`).
- **Incremental Authorization:** Recommended to only request the scope when the user triggers a sync feature.

## Background Sync
- **Requirement:** Offline access to obtain a **Refresh Token**.
- **Process:**
    1. Request `serverAuthCode` in the foreground.
    2. Exchange code for tokens (locally or via backend).
    3. Securely store the Refresh Token.
    4. In `SyncWorker`, manually refresh the Access Token if expired.

## Pitfalls
- **SHA-1 Mismatch:** Drive API is highly sensitive to the certificate fingerprint registered in the Google Cloud Console.
- **Consent Screen:** Missing `drive.appdata` in the OAuth consent screen configuration causes failures.
- **Legacy Deprecation:** `GoogleSignInClient` is deprecated. Use `Identity` and `CredentialManager`.
