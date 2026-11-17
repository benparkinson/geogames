#!/usr/bin/env bash
# Fails when a test is skipped or focused in a commit: a committed .only, .skip, .todo or .fixme,
# a second test.fail() in the browser tests, or a @Disabled Java test. Each of these passes every
# other CI job, so the suite would shrink without anyone noticing.
#
# Run from anywhere: bash scripts/check-no-skipped-tests.sh
set -u
cd "$(dirname "$0")/.."

failures=0

# Prints each match of $1 (an extended regex) as a GitHub error annotation.
# The remaining arguments are extra grep arguments (--include, --exclude, paths).
scan() {
  local pattern="$1" message="$2"
  shift 2
  local hits
  hits=$(grep -rnE --exclude-dir=node_modules "$pattern" "$@" 2>/dev/null)
  if [ -n "$hits" ]; then
    while IFS= read -r hit; do
      local file="${hit%%:*}" rest="${hit#*:}"
      echo "::error file=${file},line=${rest%%:*}::${message}: ${rest#*:}"
      failures=$((failures + 1))
    done <<<"$hits"
  fi
}

scan '\b(describe|it|test)\.(only|skip|todo|fixme)\b' \
  "A skipped or focused test" \
  --include='*.ts' --include='*.tsx' client/src client/pages e2e/tests e2e/fixtures.ts

# The one allowed test.fail() is in e2e/tests/deep-link.spec.ts (see e2e/CLAUDE.md).
scan '\btest\.fail\(' \
  "test.fail() is only allowed in deep-link.spec.ts" \
  --include='*.ts' --include='*.tsx' --exclude='deep-link.spec.ts' e2e

scan '@Disabled' "A disabled Java test" --include='*.java' src/test/java

if [ "$failures" -gt 0 ]; then
  exit 1
fi
echo "No skipped or focused tests"
