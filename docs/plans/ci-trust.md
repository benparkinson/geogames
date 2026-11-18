# Plan: make a green PR check trustworthy, and catch breakage that arrives without a PR

Read `CLAUDE.md`, `src/main/CLAUDE.md` and `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md` first. This plan fixes two backend bugs that CI passes over, then changes `.github/workflows/ci.yml`, one test class, one new script and the `CLAUDE.md` files. The API, the response shapes (`client/src/test/api-shapes.json`), the database schema and the frontend stay as they are.

Work on a new branch `ci-trust` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

Do the parts in order. Each part ends with the check named in it. If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output. Part 7 changes GitHub and Render settings: **do not run any of it yourself**; it is a list for the owner.

## What was found (16–17 Nov 2026)

The pipeline is well built: one job per kind of failure, an aggregate `test` job that fails on any failed, cancelled or skipped job, no path filters, a ruleset on `main` with no bypass, e2e tests that block the internet, and a daily production smoke test. The gaps are these.

1. **A live bug that every check passes.** `GameCleanupService.deleteOldGames()` (the `@Scheduled` method) calls `deleteGamesOlderThanMaxAge` on `this`, so the `@Transactional` on that method never applies, and `GameRepository.deleteCreatedBefore` (a `@Modifying` query) has no transaction of its own. Every scheduled run throws `TransactionRequiredException: No active transaction for update or delete query`. The stack trace is in the log of the `integration` job of the latest green run on `main` (12 matches), and by the same code path old games are not being deleted in production since PR #104. Nothing fails because the error happens on the scheduler thread, and `GameCleanupServiceTest` runs inside a test transaction.
2. **A test that fails about 1 run in 40, caused by a real bug.** `processed/tripoints.json` has 164 tripoints, and 7 country triples appear twice (for example `Mozambique|South Africa|Swaziland`, `China|India|Nepal`). `TripointFinderService` picks distinct *definitions*, not distinct *subjects*, so a game can ask the same three countries twice. `GameApiIT.everyGameTypeProducesRoundsThatDeserialiseIntoItsGameData` expects 10 distinct subjects and failed for this reason on `main` (run 43982818397, "Expected size: 10 but was: 9").
3. **Green PRs are tested against an old `main`.** The ruleset has `strict_required_status_checks_policy: false`. On 16 Nov, PRs #104, #105, #106 and #101 were merged within two minutes, each green against a base that did not contain the others. Nothing tested the combinations before they reached `main`.
4. **Runs on `main` are dropped.** The concurrency group for pushes is `ci-refs/heads/main`; GitHub keeps only one pending run per group, so the runs for #105 and #106 were cancelled. If one of them had broken `main`, the red run would be the next one, pointing at the wrong PR.
5. **A test class can skip silently in CI.** `HttpServerIT` is `@EnabledIf("staticSiteIsBuilt")`. If the `client-out` artifact ever lands in the wrong place, the class is skipped and the job stays green. `PostgresMigrationIT` already guards against this with the `CI` variable.
6. **Nothing stops a committed `.skip`, `.fixme` or `@Disabled`.** Vitest and Playwright reject `.only` in CI, but a skipped test passes every check.
7. **The `docker` job runs the image in the default profile (H2).** Production runs the same image with `SPRING_PROFILES_ACTIVE=prod` against Postgres. That combination (image, `prod` profile, 512 MB) is never started before a deploy, and the job does not look at the container's log.
8. **CI only runs when someone pushes.** `ubuntu-latest`, the floating image tags (`maven:3.9-eclipse-temurin-25`, `eclipse-temurin:25-jre`, `postgres:17-alpine`), the Playwright system packages and the Node download can all change under a branch that hasn't moved. Render rebuilds the `Dockerfile` with whatever those tags are on the day of the deploy. Today such a break is found by the next unrelated PR.

