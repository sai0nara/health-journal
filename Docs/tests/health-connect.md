# Health Connect — Test Cases

> Verifies the Health action wiring and permission flow at the UI level; the
> direct Health Connect SDK reads are exercised indirectly (there is no
> dedicated manager test today).

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` | Health button present/enabled; unavailable path toasts |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/EnrichmentPanelTest.kt` | Health button callback fires |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | metric columns survive migrations |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-3 | Health unavailable | fake SDK not available | tapping Health toasts, no permission flow |
| T-2 | FR-6 | Health button callback | Enrichment shown | tapping Health invokes the sync callback |
| T-3 | AC-1/AC-2 | Permission granted/denied | granted path | vitals filled on the entry; deny path toasts (verified via real device) |
| T-4 | FR-4 | Metric columns survive migration | schema migration | `bp_systolic`, `bp_diastolic`, `heart_rate_avg`, `sleep_hours` preserved |

## Manual checks

- On a Health Connect-enabled device, grant permissions and confirm BP/HR/sleep
  fill for a past date.
- Deny one permission and confirm the guidance toast and no vitals saved.
- Confirm the rationale screen appears in the platform permission flow.
- Verify the displayed vitals in the Add Entry health card and on a saved entry.

## Cross-references

- `Docs/prd/health-connect.md` — requirements under test.
- `Docs/psd/health-connect.md` — design the cases verify.
- [[instrumented]] — the instrumented stack.

## Sources

- `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` — Health button/unavailable path.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/EnrichmentPanelTest.kt` — Health callback.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — metric column migrations.
- `Docs/prd/health-connect.md` — requirements.
- `Docs/psd/health-connect.md` — design.