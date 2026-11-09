# Plan: close the unit-test coverage gaps and enforce coverage

Read `CLAUDE.md` first. Work on a new branch, `test-coverage`, created from `main`. Do not commit unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Do not start the next phase until that step passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

This plan comes before `ci-pipeline.md`, which wires the coverage gates added here into separate CI jobs.

## Ground rules

- **Do not change production behaviour.** Allowed production-code changes are limited to the loader refactor in phase 3. If a test exposes a bug, assert the **current** behaviour, put a `// Current behaviour:` comment above the assertion saying what looks wrong, and list it in your final report. Do not fix it.
- No network in tests. Tests must be deterministic: pass a seeded `java.util.Random` where a class takes one, and otherwise assert properties that hold for any outcome.
- Don't paper over failures: no `@Disabled`, `it.skip`, `-DskipTests`, `@ts-ignore`, broad `eslint-disable`, or deleted assertions.
- **Test behaviour, not lines.** Don't write tests for getters, setters, record accessors or `toString`. If a missed line can only be reached by such a test, leave it uncovered. The thresholds below allow for that.
- Java: JUnit 5, AssertJ, Mockito. Package-private test classes and methods, named by behaviour (`skipsLakesUnder100Km2`, not `test1`). Use `@TempDir` for files. Run `mvn spotless:apply` before each Java verification.
- Frontend: Vitest + React Testing Library. Import `describe`/`it`/`expect`/`vi` from `vitest`. Query by role, label or text. Never render a real Leaflet map.
- Environment on this machine: `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.0.2"` before Maven, and put `target/node` on `PATH` for npm (it is Node 24).

## Where things stand (measured on 2026-11-08)

**Frontend:** 45 test files, 348 tests, all passing. `npm run coverage`: statements 91.4%, branches 87.6%, functions 83.7%, lines 92.1%. No thresholds are enforced.
- `src/api`, `src/components`, `src/games/common` are at 95–100%.
- Every uncovered file of any size is a Leaflet map or layer: `games/countries/{CoastlineMap,LandBordersMap,NeighboursMap,CountryLayers}.tsx`, `games/rivers/{RiverCountriesMap,RiverEndsMap,RiverOnMapMap,RiverLayers}.tsx`, `games/lakes/LakeShapesMap.tsx`, `map/RevealTiles.tsx`, plus `map/constants.ts` (0%, though it holds the path-style functions).
- The `coverage.exclude` list in `client/vitest.config.ts` is out of date. It names only the tripoint and river-shapes maps and wrappers, so the other seven `*MapWrapper.tsx` files show up as 33%.
- **The serious gap:** no test checks that a map hides the answer while the round is being played. For example, `RiverEndsMap` must draw `RiverLine` only when `gameOver`. A regression there gives away every answer, and no test would catch it.
- Small gaps: `components/GuessBox.tsx:17` (refocus after picking a suggestion), `games/common/MapGame.tsx:37` (the dev-only "no hints" warning) and `:70` (the `takeHint` guard), `games/common/GameSummary.tsx:14-18` (unknown game type fallback), `api/games.ts:38` (a branch).
- `client/pages/` isn't in the coverage `include`. `pages/games/[gameId].tsx` has logic: it renders nothing until `router.query.gameId` is a string.

**Backend:** 46 test classes, all passing. There is no coverage tool in `pom.xml`. A one-off JaCoCo 0.8.14 run (`mvn org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.14:report`) gave **77.1% lines, 77.1% branches** overall:
- Runtime code is essentially fully covered: `service` 100% lines / 98.7% branches, `controllers` 100%, `helper` 100%, `api` 100%, `model` 98.4%, `config` 100% lines but 62.5% branches (`RiverGamesConfig` 5/8).
- Gaps are all in the offline `loaders/` programs:

