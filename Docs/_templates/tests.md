<!--
  Copy to Docs/tests/<feature>.md, then fill in. Delete this comment block.
  Conventions (same rules the wiki lint enforces for vault pages):
  * Keep `Last updated:` current.
  * In `## Sources` cite the real test files (one path per bullet, before the
    dash). Cited paths must exist and be committed or the lint fails. Do NOT
    transcribe test names/ids here - point at the files and describe scope.
  * No issue-tracker ids in file content.
-->

# <Feature Title> — Test Cases

> Maps the PRD's acceptance criteria and the PSD's edge cases to concrete
> verification. The "Automated coverage" table cites the real test files; keep
> it in sync when coverage moves.

Last updated: YYYY-MM-DD

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/.../XTest.kt` | ... |
| Instrumented | `app/src/androidTest/java/.../YTest.kt` | ... |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | ... | ... | ... | ... |

## Manual checks

Anything that needs a real device or an external system (SAF pickers, Drive).

## Cross-references

- `Docs/prd/<feature>.md` — the requirements under test.
- `Docs/psd/<feature>.md` — the design the cases verify.
- [[export-restore]] or another relevant wiki page — where the test suite is explained.

## Sources

- `app/src/test/java/.../XTest.kt` — unit coverage.
- `app/src/androidTest/java/.../YTest.kt` — instrumented coverage.
- `Docs/prd/<feature>.md` — requirements under test.
- `Docs/psd/<feature>.md` — design the cases verify.