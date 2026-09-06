# AI Insights — Product Requirements

> Generate AI-driven health advice from the user's logged entries and metrics,
> shown as insights. **Planned, not yet built** — only the dormant `ai_advice`
> schema column exists today; there is no AI backend, prompt pipeline, or UI.

Last updated: 2026-09-02

> **Status: PLANNED.** This PRD records the agreed vision. The feature has not
> been implemented: no AI client, no prompt construction, no network call, and
> no UI read the `ai_advice` column. It is documented up-front so the intent is
> captured before work starts; the product specification and test cases are
> added when the feature is built.

## Overview

The journal captures a rich, dated stream of entries and health metrics. The
insight feature turns that history into guidance: given the last period of logs
(text plus the attached blood-pressure, heart-rate, and sleep values), a large
language model acting as a health coach returns advice that the user can read
per entry and across history. The intent is informational, not diagnostic.

A single nullable `ai_advice` column already exists in the entry schema and is
preserved through backup/restore and sync as plain data — but nothing writes or
renders it.

## Goals / Non-goals

**Goals**

- Read logged entries plus their health metrics and produce per-entry advice.
- Show advice in a dedicated Insights surface and alongside entries.
- Keep AI credentials out of the shipped binary (proxy backend or user-provided
  key, never a hard-coded key).

**Non-goals**

- Replace professional medical advice; the feature is clearly a health coach,
  not a diagnosis.
- Run models on-device.
- Guarantee offline or real-time availability of insights.
- Store or expose the AI conversation history beyond the per-entry advice field.

## User stories

- As a user, I want to read AI-driven guidance based on my recent logs and
  metrics so I understand what may correlate with how I feel.
- As a user of the Insights screen, I want to trigger an analysis and see earlier
  advice so I can track my habits over time.
- As a user, I want to know the analysis is informational and that my data is
  handled without hard-coded API keys.

## Functional requirements

- FR-1: The system prompts the model to act as a health coach/analyst and sends
  a bounded window of recent logs (text + health metrics).
- FR-2: The returned advice is stored on the journal entry's `ai_advice` field.
- FR-3: A dedicated Insights surface triggers an analysis and lists historical
  advice.
- FR-4: The advice is also rendered inline with the entry it was generated for.
- FR-5: API credentials are provided without embedding a key in the binary
  (backend proxy or user-supplied key flow).

## Non-functional requirements

- Privacy: the prompt is scoped to the user's own, clearly-labeled data; no
  credentials in the app.
- Cost control: the analysis window is bounded and the user initiates each call,
  so API cost follows explicit user action.
- Robustness: a failed or unavailable model degrades to no advice, never a
  crash or a corrupted entry.

## Acceptance criteria

(To be defined with the PSD when the feature is built.)

## Out of scope

- The "Insights Tab" does not exist yet; it is part of this feature's scope once
  built.
- No AI backend exists in this repository today; adding one is implementation
  work, not documentation.

## Cross-references

- `Docs/psd/ai-insights.md` — the specification (added when the feature is built).
- `Docs/tests/ai-insights.md` — the test cases (added when the feature is built).
- [[data-layer]] — the entry schema that carries the `ai_advice` field.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — the persisted `ai_advice` column that already exists.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — the future inline-advice rendering target.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — advice is carried through backup as entry data.