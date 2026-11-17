# Plan: four new games (Border Shapes, Merged Countries, Country Cities, Country Rivers)

Read the root `CLAUDE.md` first, then the `CLAUDE.md` of each folder you touch (backend, loaders, `client/`, `e2e/`). They describe how a game type is added, the test conventions and the check scripts. This plan only says what is specific to these four games.

Work on a new branch from an up-to-date `main`, one branch per phase. Do not commit, push or open a PR unless the user asks.

Do the phases in order. Each ends with a verification step. **Don't start the next phase until it passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## The four games

All four use a blank map while playing (`blankMap`), satellite tiles once the round is over (`RevealTiles`), 3 hints and 5 wrong guesses. The answers are countries, so they use `useGuessList("countries")`.

| `GameType` constant | Title | Answers | Clue | Hint 1 | Hint 2 | Hint 3 |
|---|---|---|---|---|---|---|
| `BORDERS_BY_SHAPE(2, 3, 5)` | Border Shapes | both countries | the border between two countries as a lone line; its length is in the map caption from the start | the rest of both countries' land borders | both coastlines | both first letters |
| `MERGED_COUNTRIES(2, 3, 5)` | Merged Countries | both countries | two neighbours filled as one shape, the border between them not drawn | the surrounding countries, unlabelled | the hidden border | both first letters |
| `COUNTRIES_BY_CITIES(1, 3, 5)` | Country Cities | the country | its cities as dots sized by population, no outline | the capital is highlighted | the coastline | first letter |
| `COUNTRIES_BY_RIVERS(1, 3, 5)` | Country Rivers | the country | the rivers inside it, clipped at its borders, major rivers thicker | its lakes | the coastline | first letter |

Decisions the user has already made, so don't reopen them: the border length is always visible (not a hint); the capital comes before the coastline; the last hint of the pair games is both first letters.

Home page: set `extraDifficult: true` on Border Shapes only. Suggested registry order: put Merged Countries, Country Cities and Country Rivers after `landBorders`, and Border Shapes last. Pick icons from `react-bootstrap-icons` and check each one exists before using it.

## Memory rules (read before designing any loader or service)

The production container has 512 MB and the JVM runs with `-Xmx400M` (`Dockerfile`; the CI `docker` job runs the image with `--memory=512m`). Do not change either number. The non-heap memory of a Spring Boot app is already most of the 112 MB gap, so new data must add almost nothing to the live heap.

What is in memory today:

- `CountryShapeService` keeps all 241 country shapes for the life of the process. Their coordinates are boxed (`List<List<Double>>`), which costs roughly 80 bytes a point, about four times the JSON size. The 6.3 MB of `processed/country-shapes` is therefore an estimated 25 to 35 MB of heap (an estimate, not a measurement; phase 0 measures it).
- The `@Cacheable` caches (`countryNames`, `displayCountry`, `rivers`, `riverNames`, `lakes`, `lakeNames`, `tripointDefinitions`) use Spring's default cache manager, which never evicts.

**Eviction is not needed, and don't add it.** Every cache is keyed by nothing or by a known country name (`loadDisplayCountry` throws for an unknown name before anything is cached), so each cache has a fixed maximum size set by the data in the jar. A request cannot grow them. Evicting static reference data would only make the server parse it again. What matters is the size of what is loaded, so the rules for the new games are:

1. **No new geometry for the pair games.** The shared border and both outlines are already inside the cached shapes. Phase 1 adds only a few integers per neighbour that say which lines of `landBorders` belong to which neighbour.
2. **Cities are held as primitives.** One small file, parsed once into records with `double lon`, `double lat`, `int population`, `boolean capital` and a `String name`. About 3,500 cities is well under 1 MB.
3. **River geometry is never held in memory.** It is the only large new data set (an estimated 2 MB of JSON, so about 8 to 10 MB if cached as boxed lists). Write one file per country and read that one file when a round is built. Keep only a small index (name, river count, km) in memory. Do not put `@Cacheable` on the per-country read. A file is at most about 80 KB and a round is built once per round view, so parsing it each time is cheap.
4. **Never cache anything keyed by a round, a pair or a game.** Rounds are rebuilt from the cached shapes on every read on purpose (see "Persistence" in the backend `CLAUDE.md`). A cache of assembled rounds would bring that heap back.
5. **Budget: the live heap after warm-up may grow by at most 5 MB in total** across all four games, measured as below. The expected growth is under 2 MB. If a phase exceeds the budget, stop and report the numbers.

