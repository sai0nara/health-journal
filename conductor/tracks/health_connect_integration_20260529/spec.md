# Specification: Health Connect Integration

## Goal
Integrate Android's Health Connect SDK to automatically import physical activity metrics (steps, heart rate, sleep) into journal entries.

## Problem Statement
Users currently have to manually record their health data. Automating this via Health Connect provides a more comprehensive and accurate view of their health journey with minimal effort.

## Objectives
- Implement the Health Connect SDK.
- Create a permission request flow for the required health data types (Steps, Heart Rate, Sleep).
- Implement logic to fetch aggregated or raw metrics from Health Connect for a specific date/time range corresponding to a journal entry.
- Update the `JournalEntry` entity to store these metrics if not already present.
- Provide a "Sync Health" action in the `AddEntryScreen` that fetches data for the selected entry date.

## Data Types
- **Steps**: Total count for the day.
- **Heart Rate**: Average beats per minute during the day or activity period.
- **Sleep**: Total duration in hours for the previous night.

## Success Criteria
- User can successfully grant permissions for Health Connect data.
- Clicking "Sync Health" in the `AddEntryScreen` correctly populates steps, heart rate, and sleep fields based on the selected date.
- Imported health data is correctly persisted locally and synchronized to Google Drive.