Considered and left out: pinning actions to commit SHAs and a checksum for the Maven wrapper download (CI holds no secrets, has `contents: read` and deploys nothing, so a tampered action can't reach production); a `.dockerignore` (Render and CI build from a clean checkout); making CodeQL part of `test` (it is advisory; see Part 7 for the option).

## Part 1: the cleanup job runs in a transaction

Write the test first and watch it fail.

**New `src/test/java/com/parkinson/benjamin/geogames/service/GameCleanupIT.java`** (the name ends in `IT`, so Failsafe runs it; it is in the `service` package because `deleteOldGames()` is package-private). `@SpringBootTest`, no `@Transactional` on the class or the test, so there is no surrounding test transaction to hide the bug. Model the set-up on `RoundRebuildIT` in the same package. The test:

1. saves a game whose `createdAt` is two days ago and one created now (use whatever `GameCleanupServiceTest` uses to build a `GameEntity`),
2. calls `deleteOldGames()` on the autowired `GameCleanupService` bean,
3. asserts the old game is gone (`findByPublicId` is empty) and the new one is still there.

Run `bash scripts/check-it.sh -Dit.test=GameCleanupIT -Dskip.frontend`. It must fail with `TransactionRequiredException`. If it passes, the test is not reproducing the bug (most likely a transaction is open around it): fix the test before going on.

**`dao/GameRepository.java`**: add `@Transactional` (Spring's, `org.springframework.transaction.annotation.Transactional`) to `deleteCreatedBefore`, next to `@Modifying`. This is where Spring Data expects it for a modifying query, and it makes the method safe whoever calls it. Add a sentence to its Javadoc saying it opens its own transaction when there is none.

**`service/GameCleanupService.java`**: leave the code as it is. Add one line to the Javadoc of `deleteOldGames()` saying the transaction comes from the repository method, because the call to `deleteGamesOlderThanMaxAge` on `this` does not go through the Spring proxy.

Check: `bash scripts/check-it.sh -Dit.test=GameCleanupIT -Dskip.frontend` passes, then `bash scripts/check-java.sh`.

## Part 2: a tripoint game never asks the same three countries twice

**`service/TripointFinderService.java`**, in `generate`: pick distinct subjects instead of distinct definitions.

1. Filter the definitions by `excludedSubjects` as now.
2. Group them by `Tripoint.subjectId(d.countryNames())` (keep the encounter order, e.g. `LinkedHashMap`, so a seeded `Random` stays deterministic).
3. `RandomPicker.pickDistinct` over the list of groups, with the same `count`, `random` and `"tripoints"` label.
4. From each chosen group take one definition with `random.nextInt(group.size())`, so both locations of a repeated triple can still come up across games.

**`TripointFinderService`'s unit test**: add a test with three definitions, two of which share a country triple in a different order, asking for 2 rounds across many seeds (say 200 `new Random(seed)`): the two rounds always have different `subjectId()`s. Add a second that asks for 3 rounds from the same three definitions and expects the "not enough tripoints" failure `pickDistinct` already gives (there are only two subjects).

**The other generators**: read how each `RoundGenerator` picks its candidates and confirm two candidates can't share a `subjectId()`. If one can, apply the same grouping there and say so in your final report. Don't change a generator that is already safe.

Check: `bash scripts/check-java.sh`, then `bash scripts/check-it.sh -Dit.test=GameApiIT -Dskip.frontend`.

## Part 3: tests that must run in CI can't skip there

**`HttpServerIT.java`**: make the condition the same as `PostgresMigrationIT`'s, so a missing site fails the build in CI instead of hiding the class:

```java
static boolean staticSiteIsBuilt() {
  // In CI the integration job always has the built site; if it is missing, fail, don't skip.
  return System.getenv("CI") != null || new ClassPathResource("public/index.html").exists();
}
```

Update the class Javadoc to say so. Check that at least one test in the class fails when `public/index.html` is absent (one fetches the page and a `/_next/static` asset); if none would, add an assertion that `GET /` is 200 and contains `<html`.

**New `scripts/check-no-skipped-tests.sh`** (`#!/usr/bin/env bash`, `set -u`, `cd` to the repo root like `check-versions.sh`, mode 100755 via `git update-index --chmod=+x`). It fails, printing each match as `::error file=…,line=…::…`, when any of these is found:

- `\b(describe|it|test)\.(only|skip|todo|fixme)\b` in `client/src`, `client/pages`, `e2e/tests` or `e2e/fixtures.ts` (files `*.ts`, `*.tsx`);
- `\btest\.fail\(` in `e2e/` outside `e2e/tests/deep-link.spec.ts` (the one allowed marker, see `e2e/CLAUDE.md`), skipping `node_modules`;
- `@Disabled` in `src/test/java`.

Use `grep -rnE` with `--include`; print `No skipped or focused tests` on success. Run it now: it must pass on the current tree. Then add a `test.skip(` line to a spec temporarily, see it fail, and remove the line.

**`.github/workflows/ci.yml`**, job `lint-frontend`: add a step after "Check Java and Node versions agree":

```yaml
      # A committed .skip, .fixme or @Disabled passes every other job.
      - name: Check no test is skipped or focused
        run: bash scripts/check-no-skipped-tests.sh
```

Check: `bash scripts/check-no-skipped-tests.sh`, `bash scripts/check-it.sh -Dit.test=HttpServerIT` (with the frontend build, so the class runs).

## Part 4: the `docker` job runs what production runs

In `ci.yml`, job `docker`, replace the "Start the container" step and extend the ones after it. Keep "Build the production image", "Wait for /health" and the body of "Check the API" as they are.

```yaml
      # Production runs this image with the prod profile against Postgres (Neon runs major 17).
      - name: Start Postgres
        run: |
          docker network create geogames-ci
          docker run -d --name postgres --network geogames-ci \
            -e POSTGRES_USER=geogames -e POSTGRES_PASSWORD=geogames -e POSTGRES_DB=geogames \
            postgres:17-alpine
          for i in $(seq 1 30); do
            if docker exec postgres pg_isready -U geogames -d geogames > /dev/null; then exit 0; fi
            sleep 1
          done
          echo "::error::Postgres did not become ready within 30 seconds"
          exit 1

      # 512 MB is the memory of the Render instance; the Dockerfile runs the app with -Xmx400M.
      - name: Start the container
        run: |
          docker run -d --name geogames --network geogames-ci --memory=512m -p 8080:8080 \
            -e SPRING_PROFILES_ACTIVE=prod \
            -e DATABASE_URL=jdbc:postgresql://postgres:5432/geogames \
            -e DATABASE_USERNAME=geogames -e DATABASE_PASSWORD=geogames \
            geogames:ci
```

After "Check the API" add:

```yaml
      # Proves the game above went to Postgres, not to H2.
      - name: Check the game is in Postgres
        run: |
          games=$(docker exec postgres psql -U geogames -d geogames -tAc 'select count(*) from game_entity')
          echo "games: ${games}"
          test "${games}" -ge 1

      # Errors off the request thread (the cleanup job at start-up, the statistics writer) fail no
      # request, so the calls above pass. They do log at ERROR.
      - name: Check the container logged no errors
        run: |
          docker logs geogames > container.log 2>&1
          if grep -E ' ERROR | OutOfMemoryError' container.log; then
            echo "::error::The container logged errors (see above)"
            exit 1
          fi
```

Change the existing "Container logs" step to print both containers (`docker logs geogames; docker logs postgres`), still `if: failure()`.

Confirm the table name with `V1__games.sql` before using `game_entity`. The test credentials are throwaway values for a container that lives for the job; they are not secrets.

This can't be checked without Docker. If `docker version` works locally, run the same commands by hand against `docker build -t geogames:ci .`. Otherwise it is checked by the PR's own `docker` job: say in your report that it is unverified until then.

Fallback: if the container fails to start because of something in `application-prod.properties` that only suits Neon (not expected: SSL is in the URL, which this job supplies), stop and report; don't change production configuration to suit CI.

## Part 5: every commit on `main` gets its run, and CI runs weekly without a push

In `ci.yml`:

```yaml
on:
  pull_request:
  push:
    branches: [main]
  # Monday morning: finds breakage that arrives without a commit (the runner image, the floating
  # Docker base tags Render also builds from, Playwright's system packages).
  schedule:
    - cron: "17 5 * * 1"
  workflow_dispatch:
```

```yaml
# Pull requests cancel superseded runs. Every other run (each merge to main, the weekly run) has a
# group of its own: a shared group keeps only one pending run and cancels the rest, which would
# leave a commit on main untested.
concurrency:
  group: ci-${{ github.event_name == 'pull_request' && github.ref || github.run_id }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```

Change nothing else: `test` keeps its id and name, and no job gets an `if:`.

Check: if `actionlint` is installed run it on `.github/workflows/ci.yml`; otherwise, if Python has PyYAML, parse the file with `python -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"`. With neither, re-read the diff for indentation. Say which you did; the PR run is the real check.

## Part 6: documentation

- `CLAUDE.md`, section **CI**: the weekly and manual runs and why; the `docker` job now runs the `prod` profile against a Postgres container and fails on `ERROR` log lines; `lint-frontend` also runs `scripts/check-no-skipped-tests.sh`; each push to `main` gets its own run. In **Commands**, add `bash scripts/check-no-skipped-tests.sh` to the quick checks and add `GameCleanupIT` and the `RoundStats*IT` classes to the list of integration tests (it is out of date).
- `src/main/CLAUDE.md`: next to the `PostgresMigrationIT` sentence, say the CI `docker` job also starts the production image in the `prod` profile against Postgres.
- Backend `CLAUDE.md`: one rule under the test conventions: code that runs off the request thread (`@Scheduled`, the statistics writer) needs an `*IT` without a test transaction, because a unit test's transaction hides a missing one.
- `e2e/CLAUDE.md`: `test.skip`, `test.fixme` and a second `test.fail()` are rejected by `scripts/check-no-skipped-tests.sh`.
- `.github/workflows/smoke-production.yml`: the header says the test changes "one throwaway game in the backend's in-memory database". Production is Postgres now; say "one throwaway game, deleted after a day".

Final check, everything: `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh`, `bash scripts/check-it.sh`, `bash scripts/check-versions.sh`, `bash scripts/check-no-skipped-tests.sh`. Report what passed, what could only be verified by the PR run (Parts 4 and 5), and anything found in the other generators in Part 2.

## Part 7: settings for the owner (do not run these; list them in your report)

These can't be changed by a PR. In order of value:

1. **Require the branch to be up to date before merging.** Settings → Rules → Rulesets → `main` → "Require status checks to pass" → tick "Require branches to be up to date before merging". This closes finding 3: `test` is then always a result for the exact tree that lands on `main`. The cost is that after each merge, every other open PR needs "Update branch" and a new CI run. A merge queue would avoid that, but GitHub only offers it for repositories owned by an organisation.
2. **Make that cheap to live with.** `gh api -X PATCH repos/benparkinson/geogames -F allow_auto_merge=true -F allow_update_branch=true`, then `gh pr merge --auto --merge <n>` merges a PR by itself once its updated run is green.
3. **Render: deploy only after CI passes.** In each Render service (the web service and the static site): Settings → Build & Deploy → Auto-Deploy → "After CI Checks Pass". If it is on "On Commit" today, a red `main` is deployed anyway. This could not be read from the repository.
4. **Secret scanning and push protection** are off, and the repository is public. They are free for public repositories: Settings → Advanced Security → enable both.
5. **Optional: let CodeQL block.** CodeQL runs on every PR but is not required, so a new high-severity alert doesn't stop a merge. To change that, add the "Require code scanning results" rule to the `main` ruleset (tool CodeQL, security alerts "High or higher"). Leave it advisory if false positives would be a nuisance.
