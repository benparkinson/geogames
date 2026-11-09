#!/usr/bin/env bash
# Checks that src/main/resources/processed/ is what the offline loaders produce from geodata/ today.
# Run from anywhere. Needs JAVA_HOME (Java 25). Nothing in the repo is changed: the loaders write to
# target/regen/ and the result is compared with the committed files.
#
# Not regenerated, because they use the network: processed/tripoints.json (TripointLoaderMain) and
# geodata/*/wikidata.json (WikidataMain, which is committed).
set -euo pipefail

cd "$(dirname "$0")/.."

rm -rf target/regen
# CountryLoaderMain clears an existing output directory instead of creating it.
mkdir -p target/regen/countries
if ! ./mvnw -B -q -Pregenerate-data -Dskip.frontend -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true process-classes > target/regen.log 2>&1; then
  tail -60 target/regen.log
  echo "The loaders failed (full output: target/regen.log)."
  exit 2
fi

status=0
for dir in countries country-shapes rivers lakes; do
  committed="src/main/resources/processed/$dir"
  regenerated="target/regen/$dir"
  if diff -rq --strip-trailing-cr "$committed" "$regenerated" > /dev/null; then
    echo "OK    processed/$dir is up to date"
  else
    echo "STALE processed/$dir differs from what the loaders produce:"
    diff -rq --strip-trailing-cr "$committed" "$regenerated" || true
    git diff --no-index --stat -- "$committed" "$regenerated" || true
    status=1
  fi
done

if [ "$status" -ne 0 ]; then
  echo
  echo "If the change to geodata/ was intended, regenerate the data (see src/main/java/com/parkinson/benjamin/geogames/loaders/CLAUDE.md) and commit it."
fi
exit $status
