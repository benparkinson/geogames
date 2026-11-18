# Plan: stop re-sending round geometry after an answer, stop parsing 24 MB for country names, and hold coordinates compactly

Read `CLAUDE.md`, `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md` and `client/CLAUDE.md` first. This plan changes the backend, the frontend, the contract file (`client/src/test/api-shapes.json`) and those `CLAUDE.md` files. It needs no Flyway migration, changes no entity and must not change any file in `src/main/resources/processed/`.

Work on a new branch `lighter-answers` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

Do the three parts in order. They are independent: if Part 3 can't be made to pass its checks, revert Part 3 only and report, leaving Parts 1 and 2 in place. Each part ends with the checks named in it. If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output. The code snippets show the intended shape; if the code on `main` has moved on (for example `submitAnswer` has gained locking), keep what is there and apply only the change this plan describes.

## Goal

1. **Answers.** A round's response carries its geometry in `jsonBlob`, which can be hundreds of KB. Today one answer makes the server build and serialise that geometry three or four times:
   - `POST .../answers` returns a full `GameRound` including `jsonBlob`, and the client throws the body away (`postJson<unknown>` in `client/src/api/games.ts`).
   - The client then invalidates every cached round of the game, so the current round and round 0 (always mounted by `GameRoundPage`) are fetched again, and earlier rounds are fetched again when reopened. The geometry in those responses is identical to what the client already has; only the game result changed.

   After this plan the answer response is small (the round's answer state and points, plus the game result), and the client patches it into its cached rounds instead of refetching them.

2. **Country names.** The first `GET /api/countries` after each start reads all 242 files of `processed/countries` (24 MB of full-resolution geometry) to get each country's name and aliases. `CountryShapeService` already holds the same names and aliases in memory. After this plan the names come from there and the app never reads `processed/countries`.

3. **Coordinates.** `GeoGeometry.coordinates` is a raw `List`, so Jackson reads each `[lon, lat]` as an `ArrayList` holding two `Double` objects: about 84 bytes a point. The app keeps about 280,000 points in memory for as long as it runs (country shapes, rivers, lakes), which is roughly 23 MB. After this plan each point is one small object holding two `double` fields (about 36 bytes with its reference), so roughly 10 MB, and writing a point to JSON allocates nothing. The JSON the app reads and writes does not change by a single character.

## Part 1: a small answer response that the client patches into its cache

### Compatibility rule

The frontend and the backend deploy separately, so each must work with the other's previous version:

- The new response is `GameRound` without `gameType`, `jsonBlob` and `totalRoundCount`. Keep the four remaining field names exactly (`answerState`, `hintsUsed`, `points`, `gameResult`), so a new client reading an old backend's full `GameRound` finds the same fields.
- An old client ignores the body and refetches, so it works with the new backend unchanged.
- The new client patches its cache only when the response is complete (defined below). Anything else falls back to today's invalidation.

### Backend

**`api/RoundAnswerResponse.java`** (new):

```java
/**
 * The response to an answer: the round's answer and points, and the whole game's result. It has
 * no round data, which the client already holds. The field names match {@link GameRound}'s.
 */
public record RoundAnswerResponse(
    AnswerState answerState, int hintsUsed, int points, GameResult gameResult) {}
```

**`service/GameService.java`**: `submitAnswer` returns `Optional<RoundAnswerResponse>`. Replace its `return toResponse(game, r);` with a call to a new private method that builds the record from `r.getAnswerState()`, `r.getHintsUsed()`, `pointsFor(r)` and `calculateGameResult(game)`. It must not call a generator's `rebuild` or the `ObjectMapper`. `getGameRound` and `toResponse` stay as they are.

One behaviour changes as a result: answering a round whose data has gone (`RoundDataMissingException`) used to be a 410 and is now a normal 200, because the answer no longer rebuilds the round. `GET` of that round is still a 410. If a test asserts a 410 on an answer, change it to assert the 200 on the answer and the 410 on the `GET`.

**`controllers/GameController.java`**: `submitAnswer` returns `ResponseEntity<RoundAnswerResponse>`.

**Tests.** `GameServiceTest`, `GameControllerTest`, `GameApiIT`, `PostgresMigrationIT` and the `RoundStats*IT` classes all post answers. Where one reads `jsonBlob`, `gameType` or `totalRoundCount` from an answer response, read it from a `GET` of the round instead. Add:
- to `GameServiceTest`: answering does not call the generator's `rebuild` (verify on the mock, or whatever the test uses for generators);
- to `GameApiIT`: the answer response has exactly the fields `answerState`, `hintsUsed`, `points` and `gameResult`.

**Contract.** In `ApiContractIT.currentShapes()`, add an endpoint entry with the key `POST /api/games/{id}/rounds/{round}/answers`: create a game, post one valid answer to round 0 the way the class's existing helpers post and read bodies, and record `JsonShape.of` the response. Then regenerate the file:

```
bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend
```

The diff of `client/src/test/api-shapes.json` must show only the new endpoint.

Check: `bash scripts/check-java.sh`, then `bash scripts/check-it.sh`.

### Frontend

**`client/src/api/types.ts`**: add

```ts
/** The JSON of POST /api/games/{id}/rounds/{round}/answers. An older backend sends a whole GameRoundResponse, which has these fields too. */
export interface RoundAnswerResponse {
  answerState: AnswerState;
  hintsUsed?: number;
  points?: number;
  gameResult: GameResultResponse;
}
```

**`client/src/api/games.ts`**, `useSubmitAnswer`:

- `mutationFn` posts with `postJson<RoundAnswerResponse>`.
- Replace `onSettled` with `onSuccess` and `onError`.
- `onError`: `await queryClient.invalidateQueries({ queryKey: queryKeys.game(gameId) })`, as today. A refused answer (409) means the cache is wrong, so refetching is right.
- `onSuccess(response)`:
  1. If the response is complete, patch the cache; otherwise invalidate as in `onError`.
  2. Then prefetch the next round exactly as today (`round + 1 < totalRoundCount`). Keep returning the promise so the mutation stays pending until it is done.

"Complete" means: `answerState` is a string, `hintsUsed` and `points` are numbers, and in `gameResult` the fields `score`, `maxScore`, `roundsCorrect` and `perfectRounds` are numbers and `roundAnswerStates`, `roundPoints` and `roundHintsUsed` are arrays. Write this as one exported type-guard function with a test. A current backend always sends all of them; the guard exists for older backends and for `{}`.

Patching, with the game result view built field by field from the response (the same eight fields `GameRoundView.gameResult` has):

```ts
queryClient.setQueriesData<GameRoundView>({ queryKey: queryKeys.game(gameId) }, old =>
  old && { ...old, gameResult });
queryClient.setQueryData<GameRoundView>(queryKeys.round(gameId, round), old =>
  old && { ...old, answerState: response.answerState, hintsUsed: response.hintsUsed, points: response.points });
```

Before relying on the first call, confirm that every query stored under `queryKeys.game(gameId)` holds a `GameRoundView` (today the only key under it is `queryKeys.round`). If another kind of query lives there, restrict the patch to round queries.

Update the comments on `roundQuery` and `useSubmitAnswer` to say the answer patches the cached rounds and only a failure refetches. In `GameRoundPage.tsx` the comment "Pin the round, or the refreshed game result would move the resume point on by itself" still holds (round 0's cached result is patched); leave the code alone.

**Tests in `client/src/api/games.test.tsx`.** The existing `useSubmitAnswer across rounds` tests mock the POST with body `{}`, which is now the incomplete-response fallback: keep them, renamed to say so. Change the first `useSubmitAnswer` test and add the rest, so that these hold:
- After a successful answer with a complete response, the answered round's `answerState`, `hintsUsed`, `points` and `gameResult` in the cache equal the response's, with no further `GET` of that round.
- Another cached round of the same game gets the new `gameResult`, keeps its own `answerState` and `data`, and is not fetched again.
- The next round is prefetched when it is not cached, and nothing is fetched beyond the last round.
- A failed POST (for example a 409) refetches the game's cached rounds.

**Contract fixture.** Add an answer-response fixture to `client/src/test/fixtures.ts` and a row for `POST /api/games/{id}/rounds/{round}/answers` to the `describe.each` of endpoints in `client/src/api/contract.test.ts` ("matches the backend response exactly"). Build the mocked POST bodies in the tests above from that fixture.

Other tests that mock the answer POST (`GameRoundPage.test.tsx`, the per-game tests) return whatever they return today; if their body is not a complete response they take the fallback path and should still pass. Fix any that counted the refetch.

Check: `bash scripts/check-frontend.sh`. Then run the browser tests as `e2e/CLAUDE.md` describes; if a spec waited for the round to be refetched after an answer, make it wait for the answer response instead.

### Docs

- Root `CLAUDE.md`, "Game flow / API": the answers bullet says what the response is (the round's `answerState`, `hintsUsed`, `points` and the `GameResult`; no round data). In "Contract test", add the answers endpoint to the list of shapes.
- Backend `CLAUDE.md`, "Persistence": the sentence about serialising the round "on every read" stays true; add that answering does not rebuild the round. "API rules": note that answering a round whose data has gone succeeds, and only reading it is a 410.
- `client/CLAUDE.md`: if it describes the invalidation after an answer, describe the patch and the fallback instead.

## Part 2: country names from the shapes already in memory

`ProcessedDataIT.countryShapesMatchTheProcessedCountries` already proves that the names and aliases in `processed/country-shapes` equal those in `processed/countries`, apart from Antarctica, which has no shape. So the only change players can see is that Antarctica leaves the autocomplete list, and a guess of "Antarctica" is "not recognised" (no guess spent) instead of a wrong guess. That is accepted: Antarctica is never an answer.

`processed/countries` stays in the repository and in the jar. The offline loaders read it, and the tests below read it. Do not try to exclude it from the jar in this plan.

**`service/CountryLoaderService.java`**: take a `CountryShapeService` instead of the `JsonResourceReader`, and build the names from `loadShapes()`. Keep `@Cacheable("countryNames")`, the null-aliases-become-`List.of()` rule and the sort by name. Add a class comment saying why the names come from the shapes (the same names, already parsed, against 24 MB of files).

**Tests.**
- `TestFixtures.EXPECTED_COUNTRY_COUNT` becomes 241 and its comment says it is the number of files in `processed/country-shapes`. `CountryLoaderServiceTest` and `GameApiIT` use it.
- `CountryLoaderServiceTest`: construct the service with `new CountryShapeService(reader)`; add that the names don't include Antarctica.
- `ProcessedDataIT` and `CountryHelperTest` use `CountryLoaderService` to read the raw `processed/countries` files. They must keep reading the raw files (otherwise `countryShapesMatchTheProcessedCountries` would compare the shapes with themselves). Replace those calls with `reader.readAll("classpath*:/processed/countries/*.json", PlaceName.class)`, through one small private helper in each class, and remove the `CountryLoaderService` field from `ProcessedDataIT`.
- `PlaceNameControllerTest` mocks the service and needs no change.
- Search `client/src` and `e2e` for `Antarctica`; nothing is expected to depend on it, but fix a fixture or test that does.

The shape of `GET /api/countries` is unchanged, so `api-shapes.json` must not change in this part.

**Docs.** Backend `CLAUDE.md`, "Country shapes" paragraph: add that `GET /api/countries` is built from these shapes, so the app never reads `processed/countries` (it is loader input only). If the loaders' `CLAUDE.md` says the app reads `processed/countries` for names, correct it.

Check: `bash scripts/check-java.sh`, then `bash scripts/check-it.sh`.

## Part 3: hold each coordinate as two doubles

### Approach

`GeoGeometry(String type, List coordinates)` keeps its signature. About twenty test classes and most of the loaders build or read `coordinates` as nested lists, and none of that changes. Only what Jackson puts in the innermost lists changes: a `[lon, lat]` pair read from JSON becomes a `Position`, an immutable `List<Double>` of size 2 backed by two `double` fields. Everything that treats a position as a `List<Double>` or `List<Number>` keeps working, and `equals` against `List.of(12.34, 56.78)` still holds because `Position` extends `AbstractList`.

Don't go further than this (for example one `double[]` per ring). It would save a few more MB but changes the types the loaders and tests use.

The app uses Jackson 3 (`tools.jackson.*`, as in the imports of `GameService` and `GeoJson`). The class names below are Jackson 3's; if one doesn't compile, look it up in the Jackson jar on the classpath, don't switch to `com.fasterxml` classes.

### Code

**`model/geojson/Position.java`** (new):

```java
/**
 * One [longitude, latitude] pair read from JSON, held as two doubles: a third of the heap of an
 * ArrayList of two Doubles, and the app keeps hundreds of thousands of them. Immutable.
 */
@JsonSerialize(using = PositionSerializer.class)
public final class Position extends AbstractList<Double> implements RandomAccess {
  private final double x;
  private final double y;
  // constructor, x(), y(), get(int) (0 -> x, 1 -> y, else IndexOutOfBoundsException), size() == 2
}
```

**`model/geojson/PositionSerializer.java`** (new): a `tools.jackson.databind.ValueSerializer<Position>` that writes `writeStartArray()`, `writeNumber(x)`, `writeNumber(y)`, `writeEndArray()`. Without it Jackson would serialise the list through `get`, boxing two `Double`s for every point of every response. It must produce exactly what a list of two `Double`s produces, with the compact writer and with the loaders' pretty printer (`[ 107.783, 54.2938 ]`).

**`model/geojson/CoordinatesDeserializer.java`** (new): a `tools.jackson.databind.ValueDeserializer<List<?>>`, put on the record component with `@JsonDeserialize(using = CoordinatesDeserializer.class)` in `GeoGeometry`. It reads nested arrays of any depth, recursively:

- A float number token becomes a `Double` (`getDoubleValue()`). An integer token becomes what Jackson's untyped reading gives today (`getNumberValue()`), so nothing that is an `Integer` today becomes a `Double`.
- An array becomes an `ArrayList` of its values, trimmed to size and still mutable, as today, with one exception: an array of exactly two values that are both `Double` becomes a `Position`.
- Anything else in the array (a string, an object, null) is an error, reported through the `DeserializationContext`.

That rule keeps every other case exactly as it is now: an empty `coordinates`, positions with three values, and positions written as integers all stay plain lists.

`GeoGeometry` gets only the annotation. Its constructor, validation and `flattenedCoordinates()` stay as they are.

### Tests

- `PositionTest`: `get`, `size`, out-of-range index, equality and hash code with `List.of(1.5, 2.5)` in both directions, and that `set` and `add` throw.
- In `GeoGeometryTest` (or a new test beside it), with the app's `ObjectMapper` settings (`GeoJson.lenientReader()`):
  - Reading a Polygon, a MultiPolygon and a MultiLineString gives `Position` instances at the innermost level, and `flattenedCoordinates()` returns the same values as before.
  - A position with three values, a position of integers and an empty `coordinates` are read as plain lists with the same values and types as before this change.
  - Round trip: for each of those inputs, reading then writing gives the same JSON string as writing the same data built from plain `List.of(...)` lists, once with a plain `ObjectMapper` writer and once with `GeoJson.prettyWriter()`.
- In `ProcessedDataIT`: every innermost position of every loaded country shape (outline, coast and land borders), river and lake is a `Position`. This is what proves the saving is real and stops the fallback from quietly taking over if the data format changes.

No existing test should need its expectations changed. If one fails because it expects an `ArrayList`, or a loader mutates a position in place, fix that call site to copy the position (`new ArrayList<>(position)`); don't make `Position` mutable.

### Checks

- `bash scripts/check-java.sh`
- `bash scripts/check-it.sh`. `api-shapes.json` must not change, and `RoundRebuildIT` and `ApiContractIT` must pass untouched.
- `bash scripts/check-processed-data.sh` (see the loaders' `CLAUDE.md`). It regenerates the processed data and must report no difference: that is the proof that the loaders, which read and write through the same `GeoGeometry`, still produce byte-identical files. If it reports a difference, the serializer or the deserializer's rules are wrong; fix them, never commit regenerated data.

### Docs

Backend `CLAUDE.md`, "Conventions": one bullet saying that coordinates read from JSON are `Position`s (two doubles, immutable, a `List<Double>`), why, and that code building geometry by hand may still use plain lists.

## Finish

Run all three once more on the final tree: `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh`, `bash scripts/check-it.sh`, plus `bash scripts/check-processed-data.sh` if Part 3 is in. Report to the user:
- what changed, per part;
- the result of each check, and of the browser tests, or that they were not run and why;
- any place the code differed from this plan and what was done instead.
