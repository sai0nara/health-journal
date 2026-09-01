#!/usr/bin/env python3
"""
Hook A - name the pages a change invalidates.

Builds a page -> cited-paths map using the lint's OWN citation parser, intersects it
with the paths changed this turn (tracked modifications + new untracked files), and
emits the affected page names with the matching citation plus the scoped repair
instruction.

Rules:
  * Stays silent when ONLY vault files changed (the agent is already editing the wiki).
  * ALWAYS exits 0 - a failing end-of-turn hook would interfere with the turn.
  * Standard library + git only. Never edits content.

Env overrides (for testing against a fixture):
  WIKI_ROOT            repo root to operate on (default: paint cwd's git root)
  WIKI_CHANGED_FILES   newline-separated explicit changed-path list; when set, no
                       git diff is computed (pure fixture mode)

Changed paths derive from a plain name-only diff (tracked modifications) PLUS a
separate listing of untracked files - never by stripping status prefixes from
porcelain output (renames emit two records and would corrupt the second filename).
"""

from __future__ import annotations

import os
import subprocess
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(_HERE))  # make 'lint' importable

from lint import parse_citations, walk_pages  # reuse the lint's parser

AFFECTED_INSTRUCTION = (
    "This turn changed code that {} page(s) cite. You own the vault. For each page: "
    "re-read the changed source and the page, then edit only claims that are now false "
    "or incomplete. Bump `Last updated:` only on pages you actually edited - bumping "
    "without re-reading launders a stale claim past the freshness check. If a change "
    "does not affect any claim (a rename, formatting, a test tweak), leave the page "
    "alone and say so. Stage wiki edits alongside the code, then confirm the lint "
    "still exits 0 (python3 wiki/lint.py)."
)

VAULT_PREFIXES = ("wiki/",)


def git_root(root: str) -> str:
    r = subprocess.run(["git", "-C", root, "rev-parse", "--show-toplevel"],
                       capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else root


def _git(root: str, *args: str) -> list[str]:
    return subprocess.run(["git", "-C", root, *args],
                          capture_output=True, text=True).stdout.splitlines()


def changed_paths(root: str) -> list[str]:
    """Tracked modifications via name-only diff + untracked files, listed separately."""
    tracked_mods = _git(root, "diff", "--name-only", "HEAD")
    untracked = _git(root, "ls-files", "--others", "--exclude-standard")
    return tracked_mods + untracked


def is_vault_path(rel: str) -> bool:
    rel = rel.replace(os.sep, "/")
    return rel.startswith(VAULT_PREFIXES)


def affected_pages(root: str, changed: list[str]):
    """Return list of (page_rel, [cited paths that changed]) whose cited source changed."""
    changed_set = set(p.replace(os.sep, "/") for p in changed)
    hits = []
    for page in walk_pages(root):
        if page.is_sources:
            continue
        text = open(page.abs, "r", encoding="utf-8").read()
        cited = parse_citations(text)
        matched = [c for c in cited if c in changed_set]
        if matched:
            hits.append((page.rel, matched))
    return hits


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    root = os.environ.get("WIKI_ROOT") or (argv[0] if argv else os.getcwd())
    root = git_root(root)

    if "WIKI_CHANGED_FILES" in os.environ:
        changed = [l for l in os.environ["WIKI_CHANGED_FILES"].splitlines() if l.strip()]
    else:
        changed = changed_paths(root)

    only_vault = changed and all(is_vault_path(c) for c in changed)
    if only_vault:
        # the agent is already editing the wiki; stay silent
        return 0

    hits = affected_pages(root, changed)
    if not hits:
        return 0

    lines = [AFFECTED_INSTRUCTION.format(len(hits))]
    for page_rel, cited in hits:
        for c in cited:
            lines.append(f"  {page_rel}  (citation: {c})")
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