| Class | Lines covered | Note |
|---|---|---|
| `loaders.rivers.RiverLoaderMain` | 0% (109 lines) | All the orchestration is in `main` |
| `loaders.lakes.LakeLoaderMain` | 0% (79) | Same |
| `loaders.features.WikidataMain` | 0% (67) | `buildQuery` and `parse` are package-private statics, testable now |
| `loaders.countries.CountryShapeMain` | 0% (35) | |
| `loaders.lakes.LakeWriter` | 0% (33) | No test |
| `loaders.rivers.RiverWriter` | 0% (30) | No test |
| `loaders.tripoint.TripointLoaderMain` | 0% (18) | Scrapes Wikipedia, so leave its network part untested |
| `loaders.countries.CountryLoaderMain` | 0% (8) | |
| `loaders.lakes.LakeAssembler` | 89% lines, 51/68 branches | |
| `loaders.rivers.RiverAssembler` | 86% lines, 56/66 branches | |
| `loaders.rivers.RiverGeometry` | 84% lines, 18/32 branches | |
| `loaders.tripoint.TripointSnapper` | 93% lines, 12/18 branches | |

Those loaders write the committed files in `src/main/resources/processed/`, which every game serves. A loader bug ships wrong data as silently as a service bug ships a wrong answer.

## Phase 0: baseline

1. `git checkout main && git pull && git checkout -b test-coverage`.
2. `mvn -B verify` must pass. Note the backend and frontend test counts.
3. In `client/`: `npm run coverage`. Note the four totals.

## Phase 1: enforce backend coverage with JaCoCo

1. Add `org.jacoco:jacoco-maven-plugin` `0.8.14` (this exact version is known to work with JDK 25 here; check Maven Central for a newer patch and use it only if it is at least two weeks old) to `pom.xml`, with three executions:
   - `prepare-agent` (default phase).
   - `report` in the `test` phase. It writes `target/site/jacoco/`.
   - `check` in the `verify` phase, with rules as below. Start with the floor values (the measured numbers). Phase 3 raises them.
     - `BUNDLE`: `LINE` `COVEREDRATIO` minimum `0.77`, `BRANCH` `COVEREDRATIO` minimum `0.77`.
     - `PACKAGE`, with `includes` limited to the runtime packages (`com.parkinson.benjamin.geogames.service`, `.controllers`, `.helper`, `.api`, `.model`, `.model.geojson`, `.dao`, `.config`): `LINE` minimum `0.95`.
   - Exclude `com/parkinson/benjamin/geogames/GeogamesApplication.class` from the data (it is just `main`).
2. Respect a `jacoco.skip` property (the plugin already does) so later CI jobs can turn it off.

**Verify:** `mvn -B verify` passes and prints the JaCoCo check. Temporarily raise the bundle minimum to `0.99` and confirm that `verify` fails with a clear coverage message. Then put it back.

## Phase 2: backend tests that need no refactor

Put each test in the package of the class it tests. Open `target/site/jacoco/index.html` to see the missed branches of each class (highlighted in yellow and red) before writing tests for it.

### 2.1 `loaders/features/WikidataMainTest`
- `buildQuery`: a list of QIDs ends up in the SPARQL `VALUES` clause in order. Test an empty list, and assert whatever the current behaviour is.
- `parse`: hand-write a small SPARQL JSON result (two QIDs, one with aliases and continents, one with missing optional fields) as a string in the test. Assert the `WikidataEntry` fields, including how missing values and the joined lists come out (`split`). A row with a malformed QID: assert the current behaviour.
- Don't test `main` or `post`. They call the network.

### 2.2 `loaders/rivers/RiverWriterTest` and `loaders/lakes/LakeWriterTest`
Model them on the existing `CountryWriterTest` and `TripointWriterTest`.
- They clear the `*.json` files already in `outDir`, but leave other files.
- They create `outDir` if it doesn't exist.
- File name: `<slug>-<QID>.json`, including a name with accents or punctuation and a name that slugs to nothing (the `"river"` / lake fallback in `Slugs`).
- Each file reads back through `GeoJson` (use the same reader the runtime loader uses) into an equal `River` / `Lake`, and it ends with a newline.
- `toRiver` turns each `double[]` line into a `MultiLineString`. Do the same for the lake writer's geometry, `Polygon` vs `MultiPolygon`: read `LakeWriter` for its branches.
- Writing the same input twice gives byte-identical files. The loaders promise they are deterministic.

