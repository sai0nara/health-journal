# Drive Sync — Test Cases

> Verifies the pure merge/payload logic at JVM unit level and the Drive client
> and sign-in flow at instrumented/unit level, plus the end-to-end media merge.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/SyncMergeTest.kt` | journal merge: local/cloud/tie, tag preservation |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncMergeTest.kt` | measurement LWW merge |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/GoalSyncMergeTest.kt` | goal prune + LWW merge |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncPayloadTest.kt` | payload round-trip |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/MeasurementTombstonePayloadTest.kt` | tombstone round-trip + defensive parse |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/GoalSyncPayloadTest.kt` | goal payload round-trip |
| JVM unit | `app/src/test/java/com/example/healthjournal/sync/DriveServiceHelperTest.kt` | Drive upload/download against app-data |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/CloudSyncTest.kt` | History sign-in renders + calls viewModel |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/sync/SyncDownloadTest.kt` | worker merges multiple photos/attachments |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-2 | Local newer wins | local `lastModified` > cloud | local row kept |
| T-2 | AC-2 | Cloud wins tie | equal `lastModified` | cloud row kept |
| T-3 | AC-2 | Cloud wins with tags | cloud payload has tags | cloud tags replace |
| T-4 | AC-2 | Cloud legacy null tags | cloud tags null | local tags preserved |
| T-5 | AC-3 | Tombstone removes cloud row | cloud copy + local tombstone | cloud row filtered out; merged back |
| T-6 | AC-1 | Goal prune | local goal absent from cloud | local-only goal pruned |
| T-7 | AC-1 | Media merge | worker with 2 photos + 1 attachment | files survive; URIs remapped to filesDir |
| T-8 | AC-5 | Sign-in flow | not signed in, History shown | tapping Sign In calls viewModel.signIn |
| T-9 | FR-8 | Payload defensive parse | garbage/null cloud file | parses to empty list, no crash |

## Manual checks

- Two devices on the same account converge after edits (wait a sync cycle).
- Revoking Drive access surfaces a re-authorization state and recovers after
  re-consent.
- Sync status text transitions on the History screen during a manual sync.

## Cross-references

- `Docs/prd/drive-sync.md` — requirements under test.
- `Docs/psd/drive-sync.md` — design the cases verify.
- `Docs/prd/photos-media.md` — media merge/remap cases.
- [[unit-tests]] / [[instrumented]] — test stacks.
- [[sync-engine]] — the code under test.

## Sources

- `app/src/test/java/com/example/healthjournal/sync/SyncMergeTest.kt` — journal merge.
- `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncMergeTest.kt` — measurement merge.
- `app/src/test/java/com/example/healthjournal/sync/GoalSyncMergeTest.kt` — goal merge.
- `app/src/test/java/com/example/healthjournal/sync/MeasurementSyncPayloadTest.kt` — payload.
- `app/src/test/java/com/example/healthjournal/sync/MeasurementTombstonePayloadTest.kt` — tombstone payload.
- `app/src/test/java/com/example/healthjournal/sync/GoalSyncPayloadTest.kt` — goal payload.
- `app/src/test/java/com/example/healthjournal/sync/DriveServiceHelperTest.kt` — Drive client.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/CloudSyncTest.kt` — sign-in UI.
- `app/src/androidTest/java/com/example/healthjournal/sync/SyncDownloadTest.kt` — media merge.
- `Docs/prd/drive-sync.md` — requirements.
- `Docs/psd/drive-sync.md` — design.