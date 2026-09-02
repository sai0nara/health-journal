#!/usr/bin/env python3
"""
Self-tests for the end-of-turn hooks. No test framework required.

Run:  python3 wiki/hooks/test_hooks.py
"""

from __future__ import annotations

import io
import os
import subprocess
import sys
import tempfile
from contextlib import redirect_stdout

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _HERE)

from affected_pages import affected_pages, changed_paths, main as affected_main

PASS = 0
FAIL = 0


def check(name: str, cond: bool):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"ok   - {name}")
    else:
        FAIL += 1
        print(f"FAIL - {name}")


def _write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def _repo():
    root = tempfile.mkdtemp(prefix="hooktest-")
    _write(os.path.join(root, "foo.kt"), "x")
    _write(os.path.join(root, "wiki/index.md"),
           "# Index\n\nLast updated: 2026-01-01\n\n- [[overview]]\n- [[deep]]\n")
    _write(os.path.join(root, "wiki/service/overview.md"),
           "# Overview\n\nLast updated: 2026-01-01\n\n## Body\n\n"
           "## Sources\n- `foo.kt` — a cited source\n")
    _write(os.path.join(root, "wiki/modules/deep.md"),
           "# Deep\n\nLast updated: 2026-01-01\n\n## Body\n\n"
           "## Sources\n- `bar.kt` — another source\n")
    subprocess.run(["git", "init", "-q"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.email", "t@t"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.name", "t"], cwd=root, check=True)
    subprocess.run(["git", "add", "."], cwd=root, check=True)
    subprocess.run(["git", "commit", "-q", "-m", "init"], cwd=root, check=True)
    return root


def test_hook_a_names_affected():
    root = _repo()
    changed = ["foo.kt"]  # only overview cites foo.kt
    hits = affected_pages(root, changed)
    rels = [h[0] for h in hits]
    check("Hook A names the page that cites the changed file",
          any("overview" in r for r in rels) and not any("deep" in r for r in rels))


def test_hook_a_silent_when_only_vault_changes():
    root = _repo()
    buf = io.StringIO()
    os.environ["WIKI_ROOT"] = root
    os.environ["WIKI_CHANGED_FILES"] = "wiki/modules/deep.md"
    with redirect_stdout(buf):
        code = affected_main()
    check("Hook A silent (no output) when only vault files changed",
          code == 0 and buf.getvalue().strip() == "")
    del os.environ["WIKI_ROOT"]
    del os.environ["WIKI_CHANGED_FILES"]


def test_hook_a_exits_zero_always():
    root = _repo()
    os.environ["WIKI_ROOT"] = root
    os.environ["WIKI_CHANGED_FILES"] = "foo.kt"
    buf = io.StringIO()
    with redirect_stdout(buf):
        code = affected_main()
    check("Hook A always exits 0 and prints an instruction when affected",
          code == 0 and "You own the vault" in buf.getvalue())
    del os.environ["WIKI_ROOT"]
    del os.environ["WIKI_CHANGED_FILES"]


def test_changed_paths_handles_renames_without_corruption():
    root = _repo()
    subprocess.run(["git", "mv", "foo.kt", "renamed.kt"], cwd=root, check=True)
    _write(os.path.join(root, "brand-new.kt"), "y")
    paths = changed_paths(root)
    check("changed_paths: no status-prefix stripping corruption (rename handled cleanly)",
          "renamed.kt" in paths and "brand-new.kt" in paths)


def test_hook_b_reports_health():
    root = _repo()
    os.environ["WIKI_ROOT"] = root
    from report_health import main as health_main
    buf = io.StringIO()
    with redirect_stdout(buf):
        code = health_main()
    check("Hook B runs the lint and reports vault health",
          "[wiki-health]" in buf.getvalue() and isinstance(code, int))
    del os.environ["WIKI_ROOT"]


def main():
    test_hook_a_names_affected()
    test_hook_a_silent_when_only_vault_changes()
    test_hook_a_exits_zero_always()
    test_changed_paths_handles_renames_without_corruption()
    test_hook_b_reports_health()

    print(f"\n{PASS} passed, {FAIL} failed")
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())