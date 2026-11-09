# Plan: comprehensive frontend and backend unit tests

Read `CLAUDE.md` first. Work on a new branch, `unit-tests`, created from `main` (which already contains the dependency updates the tests target). Do not commit unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Do not start the next phase until that step passes.** If a step fails and the fix is not obvious after two attempts, stop and report to the user with the error output.

## Ground rules

- **Do not change production behaviour.** The only production-code changes allowed are the testability refactors listed in this plan (phase 3.1). If a test exposes a bug, write the test so it asserts the **current** behaviour, put a `// Current behaviour:` comment above the assertion explaining what looks wrong, and add the item to your final report. Do not fix it. Known suspects are listed in the "Behaviour to pin" notes below.
- No network in tests. No test may hit Wikipedia, tile servers or `onrender.com`.
- Tests must be deterministic. Several services use an unseeded `Random`. Don't refactor them. Assert properties that hold for any random outcome (sizes, distinctness, membership), not specific picks.
- Don't paper over failures: no `@Disabled`, `it.skip`, `-DskipTests`, `@ts-ignore`, `eslint-disable` (other than a one-line disable with a reason for a genuine test-only false positive), or deleted assertions.
- Java: JUnit 5 + AssertJ + Mockito (all come with `spring-boot-starter-test`). Prefer AssertJ `assertThat` in new tests. Package-private test classes and methods (`class FooTest`, `void doesX()`). Name tests by behaviour (`rejectsZeroRounds`, not `test1`). Run `mvn spotless:apply` before each Java verification step.
- Frontend: Vitest + React Testing Library. Query by role/label/text (what the user sees), not by CSS class, except where the class is the only signal (for example `round-dot--correct`).
- Pin exact versions in `pom.xml`. For npm use caret ranges of the version that `npm view <pkg> version` reports today.

## Where things stand (checked on 2026-11-07)

**Backend.** `src/test/java/...` has 9 test classes:
- `GeogamesApplicationTests`: context loads.
- `TripointHelperTest`, `GeoGeometryTest`, `TripointSnapperTest`: decent pure unit tests. Extend, don't rewrite.
- `GameServiceTest`: only covers `calculateGameResult` (2 cases).
- `TripointFinderTest`: one case (too many requested).
- `CountryLoaderServiceTest`, `ProcessedDataIntegrityTest`: run against the real processed data (255 countries, 164 tripoints).
- `service/TripointComputerTest`: `@Disabled("hits Wikipedia")`, asserts nothing, and sits in the wrong package. Phase 3 replaces it.

Untested: both controllers, `HealthController`, `GameService.createGame` / `getGameRound` / `submitAnswer`, `GameTypeConverter`, `GameRepository` persistence, `RiverFinderService`, `RiverLoaderService`, `TripointLoaderService`, `CountryHelper`, `OptionalHelper`, `RawCountryReader`, `ProcessedCountryReader`, `CountryWriter`, `TripointWriter`, `TripointComputer` parsing, `Tripoint.TripointBuilder`.

