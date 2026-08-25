# Health Journal — Bug & Security Audit Report

**Date:** August 23, 2026  
**Target Application:** Health Journal (Android)  
**Package:** `com.example.healthjournal`  
**Compile SDK:** 35 | **Min SDK:** 26  
**Architecture:** Jetpack Compose, Room SQLite, Coroutines/Flow, MVVM, WorkManager  

---

## 1. Executive Summary

This report documents potential bugs, data integrity issues, concurrency anomalies, and security/privacy vulnerabilities identified during an in-depth source code audit of the Health Journal Android application.

### Severity Breakdown
| Severity | Count | Summary |
| :--- | :---: | :--- |
| **Critical / High** | 4 | Plaintext sensitive health data persistence, backup data extraction exposure, sync partial-upload false-positive status, stream consumption bug in media compression. |
| **Medium** | 7 | DatePicker UTC timezone shift, ZIP duplicate filename crash, PDF metric omission bug, unescaped Drive API query, session unencrypted storage, shared tombstone table, orphaned media leak. |
| **Low / Quality** | 5 | Inconsistent archive inclusion in exports, plaintext rich-text strip in PDF, Health Connect rationale text mismatch, silent future entry drop, build tooling warnings. |

---

## 2. Security & Privacy Vulnerabilities

### SEC-01: Plaintext Local Database Storage of Sensitive Health & Medical Data
* **Severity:** **High**
* **Affected Component:** [`JournalDatabase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt)
* **Description:**  
  The app stores Protected Health Information (PHI) including systolic/diastolic blood pressure, resting heart rate, sleep duration, body circumference measurements, illness logs, and doctor visit records in standard unencrypted SQLite database (`journal_database`). While Android's sandbox provides application isolation, rooted devices, physical forensic extraction, or malware exploiting kernel vulnerabilities can read the raw database file directly in plaintext.
* **Remediation:**  
  Integrate SQLCipher (e.g. `net.zetetic:android-database-sqlcipher`) with `Room.databaseBuilder(...).openHelperFactory(SupportFactory(passphrase))` or encrypt sensitive columns using Android Keystore.

---

### SEC-02: Missing Backup & Data Extraction Protection Rules
* **Severity:** **High**
* **Affected Component:** [`AndroidManifest.xml`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/AndroidManifest.xml#L21)
* **Description:**  
  `android:allowBackup="true"` is declared in the manifest without defining `android:dataExtractionRules` (required for Android 12+, API 31+) or `android:fullBackupContent`. This allows ADB backups (`adb backup`) and Google cloud device backups to extract the plaintext health database and cached media.
* **Remediation:**  
  Set `android:allowBackup="false"` or create `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` that explicitly exclude the SQLite database and internal files directory from device-to-device and cloud backups.

---

### SEC-03: User Session Stored in Unencrypted SharedPreferences
* **Severity:** **Medium**
* **Affected Component:** [`SessionManager.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/auth/SessionManager.kt#L7)
* **Description:**  
  `SessionManager` uses standard `context.getSharedPreferences("health_journal_session", Context.MODE_PRIVATE)` to persist the user's authenticated Google email address. Notice `androidx.security:security-crypto` is already imported in `build.gradle.kts`, but not utilized here.
* **Remediation:**  
  Replace standard `SharedPreferences` with `EncryptedSharedPreferences.create(...)` using `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)`.

---

### SEC-04: Declared Legacy Storage Permissions and `requestLegacyExternalStorage`
* **Severity:** **Low**
* **Affected Component:** [`AndroidManifest.xml`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/AndroidManifest.xml#L5-L26)
* **Description:**  
  `AndroidManifest.xml` specifies `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `android:requestLegacyExternalStorage="true"`. On Android 10+ (API 29+) and especially API 35 (the target SDK), `requestLegacyExternalStorage` is ignored. The app uses modern `PickMultipleVisualMedia` and `OpenDocument` Storage Access Framework (SAF) contracts, making these permissions obsolete and potentially problematic during Google Play policy review.
* **Remediation:**  
  Remove `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `requestLegacyExternalStorage` from `AndroidManifest.xml`.

---

### SEC-05: Potential Drive Query Injection with Special Characters in File Search
* **Severity:** **Medium**
* **Affected Component:** [`DriveServiceHelper.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt#L95)
* **Description:**  
  In `findFileByName(name: String)`:
  ```kotlin
  val result = driveService.files().list()
      .setQ("name = '$name' and trashed = false")
  ```
  If `name` contains single quotes (e.g. attachment titled `dr_smith's_notes.pdf`), the query string breaks syntax, throwing a Google Drive 400 Bad Request error.
* **Remediation:**  
  Escape single quotes in the query parameter: `val escapedName = name.replace("'", "\\'")` and query `name = '$escapedName' and trashed = false`.

---

### SEC-06: FileProvider Export URI Grant Scope in Intent Chooser
* **Severity:** **Low**
* **Affected Component:** [`ExportScreen.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt#L40-L45)
* **Description:**  
  When sharing exported PDF/ZIP files, `addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)` is set on the share intent passed into `Intent.createChooser()`. On several Android OS versions, flags set on the inner intent do not automatically propagate through the chooser dialog to the selected target application, causing target apps to throw `SecurityException` upon opening the file.
* **Remediation:**  
  Set `clipData = ClipData.newRawUri("", state.fileUri)` on the share Intent, or grant URI permissions explicitly on the chooser intent as well.

---

## 3. Data Integrity & Sync Edge Cases

### BUG-01: DatePicker UTC Timezone Shift Leading to Off-by-One Day Corruption
* **Severity:** **High**
* **Affected Components:**  
  - [`AddEntryScreen.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt#L237-L242)
  - [`MeasurementEntrySheet.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt#L145)
  - [`ExportScreen.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt#L163)
* **Description:**  
  Material 3's `rememberDatePickerState().selectedDateMillis` yields UTC midnight epoch milliseconds.  
  In `AddEntryScreen`:
  ```kotlin
  val newCal = Calendar.getInstance().apply { timeInMillis = it }
  calendar.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
  calendar.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
  calendar.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
  ```
  Because `Calendar.getInstance()` uses the device's local timezone, converting UTC midnight shifts the date backward by 1 day in western timezones (e.g. UTC-5, UTC-8). Selecting Aug 23 in the picker sets the entry date to Aug 22.
* **Remediation:**  
  Use `Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }` to extract the selected Year, Month, and Day values before applying them to the local calendar.

---

### BUG-02: Media Compression Fallback Reads Consumed InputStream
* **Severity:** **High**
* **Affected Component:** [`MediaCompressionService.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/media/MediaCompressionService.kt#L64-L98)
* **Description:**  
  In `AndroidMediaCompressionService`:
  ```kotlin
  val bytes = inputStream.readBytes() // Consumes the stream
  ...
  } catch (e: Exception) {
      try {
          saveRawFallback(inputStream.readBytes(), destFile) // Fails! Stream is already at EOF
      } catch (ex: Exception) { ... }
  }
  ```
  If an exception occurs after `inputStream.readBytes()`, the catch block calls `inputStream.readBytes()` again on the already-consumed stream, returning an empty `ByteArray` (0 bytes). Consequently, `saveRawFallback` returns `null` and the user's attached photo is silently lost.
* **Remediation:**  
  Pass the already buffered `bytes` array to `saveRawFallback(bytes, destFile)` instead of re-reading the stream.

---

### BUG-03: Partial Media Upload Failure Silently Marks Entries as Fully Synced
* **Severity:** **High**
* **Affected Component:** [`SyncWorker.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt#L166-L196)
* **Description:**  
  During cloud synchronization:
  ```kotlin
  try {
      if (localFile.exists()) driveHelper.uploadFile(localFile, "image/jpeg")
  } catch (e: Exception) {
      Log.e("SyncWorker", "Failed to upload photo...", e)
  }
  ```
  If media file uploads fail due to network drops or timeouts, the exception is caught and ignored. The worker then proceeds to write the JSON metadata to Drive and marks the entry `isSynced = true`. On other connected devices, downloading the entry metadata will fail to fetch the missing media files from Drive, resulting in broken image cards.
* **Remediation:**  
  Track individual media upload success and do not mark attachments/photos as cloud-synced unless their upload succeeds. If required uploads fail, return `Result.retry()`.

---

### BUG-04: URL-Encoded Paths (`%20`) Failing in `SyncWorker` and `ZipExportUseCase`
* **Severity:** **Medium**
* **Affected Components:**  
  - [`SyncWorker.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt#L171)
  - [`ZipExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/ZipExportUseCase.kt#L52)
* **Description:**  
  `JournalViewModel.deleteSandboxedFile` correctly noted that `file://` URIs can contain URL-encoded characters (like spaces `%20`) and used `URLDecoder.decode(rawPath, "UTF-8")`. However, `SyncWorker` and `ZipExportUseCase` do `File(uri.path ?: "")` directly without URL decoding. For files with spaces or special characters in their names, `File.exists()` returns `false`, causing sync and export to skip the files.
* **Remediation:**  
  Decode `uri.path` with `java.net.URLDecoder.decode(uri.path, "UTF-8")` or use `Uri.parse(url).path` with proper decoding.

---

### BUG-05: Duplicate Attachment Filenames Crash ZIP Export
* **Severity:** **Medium**
* **Affected Component:** [`ZipExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/ZipExportUseCase.kt#L61-L75)
* **Description:**  
  In `ZipExportUseCase`:
  ```kotlin
  val entryName = "media/$fileName"
  zos.putNextEntry(ZipEntry(entryName))
  ```
  If multiple journal entries have attachments with the same filename (e.g. two entries both attaching `prescription.pdf`), `ZipOutputStream.putNextEntry()` throws `java.util.zip.ZipException: duplicate entry: media/prescription.pdf`, crashing the export workflow.
* **Remediation:**  
  Deduplicate ZIP entry names using unique prefixes or counters (e.g., `media/${entry.entry_id}_$fileName`).

---

### BUG-06: Shared `deleted_entries` Tombstone Table Without Entity Discriminator
* **Severity:** **Medium**
* **Affected Components:**  
  - [`DeletedEntry.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/data/local/DeletedEntry.kt)
  - [`SyncWorker.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt#L102-L211)
* **Description:**  
  Both `JournalRepository` and `BodyMeasurementRepository` write tombstones into the single `deleted_entries` table. In `SyncWorker`:
  ```kotlin
  val deletedIds = repository.getDeletedEntryIds()
  cloudEntries = cloudEntries.filterNot { it.entry_id in deletedIds }
  ...
  repository.clearDeletedEntries() // Purges tombstones older than 30 days
  ...
  cloudMeasurements = cloudMeasurements.filterNot { it.entry_id in deletedIds }
  ```
  When `clearDeletedEntries()` is called between the journal step and the body measurement step, if any deleted tombstones were purged, subsequent sync branches might operate on inconsistent tombstone sets. Moreover, sharing a single table without an `entity_type` column couples two distinct sync pipelines.
* **Remediation:**  
  Add `type: String` ("JOURNAL" vs "BODY_MEASUREMENT") to `deleted_entries` or maintain separate tombstone tables for each entity.

---

### BUG-07: Orphaned Media Files When Images Are Removed During Edit
* **Severity:** **Medium**
* **Affected Component:** [`AddEntryScreen.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt#L583-L598)
* **Description:**  
  When an existing journal entry is edited and photos/attachments are deleted by clicking the "Remove (X)" icon, the updated entry is saved with the remaining list of URIs. However, the physical image file on disk in `files/photos/` is never deleted. Over time, editing entries leaves abandoned image files taking up storage space.
* **Remediation:**  
  Diff the original photo URIs against the updated photo URIs in `JournalViewModel.updateEntry`, and invoke `deleteSandboxedFile` on any removed files.

---

## 4. Functional & UI Logic Bugs

### BUG-08: PDF Export Omits Heart Rate & Sleep Metrics if Blood Pressure is Null
* **Severity:** **Medium**
* **Affected Component:** [`PdfExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt#L65-L71)
* **Description:**  
  ```kotlin
  if (entry.bp_systolic != null) {
      val metrics = "BP: ${entry.bp_systolic?.toInt()}/${entry.bp_diastolic?.toInt()} mmHg | HR: ${entry.heart_rate_avg} bpm | Sleep: ${entry.sleep_hours}h"
      document.add(Paragraph(metrics)...)
  }
  ```
  If an entry has `heart_rate_avg` or `sleep_hours` recorded but `bp_systolic` is `null`, the entire `if` condition evaluates to false. As a result, heart rate and sleep metrics are completely omitted from the generated PDF report.
* **Remediation:**  
  Check and format each metric independently so any recorded metric is included in the output.

---

### BUG-09: Inconsistent Archive Inclusion Between PDF and ZIP Exports
* **Severity:** **Low**
* **Affected Components:**  
  - [`PdfExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt#L30)
  - [`ZipExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/ZipExportUseCase.kt#L21)
* **Description:**  
  `PdfExportUseCase` queries `repository.allEntries.first()`, which filters `WHERE isArchived = 0` (excluding archived entries). In contrast, `ZipExportUseCase` calls `repository.getAllEntriesInDateRange()`, which returns all entries including archived entries. Users exporting the same date range receive different sets of data depending on the chosen format.
* **Remediation:**  
  Unify data fetching by having both export use cases call `repository.getAllEntriesInDateRange(startDate, endDate)`.

---

### BUG-10: PDF Export Strips All Rich Text Styling
* **Severity:** **Low**
* **Affected Component:** [`PdfExportUseCase.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt#L60)
* **Description:**  
  `val plaintext = Html.fromHtml(entry.description, ...).toString()` converts HTML to a Spanned object and immediately flattens it to plain unformatted string. All bold, italics, underline, and links formatted in the rich text editor are lost in the PDF.
* **Remediation:**  
  Use iText 7's `HtmlConverter.convertToElements(entry.description)` or parse the Spanned styles into iText formatted Text blocks.

---

### BUG-11: Undo Snapshot Overwritten on Rapid Successive Deletions
* **Severity:** **Low**
* **Affected Component:** [`BodyMeasurementViewModel.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt#L51-L69)
* **Description:**  
  `BodyMeasurementViewModel` holds a single `pendingUndoSnapshot: BodyMeasurementEntry?`. If a user deletes two measurements in rapid succession, the second deletion overwrites the snapshot. Tapping "Undo" on the first snackbar restores the second deleted entry instead of the first.
* **Remediation:**  
  Store pending undo snapshots in a map keyed by entry ID (`Map<String, BodyMeasurementEntry>`).

---

### BUG-12: Health Connect Permission Rationale Text Mismatch
* **Severity:** **Low**
* **Affected Component:** [`PermissionsRationaleActivity.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/PermissionsRationaleActivity.kt#L33)
* **Description:**  
  The rationale dialog displays:  
  *"This app requires access to your Health data to automatically track steps, heart rate, and sleep duration..."*  
  However, the app requests Blood Pressure (`READ_BLOOD_PRESSURE`), Heart Rate (`READ_HEART_RATE`), and Sleep (`READ_SLEEP`), but not Steps. Misleading permission rationales violate Google Play Health Connect policy.
* **Remediation:**  
  Update the rationale text to match the declared permissions: *"This app requires access to your Health data to automatically track blood pressure, heart rate, and sleep duration..."*

---

### BUG-13: Silent Discard of Future-Dated Journal Entries
* **Severity:** **Low**
* **Affected Component:** [`JournalViewModel.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt#L243-L246)
* **Description:**  
  In `addEntry()`, if `timestamp > System.currentTimeMillis()`, the method logs a warning and returns immediately without inserting or notifying the UI. If a user sets a future appointment reminder or if there is slight device clock drift, the entry is silently lost.
* **Remediation:**  
  Either clamp the timestamp to `System.currentTimeMillis()` (as `AddEntryScreen` already does at line 547) or display an error message to the user.

---

### BUG-14: Incomplete Cache Cleanup in `ExportServiceImpl`
* **Severity:** **Low**
* **Affected Components:**  
  - [`ExportService.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/ExportService.kt#L11)
  - [`ImageResizer.kt`](file:///Users/sai/VS_CODE_PROJECTS/health-journal/app/src/main/java/com/example/healthjournal/export/ImageResizer.kt#L54)
* **Description:**  
  `ExportServiceImpl.cleanupCache()` only deletes files inside `cacheDir/exports`. However, `ImageResizer` creates temporary files directly in `cacheDir` (`export_tmp_img_...jpg`), and `AddEntryScreen` creates camera temp files in `cacheDir/images/`. These directories are never cleared by `cleanupCache()`.
* **Remediation:**  
  Update `cleanupCache()` to also delete `export_tmp_img_*.jpg` and empty `cacheDir/images/`.

---

## 5. Remediation Priority Matrix

| Priority | Issue ID | Area | Effort | Impact |
| :--- | :--- | :--- | :--- | :--- |
| **P0** | **BUG-01** | UI / Data Integrity | Low | Fixes DatePicker timezone shift corrupting log dates. |
| **P0** | **BUG-02** | Media / Data Integrity | Low | Prevents photo loss on compression exception fallback. |
| **P0** | **BUG-03** | Cloud Sync | Medium | Ensures cloud consistency when media uploads fail. |
| **P1** | **SEC-01** | Security / Privacy | Medium | Encrypts local SQLite database with SQLCipher. |
| **P1** | **SEC-02** | Security / Backups | Low | Disables or restricts cloud & ADB backup of medical data. |
| **P1** | **BUG-05** | Export | Low | Prevents ZIP export crash on duplicate filenames. |
| **P1** | **BUG-08** | Export | Low | Ensures HR and Sleep metrics appear in PDF reports. |
| **P2** | **SEC-03** | Security | Low | Uses `EncryptedSharedPreferences` for user session. |
| **P2** | **BUG-04** | Cloud Sync / Export | Low | Decodes URL-encoded paths for files with spaces. |
| **P2** | **BUG-07** | Storage | Low | Cleans up orphaned photos when removed in edit screen. |
| **P2** | **BUG-12** | Compliance | Low | Corrects Health Connect rationale text for Play Store compliance. |
