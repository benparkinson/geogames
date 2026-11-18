# Distance clues on wrong guesses, and Hardcore mode

## Goal

When a player makes a wrong guess in a game whose answer is a country, tell them how far the guessed country is from the answer and which way to go: `"France" isn't right. The answer is 1,240 km to the north-east.` The wrong-guess chip also shows the distance and an arrow. A **Hardcore mode** switch, remembered in the browser, turns the clues off.

The client checks answers (root `CLAUDE.md`), so the clues are worked out entirely in the browser. The server only adds a position to each country in `GET /api/countries`.

## Decisions

These are settled. Don't revisit them while implementing.

1. **Which games get clues.** Only the games whose answers are countries and whose map doesn't already show where the answer is:
   - the country games built by `makeCountryShapeGame` (`COUNTRIES_BY_NEIGHBOURS`, `COUNTRIES_BY_COASTLINE`, `COUNTRIES_BY_LAND_BORDERS`, `COUNTRIES_BY_CITIES`, `COUNTRIES_BY_RIVERS`)
   - the pair games built by `makePairGame` (`BORDERS_BY_SHAPE`, `MERGED_COUNTRIES`).

   **Tripoint gets no clues.** The flag sits on a real map at the tripoint, so the player can already see where the answers are, and with three answers a single distance doesn't mean much. **Rivers and lakes get no clues** in this change: their guesses aren't countries, and the client has no positions for them. They could be added later (see Follow-ups).
2. **The position of a country** is the area-weighted centroid of the outer ring of its largest polygon part. Using the largest part keeps France in Europe rather than pulling it towards French Guiana, and the same goes for Russia, the US, Norway and the Netherlands. The backend computes it at runtime from `CountryShape.outline`, which is already in memory, and caches it with the names. No data regeneration is needed. A centroid can fall outside a crescent-shaped country (Croatia, for example). That doesn't matter for a clue measured in hundreds of kilometres.
3. **Distance and direction.**
   - Distance is the great-circle (haversine) distance between the two positions, with an Earth radius of 6371 km, rounded to the nearest 10 km and shown with thousands separators (`1,240 km`).
   - Direction is the initial bearing **from the guessed country to the answer**, given as one of 8 compass points (north, north-east, …).
   - This is the centroid distance that Worldle-style games use: neighbours show a few hundred km, not 0.
