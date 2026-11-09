# Plan: integration and end-to-end tests, and a CI pipeline that says what broke

Read `CLAUDE.md` first. Do this after `unit-test-coverage.md` is merged. It relies on the JaCoCo gate and the Vitest thresholds from that plan. Work on a new branch, `ci-pipeline`, from an up-to-date `main`. Do not commit unless the user asks. Don't push, open a PR, or change GitHub or Render settings unless the user asks. Phase 8 lists what the user has to do.

Do the phases in order. Each phase ends with a verification step. **Don't start the next phase until that step passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Ground rules

- **Don't change production behaviour.** The one production change allowed is the build-time API URL override in phase 4.1, which changes nothing when it isn't set. If a test exposes a bug, don't fix it. Follow the instructions for that case (phase 2.3 and phase 4.4) and list it in the final report.
- No PR-time test may reach the internet. That covers Wikipedia, Wikidata, tile servers and `onrender.com`. The only exception is the production smoke workflow in phase 7, which never runs on a PR.
- No `@Disabled`, `test.skip`, `continue-on-error`, `|| true` or retries that hide a failing test. The single allowed `test.fail()` is described in phase 4.4.
- GitHub Actions: use only `actions/*`, `docker/*` and `github/*` actions, each pinned to the major version already used in the repo (`actions/checkout@v7`, `actions/setup-java@v6`). For actions not yet in the repo (`actions/setup-node`, `actions/upload-artifact`, `actions/download-artifact`), use their current major version. Give every job `timeout-minutes`.
- Environment on this machine: `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.0.2"`, put `target/node` on `PATH` for npm, and use `/c/Program Files/GitHub CLI/gh.exe` for `gh`.

## Where things stand (checked on 2026-11-08)

**CI** (`.github/workflows/ci.yml`) is one job, `test`, that runs `./mvnw -B verify`. That single Maven run does everything in sequence: install Node, `npm ci`, `npm run lint`, `next build`, compile Java, the JUnit tests, Vitest, package the jar, and `spotless:check`. When it's red, you have to read the log to find out whether it was lint, formatting, a unit test, an integration test, the frontend build or the data. `test` is the **required status check** on `main`. `codeql.yml` is separate and stays as it is.

**Integration tests that exist:**
- `GameApiIntegrationTest` (`@SpringBootTest` + MockMvc, real processed data, nothing mocked). It covers a full Tripoint game with scoring, every game type's round shape, `RANDOM` subject uniqueness, `/api/countries` and `/api/rivers`, 400s for bad requests, 404s, the hint limit, `OUT_OF_GUESSES`, and CORS for `localhost:3000` and a disallowed origin. This is good. Keep and extend it.
- `GeogamesApplicationTests`: context loads.
- `ProcessedDataIntegrityTest`: checks the committed processed data. It's plain JUnit with no Spring.
- `@WebMvcTest` slices (`GameControllerTest`, `HealthControllerTest`, `PlaceNameControllerTest`) and `@DataJpaTest` `GameRepositoryTest`. These count as unit tests and stay in Surefire.
- Nothing separates unit tests from integration tests. Surefire runs all of them.

