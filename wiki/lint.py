#!/usr/bin/env python3
"""
Wiki lint: deterministic structural + path checks for the LLM Wiki vault.

Standard library + `git` only. Never edits content.

Exit status:
    0  no FAILING findings (advisory findings may be present)
    1  at least one failing finding

Findings (see meta/spec.md):
    STALE      cited path no longer exists (or glob matches nothing)       FAIL
    UNTRACKED  cited path exists locally but is not committed              FAIL
    BROKEN     a wikilink points at a page that does not exist             FAIL
    DUPLICATE  two pages share a basename, so one is unreachable           FAIL
    STAMP      a product doc (Docs/prd|psd|tests) has no Last updated      FAIL
    MISSING    a Docs/ feature in the catalog lacks one of prd/psd/tests   FAIL
    REVIEW     a cited source's last commit is newer than 'Last updated:'  advisory
    ORPHAN     nothing links to the page (root page exempt)                advisory
    INDEX      page missing from the catalog, or catalogued but gone       advisory
    FOOTER     page missing its back-navigation footer (root+index exempt) advisory

The lint checks the wiki vault (wiki/) and the product docs (Docs/prd|psd|tests).
Product docs follow the same citation and freshness rules as vault pages; they
carry no footer/wikilink navigation, so BROKEN/ORPHAN/FOOTER do not apply to
them. Features are claimed in Docs/index.md; a claimed feature must ship all
three documents (prd + psd + tests) or MISSING fails.

Usage:
    python3 wiki/lint.py [--root DIR] [--exit0]
    wiki/lint.sh

The citation parser (parse_citations) is the single source of truth for what counts
as a citation; the end-of-turn hook imports it rather than reimplementing it.
"""

from __future__ import annotations

import fnmatch
import glob
import os
import re
import subprocess
import sys

EM_DASH = "\u2014"
HASH_DASH = "-"

# ---- citation parser (importable, single source of truth) -----------------

_SOURCES_RE = re.compile(r"^##\s+Sources\s*$")
_BULLET_RE = re.compile(r"^\s*[-*]\s+(.*)$")
_BACKTICK_RE = re.compile(r"`([^`]+)`")
_URL_SCHEME_RE = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*://")
_DOTTED_HOST_FIRST_SEG = re.compile(r"^[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+$")


def _plausible_path(token: str) -> bool:
    """Return True if a backticked token is plausibly a local path."""
    if _URL_SCHEME_RE.match(token):
        return False
    if "@" in token:
        return False
    if "/" in token:
        first = token.split("/", 1)[0]
        if _DOTTED_HOST_FIRST_SEG.match(first):
            # namespaced key / package path, not a repo-relative path
            return False
    return True


def parse_citations(markdown: str):
    """Return the list of cited paths/globs from a page's `## Sources` section.

    Only the text before the em-dash (or hyphen) of each `## Sources` bullet is
    parsed; everything after the dash is prose and ignored. The first backticked
    token before the dash is the citation (one path per bullet). Fenced code blocks
    are skipped so example link/citation syntax is never treated as real citations.
    """
    citations = []
    in_sources = False
    fence = None
    for raw in markdown.splitlines():
        if raw.lstrip().startswith(("```", "~~~")):
            if fence is None:
                fence = raw.strip()[:3]
            else:
                fence = None
            continue
        if fence is not None:
            continue
        if _SOURCES_RE.match(raw.strip()):
            in_sources = True
            continue
        if in_sources and raw.strip().startswith("## "):
            in_sources = False
            continue
        if not in_sources:
            continue
        m = _BULLET_RE.match(raw)
        if not m:
            continue
        body = m.group(1)
        # text before the first dash
        before = body
        for dash in (f" {EM_DASH} ", f" {HASH_DASH} "):
            idx = body.find(dash)
            if idx != -1:
                before = body[:idx]
                break
        for tk in _BACKTICK_RE.findall(before):
            if _plausible_path(tk):
                citations.append(tk)
                break  # one path per bullet
    return citations


# ---- git helpers -----------------------------------------------------------

def _git(root: str, *args: str) -> str:
    return subprocess.run(
        ["git", "-C", root, *args],
        capture_output=True, text=True, check=True,
    ).stdout


def _git_ok(root: str, *args: str) -> bool:
    r = subprocess.run(
        ["git", "-C", root, *args],
        capture_output=True, text=True,
    )
    return r.returncode == 0


def is_tracked(root: str, rel_path: str) -> bool:
    """True if rel_path is tracked by git (a dir counts if it holds a tracked file)."""
    if os.path.isdir(os.path.join(root, rel_path)):
        return _git_ok(root, "ls-files", "--error-unmatch", rel_path + "/")
    return _git_ok(root, "ls-files", "--error-unmatch", rel_path)


def last_commit_date(root: str, rel_path: str) -> str | None:
    """RFC3339-ish date of the last commit touching rel_path, or None."""
    try:
        out = _git(root, "log", "-1", "--format=%ad", "--date=short", "--",
                   rel_path).strip()
    except subprocess.CalledProcessError:
        return None
    return out or None


