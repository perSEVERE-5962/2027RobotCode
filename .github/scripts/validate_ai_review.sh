#!/usr/bin/env bash
set -euo pipefail

# Basic sanity checks for AI review workflows — keep lightweight for CI.
# Resolve repository root (script is in .github/scripts)
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Ensure reusable workflow exists
# Ensure reusable workflow exists
if [ ! -f "$REPO_ROOT/.github/workflows/run-ai-review.yml" ]; then
  echo "ERROR: run-ai-review.yml missing" >&2
  exit 2
fi

# Ensure main workflow references the reusable workflow
if ! grep -q "run-ai-review.yml" "$REPO_ROOT/.github/workflows/ai-review.yml"; then
  echo "WARNING: ai-review.yml does not reference run-ai-review.yml" >&2
fi

# All good
echo "validate_ai_review: basic checks passed"
exit 0
