# Plan: remove non-country entries from the country data

Goal: drop 13 Natural Earth features that are not real countries or territories (military bases,
a buffer zone, a disputed glacier, reefs and banks). They should disappear from
`src/main/resources/processed/countries/` and from `GET /api/countries`, and so from the Tripoint
autocomplete.

Read `CLAUDE.md` first. Do **not** change any polygon geometry. The holes these features leave
in neighbouring countries (e.g. Baykonur inside Kazakhstan) are accepted.

## Background (current state)

- The raw source is `geodata/countries/countries.geo.json`: a JSON array of GeoJSON Features with
  `properties.name`. Aliases are in `geodata/countries/additionalNames.json`, a map from name to a
  list of aliases.
- `loaders/countries/CountryLoaderMain <countries.geo.json> <additionalNames.json> <outDir>` calls
  `RawCountryReader.readCountries(Path, Path)`, then `CountryWriter.writeCountries(List<Country>, Path)`.
  The writer writes one file per country, named by `helper/CountryHelper.convertCountryNameToFileName`
  (spaces and dots removed, plus `.json`). **It never deletes files**, so stale files survive a
  re-run.
- At runtime `service/CountryLoaderService.loadCountryNames()` globs
  `classpath*:/processed/countries/*.json`. Removing the files is enough to remove the names from
  the API. No frontend change is needed.
- There are currently 255 processed country files.
- None of the 164 entries in `src/main/resources/processed/tripoints.json` reference the names
  below, so **do not regenerate tripoints**. That needs network access to Wikipedia.

## Names to exclude (exact `properties.name` values)

```
Akrotiri Sovereign Base Area
Dhekelia Sovereign Base Area
US Naval Base Guantanamo Bay
Baykonur Cosmodrome
Cyprus No Mans Area
Siachen Glacier
Bajo Nuevo Bank (Petrel Is.)
Serranilla Bank
Scarborough Reef
Spratly Islands
Ashmore and Cartier Islands
Coral Sea Islands
Clipperton Island
```

Keep Kosovo, Somaliland and Northern Cyprus. Kosovo is used by tripoints. Keep all overseas
territories.

## Steps

### 1. Exclusion file

Create `geodata/countries/excludedNames.json` holding a JSON array of the 13 names above, exactly
as spelled there.

### 2. `RawCountryReader`

Add a third parameter: `readCountries(Path countriesGeoJson, Path additionalNamesJson, Path excludedNamesJson)`.
- Read the excluded names as a `Set<String>` with the same `objectMapper`.
- Filter out every feature whose `properties().name()` is in the set, before mapping to `Country`.
- If an excluded name matches no feature, throw `IllegalArgumentException` and list the unmatched
  names. This catches typos in the exclusion file.
- Update the class Javadoc.

### 3. `CountryLoaderMain`

- Change the usage to
  `CountryLoaderMain <countries.geo.json> <additionalNames.json> <excludedNames.json> <outDir>`,
  and update the Javadoc to match.
- Before writing, delete every existing `*.json` file directly inside `outDir`, so that removed
  countries don't leave stale files. Do this in `CountryLoaderMain` or as a clearly named method
  on `CountryWriter`, e.g. `clearOutput(Path)`. Delete only `*.json` files at the top level and
  don't recurse.

### 4. Regenerate the processed files

Compile, then run the main class with the project classpath. For example:

```
mvn -q compile -Dskip.npm -Dskip.installnodenpm
mvn -q dependency:build-classpath -Dmdep.outputFile=<scratch>/cp.txt
java -cp "target/classes;$(cat <scratch>/cp.txt)" com.parkinson.benjamin.geogames.loaders.countries.CountryLoaderMain \
  geodata/countries/countries.geo.json \
  geodata/countries/additionalNames.json \
  geodata/countries/excludedNames.json \
  src/main/resources/processed/countries
```

(The classpath separator is `;` on Windows and `:` elsewhere. Running the main method from the
IDE also works.)

Then check `git status -- src/main/resources/processed/countries`:
- Exactly 13 files are **deleted**: `AkrotiriSovereignBaseArea.json`, `DhekeliaSovereignBaseArea.json`,
  `USNavalBaseGuantanamoBay.json`, `BaykonurCosmodrome.json`, `CyprusNoMansArea.json`,
  `SiachenGlacier.json`, `BajoNuevoBank(PetrelIs).json`, `SerranillaBank.json`,
  `ScarboroughReef.json`, `SpratlyIslands.json`, `AshmoreandCartierIslands.json`,
  `CoralSeaIslands.json`, `ClippertonIsland.json`.
- **No other file is modified.** If the remaining files show diffs (line endings, formatting), stop
  and investigate instead of committing the churn. As a fallback, revert the regenerated files and
  just `git rm` the 13 files above.
- There are 242 files left.

### 5. Tests

- `src/test/java/.../service/CountryLoaderServiceTest.java`: change `255` to `242`. Add assertions
  that the names do not contain `"Siachen Glacier"` or `"US Naval Base Guantanamo Bay"`.
- `src/test/java/.../loaders/countries/RawCountryReaderTest.java`: update the existing test for the
  new parameter (pass an empty-array exclusion file), and add:
  - a test that an excluded name is dropped and the others are kept;
  - a test that an exclusion name matching no feature throws `IllegalArgumentException`.
- If `CountryWriter` gains `clearOutput`, add a test in `CountryWriterTest` with `@TempDir`. It
  should delete `.json` files and leave a non-JSON file and a subdirectory alone.
- Use JUnit 5 and AssertJ, matching the surrounding tests. No test may hit the network.

### 6. Docs

In `CLAUDE.md`, under "Geo data pipeline":
- Mention `excludedNames.json` alongside `additionalNames.json` in the `geodata/countries/` bullet
  (non-country features such as bases and reefs to skip).
- Update the `CountryLoaderMain` usage line to the new four-argument form. Note that it clears
  `*.json` in `outDir` first, and that editing `excludedNames.json` also means re-running it.

### 7. Verify

```
mvn spotless:apply
mvn test
```

Both Java and frontend tests must pass. Don't commit unless asked.

## Out of scope

- Merging or reshaping any polygons, including filling the Baykonur, Guantanamo and Siachen holes.
- Regenerating `tripoints.json`.
- Frontend changes.
