#!/usr/bin/env python3
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / '.github' / 'scripts' / 'validate_ai_review.sh'


def extract_python_block(script_path: Path) -> str:
    text = script_path.read_text(encoding='utf-8')
    start = text.index("import sys\n", text.index("python3 - \"$WF\" <<'PY'"))
    end = text.index("\nPY\n", start)
    return text[start:end]


def run_validator(yaml_text: str) -> subprocess.CompletedProcess[str]:
    python_code = extract_python_block(SCRIPT)
    with tempfile.TemporaryDirectory() as tmpdir:
        wf = Path(tmpdir) / 'tmp-ai-review.yml'
        wf.write_text(yaml_text, encoding='utf-8')
        return subprocess.run(
            ['python3', '-c', python_code, str(wf)],
            capture_output=True,
            text=True,
            check=False,
        )


class ValidateAIReviewTests(unittest.TestCase):
    def test_job_level_env_is_rejected(self):
        result = run_validator(
            """
name: AI PR Review
jobs:
  example:
    env:
      COPILOT_GITHUB_TOKEN: abc
    runs-on: ubuntu-latest
    steps:
      - name: Step
        run: echo hi
""".strip()
        )
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_step_level_token_is_allowed(self):
        result = run_validator(
            """
name: AI PR Review
jobs:
  automatic-review:
    runs-on: ubuntu-latest
    steps:
      - name: Check Copilot token
        id: check-token
        env:
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_TOKEN }}
        run: |
          if [ -z "$COPILOT_GITHUB_TOKEN" ]; then
            echo "skip=true" >> "$GITHUB_OUTPUT"
          else
            echo "skip=false" >> "$GITHUB_OUTPUT"
          fi
""".strip()
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_unrelated_job_level_env_does_not_poison_later_step_scoping(self):
        result = run_validator(
            """
name: AI PR Review
jobs:
  automatic-review:
    env:
      DRY_RUN: 'false'
    runs-on: ubuntu-latest
    steps:
      - name: Check Copilot token
        env:
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_TOKEN }}
        run: echo ok
""".strip()
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_checkout_must_precede_pr_info_generation(self):
        result = run_validator(
            """
name: AI PR Review
jobs:
  automatic-review:
    runs-on: ubuntu-latest
    steps:
      - name: Check Copilot token
        id: check-token
        env:
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_TOKEN }}
        run: echo ok
      - name: Get PR information
        run: echo pr > pr-info.json
      - name: Checkout repository
        uses: actions/checkout@v4
""".strip()
        )
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
