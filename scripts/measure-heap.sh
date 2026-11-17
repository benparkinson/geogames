#!/usr/bin/env bash
# Measures the live heap of the server after every game type has been played once, so every loader
# and cache is warm. Needs a built jar (./mvnw -DskipTests -Dskip.frontend package is enough when
# client/out exists) and JAVA_HOME (Java 25). Nothing in the repo is changed.
#
# Usage: bash scripts/measure-heap.sh [GAME_TYPE...]   (default: every GameType constant)
#
# It starts the jar with -Xmx400M (as the Dockerfile does), posts a 10-round game of each type and
# fetches all 10 rounds, runs a GC, prints the "used" figure of GC.heap_info and stops the server.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JCMD="${JAVA_HOME:+$JAVA_HOME/bin/}jcmd"
PORT="${PORT:-8089}"
JAR="$ROOT/target/geogames-0.0.1-SNAPSHOT.jar"
BASE="http://localhost:$PORT"

if [ ! -f "$JAR" ]; then
  echo "No jar at $JAR. Build it first." >&2
  exit 2
fi

if [ "$#" -gt 0 ]; then
  types=("$@")
else
  mapfile -t types < <(grep -oE '^  [A-Z_]+\(' src/main/java/com/parkinson/benjamin/geogames/model/GameType.java | tr -d ' (')
fi

LOG="$ROOT/target/measure-heap.log"
mkdir -p "$ROOT/target"
"$JAVA" -Xmx400M -jar "$JAR" --server.port="$PORT" > "$LOG" 2>&1 &
pid=$!
trap 'kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null' EXIT

for _ in $(seq 1 120); do
  if curl -sf "$BASE/health" > /dev/null; then break; fi
  if ! kill -0 "$pid" 2>/dev/null; then echo "The server stopped; see $LOG" >&2; exit 1; fi
  sleep 1
done
curl -sf "$BASE/health" > /dev/null || { echo "The server did not start; see $LOG" >&2; exit 1; }

for type in "${types[@]}"; do
  response="$(curl -sf -X POST "$BASE/api/games" -H 'Content-Type: application/json' \
    -d "{\"gameType\":\"$type\",\"numberOfRounds\":10}")" || { echo "FAIL  creating a $type game" >&2; exit 1; }
  id="$(echo "$response" | sed -E 's/.*"id":"([^"]+)".*/\1/')"
  for round in $(seq 0 9); do
    code="$(curl -s -o "$ROOT/target/measure-heap-round.json" -w '%{http_code}' "$BASE/api/games/$id/rounds/$round")"
    if [ "$code" != "200" ]; then
      echo "FAIL  $type round $round: HTTP $code: $(head -c 300 "$ROOT/target/measure-heap-round.json")" >&2
      exit 1
    fi
  done
  echo "played $type"
done

# Git Bash pids are not Windows pids; jcmd needs the real one.
jpid="$(cat "/proc/$pid/winpid" 2>/dev/null || echo "$pid")"
"$JCMD" "$jpid" GC.run > /dev/null
info="$("$JCMD" "$jpid" GC.heap_info)"
echo "$info"
echo "$info" | grep -oE 'used [0-9]+K' | head -1 | sed -E 's/^/LIVE HEAP: /'
