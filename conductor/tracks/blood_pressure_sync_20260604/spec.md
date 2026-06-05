# Specification: Switch Steps Sync to Blood Pressure Sync

## Goal
Replace the step count metric with blood pressure (systolic/diastolic) in journal entries and Health Connect synchronization.

## Objectives
- Remove the `steps` field from the `JournalEntry` entity.
- Add `bp_systolic: Double?` and `bp_diastolic: Double?` fields to the `JournalEntry` entity.
- Update `HealthConnectManager` to:
    - Remove `StepsRecord` logic.
    - Implement `BloodPressureRecord` fetching.
- Update permissions:
    - Replace `android.permission.health.READ_STEPS` with `android.permission.health.READ_BLOOD_PRESSURE` in `AndroidManifest.xml` and `health_permissions.xml`.
- Update UI:
    - Remove step count display from `HistoryScreen` and `AddEntryScreen`.
    - Add blood pressure input/display to `AddEntryScreen` and `HistoryScreen`.

## Data Mapping
- **Systolic**: Millimeters of mercury (mmHg).
- **Diastolic**: Millimeters of mercury (mmHg).

## Success Criteria
- The application no longer requests or displays step data.
- The application successfully requests blood pressure permissions.
- "Sync Health" correctly fetches systolic/diastolic values from Health Connect and populates the entry.
- Blood pressure is persisted locally and synced to Google Drive.