### Measuring (phase 0 and after each phase)

Add `scripts/measure-heap.sh`. It needs a built jar (`./mvnw -DskipTests -Dskip.frontend package` is enough when `client/out` exists) and does this:

1. Starts `java -Xmx400M -jar target/geogames-0.0.1-SNAPSHOT.jar` in the background and waits for `/health`.
2. For every game type the server offers (pass the list as arguments, default to all `GameType` names), posts a 10-round game and fetches all 10 rounds, so every loader and cache is warm.
3. Runs `jcmd <pid> GC.run`, then prints the `used` figure from `jcmd <pid> GC.heap_info`, then stops the server.

Run it on `main` before any change and write the figure into the "Results" section at the end of this file. Run it again at the end of each phase and add a line. Keep the script: it is the check for any later data change.

## Phase 0: baseline

1. Write `scripts/measure-heap.sh` and record the baseline.
2. Run `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh` and `bash scripts/check-it.sh` once to confirm the branch starts green.

## Phase 1: the pair games (Border Shapes and Merged Countries)

### 1a. Loader: say which border lines belong to which neighbour

`CountryShape.landBorders` is one `MultiLineString` with no record of which line faces which neighbour. `CountryShapeBuilder.shape(...)` knows: each border run has the neighbour's name as its label when it is added to `borderLines`.