# ---- vault discovery -------------------------------------------------------

class Choice:
    """Categorise an md file under the vault (or the product docs)."""

    def __init__(self, root: str, path: str):
        self.root = root
        self.abs = path
        self.rel = os.path.relpath(path, root)
        rel = self.rel.replace(os.sep, "/")
        self.basename = os.path.splitext(os.path.basename(path))[0]
        self.is_sources = "/sources/" in rel
        self.is_meta = "/meta/" in rel
        tail = rel[len("wiki/"):] if rel.startswith("wiki/") else rel
        self.is_index = tail == "index.md"
        self.is_root = tail == "service/overview.md"
        self.is_docs = self._docs_type(rel) is not None
        self.docs_type = self._docs_type(rel)

    @staticmethod
    def _docs_type(rel: str) -> str | None:
        for t in DOCS_DIRS:
            if rel.startswith(f"Docs/{t}/"):
                return t
        return None


def walk_pages(root: str, vault: str = "wiki"):
    pages = []
    for dirpath, _dirnames, filenames in os.walk(os.path.join(root, vault)):
        for fn in filenames:
            if fn.endswith(".md"):
                pages.append(Choice(root, os.path.join(dirpath, fn)))
    return pages


DOCS_DIRS = ("prd", "psd", "tests")


def walk_docs(root: str):
    """Product-doc pages under Docs/prd|psd|tests."""
    pages = []
    for t in DOCS_DIRS:
        base = os.path.join(root, "Docs", t)
        if not os.path.isdir(base):
            continue
        for dirpath, _dirnames, filenames in os.walk(base):
            for fn in filenames:
                if fn.endswith(".md"):
                    pages.append(Choice(root, os.path.join(dirpath, fn)))
    return sorted(pages, key=lambda p: p.rel)


def docs_slugs(root: str):
    """Return (all_slugs, planned_slugs) claimed in Docs/index.md.

    A feature is claimed by listing any of its `Docs/<type>/<slug>.md` paths
    (backticked) in the catalog. A feature is *planned* (not yet built) when the
    claimed line also carries the marker `— planned`; a planned feature ships
    its PRD up front and is exempt from the psd/tests requirement.
    """
    all_slugs = set()
    planned = set()
    path = os.path.join(root, "Docs", "index.md")
    if not os.path.exists(path):
        return all_slugs, planned
    for line in _read(path).splitlines():
        m = re.match(r"^-\s+`Docs/(%s)/([^/]+?)\.md`\s*—\s*(.*)$" % "|".join(DOCS_DIRS), line)
        if not m:
            continue
        feature_type, slug, rest = m.group(1), m.group(2), m.group(3)
        all_slugs.add(slug)
        if "planned" in rest.lower():
            planned.add(slug)
    return all_slugs, planned


def _basename_map(pages):
    m = {}
    for p in pages:
        m.setdefault(p.basename, []).append(p)
    return m


def _wikilinks(markdown: str):
    return re.findall(r"\[\[([^\]|]+)(?:\|[^\]]+)?\]\]", markdown)


# ---- the lint --------------------------------------------------------------