**Boot 4 test slices.** In Spring Boot 4 the test slices moved out of `spring-boot-test-autoconfigure` into per-technology modules. `@WebMvcTest` / `@AutoConfigureMockMvc` need `spring-boot-starter-webmvc-test`, and `@DataJpaTest` needs `spring-boot-starter-data-jpa-test`. Both are managed by the Boot BOM (no `<version>`). `@MockBean` is gone. Use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`). Check the exact package names in the downloaded jars instead of guessing (for example `unzip -l` the jar from `~/.m2`, or let your IDE resolve the import).

**Frontend.** No test framework, no tests. `client/README.md` is stale boilerplate. Ignore it. Things that matter for testing:
- `tsconfig.json` includes `**/*.ts(x)`, so `next build` type-checks test files too. Test files must compile with `strict: false` and must import from `vitest` explicitly (no implicit globals).
- **Never put test files under `client/pages/`.** Next would treat them as routes.
- jsdom lacks `window.matchMedia` (used by `MapGameInput`) and `ResizeObserver` (used by `MapResizer`). Stub them in the setup file.
- `GameRoundPage` reads with `useQuery` (the provider's client) but invalidates through the **singleton** `client` exported from `src/api/gateway.ts`. Tests of `GameRoundPage` must wrap it in `<QueryClientProvider client={client}>` with that singleton, and call `client.clear()` in `afterEach`. Otherwise the refetch after an answer never happens.
- `useCountryNames` hard-codes `retry: 1` on the query, which overrides `QueryClient` defaults. Error-path tests for it must make **both** fetch calls fail.

## Phase 0: baseline

1. `git checkout -b unit-tests` (from `main`).
2. Run `mvn test` and note pass/fail/skip counts in your notes (not a file). Expect 1 skipped (`TripointComputerTest`).
3. In `client/`: `npm ci`, `npm run lint`, `npm run build`. All must pass before you change anything. If one doesn't, stop and report.

## Phase 1: backend test dependencies

1. Add test-scoped `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` to `pom.xml`, no versions.
2. Don't add a test `application.properties`. The main one (in-memory H2) works for tests. Add test property files only if a specific test needs one.

**Verify:** `mvn test` passes with the same counts as phase 0, and `mvn dependency:tree -Dscope=test` shows both new starters resolved to 4.1.x.

## Phase 2: backend pure unit tests

No Spring context in this phase. Construct classes directly, and use Mockito for collaborators. Put each test in the same package as the class under test.

### 2.1 `dao/GameTypeConverterTest`
- `convertToDatabaseColumn` returns `"TRIPOINT"` / `"RIVERS_BY_SHAPE"`.
- Round trip: for every `GameType.values()`, `convertToEntityAttribute(convertToDatabaseColumn(t))` is the same instance (`isSameAs`).
- Behaviour to pin: an unknown string and `null` both map to `RIVERS_BY_SHAPE`. `convertToDatabaseColumn(null)` throws `NullPointerException`. Assert both, mark them `// Current behaviour:`, and report them.

### 2.2 `dao/GameTypeTest`
- `values()` contains exactly `TRIPOINT` and `RIVERS_BY_SHAPE`. Answer counts are 3 and 1. Names are unique.

### 2.3 `helper/CountryHelperTest`
Parameterised (`@ParameterizedTest` + `@CsvSource`): `"France" → "France.json"`, `"United States of America" → "UnitedStatesofAmerica.json"`, `"St. Lucia"` (spaces and dots removed), a name with an accent such as `"Curaçao"` (kept). Also: every country name from `CountryLoaderService.loadCountryNames()` maps to a file that exists on the classpath (this catches name/file drift).

### 2.4 `helper/OptionalHelperTest`
Both present → applied. Either empty → empty. The function is not called when either is empty (verify with a counter or a Mockito mock `BiFunction`).

### 2.5 `helper/TripointHelperTest` (extend)
Add: `MultiLineString` input. Multiple rings/polygons are flattened in order. Integer coordinates (`List.of(10, 20)`) convert to doubles. Empty geometry gives an empty list.

### 2.6 `model/geojson/GeoGeometryTest` (extend)
Add: the constructor rejects `"Point"`, `"LineString"` and `"Feature"` with `IllegalArgumentException`. Empty coordinate lists flatten to empty. Jackson deserialises a small `Polygon` and a small `MultiPolygon` JSON string into a `GeoGeometry` whose `flattenedCoordinates()` is correct (use `JsonMapper.builder().build()` from `tools.jackson.databind.json`).

### 2.7 `model/TripointTest`
- `TripointBuilder` builds the right `Coordinate(latitude, longitude)` (watch the order: latitude first) and collects countries. Adding the same country twice keeps one.
- `Tripoint.gameType()` is `TRIPOINT`, and `River.gameType()` is `RIVERS_BY_SHAPE`.

### 2.8 `service/GameServiceTest` (extend)
Build `GameService` with mocks for `GameRepository`, `TripointFinderService`, `RiverLoaderService`, `RiverFinderService`, and a **real** `JsonMapper.builder().build()`. Add a small fixture factory in the test for a `Tripoint` (three tiny square countries, reuse the `square(...)` idea from `TripointSnapperTest`) and a `River`. Stub `gameRepository.save(any())` to return its argument with an id set.

`createGame`:
- Rejects 0 and 11 rounds with `IllegalArgumentException`. Accepts 1 and 10 (boundaries).
- Unknown game type (`"FOO"`, `null`, lowercase `"tripoint"`) throws `IllegalArgumentException`. No call to `save`.
- `TRIPOINT` with N rounds: `findRandomTripoints(N)` called once. The river services are never called. Capture the saved `GameEntity` with `ArgumentCaptor`: N rounds, indices `0..N-1`, every `gameType` is `TRIPOINT`, every `answerState` is `UNANSWERED`, and each `jsonBlob` parses (with Jackson) into an object with a `countries` array of size 3 and a `coordinate`.
- `RIVERS_BY_SHAPE`: same shape of assertions. `jsonBlob` has `name`, `additionalNames`, `continent`, `geoData`. `findRandomRivers(rivers, N)` is called with the list that `loadRivers()` returned.
- `RANDOM` with N rounds (stub both finders with `thenAnswer` that returns as many items as asked, **including 0**): N rounds in total, indices `0..N-1`, and the counts passed to the two finders (capture them) sum to N. Repeat the test with `@RepeatedTest(20)` so both branches of the random split get exercised.
- Returns `GameCreationResponse` with the saved id.

`getGameRound`:
- Game not found → empty. Round index not present (including -1 and `totalRoundCount`) → empty.
- Found: returns the round's `gameType`, `jsonBlob`, `answerState`, the total round count and a matching `GameResult`.

`submitAnswer`:
- Not found / bad round → empty, `save` never called.
- Sets the answer state on the right round only, calls `save` once, and the returned `GameResult` reflects the new state (CREATED → IN_PROGRESS → FINISHED across a 2-round game).
- Behaviour to pin: answering an already-answered round overwrites it (CORRECT → GAVE_UP succeeds), and `UNANSWERED` is accepted as an answer. Assert, comment, report.

`calculateGameResult` (extend the existing tests):
- `Optional.empty()` → `FINISHED`, 0 unanswered, score 0, empty list. This is odd, so pin and report it.
- All unanswered → `CREATED`.
- Score counts only `CORRECT`, not `GAVE_UP`.

### 2.9 `service/TripointFinderTest` (extend)
Use a mocked `TripointLoaderService` returning ~5 `TripointDefinition`s and a mocked `CountryLoaderService` that returns a `Country` named after its argument.
- Returns exactly `howMany` tripoints. All distinct (compare coordinates). Each is built from one of the definitions, with the matching three countries loaded by name (verify `loadCountryByName` calls).
- `howMany == definitions.size()` returns all of them (every definition appears once).
- `howMany == 0` returns empty without loading countries.
- An `IOException` from `loadCountryByName` propagates.

### 2.10 `service/RiverFinderServiceTest`
- Returns `howMany` distinct rivers, all from the input list.
- `howMany == rivers.size()` returns every river.
- `howMany > size` throws `IllegalArgumentException` with both numbers in the message.
- `howMany == 0` returns empty.
- Behaviour to pin: duplicate equal rivers in the input with `howMany == size` would loop forever. **Don't write that test.** Note the risk in the report instead.

### 2.11 `service/RiverLoaderServiceTest` and `service/TripointLoaderServiceTest`
These read the real classpath data, like `CountryLoaderServiceTest`. Construct `RiverLoaderService` with a `new GenericApplicationContext()` (it only needs `getResources`). Check that it resolves `classpath*:` patterns. If it doesn't, use `new AnnotationConfigApplicationContext()` and close it after the test.
- Rivers: non-empty. Every river has a non-blank name, non-null `additionalNames`, a non-blank continent, and a `MultiLineString` geometry. Names are unique. This is the data-integrity guard for `additionalData.json`: today a river file with no entry there causes a `NullPointerException` at runtime, so mention that in the report.
- Tripoints: same count as `ProcessedDataIntegrityTest` (164). Don't duplicate its other checks. One test that `loadTripointDefinitions()` returns coordinates in valid lat/long ranges is enough.

### 2.12 Loader programs (`loaders/countries`, `loaders/tripoint`)
Use JUnit `@TempDir`.
- `CountryWriterTest`: writes one file per country, named by `convertCountryNameToFileName`. The output round-trips through `ProcessedCountryReader` to equal records. Property order in the JSON is `name, additionalNames, geoData` (`SORT_PROPERTIES_ALPHABETICALLY` disabled).
- `ProcessedCountryReaderTest`: ignores non-`.json` files, returns countries sorted by file name, and tolerates unknown properties.
- `RawCountryReaderTest`: write a tiny `countries.geo.json` (2 features) and `additionalNames.json` (aliases for one of them) to the temp dir. The country with aliases gets them. The other gets an empty list.
- `TripointWriterTest`: output parses back into the same `TripointDefinition` list.

**Verify:** `mvn spotless:apply`, then `mvn test`. All green, 1 skipped (still the old `TripointComputerTest`).

## Phase 3: `TripointComputer` without the network

### 3.1 Allowed refactor (the only production change in this plan)
In `TripointComputer`:
- Split `computeTripoints(List<Country>)` into the network call plus a new package-private `List<Tripoint> computeTripoints(List<Country> countries, Document doc)` that holds all the existing parsing logic unchanged. The public method becomes `return computeTripoints(countries, Jsoup.connect(URL).get());`.
- Make `convertToDecimalCoordinate` package-private (it can stay an instance method).

Behaviour must be identical. Diff the method bodies to make sure.

### 3.2 Tests
Delete `src/test/java/.../service/TripointComputerTest.java` and create `loaders/tripoint/TripointComputerTest.java`.

Add a small **hand-written** HTML fixture, `src/test/resources/fixtures/tripoints-sample.html`, that mimics the Wikipedia table structure the parser expects. Do not paste the real page. It should have:
- A header row.
- A row with an `.mbox-image` (must be skipped).
- A valid row: three cells with `.flagicon` + `<a>` country links, and a cell with `.geo-inline` containing `.latitude` / `.longitude` spans in DMS, for example `47°35′N` and `9°33′E`.
- A row where one country name matches only via `additionalNames`, in a different case.
- A row with only two recognisable countries (must be filtered out).
- A row with an unknown country name.

Load it with `Jsoup.parse(...)`. Assert:
- Only the rows with exactly 3 known countries come back.
- Alias and case-insensitive lookup work.
- Coordinates convert correctly.

`convertToDecimalCoordinate`, parameterised: `"47°35′N" → 47.5833…`, `"9°33′E"`, `"10°S" → -10.0`, `"0°30′W" → -0.5`, `"12°34′56.7″N"`, and invalid input (`"abc"`, `"47.5"`) → `IllegalArgumentException`.

**Verify:** `mvn spotless:apply`, then `mvn test`. All green, **0 skipped**.

## Phase 4: backend slice and integration tests

### 4.1 `controllers/GameControllerTest` (`@WebMvcTest(GameController.class)`, `@MockitoBean GameService`)
- `POST /api/games` with a valid body → 200 and `{"id": 42}`. Verify the service got the right `gameType` / `numberOfRounds`.
- Service returns empty → 400.
- Behaviour to pin: the service throws `IllegalArgumentException` for bad rounds or an unknown type, and there is no exception handler, so the request **fails** instead of returning 400 (the controller's `badRequest()` branch is unreachable in practice). Assert what MockMvc actually does (likely the exception propagates wrapped in a `ServletException`). Comment it and report it.
- `GET /api/games/1/rounds/0` → 200. Assert the **JSON contract the frontend relies on**: `$.gameType.name`, `$.gameType.answerCount`, `$.jsonBlob` (a string), `$.totalRoundCount`, `$.answerState`, `$.gameResult.gameState`, `$.gameResult.score`, `$.gameResult.roundsUnanswered`, `$.gameResult.roundAnswerStates`. `GameType` is not an enum, so this checks that it serialises as an object.
- Not found → 404. Non-numeric id → 400.
- `POST .../answers` with `{"answerState":"CORRECT"}` → 200 with the round. Not found → 404. Invalid enum (`"WRONG"`) → 400.
- CORS: a request with `Origin: http://localhost:3000` gets `Access-Control-Allow-Origin` back. `Origin: https://evil.example` does not (a preflight `OPTIONS` is rejected with 403).

### 4.2 `controllers/CountryControllerTest` (`@WebMvcTest`, `@MockitoBean CountryLoaderService`)
- 200, JSON array of `{name, additionalNames}`, `Cache-Control` contains `max-age=86400`.
- `IOException` from the service → the request fails. Pin and report, like 4.1.

### 4.3 `controllers/HealthControllerTest`
`GET /health` → 200, body `Server running!`.

### 4.4 `dao/GameRepositoryTest` (`@DataJpaTest`)
- Save a `GameEntity` with 3 rounds, flush and clear the `TestEntityManager`, and reload by id. You get the same number of rounds, with `index`, `gameType` (same instance as the constant, via the converter), `jsonBlob` and `answerState` preserved.
- A `jsonBlob` of about 2 MB (real country geometry is large) survives the round trip. This guards the `@Lob` mapping.
- Updating a round's `answerState` and saving the parent persists (cascade).
- `findById` of an unknown id → empty.

### 4.5 `GameApiIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, real data, no mocks)
One end-to-end flow per game type, to protect the API contract with the real processed data:
- Create a `TRIPOINT` game with 3 rounds. GET each round: `gameType.name == "TRIPOINT"`, `answerCount == 3`. Parse `jsonBlob`: 3 `countries`, each with `name`, `additionalNames` and `geoData.geometry`. `gameResult.gameState == "CREATED"`.
- Answer round 0 `CORRECT` → `IN_PROGRESS`, score 1. Answer round 1 `GAVE_UP` and round 2 `CORRECT` → `FINISHED`, score 2, `roundAnswerStates == [CORRECT, GAVE_UP, CORRECT]`.
- `RIVERS_BY_SHAPE`, 2 rounds: `jsonBlob` has `name`, `continent` and `geoData`.
- `RANDOM`, 10 rounds: 10 rounds, each type is one of the two.
- `GET /api/countries` returns 255 entries, sorted.

Keep this to one class so the context is created once. If it is slow (over ~15 s), say so in the report, but don't remove it.

**Verify:** `mvn spotless:apply`, `mvn spotless:check`, `mvn test`. All green, 0 skipped.

## Phase 5: frontend test tooling

All commands in `client/`.

1. Install dev dependencies (latest, caret ranges): `vitest`, `@vitest/coverage-v8`, `jsdom`, `@vitejs/plugin-react`, `@testing-library/react`, `@testing-library/dom`, `@testing-library/user-event`, `@testing-library/jest-dom`. Check that `npm ci` has no peer-dependency errors with React 19 and Node 24. If there are, pick compatible versions. Don't use `--legacy-peer-deps`.
2. `client/vitest.config.mts`:
   - `plugins: [react()]`, `test.environment: "jsdom"`, `test.setupFiles: ["./vitest.setup.ts"]`, `test.include: ["src/**/*.test.{ts,tsx}"]`.
   - Coverage: provider `v8`, `include: ["src/**"]`, `exclude` the Leaflet-rendering components `src/map/TripointMap.tsx`, `src/map/RiverShapesMap.tsx`, both `*MapWrapper.tsx`, and `src/test/**`.
