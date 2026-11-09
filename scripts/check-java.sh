#!/usr/bin/env bash
# Formats the Java code (spotless:apply), then runs the Java unit tests and the JaCoCo gate.
# No Node, no frontend, no integration tests. Prints a summary; the full Maven output is in
# target/check-logs/. Needs JAVA_HOME (Java 25). Run from anywhere.
#
# Extra arguments go to Maven. One class or method: -Dtest=GameServiceTest or -Dtest=GameServiceTest#method
# (the coverage gate is skipped then, since a single class can't meet it).
set -uo pipefail
source "$(dirname "$0")/lib/common.sh"
require_java25
cd "$ROOT"

args=("$@")
for a in "$@"; do
  case "$a" in
    -Dtest=*) args+=(-Djacoco.skip=true -Dsurefire.failIfNoSpecifiedTests=false) ;;
  esac
done

summarise_java-format() { maven_errors "$1"; }
summarise_java-unit() { maven_errors "$1"; }

run_step java-format "$MVN" -B spotless:apply || exit 1
if run_step java-unit "$MVN" -B verify -Dskip.frontend -DskipITs -Dspotless.check.skip=true "${args[@]}"; then
  echo "      $(maven_test_summary "$LOG_DIR/java-unit.log")"
else
  exit 1
fi
