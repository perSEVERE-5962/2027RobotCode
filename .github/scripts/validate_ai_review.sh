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
# Also enforce checkout-before-PR-data generation so pr-info.json/pr.diff/changed-files.txt
# are not removed when actions/checkout resets the workspace for a non-Git checkout.
python3 - "$WF" <<'PY'
import sys
from pathlib import Path

try:
    import yaml
except ModuleNotFoundError as exc:
    raise SystemExit("PyYAML is required for workflow validation. Install it with 'python3 -m pip install pyyaml'.") from exc

wf = Path(sys.argv[1])
doc = yaml.safe_load(wf.read_text(encoding='utf-8')) or {}

jobs = doc.get('jobs', {})
if not isinstance(jobs, dict):
    raise SystemExit("Workflow does not contain a valid jobs map.")

for job_name, job in jobs.items():
    if not isinstance(job, dict):
        continue

    job_env = job.get('env', {})
    if isinstance(job_env, dict) and 'COPILOT_GITHUB_TOKEN' in job_env:
        raise SystemExit(f"Job-level COPILOT_GITHUB_TOKEN is present in job '{job_name}'; scope it to a step env block.")

    steps = job.get('steps', [])
    if not isinstance(steps, list):
        continue

    checkout_index = None
    checkout_uses_head_ref = False
    pr_head_ref_output = False

    for index, step in enumerate(steps):
        if not isinstance(step, dict):
            continue

        step_name = str(step.get('name', ''))
        uses = str(step.get('uses', ''))
        if 'actions/checkout' in uses or 'Checkout repository' in step_name:
            checkout_index = index
            with_block = step.get('with', {}) or {}
            if isinstance(with_block, dict):
                ref_value = str(with_block.get('ref', '') or '')
                if 'steps.pr.outputs.head_ref_oid' in ref_value or 'headRefOid' in ref_value or 'refs/pull/' in ref_value:
                    checkout_uses_head_ref = True
            break

        if step_name == 'Get PR information':
            run_script = str(step.get('run', '') or '')
            if 'head_ref_oid' in run_script or 'headRefOid' in run_script:
                pr_head_ref_output = True

    for index, step in enumerate(steps):
        if not isinstance(step, dict):
            continue

        step_name = str(step.get('name', ''))
        if step_name in {'Get PR information', 'Get PR diff', 'Get changed files'}:
            if checkout_index is None or index < checkout_index:
                if step_name == 'Get PR information' and pr_head_ref_output and checkout_uses_head_ref:
                    continue
                raise SystemExit(
                    f"Job '{job_name}' writes PR metadata before checkout: '{step_name}' appears before the repository checkout."
                )
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
