# Plan: autocomplete for river and lake guesses

Audience: the agent implementing this. Read `CLAUDE.md` first; its conventions (tests, formatting,
Node from `target/node`, no network in tests, never commit to `main`) all apply.

**Already in place (build on it):** hint scoring and the guess limit (`docs/plans/guess-limit.md`).
Only *wrong* guesses use up the limit. A guess that `isKnownGuess` rejects is reported as
"unknown" ("We don't recognise "x" as a river. Check the spelling.") and is free.

## Goal

River games, and later lake games, get the same guess support Tripoint has for countries:
- **Autocomplete:** the guess box suggests the names of every river (or lake) in the game. This
  also tells the player which rivers are in play.
- **Spell checking:** a name that isn't a river in the game, or one of its aliases, is "unknown"
  rather than wrong, so a typo no longer costs one of the 5 guesses.

Matching is unchanged: the answer is still checked against the round's own name and aliases by
`matchAnswer`.

## Decisions (don't reopen them)

1. **One list per family, not per game type.** Every river game suggests all the rivers, including
   `RIVERS_BY_COUNTRIES`, which only asks about rivers through 3+ countries. A shorter list per type
   would hint at the answer, and one cached list is simpler.
2. **Suggest display names only.** The datalist shows `name`. Aliases ("Donau") still count as known
   and still match, but aren't listed, because a list with every alias would be cluttered.
3. **One endpoint per family, the same shape as `/api/countries`:** `GET /api/rivers` and (later)
   `GET /api/lakes` return `[{name, additionalNames}]`, sorted by name, with a day of
   `Cache-Control`. They return names only, never geometry.
4. **If the list fails to load,** the game still works without suggestions, and every guess is
   judged right or wrong as today. This is what Tripoint already does.

## Facts (don't re-derive)

