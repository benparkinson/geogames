# Plan: a shared pipeline for named features, then Lake Shapes

Audience: the agent implementing this. Read `CLAUDE.md` first; its conventions (tests, formatting,
Node from `target/node`, no network in tests, never commit to `main`) all apply. Also read
`docs/plans/river-import.md`, which explains the river pipeline this plan generalises.

**Already in place (build on it, don't re-implement it):**
- **Hint scoring** (`docs/plans/hint-scoring.md`). `HintConfig` has no `total`, `MapGame` takes
  the hint count from `round.hintCount`, and a game supplies hint content for exactly that many.
- **The guess limit** (`docs/plans/guess-limit.md`):
  - `GameType` has three components, `(answerCount, hintCount, guessLimit)`, and `guessLimit` must
    be ≥ 1. `GameTypeResponse` sends all three.
  - Only *wrong* guesses count; the last one ends the round as `OUT_OF_GUESSES` with 0 points.
  - `MapProps.gaveUp` is true for both giving up and running out.
  - `test/fixtures.ts` builds every `gameType` / `Round` with a `guessLimit`.

Phases 1 and 2 are offline tooling and don't touch either.

## Goal

1. **Generalise the offline river pipeline** so the parts that aren't about rivers (Natural Earth
   grouping by Wikidata ID, the Wikidata fetch, continents, aliases, fame-ranked selection with
   continent quotas, simplification, country location) can serve other named features. **The river
   output must not change by a single byte.**
2. **Add lakes** with it: `processed/lakes/*.json`, about 50 famous lakes balanced across
   continents.
3. **Add one game type, `LAKES_BY_SHAPE`** ("Lake Shapes"): the lake's polygon on a blank map, with
   a scale bar. `answerCount` 1, `hintCount` 3, `guessLimit` 5 (the same as every other type).
   When the round ends (solved, given up or out of guesses) it shows the satellite map.

Islands, seas and peaks could reuse the same pipeline later. They're out of scope.

## Facts established while planning (don't re-derive)

- The river pipeline lives in `loaders/rivers/`. Not river-specific: `NaturalEarthReader` (except
  that it only reads lines and skips `Canal`), `RiverWikidataMain` (its SPARQL query asks nothing
  river-specific), `WikidataEntry`, `ContinentResolver`, `DouglasPeucker`, `RiverSelector` (except
  its record type), `RiverGeometry.haversineKm` / `lineKm`, `RiverCountryLocator` (it samples lines,
  and a lake shoreline is a closed line), and `AliasBuilder` (except `riverVariant`). River-specific:
  `RiverAssembler`, `RiverCandidate`, `RiverOverride`, `RiverEnds`, `RiverFacts`, `RiverWriter`
  and `RiverLoaderMain`.
- **Country polygons include their lakes.** Measured: Uganda has no edge that only it uses, so
  Lake Victoria lies inside Uganda / Kenya / Tanzania rather than being a gap. So a lake's shoreline
  can be located in countries the same way river lines are. The Caspian is the exception: it's a gap
  between countries, so its shoreline lands on the countries' coast edges and may locate
  unreliably. The summary in 2.4 lists lakes with no countries, and an override fixes them.
- The Natural Earth lakes file hasn't been inspected yet. Step 2.1 is a checkpoint.

## Delivery

`main` is protected. Use one branch and PR per phase, in order, each green on `mvn clean install`
before you open it. Run `mvn spotless:apply` and `npm run lint` (in `client/`) before each commit.

---

## Phase 1: generalise the pipeline (no output change)

Create the package `com.parkinson.benjamin.geogames.loaders.features` and move the shared code into
it. Keep `loaders/rivers` for what is only about rivers.

### 1.1 Moves and renames