3. `client/vitest.setup.ts`:
   - `import "@testing-library/jest-dom/vitest";`
   - `afterEach(cleanup)`, because RTL auto-cleanup needs globals, which we don't enable.
   - Stub `window.matchMedia`. Default `matches: false`, overridable per test.
   - Stub `ResizeObserver` with a class that has `observe` / `unobserve` / `disconnect` spies.
4. `package.json` scripts: `"test": "vitest run"`, `"test:watch": "vitest"`, `"coverage": "vitest run --coverage"`.
5. Add `coverage/` to `.gitignore` (root `.gitignore` or a `client/.gitignore`, whichever already covers `client/out`. Check first).
6. Test helpers in `client/src/test/`:
   - `fixtures.ts`: factories `makeRound(overrides)` returning a `GameRoundModel`, `makeTripointData()` (3 countries with tiny polygons, one with `additionalNames: ["Holland"]`, say), `makeRiverData()`, `makeRoundProps(overrides)` returning a `Round`, plus a `countryNames` list for `/api/countries`.
   - `render.tsx`: `renderWithClient(ui, client = newTestClient())`, where `newTestClient()` makes a `QueryClient` with `retry: false`, `gcTime: 0`.
   - `fetchMock.ts`: a tiny helper over `vi.stubGlobal("fetch", vi.fn())` that routes by URL and method to JSON responses (`{ ok, status, json }`). Unrouted calls should fail the test loudly. Call `vi.unstubAllGlobals()` in `afterEach`.
