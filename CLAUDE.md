# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Geogames: geography quiz games. The backend is Spring Boot 4 / Java 17 / Maven, at the repo root. The frontend is Next.js 16 / React 19 / TypeScript, in `client/`. It uses TanStack Query (`@tanstack/react-query`), react-bootstrap and react-leaflet.

## Commands

Backend (repo root; use `./mvnw` or `mvnw.cmd` if `mvn` isn't installed):
- Full build (also builds the frontend and bundles it into the jar): `mvn clean install` (add `-DskipTests` to skip tests)
- Run the packaged app: `java -jar target/geogames-0.0.1-SNAPSHOT.jar`. It serves on port 8080.
- Run the backend in dev: run the main method of `GeogamesApplication` (for example from IntelliJ), or use `mvn spring-boot:run`
- Tests: `mvn test`. Run a single test with `mvn test -Dtest=TripointFinderTest` or `-Dtest=TripointFinderTest#methodName`. `mvn test` and `mvn install` also run the frontend Vitest suite (a `frontend-maven-plugin` execution in the `test` phase), and `-DskipTests` skips both. To run only the Java tests quickly, add `-Dskip.npm -Dskip.installnodenpm` (this skips all the plugin's npm steps, including the frontend build and tests, and needs `client/out` to exist from an earlier build).
- Backend test conventions: JUnit 5, AssertJ and Mockito. Boot 4 moved the test slices out of `spring-boot-test-autoconfigure`, so `@WebMvcTest` / `@AutoConfigureMockMvc` come from `spring-boot-starter-webmvc-test` (package `org.springframework.boot.webmvc.test.autoconfigure`) and `@DataJpaTest` from `spring-boot-starter-data-jpa-test`. Use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`), not `@MockBean`. No test may hit the network. `TripointComputer` is tested against the hand-written HTML in `src/test/resources/fixtures/`.
- Format Java (google-java-format via Spotless): `mvn spotless:apply`. Check formatting with `mvn spotless:check`.

Frontend (`client/`):
- Dev server with hot reload on port 3000: `npm run dev`. It calls the backend at `http://localhost:8080`.
- Lint: `npm run lint`
- Production static export: `npm run build`. This runs `next build` (with `output: "export"` in `next.config.mjs`) and writes to `client/out/`.
- Tests (Vitest + React Testing Library + jsdom): `npm test` (single run), `npm run test:watch`, `npm run coverage` (v8; the Leaflet-rendering map components are excluded). Tests are colocated with their source as `*.test.ts(x)` under `client/src`, and shared helpers (fixtures, a `fetch` router, a query-client wrapper) live in `client/src/test/`. Never put test files under `client/pages/`, because Next would treat them as routes. Test files are type-checked by `next build`, so import `describe`/`it`/`expect`/`vi` from `vitest` explicitly. Never render real Leaflet maps: mock the `*MapWrapper` modules. Testing Library only detects Jest's fake timers, so with `vi.useFakeTimers()` use `fireEvent` instead of `userEvent`. Components use TanStack Query through the provider in `pages/_app.tsx`, so tests wrap them in a `QueryClientProvider` (`renderWithClient` in `client/src/test/render.tsx`).
- Use the Node that Maven installed (`target/node`, v24) for npm commands. The system Node may be older than Next 16 supports.

## Architecture

**Build coupling.** The Maven `frontend-maven-plugin` installs Node 24 into `target/`, then runs `npm ci` and `npm run build` in `client/`. `maven-antrun-plugin` copies `client/out` into `target/classes/public`, so Spring serves the static site and the API from the same jar. The `Dockerfile` does the same build in two stages.

**Environment switching.** `client/src/api/gateway.ts` (`serverEndpoint()`) picks the backend URL from `NODE_ENV`: production uses `https://geogames-web-service.onrender.com`, anything else uses `localhost:8080`. CORS origins are hard-coded in `@CrossOrigin` on both `GameController` and `CountryController`. Update both if the hosting changes.

**Game flow / API** (`GameController` → `GameService`):
- `POST /api/games` with `{gameType, numberOfRounds}` (1–10). `gameType` is `TRIPOINT`, `RIVERS_BY_SHAPE`, or `RANDOM` (a mix of both).
- `GET /api/countries` returns `[{name, additionalNames}]` for every processed country. It is used for Tripoint autocomplete and guess validation, and is cached.
- `GET /api/games/{id}/rounds/{round}` returns a `GameRound`: the game type, `jsonBlob`, the total round count, the answer state and the overall `GameResult`.
- `POST /api/games/{id}/rounds/{round}/answers` with `{answerState}` (`CORRECT` / `GAVE_UP`).
- Games are stored with JPA in an **in-memory H2** database (`application.properties`), so they are lost on restart. Each `GameRoundEntity` stores its round data as a serialized JSON string (`jsonBlob`, a `GameData` implementation such as `Tripoint` or `River`). The frontend `JSON.parse`s it.
- **The client checks answers, not the server.** The frontend compares guesses against the data in `jsonBlob`, including `additionalNames` aliases normalised by `stringHelper.normaliseString`, and only reports the final `AnswerState` to the server. The matching lives in `client/src/games/common/answers.ts` (`matchAnswer`) and the guess rules (duplicates, aliases, unknown names, hints) in the pure `guessReducer.ts` next to it.

**Game types.** `dao/GameType` is a class with static constants, not a Java enum. It has a name and an `answerCount` (TRIPOINT=3, RIVERS_BY_SHAPE=1) and is persisted through `GameTypeConverter`. Adding a game type takes changes in several places:
1. A constant in `GameType` plus an entry in `values()`.
2. A `GameData` model and loader/finder services, wired into `GameService.createGame`, including the `RANDOM` branch.
3. Frontend: create `client/src/games/<name>/` with a module built by `defineGame` (its fields are described in `games/types.ts`: the backend `type`, titles, description, icon, component and `answerNames`), a component (usually built on the generic `games/common/MapGame`), and a `*Map.tsx` + `*MapWrapper.tsx` pair that takes `MapProps<T>`. Then add the module to `GAME_MODULES` in `games/registry.ts`. Nothing in `games/common/`, `GameRoundPage`, `GameSummary` or `HomePage` needs to change. The component gives `MapGame` its answers as `AnswerCandidate[]` (a name plus aliases), and matching (aliases, duplicates) is handled centrally.

**Leaflet and SSR.** Leaflet can't render on the server. Each map has a `*MapWrapper.tsx` that is just a `dynamic(() => import("./X"), { ssr: false, ... })` call (keep the `import(...)` literally in that file), and the real `*Map.tsx` takes `MapProps<T>` (`data`, `gameOver`, `gaveUp`, `hintsUsed`). Follow this pattern for new maps.

**Frontend structure** (`client/src`). `api/` owns fetching (`fetchJson`), query keys, hooks and the one `JSON.parse` of `jsonBlob`, and components never call `fetch`. `games/common/` is the game-agnostic shell (round page, summary, `MapGame`, answer matching and guess reducer). Each `games/<name>/` folder holds one game: its module, component, model types and maps. `games/registry.ts` is the only list of games. `map/` holds the shared Leaflet helpers. The `QueryClientProvider` lives in `pages/_app.tsx`.

**Geo data pipeline**:
- `geodata/countries/` (repo root, **not** in the jar) holds the raw sources: `countries.geo.json` (Natural Earth) and `additionalNames.json` (aliases). `src/main/resources/data/rivers/` still holds the raw river data, loaded at runtime. `processRiverImport.sh` strips extra properties from raw river exports.
- `src/main/resources/processed/` holds files generated offline by standalone `main` programs, not part of the Spring app:
  - `loaders/countries/CountryLoaderMain <countries.geo.json> <additionalNames.json> <outDir>` writes `processed/countries/*.json` (one `Country` per file). Editing `additionalNames.json` means re-running this.
  - `loaders/tripoint/TripointLoaderMain <processedCountriesDir> <outFile>` scrapes Wikipedia's list of tripoints and writes the single file `processed/tripoints.json`, an array of `{coordinate, countryNames}`. Each Wikipedia coordinate is snapped by `TripointSnapper` to the vertex where the three countries' borders meet. A tripoint with no such vertex within 30 km is dropped. The tripoint count comes from the file, so there's nothing to update by hand.
- At runtime `TripointFinderService` picks random definitions from `tripoints.json` and loads each country with `CountryLoaderService.loadCountryByName`, so the `jsonBlob` sent to the frontend still contains full country geometry.
- Rivers are loaded at runtime by classpath glob and cached (`@Cacheable("rivers")`).