| From (`loaders/rivers`)        | To (`loaders/features`)    | Change                                                                 |
|--------------------------------|----------------------------|------------------------------------------------------------------------|
| `NaturalEarthReader`           | `NaturalEarthReader`       | See 1.2.                                                               |
| `NaturalEarthRiver`            | `NaturalEarthFeature`      | See 1.2.                                                               |
| `RiverWikidataMain`            | `WikidataMain`             | Usage unchanged. The User-Agent becomes `geogames-data-loader/0.1 (https://github.com/benparkinson/geogames)`. Its Javadoc says it serves any Natural Earth file with `wikidataid`. |
| `WikidataEntry`                | `WikidataEntry`            | None.                                                                  |
| `ContinentResolver`            | `ContinentResolver`        | None.                                                                  |
| `DouglasPeucker`               | `DouglasPeucker`           | None.                                                                  |
| `RiverGeometry.haversineKm`, `lineKm`, the `EARTH_RADIUS_KM` constant | `Geo` (new, final, static) | Public. `RiverGeometry` calls them.                     |
| `RiverCountryLocator`          | `CountryLocator`           | Add `locateRings` (1.4).                                               |
| `RiverSelector`                | `FeatureSelector`          | Generic (1.3). `CONTINENT_QUOTAS` moves to `RiverLoaderMain` as `RIVER_QUOTAS`. |
| `AliasBuilder`                 | `AliasBuilder`             | Takes the variants function and an override interface (1.5).          |
| `RiverWriter.slug`             | `Slugs.slug(name, fallback)` | The river fallback stays `"river"`.                                  |

Move the matching tests with them (`ContinentResolverTest`, `DouglasPeuckerTest`, `AliasBuilderTest`,
`RiverSelectionTest` → `FeatureSelectorTest`, `RiverCountryLocatorTest` → `CountryLocatorTest`).
If the country-shape plan (`docs/plans/country-shape-games.md`) has already landed, update its
`DouglasPeucker` / `haversineKm` imports.

### 1.2 `NaturalEarthReader` reads any geometry

```java
/** All Natural Earth features that share one Wikidata ID. Points are {lon, lat}. */
public record NaturalEarthFeature(
    String qid, String name, String nameEn,
    List<List<double[]>> lines,              // from LineString / MultiLineString
    List<List<List<double[]>>> polygons) {}  // from Polygon / MultiPolygon: each is a list of rings
```

`read(Path file, Set<String> skippedClasses)` and `group(JsonNode, Set<String> skippedClasses)`.
Rivers pass `Set.of("Canal")`. The grouping, the name merging and the order of the result are
exactly as now, so the river output can't change. Add a polygon case to the reader's tests: write
one if none exists, using an inline JSON string.

### 1.3 `FeatureSelector`

```java
public interface RankedFeature { String qid(); String continent(); int sitelinks(); }
public static <T extends RankedFeature> Selection<T> select(List<T> candidates, Map<String, Integer> quotas)
```

`RiverCandidate implements RankedFeature`; its record accessors already match. The ranking, the
tie-breaks and the "unresolved" cutoff are unchanged.

### 1.4 `CountryLocator.locateRings`

`Result locateRings(List<List<List<double[]>>> polygons)` locates the **outer** ring of each polygon,
treated as a closed line (`locate` on those rings). Test it with a square lake on the border of two
square countries. Make `MIN_KM` a constructor argument (rivers keep 10).

### 1.5 `AliasBuilder` without the river rule

```java
public interface NamedOverride {
  Boolean exclude(); String name(); String continent();
  List<String> aliases(); List<String> addAliases(); List<String> removeAliases();
  default boolean isExcluded() { return Boolean.TRUE.equals(exclude()); }
}
public static List<String> build(String name, List<String> wikidataAliases, String neName,
    String neNameEn, NamedOverride override, Function<String, List<String>> variants)
```

`RiverOverride implements NamedOverride`; remove its own `isExcluded`. The river variants function
is today's `riverVariant` wrapped in `List.of(...)`, and it lives in `RiverAssembler`. Keep the
order in which candidates are added exactly as now.

### 1.6 Prove nothing changed

- Run `RiverLoaderMain` with the same arguments as in `CLAUDE.md` / `docs/plans/river-game-variants.md`
  1.6. Then `git status --porcelain src/main/resources/processed/rivers` must print nothing.
- Don't re-run the Wikidata fetch for rivers.
- `CLAUDE.md`: update the class names in the pipeline section: `WikidataMain`, and
  `RIVER_QUOTAS` in `RiverLoaderMain` instead of `RiverSelector.CONTINENT_QUOTAS`. Add one sentence
  saying `loaders/features` is shared by every Natural Earth + Wikidata feature type.