7. One smoke test, `src/helper/stringHelper.test.ts`, with a single case, to prove the wiring.

**Verify:** `npm test` passes, `npm run lint` passes, and `npm run build` still passes (this proves the test files type-check under Next and nothing leaked into `out/`).

## Phase 6: frontend tests

Colocate each test next to its source (`Foo.tsx` → `Foo.test.tsx`). Common mocks (declare per file with `vi.mock`; put shared mock factories in `src/test/` if they repeat):
- `next/router`: `useRouter` → `{ push: vi.fn(), query: {} }`.
- `next/head`: render children inline (`({ children }) => <>{children}</>`), so tests can read `document.querySelector("title")`.
- `next/link`: try the real one first. If it errors outside a router, mock it to a plain `<a href>`.
- `../map/TripointMapWrapper` and `../map/RiverShapesMapWrapper`: a stub component that renders `<div data-testid="map" data-game-over=… data-gave-up=…>` and, for tripoints, the names of the countries with `showAsHint` set. **Never render real Leaflet maps.**
- `@tanstack/react-query-devtools`: `ReactQueryDevtools` → `() => null`.

Use `userEvent.setup()` for interaction. Use fake timers only where a test needs them (the reveal-confirm timeout, the "waking up" message), and make sure `userEvent` advances them (`userEvent.setup({ advanceTimers: vi.advanceTimersByTime })`).

