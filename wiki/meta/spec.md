# Vault Specification

> The contract that governs the LLM Wiki: what may go in, who publishes, how freshness is maintained, and the accuracy gate.

Last updated: 2026-09-02

## Purpose

The vault is an in-repo, agent-maintained knowledge base of the durable answers
about the health-journal codebase. It exists for agents and the engineers who run
them — not for stakeholders, not as general service documentation, not as a
replacement for the README. It reduces the cost of working with the codebase (fewer
tool calls, fewer tokens, fresh-session-per-task) rather than claiming to make a
capable agent more correct than it would be reading source directly.

## The three rules

1. **The agent owns the vault.** When a change touches code a page cites, that page
   is updated in the **same commit** as the code. There is no separate documentation
   change-set and no human doc-owner. A human reviews the change like any other code
   review.
2. **Cite, don't copy.** Durable structure is explained; mutable values are *cited*
   to the file that owns them, never transcribed.
3. **The lint exits 0 before the turn ends.**

Rule 3 is extended to the product docs: the lint enforces the same
citation/freshness rules on `Docs/prd`, `Docs/psd`, and `Docs/tests`, and treats
`Docs/index.md` as the source of truth for which feature documents must exist
(the `MISSING` check fails a feature that lacks any of its three documents).
Templates under `Docs/_templates/` are exempt from checks.

## Accuracy gate

Before a page is committed, it must satisfy every item:

1. No copied mutable values; they are cited instead.
2. Every cited path exists, and is committed.
3. No invented entities — no channel, URL, or environment name that was not verified.
4. Operational claims match the real setup, not a plausible-sounding alternative.
5. No one-off or task-specific debris.
6. `Last updated:` and `## Sources` are both present.
7. Any page describing another system carries an outward link to that system's own
   source of truth.

## Devotional table

Where this vault deviates from the reference taxonomy, the deviation is recorded
here so it stays a decision rather than drift:

| Deviation | Reason |
|---|---|
| Vault lives under `wiki/`, not a fresh top-level `<vault>/` directory | An existing `wiki/` (with its own `GEMINI.md` schema, `sources/`, `pages/`, `log.md`) already shipped in this repo. Building in place keeps existing references valid, and the pre-existing page content is migrated into the new structure in the same change. |
| `ci/` and `deploy/` categories are dropped | The repository has no CI pipeline (no `.github/workflows`) and no deployment path. Categories that answer no real question are omitted rather than left empty. |
| Pre-existing wiki content absorbed | `wiki/pages/google_drive_auth_summary.md`, `wiki/pages/app_data_folder.md`, `wiki/pages/credential_manager.md`, and `wiki/sources/google_drive_auth_research_2026.md` are folded into `wiki/integrations/google-drive.md`. |
| Product docs sit outside the vault tree | PRD/PSD/tests docs are authored under `Docs/prd`, `Docs/psd`, `Docs/tests`, not as vault pages, because three files per feature share a basename and collide with the vault's wikilink-by-basename rule. They reuse the vault's citation/freshness rules (schema `Product docs` section) and are governed by the same lint. |

## Freshness

- `Last updated:` reflects the date the page was actually re-read and edited.
  Bumping it without re-reading launders a stale claim past the `REVIEW` check and
  is forbidden.
- The `REVIEW` lint finding (a cited source's last commit is newer than the page's
  `Last updated:`) is advisory: it means *re-read the source and confirm*, and it
  resolves only when the page is genuinely re-verified.

## Rate of change

The lint levels are structural or path facts, checked deterministically with the
standard library and `git` only; it never edits content. It does **not** check that
prose is true. A page can report zero findings and still assert something false in a
sentence. That is the system's real ceiling and it is intended.

## Sources

- `wiki/lint.sh` — the deterministic checker that enforces the structural rules.
- `wiki/meta/schema.md` — the operational schema that tells an author how to file a
  page in this vault.
- `Docs/index.md` — the feature catalog that the docs completeness check reads.

Back to [[overview]]
