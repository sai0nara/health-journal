<!--
  Copy to Docs/prd/<feature>.md, then fill in. Delete this comment block.
  Conventions (same rules the wiki lint enforces for vault pages):
  * Keep `Last updated:` current.
  * In `## Sources` cite the files that own the behaviour (one path per bullet,
    before the dash). Cited paths must exist and be committed or the lint fails.
  * Cite, don't copy - never transcribe mutable values (schema versions, ids,
    constants) into this page; reference the owning file instead.
  * No issue-tracker ids in file content.
-->

# <Feature Title> — Product Requirements

> One sentence on the value this feature delivers to the user.

Last updated: YYYY-MM-DD

## Overview

Short context: what the feature is and why the app needs it. Link to the owning
code (backticked path), not to a copy of its state.

## Goals / Non-goals

**Goals**

- ...

**Non-goals**

- ...

## User stories

- As a ..., I want ... so that ...

## Functional requirements

- FR-1: ...
- FR-2: ...

## Non-functional requirements

- Reliability, performance, privacy/security, offline behaviour...

## Acceptance criteria

Concrete, checkable statements. The product specification (`Docs/psd/<feature>.md`)
defines how these are met; the test cases (`Docs/tests/<feature>.md`) turn them
into scenarios.

## Out of scope

Anything explicitly deferred past this feature.

## Sources

- `app/src/main/java/.../Owning.kt` — where the above requirement is implemented.
- `Docs/psd/<feature>.md` — product specification.
- `Docs/tests/<feature>.md` — test cases.