### 2.3 Missed branches in the assemblers and geometry
For `LakeAssembler`, `RiverAssembler`, `RiverGeometry`, `TripointSnapper`, `CountryLocator.Part`, `WikidataEntry` and `Slugs`: add tests for each missed branch that is a real rule (an override that excludes, renames or names a small lake; a feature with no Wikidata entry; a degenerate line with one point; a snap just inside or outside 30 km; and so on). Name each test after the rule. Extend the existing test classes, and use their builders and helpers rather than new fixtures.

### 2.4 `config/RiverGamesConfigTest` and `config/CountryGamesConfigTest`
Without Spring, call each `@Bean` method with a stub loader. Use three rivers: one through 1 country, one through 3, and one with no `source`/`mouth` if the filter checks that. Assert the eligibility filter of each bean (`RIVERS_BY_COUNTRIES` needs three or more countries) and the `GameType` it returns. Do the same for the country-shape beans against `CountryShapeEligibility`. Skip any of this that existing generator tests already pin. Check them first.

**Verify:** `mvn spotless:apply && mvn -B verify` passes. Read the JaCoCo report and note the new totals.

## Phase 3: make the loader programs testable

The `*Main` classes do all their work inside `main`, so nothing tests that the pieces fit together: reading, assembly, selection by continent quota, locating countries, writing. Refactor each one the same way:

1. Move the body of `main` into a `static Report run(...)` method that takes `Path` parameters (one per current `args[i]`), does everything `main` did except printing, and returns a small `record Report(...)` holding what the summary prints (the written files, the selection, the drop counts, the facts, and so on). `main` becomes: parse `args`, call `run`, print the report. **The printed output must not change.** Before the refactor, run each main on the real data with its output redirected to a file. After it, do it again and `diff` the outputs. Use the argument lists in `CLAUDE.md` and write into a scratch directory, not `src/main/resources/processed`.
2. Do this for `RiverLoaderMain`, `LakeLoaderMain`, `CountryShapeMain` and `CountryLoaderMain`. For `TripointLoaderMain`, split only the offline part (snapping and writing the parsed definitions) out of the Wikipedia fetch, if that is clean. Otherwise leave it and say so in the report. Leave `WikidataMain` as it is.

Then write one test class per program (`RiverLoaderMainTest` and so on), with tiny hand-written fixtures under `src/test/resources/fixtures/loaders/<program>/`. Keep each fixture file under about 5 KB and shaped like the real input:
- **Rivers:** a Natural Earth file with about 5 features in two continents, `wikidata.json` for them (one with no continent), `overrides.json` with one `exclude`, one `name` and one `reverseFlow`, one tiny OSM file used through `osmFile`, and a processed-countries dir with two or three square countries. Assert: the excluded river isn't written, the renamed one has its new name, the reversed one has `source` and `mouth` swapped, the river with no continent shows up in `report.unresolved()`, each river's `countries` are correct, and running twice gives identical files.
- **Lakes:** the same idea. One lake under 100 km² that is dropped, one under 100 km² kept because an override names it, a `countries` override that replaces the located countries, and a shore within 5 km of a country.
- **Country shapes:** two countries sharing an edge, one with a coast. Assert `coast`, `landBorders`, `neighbours` and `focus`, and that the shared border is identical on both sides.
- **Countries:** a raw `countries.geo.json` with three features, one of them in `excludedNames.json`, plus an `additionalNames.json` alias.

Raise the JaCoCo minimums to the new measured values, rounded **down** to the nearest whole percent. The target is at least `0.90` lines and `0.85` branches for the bundle, and `0.97` lines for the runtime packages. If you can't reach a target without testing getters or network code, set the gate to what you reached and say why in the report.

**Verify:** `mvn spotless:apply && mvn -B verify` passes with the raised gates, and the before and after summary outputs of each refactored main are identical.

## Phase 4: frontend map tests (the answer-leak check)

