<!--
  Copy to Docs/psd/<feature>.md, then fill in. Delete this comment block.
  Conventions (same rules the wiki lint enforces for vault pages):
  * Keep `Last updated:` current.
  * In `## Sources` cite the files that own the design (one path per bullet,
    before the dash). Cited paths must exist and be committed or the lint fails.
  * Cite, don't copy - never transcribe mutable values (schema versions, ids,
    constants) into this page; reference the owning file instead.
  * No issue-tracker ids in file content.
-->

# <Feature Title> — Product Specification

> One sentence on the technical design ("how") that delivers the PRD's "what".

Last updated: YYYY-MM-DD

## Overview

The shape of the design and the trade-offs made. Reference the PRD
(`Docs/prd/<feature>.md`) rather than repeating it.

## Architecture

Layers involved, patterns used (MVVM/StateFlow, use-case orchestration,
WorkManager), and where the seams are for testing.

## Data flow

Numbered steps from user action to durable change. Keep every step mappable to
a file cited below.

## Components

| Component | File | Responsibility |
|---|---|---|
| ... | `app/src/main/java/.../X.kt` | ... |

## Edge cases & failure handling

Table of the failure modes this design anticipates and the expected behaviour
for each (typed errors, retry, data-intact guarantees).

## Dependencies

Libraries or platform services the feature relies on (Room transactions,
WorkManager, cryptography, SAF, ...).

## Sources

- `app/src/main/java/.../Owning.kt` — where the design is implemented.
- `Docs/prd/<feature>.md` — requirements this specification implements.
- `Docs/tests/<feature>.md` — test cases.