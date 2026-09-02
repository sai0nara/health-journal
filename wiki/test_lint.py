#!/usr/bin/env python3
"""
Self-tests for wiki/lint.py. No test framework required.

Run:  python3 wiki/test_lint.py
Exit 0 => all pass; non-zero => failures printed.
"""

from __future__ import annotations

import datetime
import os
import subprocess
import sys
import tempfile

from lint import (
    parse_citations,
    run_lint,
)
from lint import _last_updated

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


# ---- citation parser (pure, no git) -----------------------------------------

def test_parser():
    src = (
        "Last updated: 2026-01-01\n\n"
        "## Sources\n"
        "- `app/build.gradle.kts` — the module build file\n"
        "- `app/src/main/java/Foo.kt`\n"
        "- `wiki/glob/**` — a glob\n"
        "- `https://example.com/x` — outward url (skipped)\n"
        "- `com.example.Service/a` — namespaced (skipped)\n"
        "- `user@host/path` — ssh-ish (skipped)\n"
        "- prose with a url `https://ex.com/z` — url is skipped\n"
    )
    got = parse_citations(src)
    check(
        "parser: em-dash + one path per bullet + plausible filters",
        got == [
            "app/build.gradle.kts",
            "app/src/main/java/Foo.kt",
            "wiki/glob/**",
        ],
    )

    src2 = (
        "## Sources\n"
        "- `a.kt` - hyphen dash variant\n"
    )
    check("parser: hyphen dash variant", parse_citations(src2) == ["a.kt"])

    src3 = "## Not Sources\n- `ignored.kt`\n"
    check("parser: only ## Sources section", parse_citations(src3) == [])


# ---- git fixtures -----------------------------------------------------------

def _sh(cwd, *args):
    subprocess.run(args, cwd=cwd, check=True, capture_output=True)


def _write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def _make_repo(pages, commit: bool):
    root = tempfile.mkdtemp(prefix="wikitest-")
    for rel, text in pages.items():
        _write(os.path.join(root, rel), text)
    _sh(root, "git", "init", "-q")
    _sh(root, "git", "config", "user.email", "t@t")
    _sh(root, "git", "config", "user.name", "t")
    if commit:
        _sh(root, "git", "add", ".")
        _sh(root, "git", "commit", "-q", "-m", "init")
    return root


# Bootstrap: a fixture that is structurally sound so specific checks stand alone.
def _base_pages():
    return {
        "README.md": "# fixture\n",
        "wiki/index.md": (
            "# Index\n\n"
            "Last updated: 2026-01-01\n\n"
            "## Pages\n"
            "- [[overview]]\n"
            "- [[alpha]]\n"
            "- [[beta]]\n"
            "- [[gamma]]\n"
        ),
        "wiki/service/overview.md": (
            "# Overview\n\nLast updated: 2026-01-01\n\n"
            "## Body\nSee [[alpha]] and [[beta]] and [[gamma]].\n\n"
            "## Sources\n- `README.md` — the readme\n"
        ),
        "wiki/modules/alpha.md": (
            "# Alpha\n\nLast updated: 2026-01-01\n\n"
            "## X\nrefers to [[beta]]\n\n"
            "## Sources\n- `README.md` — the readme\n\n"
            "Back to [[overview]]\n"
        ),
        "wiki/modules/beta.md": (
            "# Beta\n\nLast updated: 2026-01-01\n\n"
            "## X\nrefers to [[alpha]]\n\n"
            "## Sources\n- `README.md` — the readme\n\n"
            "Back to [[overview]]\n"
        ),
        "wiki/modules/gamma.md": (
            "# Gamma\n\nLast updated: 2026-01-01\n\n"
            "## X\n\n## Sources\n- `README.md` — the readme\n\n"
            "Back to [[overview]]\n"
        ),
    }


def _kinds(findings):
    return sorted({k for k, _rel, _m in findings})


def test_golden():
    root = _make_repo(_base_pages(), commit=True)
    kinds = _kinds(run_lint(root))
    check("golden fixture: no failing/advisory findings",
          kinds == [] or kinds == ["REVIEW"])


def test_index_not_inbound_source():
    pages = _base_pages()
    # gamma is referenced only from the catalog (index), never from any other page.
    pages["wiki/index.md"] = (
        "# Index\n\nLast updated: 2026-01-01\n\n## Pages\n"
        "- [[overview]]\n- [[alpha]]\n- [[beta]]\n- [[gamma]]\n"
    )
    pages["wiki/service/overview.md"] = (
        "# Overview\n\nLast updated: 2026-01-01\n\n"
        "## Body\nSee [[alpha]] and [[beta]].\n\n"
        "## Sources\n- `README.md` — the readme\n"
    )
    root = _make_repo(pages, commit=True)
    findings = [f for f in run_lint(root) if f[0] == "ORPHAN"]
    orphan_gamma = any(f[1].endswith("gamma.md") for f in findings)
    check("ORPHAN fires when a page is linked only from the catalog", orphan_gamma)


