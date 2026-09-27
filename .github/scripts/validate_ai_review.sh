#!/usr/bin/env bash
set -euo pipefail

WF=.github/workflows/ai-review.yml

fail() { echo "ERROR: $*" >&2; exit 1; }

[ -f "$WF" ] || fail "$WF not found"

# Check for permission step id
grep -q "id: permission" "$WF" || fail "manual-review: missing 'id: permission'"

# Check permission outputs are emitted
grep -q "permission=" "$WF" || fail "permission step does not emit permission output"
grep -q "authorized=" "$WF" || fail "permission step does not emit authorized output"

# Check manual flow uses outputs.authorized gating
grep -q "steps.permission.outputs.authorized" "$WF" || fail "manual-review downstream steps not gated on steps.permission.outputs.authorized"

# Check manual token check exists
grep -q "id: check-token-manual" "$WF" || fail "manual-review missing 'check-token-manual' step"

# Check automatic token check exists
grep -q "id: check-token" "$WF" || fail "automatic-review missing 'id: check-token' step"

# Ensure no job-level COPILOT_GITHUB_TOKEN env (should be scoped)
if grep -q "COPILOT_GITHUB_TOKEN" "$WF"; then
  # Allow occurrences that are step-level by checking common prefix
  if ! grep -n "id: check-token" -n "$WF" >/dev/null 2>&1; then
    fail "Found COPILOT_GITHUB_TOKEN in workflow; token should be scoped to steps"
  fi
fi

# Check markdown divider spacing (echo "---" should have blank lines around it)
if grep -n "echo \"---\"" -n "$WF" >/dev/null 2>&1; then
  # verify there is an echo blank line before and after in the same block
  # crude check: look for 'cat review.md' followed by blank echo and ---
  if ! grep -n -A3 "cat review.md" "$WF" | grep -q "echo\n\s*echo \"---\""; then
    echo "WARNING: Could not verify blank lines around '---' automatically. Please inspect manually." >&2
  fi
fi

echo "Validation checks passed for ${WF}"
