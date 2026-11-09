#!/usr/bin/env bash
# Runs the integration tests (the *IT classes, Failsafe), with the frontend build that HttpServerIT
# needs, but without the unit tests, lint, the coverage gate or spotless. Prints a summary; the full
# Maven output is in target/check-logs/. Needs JAVA_HOME (Java 25). Run from anywhere.
#
# Extra arguments go to Maven:
#   -Dit.test=GameApiIT       one class
#   -Dskip.frontend           reuse client/out from an earlier build (HttpServerIT is skipped without it)
#   -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend
#                             regenerate client/src/test/api-shapes.json
set -uo pipefail
source "$(dirname "$0")/lib/common.sh"
require_java25
use_maven_node
cd "$ROOT"

summarise_integration() { maven_errors "$1"; }

if run_step integration "$MVN" -B verify -DskipUnitTests -Dskip.lint -Djacoco.skip=true -Dspotless.check.skip=true -Dfailsafe.failIfNoSpecifiedTests=false "$@"; then
  echo "      $(maven_test_summary "$LOG_DIR/integration.log")"
else
  exit 1
fi
