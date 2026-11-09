# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Each area has its own `CLAUDE.md`, which loads when you work in that folder:
- `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md`: the backend (API rules, persistence, stats, game types and families, test conventions)
- `src/main/CLAUDE.md`: configuration, profiles, the production database and Flyway migrations
- `src/main/java/com/parkinson/benjamin/geogames/loaders/CLAUDE.md` (also loaded from `geodata/`): the offline geo data pipeline
- `client/CLAUDE.md`: the frontend (commands, lint, tests, credits, structure, games)
- `e2e/CLAUDE.md`: the Playwright browser tests

## Overview

Geogames: geography quiz games. The backend is Spring Boot 4 / Java 25 / Maven, at the repo root. The frontend is Next.js 16 / React 19 / TypeScript, in `client/`. It uses TanStack Query (`@tanstack/react-query`), react-bootstrap and react-leaflet.

## Working efficiently

- Check your work with the `scripts/check-*.sh` scripts below. They print a few lines on success and only the failures otherwise; the full output is in `target/check-logs/`. Read a log only when the summary isn't enough.
- Read the files (or line ranges) you need rather than `cat`-ing whole folders. `src/main/resources/processed/` (580+ generated JSON files, some a single line of megabytes) and the raw sources in `geodata/` are hidden from ripgrep by `.ignore`. Never print them without a narrow path and `head` or `grep -o`. To list the source files: `git ls-files ':!src/main/resources/processed' ':!geodata'`.
- Every text file is LF in the working tree (`.gitattributes`), except `mvnw.cmd`. Edit scripts don't need to handle CRLF.

## Commands

JAVA_HOME must be a Java 25 JDK. Use `./mvnw` (or `mvn`). For npm commands use the Node that Maven installed (`target/node`, v24): the system Node may be older than Next 16 supports. The check scripts do this for you.

Quick checks (run from anywhere; extra arguments are passed on):
- `bash scripts/check-java.sh`: `spotless:apply`, then the Java unit tests and the JaCoCo gate (no Node). One class or method: `-Dtest=GameServiceTest` or `-Dtest=GameServiceTest#method` (the gate is skipped then).
- `bash scripts/check-frontend.sh`: ESLint (zero warnings), `lint:rules`, `tsc` and Vitest in `client/`. Arguments go to Vitest: `bash scripts/check-frontend.sh src/games/lakes`.
- `bash scripts/check-it.sh`: the integration tests, with the frontend build `HttpServerIT` needs. One class: `-Dit.test=GameApiIT`. Add `-Dskip.frontend` to reuse `client/out` from an earlier build.
- `bash scripts/check-versions.sh` fails unless the versions agree: `<java.version>` in `pom.xml`, every `setup-java` `java-version` in `.github/workflows` and both `temurin` tags in the `Dockerfile`; `<nodeVersion>` in `pom.xml` and `client/.nvmrc`; and the `@types/node` major in `client/` and `e2e/` with the Node major. A Java or Node upgrade changes all of them together. Dependabot ignores `@types/node` majors, and it files a JDK change in a Docker tag as minor, so this check is what makes those PRs fail.
- `bash scripts/check-processed-data.sh`: see the loaders `CLAUDE.md`.