**What nothing tests, and what can break production with CI green:**
1. **The Dockerfile.** Render builds the backend from it, but CI never builds it. A broken `Dockerfile`, a base image that changes, or the app no longer starting in `-Xmx400M` would only show up on deploy.
2. **The real HTTP server.** Every test goes through MockMvc. Nothing starts Tomcat on a port and calls it, and nothing checks the static site the jar serves (`/`, `/_next/...`).
3. **The production CORS origin.** The CORS test checks `http://localhost:3000`, not `https://geogauntlet.onrender.com`, the production frontend. An edit to `geogames.cors.allowed-origins` could break production with every test passing.
4. **`/api/lakes`** has no integration test (`/api/rivers` does).
5. **The frontend–backend contract.** The frontend tests use hand-written fixtures (`client/src/test/fixtures.ts`) and a mocked `fetch`. If the backend renames a field in a `GameData` record, both suites still pass and the game breaks. The frontend and backend are deployed separately on Render (`client/src/api/types.ts` already has "missing when talking to an older backend" fields), which makes this more likely.
6. **No browser test.** Nothing loads the built site in a browser, starts a game, types an answer and sees the summary.
7. **The jar's frontend calls production.** `serverEndpoint()` returns `https://geogames-web-service.onrender.com` whenever `NODE_ENV` is `production`, which includes the `next build` bundled into the jar. A browser test against the jar would therefore hit production. Phase 4.1 fixes this for tests.
8. **Deep links into the jar.** `next build` writes `out/games/[gameId].html` literally, and nothing in `config/` forwards `/games/{id}` to it. So reloading a game page served by the jar probably returns 404. Phase 2.3 confirms it. On Render the static site may have a rewrite rule. That isn't visible from the repo, so ask the user.
9. **Data drift.** The offline loaders are deterministic and their inputs (`geodata/`) are committed, but nothing checks that `src/main/resources/processed/` is what the loaders produce now. Someone can edit `overrides.json` or a loader and forget to regenerate. `ProcessedDataIntegrityTest` checks the data is internally consistent, not that it is current.
10. **The pipeline itself.** There's no concurrency group (superseded PR pushes keep running), no job timeouts, no test reports kept on failure, and the Node version is in `pom.xml` only.

## Phase 0: baseline

1. `git checkout main && git pull && git checkout -b ci-pipeline`.
2. `mvn -B verify` passes. Note the test counts.

## Phase 1: separate unit and integration tests in Maven

1. **Naming convention:** integration tests end in `IT` and run under Failsafe. Everything else runs under Surefire. Rename the classes and keep their history with `git mv`:
   - `GameApiIntegrationTest` → `GameApiIT`
   - `GeogamesApplicationTests` → `ApplicationContextIT`
   - `ProcessedDataIntegrityTest` → `ProcessedDataIT`

   Update the references to these names in `CLAUDE.md`.
2. Add `maven-failsafe-plugin` to `pom.xml` with the `integration-test` and `verify` goals. Spring Boot's parent manages the version, so don't set one. Failsafe runs `*IT.java` by default. Check that Surefire's default includes don't also pick them up. Make sure JaCoCo's `prepare-agent` also covers Failsafe: either add a `prepare-agent-integration` execution, or leave the gate on unit tests only. **Leave the gate on unit tests only** and say so in a comment in the POM.
3. Add these properties, all defaulting to `false`, so each CI job can run exactly one thing:
   - `skipUnitTests`: set Surefire's `<skipTests>${skipUnitTests}</skipTests>`, and add `<skip>${skipUnitTests}</skip>` to the frontend plugin's `npm test` execution.
   - `skipITs`: Failsafe already honours this one. `-DskipTests` must still skip everything, as it does today.
   - `skip.lint`: `<skip>${skip.lint}</skip>` on the `npm run lint` execution.
   - `skip.frontend`: skips the node install and all the npm executions. Set the plugin's own `skip.installnodenpm` and `skip.npm` from it, or put `<skip>` on each execution. Pick one approach and explain it in a POM comment.
   - Make the `maven-antrun-plugin` copy tolerate a missing `client/out` (`erroronmissingdir="false"` on the `fileset`), so a backend-only build with `-Dskip.frontend` works without an earlier frontend build. When `client/out` does exist it must still be copied.
4. Add `client/.nvmrc` containing `24.22.2`, the `nodeVersion` in `pom.xml`. Add a comment next to `nodeVersion` saying both must change together. Phase 6 checks this.

**Verify each command on its own:**
- `mvn -B verify` (everything) passes and runs both Surefire and Failsafe. The log shows `Tests run:` from both.
- `mvn -B verify -Dskip.frontend -DskipITs -Dspotless.check.skip=true` runs only the Java unit tests and the JaCoCo gate. Delete `client/out` first to prove it doesn't need it, then rebuild it.
- `mvn -B verify -DskipUnitTests -Dskip.lint -Djacoco.skip=true -Dspotless.check.skip=true` runs only the frontend build and the ITs.
- `mvn -B -DskipTests package` still builds a working jar.

