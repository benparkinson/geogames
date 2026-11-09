# Plan: three new river game variants

Audience: the agent implementing this. Read `CLAUDE.md` first; its conventions (tests, formatting,
Node from `target/node`, no network in tests, never commit to `main`) all apply.

## Goal

`RIVERS_BY_SHAPE` is too hard. Add three variants that use the same 150 rivers, and include them in
the `RANDOM` mix:

| GameType              | Title (home card)  | What the player sees before answering                                   |
|-----------------------|--------------------|-------------------------------------------------------------------------|
| `RIVERS_ON_MAP`       | Rivers on the Map  | The river line on satellite imagery. Zoom is limited, no panning.       |
| `RIVERS_BY_ENDS`      | River Ends         | Two flags on satellite imagery: where the river starts and where it ends. No line. |
| `RIVERS_BY_COUNTRIES` | River Countries    | The outlines of the countries the river flows through, on a blank map.  |

All three have `answerCount` 1. After the round ends (correct or gave up) every variant reveals the
satellite map and the river line, as the shape game does now.

## Facts established while planning (don't re-derive)

- **The ends aren't in the data yet.** Processed rivers are `MultiLineString`s whose lines often
  don't touch (only 62 of 150 chain end to end; the Danube has 30 separate pieces). The ends can be
  found by building a graph whose nodes are line ends, joining ends of different lines that are
  within 25 km, and taking the two nodes furthest apart by shortest path. This was prototyped
  against the real data and gives plausible ends for every river.
- **The flow direction is mostly in the data.** Natural Earth digitises most lines downstream.
  Voting along that path (+km for each line walked start→end, −km for each line walked end→start)
  gives a confident direction (|vote| / km ≥ 0.6) for 131 of 150 rivers. Of the 19 low-confidence
  rivers, these 7 come out **reversed** and need an override: Argun River, Don, Niger, Orinoco
  River, Tocantins River, Vilyuy, Vistula. The other 12 (Arkansas, Dniester, Ebro, Helmand,
  Irtysh, Jordan, Loire, Oka, Rhone, Shannon, Tobol, White Nile) come out right.
- **Countries per river can be computed offline** from `processed/countries/*.json`: sample points
  every ≤2 km along each line, test them against the country polygons, and keep countries with ≥10 km.
  Result: 69 rivers cross 1 country, 37 cross 2, 33 cross 3, 11 cross 4–9 (the Danube crosses 9).
  So **81 rivers are eligible** for the countries game (≥2 countries). A one-country river is
  unguessable from a single outline, so leave those out.
- **Payload.** Country geometry is large (Canada.json 2.9 MB, USA 1.6 MB and Russia 1.5 MB,
  pretty-printed). A countries round would carry a median of about 450 kB and up to about 4.5 MB.
  Tripoint rounds already work this way, so ship it and measure. Phase 6 is the fix if it's slow.
- The satellite tiles (`SATELLITE_TILE_URL`, ArcGIS World_Imagery) have no labels, so showing them
  before the answer doesn't give names away.

## Evaluation of the "add a game" framework

What works well and should stay:
- Backend: generators are discovered by type, startup fails if a type has no generator (or two),
  `GameSelection` handles `RANDOM`, and `RandomPicker` takes a seeded `Random`.
- Frontend: `defineGame` with one registry, the generic `MapGame`, centralised answer matching,
  and a summary page and home page that need no edits.

What makes these variants awkward:
1. **Backend: data shape is tied to one game type.** `River implements GameData` and hard-codes
   `gameType()` to `RIVERS_BY_SHAPE`. `River` is also the processed file format. Several game types
   can't share it. → Phase 0.1.
2. **Backend: each variant would need its own copy of the same generator.** The four river
   generators differ only in type, which rivers are eligible, and whether to attach countries.
   → one parameterised generator class, registered as four beans (Phase 0.2).
