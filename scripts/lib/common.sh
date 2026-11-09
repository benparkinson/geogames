#!/usr/bin/env bash
# Shared helpers for the scripts/check-*.sh scripts. Source it; don't run it.
# Each step's full output goes to a log under target/check-logs/, and only a summary (or the failure)
# is printed, so a passing check costs a few lines of output.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/target/check-logs"
mkdir -p "$LOG_DIR"

# The Maven wrapper, unless MVN names another Maven.
MVN="${MVN:-$ROOT/mvnw}"

# Puts the Node that Maven installed (target/node) first on PATH, when it exists. The system Node
# may be older than Next 16 supports.
use_maven_node() {
  if [ -d "$ROOT/target/node" ]; then
    export PATH="$ROOT/target/node:$PATH"
  fi
}

# Fails early, with a clear message, unless JAVA_HOME points at Java 25.
require_java25() {
  if [ -z "${JAVA_HOME:-}" ]; then
    echo "JAVA_HOME is not set. This build needs Java 25." >&2
    exit 2
  fi
  if [ ! -x "$JAVA_HOME/bin/java" ] && [ ! -x "$JAVA_HOME/bin/java.exe" ]; then
    echo "JAVA_HOME ($JAVA_HOME) has no bin/java. This build needs Java 25." >&2
    exit 2
  fi
  local version
  version="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
  case "$version" in
    *'"25'*) ;;
    *) echo "JAVA_HOME ($JAVA_HOME) is not Java 25: $version" >&2; exit 2 ;;
  esac
}

# run_step NAME COMMAND...: runs the command with its output in $LOG_DIR/NAME.log.
# Prints "OK    NAME" on success. On failure prints "FAIL  NAME", the interesting lines of the log
# (from the summarise_NAME function if there is one, else its last 60 lines) and returns 1.
run_step() {
  local name="$1"; shift
  local log="$LOG_DIR/$name.log"
  if "$@" > "$log" 2>&1; then
    echo "OK    $name"
    return 0
  fi
  echo "FAIL  $name (full output: ${log#"$ROOT"/})"
  if declare -F "summarise_$name" > /dev/null; then
    "summarise_$name" "$log"
  else
    tail -60 "$log"
  fi
  return 1
}

# The [ERROR] lines of a Maven log, without the blank ones and the generic help text at the end,
# plus JaCoCo's "Rule violated" warnings.
#
# For test failures this is the Surefire/Failsafe "Failures:" / "Errors:" block, which holds each
# failed test with its assertion message (the message lines have no [ERROR] prefix).
maven_errors() {
  local clean
  clean="$(tr -d '\r' < "$1")"
  if grep -qE '^\[ERROR\] (Failures|Errors): *$' <<< "$clean"; then
    awk '/^\[ERROR\] (Failures|Errors): *$/ {on=1} on {print} /^\[ERROR\] Tests run:/ {on=0}' <<< "$clean" | head -80
    grep -E 'Failed to execute goal' <<< "$clean" | head -3
    return
  fi
  grep -E '^\[ERROR\]|Rule violated' <<< "$clean" \
    | grep -vE '^\[ERROR\] *$|-> \[Help 1\]|Re-run Maven|full stack trace|re-run Maven|For more information|After correcting the problems|^\[ERROR\] See ' \
    | head -60
}

# The last test summary in a Maven log ("Tests run: N, Failures: 0, ...").
maven_test_summary() {
  tr -d '\r' < "$1" | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' | tail -1 \
    | sed -E 's/^\[(INFO|WARNING|ERROR)\] *//'
}