def run_lint(root: str):
    root = os.path.abspath(root)
    pages = walk_pages(root)
    all_md = [p for p in pages]
    content = [p for p in pages if not p.is_meta and not p.is_index and not p.is_sources]
    nonmeta = [p for p in pages if not p.is_meta and not p.is_sources]
    findings = []  # (kind, page_rel, message)

    valid_basenames = set(_basename_map(pages).keys())
    content_basenames = set(p.basename for p in content)

    # DUPLICATE
    for base, ps in _basename_map(pages).items():
        if len(ps) > 1:
            for p in ps[1:]:
                findings.append(("DUPLICATE", p.rel, f"basename '{base}' shared with {ps[0].rel}"))

    # build inbound link counts, excluding index and meta as sources.
    # wikilink targets are basenames, so key the map by basename.
    inbound = {p.basename: 0 for p in content}
    for p in nonmeta:
        if p.is_index or p.is_meta:
            continue
        text = _read(p.abs)
        for target in _wikilinks(text):
            if target in inbound:
                inbound[target] += 1
            # self-links are not inbound for orphan purposes
    # remove self-links contributed above (a page referencing itself)
    for p in content:
        inbound[p.basename] = max(0, inbound.get(p.basename, 0))

    for p in pages:
        rel = p.rel
        if p.is_sources:
            continue
        text = _read(p.abs)
        date = _last_updated(text)
        links = _wikilinks(text)

        # BROKEN (meta exempt)
        if not p.is_meta:
            for target in links:
                if target not in valid_basenames:
                    findings.append(("BROKEN", rel, f"wikilink [[{target}]] resolves to no page"))

        # FOOTER (root + index + meta exempt)
        if not p.is_root and not p.is_index and not p.is_meta:
            if not re.search(r"Back to \[\[[^\]]+\]\]", text):
                findings.append(("FOOTER", rel, "missing back-navigation footer"))

        # INDEX (content pages must be catalogued; meta referenced by backtick)
        if not p.is_index and not p.is_meta and not p.is_sources:
            index = _read(os.path.join(root, "wiki", "index.md"))
            if f"[[{p.basename}]]" not in index:
                findings.append(("INDEX", rel, "not listed in the catalog"))

        # ORPHAN (content pages, root exempt)
        if p in content and not p.is_root:
            if inbound.get(p.basename, 0) == 0:
                findings.append(("ORPHAN", rel, "no inbound links from other pages"))

        # STALE / UNTRACKED / REVIEW from citations
        for cite in parse_citations(text):
            status = _check_citation(root, cite)
            if status == "stale":
                findings.append(("STALE", rel, f"cited path '{cite}' does not exist"))
            elif status == "untracked":
                findings.append(("UNTRACKED", rel, f"cited path '{cite}' exists but is not committed"))
            elif status == "ok" and date and not _is_glob(cite):
                src_date = last_commit_date(root, cite)
                if src_date and src_date > date:
                    findings.append(("REVIEW", rel, f"source '{cite}' changed ({src_date}) after page date ({date})"))

    # catalogued-but-gone (INDEX reverse)
    index_path = os.path.join(root, "wiki", "index.md")
    if os.path.exists(index_path):
        index_text = _read(index_path)
        for target in _wikilinks(index_text):
            if target not in content_basenames and target not in valid_basenames:
                findings.append(("INDEX", "wiki/index.md", f"catalogued [[{target}]] but no such page"))

    # ---- product docs (Docs/prd | psd | tests) ------------------------------
    docs = walk_docs(root)
    indexed_slugs, planned_slugs = docs_slugs(root)

    for p in docs:
        text = _read(p.abs)
        date = _last_updated(text)
        if date is None:
            findings.append(("STAMP", p.rel, "missing 'Last updated:' stamp"))
        for cite in parse_citations(text):
            status = _check_citation(root, cite)
            if status == "stale":
                findings.append(("STALE", p.rel, f"cited path '{cite}' does not exist"))
            elif status == "untracked":
                findings.append(("UNTRACKED", p.rel, f"cited path '{cite}' exists but is not committed"))
            elif status == "ok" and date and not _is_glob(cite):
                src_date = last_commit_date(root, cite)
                if src_date and src_date > date:
                    findings.append(("REVIEW", p.rel, f"source '{cite}' changed ({src_date}) after page date ({date})"))
        if p.basename not in indexed_slugs:
            findings.append(("INDEX", p.rel, f"feature '{p.basename}' not listed in the docs catalog"))

    # completeness: a feature claimed in Docs/index.md must ship the PRD, and
    # (unless marked planned) the psd + tests too
    if os.path.exists(os.path.join(root, "Docs", "index.md")):
        for slug in sorted(indexed_slugs):
            required = ("prd",) if slug in planned_slugs else DOCS_DIRS
            for t in required:
                fp = os.path.join("Docs", t, f"{slug}.md")
                if not os.path.exists(os.path.join(root, fp)):
                    findings.append(("MISSING", "Docs/index.md", f"feature '{slug}' lacks '{fp}'"))

    return findings


def _last_updated(text: str) -> str | None:
    m = re.search(r"^Last updated:\s*(\d{4}-\d{2}-\d{2})\s*$", text, re.MULTILINE)
    return m.group(1) if m else None


def _is_glob(cite: str) -> bool:
    return any(ch in cite for ch in "*?[")


def _check_citation(root: str, cite: str) -> str:
    """One of 'ok', 'stale', or 'untracked'.

    A glob resolves if at least one file matches. Tracked-ness is tested against
    git, not the filesystem.
    """
    if _is_glob(cite):
        matches = glob.glob(os.path.join(root, cite))
        return "stale" if not matches else "ok"

    abs_path = os.path.join(root, cite)
    if not os.path.exists(abs_path):
        return "stale"
    if not is_tracked(root, cite):
        return "untracked"
    return "ok"


def _read(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


# ---- CLI -------------------------------------------------------------------

def _report(root: str, findings) -> int:
    order = {"STALE": 0, "UNTRACKED": 1, "BROKEN": 2, "DUPLICATE": 3,
             "STAMP": 4, "MISSING": 5,
             "REVIEW": 6, "ORPHAN": 7, "INDEX": 8, "FOOTER": 9}
    failing = {k: v for k, v in order.items() if v <= 5}
    grouped = {}
    for kind, rel, msg in findings:
        grouped.setdefault(kind, []).append((rel, msg))
    exit_code = 0
    for kind, _ in sorted(order.items(), key=lambda kv: kv[1]):
        for rel, msg in grouped.get(kind, []):
            exit_code = exit_code or (1 if kind in failing else 0)
            print(f"{kind:9s} {rel}: {msg}")
    return exit_code


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    root = "."
    if "--root" in argv:
        i = argv.index("--root")
        root = argv[i + 1]
    findings = run_lint(root)
    return _report(root, findings)


if __name__ == "__main__":
    sys.exit(main())