3. **Backend: a mixed game can repeat a subject.** Once several types draw on the same rivers, a
   `RANDOM` game can show the Danube twice (by shape, then on the map). → generators take a
   set of excluded subjects (Phase 0.3).
4. **Backend: the mix is uniform per type.** With 5 types, 80% of mixed rounds will be rivers.
   For the trial this is fine, because it gives the new variants exposure. Phase 6 suggests weights.
5. **Frontend: one boolean controls two things in `MapGame`.** `blankMapCaption` both blanks the
   map and shows a caption. The ends game wants a caption on a satellite map. → split into
   `blankMap` + `mapCaption` (Phase 0.4).
6. **Frontend: each variant would copy the shape component.** → `makeRiverGame(config)` (Phase 0.5),
   shared river map layers and a shared hint builder.
7. **Frontend: no locked-view helper.** Two variants need "fit to these bounds, limit zoom, no
   panning, unlock when the round ends" → `map/LockedView.tsx`.
8. **Frontend: the `Country` type lives in `tripoint/model.ts`.** → move it to a shared module.

Fixing these makes a future variant about 3 small frontend files plus one `@Bean` and one enum
constant.

## Delivery

`main` is protected. Use one branch and PR per phase, in order, each green on `mvn clean install`
(which runs the Java and Vitest suites) before you open it. Run `mvn spotless:apply` and
`npm run lint` (in `client/`) before each commit.

---

## Phase 0: framework changes (no new game types)

The behaviour must not change: the existing two games work exactly as before.

### 0.1 Separate the river file format from the round data

- `model/River`: no longer `implements GameData` (drop `gameType()`). In Phase 1 it gains fields.
- New `model/RiverRound`:
  ```java
  public record RiverRound(GameType gameType, River river, List<Country> countries)
      implements GameData {
    public RiverRound { countries = countries == null ? List.of() : List.copyOf(countries); }
    @Override public String subjectId() { return "river:" + river.id(); }
  }
  ```
  `gameType` gets serialised into the blob too. That's harmless, because the frontend ignores it.
- The jsonBlob for rivers becomes `{gameType, river: {...}, countries: [...]}` instead of a bare
  river. The frontend changes this needs are in 0.5. Games live in in-memory H2, so no old data
  needs migrating.

### 0.2 One parameterised river generator

- Replace `service/RiverFinderService` with `service/RiverRoundGenerator implements RoundGenerator`.
  It is **not** annotated `@Service`.
  ```java
  public RiverRoundGenerator(
      GameType gameType,
      RiverLoaderService rivers,
      Random random,
      Predicate<River> eligible,
      Function<River, List<Country>> countries)
  ```
  `generate` filters `rivers.loadRivers()` by `eligible` and by the excluded subjects (0.3), then
  `RandomPicker.pickDistinct(..., "rivers")`, then maps each river to
  `new RiverRound(gameType, river, countries.apply(river))`.
- New `config/RiverGamesConfig` (`@Configuration`) with one `@Bean RoundGenerator` per river type.
  In this phase there's only `riversByShape` (`eligible = r -> true`, `countries = r -> List.of()`).
- Rename `RiverFinderServiceTest` to `RiverRoundGeneratorTest` and cover type, eligibility filter,
  exclusion, countries attached, and too few eligible rivers → `IllegalArgumentException`.

### 0.3 No repeated subject in a game

- `GameData` gains `String subjectId()`. `Tripoint` returns `"tripoint:" +` its country names,
  sorted and joined with `|`.
- `RoundGenerator.generate(int count, Set<String> excludedSubjects)` replaces `generate(int)`.
  `TripointFinderService` filters its definitions the same way. Build the id from the definition's
  country names so countries don't load just to be filtered.
- `GameService.generateMixedRounds`: walk `roundsPerType` (EnumMap order, deterministic), keep a
  `Set<String> used`, pass it to each generator and add the ids of what comes back. Single-type
  games pass `Set.of()`.