- `processed/rivers` holds 50 rivers with 106 aliases between them. No two rivers share a
  normalised name or alias (checked with `normaliseString`'s rules), so a single "known names" set
  is unambiguous. The integrity test in B3 keeps it that way.
- The country version of this is spread across `controllers/CountryController`,
  `CountryLoaderService.loadCountryNames` (`@Cacheable("countryNames")`), `model/CountryName`,
  `client/src/api/countries.ts` (`useCountryNames`, `staleTime`/`gcTime: Infinity`, `retry: 1`),
  `api/queryKeys.ts`, and the `useMemo` code in `games/tripoint/TripointGame.tsx`.
- `docs/plans/country-shape-games.md` Phase 0.1 extracts that Tripoint code into a shared hook.
  This plan defines the general version, `useGuessList(kind)`. Whichever plan runs first creates
  it, and the other uses it.

## Delivery

Two PRs, in order: `river-autocomplete-backend`, then `river-autocomplete-frontend`. Each must be
green on `mvn clean install`. Run `mvn spotless:apply` and `npm run lint` before committing.

---

## Backend

### B1. A generic name record

Rename `model/CountryName` to `model/PlaceName` (`name`, `additionalNames`). Its JSON is unchanged.
Update `CountryLoaderService.loadCountryNames`, `CountryController` and their tests. Don't touch
`CountryHelper` / `convertCountryNameToFileName`, which are about file names.

### B2. `GET /api/rivers`

- `RiverLoaderService.loadRiverNames()`: `@Cacheable("riverNames")`. It maps `loadRivers()` to
  `PlaceName(name, additionalNames == null ? List.of() : additionalNames)`, sorted by name.
- Rename `CountryController` to `PlaceNameController` (keep `@RequestMapping("/api")` and the
  existing `/countries` method unchanged) and add:
  ```java
  @GetMapping("/rivers")
  public ResponseEntity<List<PlaceName>> rivers()   // same Cache-Control as /countries
  ```
- Rename `CountryControllerTest` to `PlaceNameControllerTest`, mocking both services. Add the
  `/rivers` cases mirroring the country ones: the body, a day of caching, and a loading failure that
  escapes the controller.
- `RiverLoaderServiceTest`: the names come back sorted, and a null `additionalNames` becomes empty.
- `GameApiIntegrationTest`: `GET /api/rivers` returns 50 items, and every river in a created
  `RIVERS_BY_SHAPE` game's `jsonBlob` is in that list by name.
- `WebConfig` already applies CORS to `/api/**`, so the new endpoint needs nothing extra. Check that.

### B3. Integrity

`ProcessedDataIntegrityTest`: no two rivers share a name or alias once normalised (use
`AliasBuilder.normalise`, the Java copy of the frontend's `normaliseString`). If this ever fails,
fix it with an override (`name` or `removeAliases`), not in code.

### B4. `CLAUDE.md`

In "Game flow / API", add `GET /api/rivers` next to `/api/countries`: rivers for autocomplete and
spell checking, cached.

---

## Frontend

### F1. A generic names query (`client/src/api/`)

- Rename `countries.ts` to `placeNames.ts`, and its test to match:
  ```ts
  export type PlaceKind = "countries" | "rivers" | "lakes";
  export interface PlaceName { name: string; additionalNames: string[] }
  export function usePlaceNames(kind: PlaceKind): UseQueryResult<PlaceName[]>
  ```
  It fetches `/api/${kind}` with the same options as `useCountryNames` today, and the error text
  "load countries" / "load rivers" / "load lakes".
- `queryKeys.ts`: replace `countries` with `placeNames: (kind: PlaceKind) => ["placeNames", kind] as const`.
- Keep `useCountryNames` only if something other than Tripoint uses it. Otherwise remove it, since
  F2 replaces it.
- Tests: each kind hits its URL, one fetch is shared between hooks, the kinds don't share a cache
  entry, and the retry-then-error case. Add `riverNames` next to `countryNames` in
  `test/fixtures.ts`, matching `makeRiverData()` (Danube / "Donau") plus one other river.

### F2. `useGuessList` (`client/src/games/common/useGuessList.ts`)

If `useCountryGuessing` exists (from the country-shape plan), turn it into this hook and update its
callers. Otherwise, extract this hook from `TripointGame.tsx`.

```ts
/** Autocomplete names and a spell check for a family of answers, from GET /api/<kind>. */
export function useGuessList(kind: PlaceKind): {
  suggestions?: string[];                      // display names, sorted and de-duplicated
  isKnownGuess?: (guess: string) => boolean;  // name or alias, via normaliseString
};
```

Both are `undefined` until the list loads, or if it fails, so `MapGame` behaves as it does today.
`TripointGame` uses `useGuessList("countries")`. Test it with `renderHook` and `mockFetch`: undefined
before the list loads, aliases and accent/case variants known, an unknown name rejected, suggestions
without aliases, and undefined after a failed load.

### F3. River games

`games/rivers/RiverGame.tsx` (`makeRiverGame`): call `useGuessList("rivers")` and pass `suggestions`
and `isKnownGuess` to `MapGame`. This one change covers all four river variants.

Tests: the river game tests now render with a `fetch` route for `/api/rivers` (use the
`riverNames` fixture). Add to `RiverGame.test.tsx`:
- The guess box offers suggestions (check the datalist options once the list has loaded).
- An unknown name ("Danub") gives the "don't recognise … as a river" feedback and **doesn't**
  lower the guesses-left count.
- A known but wrong river ("Thames") is wrong and lowers the count.
- An alias ("Donau") still counts as correct.
- With `/api/rivers` failing, a misspelling counts as wrong, as before.

Check the other river variant tests (`onMap`, `ends`, `countries`, `shapes`): any that render the
component now need the `/api/rivers` route, or they'll log failed fetches.

### F4. `CLAUDE.md`

In "Frontend structure", `useGuessList(kind)` gives the autocomplete and spell check for countries,
rivers and lakes. A game whose answers come from a fixed list should use it.

---

## Lakes

Lakes don't exist yet; they come from `docs/plans/feature-pipeline-and-lakes.md`, which has
already been updated to use this.
- **If this plan lands first:** that plan adds `/api/lakes`
  (`LakeLoaderService.loadLakeNames()` with `@Cacheable("lakeNames")`, plus a method on
  `PlaceNameController`) and uses `useGuessList("lakes")`. The `"lakes"` kind already exists in
  F1, so nothing else is needed.
- **If lakes land first:** add those two things in this plan's PRs, with the same tests as for
  rivers, and the lake-name uniqueness check in B3.

## Manual check

Run the backend and `npm run dev`. In a river game:
- Typing "Dan" suggests Danube.
- "Danub" says it doesn't recognise the name and keeps the guesses-left count.
- A wrong river costs a guess.
- "Donau" is accepted.

Repeat the checks in a Tripoint game to confirm the country autocomplete is unchanged. Check the
datalist on a phone-sized viewport; native datalists differ between browsers.

## Out of scope

- A custom autocomplete dropdown (fuzzy matching, matching on aliases as you type). The native
  `datalist` is what Tripoint uses, so it stays.
- "Did you mean …?" suggestions for unknown names.