4. **Several answers (the pair games).** Measure to the **nearest answer not yet found** at the moment of the guess, and say so in the text ("The nearest country you still need is …"). The clue is worked out once, when the guess is made, and stored on the guess, so finding the other country later doesn't change it.
5. **Hardcore mode** only hides the clues. It doesn't change scoring and the server never knows about it, so the API, the statistics and the database are unchanged. It is stored in `localStorage` under `gg-hardcore` (`"on"` when set; remove the key when it's turned off), and defaults to off, so clues are on. The clues are always computed. The UI shows or hides them, so switching mid-round simply shows or hides them for the guesses already made.
6. Unknown, duplicate and empty guesses get no clue, and nor do correct ones. A reopened round has no guesses (the server doesn't keep them), so it has no clues either.

## Backend

1. **`model/CountryName`**: a new record, `CountryName(String name, List<String> additionalNames, double lat, double lon)`, with a Javadoc saying what the position is. Keep `PlaceName` for rivers and lakes.
2. **`helper/CountryCentroid`**: a final class with a static `Coordinate`-returning or `double[]`-returning method that takes a `GeoData` (Polygon or MultiPolygon):
   - Pick the polygon part whose outer ring has the largest area. Use the shoelace formula on lon/lat with the longitudes scaled by `cos(mean latitude)` of that ring. Only the comparison between parts matters.
   - Return the area-weighted centroid of that outer ring (the shoelace centroid; ignore holes).
   - Round to 2 decimal places.
   - Look at how `GeoData`/`GeoGeometry` expose coordinates (`model/geojson/`) and at `loaders/countries/CountryShapeBuilder.info`, which already picks the largest part offline. Don't depend on the `loaders` package from the app; write the small amount of maths again in the helper.
3. **`CountryLoaderService.loadCountryNames()`** returns `List<CountryName>` with the centroid of each shape. It stays `@Cacheable("countryNames")`, so the work is done once. Update `PlaceNameController.countries()` to return `List<CountryName>`; make `cached` generic.
4. **Tests.**
   - `CountryCentroidTest`: a unit square gives its centre; for a MultiPolygon the largest part wins over a far-away small one; a ring listed clockwise gives the same answer as one listed anticlockwise.
   - `CountryLoaderServiceTest` and `PlaceNameControllerTest`: assert `lat`/`lon` for one or two real countries. Use loose bounds, for example France's centroid is within lat 45–48 and lon 1–4 (mainland, not French Guiana), and Russia's lon is between 40 and 110.
   - Update `GameApiIT` and `ProcessedDataIT` where they use `PlaceName` for countries.
   - `ProcessedDataIT`: every country has a finite position inside its `focus` bounds, widened by 1°.
5. **Contract.** Regenerate `client/src/test/api-shapes.json`: `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend`. Commit it. `GET /api/countries` gains `lat` and `lon` as `number`.

Memory: about 250 small records with two more doubles each, which is negligible. No heap measurement is needed.

## Frontend

### Data

1. `api/placeNames.ts`: add `export interface CountryName extends PlaceName { lat: number; lon: number }` and a `useCountryNames()` hook, or make `usePlaceNames` generic so that `usePlaceNames("countries")` is typed `CountryName[]`. Choose whichever reads better. Keep the query key and the single fetch, because `useGuessList("countries")` already loads this endpoint and the two must share the cache.
2. Update `src/test/fixtures.ts` (the countries fixture gets `lat`/`lon`) so that `contract.test.ts` passes.

### Geometry (pure, unit-tested)

`games/common/distance.ts`:
- `distanceKm(a, b)`: haversine distance.
- `bearingDegrees(from, to)`: initial bearing, 0–360.
- `compassPoint(bearing)`: one of `"north" | "north-east" | … | "north-west"` (8 sectors of 45°, centred on each point).
- `formatKm(km)`: rounds to 10 km and formats with `toLocaleString("en-GB")`.
- Tests (`distance.test.ts`):
  - London (51.51, −0.13) to Paris (48.86, 2.35) is about 344 km at a bearing of about 148°, which is south-east.
  - Bearing due north is 0, east is 90.
  - Points across the antimeridian (lon 179 to −179) are about 222 km apart at the equator, and the bearing points east.
  - Sector edges: 22.4° is north and 22.6° is north-east.
  - `formatKm(1236)` is `"1,240"`.

### Clues on guesses

1. **`guessReducer.ts`**:
   - Add `clue?: GuessClue` to `Guess`, where `interface GuessClue { km: number; bearing: number }`.
   - Add an optional `clueFor?: (guess: string, targets: string[]) => GuessClue | null` to `GuessConfig`.
   - In `applyGuess`, for a **wrong** guess only (including the one that runs out of guesses), call `clueFor(trimmed, unfound)`. Here `unfound` is the canonical answer names not yet found. Attach the result to the new `Guess`, and also to the `wrong` / `outOfGuesses` `GuessResult` (add `clue?: GuessClue` to those two variants).
   - The reducer stays pure: `clueFor` is a deterministic function passed in.
2. **`games/common/useCountryClues.ts`**: a hook that builds `clueFor` from the country list.
   - Build a map from the normalised name and every normalised alias (`normaliseString`) to the position. Look up the guess and each target.
   - Return the clue for the nearest target.
   - Return `null` if the guess or every target is missing. Log `console.error` in development if a target answer isn't in the list, because that would be a data bug.
   - Return `undefined` until the list has loaded.
   - Memoise on the data.
3. **`MapGame`**: a new optional prop `clueFor` (with a doc comment), passed through to the reducer config. The props `clueTargetNoun` and `answerCount` decide the wording (below).
4. **Opting in.** `makeCountryShapeGame` and `makePairGame` call `useCountryClues()` and pass `clueFor`. `TripointGame`, `RiverGame` and `LakeGame` don't change.

### Showing them

1. **`MapGameInput`** receives `showClues: boolean` (true when `!hardcore`):
   - The `wrong` feedback with a clue, for one answer: `"France" isn't right. The answer is 1,240 km to the north-east. 4 guesses left.`
   - For two answers: `… The nearest country you still need is 1,240 km to the north-east. …`
   - `outOfGuesses` keeps its current text. The round is over and the answer is revealed.
   - Keep the existing sentences for no clue or Hardcore mode, so the current tests still pass unchanged.
   - Each wrong-guess `Chip` with a clue and `showClues` shows the distance and an arrow after the name: `✗ France · 1,240 km ↗`.
     - Use `ArrowUp` from `react-bootstrap-icons`, rotated with an inline `style={{ transform: \`rotate(${bearing}deg)\` }}`. That's a transform, not a colour, so the theme check allows it.
     - Mark the arrow `aria-hidden`, and add visually hidden text: `1,240 km north-east`.
2. **The Hardcore mode setting**, in `client/src/settings/hardcoreMode.ts`:
   - `useHardcoreMode(): [boolean, (on: boolean) => void]`, built on `useSyncExternalStore`.
   - Subscribe to the `storage` event (other tabs) and to a custom `gg-hardcore-change` event dispatched on `window` by the setter (this tab).
   - Wrap every `localStorage` access in try/catch, the same as `ThemeToggle`. When storage is blocked, the setting lasts for the page only (keep a module-level fallback value).
   - The server snapshot is `false`.
3. **`components/HardcoreToggle.tsx`**: a labelled switch using the existing UI kit (look at `ui/ToggleGroup` and `ui/Button`; don't add Bootstrap component classes or colours).
   - Label: "Hardcore mode". Description, as a `title` and as small muted text where there's room: "No distance or direction clues after a wrong guess."
   - Place it:
     - on the **home page**, near the game list, where it's visible (not hidden in the footer)
     - in the **game panel** under the guess form, only when the round has clues (`clueFor` was given) and the round isn't over. Use a compact form there ("Hardcore" with a small switch).
4. **The privacy page** (`components/PrivacyPage.tsx`, "On your device"): it now keeps up to two items, `gg-theme` and `gg-hardcore`, each set only if you use its switch.

### Frontend tests

- `distance.test.ts` (above).
- `guessReducer` tests:
  - A wrong guess stores the clue from `clueFor`, and `targets` excludes found answers.
  - No clue is stored for correct, unknown or duplicate guesses.
  - Without `clueFor`, behaviour is unchanged.
- `useCountryClues` test (with the `fetch` router in `src/test/`):
  - An alias guess ("Deutschland") is located.
  - The nearest of two targets is picked.
  - A missing name gives `null`.
- `MapGameInput` / `MapGame` tests:
  - The clue text and the chip distance appear.
  - With Hardcore on (set `localStorage` in the test), neither appears and the old text is shown.
  - Toggling in the panel hides them straight away.
- `hardcoreMode` test: the default is off; the setting persists; it updates across hook instances; it still works when `localStorage.setItem` throws.
- One test each in the country-shape and pair game tests that a wrong country guess shows a distance, and in `TripointGame` that it doesn't.
- `HomePage` test: the switch is there and toggles.

### Browser tests

`e2e/tests/guessing.spec.ts` asserts the wrong-guess feedback text. Update the assertions that now gain a clue. Match with a regex such as `/isn't right\. The answer is [\d,]+ km to the (north|south|east|west)/`. Don't hard-code a distance. Add one test that turns Hardcore on from the home page, plays a country round and sees no "km" in the feedback.

## Docs

- `client/CLAUDE.md`, the **Answers** section: one paragraph on clues. `clueFor` and `GuessClue` live in the reducer config; `useCountryClues` opts a game in; Hardcore mode is `settings/hardcoreMode.ts`; Tripoint, rivers and lakes deliberately have none.
- `client/CLAUDE.md`, **Adding a game**: a game whose answers are countries and whose map hides the location should pass `clueFor={useCountryClues()}`.
- Root `CLAUDE.md`, Game flow / API: `GET /api/countries` also returns `lat`/`lon` (the centroid of the largest part).
## Checks

Run these in order, fixing failures as you go:

1. `bash scripts/check-java.sh`
2. `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend`, then commit `api-shapes.json`
3. `bash scripts/check-frontend.sh`
4. `bash scripts/check-it.sh`
5. `bash scripts/check-no-skipped-tests.sh`
6. The Playwright tests (see `e2e/CLAUDE.md`)

Work on a feature branch off `main` and open a PR. Never commit to `main`.

## Follow-ups (not in this change)

- **Note Hardcore mode on the results page.** The summary is built from the server's round data, so the server would have to record a `hardcore` flag on each answered round, as it does `hintsUsed`. That means a Flyway migration, an API field and a line on the privacy page. Scoring stays the same either way; that was decided.
- **"Neighbour!" instead of a distance** when the guess borders the answer. `CountryShape.neighbours` already has the data; `/api/countries` would need to carry neighbour names.
- **Lakes**: a centroid per lake in `/api/lakes` would give the same clue in Lake Shapes. Rivers are lines, so a clue for them would need a different measure.
- **A proximity percentage or a colour scale** (hot/cold) on the chip.
