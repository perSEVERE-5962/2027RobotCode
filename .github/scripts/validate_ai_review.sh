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
# A job-level env sits directly under a job (e.g. "    env:"), while a step-level
# env sits inside a step body (e.g. "        env:" under "- name:").
job_indent = None
step_indent = None
job_env = False

for idx, line in enumerate(lines):
    stripped = line.strip()
    if not stripped or stripped.startswith('#'):
        continue

    indent = len(line) - len(line.lstrip(' '))

    if stripped.startswith('jobs:'):
        job_indent = 0
        step_indent = None
        job_env = False
        continue

    if job_indent is not None and indent == 2 and stripped.endswith(':') and not stripped.startswith('-'):
        # A new job starts at this indentation; reset any previous step/job env state.
        job_indent = indent
        step_indent = None
        job_env = False
        continue

    if stripped.startswith('- name:') or stripped.startswith('- id:'):
        step_indent = indent
        job_env = False
        continue

    if stripped == 'env:':
        if step_indent is None and job_indent is not None and indent > job_indent and indent <= job_indent + 2:
            job_env = True
        else:
            job_env = False
        continue

    if stripped.startswith('COPILOT_GITHUB_TOKEN:'):
        if job_env:
            raise SystemExit("Job-level COPILOT_GITHUB_TOKEN is present; scope it to a step env block.")
        if step_indent is None and job_indent is not None and indent <= job_indent + 2:
            raise SystemExit("COPILOT_GITHUB_TOKEN is not clearly scoped to a step env block.")

    if indent == 0 and stripped.endswith(':') and not stripped.startswith('-'):
        # Leaving the current job context. A later env block is only job-level if it is
        # nested under the job itself.
        step_indent = None
        job_indent = None
        job_env = False
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
