# Plan: import many more rivers

Goal: grow the Rivers game from 20 hand-imported rivers to about 150. The rivers come from a
repeatable offline loader, replacing manual Overpass Turbo exports and `processRiverImport.sh`.
Lower-resolution geometry is fine and intended: the map never zooms past 7.

Read `CLAUDE.md` first. Follow the existing offline-loader pattern (`loaders/countries/`).
There may be uncommitted country-data work in the tree. Leave it alone.

## Background (checked against the repo on 2026-11-07)

- **Raw data in the jar.** `src/main/resources/data/rivers/` holds 20 `*.geo.json` files (about
  23 MB in total, 1.5k–47k points each). Each one is a single GeoJSON `Feature` whose
  `MultiLineString` has between 2 and 82 lines, with `properties.name`. The folder also holds
  `additionalData.json` (name → `{names, continent}`) and `processRiverImport.sh`.
- **Runtime loading.** `service/RiverLoaderService.loadRivers()` (`@Cacheable("rivers")`) reads the
  files through `service/JsonResourceReader` (`read`, `readAll(pattern, Class)`). It joins them to
  `additionalData.json` by name, and throws `IllegalStateException` when a river has no entry.
  `service/RiverFinderService` is the `RoundGenerator` and needs no change.
- **The record.** `model/River` is
  `record River(String name, List<String> additionalNames, String continent, GeoData geoData) implements GameData`.
  The whole record goes into each round's `jsonBlob`. `GeoGeometry` accepts `MultiLineString`.
- **JSON settings.** Offline loaders use `helper/GeoJson.prettyWriter()` and
  `GeoJson.lenientReader()`. `loaders/countries/CountryWriter.clearOutput(Path)` shows the
  "delete `*.json` in outDir first" pattern.
- **Frontend.** The river game is `client/src/games/rivers/`: `model.ts` (the `River` interface),
  `RiverShapesGame.tsx` and `RiverShapesMap.tsx` (`maxZoom={7}`, which keys the GeoJSON and
  `FitBounds` by `river.name`). The test fixture `makeRiverData()` is in `client/src/test/fixtures.ts`.
- **Answer matching** is `normaliseString` in `client/src/helper/stringHelper.ts`: NFD, strip
  combining marks, lowercase, then drop everything except `[a-z0-9]`. So "Rhône" already matches
  "rhone", but "Murray" doesn't match "Murray River", and **an alias in a non-Latin script
  normalises to `""`**. Such an alias must never be written.
- **Continents.** The only allowed strings are `Africa`, `Asia`, `Europe`, `North America`,
  `South America` and `Australia`. The hint shows them.
- **Credits.** `client/src/components/HomePage.tsx` already has an OSM attribution footer.
- **Tests that build a `River`:** `service/RiverLoaderServiceTest`, `service/RiverFinderServiceTest`,
  `service/GameServiceTest` (`river(int)`) and `loaders/tripoint/TripointBuilderTest` (line ~44).
  `GameApiIntegrationTest.createsARiverGame` checks `name`, `continent` and `geoData` in the blob.

## Design in one paragraph

Natural Earth supplies the geometry and a Wikidata ID for every river. A small **network** program
fetches the Wikidata facts once and writes them to a **committed** `geodata/rivers/wikidata.json`.
A separate **offline** program then combines Natural Earth, that file, a hand-written
`overrides.json` and the 20 existing OSM files. It simplifies the geometry and writes one JSON file
per river to `src/main/resources/processed/rivers/`. At runtime the app just reads those files as
`River` records.

## Steps

### 0. Check the Natural Earth data (checkpoint)

Download `ne_10m_rivers_lake_centerlines.geojson` from the `geojson/` folder of the
`nvkelso/natural-earth-vector` GitHub repo into `geodata/rivers/naturalearth/` (new folder). With
a throwaway script in the scratchpad (not committed), print:
- the feature count and the property keys;
- the geometry types seen;
- how many features have a non-empty `wikidataid`, and how many distinct values there are;
- five sample features' properties.

The expected properties are `name`, `name_en`, `name_alt`, `scalerank`, `featurecla` and
`wikidataid`. These are unconfirmed. If the names differ, adapt the steps below. **If
`wikidataid` is missing or mostly empty, stop and report back.**

Do not use the Europe / North America supplement files. That's out of scope.

### 1. Raw input layout (`geodata/rivers/`, not in the jar)

