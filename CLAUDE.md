# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Geogames: geography quiz games. The backend is Spring Boot 3 / Java 17 / Maven, at the repo root. The frontend is Next.js 13 / React 18 / TypeScript, in `client/`. It uses react-query, react-bootstrap and react-leaflet.

## Commands

Backend (repo root; use `./mvnw` or `mvnw.cmd` if `mvn` isn't installed):
- Full build (also builds the frontend and bundles it into the jar): `mvn clean install` (add `-DskipTests` to skip tests)
- Run the packaged app: `java -jar target/geogames-0.0.1-SNAPSHOT.jar`. It serves on port 8080.
- Run the backend in dev: run the main method of `GeogamesApplication` (for example from IntelliJ), or use `mvn spring-boot:run`
- Tests: `mvn test`. Run a single test with `mvn test -Dtest=TripointFinderTest` or `-Dtest=TripointFinderTest#methodName`
- Format Java (google-java-format via Spotless): `mvn spotless:apply`. Check formatting with `mvn spotless:check`.

Frontend (`client/`):
- Dev server with hot reload on port 3000: `npm run dev`. It calls the backend at `http://localhost:8080`.
- Lint: `npm run lint`
- Production static export: `npm run build`. This runs `next build && next export` and writes to `client/out/`.
- The frontend has no tests. Ignore `client/README.md`; it is leftover Create React App boilerplate.

## Architecture

**Build coupling.** The Maven `frontend-maven-plugin` installs Node 18 into `target/`, then runs `npm install` and `npm run build` in `client/`. `maven-antrun-plugin` copies `client/out` into `target/classes/public`, so Spring serves the static site and the API from the same jar. The `Dockerfile` does the same build in two stages.

**Environment switching.** `client/src/api/gateway.ts` (`serverEndpoint()`) picks the backend URL from `NODE_ENV`: production uses `https://geogames-web-service.onrender.com`, anything else uses `localhost:8080`. CORS origins are hard-coded in `@CrossOrigin` on `GameController`. Update both if the hosting changes.

**Game flow / API** (`GameController` → `GameService`):
- `POST /api/games` with `{gameType, numberOfRounds}` (1–10). `gameType` is `TRIPOINT`, `RIVERS_BY_SHAPE`, or `RANDOM` (a mix of both).
- `GET /api/games/{id}/rounds/{round}` returns a `GameRound`: the game type, `jsonBlob`, the total round count, the answer state and the overall `GameResult`.
- `POST /api/games/{id}/rounds/{round}/answers` with `{answerState}` (`CORRECT` / `GAVE_UP`).
- Games are stored with JPA in an **in-memory H2** database (`application.properties`), so they are lost on restart. Each `GameRoundEntity` stores its round data as a serialized JSON string (`jsonBlob`, a `GameData` implementation such as `Tripoint` or `River`). The frontend `JSON.parse`s it.
- **The client checks answers, not the server.** The frontend compares guesses against the data in `jsonBlob`, including `additionalNames` aliases normalised by `stringHelper.normaliseString`, and only reports the final `AnswerState` to the server.

**Game types.** `dao/GameType` is a class with static constants, not a Java enum. It has a name and an `answerCount` (TRIPOINT=3, RIVERS_BY_SHAPE=1) and is persisted through `GameTypeConverter`. Adding a game type takes changes in several places:
1. A constant in `GameType` plus an entry in `values()`.
2. A `GameData` model and loader/finder services, wired into `GameService.createGame`, including the `RANDOM` branch.
3. Frontend: a component in `client/src/games/` (usually built on the generic `games/common/MapGame`), a map component in `client/src/map/`, and branches in `GameRoundPage.renderGame` / `renderTitle`.

**Leaflet and SSR.** Leaflet can't render on the server. Each map has a `*MapWrapper.tsx` that loads the real `*Map.tsx` with `next/dynamic` and `ssr: false`. Follow this pattern for new maps.

**Geo data pipeline** (`src/main/resources`):
- `data/` holds the raw sources: `countries/countries.geo.json`, `countries/additionalNames.json` (aliases), `rivers/*.geo.json` and `rivers/additionalData.json`.
- `processed/` holds files generated offline. These are standalone `main` programs, not part of the Spring app:
  - `loaders/countries/CountryLoaderMain <outDir>` writes `processed/countries/*.json`.
  - `loaders/tripoint/TripointLoaderMain <outDir>` computes tripoints from country borders (`TripointComputer`) and writes `processed/tripoints/{1..N}.json`.
- `TripointLoaderService.countTripoints()` **hard-codes 170**, because files can't be counted inside a jar. Update it if you regenerate tripoints.
- Rivers are loaded at runtime by classpath glob and cached (`@Cacheable("rivers")`). `data/rivers/processRiverImport.sh` strips extra properties from raw river exports.
