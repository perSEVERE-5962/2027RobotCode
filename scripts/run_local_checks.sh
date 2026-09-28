#!/usr/bin/env bash
set -euo pipefail

# scripts/run_local_checks.sh
# Run the same validation checks locally as the CI `Validate Workflows` job.
# Usage: ./scripts/run_local_checks.sh [--install-missing]

INSTALL_MISSING=false
if [[ ${1-} == "--install-missing" || ${1-} == "-i" ]]; then
  INSTALL_MISSING=true
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

missing=()

check_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    missing+=("$1")
    return 1
  fi
  return 0
}

echo "Checking required tools..."
check_cmd actionlint || true
check_cmd yamllint || true
check_cmd shellcheck || true
check_cmd python3 || true
check_cmd pip3 || true

if [ ${#missing[@]} -ne 0 ]; then
  echo "Missing: ${missing[*]}"
  if [ "$INSTALL_MISSING" = true ]; then
    echo "Attempting to install missing tools (may require sudo)..."
    for tool in "${missing[@]}"; do
      case "$tool" in
        actionlint)
          echo "Installing actionlint..."
          ACTIONLINT_VER="1.7.12"
          URL="https://github.com/rhysd/actionlint/releases/download/v${ACTIONLINT_VER}/actionlint_${ACTIONLINT_VER}_linux_amd64.tar.gz"
          curl -sSL "$URL" -o /tmp/actionlint.tar.gz
          tar -xzf /tmp/actionlint.tar.gz -C /tmp
          if [ -f /tmp/actionlint ]; then
            sudo install /tmp/actionlint /usr/local/bin/ || true
          else
            echo "Failed to extract actionlint binary from archive" >&2
          fi
          ;;
        yamllint)
          echo "Installing yamllint & pyyaml via pip..."
          pip3 install --user yamllint pyyaml || pip3 install yamllint pyyaml || true
          ;;
        shellcheck)
          echo "Installing shellcheck via apt (Debian/Ubuntu)..."
          sudo apt-get update -y || true
          sudo apt-get install -y shellcheck || true
          ;;
        *)
          echo "No automated installer for $tool. Please install it manually.";
          ;;
      esac
    done
  else
    echo "Re-run with --install-missing to attempt automatic installs."
  fi
fi

# Track overall exit code
EXIT_CODE=0

# 1) actionlint
if command -v actionlint >/dev/null 2>&1; then
  echo "\nRunning actionlint..."
  if ! actionlint .github/workflows/*.yml; then
    echo "actionlint found issues"
    EXIT_CODE=1
  else
    echo "actionlint OK"
  fi
else
  echo "Skipping actionlint (not installed)"
fi

# 2) yamllint
if command -v yamllint >/dev/null 2>&1; then
  echo "\nRunning yamllint..."
  if ! yamllint .github/workflows; then
    echo "yamllint found issues"
    EXIT_CODE=1
  else
    echo "yamllint OK"
  fi
else
  echo "Skipping yamllint (not installed)"
fi

# 3) shellcheck on run blocks (use same extraction logic as CI)
if command -v shellcheck >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1; then
  echo "\nExtracting run blocks and running shellcheck..."
  TMP_SCRIPT=$(mktemp --suffix=.sh)
  python3 - <<'PY' > "$TMP_SCRIPT"
import os,re,tempfile
workflow_dir='.github/workflows'
run_blocks=[]
for name in sorted(os.listdir(workflow_dir)):
    if not name.endswith('.yml'):
        continue
    path=os.path.join(workflow_dir,name)
    with open(path,'r',encoding='utf-8') as f:
        lines=f.read().splitlines()
    i=0
    while i<len(lines):
        line=lines[i]
        if re.match(r'^\s*run:\s*\|\s*$',line):
            indent=len(line)-len(line.lstrip(' '))
            block_lines=[]
            i+=1
            while i<len(lines):
                s=lines[i]
                if s.strip()=='' :
                    block_lines.append('')
                    i+=1
                    continue
                current_indent=len(s)-len(s.lstrip(' '))
                if current_indent<=indent and not s.lstrip().startswith('#'):
                    break
                block_lines.append(s[indent+2:] if s.startswith(' '*(indent+2)) else s)
                i+=1
            run_blocks.append('\n'.join(block_lines))
            continue
        i+=1
if not run_blocks:
    print('')
else:
    print('\n\n'.join(run_blocks))
PY
  # Prepend shebang so shellcheck can analyze
  sed -i '1i#!/usr/bin/env bash\nset -euo pipefail\n' "$TMP_SCRIPT"
  if ! shellcheck "$TMP_SCRIPT"; then
    echo "shellcheck reported issues"
    EXIT_CODE=1
  else
    echo "shellcheck OK"
  fi
  rm -f "$TMP_SCRIPT"
else
  echo "Skipping shellcheck (shellcheck or python3 not installed)"
fi

# 4) repo-specific validator
if [ -f .github/scripts/validate_ai_review.sh ]; then
  echo "\nRunning repo validator .github/scripts/validate_ai_review.sh..."
  if ! bash .github/scripts/validate_ai_review.sh; then
    echo "Repo validator failed"
    EXIT_CODE=1
  else
    echo "Repo validator OK"
  fi
else
  echo "No repo validator script found at .github/scripts/validate_ai_review.sh"
fi

if [ "$EXIT_CODE" -eq 0 ]; then
  echo "\nAll checks passed locally"
else
  echo "\nSome checks failed (exit code $EXIT_CODE)"
fi

exit $EXIT_CODE