```
geodata/rivers/
  naturalearth/ne_10m_rivers_lake_centerlines.geojson   # step 0
  osm/                     # git mv the 20 *.geo.json files here, unchanged
  wikidata.json            # written by step 2, committed
  overrides.json           # hand-written
```

`overrides.json` is keyed by Wikidata QID. The QIDs below are placeholders.

```json
{
  "Q0000001": {
    "name": "Murray River",
    "aliases": ["Murray"],
    "continent": "Australia",
    "osmFile": "murray.geo.json"
  },
  "Q0000002": { "exclude": true },
  "Q0000003": { "continent": "Asia" },
  "Q0000004": { "addAliases": ["Huang He"], "removeAliases": ["Mother River"] }
}
```

Fields (all optional):
- `exclude: true` drops the river.
- `name` replaces the derived name.
- `continent` replaces the derived continent.
- `aliases` replaces the derived alias list entirely.
- `addAliases` and `removeAliases` adjust the derived alias list. Matching ignores case and accents.
- `osmFile` takes the geometry from `geodata/rivers/osm/<file>` instead of Natural Earth.

**Every override entry that isn't `exclude` is always included,** whatever its fame or length. An
entry with `osmFile`, `name`, `continent` and `aliases` needs nothing from Natural Earth or
Wikidata.

Seed it with one complete entry per existing river. Copy `name`, `aliases` (from `names`) and
`continent` from `additionalData.json`, and set `osmFile` to its file. Find each river's QID in
`wikidata.json` (step 2) by matching the label or the Natural Earth name. If a QID can't be found
there, look it up on wikidata.org by hand. The 20 rivers must keep their current display names
and aliases.

### 2. `loaders/rivers/RiverWikidataMain` (network, run once)

Usage: `RiverWikidataMain <naturalEarth.geojson> <outFile wikidata.json>`

- Collect the distinct non-empty `wikidataid` values from the Natural Earth file.
- Query `https://query.wikidata.org/sparql` with `java.net.http.HttpClient`. Use POST with the
  form field `query`, `Accept: application/sparql-results+json`, and
  `User-Agent: geogames-river-loader/0.1 (https://github.com/benparkinson/geogames)` (no email).
  Send at most 200 QIDs per request in a `VALUES` block, wait 1 s between requests, and retry
  429/5xx up to 3 times with backoff. Suggested query (check it on query.wikidata.org first):

  ```sparql
  SELECT ?item ?label ?sitelinks
         (GROUP_CONCAT(DISTINCT ?alt; separator="|") AS ?aliases)
         (GROUP_CONCAT(DISTINCT ?contLabel; separator="|") AS ?continents)
         (GROUP_CONCAT(DISTINCT ?countryContLabel; separator="|") AS ?countryContinents)
  WHERE {
    VALUES ?item { wd:Q1 wd:Q2 }
    OPTIONAL { ?item rdfs:label ?label FILTER(LANG(?label) = "en") }
    OPTIONAL { ?item wikibase:sitelinks ?sitelinks }
    OPTIONAL { ?item skos:altLabel ?alt FILTER(LANG(?alt) = "en") }
    OPTIONAL { ?item wdt:P30 ?cont . ?cont rdfs:label ?contLabel FILTER(LANG(?contLabel) = "en") }
    OPTIONAL { ?item wdt:P17/wdt:P30 ?cc . ?cc rdfs:label ?countryContLabel FILTER(LANG(?countryContLabel) = "en") }
  }
  GROUP BY ?item ?label ?sitelinks
  ```

- Write `wikidata.json` sorted by QID with `GeoJson.prettyWriter()`, as
  `{ "Q1653": { "label": "Danube", "sitelinks": 180, "aliases": [...], "continents": [...], "countryContinents": [...] } }`.
  Use empty lists, not `""`, when a value is missing.
- Keep the HTTP code in this class only. The offline loader and every test read `wikidata.json`
  and never touch the network.

### 3. `loaders/rivers/RiverLoaderMain` (offline, deterministic)

Usage: `RiverLoaderMain <naturalEarth.geojson> <wikidata.json> <overrides.json> <osmDir> <outDir> [maxRivers=150]`

Keep the logic in small, plain classes that can be tested without files where possible.