---

## Phase 2: lake data

### 2.1 Check the Natural Earth data (checkpoint)

Download `ne_10m_lakes.geojson` from the `geojson/` folder of the `nvkelso/natural-earth-vector`
GitHub repo into `geodata/lakes/naturalearth/` (a new folder). Don't use the `_europe`,
`_north_america`, `_historic` or `_pluvial` variants. With a throwaway script in the scratchpad (not
committed), print:
- the feature count, the property keys, and the geometry types;
- how many features have a non-empty `wikidataid`, and how many distinct values there are;
- the `featurecla` values with their counts;
- the properties of these lakes, if present: Baikal, Victoria, Superior, Titicaca, Geneva, Caspian,
  Aral, Ladoga, Tanganyika, Garda.

Expected (unconfirmed): `name`, `name_en`, `featurecla` (Lake, Reservoir, Alkaline Lake),
`scalerank` and `wikidataid`. **If `wikidataid` is missing or mostly empty, stop and report back.**
Put what you found in the PR description, along with whether the Caspian and Aral are in the
file and how they're drawn.

### 2.2 Wikidata facts

Run `WikidataMain geodata/lakes/naturalearth/ne_10m_lakes.geojson geodata/lakes/wikidata.json`
(this is the network step) and commit the output. `NaturalEarthReader` now reads polygons, so
the QIDs are collected the same way as for rivers.

### 2.3 `loaders/lakes/`

- `LakeOverride(exclude, name, continent, aliases, addAliases, removeAliases, List<String> countries)`
  `implements NamedOverride`. `countries` replaces the located countries outright. Start
  `geodata/lakes/overrides.json` as `{}`.
- `LakeCandidate(qid, name, aliases, continent, sitelinks, fromOverride, polygons, areaKm2)`
  `implements RankedFeature`.
- `LakeGeometry` (pure, unit tested):
  - `areaKm2(polygons)`: the shoelace formula on points projected to km
    (`x = lon · 111.32 · cos(lat₀)`, `y = lat · 110.57`, where `lat₀` is the ring's mean latitude).
    Subtract the holes.
  - `simplify(polygons)`: Douglas-Peucker on every ring, starting at tolerance 0.002° and doubling
    up to 0.08° until the total is ≤ `MAX_POINTS = 800`. Then:
    - drop the holes (islands) with an area under 1% of their outer ring;
    - drop the polygons with an area under 2% of the largest one, which keeps the main body of
      the lake and drops stray fragments;
    - close each ring (first point = last), and drop a ring with fewer than 4 points; drop an outer
      ring's polygon entirely if that leaves none;
    - round to 4 decimals.
  - Test areas against squares of a known size, the hole and fragment dropping, the cap holding on a
    many-point circle, and closed output rings.
- `LakeAssembler`, the counterpart of `RiverAssembler`, reading no files:
  - the same rules for including a QID (overrides, missing Wikidata);
  - geometry from `NaturalEarthFeature.polygons`, simplified, with "no geometry" as a drop reason;
  - `MIN_AREA_KM2 = 100` unless the lake comes from an override (Loch Ness and Lake Bled are
    smaller; force them in only if they turn out to be wanted);
  - the name from the override, then the Wikidata label, then `name_en`, then `name`;
  - the continent through `ContinentResolver`;
  - aliases through `AliasBuilder` with the lake variants function (below);
  - featurecla isn't filtered: famous reservoirs (Volta, Kariba, Nasser) are fair game, and
    sitelinks decide.
- **Lake variants**:
  - `"Lake X"` gives `"X"`;
  - `"X Lake"` gives `"X"`;
  - any other name gives `"Lake " + name`.

  Skip a variant that starts with a lower-case letter, so "Lake of the Woods" doesn't make
  "of the Woods". Names ending in "Sea" (Aral, Dead) get no variant. Unit test each case.
- `model/Lake` (runtime and file format):
  ```java
  public record Lake(String id, String name, List<String> additionalNames, String continent,
      GeoData geoData, List<String> countries) {}
  ```
  `geoData` is a `Feature` with a `Polygon` (one polygon) or a `MultiPolygon`. `countries` holds
  the names exactly as `Country.name()`, longest shoreline first.
- `LakeWriter`: clears `*.json` in `outDir`, then writes `<slug>-<QID>.json` (the slug fallback is
  `"lake"`).
- `LakeLoaderMain <naturalEarth.geojson> <wikidata.json> <overrides.json> <countriesDir> <outDir>`.
  It's offline and deterministic. `LAKE_QUOTAS`: Europe 12, Asia 10, Africa 8, North America 10,
  South America 5, Australia 3.
  1. Assemble, select with `FeatureSelector`, and sort by name, then QID.
  2. Locate countries with `CountryLocator.locateRings`, with `MIN_KM = 5`, since lakes are
     smaller than rivers.
  3. Write, then print a summary like the river one: written per continent, the drop reasons,
     beyond quota, the unresolved famous lakes, the lakes with no countries, the total and largest
     bytes, and one line per lake with its name, QID, continent, sitelinks, km², points, countries
     and aliases.

### 2.4 Generate and review

- Run `LakeLoaderMain`. **Read the printed list** and fix it with overrides until it looks right:
  - Exclude the lakes that are just blobs or that nobody would know.
  - Give the lakes with an unresolved continent a `continent`. The Caspian will probably be one
    (Europe/Asia). Decide whether to include it as Asia, and say which in the PR.
  - Give the lakes with no countries their `countries`.
  - Remove bad aliases.
  - Adjust `LAKE_QUOTAS` if a continent hasn't got enough good lakes (South America and Australia
    may not). Put the final list in the PR description.