1. Create `client/src/test/leafletMock.tsx`, a module for `vi.mock("react-leaflet", ...)`. It should render each component used by the maps (`MapContainer`, `TileLayer`, `GeoJSON`, `Marker`, `Tooltip`, `Polyline`, `ScaleControl`, and anything else `grep -rh "from \"react-leaflet\"" client/src` turns up) as a plain `<div>`:
   - with `data-testid` set to the component name,
   - with the props a test needs as data attributes: `data-url` for `TileLayer`, the JSON `data`'s `properties.name` and the `pathOptions.color` for `GeoJSON`, the `position` for `Marker`,
   - and with its children rendered.

   Give `useMap` a stub map object with `vi.fn()` methods, as the existing `FitBounds` / `LockedView` tests do (reuse their stub if there is one). The goal is to see which layers are drawn, not Leaflet's own behaviour.
2. For each of the nine `*Map.tsx` files (tripoint, the four river maps, the three country maps, lake shapes), write `<Name>Map.test.tsx` next to it, with the data from `client/src/test/fixtures.ts`. For each map, test:
   - **While playing** (`gameOver: false`): no answer layer is drawn (the river line, the country outline, the lake polygon, the tripoint countries: read the map to find out which it is), and no satellite `TileLayer` is drawn on maps that are blank while playing.
   - **After the round** (`gameOver: true`): the answer layer is drawn. With `gaveUp: true` its colour is the "gave up" colour from `answerPathOptions(true)`, which differs from the solved colour.
   - **Hints:** for maps whose layers depend on `hintsUsed`, the hint layer appears only at the right hint number.
3. `map/constants.ts`: add `constants.test.ts` asserting that `answerPathOptions(true)` and `answerPathOptions(false)` differ, and that the clue and hint option functions return the styles the maps rely on (for example the hint line is dashed). Keep it short.
4. `RiverLayers.tsx` and `CountryLayers.tsx` are covered through the map tests. Add direct tests only for what those miss (for example, `FocusFit` passes `country.focus` as bounds).
5. Update `client/vitest.config.ts`. Replace the four hard-coded exclusions with `src/**/*MapWrapper.tsx` (they are only a `dynamic()` import). Then remove anything that is now covered.
6. Update the `CLAUDE.md` line "Never render real Leaflet maps: mock the `*MapWrapper` modules" so it also says map components themselves are tested with `src/test/leafletMock.tsx`.

**Verify:** `npm test` and `npm run lint` pass. As a sanity check of the leak test, temporarily make `RiverEndsMap` always draw `RiverLine`, confirm its test fails, then revert.

## Phase 5: remaining frontend gaps and thresholds

1. `GuessBox`: picking a suggestion refocuses the input on the next tick (`vi.useFakeTimers()`, `fireEvent.change` with `nativeEvent.inputType` set to `"insertReplacementText"`, then `vi.runAllTimers()`), and typing a non-suggestion doesn't.
2. `MapGame`: with `vi.stubEnv("NODE_ENV", "development")`, a round with `hintCount > 0` and no `hints` logs the console error (spy on `console.error`). With `NODE_ENV` `"test"` it doesn't. The hint button can't take a hint past `hintCount` or after the round is over.
3. `GameSummary`: a round whose `gameType.name` has no module falls back to the raw name and no answers.
4. `api/games.ts:38`: cover the missed branch (read the line to see which).
5. `pages/games/[gameId].tsx`: add `src/pages/gameIdPage.test.tsx`. It must **not** go under `client/pages/`. Mock `next/router` and `GameRoundPage`. It renders nothing while `query.gameId` is undefined, and renders `GameRoundPage` with the id once it is a string. Add `pages/**` to the coverage `include`, and exclude `pages/_document.tsx`.
6. Add `coverage.thresholds` to `client/vitest.config.ts` set to the measured totals rounded **down** to the whole percent. The target is at least 95 statements, 90 branches, 90 functions and 95 lines. Reporters: `["text", "html", "lcov"]`.

**Verify:** `npm run coverage` passes and prints totals at or above the thresholds. Temporarily set `lines: 100`, confirm the run fails, then revert. `mvn -B verify` passes.

## Final report (in chat, not a file)

- Backend and frontend test counts and coverage, before and after, as a small table.
- The thresholds you set and where each gate lives.
- The loader refactor in one line per program, and confirmation that the printed summaries didn't change.
- Every `// Current behaviour:` item, with file:line and a one-sentence suggestion.
- Anything you couldn't cover and why.