## Phase 2: more backend integration tests

### 2.1 Extend `GameApiIT` (MockMvc)
- **Every game type through one parameterised test** over `GameType.values()`: create a 10-round game, read every round, and deserialise each `jsonBlob` into the type's `GameData` record with the application's own `ObjectMapper` (inject it). Assert that `gameType()` matches and that the `subjectId()`s within the game are distinct. A new `GameType` then gets this coverage without anyone writing a test. The existing per-type tests check type-specific fields, so keep them.
- **Answering again:** posting the identical answer twice returns 200 both times and doesn't change the score. Posting a different answer to an answered round returns 409, with content type `application/problem+json` and a `ProblemDetail` body (`status`, `title`, `detail`).
- **Bad bodies:** malformed JSON returns 400 with `application/problem+json`. A `text/plain` body returns 415. An answer to an unknown game returns 404. If any of these comes back as 500 or as a non-ProblemDetail body, assert the current behaviour with `// Current behaviour:` and report it.
- **`/api/lakes`:** sorted, the same count as the files in `processed/lakes`, and it contains the lake of a `LAKES_BY_SHAPE` round (as the rivers test does).
- **The production CORS origin:** for every origin in `geogames.cors.allowed-origins` (read the property, don't hard-code it), a `POST /api/games` preflight and a `GET /api/countries` preflight both allow that origin. Also add one test that hard-codes `https://geogauntlet.onrender.com`, with a comment explaining that it is the production frontend and that removing it from the property breaks production.

### 2.2 New `HttpServerIT`: a real port
`@SpringBootTest(webEnvironment = RANDOM_PORT)`. In Boot 4 the HTTP test clients moved too. Find out from the jars, not from memory, whether to use `RestTestClient` (Spring Framework 7, with `@AutoConfigureRestTestClient`) or `TestRestTemplate` (from the `spring-boot-resttestclient` module), and add the test-scoped starter it needs with no version. Then test:
- `GET /health` returns 200 and `Server running!`.
- `POST /api/games`, then `GET` round 0 over real HTTP. Assert the response's `Content-Type`, and that a real CORS request (not only a preflight) from the production origin gets `Access-Control-Allow-Origin` back.
- The static site: `GET /` returns 200 HTML containing the `<div id="__next"` marker (check `client/out/index.html` for the real marker), and one `/_next/static/...` asset linked from that page returns 200. This test only means something when `client/out` was built. Annotate the class with `@EnabledIf` on a condition "classpath has `public/index.html`", so a backend-only build doesn't fail it. Phase 5's integration job always builds the frontend, so it runs there. Say this in the class Javadoc.

### 2.3 Find out what deep links do
In `HttpServerIT`, `GET /games/1`. **Don't fix anything.** Record the status in the final report:
- If it's 200 with the game page, assert that.
- If it's 404, which is expected, write the test so it asserts 404 with `// Current behaviour: reloading a game page served by the jar is a 404, because nothing forwards /games/{id} to games/[gameId].html`, and report it as a bug. The fix is a small `WebMvcConfigurer` forward, but that's for the user to decide.

### 2.4 Keep `ProcessedDataIT` as it is, apart from the rename.

**Verify:** `mvn spotless:apply && mvn -B verify` passes.

## Phase 3: contract test between backend and frontend

Goal: if the JSON the backend sends changes shape, a test fails on **both** sides until someone updates the frontend.

1. **The shape format.** A shape is a JSON value that describes another JSON value:
   - an object becomes an object with the same keys (sorted), each mapped to its child's shape;
   - an array becomes `{"array": <the merged shape of its elements>}`, and an empty array becomes `{"array": "empty"}`. Merging two object shapes takes the union of their keys, so a key missing from some elements is still listed. Merging `"empty"` with anything gives the other shape;
   - primitives become `"string"`, `"number"`, `"boolean"` or `"null"`;
   - the value of any key named `coordinates` becomes `"coordinates"`, because GeoJSON nesting depth varies between `Polygon`, `MultiPolygon` and `MultiLineString`.
2. **Backend `ApiContractIT`.** For every `GameType`, create a 10-round game, and collect the shape of the full `GameRound` response with its `jsonBlob` parsed and shaped too, merging the 10 rounds. Also collect the shapes of `GET /api/countries`, `/api/rivers` and `/api/lakes`, and of the `POST /api/games` response. Compare the result to the committed `contract/api-shapes.json` at the repo root, keyed by `GameType` name and endpoint. If it differs, fail with a message that says how to regenerate. When run with `-Dcontract.update=true`, write the file instead of comparing. Write it pretty-printed with sorted keys, so diffs are readable. Then generate the file and commit it as part of this work.
3. **Frontend `client/src/api/contract.test.ts`.** Import `../../../contract/api-shapes.json` (check that Vite resolves it, and that `next build` type-checks it with `resolveJsonModule`; if the path is awkward, put the file in `client/src/test/` and point the backend there instead). Write a `shapeOf` helper in `client/src/test/shape.ts` that implements the same format. For each game type, the shape of the fixture from `client/src/test/fixtures.ts` (and `makeRoundResponse` or its equivalent, for the envelope) must be a **subset** of the backend shape: every key the fixture has exists on the backend with the same primitive type. Then make the reverse check: list every key on the backend that the fixture lacks, and fail unless it appears in an explicit `IGNORED_BACKEND_FIELDS` list in the test, each entry with a one-line reason. Fixtures that are deliberately minimal can stay minimal, but nothing new can appear unnoticed.
4. Fix the fixtures where they differ from the real shapes. That's test code, so it's allowed. If a fixture was wrong in a way that hid a real frontend bug (the frontend reads a field the backend never sends), report it.

**Verify:** both sides pass. Rename one field in a `GameData` record (for example `River.mouth` → `outlet`) and check that `ApiContractIT` fails. Regenerate the file with `-Dcontract.update=true` and check that `contract.test.ts` now fails. Revert both.

## Phase 4: browser end-to-end tests with Playwright

### 4.1 Make the API URL configurable at build time
In `client/src/api/gateway.ts`, if `process.env.NEXT_PUBLIC_API_URL` is set and not empty, `serverEndpoint()` returns it. Otherwise it behaves exactly as now. Add cases to `gateway.test.ts` with `vi.stubEnv` for: set, empty, and unset in both production and development. Add the variable to the "Environment switching" paragraph of `CLAUDE.md`.

### 4.2 Set up the project
- Create a separate npm package `e2e/` at the repo root (its own `package.json`, `package-lock.json`, `tsconfig.json` and `playwright.config.ts`), so Playwright doesn't enter the client's dependencies, `next build` or Dependabot's `/client` group. Add a Dependabot entry for `/e2e` grouped like the others.
- Dependency: `@playwright/test`, caret range of the current version, at least two weeks old. Use Chromium only, with two projects: `desktop` (Desktop Chrome) and `mobile` (Pixel 7, which exercises `MapGameInput`'s mobile path).
- `baseURL` comes from `E2E_BASE_URL` and defaults to `http://localhost:8080`. Set `webServer` to start `java -jar ../target/geogames-0.0.1-SNAPSHOT.jar` with `url: <base>/health`, `reuseExistingServer: !process.env.CI` and `timeout: 120_000`.
- In CI: `retries: 0` (a flaky test must show up), `forbidOnly: true`, the `github` and `html` reporters, and traces, screenshots and videos kept `on-first-retry`/`retain-on-failure`.
- `e2e/README.md`: how to run locally. Build the jar with the API override: `NEXT_PUBLIC_API_URL=http://localhost:8080 mvn -B -DskipTests package`. Then `cd e2e && npm ci && npx playwright install chromium && npx playwright test`.

### 4.3 Shared fixture `e2e/fixtures.ts`
Extend `test` so every test automatically:
- **Blocks the internet.** Using `context.route("**/*", ...)`, continue requests to `localhost` / `127.0.0.1` and abort everything else, recording the aborted hosts. Map tiles will be missing, which is fine. At the end of the test, fail if any aborted request was to `onrender.com`, since that means the API override didn't work.
- **Fails on errors in the page.** Collect `pageerror` events and `console` messages of type `error`. Ignore the aborted tile requests with a narrow, commented filter, and fail on anything else.
- **Provides `api`,** a helper on Playwright's `request` fixture that reads a round (`GET /api/games/{id}/rounds/{n}`), parses `jsonBlob` and returns the correct answer names. Get the names the way the frontend does: tripoint is the 3 country names, rivers and lakes the name, country shapes the country name. Read each module's `answerNames` to confirm.

### 4.4 Scenarios (`e2e/tests/*.spec.ts`)
Find elements by role, label and text, as in the unit tests. Read `HomePage`, `MapGameInput`, `RoundResult` and `GameSummary` for the actual labels. Don't guess.
1. **`home.spec.ts`:** the home page lists every game in `client/src/games/registry.ts`, plus Random. Hard-code the expected titles in the spec, with a comment that they mirror the registry. The round-count options are there.
2. **`every-game.spec.ts`:** parameterised over every backend game type plus `RANDOM`. From the home page, choose the game and 1 round and start it. The URL becomes `/games/<id>`. A `.leaflet-container` is visible. Type the correct answer(s) from `api`. The round shows as correct. Open the summary, which shows 100 points out of 100. This is the main "every game works in a real browser" check.
3. **`full-game.spec.ts`:** a 3-round Tripoint game. Round 1: take 1 hint, then answer correctly (83 points). Round 2: give up (0 points, answers revealed). Round 3: make wrong guesses of real country names that aren't answers until the guess limit (5) ends the round as out of guesses (0 points). The summary shows 83 / 300 and the three round states. The numbers come from `model/Scoring`.
4. **`guessing.spec.ts`:** a misspelt name that isn't a country is rejected and doesn't use up a guess. A wrong real country uses up one guess, and the "guesses left" count drops. An alias from `additionalNames` (for example "Holland" when the Netherlands is an answer; find a round where an answer has an alias by creating games until one does, with a cap) is accepted.
5. **`deep-link.spec.ts`:** start a game, then `page.reload()`. If phase 2.3 found a 404, mark this test `test.fail()` with a comment that points to the bug. Then it passes while the bug exists and fails once it is fixed, which tells whoever fixes it to remove the marker. This is the only allowed `test.fail()`.

### 4.5 Speed
The whole suite should take under 3 minutes in CI. If `every-game` is slow, run the specs in parallel (`fullyParallel: true`): the backend is stateless apart from the games it creates.

**Verify:** locally, build the jar with the override, then `npx playwright test` passes on both projects. Run `--repeat-each=3` once to check for flakiness. Then build the jar **without** the override, run the suite, and check that it fails with the `onrender.com` guard message, not with timeouts. Rebuild with the override afterwards.

## Phase 5: split the GitHub Actions workflow

Rewrite `.github/workflows/ci.yml`. Every job runs on `ubuntu-latest`, with `permissions: contents: read` at the top. Add this at the top level:

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```

Name the jobs so a red check names the failure:

| Job id / name | Needs | Runs | Fails when |
|---|---|---|---|
| `lint-backend` | none | `./mvnw -B spotless:check` | Java formatting |
| `lint-frontend` | none | `actions/setup-node` with `node-version-file: client/.nvmrc` and npm cache on `client/package-lock.json`; in `client/`: `npm ci`, `npm run lint`, `npm run typecheck` (add `"typecheck": "tsc --noEmit"` to `client/package.json`; check it passes on the current code) | ESLint or TypeScript errors |
| `unit-backend` | none | `./mvnw -B verify -Dskip.frontend -DskipITs -Dspotless.check.skip=true`. Always upload `target/surefire-reports` and `target/site/jacoco` as artifacts (`if: always()`) | A JUnit test or the JaCoCo gate |
| `unit-frontend` | none | setup-node as above; `npm ci`; `npm run coverage -- --reporter=default --reporter=github-actions`. Always upload `client/coverage` | A Vitest test or the coverage thresholds |
| `build` | none | setup-node, setup-java, then `NEXT_PUBLIC_API_URL=http://localhost:8080 ./mvnw -B package -DskipTests -Dskip.lint -Dspotless.check.skip=true -Djacoco.skip=true`. Upload `target/geogames-0.0.1-SNAPSHOT.jar` and `client/out` | The frontend or backend doesn't compile or package. The API override is set because this jar is only for tests; production is built by Render, which the `docker` job covers |
| `integration` | `build` | download the `client/out` artifact into `client/out`; `./mvnw -B verify -Dskip.frontend -DskipUnitTests -Dspotless.check.skip=true -Djacoco.skip=true`. Always upload `target/failsafe-reports` | A backend IT, including the contract test and the data checks |
| `contract-frontend` | none | the Vitest contract test alone (`npx vitest run src/api/contract.test.ts`) | The frontend fixtures no longer match the committed backend shapes |
| `e2e` | `build` | download the jar into `target/`; setup-java (JRE 25 is enough); setup-node with `node-version-file: client/.nvmrc`; in `e2e/`: `npm ci`, `npx playwright install --with-deps chromium`, `npx playwright test`. Always upload `e2e/playwright-report` and `e2e/test-results` | A browser scenario |
| `docker` | none | `docker build -t geogames:ci .`; `docker run -d --memory=512m -p 8080:8080 geogames:ci`; poll `/health` for up to 180 s; then `curl` `/api/countries` (expect more than 200 entries, using `jq`), `POST /api/games` with a 1-round `RANDOM` game, and `GET` its round 0. On failure, print `docker logs`. Use `docker/setup-buildx-action` and the GitHub Actions layer cache (`cache-from: type=gha`) if a cold build takes more than about 5 minutes | The production image doesn't build or doesn't start and serve within 512 MB |
| `data` | none | the regeneration check from phase 6 | Committed processed data is stale |
| `test` | all the above | `if: always()`; fail unless every needed job's result is `success` (check `needs.*.result` with `contains(needs.*.result, 'failure') \|\| contains(needs.*.result, 'cancelled') \|\| contains(needs.*.result, 'skipped')`) | Any of them failed. **Keep the id and name `test`**: it is the required check on `main`, so branch protection keeps working with no settings change |

Notes:
- `unit-backend` doesn't install Node, which proves the Java unit tests don't need the frontend.
- `contract-frontend` duplicates one file of `unit-frontend` on purpose, so a contract break shows up under its own name. If the user would rather not run it twice, exclude it from `unit-frontend` with Vitest's `--exclude`.
- Don't add a `paths` filter. Every job runs on every PR. Use `actions/setup-java` `cache: maven` and setup-node's npm cache to keep things fast.
- Remove nothing from the `pom.xml` default build. A local `mvn verify` must still run everything, as `CLAUDE.md` says.

**Verify:** push the branch only if the user agrees (it's outward-facing). Otherwise check the YAML with `actionlint` if it's available (`go install` or the release binary; skip if neither works) and read it through once against the table above. If pushed: every job is green on the PR, and the job names show in the checks list. Then break one thing at a time on a throwaway commit (a lint error, a failing Vitest test, a failing JUnit test, a renamed `GameData` field) and confirm that only the matching job, and `test`, go red. Revert.

## Phase 6: data and toolchain consistency checks

1. **Data regeneration (`data` job).** Add a Maven profile `regenerate-data` that uses `exec-maven-plugin` (pin the current version) to run, in order, into `target/regen/`:
   - `CountryLoaderMain`, then `CountryShapeMain` from the regenerated countries,
   - `RiverLoaderMain` and `LakeLoaderMain`, using the regenerated countries dir.

   Use the argument lists in `CLAUDE.md`. Skip `TripointLoaderMain` and `WikidataMain`, because they use the network. Then `diff -r` each output dir against `src/main/resources/processed/<same>`, and `git diff --no-index --stat` on failure so the log shows which files changed. The job runs `./mvnw -B -Pregenerate-data -Dskip.frontend -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true compile exec:java@...`, or whatever form the plugin needs. Document the command in `CLAUDE.md` under "Geo data pipeline" as the way to check the data locally.
   - **If the first run shows differences on current `main`, stop.** Don't regenerate or commit data. Report which files differ and how (ordering, floating-point formatting, real changes). That's the user's decision. Line-ending differences mean you should add a `.gitattributes` with `*.json text eol=lf` for `src/main/resources/processed/`, which is allowed, but tell the user.
2. **Node version check.** Add a step to `lint-frontend` that fails if `client/.nvmrc` doesn't match `<nodeVersion>` in `pom.xml` (ignoring the leading `v`). Use a few lines of `grep`/`sed` in the workflow.

**Verify:** `mvn -B -Pregenerate-data ...` passes locally with no diff, and the Node check passes. Change one alias in `geodata/countries/additionalNames.json` and check that the data job fails with a readable diff. Revert.

## Phase 7: production smoke workflow

Add `.github/workflows/smoke-production.yml`, triggered on `workflow_dispatch`, a daily `schedule`, and `deployment_status` (only when `github.event.deployment_status.state == 'success'`). Render posts GitHub deployment statuses when its GitHub app is connected. Ask the user whether it is. If not, keep only the other two triggers. It:
- polls `https://geogames-web-service.onrender.com/health` for up to 3 minutes (the free tier sleeps), then checks that `/api/countries` returns more than 200 entries, and creates and reads a 1-round `RANDOM` game;
- checks that `https://geogauntlet.onrender.com/` returns 200, and that a CORS preflight from that origin to the API is allowed;
- needs no secrets, and makes no changes beyond one throwaway in-memory game.

This is the only workflow allowed to use the network. Say so in a comment at the top of the file.

**Verify:** `actionlint` passes, if available. It can't be run until merged, so say that in the report.

## Phase 8: documentation, and handing over to the user

1. `CLAUDE.md`: update "Commands" with the new properties (`skipUnitTests`, `skipITs`, `skip.lint`, `skip.frontend`), the `*IT` convention, `-Dcontract.update=true`, the e2e commands, the data-regeneration command, and a short "CI" paragraph listing the jobs from the phase 5 table and saying that `test` is the aggregate required check. Update the Git workflow section if anything there changed.
2. Final report to the user (in chat, not a file):
   - Test counts per layer (backend unit, frontend unit, backend IT, contract, e2e) and how long each CI job takes, if it was pushed.
   - **Settings only the user can change, as a checklist:**
     - On both Render services (`geogames-web-service` and `geogauntlet`), set Auto-Deploy to **"After CI checks pass"**, so a red `main` never deploys.
     - In branch protection on `main`, turn on "Require branches to be up to date before merging", so each PR is tested against the latest `main`. `test` stays the required check, and no new checks need adding.
     - Whether the Render static site has a rewrite of `/games/*` to `/games/[gameId].html`. If it doesn't, deep links are broken in production too.
     - Whether Render's GitHub integration posts deployment statuses (for the phase 7 trigger), and the instance's real memory limit, if it isn't 512 MB, so the `docker` job's `--memory` matches it.
   - Every `// Current behaviour:` item and the deep-link result, with a one-line suggested fix each.
   - Whether the data regeneration check passed on first run, and if not, exactly what differed.
