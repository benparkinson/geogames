# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Each area has its own `CLAUDE.md`, which loads when you work in that folder:
- `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md`: the backend (API rules, persistence, stats, game types and families, test conventions)
- `src/main/CLAUDE.md`: configuration, profiles, the production database and Flyway migrations
- `src/main/java/com/parkinson/benjamin/geogames/loaders/CLAUDE.md` (also loaded from `geodata/`): the offline geo data pipeline
- `client/CLAUDE.md`: the frontend (commands, lint, tests, credits, structure, games)
- `e2e/CLAUDE.md`: the Playwright browser tests
- `.github/CLAUDE.md`: the CI jobs and workflow rules

## Overview

Geogames: geography quiz games. The backend is Spring Boot 4 / Java 25 / Maven, at the repo root. The frontend is Next.js 16 / React 19 / TypeScript, in `client/`. It uses TanStack Query (`@tanstack/react-query`) and react-leaflet, with plain CSS and design tokens.

## Working efficiently

- Check your work with the `scripts/check-*.sh` scripts below. They print a few lines on success and only the failures otherwise; the full output is in `target/check-logs/`. Read a log only when the summary isn't enough.
- **One PR per session.** Every call re-sends the whole conversation, so work carried over from one PR is paid for again on every call of the next. After raising a PR, stop: report it and suggest a new session (`/clear`) for the next phase, naming the plan and phase to start it with. Start the next PR in the same session only when the user asks.
- Read the files (or line ranges) you need with `Read`. Don't `cat` several whole files in one Bash command or `cat` whole folders; in a large file, `grep -n` for the symbol and read that range. When a tool's output is too large and is saved to a file, `grep` it or read a range of it, never the whole file.
- **Keep the CLAUDE.md files lean**: they load on every call. Add a line only when Claude would get something wrong without it (a rule, a trap, a non-obvious command). What the code does belongs in the code and its tests, so prefer replacing or deleting a line to adding one.
- **Plans** in `docs/plans/` stay under about 8 KB: the decisions, the files to change and the tests, without restating a `CLAUDE.md`. Each phase (one PR) carries enough to be implemented in a fresh session.
- `src/main/resources/processed/` (580+ generated JSON files, some a single line of megabytes) and the raw sources in `geodata/` are hidden from ripgrep by `.ignore`. Never print them without a narrow path and `head` or `grep -o`. To list the source files: `git ls-files ':!src/main/resources/processed' ':!geodata'`.
- `client/public/tiles/` holds about 5,500 committed JPEG map tiles (the relief map) and is hidden from ripgrep by `.ignore`.
- Every text file is LF in the working tree (`.gitattributes`), except `mvnw.cmd`. Edit scripts don't need to handle CRLF.

## Commands

JAVA_HOME must be a Java 25 JDK. Use `./mvnw` (or `mvn`). For npm commands use the Node that Maven installed (`target/node`, v24): the system Node may be older than Next 16 supports. The check scripts do this for you.

Quick checks (run from anywhere; extra arguments are passed on):
- `bash scripts/check-java.sh`: `spotless:apply`, then the Java unit tests and the JaCoCo gate (no Node). One class or method: `-Dtest=GameServiceTest` or `-Dtest=GameServiceTest#method` (the gate is skipped then).
- `bash scripts/check-frontend.sh`: ESLint (zero warnings), `lint:rules`, `tsc` and Vitest in `client/`. Arguments go to Vitest: `bash scripts/check-frontend.sh src/games/lakes`.
- `bash scripts/check-it.sh`: the integration tests, with the frontend build `HttpServerIT` needs. One class: `-Dit.test=GameApiIT`. Add `-Dskip.frontend` to reuse `client/out` from an earlier build.
- `bash scripts/check-versions.sh` fails unless the versions agree: `<java.version>` in `pom.xml`, every `setup-java` `java-version` in `.github/workflows` and both `temurin` tags in the `Dockerfile`; `<nodeVersion>` in `pom.xml` and `client/.nvmrc`; and the `@types/node` major in `client/` and `e2e/` with the Node major. A Java or Node upgrade changes all of them together. Dependabot ignores `@types/node` majors, and it files a JDK change in a Docker tag as minor, so this check is what makes those PRs fail.
- `bash scripts/check-no-skipped-tests.sh` fails on a committed `.only`, `.skip`, `.todo` or `.fixme` in the frontend or browser tests, a second `test.fail()` in `e2e/` and a `@Disabled` Java test.
- `bash scripts/check-processed-data.sh`: see the loaders `CLAUDE.md`.