def test_root_needs_no_footer():
    pages = _base_pages()
    # overview already has no footer by construction (base fixture)
    root = _make_repo(pages, commit=True)
    footers = [f for f in run_lint(root) if f[0] == "FOOTER"]
    check("root page exempt from FOOTER",
          not any(f[1].endswith("overview.md") for f in footers))


def test_index_needs_no_footer():
    pages = _base_pages()
    root = _make_repo(pages, commit=True)
    footers = [f for f in run_lint(root) if f[0] == "FOOTER"]
    check("catalog (index) exempt from FOOTER",
          not any(f[1].endswith("index.md") for f in footers))


def test_meta_exempt_from_structure_but_not_freshness():
    pages = _base_pages()
    # meta page: no footer, wikilink to nowhere, HAS Last updated.
    pages["wiki/meta/doomed.md"] = (
        "# Doomed\n\nLast updated: 2099-01-01\n\n"
        "## Body\nlink to [[nowhere]]\n\n"
        "## Sources\n- `README.md` — the readme\n"
    )
    root = _make_repo(pages, commit=True)
    findings = run_lint(root)
    doomed = [f for f in findings if f[1].endswith("doomed.md")]
    check("meta exempt from BROKEN/FOOTER/ORPHAN",
          all(f[0] not in ("BROKEN", "FOOTER", "ORPHAN") for f in doomed))


def test_review_fires_for_meta():
    # meta is exempt from structure, NOT from freshness: a meta page whose cited
    # source is committed AFTER its Last updated must report REVIEW.
    pages = _base_pages()
    root = _make_repo(pages, commit=True)  # first commit has all fixtures
    # advance a cited source after the fixture pages were "authored"
    with open(os.path.join(root, "README.md"), "a") as f:
        f.write("\n# later change\n")
    _sh(root, "git", "add", ".")
    _sh(root, "git", "commit", "-q", "-m", "later")

    pages["wiki/meta/doomed.md"] = (
        "# Doomed\n\nLast updated: 2026-01-01\n\n"
        "## Body\nlink to [[nowhere]]\n\n"
        "## Sources\n- `README.md` — the readme\n"
    )
    # commit the meta page now (its date 2026-01-01 is BEFORE README's latest)
    _write(os.path.join(root, "wiki/meta/doomed.md"),
           pages["wiki/meta/doomed.md"])
    _sh(root, "git", "add", ".")
    _sh(root, "git", "commit", "-q", "-m", "add meta")
    findings = run_lint(root)
    review_meta = any(f[0] == "REVIEW" and f[1].endswith("doomed.md")
                      for f in findings)
    check("meta not exempt from freshness: REVIEW fires for a meta page",
          review_meta)


def test_duplicate_reported():
    pages = _base_pages()
    pages["wiki/integrations/alpha.md"] = (
        "# Alpha dup\n\nLast updated: 2026-01-01\n\n## X\n\n"
        "## Sources\n- `README.md` — the readme\n\nBack to [[overview]]\n"
    )
    root = _make_repo(pages, commit=True)
    dups = [f for f in run_lint(root) if f[0] == "DUPLICATE"]
    check("DUPLICATE reported for shared basename", any(f[1] for f in dups))


def test_stale_and_broken_and_untracked():
    pages = _base_pages()
    pages["wiki/modules/alpha.md"] = (
        "# Alpha\n\nLast updated: 2026-01-01\n\n## X\nlinks to [[missing-page]]\n\n"
        "## Sources\n- `README.md` — the readme\n- `does/not/exist.kt` — missing\n"
        "Back to [[overview]]\n"
    )
    root = _make_repo(pages, commit=True)
    findings = run_lint(root)
    check("BROKEN fires", any(f[0] == "BROKEN" for f in findings))
    check("STALE fires", any(f[0] == "STALE" for f in findings))

    # untracked: write an untracked file and cite it
    pages2 = dict(pages)
    pages2["wiki/modules/beta.md"] = (
        "# Beta\n\nLast updated: 2026-01-01\n\n## X\n\n"
        "## Sources\n- `fresh.kt` — untracked\n\nBack to [[overview]]\n"
    )
    root2 = _make_repo(pages2, commit=False)  # nothing committed -> all untracked
    _write(os.path.join(root2, "fresh.kt"), "x")
    findings2 = run_lint(root2)
    check("UNTRACKED fires for uncommitted cited path", any(f[0] == "UNTRACKED" for f in findings2))


def test_last_updated_parser():
    check("Last updated parser",
          _last_updated("xx\nLast updated: 2026-09-01\n") == "2026-09-01")
    check("Last updated parser missing", _last_updated("xx\n\n") is None)


# ---- product docs (Docs/prd | psd | tests) ----------------------------------

_TODAY = datetime.date.today().isoformat()


def _doc(text: str) -> str:
    return (
        f"# Doc\n\nLast updated: {_TODAY}\n\n"
        "## Body\n\n"
        + text
        + "\n\n## Sources\n- `README.md` — the readme\n"
    )