- Add `List<Integer> lines` to `CountryShape.Neighbour`: the indexes into `landBorders.geometry.coordinates` of the lines shared with that neighbour, ascending.
- Add `long areaKm2` to `CountryShape`: the area of the largest part (outer ring minus holes) from the full-resolution rings. `Geo` has no area function, so add one: for a ring of `{lon, lat}` points in radians, `area = |Σ (λ₂ − λ₁) · (2 + sin φ₁ + sin φ₂)| · R² / 2` with `R = Geo.EARTH_RADIUS_KM`. Test it against a known country to within 2% (France's mainland is about 548,000 km²).
- Re-run `CountryShapeMain` (see the loaders `CLAUDE.md`) and commit the regenerated `processed/country-shapes`. `bash scripts/check-processed-data.sh` must then pass.
- Mirror both fields in `client/src/games/countries/model.ts`.

Both fields travel inside `jsonBlob` for the three existing country games, so the contract changes: regenerate `api-shapes.json` (command in the root `CLAUDE.md`) and update the frontend fixtures.

Add to `ProcessedDataIT`: for every neighbour, `lines` is not empty, every index is in range, and no line index belongs to two neighbours; for every pair A and B, the points of A's lines for B equal the points of B's lines for A as a set (the builder simplifies a shared border identically on both sides, so this must hold exactly).

### 1b. Backend

- `model/CountryPairRound(GameType gameType, List<CountryShape> countries, GeoData sharedBorder, long borderKm, List<CountryOutline> neighbours, Bounds focus, Bounds borderBounds) implements GameData`.
  - `countries`: the two shapes, sorted by name.
  - `sharedBorder`: a `MultiLineString` built by picking the first country's `lines` for the second out of its `landBorders`.
  - `borderKm`: the first country's `Neighbour.km` for the second.
  - `neighbours`: for Merged Countries, the mainland neighbours of either country except the pair itself, each once; empty for Border Shapes.
  - `focus`: the union of the two countries' `focus` boxes. `borderBounds`: the bounding box of `sharedBorder`.
  - `subjectId()` is `pair:<A>|<B>` with the names sorted. Both pair games share it, so a mixed game never shows the same pair as a border and as a merged shape.
- `service/CountryPairRoundGenerator`: one class, two beans in a new `config/CountryPairGamesConfig`, following `CountryShapeRoundGenerator` and `CountryGamesConfig`. `generate` lists every pair once (A before B by name) from `CountryShapeService.loadShapes()`; it builds nothing until a pair is picked. `rebuild` parses the key and uses the same private method.
- `helper/CountryPairEligibility`, with the reason for each threshold as in `CountryShapeEligibility`:
  - both games: each country is a **mainland** neighbour of the other (so France and Brazil, which meet only in French Guiana, are out and the map never has to frame an overseas part), and the border is at least 50 km;
  - Merged Countries also: the larger `areaKm2` is at most 8 times the smaller, so one country isn't a bump on the other.
- Tests: generator and config unit tests like the existing country ones; extend `ProcessedDataIT.roundKeysAreUniqueWithinEachGameTypeAndFitTheColumn` and add a test that each pair game has at least 150 eligible pairs. `RoundRebuildIT` covers the new generators by itself.

### 1c. Frontend: `client/src/games/pairs/`

Follow `games/countries/` (a `makePairGame(variant)` like `makeCountryShapeGame`, shared layers, one module file per game, a `*Map.tsx` and `*MapWrapper.tsx` pair each).

- Answers: both countries, as `TripointGame` passes several. Prompts: "Name the two countries this border separates." and "Name the two countries merged into this shape."
- Border Shapes:
  - Draw `sharedBorder` with `ClueLines`.
  - The caption is built from the data: `Border length: 1,234 km`.
  - Fit to `borderBounds` at first. From hint 1 on, fit to `focus` (change the `FitBounds` `id` so it fits again).
  - Hint 1 draws both countries' `landBorders` with `HintLines` under the clue line; hint 2 adds both `coast`s.
- Merged Countries:
  - Draw both outlines with the same **opaque** fill and a stroke of the same colour. Two adjacent polygons filled without a stroke show a hairline seam where they meet, which would give the border away. Check this by eye in the dev server at several zoom levels.
  - Hint 1 draws `neighbours` with `NeighbourOutlines`.
  - Hint 2 draws `sharedBorder` in a style that contrasts with the fill.
  - Fit to `focus`.
- Hint 3 for both: one text naming both first letters, countries in name order.
- When the round is over, draw each country with `AnswerOutline` (it carries the name tooltip).
- New path styles go in `client/src/map/constants.ts`. No colour literal anywhere else (`npm run lint` rejects it).
- `e2e/fixtures.ts` works out the answers with `gameType === "TRIPOINT" ? blob.countries : ...`. Change it to use `blob.countries` whenever it is present. Add both games to `GAMES` in `e2e/tests/every-game.spec.ts`.

### Verification

`check-java.sh`, `check-frontend.sh`, `check-it.sh` and `check-processed-data.sh` pass. Play three rounds of each game in the dev server, in light and dark mode. Run `measure-heap.sh` and record the figure.

## Phase 2: Country Cities

### 2a. Data

- Source: Natural Earth `ne_10m_populated_places_simple.geojson` (public domain, 4.9 MB). Download it once into `geodata/cities/naturalearth/` from `https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_populated_places_simple.geojson` (this URL was checked on 2026-11-17). Prefer a release tag over `master` if one resolves, and record the exact URL used in the loaders `CLAUDE.md`. This download is the only network use in the whole plan; nothing in the build or the tests may fetch it.
- Look at one feature's properties with `head -c 2000` before writing the reader. Expect `name`, `pop_max`, `adm0name`, `adm0cap` (1 for a capital), `featurecla`, `longitude` and `latitude`, but confirm.
- Natural Earth is already credited in the footer, so no new credit is needed.

### 2b. Loader: `loaders/cities/CityLoaderMain <populatedPlaces.geojson> <countriesDir> <outFile>`

Offline and deterministic, like `LakeLoaderMain`.

- Assign each city to a country by testing its point against the `processed/countries` polygons. `CountryLocator` already holds the polygons with bounding boxes and an even-odd test; expose a `countryAt(double[] point)` method on it and reuse that. A coastal city can fall just outside its polygon: if the point is in no country, try four points 0.03° north, south, east and west; if those fail too, match `adm0name` to a country name or alias; otherwise drop the city and count it in the printed summary.
- Per country keep the 40 largest by `pop_max`, and always keep every capital.
- Write the single file `processed/cities.json`: an array of `{country, cities: [{name, lon, lat, population, capital}]}`, countries sorted by name and cities by population descending then name, coordinates rounded to 4 decimals.
- Add the program to the `regenerate-data` profile in `pom.xml` and the file to `scripts/check-processed-data.sh` (the script loops over directories today, so it needs a file comparison too).

### 2c. Backend

- `service/CityService` reads `processed/cities.json` once into primitive-field records (memory rule 2). A snapshot field as in `CountryShapeService`, or `@Cacheable` on a no-argument method, are both fine.
- `model/CountryCitiesRound(gameType, CountryShape country, List<City> cities)`. Its `subjectId()` is `country:<name>`, the same as the existing country games, so a mixed game never asks about one country twice.
- `service/CountryCitiesRoundGenerator` as a `@Service`. Eligible: the country has a shape and at least 8 cities. Put the threshold and its reason in the eligibility helper. Print the eligible count in the loader summary; if it is under 80, report to the user before lowering the threshold.
- `ProcessedDataIT`: every country in `cities.json` has a shape, every eligible country has at least one capital flagged or is listed in a short documented exception list, every coordinate is in range, and at least 80 countries are eligible.

### 2d. Frontend

Add to `games/countries/` (the round has a `country: CountryShape`, so `FocusFit`, `HintLines`, `AnswerOutline` and the hint builders are reused). `makeCountryShapeGame` is typed to `CountryShapeRound`; make it generic over a round type that has `country`.

- Draw each city as a react-leaflet `CircleMarker`, with the radius from the population on a square-root scale between a minimum and a maximum pixel size (constants in `map/constants.ts`). No tooltips while playing; the city name as a tooltip once the round is over.
- Hint 1 redraws the capital in a clearly different style. Hint 2 draws `country.coast` with `HintLines`. Hint 3 is `firstLetterHint`.
- A country with no coast gets nothing from hint 2. Say so in the hint text ("It has no coastline"), which is itself a clue.
- `src/test/leafletMock.tsx` has no `CircleMarker`; add one that records its position and radius so tests can assert what is drawn.
- Add the game to the e2e `GAMES` list.

### Verification

As phase 1, plus the heap figure.

## Phase 3: Country Rivers

### 3a. Loader: `loaders/rivers/CountryRiversMain <rivers.geojson> <lakes.geojson> <countriesDir> <outDir>`

It reads **every** feature of `geodata/rivers/naturalearth/ne_10m_rivers_lake_centerlines_scale_rank.geojson`, not the 50 famous rivers in `processed/rivers`. Each feature has `scalerank` (lower is more important), `rivernum` and `featurecla`.

- **Clipping.** For each segment of each line, test its midpoint and four points 0.03° north, south, east and west of it with `countryAt`. The segment belongs to every country any of the five points is in. This gives a river that runs along a border to both countries, which the game wants. Consecutive segments in the same country form a run.
- **Simplifying.** Use `DouglasPeucker` with the tolerance formula `CountryShapeBuilder` uses for the country (its diagonal divided by 400, clamped), round to 4 decimals and drop runs under 5 km.
- **Size cap.** At most 4,000 points per country. If a country is over, drop its highest `scalerank` runs until it fits.
- **Two widths.** Split the runs into `major` and `minor` by `scalerank`. Print the distribution of `scalerank` first and choose the cut so that roughly a quarter of the total length is major.
- **Lakes (hint 1).** Take every lake of at least 100 km² in `ne_10m_lakes.geojson`, simplified with `LakeGeometry`. Find the countries on its shore with `CountryLocator.locateRings` and give the whole polygon to each of them.
- **Output.** `processed/country-rivers/<file>.json` per country, named as `CountryWriter` names country files: `{name, major, minor, lakes}`, each a GeoJSON feature. Plus `processed/country-rivers/index.json`: `[{name, file, riverCount, riverKm, lakeCount}]` sorted by name, where `riverCount` is the number of distinct `rivernum` values. Write a file only for countries with at least one run.
- Add the program to the `regenerate-data` profile and the directory to `check-processed-data.sh`.

### 3b. Backend

- `service/CountryRiversService`: `index()` reads `index.json` once and keeps it; `load(name)` reads that country's file **on every call, with no cache** (memory rule 3). It reads by exact path with `JsonResourceReader.read`, never with a glob.
- `model/CountryRiversRound(gameType, CountryShape country, GeoData major, GeoData minor, GeoData lakes)`, with `subjectId()` `country:<name>`.
- `service/CountryRiversRoundGenerator` as a `@Service`. `generate` filters the index, so it reads no geometry until a country is picked.
- Eligibility (helper, with reasons): start with at least 5 distinct rivers and at least 1,500 km of river. Print the eligible list in the loader summary and look at it. Aim for 60 to 110 countries; adjust the two thresholds to get there and report the final list to the user.
- `ProcessedDataIT`: every index entry has a file and a shape, every file is at most 4,000 points, and the eligible count is at least 60.

### 3c. Frontend

Add to `games/countries/`, as in phase 2.

- Draw `major` with the clue line style and `minor` with a thinner one.
- Hint 1 draws `lakes` filled. If the country has none, the hint text says "It has no large lakes".
- Hint 2 draws the coast, with the same no-coast text as Country Cities. Hint 3 is the first letter.
- Add the game to the e2e `GAMES` list.

### Verification

As phase 1, plus the heap figure. Also confirm that the per-country read is not cached: the unit test for `CountryRiversService` should call `load` twice and see two reads of the resource.

## Phase 4: finish

1. Record the final heap figure and the total growth under "Results". Confirm it is within the 5 MB budget.
2. Update the docs: the game type list and "Where things live" in the root `CLAUDE.md`; "Game families" in the backend and `client/` files; sources and programs in the loaders `CLAUDE.md`, including the rule that river geometry per country is read on demand and why.
3. Run `./mvnw verify` once, then the e2e suite as `e2e/CLAUDE.md` describes.

## Things that are easy to get wrong

- `GameType` is stored by name and no migration is needed: there is no check constraint on `game_type`, and the stats lookup rows are created on first use.
- `rebuild` must not apply the eligibility filter, and must share its private method with `generate`.
- The JaCoCo gate (95% lines, 92% branches) counts the loaders, and the Vitest thresholds apply to every new frontend file except `*MapWrapper.tsx`. Every new class and component needs its own test.
- Never render a real Leaflet map in a test (see `client/CLAUDE.md`).
- Never print a processed or raw data file in full. Use `head -c` or `grep -o`.
- Data files must be byte-for-byte reproducible: sort everything, round coordinates, and don't depend on map iteration order.

## Not in this plan

- Shrinking the existing country shapes in memory (for example primitive arrays instead of boxed lists). It would free an estimated 20 MB or more, but it changes the model every game uses. Raise it with the user if phase 0 shows the baseline is already close to the limit.
- Changing `-Xmx` or the container size.
- Drop the pin and On the border, which were discussed but not chosen.

## Results

| When | Live heap after warm-up | Change from baseline |
|---|---|---|
| Baseline (`main`) | 42,610K (41.6 MB) | - |
| After phase 1 | 42,752K (41.7 MB) | +142K |
| After phase 2 | 43,235K (42.2 MB) | +625K |
| After phase 3 | 43,593K (42.6 MB; a second run gave 43,288K) | +983K |
