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

# Ensure no job-level COPILOT_GITHUB_TOKEN env is used; it must be step-scoped.
python3 - "$WF" <<'PY'
import sys
from pathlib import Path

wf = Path(sys.argv[1])
text = wf.read_text(encoding='utf-8')
lines = text.splitlines()

# We only reject job-level env declarations, not step-level env blocks.
# A job-level entry is a YAML key under a job, while step-level entries are nested under a step.
job_env = False
step_indent = None
for idx, line in enumerate(lines):
    stripped = line.strip()
    if stripped.startswith('env:') and not stripped.startswith('- env:'):
        indent = len(line) - len(line.lstrip(' '))
        if step_indent is None:
            if indent <= 6:
                job_env = True
        else:
            if indent <= step_indent + 2:
                job_env = True
    if stripped.startswith('COPILOT_GITHUB_TOKEN:'):
        if job_env:
            raise SystemExit("Job-level COPILOT_GITHUB_TOKEN is present; scope it to a step env block.")
        # If it is configured under a step, the preceding env block is valid.
        prev = lines[idx - 1].strip() if idx > 0 else ''
        if prev == 'env:':
            continue
        # Fallback guard: do not allow bare token usage outside a step env block.
        if not any((line.strip() == 'env:') for line in lines[max(0, idx - 8):idx]):
            raise SystemExit("COPILOT_GITHUB_TOKEN is not clearly scoped to a step env block.")

    if stripped.startswith('- name:'):
        step_indent = len(line) - len(line.lstrip(' '))
    elif stripped and not stripped.startswith('#') and not line.startswith(' '):
        # Left a step scope; no step env is active for subsequent job-level checks.
        step_indent = None
PY

# Check markdown divider spacing (echo "---" should have blank echo lines around it)
python3 - "$WF" <<'PY'
import sys
from pathlib import Path

lines = Path(sys.argv[1]).read_text(encoding='utf-8').splitlines()
for i, line in enumerate(lines):
    if 'echo "---"' in line:
        prev = lines[i - 1].strip() if i > 0 else ''
        nxt = lines[i + 1].strip() if i + 1 < len(lines) else ''
        if prev not in ('echo', '') or nxt not in ('echo', ''):
            raise SystemExit("Missing blank echo separators around '---' in review output formatting.")
PY

echo "Validation checks passed for ${WF}"