- `ProcessedDataIntegrityTest`:
  - lake ids are unique;
  - every lake has a continent and at least one country, and every country loads through
    `CountryLoaderService.loadCountryByName`;
  - every ring is closed with ≥4 points, and no lake has more than 800 points;
  - there are at least 20 lakes.
- `CLAUDE.md`: a `geodata/lakes/` bullet (its sources and the override fields) and a
  `LakeLoaderMain` bullet. Also update the credit next to the existing Natural Earth river credit
  (find it with Grep `naturalearthdata`) to cover lakes.

---

## Phase 3: `LAKES_BY_SHAPE`

### 3.1 Backend

- `GameType.LAKES_BY_SHAPE(1, 3, 5)`, plus `GameTypeTest` (its per-type tables of hint counts and
  guess limits) and `GameSelectionTest`.
- `model/LakeRound(GameType gameType, Lake lake) implements GameData`, with
  `subjectId() = "lake:" + lake.id()`.
- `service/LakeLoaderService`: `loadLakes()` with `@Cacheable("lakes")`, reading
  `classpath*:/processed/lakes/*.json`, like `RiverLoaderService`.
- `service/LakeRoundGenerator`, a `@Service implements RoundGenerator`: filter out the excluded
  subjects, then `RandomPicker.pickDistinct(..., "lakes")`. If lake variants are ever added, turn
  it into a parameterised class with `@Bean`s, as the rivers did.
- Tests: `LakeLoaderServiceTest`, `LakeRoundGeneratorTest` (exclusion, too few →
  `IllegalArgumentException`), and `GameApiIntegrationTest.createsALakeShapesGame`.

### 3.2 Frontend (`client/src/games/lakes/`)

- **Shared hint helpers.** If `client/src/games/common/hints.ts` doesn't exist yet, do step 0.2 of
  `docs/plans/country-shape-games.md` first (`firstLetterHint`, `listHint`, with the river hints
  rebuilt on top of them).
- `model.ts`: `Lake` (mirroring the Java record; `geoData` is a `Polygon` / `MultiPolygon`
  feature, so reuse or generalise `CountryGeoData` from `common/country.ts`) and
  `LakeRound { lake: Lake }`.
- `lakeHints.ts`, unit tested:
  1. `Continent: Africa`;
  2. `listHint("Shores in", lake.countries)` (this renders as "Shores in: Kenya, Tanzania, Uganda";
     use "Lies in" when there is only one country);
  3. `firstLetterHint(lake.name)`.
