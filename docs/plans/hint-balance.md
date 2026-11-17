# Plan: rebalance the hints

Read `CLAUDE.md` and `client/CLAUDE.md` first. This plan changes only the frontend (`client/`). The backend, the API, the contract shapes, the scoring formula, and each type's `hintCount` and `guessLimit` all stay as they are.

Work on a new branch `hint-balance` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

Do the steps in order. Each step ends with `bash scripts/check-frontend.sh <folder>` for the folders it touched; the last step runs everything. If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Goal

A hint should make the round easier without naming the answer, and each hint in a mode should be worth roughly the same, because every hint costs the same number of points (17/16/17 with three hints, 25/25 with two). Five things break that today:

1. **Tripoint:** a hint can outline a country the player has already named.
2. **Country Gaps:** for landlocked countries hint 1 says "It has no coastline", which the map already shows. Hint 2 names three neighbours, which identifies 139 of the 141 eligible countries.
3. **River Shapes and Lake Shapes:** hint 2 lists the countries. After continent plus that list, 35 of 50 rivers and 30 of 48 lakes are the only candidate left, so hint 3 has nothing to do.
4. **Rivers on the Map and River Ends:** hint 1 ("Flows through: …") mostly restates what the satellite map shows.
5. **Coastlines:** the neighbour count in hint 1 includes neighbours of overseas territories (France 10, Indonesia 3), which doesn't match the mainland the map shows.

## The hints after this plan

| Mode | Hint 1 | Hint 2 | Hint 3 |
|---|---|---|---|
| Tripoint | outline of a country not yet named | same | same |
| Country Gaps, with a coast | coastline on the map (unchanged) | `Longest border: Germany` | first letter (unchanged) |
| Country Gaps, landlocked | `No coastline. Its borders are 5,224 km long` | `Longest border: Germany` | first letter |
| Coastlines | `Its mainland borders 3 countries` | land borders on the map (unchanged) | first letter (unchanged) |
| Land Borders | unchanged | unchanged | unchanged |
| River Shapes | continent (unchanged) | `Flows through 3 countries` | first letter (unchanged) |
| Lake Shapes | continent (unchanged) | `Lies in 1 country` / `Shores in 3 countries` | first letter (unchanged) |
| Rivers on the Map | unlocks panning and zoom | first letter (unchanged) | |
| River Ends | draws the river's course and unlocks panning and zoom | first letter (unchanged) | |
| River Countries | unchanged | unchanged | |

Measured on the real data, the new second hints leave a pool the last hint still has to narrow: continent plus country count leaves 3.3 rivers or 5.5 lakes on average, and the longest neighbour alone leaves 2.1 countries.

## Ground rules

- Hint counts come from the backend (`round.hintCount`). Never change how many hints a mode has.
- `HintConfig.description` is the hint button's tooltip. Update it in every mode whose hints change, so it still describes them in order.
- Delete a hint builder (and its tests) once nothing uses it. Don't leave unused exports.
- The Vitest coverage thresholds apply: every new function and branch needs a test. Follow the test conventions in `client/CLAUDE.md` (never render a real Leaflet map; use `leafletMock` for `*Map.tsx` tests and mock the `*MapWrapper` modules in game tests).
- Map colours stay in `client/src/map/constants.ts`. Use the existing `hintLineOptions()` for anything a hint draws.

## Step 1: Tripoint hints skip countries already named

Today `TripointGame.tsx` shuffles `hintOrder` once and `TripointMap.tsx` outlines `hintOrder.slice(0, hintsUsed)`. The map doesn't know what has been found.

The choice must be made when the hint is taken and then stay fixed. Deriving it from the current found list would be wrong: if hint 1 outlined A and the player then names A, the outline would jump to B for free. So record what was found at the moment of each hint.