### 6.1 Pure logic
- `helper/stringHelper.test.ts`: lowercases. Strips accents (`"Curaçao"` → `"curacao"`, `"Côte d'Ivoire"` → `"cotedivoire"`). Strips spaces and punctuation (`"Bosnia & Herzegovina"`). Keeps digits. Empty string → `""`. Idempotent (`normalise(normalise(x)) === normalise(x)`).
- `games/common/answers.test.ts`: `answerNames` for a tripoint (3 names in blob order), a river (`[name]`), and an unknown type (`[]`). `gameTypeLabel` for each `GAME_OPTIONS` entry plus an unknown name (returned as-is).
- `games/gameTypes.test.ts`: option names match the backend constants (`TRIPOINT`, `RIVERS_BY_SHAPE`, `RANDOM`), and every `ROUND_OPTIONS` value is within the backend's 1–10 limit.
- `games/common/Model.test.ts`: `new Round(init)` copies every field.
- `config/environment.test.ts` and `api/gateway.test.ts`: use `vi.stubEnv("NODE_ENV", "production")` / `"development"` with `vi.unstubAllEnvs()`. Production → the onrender URL. Otherwise → `http://localhost:8080`.

### 6.2 API hooks (`api/games.test.tsx`, `api/countries.test.tsx`)
Use `renderHook` from RTL with a wrapper that provides a test `QueryClient`.
- `roundUrl("abc", 2)` → `/api/games/abc/rounds/2`.
- `fetchRound`: calls `serverEndpoint() + roundUrl(...)`. Resolves with the JSON. On a non-ok response, rejects with a message that includes the round and the status.
- `useCreateGame`: `createGame("TRIPOINT", 5)` POSTs JSON `{gameType, numberOfRounds}` with a `Content-Type` header. On success it calls `router.push("/games/<id>")`. `isLoading` is true while pending. A non-ok response sets `error` and doesn't push.
- `useCountryNames`: returns the list. Calls `fetch` **once** across two hooks rendered with the same client (cached, `staleTime: Infinity`). Error after both attempts fail (because of `retry: 1`. See "Where things stand").