Full builds:
- `./mvnw clean install` builds the frontend too and bundles it into the jar (`-DskipTests` skips all tests). Run it with `java -jar target/geogames-0.0.1-SNAPSHOT.jar` (port 8080). Backend in dev: the main method of `GeogamesApplication`, or `./mvnw spring-boot:run`.
- `./mvnw verify` runs everything: frontend lint, build and Vitest, the Java unit tests, the integration tests, the JaCoCo gate and `spotless:check`. `mvn test` and `mvn install` also run Vitest (a `frontend-maven-plugin` execution in the `test` phase).
- **Unit and integration tests.** Test classes whose name ends in `IT` are integration tests, run by Failsafe in the `verify` phase (`mvn test` doesn't run them). Every other test class is a unit test for Surefire. The JaCoCo gate counts the unit tests only. Switches, all default `false`: `-DskipUnitTests` (Surefire and the frontend `npm test`), `-DskipITs` (Failsafe), `-Dskip.lint` (`npm run lint`) and `-Dskip.frontend` (Node, `npm ci`, lint, the Next build and Vitest; a bundled jar then needs `client/out` from an earlier build, and `HttpServerIT` is skipped without it, except in CI, where a missing site fails it). `-DskipTests` skips everything. The old `-Dskip.npm -Dskip.installnodenpm` still works but doesn't skip lint; prefer `-Dskip.frontend`.
- Format Java (google-java-format via Spotless): `./mvnw spotless:apply`; check with `spotless:check`. `mvn verify` runs both lint and `spotless:check`, so a PR fails the required `test` check if either fails.
- **Contract test.** `client/src/test/api-shapes.json` holds the shape of every JSON response the frontend reads (per game type, plus `/api/countries`, `/api/rivers`, `/api/lakes` and `POST /api/games`, `POST /api/games/{id}/rounds/{round}/answers`). `ApiContractIT` fails when the backend's responses stop matching it. Regenerate it with `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend` and commit the file. The frontend's `client/src/api/contract.test.ts` then fails until its fixtures match (see `client/CLAUDE.md`). The shape format is in `JsonShape.java` and `client/src/test/shape.ts`; keep them the same.

## Git workflow

`main` is protected: changes must go through a pull request and the required `test` status check, so pushes to `main` are rejected. Never commit to or push `main`. Create a feature branch first, push that, and open a PR.

The skills in `.claude/skills/` hold the usual workflows: `/write-plan` (a plan for a fresh session), `/ship-plan` (a plan or task on its own worktree, through to a PR) and `/fix-pr` (red checks, merge conflicts, stacks).

**Keep PRs easy to review.** Plan several focused PRs before starting; each passes CI alone and leaves the product working. A feature that crosses layers usually splits into: 1. the backend (the schema, the new API field, which starts optional, `api-shapes.json` and the client fixture the contract test needs); 2. separate backend concerns such as usage statistics, with their own migration; 3. the frontend that uses the field. Refactors and test-helper changes go first, in their own PR. Don't split a change that is already small or whose parts make no sense apart; if a PR is still large, say why in its description.

## CI

`.github/workflows/ci.yml` runs one job per kind of failure, so a red check names what broke; `test` is the aggregate and the one required status check on `main`. Nothing run on a PR may reach the internet, and no test may hit the network. The jobs and the workflow rules are in `.github/CLAUDE.md`.

## Architecture

**Build coupling.** The Maven `frontend-maven-plugin` installs Node 24 into `target/`, then runs `npm ci` and `npm run build` in `client/`. `maven-antrun-plugin` copies `client/out` into `target/classes/public`, so Spring serves the static site and the API from the same jar. The `Dockerfile` does the same build in two stages.

**Theme.** `client/src/theme/tokens.css` is the only place for colours, fonts, radii, shadows and durations (a light block and a dark block). Components use the semantic `--gg-color-*` tokens, never a raw colour. Controls come from `client/src/ui/` (`Button` with variants by meaning, `buttonClass` for links that look like buttons, `ToggleGroup`, `Spinner`, `Notice`, `Chip`), styled in `theme/components.css`. `npm run lint` also runs `scripts/check-theme.mjs`, which rejects colour literals, `react-bootstrap` and Bootstrap class names. `theme/base.css` is our own small reset; layout uses `gg-<component>__<part>` classes in `theme/components.css` built from the spacing and type tokens. Map colours live in `client/src/map/palette.ts` (they don't follow light/dark), icons in `client/src/ui/icons.ts`. State a style depends on goes on a `data-*` attribute that the CSS selects (`[data-outcome="missed"]`), so tests check those, roles and text, never a styling class.

**Environment switching.** `client/src/api/gateway.ts` (`serverEndpoint()`) picks the backend URL from `NODE_ENV`: production uses `https://geogames-web-service.onrender.com`, anything else `localhost:8080`. A non-empty `NEXT_PUBLIC_API_URL` at build time wins over both: the browser tests build the bundled site with `NEXT_PUBLIC_API_URL=http://localhost:8080` so the jar talks to itself. Never set it for the Render build. `config/WebConfig` forwards `/privacy` to `privacy.html` and `/daily` to `daily.html`, so those pages load directly from the jar (and `/daily`, the daily challenge's share link, survives a reload) (`/games/{id}` has no such forward). The allowed CORS origins are `geogames.cors.allowed-origins` in `application.properties`; update it if the hosting changes.

**Storage and privacy.** Games are stored with JPA: in-memory H2 by default (no database or secrets needed), Postgres (Neon) under the `prod` profile on Render. The schema belongs to Flyway, so **any change to an entity needs a new migration**, never an edit to an applied one. The `daily_challenge` table holds one row per date with the round keys of that day's challenge (dates and place keys only, no user data, never deleted). The `challenge` table (challenge a friend) holds a finished game's rounds, mode, difficulty, score and creation date (a date, no time), with no link to the game or any player, and is deleted after 7 days. The `daily_score_by_difficulty` table holds one row per date and difficulty with running totals of finished daily games (a count, a score sum and 11 score buckets: no per-player rows, never deleted). A game stores no user data and must not be matchable to a person (its rounds keep the guesses made, but only as known place names in the lists' spelling, never the text as typed; unknown text is rejected): never add exact timestamps, client addresses, request logging or browser data to production, and never expose or log `GameEntity.playId`. The `/privacy` page describes what is stored and who receives requests, and must be updated when that changes. Details in `src/main/CLAUDE.md` and the backend `CLAUDE.md`.

**API.** `GameController` → `GameService` under `/api`: games (`POST /api/games`, then each round's data, guesses, hints and answers), the daily challenge (`POST /api/daily-games`, `GET /api/daily-scores/{date}`) and the name lists for autocomplete (`/api/countries`, `/api/rivers`, `/api/lakes`). A game's id is a random UUID; the `long` primary key never leaves the server. Errors are a `ProblemDetail` body. Each endpoint's request and response is in the backend `CLAUDE.md` ("API"), and the response shapes the client reads are in `client/src/test/api-shapes.json`.

**The client checks answers, not the server.** The frontend compares guesses with the data in `jsonBlob` and reports only the final `AnswerState`, `hintsUsed` and the wrong-guess count (see `client/CLAUDE.md`).

**Game types.** `model/GameType` is a Java enum with `answerCount` (TRIPOINT=3, the two pair games 2, 1 for the rest), `hintCount` and `guessLimit` (wrong guesses allowed, at least 1); the client reads all three from the API. The types are `TRIPOINT`, the river family (`RIVERS_BY_SHAPE`, `RIVERS_BY_COUNTRIES`), the country-shape family (`COUNTRIES_BY_NEIGHBOURS`, `COUNTRIES_BY_COASTLINE`, `COUNTRIES_BY_LAND_BORDERS`), `LAKES_BY_SHAPE`, the pair games (`BORDERS_BY_SHAPE`, `MERGED_COUNTRIES`: two neighbouring countries) and the games that draw data inside a country (`COUNTRIES_BY_CITIES`, `COUNTRIES_BY_RIVERS`). Adding one takes:
1. A constant in `GameType`.
2. A `GameData` record in `model/` and its loader services, unless an existing record fits.
3. A `RoundGenerator` bean. `RoundGenerators` picks it up with no edits, and `RANDOM` mixes it in.
4. A frontend module in `client/src/games/<name>/`, added to `GAME_MODULES` in `games/registry.ts`.

Steps 1–3 are explained in the backend `CLAUDE.md`, step 4 in `client/CLAUDE.md`. Usage statistics need nothing extra.

**Scoring.** A correct round is worth 100 points; hints share a 50-point penalty equally between the type's `hintCount` hints, so answering after all hints scores 50; a revealed round (`GAVE_UP`) earns its share of what a correct answer would have earned, `answersFound` of the type's `answerCount` answers, rounded half up (2 of 3 countries in a tripoint earns 2/3), so a single-answer round or one with nothing found scores 0; wrong guesses cost nothing until the type's `guessLimit` is reached, which ends the round as `OUT_OF_GUESSES`, scored the same way as a revealed round. The client reports `answersFound`, and the server takes the larger of that and the correct guesses it recorded, stored in `game_round_entity.answers_found` (`NULL`: answered before partial credit, which scores 0 unless correct). The client reports `hintsUsed`, but the server charges at least the hints it recorded (`hintsTakenAt`), and computes the points when needed (they are never stored). The formula lives in `model/Scoring` and `client/src/games/common/scoring.ts`, which must agree (both tests hold the same table). A new game type only chooses its `hintCount` and `guessLimit`.

**Where things live.** Backend (`com.parkinson.benjamin.geogames`): `api/` (HTTP request and response records), `model/` (domain types), `dao/` (JPA entities and repository), `service/` (game logic, round generators, data loaders), `config/` (Spring configuration), `controllers/`, `helper/`, `loaders/` (offline data programs, not part of the app). Frontend (`client/src`): `api/` (all fetching), `games/common/` (the game-agnostic shell), `games/<name>/` (one game each), `games/registry.ts` (the only list of games), `map/` (shared Leaflet helpers). Data: `geodata/` (raw sources, not in the jar) → `src/main/resources/processed/` (generated by the loaders).

**Memory.** The production container has 512 MB and the JVM runs with `-Xmx400M` (`Dockerfile`; the CI `docker` job runs the image with `--memory=512m`): don't change either number. Static data is held in memory only when it is small (the country shapes, the cities as primitive records, the rivers index); the large per-country river geometry is read from its file each time a round is built and never cached (see the backend and loaders `CLAUDE.md`). Never cache anything keyed by a round, a pair or a game. After a data or loader change, run `bash scripts/measure-heap.sh` (it needs a built jar, plays every game type and prints the live heap after a GC) and compare with `docs/plans/new-country-games.md` ("Results": about 42.6 MB before the four country games, 43.6 MB after); a new game or data set should add well under 5 MB.
