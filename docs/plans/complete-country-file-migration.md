# Plan: finish migrating to per-country files and align tripoint markers with borders

Work on a new branch, `complete-country-migration`. Do not commit unless the user asks. Run `mvn spotless:apply` before finishing. Read `CLAUDE.md` first.

## Where things stand (checked against the repo on 2026-11-06)

- `src/main/resources/data/countries/countries.geo.json` has 255 Natural Earth features (24 MB).
- `src/main/resources/processed/countries/*.json` has 255 files. Each is a `Country` record (`name`, `additionalNames`, `geoData`), written by `CountryLoaderMain`. **Every geometry is byte-for-byte the same as in `countries.geo.json`.** The per-country files are a split of that file, not a higher-resolution version.
- `src/main/resources/processed/tripoints/{1..170}.json` each hold a coordinate plus **full copies** of the three countries' geometry (64 MB in total, 135 distinct countries). That geometry is also identical to the per-country files.
- The migration is half done:
  - `CountryLoaderService.loadCountryByName` exists but nothing calls it. It is also broken: it deserialises the file as `GeoData`, but the file is a `Country`, so `properties` comes back null.
  - `GameService` injects `CountryLoaderService` but never uses it.
  - `TripointLoaderService.countTripoints()` hard-codes 170.
- **Why the markers miss the borders:** tripoint coordinates are scraped from Wikipedia (`TripointComputer`), mostly rounded to the arc-minute, and are never reconciled with the Natural Earth geometry. Measured against the point where the three countries' polygons actually meet: median offset 2.0 km, 152 of 170 more than 1 km off, 17 more than 5 km off. At the map's `maxZoom` of 10 that is easy to see. Switching geometry files alone would not fix this, because the geometry is the same everywhere.
- Commit `dc32a7c` ("Omit dodgy tripoints") curated the 170 files by hand. **Do not re-scrape Wikipedia to regenerate them.** Convert the existing files.

## Target end state

- Tripoints are stored once, as a single small file `src/main/resources/processed/tripoints.json`: an array of `{ "coordinate": {latitude, longitude}, "countryNames": [a, b, c] }`.
- Each coordinate has been snapped to the vertex where the three countries' borders meet in the Natural Earth data. Tripoints with no such vertex nearby are removed. That is six of the current 170, which leaves 164.
- At game creation the server loads the countries by name from `processed/countries/` and builds the same `Tripoint` JSON as today. **The `jsonBlob` shape the frontend receives does not change, so the frontend needs no edits.**
- The tripoint count comes from the file. The hard-coded 170 is gone.
- The raw `countries.geo.json` and `additionalNames.json` are no longer on the runtime classpath. They live outside `src/main/resources` and are read only by the offline loader programs.

## Steps

### 1. Fix runtime country loading

In `service/CountryLoaderService.java`:
- Change `loadCountryByName` to `loadFile("processed/countries/" + fileName, Country.class)` and return it directly. The processed files already contain `additionalNames`, so it does not need to read `additionalNames.json`.
- Annotate it `@Cacheable("country")`. Callers are other beans, so the Spring proxy applies.
- Move `loadCountries()` (which reads the raw file) out of this service. See step 4.

Remove the unused `CountryLoaderService` field and constructor parameter from `GameService`.

### 2. New tripoint model and runtime loading

- Add `model/TripointDefinition.java`: `public record TripointDefinition(Coordinate coordinate, List<String> countryNames) {}`.
- Rewrite `service/TripointLoaderService`:
  - `@Cacheable("tripointDefinitions") List<TripointDefinition> loadTripointDefinitions()` reads `processed/tripoints.json` as `TripointDefinition[]`.
  - Delete `countTripoints()` and `loadTripointByIndex()`.
- Rewrite `service/TripointFinderService`:
  - Inject `TripointLoaderService` and `CountryLoaderService`.
  - `List<GameData> findRandomTripoints(int howMany)`: load the definitions and throw `IllegalArgumentException` (same message style as today) if `howMany` is greater than the count. Pick `howMany` **distinct indices**, for example by shuffling the index list with the existing `Random` and taking the first `howMany`. For each pick, build `new Tripoint(def.coordinate(), countries)`, where `countries` is a `Set<Country>` loaded via `loadCountryByName` for each name.
  - Do not deduplicate by putting `Tripoint`s in a `HashSet`, which is what happens today. That hashes entire geometries.
- In `GameService.getXRandomTripoints`, call `tripointFinderService.findRandomTripoints(numberOfRounds)`.
- Keep `model/Tripoint.java` unchanged, because it is the `jsonBlob` contract.

### 3. Snap the coordinates and convert the 170 legacy files

Add `loaders/tripoint/TripointSnapper.java`, a pure class with no I/O:
- Input: the original `Coordinate` and the three `Country` objects.
- Use `TripointHelper.getCoordinates(country.geoData().geometry())` to get each country's vertices.
- Consider the vertices of country A within 0.5° of the original coordinate. For each one, compute the distance in km to the nearest vertex of B and the nearest vertex of C, using equirectangular km: `dx = Δlon·111.32·cos(meanLat)`, `dy = Δlat·110.57`. A vertex is a **junction** if both distances are at most 0.05 km.
- Return the junction closest to the original coordinate if it is within **30 km**. Otherwise return empty. The caller then **drops that tripoint** and logs a warning that names the countries and the reason.
- Distances are only compared within a ~1° window, so the simple approximation is enough.