### 6.3 Small components
- `components/GuessBox.test.tsx`: renders with `aria-label` / `placeholder` = `name`. `onChange` fires on typing. `disabled` is respected. With `suggestions`, the input's `list` attribute points at a `datalist` with one `option` per suggestion. Without suggestions there is no `list` attribute and no datalist.
- `games/common/AnswerSlots.test.tsx`: the group label says `Answers: 1 of 3 found`. Found answers render first. Unfound slots render `?` with `aria-label="Not found yet"`. When `gaveUp`, the remaining slots show the revealed names with the visually hidden "(revealed)" text.
- `games/common/RoundProgress.test.tsx`: one button per state. Labels follow `describe()` (current / correct / revealed / not played). `aria-current="step"` only on the current round. Visitable rules: answered rounds, the current one and the **first** unanswered one are enabled, and later unanswered rounds are disabled. Clicking calls `goToRound(i)`. Correct/revealed dots get `round-dot--correct` / `round-dot--revealed`.
- `games/common/RoundResult.test.tsx`: shows "Correct!" or "Answer revealed". With `hasNextRound`, "Next round" calls `round.nextRound`. Without it, "See results" calls `round.showSummary`.
- `games/common/GameTopBar.test.tsx`: shows `Score N` mid-game, and `Final score N / T` plus a "Results" button (calls `showSummary`) when `gameCompleted`. When `gameOver` is true, the current round's dot reflects the local result straight away (CORRECT, or GAVE_UP when `gaveUp`) even if `roundAnswerStates` still says UNANSWERED. It does not mutate the `round.roundAnswerStates` array passed in.
- `games/common/GameLayout.test.tsx`: renders `topBar` in `header`, `map` in `main`, `panel` in `aside`. `mapClassName` is appended.

