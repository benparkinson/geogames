#!/usr/bin/env bash
# Runs the frontend checks in client/: ESLint (zero warnings), the lint-rules probe, tsc and Vitest.
# Uses the Node that Maven installed (target/node) when it exists, and runs npm ci first if
# client/node_modules is missing. Prints a summary; full output is in target/check-logs/.
#
# Extra arguments go to Vitest, for example a folder or file: scripts/check-frontend.sh src/games/lakes
# (lint and tsc still check the whole client).
set -uo pipefail
source "$(dirname "$0")/lib/common.sh"
use_maven_node
cd "$ROOT/client"

summarise_frontend-lint() { grep -vE '^\s*$|^>' "$1" | head -60; }
summarise_frontend-tsc() { grep -E 'error TS' "$1" | head -40; }
# Vitest prints each failure's details, then a summary; the failed test names come first.
summarise_frontend-vitest() {
  grep -E '(FAIL|×) ' "$1" | head -20
  echo "..."
  tail -40 "$1"
}

if [ ! -d node_modules ]; then
  run_step frontend-npm-ci npm ci || exit 1
fi

status=0
run_step frontend-lint npm run lint || status=1
run_step frontend-lint-rules npm run lint:rules || status=1
run_step frontend-tsc npm run typecheck || status=1
if run_step frontend-vitest npx vitest run "$@"; then
  echo "      $(grep -E '^\s*Tests ' "$LOG_DIR/frontend-vitest.log" | tail -1 | sed -E 's/^\s+//')"
else
  status=1
fi
exit $status
