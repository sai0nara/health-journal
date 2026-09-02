#!/usr/bin/env python3
"""
Hook B - report vault health.

Runs the wiki lint and surfaces its findings. Reuses lint.run_lint so it can never
disagree with the checker. Exit code mirrors the lint (0 if no failing findings).

Never edits content.
"""

from __future__ import annotations

import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(_HERE))  # make 'lint' importable

from lint import run_lint, _report  # reuse the lint, including its reporting


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    root = os.environ.get("WIKI_ROOT") or (argv[0] if argv else os.getcwd())
    if os.path.dirname(root) and not os.path.isabs(root):
        root = os.path.abspath(root)
    findings = run_lint(root)
    code = _report(root, findings)
    print(f"[wiki-health] {len(findings)} finding(s); exit={code}")
    return code


if __name__ == "__main__":
    sys.exit(main())
