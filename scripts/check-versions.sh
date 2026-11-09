#!/usr/bin/env bash
# Checks that the Java and Node versions agree everywhere they are written down, so a
# dependency bump that moves one of them alone (a Dependabot Docker tag, @types/node) fails CI.
#
#   Java: <java.version> in pom.xml == every setup-java `java-version` in .github/workflows
#         == the temurin tags in the Dockerfile (build and runtime stages).
#   Node: <nodeVersion> in pom.xml == client/.nvmrc, and the major of @types/node in
#         client/ and e2e/ == the Node major (newer types accept APIs the runtime lacks).
#
# Run from the repo root: bash scripts/check-versions.sh
set -u
cd "$(dirname "$0")/.."

failures=0
fail() {
  echo "::error file=$1::$2"
  failures=$((failures + 1))
}

# --- Java ---
java_version=$(sed -n 's|.*<java.version>\([^<]*\)</java.version>.*|\1|p' pom.xml | head -n1 | tr -d '[:space:]')
echo "Java: pom.xml <java.version> = '${java_version}'"
if [ -z "${java_version}" ]; then
  fail pom.xml "no <java.version> in pom.xml"
fi

for workflow in .github/workflows/*.yml; do
  while IFS= read -r line; do
    found=$(echo "${line}" | sed -n "s|.*java-version *: *['\"]\{0,1\}\([0-9][0-9.]*\)['\"]\{0,1\}.*|\1|p")
    [ -z "${found}" ] && continue
    echo "  ${workflow}: java-version ${found}"
    if [ "${found}" != "${java_version}" ]; then
      fail "${workflow}" "${workflow} sets up Java ${found}, but pom.xml <java.version> is ${java_version}"
    fi
  done < <(grep -E 'java-version *:' "${workflow}")
done

docker_tags=$(grep -oE 'temurin[-:][0-9]+' Dockerfile | grep -oE '[0-9]+$')
if [ -z "${docker_tags}" ]; then
  fail Dockerfile "no eclipse-temurin image tag found in the Dockerfile"
fi
for found in ${docker_tags}; do
  echo "  Dockerfile: temurin ${found}"
  if [ "${found}" != "${java_version}" ]; then
    fail Dockerfile "the Dockerfile uses temurin ${found}, but pom.xml <java.version> is ${java_version}; the build and runtime images must match the JDK CI tests with"
  fi
done

# --- Node ---
pom_node=$(sed -n 's|.*<nodeVersion>v\{0,1\}\([^<]*\)</nodeVersion>.*|\1|p' pom.xml | tr -d '[:space:]')
nvmrc_node=$(tr -d '[:space:]' < client/.nvmrc | sed 's/^v//')
echo "Node: pom.xml <nodeVersion> = '${pom_node}', client/.nvmrc = '${nvmrc_node}'"
if [ -z "${pom_node}" ] || [ "${pom_node}" != "${nvmrc_node}" ]; then
  fail client/.nvmrc "client/.nvmrc (${nvmrc_node}) and <nodeVersion> in pom.xml (${pom_node}) must be the same"
fi
node_major=${nvmrc_node%%.*}

for package in client/package.json e2e/package.json; do
  types=$(sed -n 's|.*"@types/node" *: *"[^0-9]*\([0-9][0-9]*\).*|\1|p' "${package}")
  echo "  ${package}: @types/node major ${types:-none}"
  if [ -n "${types}" ] && [ "${types}" != "${node_major}" ]; then
    fail "${package}" "${package} has @types/node ${types}.x, but the runtime is Node ${node_major}; the types must match the runtime major"
  fi
done

if [ "${failures}" -gt 0 ]; then
  echo "${failures} version mismatch(es)"
  exit 1
fi
echo "All versions agree"