- Tests: `GameServiceTest` checks that a mixed game passes the earlier generators' subjects to later
  ones. `GameApiIntegrationTest.createsARandomGame` checks that the subject ids are distinct (parse
  each round's `jsonBlob`).

### 0.4 `MapGame`: split the blank-map flag from the caption

- Replace `blankMapCaption?: string` with `blankMap?: boolean` (grey background while playing) and
  `mapCaption?: string` (shown while playing, on any map).
- `showBlankMap = !!blankMap && !gameOver` and the caption shows when `!!mapCaption && !gameOver`.
- Update `RiverShapesGame` (`blankMap` plus the same caption) and `MapGame.test.tsx`.

### 0.5 Frontend river family plumbing

- Move `TripointCountry` / `TripointGeoData` to `client/src/games/common/country.ts` as `Country` /
  `CountryGeoData`. `tripoint/model.ts` imports them (keep the old names as type aliases only if
  that keeps the diff small).
- `games/rivers/model.ts`: keep `River`, and add
  `export interface RiverRound { river: River; countries: Country[] }`.
- `games/rivers/RiverGame.tsx`: a factory so each variant is configuration only:
  ```tsx
  export interface RiverVariant {
    MapComponent: React.ComponentType<MapProps<RiverRound>>;
    prompt: string;
    blankMap?: boolean;
    mapCaption?: string;
    hints: (river: River) => HintConfig;
  }
  export function makeRiverGame(variant: RiverVariant) {
    function RiverGame({ data, round, answerCount, submitAnswer }: GameComponentProps<RiverRound>) {
      return <MapGame data={data} answers={[data.river]} inputPlaceholder="River name…" answerNoun="river" ... />;
    }
    return RiverGame;
  }
  ```
- `games/rivers/riverHints.ts`: pure helpers that return hint text: `continentHint`,
  `flowsThroughHint` ("Flows through: France, Switzerland", or null when the list is empty), and
  `firstLetterHint` ("Starts with “R”"). Add `textHints(river, ...builders): HintConfig`, which
  makes `total` the builder count and `text(i)` call builder `i`. Unit test it.
- `games/rivers/RiverLayers.tsx` (it imports react-leaflet, so only `*Map.tsx` files import it):
  - `RiverLine({ river, gaveUp })`: the `GeoJSON` line with `answerPathOptions(gaveUp)` and the
    `key` trick from `RiverShapesMap`.
  - `RevealTiles({ show })`: the satellite `TileLayer` when `show`.
  - (Phase 3 adds `EndFlags`, Phase 4 adds `CountryOutlines`.)
- Rework the shape game onto these. Its module uses `makeRiverGame` with `RiverShapesMap` (which now
  takes `MapProps<RiverRound>`), `blankMap: true`, the existing caption and prompt, and
  `hints: r => textHints(r, continentHint)`. Delete `RiverShapesGame.tsx` and move its tests to
  `RiverGame.test.tsx` (they test the factory with the shape config). Change the module title to
  "River Shapes" and singular to "River shape" so it reads well beside the new variants.
- `answerNames: (round) => [round.river.name]`.
- `test/fixtures.ts`: `makeRiverData()` returns a `River`; add
  `makeRiverRoundData(overrides?)` returning `{ river: makeRiverData(), countries: [] }`. Make
  `makeRiverRound` (the API response) use it.

### 0.6 `map/LockedView.tsx` (built now, used from Phase 2)

```tsx
export function LockedView({ bounds, id, locked, zoomOut = 1, zoomIn = 1 }: {
  bounds: L.LatLngBoundsExpression; id: string; locked: boolean; zoomOut?: number; zoomIn?: number;
}): null
```
- On mount / `id` change: `fit = map.getBoundsZoom(bounds, false, L.point(48, 48))`,
  `map.setMinZoom(fit - zoomOut)`, `map.setMaxZoom(fit + zoomIn)`, then
  `map.setView(L.latLngBounds(bounds).getCenter(), fit)`.
- While `locked`: `map.dragging.disable()`, `map.keyboard.disable()`, `map.boxZoom.disable()`.
  When `locked` becomes false (the round ended): enable them again and widen the zoom to
  `setMinZoom(2)` / `setMaxZoom(10)`, so the player can explore the reveal.
- The map using it must also pass `scrollWheelZoom="center"`, `touchZoom="center"` and
  `doubleClickZoom="center"` to `MapContainer`. Without those, zooming at the cursor pans the map.
- Test it the way `FitBounds.test.tsx` / `CenterUpdater.test.tsx` mock `useMap`.

---

## Phase 1: river data pipeline (ends and countries)

### 1.1 Model

`model/River` gains `Coordinate source, Coordinate mouth, List<String> countries` (country names
exactly as `Country.name`, sorted by km inside the country, longest first, ties by name).
`client/src/games/rivers/model.ts` `River` gains
`source: Coordinate; mouth: Coordinate; countries: string[]`. Move the tripoint `TripointCoordinate`
to `common/country.ts` as `Coordinate` too.

### 1.2 `loaders/rivers/RiverEnds` (pure, unit tested)

`static Ends find(List<List<double[]>> lines)` → `record Ends(double[] source, double[] mouth, double confidence)`.
Points are `{lon, lat}` as everywhere in `loaders/rivers`.
1. Nodes are each line's first and last point (node `2i` = start of line `i`, `2i+1` = end).
2. Edges: each line joins its two ends, weighted by `RiverGeometry.lineKm`. Gap edges join any two
   ends of *different* lines within `GAP_KM = 25`, weighted by `RiverGeometry.haversineKm`.
3. Run Dijkstra from every node (at most about 80 nodes, so O(n²) per run is fine). The ends are the
   pair with the greatest finite distance. Break ties by lowest (i, j), to be deterministic.
4. Rebuild that path and vote: +km for each line edge walked start→end, −km for end→start. Gap edges
   don't vote. If the vote is < 0, swap the ends. `confidence = |vote| / line km on the path`
   (0 when the path has no line edges).
Tests: a single line (source is its first point, confidence 1), a reversed line, two lines with a
10 km gap, two lines with a 40 km gap (they don't join, so the longer line wins), a branched
"Y" shape (the longest path wins), and a mixed-direction chain (confidence < 1).

### 1.3 `loaders/rivers/RiverCountryLocator` (pure, unit tested)

- Constructor takes `List<Country>`. Parse each polygon once into `double[][]` rings, with a bounding
  box per polygon part (`Polygon` and `MultiPolygon`; see `TripointHelper` for how
  `GeoGeometry.coordinates` is shaped). Skip Antarctica.
- `List<String> locate(List<List<double[]>> lines)`: for each segment, take
  `n = max(1, ceil(km / 2))` sample points at the midpoints of `n` equal parts, each worth
  `km / n`. Find the first country whose polygon contains the point (bbox check first, then
  even-odd ray casting on the outer ring, and the point must not be in a hole). Add up km per country,
  keep those with `MIN_KM = 10`, and sort longest first, then by name.
- Also expose the km that fell in no country, for the summary.
- Tests: hand-made squares (the river crosses two, passes 5 km through a third which gets dropped, and
  passes through a hole).

### 1.4 Overrides

`RiverOverride` gains `Boolean reverseFlow`, `Coordinate source` and `Coordinate mouth` (explicit
values win over computed ones; `reverseFlow` swaps the computed ends). Add `"reverseFlow": true` to
`geodata/rivers/overrides.json` for the 7 reversed rivers listed above. Find their QIDs from
the file names in `src/main/resources/processed/rivers/` (`<slug>-<QID>.json`). Overrides are keyed
by QID, and a river that already has an entry gets the new field merged into it. **Note:** in
`RiverAssembler`, any non-`exclude` override forces a river in. That's fine here, because all 7 are
already selected, but check that the river count stays at 150.

### 1.5 Wire it into `RiverLoaderMain` / `RiverWriter`

- New positional argument: `<naturalEarth> <wikidata> <overrides> <osmDir> <countriesDir> <outDir> [maxRivers=150]`.
  Read the countries with `ProcessedCountryReader`.
- After selection, compute ends (applying overrides) and countries for each selected river, and pass
  them to `RiverWriter` (for example a `Map<String qid, RiverFacts>` with
  `record RiverFacts(Coordinate source, Coordinate mouth, double flowConfidence, List<String> countries)`).
  Note that `Coordinate` is `(latitude, longitude)`, while loader points are `{lon, lat}`.
- The summary also prints: rivers with `flowConfidence < 0.6` and no override (for review), the
  count of rivers per number of countries, and rivers with more than 50 km in no country.
- Update `CLAUDE.md` (the pipeline bullet for `RiverLoaderMain`, and the overrides field list).

### 1.6 Regenerate and check

- Build the classpath and run the loader from the repo root (Windows classpath separator `;`):
  ```
  mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
  java -cp "target/classes;$(cat target/cp.txt)" com.parkinson.benjamin.geogames.loaders.rivers.RiverLoaderMain ^
    geodata/rivers/naturalearth/ne_10m_rivers_lake_centerlines_scale_rank.geojson geodata/rivers/wikidata.json ^
    geodata/rivers/overrides.json geodata/rivers/osm src/main/resources/processed/countries src/main/resources/processed/rivers
  ```
- Expect 150 rivers, 81 (±a few) with ≥2 countries, and the low-confidence list to show only the 12
  correct rivers named above.
- Spot check the source and mouth of the 20 rivers with the most sitelinks against known geography (the source should be
  inland, upstream). Add `reverseFlow` or explicit coordinates for any that are wrong, and re-run.
- `ProcessedDataIntegrityTest`: every river has a non-null `source`/`mouth` with source ≠ mouth,
  every name in every `river.countries` loads through `CountryLoaderService.loadCountryByName`, and
  at least 20 rivers have ≥2 countries (the countries game's 10-round maximum, plus margin for the
  mixed-game exclusions).

---

## Phase 2: `RIVERS_ON_MAP`

Backend:
- `GameType.RIVERS_ON_MAP(1)`, and a `@Bean` in `RiverGamesConfig` (all rivers eligible, no countries).
- Update `GameTypeTest`, `GameSelectionTest` and add `GameApiIntegrationTest.createsARiverOnMapGame`.

Frontend (`games/rivers/`):
- `RiverOnMapMap.tsx` + `RiverOnMapMapWrapper.tsx` (the wrapper is literally the `dynamic(() => import("./RiverOnMapMap"), { ssr: false, ... })` pattern):
  `MapContainer` with `dragging={false} keyboard={false} scrollWheelZoom="center" touchZoom="center" doubleClickZoom="center"`.
  Always render the satellite `TileLayer` and `RiverLine`, then
  `<LockedView bounds={L.geoJSON(river.geoData).getBounds()} id={river.id} locked={!gameOver} zoomOut={1} zoomIn={1} />`
  and `MapResizer`.
- `onMap.ts` module: type `RIVERS_ON_MAP`, title "Rivers on the Map", singular "River on map",
  description "Name a river drawn on a satellite map.", icon `Map` (react-bootstrap-icons), Component
  `makeRiverGame({ prompt: "Name the river shown on the map.", mapCaption: "No panning: zoom is limited.", hints: r => textHints(r, flowsThroughHint, firstLetterHint) })`.
- Add it to `GAME_MODULES` (after the shape game) and update `registry.test.ts`.
- Tests: `RiverGame.test.tsx` with this config (prompt, caption, both hints in order, hints disabled
  when used up, accepts an alias). Mock `./RiverOnMapMapWrapper`.

## Phase 3: `RIVERS_BY_ENDS`

Backend: `GameType.RIVERS_BY_ENDS(1)` and a bean (eligible: `source != null && mouth != null`). Same
test updates.

Frontend:
- `map/flagIcons.ts`: `startFlagIcon` and `endFlagIcon` as `L.divIcon` with an inline SVG flag
  (green for the start, red for the end) plus CSS in `styles.css`. Anchor them at the bottom of the pole,
  like `TripointMap`'s icon.
- `RiverLayers.tsx` `EndFlags({ river })`: two `Marker`s, each with a `Tooltip` ("Start" / "End").
- `RiverEndsMap.tsx` + wrapper: satellite tiles and `EndFlags` always, `RiverLine` only when
  `gameOver`, and `LockedView` on the bounds of the two ends with `zoomOut={2} zoomIn={1}` (fitting
  the two points only, so the river's extent isn't given away; the extra zoom-out gives context).
- `ends.ts` module: title "River Ends", singular "River ends", description "Name a river from where
  it starts and ends.", icon `Signpost2`, prompt "Name the river that flows from the green flag to the
  red flag.", hints `flowsThroughHint, firstLetterHint`.
- Registry and tests as in Phase 2.

## Phase 4: `RIVERS_BY_COUNTRIES`

Backend:
- `GameType.RIVERS_BY_COUNTRIES(1)`. Bean: eligible `r.countries().size() >= 2`; countries
  `r -> r.countries().stream().map(countryLoaderService::loadCountryByName).toList()`.
- `RiverRoundGeneratorTest` covers the countries being attached in order. Add an integration test
  that creates a 10-round game of this type.

Frontend:
- `RiverLayers.tsx` `CountryOutlines({ countries })`: one `GeoJSON` per country with a neutral
  style (add `CLUE_COLOR` / `clueOutlineOptions()` to `map/constants.ts`, distinct from the
  answer colours).
- `RiverCountriesMap.tsx` + wrapper: `CountryOutlines` always, `FitBounds` on `river.geoData`
  (not on the countries, because Russia crosses the antimeridian and would fit the whole world),
  `EndFlags` when `hintsUsed >= 1` or `gameOver`, and satellite tiles and `RiverLine` when `gameOver`.
  Free pan and zoom (it's a blank map, so there's nothing to find by exploring).
- `countries.ts` module: title "River Countries", singular "River by countries", description "Name a
  river from the countries it flows through.", icon `Globe`, `blankMap: true`, prompt "Name the river
  that flows through all of these countries.", hints:
  `{ total: 2, description: "Shows where the river starts and ends, then its first letter", text: i => i === 0 ? "Start and end flags are on the map" : firstLetterHint(river) }`.
  (Build this one by hand, not with `textHints`, because hint 0 changes the map.)
- `fixtures.ts`: `makeRiverRoundData({ countries: [...] })` reuses the existing square polygons.

## Phase 5: wrap-up

- `CLAUDE.md`: in "Game types", document the river family (one `River` format, `RiverRound`
  wrapping it, `RiverGamesConfig` beans, `makeRiverGame`), say that generators can be a `@Bean`
  rather than a `@Service`, and describe `subjectId` / excluded subjects. Also update the
  `MapGame` caption props and the new pipeline argument.
- Check the home page with 6 cards at phone width and desktop (`npm run dev` + backend), and play one
  round of each new type, including give up, hints and reveal.
- Removing a variant after the trial means deleting its enum constant, its `@Bean`, its module and its
  registry entry, and its map pair.

## Phase 6 (optional, after trying the variants)

- **Payload:** if countries rounds are slow, simplify country geometry for display at runtime. Add a
  cached `CountryLoaderService.loadDisplayCountry(name)` that runs the existing `DouglasPeucker`
  (tolerance about 0.02°) over each ring. Don't simplify the processed files, because
  `TripointSnapper` needs full precision. Tripoints could use it too.
- **Mix weights:** if mixed games feel river-heavy, give `GameType` a `mixWeight` (or a family, picked
  uniformly first) and draw types by weight in `generateMixedRounds`.