### 6.4 `games/common/MapGameInput.test.tsx`
Render with a `submitGuess` mock that returns each `GuessResult` kind in turn, and check the feedback text in the `aria-live` region:
- `empty` → "Type a guess first." `duplicate`, `unknown` (input keeps its value), `wrong` (input cleared). `correct` with `remaining > 0` → "Correct! 2 to go.", and with `remaining: 0` → "Correct!". `over` → no feedback change.
- Submits on Enter and on the Guess button. The input text is passed to `submitGuess`.
- Reveal needs two clicks: the first changes the label to "Tap again to reveal" without calling `giveUp`, and the second calls `giveUp`. After 3 s without a second click it resets (fake timers). Unmounting while the timer is pending doesn't warn or throw.
- Hint button: shows `Hint (N left)`, calls `useHint`, and is disabled when no hints are left. It is absent when `hints` is undefined.
- `gameOver` → renders `RoundResult` instead of the form.
- Wrong guesses list renders each wrong answer.
- Behaviour to pin: `suggestions` is accepted as a prop but **not passed to `GuessBox`**, so Tripoint's autocomplete datalist never renders. Write a test asserting no `datalist` is rendered even with suggestions, comment it and report it.

### 6.5 `games/common/MapGame.test.tsx`
Render `MapGame` directly with a stub `MapComponent` (a component that renders `data-game-over` / `data-gave-up`), a simple `isAnswerCorrect` (case-insensitive membership in a list) and a `submitAnswer` mock. Drive it through the real `MapGameInput` with `userEvent`:
- Fresh round (`answerState: UNANSWERED`): no slots filled, map not in game-over mode, blank-map caption shown when `blankMapCaption` is set.
- A wrong guess is listed under wrong guesses and `submitAnswer` is not called.
- A duplicate guess (different case or accents) → duplicate feedback, not counted twice.
- With `isKnownGuess` returning false → "unknown" feedback, not added to wrong guesses.
- Finding all N answers → `submitAnswer("CORRECT")` exactly once, game-over mode, `RoundResult` shown, caption hidden.
- Reveal (double click) → `submitAnswer("GAVE_UP")` once, all answers shown as revealed.
- Initial `answerState: "CORRECT"` → starts in game-over with all answers filled. Initial `"GAVE_UP"` → starts revealed. Neither calls `submitAnswer` on mount.
- Hints: each use calls `hints.use(i)` with an increasing index. Returned text appears with the lightbulb. A `null` return adds no text but still uses up a hint.

### 6.6 Game components
- `games/Tripoint.test.tsx` (mock `fetch` for `/api/countries`, with the map wrapper stubbed):
  - Correct by exact name, by a different case, and by an alias from `additionalNames`.
  - The prompt says "Name the 3 countries…".
  - Before `/api/countries` loads, any guess is allowed (no "unknown" check). After it loads, a name not in the list gets "unknown" feedback, and an alias in the list is accepted.
  - Hint: each use marks one more country as `showAsHint` (visible through the map stub), never the same one twice. After all 3 the hint button is disabled. Stub `Math.random` with `vi.spyOn` for a deterministic pick.
  - Behaviour to pin: `showRandomCountryBorder` mutates the country objects in the `tripoint` prop (`randomCountry.showAsHint = true`). Assert the original fixture object was mutated, comment it and report it.
- `games/RiverShapes.test.tsx`: correct by name and by alias, the hint text is `Continent: <continent>` and only one hint is available, and the blank-map caption shows until the round ends.