1. `games/common/guessReducer.ts`: add `foundAtHint: string[][]` to `GuessState`. It has one entry per hint taken, holding the canonical answers found when that hint was taken.
   - The `"hint"` action appends `state.guesses.flatMap(g => g.answer === null ? [] : [g.answer])`.
   - `initialGuessState`: `[]` for an unanswered round. For an answered round, `hintsUsed` empty arrays (the server doesn't keep the guesses, and the map shows the answers anyway).
   - Keep the invariant `foundAtHint.length === hintsUsed`.
2. `map/MapProps.ts`: add optional `foundAtHint?: string[][]` with a doc comment. Optional, so the other maps and their tests don't change.
3. `games/common/MapGame.tsx`: pass `foundAtHint={guessState.foundAtHint}` to `MapComponent`.
4. `games/tripoint/model.ts`: add a pure function

   ```ts
   /** The countries the hints outline, in the order they were taken. */
   export function hintedCountries(hintOrder: string[], hintsUsed: number, foundAtHint: string[][] = []): string[]
   ```

   For each hint `i` from 0 to `hintsUsed - 1`: pick the first name in `hintOrder` that is not already picked and not in `foundAtHint[i]` (treat a missing entry as empty). If there is none, pick the first name not already picked. If every name is picked, stop.
5. `games/tripoint/TripointMap.tsx`: replace `hintOrder.slice(0, hintsUsed)` with `hintedCountries(...)`.
6. `TripointGame.tsx`: change the description to "Shows the outline of a country you haven't named yet".

Tests:
- `guessReducer.test.ts`: a hint with nothing found records `[]`; a hint after a correct guess records that answer; wrong guesses are not recorded; `initialGuessState` for answered and unanswered rounds. Update the existing `toEqual` state expectations, which now include `foundAtHint`.
- `tripoint/model` (new test file): no hints; hints with nothing found follow `hintOrder`; a found country is skipped; an outline doesn't move when its country is found later (`foundAtHint = [[], ["A"]]` still picks A first); the fallback when every unfound country is already outlined; a missing `foundAtHint`.
- `TripointMap.test.tsx`: with `hintOrder` Germany, Belgium, Netherlands, `hintsUsed: 1` and `foundAtHint: [["Germany"]]`, Belgium's outline is drawn and Germany's is not.
- `MapGame.test.tsx`: the map component receives `foundAtHint`.
- `TripointGame.test.tsx`: its mocked map wrapper uses `hintOrder.slice`; keep it working (use `hintedCountries` there or assert on `hintsUsed`).

Not in this step: disabling the hint button when every unnamed country is already outlined. The fallback outlines a named country in that case, which the player can see coming.

Check: `bash scripts/check-frontend.sh src/games/common src/games/tripoint src/map`.

## Step 2: Country Gaps

In `games/countries/countryHints.ts`:

1. Add `longestBorderHint`: `Longest border: ${name}`, where `name` is the first neighbour with `mainland: true` (`country.neighbours` is sorted longest border first). Null when there is none.
2. Add `borderLengthHint`: `No coastline. Its borders are ${country.landBorderKm.toLocaleString("en-GB")} km long`.
3. Delete `neighbourNamesHint`.

In `games/countries/neighbours.ts`:

- Hint 1: `borderLengthHint` when `country.coastKm === 0`, otherwise `mapHint("Its coastline is on the map")` as now.
- Hint 2: `longestBorderHint`.
- Hint 3: `firstLetterHint`.
- Description: "Shows its coastline, then its longest border, then its first letter" for a country with a coast; "Shows how long its borders are, then its longest border, then its first letter" for a landlocked one.

`NeighboursMap.tsx` needs no change.

Tests: update `countryHints.test.ts` and `neighbours.test.tsx` (the "Borders: Germany, Belgium" and "It has no coastline" expectations).

Check: `bash scripts/check-frontend.sh src/games/countries`.

## Step 3: Coastlines counts mainland neighbours only

In `countryHints.ts`, change `neighbourCountHint` to count only neighbours with `mainland: true`, and word it with `countHint(count, "country", "countries", "Its mainland borders")`, giving "Its mainland borders 3 countries". Update its doc comment.

The United Kingdom is the one eligible country with a mainland count of 0 ("Its mainland borders 0 countries"). That is correct: the map shows Great Britain.

Tests: `countryHints.test.ts` (add a case where an overseas neighbour is not counted) and `coastline.test.tsx` ("Borders 3 countries").

Check: `bash scripts/check-frontend.sh src/games/countries`.

## Step 4: River Shapes and Lake Shapes say how many countries, not which

1. `games/rivers/riverHints.ts`: add `countryCountHint`: `countHint(river.countries.length, "country", "countries", "Flows through")`. Use it as hint 2 in `shapes.ts` in place of `flowsThroughHint`. Leave `flowsThroughHint` until step 5 removes its last users.
2. `games/lakes/lakeHints.ts`: replace `shoresHint` with `countryCountHint`: `countHint(n, "country", "countries", n === 1 ? "Lies in" : "Shores in")`. Delete `shoresHint`.
3. `client/CLAUDE.md`, "Game families": the lakes line says the hints are "continent, countries on the shore, first letter". Change it to "continent, number of countries on the shore, first letter".

Tests: `riverHints.test.ts`, `shapes.test.tsx`, `lakeHints.test.ts`, `LakeGame.test.tsx`.

Check: `bash scripts/check-frontend.sh src/games/rivers src/games/lakes`.

## Step 5: Rivers on the Map and River Ends get a map hint

Both maps use `map/LockedView` with `locked={!gameOver}`. `LockedView` already handles `locked` turning false: panning comes back and the zoom range opens. Use that for hint 1.

**The caption.** Rivers on the Map shows the caption "No panning: zoom is limited." while playing, which would be false after the hint. Let a caption depend on the hints:

- `MapGame.tsx`: change `mapCaption` to `string | ((hintsUsed: number) => string | undefined)`. Resolve it before rendering; show no caption element when the result is empty.
- `RiverGame.tsx`: give `RiverVariant.mapCaption` the same type.

**Rivers on the Map.**
- `RiverOnMapMap.tsx`: read `hintsUsed` and pass `locked={!gameOver && hintsUsed < 1}`. Update the doc comment.
- `onMap.ts`: build the hints by hand, as `countries.ts` does. Hint 1 text: "The map is unlocked: pan and zoom freely". Hint 2: `firstLetterHint(river)`. Description: "Unlocks panning and zoom, then shows its first letter". Caption: `(hintsUsed) => (hintsUsed < 1 ? "No panning: zoom is limited." : undefined)`.

**River Ends.**
- `RiverLayers.tsx`: add `RiverHintLine({ river })`, a `GeoJSON` of `river.geoData` styled with `hintLineOptions()` and keyed by `river.id + "/hint"`.
- `RiverEndsMap.tsx`: read `hintsUsed`. Draw `RiverHintLine` when `!gameOver && hintsUsed >= 1`, and pass `locked={!gameOver && hintsUsed < 1}` so the whole course can be seen (the view is fitted to the two ends only). Update the doc comment.
- `ends.ts`: hint 1 text: "Its course is on the map. Pan and zoom freely". Hint 2: `firstLetterHint(river)`. Description: "Shows the river's course, then its first letter".

**Clean up.** Delete `flowsThroughHint` from `riverHints.ts`, and `listHint` from `games/common/hints.ts` if nothing uses it any more (check with a search). Delete their tests. `textHints` stays (River Shapes uses it).

Tests:
- `MapGame.test.tsx`: a function caption is called with `hintsUsed`, and the caption disappears when it returns `undefined`.
- `RiverOnMapMap.test.tsx`: locked with no hints, unlocked after one hint, unlocked when the round is over.
- `RiverEndsMap.test.tsx`: no line with no hints; the hint line after one hint; the answer line (not the hint line) when the round is over; the lock as above.
- `onMap.test.tsx` and `ends.test.tsx`: the new hint texts replace "Flows through: Romania, Germany, Austria"; in `onMap.test.tsx` the caption goes away after the first hint.
- `riverHints.test.ts`, `hints.test.ts`: remove the tests of deleted functions.

Check: `bash scripts/check-frontend.sh src/games/rivers src/games/common`.

## Step 6: Verify everything

1. `bash scripts/check-frontend.sh` (lint with zero warnings, `lint:rules`, `tsc`, all of Vitest).
2. Coverage thresholds, which CI enforces: in `client/`, with `target/node` on `PATH`, run `npm run coverage`.
3. Search `client/src` and `e2e` for the removed texts and names and confirm nothing is left: `Flows through:`, `Shores in:`, `Lies in:`, `Borders:`, `It has no coastline`, `flowsThroughHint`, `shoresHint`, `neighbourNamesHint`.
4. No backend check is needed: nothing under `src/` changes, and `client/src/test/api-shapes.json` must be untouched. If it shows up in `git status`, something went wrong.

Then report to the user: what changed per mode, the check results, and anything that didn't go to plan.

## Not in this plan

- Changing the scoring (for example making the last hint cost more) or any `hintCount`.
- Swapping hints 1 and 2 in Land Borders.
- Disabling the Tripoint hint button when no unnamed country is left to outline.
- New data: no hint here needs a field the round doesn't already carry.
