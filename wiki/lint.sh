#!/usr/bin/env bash
# Deterministic structural + path checker for the LLM Wiki vault.
# Standard library + git only. Never edits content.
# Usage: wiki/lint.sh   (run from the repository root)
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$here/lint.py" --root "$(cd "$here/.." && pwd)"