**a. Group Natural Earth features** by `wikidataid`, skipping features that have none. Collect
every `LineString` / `MultiLineString` part into one list of lines. Keep the first non-empty
`name` and `name_en`. Include `Lake Centerline` features: they keep a river continuous through
lakes.

**b. Candidate set:** every group that has a `wikidata.json` entry, plus every non-excluded
override QID. Drop excluded QIDs.

**c. Geometry:** `osmFile` if overridden (read as `GeoData`, take its lines), otherwise the
Natural Earth lines.

**d. Simplify (`DouglasPeucker`, hand-written; don't add JTS or any other dependency).**
- Run per line, in degree space, starting at tolerance 0.005.
- While the river has more than 600 points in total, double the tolerance (stop at 0.16).
- Then drop lines shorter than 5 km, but always keep the longest line.
- Round coordinates to 4 decimal places.

Don't try to join line pieces together. Each line keeps at least 2 points, and the legacy files
have at most 82 lines.

**e. Length in km:** haversine over the simplified lines. Drop non-override rivers shorter than
200 km.

**f. Name:** override `name`, then the Wikidata label, then Natural Earth `name_en`, then `name`.

**g. Aliases:** if the override has `aliases`, use exactly those. Otherwise:
1. Start from the Wikidata aliases plus the Natural Earth `name` and `name_en`.
2. Add the name with a trailing " River" removed, or with " River" appended if it doesn't contain
   "River".
3. Apply `addAliases` / `removeAliases`.
4. Filter. Use a Java copy of `normaliseString`:
   `Normalizer.normalize(s, NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "")`.
   - Drop any alias that normalises to `""` (non-Latin scripts, for example).
   - Drop any alias that is longer than 40 characters or contains a digit.
   - Drop any alias that normalises to the same thing as the name or as an earlier alias.

**h. Continent:** map each continent label to the allowed strings:
- `Oceania`, `Insular Oceania`, `Australia` and `Australian continent` → `Australia`;
- the other five pass through unchanged;
- anything else (Antarctica, Eurasia, …) is ignored.

Then pick, in order: the override `continent`; the Wikidata `continents` if exactly one distinct
value remains; the `countryContinents` if exactly one distinct value remains. Otherwise the
continent is unresolved.

**i. Select:**
1. Sort the non-override candidates by sitelinks, descending, then by QID.
2. Skip unresolved-continent rivers, but record them.
3. Take rivers until `maxRivers` minus the number of override rivers is reached, then add all the
   override rivers.

**j. Write:**
- Clear `*.json` in `outDir` first, like `CountryWriter.clearOutput`.
- Write one file per river with `GeoJson.prettyWriter()`, named `<slug>-<QID>.json`. The slug is the
  name lowercased, with each run of non-`[a-z0-9]` characters (after accent stripping) replaced by
  `-`. For example `rio-grande-Q2018.json` (a placeholder QID). The content is the `River` record:

  ```json
  {
    "id": "Q2018",
    "name": "Rio Grande",
    "additionalNames": ["Rio Bravo", "Río Bravo del Norte"],
    "continent": "North America",
    "geoData": {
      "type": "Feature",
      "properties": { "name": "Rio Grande" },
      "geometry": { "type": "MultiLineString", "coordinates": [[[-106.1234, 37.8123], [-106.0, 37.7]]] }
    }
  }
  ```

  If `gameType` shows up in the output, exclude it. It isn't a record component.

**k. Print a summary:**
- the counts written and dropped, by reason;
- the total bytes and the largest file;
- the skipped rivers with an unresolved continent that would have been selected, with name, QID
  and sitelinks, so overrides can be added for them;
- one line per written river: name, QID, continent, sitelinks, km, points and aliases.

### 4. Runtime and frontend changes

- `model/River`: add `String id` as the **first** component, then update the four test call sites
  listed above.
- `service/RiverLoaderService.loadRivers()`: return
  `reader.readAll("classpath*:/processed/rivers/*.json", River.class)`. Keep `@Cacheable("rivers")`.
  Remove `RiverMetadata` and the `additionalData.json` lookup.
- Delete `src/main/resources/data/rivers/` (`additionalData.json` and `processRiverImport.sh`; the
  `.geo.json` files were moved in step 1).
- `client/src/games/rivers/model.ts`: add `id: string` to `River`.
- `client/src/games/rivers/RiverShapesMap.tsx`: use `river.id` instead of `river.name` in the
  GeoJSON `key` and the `FitBounds` `id`. Names may now repeat, for example "Colorado River".
- `client/src/test/fixtures.ts`: add `id` to `makeRiverData()`.
- `client/src/components/HomePage.tsx` footer: after the OSM credit, add
  ` · River lines: <a href="https://www.naturalearthdata.com/">Natural Earth</a>`.

### 5. Tests (JUnit 5 + AssertJ, no network)

- `DouglasPeuckerTest`: a straight line collapses to its 2 endpoints; a zig-zag larger than the
  tolerance is kept; the endpoints are always kept.
- Alias test: "Murray River" gives "Murray"; a Cyrillic alias is dropped; a duplicate by
  normalisation is dropped; the name is never in its own aliases; `aliases` in an override replaces
  everything.
- Continent test: one direct value wins; the country fallback works; "Oceania" maps to `Australia`;
  `Europe` + `Asia` is unresolved; an override wins over all of these.
- Grouping/selection test, with small in-memory inputs: two features with the same QID become one
  river; a feature with no QID is skipped; the top N is taken by sitelinks; override rivers are
  always included and excluded ones never are.
- Rewrite `service/RiverLoaderServiceTest`:
  - **Real data test:** at least 100 rivers; ids unique; names and continents non-blank; continent
    in the six allowed strings; no blank alias, and no alias that normalises to `""`; geometry
    `MultiLineString`, non-empty, every line ≥ 2 points, total ≤ 600 points; all 20 original
    river names present.
  - **Stub test:** with a mocked `JsonResourceReader`, `loadRivers()` returns what `readAll` gives.
- Update `RiverFinderServiceTest`, `GameServiceTest` and `TripointBuilderTest` for the new `id`
  argument only.

### 6. Run and curate

```bash
mvn -q compile -Dskip.npm -Dskip.installnodenpm
mvn -q dependency:build-classpath -Dmdep.outputFile=<scratch>/cp.txt
CP="target/classes;$(cat <scratch>/cp.txt)"   # ':' instead of ';' off Windows
java -cp "$CP" com.parkinson.benjamin.geogames.loaders.rivers.RiverWikidataMain \
  geodata/rivers/naturalearth/ne_10m_rivers_lake_centerlines.geojson geodata/rivers/wikidata.json
# now write the 20 seed entries in overrides.json (step 1)
java -cp "$CP" com.parkinson.benjamin.geogames.loaders.rivers.RiverLoaderMain \
  geodata/rivers/naturalearth/ne_10m_rivers_lake_centerlines.geojson geodata/rivers/wikidata.json \
  geodata/rivers/overrides.json geodata/rivers/osm src/main/resources/processed/rivers 150
```

Read the summary. Fix clear problems with `overrides.json` and rerun the loader. Don't refetch
Wikidata for this. Problems to look for:
- a missing continent for a well-known river;
- an odd display name;
- aliases that are wrong or too loose (for example a bare "River" or another river's name);
- a duplicate of one of the 20 legacy rivers (the same QID means it's already merged; a different
  QID means exclude one).

Don't spend long hand-curating. List anything you're unsure about in the final report instead.

Targets: `processed/rivers/` under 8 MB in total, and no single file over 60 KB.

### 7. Docs

In `CLAUDE.md` "Geo data pipeline":
- Replace the `data/rivers` / `processRiverImport.sh` sentences with a `geodata/rivers/`
  description: Natural Earth, the `osm/` legacy files, `wikidata.json` and `overrides.json`.
- Add both loader usages, noting that only `RiverWikidataMain` uses the network, and that editing
  `overrides.json` means rerunning `RiverLoaderMain`.
- Change "Rivers are loaded at runtime by classpath glob" to say they're read from
  `processed/rivers/*.json`.

## Verification (all must pass before reporting done)

```bash
mvn spotless:apply
mvn clean install
```

That runs the Java and Vitest suites and builds the jar. Then run
`java -jar target/geogames-0.0.1-SNAPSHOT.jar` and play a Rivers game and a Random game at
`http://localhost:8080`. The shapes should be recognisable and the continent hint should work.
Don't commit unless asked.

## Out of scope (mention in the final report, don't do)

- Fetching geometry from Overpass/OSM programmatically.
- Natural Earth supplement files, HydroRIVERS and GRDC.
- Difficulty levels based on sitelinks.
- Any change to the API, the game rules or the tripoint/country data.