Add `loaders/tripoint/TripointMigrationMain.java`, a one-off program. It takes `<legacyTripointDir> <outFile>`. It reads `1.json`..`170.json` as the existing `Tripoint` class, so it doesn't need country lookups. It snaps each coordinate using the geometry embedded in the file. It writes only the snapped tripoints to the new `tripoints.json`, pretty-printed and in the original order. For every tripoint it prints `n: names, moved X km` or `n: names, DROPPED (reason)`, plus a summary at the end.

Run it from the repo root, for example:
`mvn -q compile exec:java -Dexec.mainClass=com.parkinson.benjamin.geogames.loaders.tripoint.TripointMigrationMain -Dexec.args="src/main/resources/processed/tripoints src/main/resources/processed/tripoints.json"`
If `exec-maven-plugin` isn't available, use `java -cp` with `target/classes` and the dependency classpath from `mvn dependency:build-classpath`.

**Expected output**, as a sanity check from my own measurement: 164 snapped, with a median move of about 2 km, and 164 entries in `tripoints.json`. These six should come out DROPPED, and the user has agreed to remove them:
- `39.json` Somalia/Djibouti/Ethiopia: Natural Earth splits off Somaliland, so Somalia has no vertices nearby.
- `88`, `89`, `90.json`: Caspian Sea maritime tripoints, which have no land junction.
- `100.json` Pakistan/India/China: disputed, so there is no exact junction.
- `46.json` Ethiopia/South Sudan/Kenya: the nearest junction is about 110 km away, at the Ilemi Triangle, which is beyond the 30 km cap.

If the dropped set is anything other than exactly these six, stop and report back instead of continuing.

Then delete `src/main/resources/processed/tripoints/` (all 170 files) and `TripointMigrationMain.java`. In your final report, list the dropped tripoints.

### 4. Move raw data out of the jar and update the offline loaders

- Use `git mv` to move `src/main/resources/data/countries/` to `geodata/countries/` at the repo root. `data/rivers/` stays where it is, because rivers are still loaded at runtime.
- Add `loaders/countries/RawCountryReader.java` containing the old `loadCountries()` logic. It reads from **file paths** (`Path`), not the classpath, using its own `ObjectMapper` configured like `FileLoaderService`.
- `CountryLoaderMain` args: `<countries.geo.json> <additionalNames.json> <outDir>`.
- Add `loaders/countries/ProcessedCountryReader.java`, which reads every `*.json` in a directory `Path` as `Country`.
- Update `TripointLoaderMain` (the Wikipedia scrape, for future regeneration):
  - Get its countries from `ProcessedCountryReader` (args: `<processedCountriesDir> <outFile>`).
  - Run each result through `TripointSnapper`, and drop any result that doesn't snap, the same way the migration does.
  - Map each result to a `TripointDefinition`.
  - Write a single file.
- Change `TripointWriter` to write a `List<TripointDefinition>` to one file.
- Leave `TripointComputer` logic unchanged.

### 5. Tests

- Update `TripointFinderTest` for the new constructor and signature. Mock or stub the loader so it returns 0 definitions, and assert the `IllegalArgumentException`. Mockito comes with `spring-boot-starter-test`.
- Add `TripointSnapperTest` with small synthetic polygons:
  - Three squares meeting at a shared corner snap to that corner.
  - No shared vertex gives empty.
  - A shared vertex more than 30 km away gives empty.
- Add `ProcessedDataIntegrityTest`, a plain JUnit test that instantiates the services directly without Spring:
  - `tripoints.json` has 164 entries, and none of them is one of the six dropped tripoints.
  - Every entry in `tripoints.json` has exactly 3 distinct names.
  - Each name loads via `CountryLoaderService.loadCountryByName` with a non-null `geoData().geometry()`.
- `TripointComputerTest` makes a live call to Wikipedia and asserts nothing. Mark it `@Disabled("hits Wikipedia")` so `mvn test` works offline.

### 6. Docs

Update the "Geo data pipeline" section of `CLAUDE.md`:
- Raw sources are now in `geodata/countries/`.
- The new loader arguments.
- `processed/tripoints.json` is the single tripoint file, and its count is derived automatically.
- Snapping, including the rule that tripoints which can't be snapped are dropped, and the fact that editing `additionalNames.json` means re-running `CountryLoaderMain`.
- Remove the "hard-codes 170" bullet.

## Verification (all must pass before reporting done)

1. `mvn spotless:apply`, then `mvn test`. Everything should be green.
2. `mvn clean install -DskipTests`, then `unzip -l target/geogames-0.0.1-SNAPSHOT.jar | grep -E "countries.geo.json|processed/tripoints/"` should print nothing. Report the jar size before and after.
3. Start the app (`java -jar target/geogames-0.0.1-SNAPSHOT.jar`), then:
   - `curl -X POST localhost:8080/api/games -H 'Content-Type: application/json' -d '{"gameType":"TRIPOINT","numberOfRounds":10}'`
   - `GET /api/games/{id}/rounds/0` through `/9`. Each `jsonBlob` must parse into `coordinate` plus 3 `countries`, each with `name`, `additionalNames`, and `geoData.geometry.coordinates`.
   - Repeat the create request with `"gameType":"RANDOM"`.
4. `cd client && npm run lint` should be unchanged. No frontend files should be modified.

## Out of scope (mention in the final report, don't do)

- Natural Earth borders are generalised at 1:10m. Even with snapped markers, the borders won't line up exactly with the ArcGIS satellite imagery at zoom 10. A higher-resolution border source (for example geoBoundaries), or a lower `maxZoom`, would be a separate change.