### 6.7 Pages
- `games/common/GameSummary.test.tsx`: fetch-mock N rounds with mixed states. While they load, a spinner shows. When loaded: "You scored X / N", "X correct · Y revealed", one row per round with the type label and the answer names. Clicking a row calls `onOpenRound(i)`. "Play again" POSTs `/api/games` with the **same** game type when all rounds share it, `RANDOM` when they're mixed, and `numberOfRounds = N`. If create fails, the error alert shows.
- `games/common/GameRoundPage.test.tsx` (wrap in the gateway singleton `client`, see "Where things stand"; `client.clear()` in `afterEach`):
  - Spinner while loading. Error alert when the round fetch fails.
  - Title is `Tripoint | Geogames` / `Rivers | Geogames` / `Unknown game! | Geogames` per type, and `Loading... | Geogames` before data arrives.
  - Renders the Tripoint game for `TRIPOINT`, the river game for `RIVERS_BY_SHAPE`, and "Unknown game type..." otherwise.
  - Solving a round POSTs `/api/games/<id>/rounds/0/answers` with `{answerState: "CORRECT"}`, then refetches round 0 (assert a second GET).
  - "Next round" fetches round 1. A round dot navigates to that round.
  - On the last round, "See results" shows `GameSummary`. Opening a round from the summary returns to the game view.
  - Missing `gameResult.roundAnswerStates` (older backend) falls back to an array of `UNANSWERED` with the current round's state filled in.
- `components/HomePage.test.tsx`:
  - On mount, it fires `GET <server>/health` with `mode: "no-cors"`, and a rejected health fetch doesn't break the page.
  - Tripoints is selected by default, and the radio group has `aria-checked` on exactly one card.
  - Clicking a card selects it. Arrow keys move the selection and the focus, wrapping in both directions. Other keys do nothing.
  - The rounds toggle defaults to 5, and changing it is reflected in the create request.
  - "Start game" POSTs `{gameType, numberOfRounds}` and pushes `/games/<id>`. While pending, the button is disabled and shows "Starting…".
  - With fake timers: after 3 s pending, the "Waking up the server" message appears. It disappears once the request settles.
  - A failed create shows the danger alert.

### 6.8 Leaflet helper components
`vi.mock("react-leaflet", () => ({ useMap: () => fakeMap }))` with a `fakeMap` of spies:
- `CenterUpdater`: calls `setView([lat, lng], zoom)` on mount (default zoom 4), and again when the centre changes, but not on a re-render with the same values.
- `FitBounds`: calls `fitBounds` with padding `[24, 24]` and `maxZoom: 7`. It re-fits when `id` changes but not when only `data` changes (pinned intentionally by the eslint-disable in the source).
- `MapResizer`: observes `map.getContainer()`, a resize callback calls `invalidateSize`, and unmount disconnects.

**Verify:** `npm test`, `npm run lint`, `npm run build` all pass. Run `npm run coverage` and note the numbers. Target ≥ 85 % lines on the covered `src/**` files. If a file is well below, add tests rather than excluding it. The only exclusions allowed are the ones in phase 5.

## Phase 7: wire frontend tests into the Maven build

1. In `pom.xml`, add a fourth `frontend-maven-plugin` execution `npm test` with `<phase>test</phase>` and `<arguments>run test</arguments>`. The plugin skips test-phase executions when `-DskipTests` is set. Verify that it does. If not, add `<skip>${skipTests}</skip>` with a `<skipTests>false</skipTests>` default in `<properties>`.
2. Leave the `Dockerfile` alone (it already passes `-DskipTests`).

**Verify:**
- `mvn clean install` runs both the Java and the Vitest suites and passes. Check the log for the Vitest summary.
- `mvn clean install -DskipTests` does **not** run Vitest.
- Break one frontend assertion on purpose, confirm `mvn test` fails, then revert it.

## Phase 8: docs and report

1. Update `CLAUDE.md`:
   - Replace "The frontend has no tests." with the commands (`npm test`, `npm run test:watch`, `npm run coverage`) and where tests live (colocated `*.test.ts(x)` under `client/src`, helpers in `client/src/test/`, never under `pages/`).
   - Note that `mvn test` / `mvn install` also run the frontend tests, and that `-DskipTests` skips both.
   - Mention the Boot 4 test starters (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`) and `@MockitoBean`.
2. Final report to the user (in chat, not a file):
   - Test counts before and after (backend and frontend) and frontend coverage.
   - The phase 3.1 refactor, in one line.
   - **Every `// Current behaviour:` item**, as a list with file:line and a one-sentence suggestion. Expected at least: `GameTypeConverter` fallback, the controller 500-instead-of-400, re-answering a round, `calculateGameResult(empty)` → FINISHED, the `RiverLoaderService` NPE risk, the `RiverFinderService` infinite-loop risk with duplicates, `MapGameInput` dropping `suggestions`, and the Tripoint prop mutation.
   - Anything slow, flaky or skipped, and why.