def _docs_pages(claimed=True):
    pages = {
        "README.md": "# fixture\n",
        "Docs/index.md": (
            "# Docs\n\n"
            f"Last updated: {_TODAY}\n\n"
            "## Features\n"
            "- `Docs/prd/restore.md` — the PRD\n"
            "- `Docs/psd/restore.md` — the PSD\n"
            f"- `Docs/tests/restore.md` — the tests\n"
        ),
    }
    if claimed:
        pages["Docs/prd/restore.md"] = _doc("PRD body with project context `Docs/psd/restore.md`.")
        pages["Docs/psd/restore.md"] = _doc("PSD body.")
        pages["Docs/tests/restore.md"] = _doc("Tests body.")
    return pages


def test_docs_golden():
    root = _make_repo(_docs_pages(), commit=True)
    kinds = _kinds(run_lint(root))
    check("docs golden: no STAMP/MISSING/STALE", not (set(kinds) & {"STAMP", "MISSING", "STALE"}))


def test_docs_stamp_fires_without_last_updated():
    pages = _docs_pages()
    pages["Docs/tests/restore.md"] = (
        "# Tests\n\n## Body\n\n## Sources\n- `README.md` — the readme\n"
    )
    root = _make_repo(pages, commit=True)
    stamps = [f for f in run_lint(root) if f[0] == "STAMP"]
    check("STAMP fires for a doc without Last updated",
          any(f[1].endswith("tests/restore.md") for f in stamps))


def test_docs_missing_fires_for_unshipped_doc():
    pages = _docs_pages(claimed=False)
    # claimed via index but the tests doc is never written
    pages["Docs/index.md"] = (
        "# Docs\n\n"
        f"Last updated: {_TODAY}\n\n"
        "## Features\n"
        "- `Docs/prd/restore.md` — the PRD\n"
        "- `Docs/psd/restore.md` — the PSD\n"
        "- `Docs/tests/restore.md` — the tests\n"
    )
    pages["Docs/prd/restore.md"] = _doc("PRD body.")
    pages["Docs/psd/restore.md"] = _doc("PSD body.")
    root = _make_repo(pages, commit=True)
    missing = [f for f in run_lint(root) if f[0] == "MISSING"]
    check("MISSING fires for a claimed-but-absent doc",
          any("tests/restore.md" in f[2] for f in missing))


def test_docs_stale_and_index_advisory():
    pages = _docs_pages(claimed=False)
    # catalog claims a different feature; the restore docs go unclaimed
    pages["Docs/index.md"] = (
        "# Docs\n\n"
        f"Last updated: {_TODAY}\n\n"
        "## Features\n"
        "- `Docs/prd/export.md` — the PRD\n"
        "- `Docs/psd/export.md` — the PSD\n"
        "- `Docs/tests/export.md` — the tests\n"
    )
    pages["Docs/prd/restore.md"] = _doc("PRD body.")
    pages["Docs/psd/restore.md"] = (
        "# PSD\n\nLast updated: 2026-01-01\n\n## Body\n\n"
        "## Sources\n- `README.md` — the readme\n- `does/not/exist.kt` — missing\n"
    )
    root = _make_repo(pages, commit=True)
    findings = run_lint(root)
    check("STALE fires for a doc citing a dead path",
          any(f[0] == "STALE" for f in findings))
    check("INDEX advisory fires for a doc absent from the catalog",
          any(f[0] == "INDEX" and f[1].endswith("restore.md") for f in findings))


def test_docs_planned_feature_exempt_from_missing():
    pages = _docs_pages(claimed=False)
    # ai-insights is claimed but marked planned: only its PRD is required
    pages["Docs/index.md"] = (
        "# Docs\n\n"
        f"Last updated: {_TODAY}\n\n"
        "## Features\n"
        "- `Docs/prd/ai-insights.md` — planned\n"
    )
    pages["Docs/prd/ai-insights.md"] = _doc("Planned PRD body.")
    root = _make_repo(pages, commit=True)
    missing = [f for f in run_lint(root) if f[0] == "MISSING"]
    check("MISSING does not fire for a planned feature lacking psd/tests", not missing)


def test_docs_planned_feature_still_needs_prd():
    pages = _docs_pages(claimed=False)
    pages["Docs/index.md"] = (
        "# Docs\n\n"
        f"Last updated: {_TODAY}\n\n"
        "## Features\n"
        "- `Docs/prd/ai-insights.md` — planned\n"
    )
    # index claims the PRD but no PRD file exists
    root = _make_repo(pages, commit=True)
    missing = [f for f in run_lint(root) if f[0] == "MISSING"]
    check("MISSING fires for a planned feature without its PRD",
          any("ai-insights" in f[2] and "prd" in f[2] for f in missing))


def main():
    test_parser()
    test_golden()
    test_index_not_inbound_source()
    test_root_needs_no_footer()
    test_index_needs_no_footer()
    test_meta_exempt_from_structure_but_not_freshness()
    test_review_fires_for_meta()
    test_duplicate_reported()
    test_stale_and_broken_and_untracked()
    test_last_updated_parser()
    test_docs_golden()
    test_docs_stamp_fires_without_last_updated()
    test_docs_missing_fires_for_unshipped_doc()
    test_docs_stale_and_index_advisory()
    test_docs_planned_feature_exempt_from_missing()
    test_docs_planned_feature_still_needs_prd()

    print(f"\n{PASS} passed, {FAIL} failed")
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
