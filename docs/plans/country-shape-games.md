# Plan: three country games built from border geometry

Audience: the agent implementing this. Read `CLAUDE.md` first; its conventions (tests, formatting,
Node from `target/node`, no network in tests, never commit to `main`) all apply.

**Already in place (build on it, don't re-implement it):**
- **Hint scoring** (`docs/plans/hint-scoring.md`). `HintConfig` has no `total`, `MapGame` takes
  the hint count from `round.hintCount`, and a game supplies hint content for exactly that many.
- **The guess limit** (`docs/plans/guess-limit.md`):
  - `GameType` has three components, `(answerCount, hintCount, guessLimit)`, and `guessLimit` must
    be ≥ 1. `GameTypeResponse` sends all three.
  - Only *wrong* guesses count. Duplicates and unknown names (rejected by `isKnownGuess`) are free.
  - The last wrong guess ends the round as `OUT_OF_GUESSES` with 0 points.
  - `MapProps.gaveUp` is true for both giving up and running out, so maps that colour the answer
    with `answerPathOptions(gaveUp)` need nothing extra.
  - `test/fixtures.ts` builds every `gameType` / `Round` with a `guessLimit`.

## Goal

Three new game types. All use only the country data we already have:

| GameType                    | Title (home card)    | What the player sees before answering                                                  |
|-----------------------------|----------------------|----------------------------------------------------------------------------------------|
| `COUNTRIES_BY_NEIGHBOURS`   | Country Gaps         | The filled outlines of a country's neighbours on a blank map. The country is the gap. |
| `COUNTRIES_BY_COASTLINE`    | Coastlines           | Only the country's coastline, as lines on a blank map. No land borders.               |
| `COUNTRIES_BY_LAND_BORDERS` | Land Borders         | Only the country's land borders, as lines on a blank map. No coastline.               |

Each has `answerCount` 1, `hintCount` 3 and `guessLimit` 5 (the same limit as every other type),
so the constants are `(1, 3, 5)`. When the round ends (solved, given up or out of guesses), every
game shows the satellite map and the country's full outline in the answer colour.

All three use the country autocomplete and `isKnownGuess`, as Tripoint does. So a misspelled
country is an "unknown" guess and doesn't use up one of the 5.

## Facts established while planning (don't re-derive)

These were measured against `src/main/resources/processed/countries/*.json` (242 files, one of them
Antarctica) with a throwaway script.

- **Shared borders share vertices.** Natural Earth admin-0 comes from one topology, so where two
  countries meet, both polygons have the same vertices. Exact `double` equality **doesn't** quite
  hold. Key vertices by coordinates rounded to **5 decimal places** (about 1 m); with that, every
  shared edge matches. Checks that came out right with 5-decimal keys: every landlocked country
  (Switzerland, Austria, Mongolia, Bolivia, Chad, Zambia, …) has 0 km of coast, and
  France has 10 neighbours including Brazil and Suriname (through French Guiana). With full
  precision, or even 6 decimals, some borders show up as false coast (Zambia 13 km, Slovakia
  87 km at full precision).
- **An edge used by one country is coast; an edge used by two is a land border.** Lakes are inside
  country polygons (Uganda has 0 km of coast, so Lake Victoria isn't "coast"). The Caspian isn't a
  country, so Kazakhstan, Azerbaijan and Turkmenistan have Caspian "coastline". That's correct and
  intended.
- **The antimeridian.** Russia (26 edges), Fiji (18) and Antarctica (27) are split along ±180°.
  Those edges have both ends at |lon| ≥ 179.9999 on the same side. They are neither coast nor
  border: drop them from both, and from the lines you output.
- **Skip Antarctica** entirely, both as a target and as a neighbour.
- **Totals (Antarctica excluded):** 241 countries. 42 have land borders but no coast, and 79
  have no land border (islands). 142 have ≥2 neighbours.
- **Eligible counts with the thresholds below:** coastline game (coast ≥ 150 km and land ≥ 50 km)
  107. Land borders game (land ≥ 300 km and coast ≥ 50 km) 105. The neighbours game needs ≥2
  *mainland* neighbours (defined in 1.2); that count wasn't measured, but expect it to be a bit
  under 142.
- **Payload:** the raw geometry is large (524k points; Canada alone is 68k). Simplifying
  everything at 0.02° with the shared-chain method in 1.2 gives 112k points in all, with Canada the
  largest at 16k. The biggest neighbours round (USA: Canada + Mexico) is about 17k points; the
  median (Panama) is about 760. So these games ship **simplified** geometry generated offline,
  never the raw `processed/countries` files.
- **Simplifying each country on its own would leave gaps** between neighbours. Split each ring into
  runs of edges with the same label (coast, or one neighbour), then simplify each run in a
  canonical direction. Both countries then simplify a shared run identically. The prototype did
  this and the counts above come from it.

## Delivery

`main` is protected. Use one branch and PR per phase, in order, each green on `mvn clean install`
(which runs the Java and Vitest suites) before you open it. Run `mvn spotless:apply` and
`npm run lint` (in `client/`) before each commit.

---

## Phase 0: frontend preparation (no behaviour change)

### 0.1 `useGuessList("countries")`

The country games need Tripoint's autocomplete and spell check. That's the shared hook
`useGuessList(kind)`, specified in `docs/plans/river-lake-autocomplete.md` (F1 and F2: the
`usePlaceNames(kind)` query and `games/common/useGuessList.ts`).
- If it already exists, use it and skip this step.
- Otherwise, build F1 and F2 from that plan here, frontend only. The `"countries"` kind is all that
  works until that plan's backend lands, and that's fine.

`TripointGame` uses `useGuessList("countries")`.

### 0.2 Shared hint text helpers

`games/rivers/riverHints.ts` has hint builders that other families need. Create
`client/src/games/common/hints.ts` with the generic parts:

- `firstLetterHint(name: string): string | null`: "Starts with “D”".
- `listHint(label: string, names: string[], max = 3): string | null`: `null` for an empty list,
  otherwise "Label: A, B, C", or "Label: A, B, C and 6 more" past `max`. If `riverHints.ts`
  already caps `flowsThroughHint` this way (hint-scoring Phase 3), move that logic here.
- `countHint(count: number, singular: string, plural: string, verb: string)`: for example
  `countHint(1, "country", "countries", "Borders")` returns "Borders 1 country".

Rewrite the river builders on top of these, so the river hint text stays exactly the same; the
existing river tests must pass unchanged. Unit test `hints.ts`.

### 0.3 `FitBounds` takes bounds as well as GeoJSON

`map/FitBounds.tsx` takes `{ id, data?: GeoJsonObject, bounds?: L.LatLngBoundsExpression }`. It
uses `bounds` when given, and otherwise `L.geoJSON(data).getBounds()`. The existing callers keep
passing `data`. Add a test case to `FitBounds.test.tsx`.

---

## Phase 1: offline country shapes

### 1.1 Model (`model/`)

```java
/** Simplified display geometry for one country, from processed/country-shapes. */
public record CountryShape(
    String name,
    List<String> additionalNames,
    double coastKm,
    double landBorderKm,
    List<Neighbour> neighbours,   // longest shared border first, ties by name
    Bounds focus,                 // bounding box of the largest polygon part
    GeoData outline,              // simplified Polygon / MultiPolygon
    GeoData coast,                // MultiLineString, may have no lines
    GeoData landBorders) {        // MultiLineString, may have no lines

  /** @param mainland whether it touches the country's largest polygon part */
  public record Neighbour(String name, double km, boolean mainland) {}
}

public record Bounds(double west, double south, double east, double north) {}
```

`GeoGeometry` already accepts `Polygon`, `MultiPolygon` and `MultiLineString`, and a
`MultiLineString` with empty `coordinates` is valid GeoJSON. Round `coastKm` / `landBorderKm` / `km`
to whole kilometres.

`focus` is there because fitting the whole outline breaks for countries split by the antimeridian
(Russia, Fiji, and the USA with the Aleutians would fit the whole world), and because the overseas
parts (France with French Guiana) would shrink the view. The largest part is the one with the
largest shoelace area, worked out in degrees. That's fine for comparing the parts of one country.

### 1.2 `loaders/countries/CountryShapeBuilder` (pure, unit tested)

Input: `List<Country>` (from `ProcessedCountryReader`). Output: `List<CountryShape>`. Points are
`{lon, lat}` `double[]`, as in `loaders/rivers`.

1. **Drop Antarctica.** Read each country's rings: the outer ring and holes of every polygon part,
   for both `Polygon` and `MultiPolygon` (see how `RiverCountryLocator.toPart` walks
   `GeoGeometry.coordinates`).
2. **Index the edges.** The key of a vertex is `String.format("%.5f,%.5f", lon, lat)` (use
   `Locale.ROOT`), and the key of an edge is its two vertex keys, sorted and joined. Map each edge
   key to the set of countries that use it.
3. **Label each edge of each ring:** `SEAM` when both ends have |lon| ≥ 179.9999 with the same sign,
   otherwise the other country's name when two countries use it, otherwise `COAST`. If more than two
   countries use an edge, which would be a data error, throw `IllegalStateException` naming them.
4. **Add up** `coastKm`, `landBorderKm`, and km per neighbour (use `RiverGeometry.haversineKm`;
   make it public if it isn't). `SEAM` edges count towards nothing. A neighbour is `mainland`
   when it shares at least one edge with the country's largest part.
5. **Split each ring into runs** of consecutive edges with the same label. Rotate the ring so it
   starts at a label change. A ring with a single label throughout is one *closed* run.
6. **Simplify each run** with `DouglasPeucker.simplify` (in `loaders.rivers`, or `loaders.features`
   if the lakes plan's Phase 1 has run). Do it so that **both countries get the same points for a
   shared run:**
   - The tolerance of a country is `clamp(diagonal of its focus box / 400, 0.002, 0.02)` degrees,
     so small countries keep their detail. A `COAST` run uses its own country's tolerance. A
     shared run uses the **smaller** of the two countries' tolerances, which both sides work out the
     same.
   - Open run: if the first vertex key is greater than the last, reverse it, simplify it, then
     reverse the result back.
   - Closed run (an island, or an enclave like Lesotho in South Africa, Vatican City in Italy or San
     Marino in Italy): rotate it to start at the smallest vertex key. Pick the direction in which the
     second vertex key is smaller than the second-to-last. Split it at the vertex furthest from the
     start, simplify the two halves, and join them. Then restore the original direction.
   - Round the points to 4 decimals afterwards, as `RiverGeometry` does.
7. **Drop tiny islands:** a ring that is a single `COAST` run with a perimeter under 10 km is
   dropped, unless it's the country's largest part (Nauru, Tuvalu and other micro-states keep
   theirs). Rings with land borders are never dropped.
8. **Output:**
   - `outline`: the simplified rings put back together from their runs (closed, ≥4 points), in
     the original polygon structure. Use `Polygon` if the source was one, else `MultiPolygon`.
   - `coast`: a `MultiLineString` of the simplified `COAST` runs.
   - `landBorders`: a `MultiLineString` of the simplified runs labelled with a neighbour.
   - `additionalNames` is copied from the `Country`.

Tests (`CountryShapeBuilderTest`, hand-made squares in degrees near the equator):
- Two squares sharing an edge with extra vertices on it: each has the other as a neighbour with the
  right km, and that edge is in `landBorders`, not `coast`.
- A square with a seam edge on lon 180: the seam is in neither line set, and no km counts it.
- An enclave (a square inside another's hole): both list each other as neighbours, the inner one
  has `coastKm` 0, and its simplified ring has the **same set of points** as the outer one's hole.
- **Consistency:** a wiggly shared border with many vertices, simplified with a tolerance that
  removes some: country A's run equals country B's reversed.
- Coast-only islands under 10 km are dropped, and a micro-state's only ring is kept.
- `focus` is the largest part's box, and `mainland` is false for a neighbour of a small part.
- Three countries sharing an edge throws `IllegalStateException`.

### 1.3 `loaders/countries/CountryShapeMain`

Usage: `CountryShapeMain <processedCountriesDir> <outDir>`. Read with `ProcessedCountryReader`, build,
clear `*.json` in `outDir` (`CountryWriter.clearOutput`), and write one file per country with
`GeoJson.prettyWriter()`, named with `CountryHelper.convertCountryNameToFileName` (the same file
names as `processed/countries`). Print a summary: the count written, the count with no coast, with
no land border and with ≥2 mainland neighbours, the eligible count for each game (thresholds in
2.2), the 5 largest point counts, and the total bytes.

### 1.4 Generate and check

Build the classpath and run from the repo root, as `docs/plans/river-game-variants.md` 1.6 does:

```
java -cp "target/classes;$(cat target/cp.txt)" com.parkinson.benjamin.geogames.loaders.countries.CountryShapeMain \
  src/main/resources/processed/countries src/main/resources/processed/country-shapes
```

Expect 241 files and the counts in "Facts" (±a few, since the tolerances differ from the
prototype's flat 0.02°). Spot-check these:
- Switzerland: coast 0, land ≈ 1,480 km, 5 neighbours.
- France: 10 neighbours, with Brazil and Suriname `mainland: false`.
- Chile: coast ≫ land.
- Lesotho: one neighbour (South Africa), coast 0.

If the total size is over about 15 MB, raise the tolerance cap to 0.03 and report the size in the
PR.

Then open a few outlines in a GeoJSON viewer you write yourself, or render them in the dev app
after Phase 2, and look for gaps between neighbours.

### 1.5 Runtime loading and integrity

- `service/CountryShapeService`: `loadShape(name)` with `@Cacheable("countryShape")`, reading
  `processed/country-shapes/<file name>`, and `loadShapes()` with `@Cacheable("countryShapes")`,
  reading `classpath*:/processed/country-shapes/*.json`. Both go through `JsonResourceReader`.
  Test it like `CountryLoaderServiceTest`.
- `ProcessedDataIntegrityTest` additions:
  - The country shape names are exactly the processed country names minus Antarctica, with equal
    `additionalNames`. This catches someone re-running `CountryLoaderMain` but not
    `CountryShapeMain`.
  - Neighbours are symmetric: A lists B iff B lists A, with km within 1 km.
  - Every neighbour name loads through `loadShape`, every `outline` ring is closed with ≥4 points,
    and no file has more than 20,000 points across outline, coast and land borders.
  - Each game in Phase 2 has at least 20 eligible countries.
- `CLAUDE.md` pipeline section: add the `CountryShapeMain` bullet and say that editing
  `additionalNames.json` / `excludedNames.json` means re-running `CountryLoaderMain` **and then**
  `CountryShapeMain`.

---

## Phase 2: backend round type, and `COUNTRIES_BY_NEIGHBOURS`

### 2.1 Round data

```java
/** The data for one round of any country-shape game type. */
public record CountryShapeRound(GameType gameType, CountryShape country, List<CountryOutline> neighbours)
    implements GameData {
  public CountryShapeRound { neighbours = neighbours == null ? List.of() : List.copyOf(neighbours); }
  @Override public String subjectId() { return "country:" + country.name(); }
}

/** Just a neighbour's name and outline: the games draw nothing else of it. */
public record CountryOutline(String name, GeoData outline) {}
```

Sending neighbours as `CountryOutline`, rather than the full `CountryShape`, roughly halves the
payload. The subject id is shared by all three games, so a mixed game never asks about France twice.
Tripoint subjects are `tripoint:…`, so they don't clash.

### 2.2 One parameterised generator, registered as beans

`service/CountryShapeRoundGenerator implements RoundGenerator`, **not** a `@Service`, modelled on
`RiverRoundGenerator`:

```java
public CountryShapeRoundGenerator(
    GameType gameType,
    CountryShapeService shapes,
    Random random,
    Predicate<CountryShape> eligible,
    boolean withNeighbours)
```

`generate` filters `shapes.loadShapes()` by `eligible` and by the excluded subjects, picks with
`RandomPicker.pickDistinct(..., "countries")`, and, when `withNeighbours` is set, attaches the
**mainland** neighbours in the stored order as
`new CountryOutline(n.name(), shapes.loadShape(n.name()).outline())`.

`config/CountryGamesConfig` (`@Configuration`) holds the eligibility thresholds as named constants
with a one-line reason each:
- neighbours game: at least 2 mainland neighbours, `withNeighbours = true`;
- coastline game (Phase 3): `coastKm >= 150 && landBorderKm >= 50`, `withNeighbours = false`. Under
  150 km a coast is just a short line (Iraq 91, Belgium 95). Without land borders it's the whole
  outline, which makes it a different, easier game;
- land borders game (Phase 4): `landBorderKm >= 300 && coastKm >= 50`, `withNeighbours = true`.
  Landlocked countries are left out, because their "land borders only" is the whole outline.

`CountryShapeRoundGeneratorTest`: type, eligibility filter, exclusion, neighbours attached
(mainland only, in order) or not, and too few eligible → `IllegalArgumentException`.

### 2.3 `GameType.COUNTRIES_BY_NEIGHBOURS(1, 3, 5)`

Add the constant and its bean. Update `GameTypeTest` (its per-type tables of hint counts and guess
limits), `GameSelectionTest`,
and add `GameApiIntegrationTest.createsACountriesByNeighboursGame` (10 rounds, then parse a
`jsonBlob` and check it has `country.outline` and a non-empty `neighbours`).

### 2.4 Frontend: the country-shape family (`client/src/games/countries/`)

Follow the river family's shape: shared plumbing, then one small module per game.

- `model.ts`: `CountryShape`, `Neighbour`, `Bounds`, `CountryOutline` and `CountryShapeRound`,
  mirroring the Java records. Reuse `CountryGeoData` from `common/country.ts` for `outline`, and add
  a `LineGeoData` (a `MultiLineString` feature) for `coast` / `landBorders`. If
  `rivers/model.ts`'s `RiverGeoData` has the same shape, move it to `common/` and share it.
- `CountryShapeGame.tsx`: `makeCountryShapeGame(variant)`, like `makeRiverGame`:
  ```tsx
  export interface CountryShapeVariant {
    MapComponent: ComponentType<MapProps<CountryShapeRound>>;
    prompt: string;
    mapCaption: string;
    hints: (round: CountryShapeRound) => HintConfig;
  }
  ```
  The component renders `MapGame` with `blankMap`, the caption,
  `answers={[{ name: country.name, additionalNames: country.additionalNames }]}` (check the
  `AnswerCandidate` shape in `common/answers.ts`), `inputPlaceholder="Country name…"`,
  `answerNoun="country"`, and the `suggestions` / `isKnownGuess` from `useGuessList("countries")`.
- `countryHints.ts`: pure builders on `CountryShapeRound`, unit tested:
  - `neighbourCountHint`: `countHint(all neighbours, "country", "countries", "Borders")`, counting
    every neighbour, not only the mainland ones.
  - `neighbourNamesHint`: `listHint("Borders", mainland neighbour names)`, longest border first,
    capped at 3.
  - `firstLetterHint` from `common/hints.ts`.
  - `mapHint(text)`: a builder that always returns the given text, for hints that change the map.
- `CountryLayers.tsx` (it imports react-leaflet, so only `*Map.tsx` files import it):
  - `FocusFit({ country })`: `FitBounds` with `bounds` from `country.focus`
    (`[[south, west], [north, east]]`), and `id = country.name`.
  - `NeighbourOutlines({ neighbours })`: one `GeoJSON` per neighbour with `clueOutlineOptions()`.
  - `ClueLines({ data, id })`: a `GeoJSON` line layer for the clue lines while playing. Add
    `clueLineOptions()` (`CLUE_COLOR`, weight 3, no fill) to `map/constants.ts`.
  - `HintLines({ data, id })`: the same, but dashed and fainter: `hintLineOptions()`
    (`dashArray: "6 6"`, opacity 0.7).
  - `AnswerOutline({ country, gaveUp })`: the outline with `answerPathOptions(gaveUp)` and the
    `key` trick from `RiverLine`, so the colour changes on give-up. `gaveUp` is also true when the
    round ran out of guesses, so that case needs no code of its own.
  - `RevealTiles` is in `rivers/RiverLayers.tsx`. Move it to `map/RevealTiles.tsx` and import it in
    both families.
  - Give every `GeoJSON` a `key` built from the country name, because react-leaflet's `GeoJSON`
    doesn't redraw when `data` changes.
- Every map: `MapContainer zoom={3} minZoom={2} maxZoom={10} center={[0, 0]}`, a `FocusFit`, and
  a `MapResizer`. The pan and zoom are free, since it's a blank map with nothing to find by exploring.

### 2.5 `neighbours.ts` + `NeighboursMap.tsx` + `NeighboursMapWrapper.tsx`

- Map, while playing: `NeighbourOutlines` always. When `hintsUsed >= 1`, also
  `HintLines(country.coast)`. Once the round is over: `RevealTiles`, the neighbour outlines and
  `AnswerOutline`.
- Module: type `COUNTRIES_BY_NEIGHBOURS`, title "Country Gaps", singular "Country by neighbours",
  description "Name the country hidden between its neighbours.", icon `PuzzleFill`
  (react-bootstrap-icons; pick another if it doesn't exist), prompt "Name the missing country.",
  caption "Only its neighbours are shown. The country is the gap.".
- Hints, in order:
  1. the coastline drawn on the map. The text is "Its coastline is on the map", or "It has no
     coastline" when `coastKm === 0`;
  2. `neighbourNamesHint`;
  3. `firstLetterHint`.
- Wrapper: literally the `dynamic(() => import("./NeighboursMap"), { ssr: false, ... })` pattern.
- Add it to `GAME_MODULES` after the river games, and update `registry.test.ts`.
- `test/fixtures.ts`: `makeCountryShapeData(overrides?)` and `makeCountryShapeRoundData(overrides?)`,
  built from the existing `square(n)` helper, plus `makeCountryShapeRound` for the API response
  (`gameType: { name: "COUNTRIES_BY_NEIGHBOURS", answerCount: 1, hintCount: 3, guessLimit: 5 }`,
  following how the existing river fixtures fill in `gameType`).
- `neighbours.test.tsx` (mock `./NeighboursMapWrapper`): the prompt and caption, all 3 hint texts in
  order and non-null for every index below the fixture's `hintCount`, the landlocked text for
  hint 1, a correct guess by an alias, and an unknown name rejected once the country list loads,
  without the guesses-left count going down. The limit itself is tested in `MapGame` /
  `guessReducer`, so don't repeat that here.

---

## Phase 3: `COUNTRIES_BY_COASTLINE`

- Backend: `GameType.COUNTRIES_BY_COASTLINE(1, 3, 5)` and its bean (thresholds in 2.2, no neighbours).
  Update `GameTypeTest`, `GameSelectionTest`, and add an integration test.
- `CoastlineMap.tsx` + wrapper. While playing: `ClueLines(country.coast)`. When `hintsUsed >= 2`,
  also `HintLines(country.landBorders)`, which completes the outline. Once the round is over:
  `RevealTiles` and `AnswerOutline`.
- `coastline.ts` module: title "Coastlines", singular "Country by coastline", description "Name a
  country from its coastline alone.", icon `Water` is taken by rivers, so use `Tsunami` or `Life
  Preserver` (check what exists). Prompt "Name the country with this coastline.", caption
  "Coastline only. Land borders are hidden.".
- Hints, in order:
  1. `neighbourCountHint`;
  2. the land borders on the map, with the text "Its land borders are on the map";
  3. `firstLetterHint`.
- Registry, fixtures and tests as in Phase 2.

## Phase 4: `COUNTRIES_BY_LAND_BORDERS`

- Backend: `GameType.COUNTRIES_BY_LAND_BORDERS(1, 3, 5)` and its bean (thresholds in 2.2, with
  neighbours). Same test updates.
- `LandBordersMap.tsx` + wrapper. While playing: `ClueLines(country.landBorders)`. When
  `hintsUsed >= 1`, also `NeighbourOutlines`. When `hintsUsed >= 2`, also `HintLines(country.coast)`.
  Once the round is over: `RevealTiles`, `AnswerOutline` and the neighbours.
- `landBorders.ts` module: title "Land Borders", singular "Country by land borders", description
  "Name a country from its land borders alone.", icon `BoundingBox` (or similar), prompt "Name the
  country with these land borders.", caption "Land borders only. The coastline is hidden.".
- Hints, in order:
  1. the neighbours' outlines on the map, with the text "Its neighbours are on the map";
  2. the coastline on the map, with the text "Its coastline is on the map";
  3. `firstLetterHint`.
- Registry, fixtures and tests as in Phase 2.

## Phase 5: wrap-up

- `CLAUDE.md`, "Game types": describe the country-shape family next to the river family:
  - `CountryShape` is the file format and `CountryShapeRound` the round data;
  - `CountryGamesConfig` registers the beans;
  - `makeCountryShapeGame` builds the components;
  - the `country:` subject id;
  - `useGuessList` and `common/hints.ts`;
  - removing a variant works as for rivers.
- Run `npm run dev` with the backend. Check the home page cards at phone width and on desktop, and
  play a round of each new game. Include giving up, running out of guesses (the reveal must look
  the same as giving up), checking that a misspelled country doesn't use up a guess, every hint, the
  reveal, and countries split by
  the antimeridian (Russia, Fiji, USA) and with overseas parts (France, Netherlands, Denmark).
  Watch the payload size in the network tab for the USA/Canada/China rounds.

## Out of scope (mention in the PR descriptions, don't build)

- **Mix weights.** With these three games there are 8 types, 4 of them rivers, and `RANDOM` picks
  types uniformly. `docs/plans/river-game-variants.md` Phase 6 describes weights by family. It
  becomes worth doing now.
- **River Countries could use `CountryShape.outline`** instead of full country geometry, which would
  cut its payload by about 5×. That's a small follow-up once this has landed.
- Continents as a hint: the country data has no continent today.