Full builds:
- `./mvnw clean install` builds the frontend too and bundles it into the jar (`-DskipTests` skips all tests). Run it with `java -jar target/geogames-0.0.1-SNAPSHOT.jar` (port 8080). Backend in dev: the main method of `GeogamesApplication`, or `./mvnw spring-boot:run`.
- `./mvnw verify` runs everything: frontend lint, build and Vitest, the Java unit tests, the integration tests, the JaCoCo gate and `spotless:check`. `mvn test` and `mvn install` also run Vitest (a `frontend-maven-plugin` execution in the `test` phase).
- **Unit and integration tests.** Test classes whose name ends in `IT` (`GameApiIT`, `ApiContractIT`, `HttpServerIT`, `ApplicationContextIT`, `ProcessedDataIT`, `RoundRebuildIT`, `PostgresMigrationIT`) are integration tests, run by Failsafe in the `verify` phase (`mvn test` doesn't run them). Every other test class is a unit test for Surefire. The JaCoCo gate counts the unit tests only. Switches, all default `false`: `-DskipUnitTests` (Surefire and the frontend `npm test`), `-DskipITs` (Failsafe), `-Dskip.lint` (`npm run lint`) and `-Dskip.frontend` (Node, `npm ci`, lint, the Next build and Vitest; a bundled jar then needs `client/out` from an earlier build, and `HttpServerIT` is skipped without it). `-DskipTests` skips everything. The old `-Dskip.npm -Dskip.installnodenpm` still works but doesn't skip lint; prefer `-Dskip.frontend`.
- Format Java (google-java-format via Spotless): `./mvnw spotless:apply`; check with `spotless:check`. `mvn verify` runs both lint and `spotless:check`, so a PR fails the required `test` check if either fails.
- **Contract test.** `client/src/test/api-shapes.json` holds the shape of every JSON response the frontend reads (per game type, plus `/api/countries`, `/api/rivers`, `/api/lakes` and `POST /api/games`). `ApiContractIT` fails when the backend's responses stop matching it. Regenerate it with `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend` and commit the file. The frontend's `client/src/api/contract.test.ts` then fails until its fixtures match (see `client/CLAUDE.md`). The shape format is in `JsonShape.java` and `client/src/test/shape.ts`; keep them the same.

## Git workflow

`main` is protected: changes must go through a pull request and the required `test` status check, so pushes to `main` are rejected. Never commit to or push `main`. Create a feature branch first, push that, and open a PR.

## CI

`.github/workflows/ci.yml` runs one job per kind of failure, so a red check names what broke: `lint-backend` (Spotless), `lint-frontend` (ESLint, `npm run lint:rules`, `tsc` for `client/` and `e2e/`, and `scripts/check-versions.sh`), `unit-backend` (Surefire and the JaCoCo gate, no Node), `unit-frontend` (Vitest and its coverage thresholds), `build` (the bundled jar, built with `NEXT_PUBLIC_API_URL=http://localhost:8080`), `integration` (the `*IT` classes against the built frontend), `contract-frontend`, `e2e` (Playwright against the jar), `docker` (builds the production `Dockerfile` image, runs it with `--memory=512m` and calls the API) and `data` (`scripts/check-processed-data.sh`). `test` is the aggregate: it needs all of them and fails unless each succeeded, and it is the one required status check on `main`, so keep its id and name. Pull requests cancel superseded runs. `smoke-production.yml` is the only workflow that touches the network (it calls the deployed services, on demand and daily) and never runs on a PR. Nothing run on a PR may reach the internet, and no test may hit the network.

## Architecture

**Build coupling.** The Maven `frontend-maven-plugin` installs Node 24 into `target/`, then runs `npm ci` and `npm run build` in `client/`. `maven-antrun-plugin` copies `client/out` into `target/classes/public`, so Spring serves the static site and the API from the same jar. The `Dockerfile` does the same build in two stages.

**Environment switching.** `client/src/api/gateway.ts` (`serverEndpoint()`) picks the backend URL from `NODE_ENV`: production uses `https://geogames-web-service.onrender.com`, anything else `localhost:8080`. A non-empty `NEXT_PUBLIC_API_URL` at build time wins over both: the browser tests build the bundled site with `NEXT_PUBLIC_API_URL=http://localhost:8080` so the jar talks to itself. Never set it for the Render build. The allowed CORS origins are `geogames.cors.allowed-origins` in `application.properties`; update it if the hosting changes.

**Storage and privacy.** Games are stored with JPA: in-memory H2 by default (no database or secrets needed), Postgres (Neon) under the `prod` profile on Render. The schema belongs to Flyway, so **any change to an entity needs a new migration**, never an edit to an applied one. A game stores no user data and must not be matchable to a person: never add exact timestamps, client addresses, request logging or browser data to production, and never expose or log `GameEntity.playId`. Details in `src/main/CLAUDE.md` and the backend `CLAUDE.md`.

**Game flow / API** (`GameController` → `GameService`; validation rules in the backend `CLAUDE.md`):
- `POST /api/games` with `{gameType, numberOfRounds}` (1–10). `gameType` is a `GameType` constant or `RANDOM` (a mix of every type). Returns `{id}`, the game's **public id** (a random UUID; the `long` primary key never leaves the server).
- `GET /api/countries`, `GET /api/rivers`, `GET /api/lakes` return `[{name, additionalNames}]` for autocomplete and spell checking, cached.
- `GET /api/games/{id}/rounds/{round}` returns a `GameRound`: the game type, `jsonBlob` (the round data, which the frontend `JSON.parse`s), the total round count, the answer state, `hintsUsed`, `points` and the overall `GameResult` (`score` out of `maxScore`, `roundsCorrect`, `perfectRounds`, per-round `roundPoints` / `roundHintsUsed`).
- `POST /api/games/{id}/rounds/{round}/answers` with `{answerState, hintsUsed, wrongGuesses}` (`CORRECT` / `GAVE_UP` / `OUT_OF_GUESSES`).
- Errors are a `ProblemDetail` JSON body (`controllers/ApiExceptionHandler`).

**The client checks answers, not the server.** The frontend compares guesses with the data in `jsonBlob` and reports only the final `AnswerState`, `hintsUsed` and the wrong-guess count (see `client/CLAUDE.md`).

**Game types.** `model/GameType` is a Java enum with `answerCount` (TRIPOINT=3, 1 for the rest), `hintCount` and `guessLimit` (wrong guesses allowed, at least 1); the client reads all three from the API. The types are `TRIPOINT`, the river family (`RIVERS_BY_SHAPE`, `RIVERS_BY_COUNTRIES`), the country-shape family (`COUNTRIES_BY_NEIGHBOURS`, `COUNTRIES_BY_COASTLINE`, `COUNTRIES_BY_LAND_BORDERS`) and `LAKES_BY_SHAPE`. Adding one takes:
1. A constant in `GameType`.
2. A `GameData` record in `model/` and its loader services, unless an existing record fits.
3. A `RoundGenerator` bean. `GameService` picks it up with no edits, and `RANDOM` mixes it in.
4. A frontend module in `client/src/games/<name>/`, added to `GAME_MODULES` in `games/registry.ts`.

Steps 1–3 are explained in the backend `CLAUDE.md`, step 4 in `client/CLAUDE.md`. Usage statistics need nothing extra.

**Scoring.** A correct round is worth 100 points; hints share a 50-point penalty equally between the type's `hintCount` hints, so answering after all hints scores 50; a revealed round (`GAVE_UP`) scores 0, and wrong guesses cost nothing until the type's `guessLimit` is reached, which ends the round as `OUT_OF_GUESSES` with 0 points. The client reports `hintsUsed`, and the server computes the points when needed (they are never stored). The formula lives in `model/Scoring` and `client/src/games/common/scoring.ts`, which must agree (both tests hold the same table). A new game type only chooses its `hintCount` and `guessLimit`.

**Where things live.** Backend (`com.parkinson.benjamin.geogames`): `api/` (HTTP request and response records), `model/` (domain types), `dao/` (JPA entities and repository), `service/` (game logic, round generators, data loaders), `config/` (Spring configuration), `controllers/`, `helper/`, `loaders/` (offline data programs, not part of the app). Frontend (`client/src`): `api/` (all fetching), `games/common/` (the game-agnostic shell), `games/<name>/` (one game each), `games/registry.ts` (the only list of games), `map/` (shared Leaflet helpers). Data: `geodata/` (raw sources, not in the jar) → `src/main/resources/processed/` (generated by the loaders).