- `LakeShapesMap.tsx` + `LakeShapesMapWrapper.tsx` (the usual `dynamic(() => import(...))`
  wrapper):
  - `MapContainer zoom={3} minZoom={2} maxZoom={12} center={[0, 0]}`;
  - `RevealTiles` when `gameOver` (from `map/RevealTiles.tsx` if the country plan moved it, otherwise
    from `rivers/RiverLayers.tsx`);
  - the lake as a `GeoJSON` with `answerPathOptions(gaveUp)` plus `fillOpacity: 0.35`, with the
    `key` trick from `RiverLine` so the colour changes on give-up. `gaveUp` is also true when the
    round ran out of guesses, so that case needs no code of its own;
  - `FitBounds data={lake.geoData}`;
  - react-leaflet's `<ScaleControl position="bottomleft" imperial={false} />`, always shown: size
    is a fair clue, and fitting the bounds would otherwise hide it;
  - `MapResizer`.

  Raise `FitBounds`' `maxZoom` (7 today) for small lakes. Add an optional `maxZoom` prop and pass
  11. The existing callers keep 7.
- `LakeGame.tsx`: `MapGame` with `blankMap`,
  `mapCaption="Shape only. The scale bar shows its size."`,
  `prompt="Name this lake from its shape."`, `inputPlaceholder="Lake name…"`, `answerNoun="lake"`,
  `answers={[data.lake]}`, and the hints.
- **Autocomplete and spell checking**, as rivers have (`docs/plans/river-lake-autocomplete.md`):
  - Backend: `LakeLoaderService.loadLakeNames()` with `@Cacheable("lakeNames")`, returning
    `PlaceName`s sorted by name, and `GET /api/lakes` on `PlaceNameController` with the same
    `Cache-Control` as `/api/rivers`.
  - Frontend: `LakeGame` passes the `suggestions` and `isKnownGuess` from `useGuessList("lakes")`.
    A misspelling ("Bakal") is then "unknown" and free.
  - Tests: as for rivers, plus a lake-name uniqueness check in `ProcessedDataIntegrityTest`.
  - If the autocomplete plan hasn't landed yet, do its B1, F1 and F2 first (the `PlaceName` rename,
    `usePlaceNames` and `useGuessList`). They're small.
- `index.ts` module: type `LAKES_BY_SHAPE`, title "Lake Shapes", singular "Lake shape", description
  "Name a lake from its shape alone.", icon `DropletFill` (react-bootstrap-icons), and
  `answerNames: (round) => [round.lake.name]`. Add it to `GAME_MODULES` and update
  `registry.test.ts`.
- `test/fixtures.ts`: `makeLakeData()` (a square polygon), `makeLakeRoundData()` and
  `makeLakeRound()` (`gameType: { name: "LAKES_BY_SHAPE", answerCount: 1, hintCount: 3,
  guessLimit: 5 }`, following how the existing river fixtures fill in `gameType`).
- `LakeGame.test.tsx` (mock `./LakeShapesMapWrapper`): the prompt and caption, all 3 hints in order
  and non-null for every index below the fixture's `hintCount`, the "Lies in" wording for one
  country, a correct guess by an alias and by the "Baikal"-style variant, and an unknown name not
  lowering the guesses-left count. Add a `/api/lakes` `fetch` route with a `lakeNames` fixture.

### 3.3 Wrap-up

- `CLAUDE.md`: list `LAKES_BY_SHAPE` in the API game types and under "Game types", and add the
  `lake:` subject id.
- Run `npm run dev` with the backend. Play a few rounds, including a small lake (check the zoom
  and the scale bar), a lake with islands, a reservoir, and one round played until it runs out of
  guesses (the reveal must look the same as giving up). Check the home page card at phone width.

## Out of scope (mention in the PR description, don't build)

- Lake variants on the river model (on satellite imagery, by the countries around it).
- Islands, seas and peaks on the shared pipeline.
- Mix weights. `RANDOM` picks types uniformly, and families of games make that skewed; see
  `docs/plans/river-game-variants.md` Phase 6.
