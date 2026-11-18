# Persist guesses across a page refresh

## Goal

Today a round's guesses live only in `MapGame`'s React state. Refreshing the page (or opening the game link in another tab) gives the player all their wrong guesses back, and they can type the same wrong guess again. After this change:

- A refresh shows the same wrong guesses and the same "N guesses left" as before it.
- A guess made before the refresh is a duplicate after it ("already guessed"), so it doesn't use another guess.
- Correct guesses in a multi-answer round (Tripoints, Border Shapes, Merged Countries) are kept too, so a half-solved round stays half-solved. It is the same mechanism and the replay (below) needs them to give the right state.

**Out of scope:** hints. `hintsUsed` is still sent only with the answer, so a refresh still resets the hints of an unanswered round. The new endpoint is shaped so a later change could add hints the same way; don't do it here.

## Decision: store the guesses on the server

The guesses go in the database next to the round, not in `localStorage`. Reasons:
- Answers are already server state and final. Guesses belong with them: a second tab, a second device or the shared `/games/{id}` link see the same round.
- No new browser storage to describe, expire or clear.

Privacy (root `CLAUDE.md`, "Storage and privacy"): a guess is free text the player typed, so the server **must not store it as typed**. It accepts only a guess that is a known place name or alias for the round's type (the same lists as `GET /api/countries`, `/api/rivers`, `/api/lakes`) and stores that list's spelling. Anything else is a 400 and nothing is stored. Every game already rejects unknown names on the client with `isKnownGuess`, so a normal player never hits that 400. Guesses are deleted with the game after 24 hours, through the foreign key cascade.

## Flow

1. The player makes a guess that `guessReducer` records (wrong or correct, not empty, duplicate or unknown).
2. `MapGame` calls `round.recordGuess({ text, correct })`, which POSTs `/api/games/{id}/rounds/{round}/guesses`.
3. If that guess ended the round, `MapGame` then calls `submitAnswer` as today. Both mutations share a TanStack Query mutation `scope`, so the answer is sent only after the guess has been sent.
4. `GET /api/games/{id}/rounds/{round}` returns the stored `guesses`. On mount, `MapGame` replays them through `guessReducer` to rebuild its state.

## Backend

Read `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md` and `src/main/CLAUDE.md` first.

### 1. Migration

Add `src/main/resources/db/migration/V6__round_guesses.sql`. V5 is the latest today; check that no other branch on `main` has taken V6, and use the next free number if one has. Write it to run on both Postgres and H2 in PostgreSQL mode, like V1:

```sql
-- The guesses made in a round, in order, so a refreshed page can restore them. Only known place
-- names are stored (the spelling from the name lists), never the text as typed.
create table game_round_guess (
    round_id bigint       not null references game_round_entity (id) on delete cascade,
    position int          not null,
    guess    varchar(255) not null,
    correct  boolean      not null,
    primary key (round_id, position)
);
```

`on delete cascade` is required: `GameCleanupService` deletes games in bulk, the rounds go by cascade, and the guesses must go with them.

### 2. Entity

- New `@Embeddable` class `dao/RoundGuess` with `guess` (`String`) and `correct` (`boolean`). Give it a no-arg constructor and getters, like the other entities.
- In `GameRoundEntity`, add:
  ```java
  @ElementCollection
  @CollectionTable(name = "game_round_guess", joinColumns = @JoinColumn(name = "round_id"))
  @OrderColumn(name = "position")
  private List<RoundGuess> guesses = new ArrayList<>();
  ```
  with a getter (return an unmodifiable view) and an `addGuess(RoundGuess)` method. Map the embeddable's columns explicitly (`@Column(name = "guess", nullable = false, length = 255)`, `@Column(name = "correct", nullable = false)`). `ddl-auto=validate` checks the mapping against the migration, so a mismatch fails at start-up.
- A change to an element collection increments the owning entity's `@Version`, so two guesses at the same moment conflict like two answers do. Test this (see Tests); don't add `@OptimisticLock(excluded = true)`.

### 3. Which name list a type uses

- New enum `model/PlaceKind { COUNTRIES, RIVERS, LAKES }`.
- Add it as a fourth argument to each `GameType` constant, with a `placeKind()` getter. The mapping is the same as the frontend's `useGuessList` calls: `RIVERS_BY_SHAPE` and `RIVERS_BY_COUNTRIES` are `RIVERS`, `LAKES_BY_SHAPE` is `LAKES`, every other type is `COUNTRIES` (including `COUNTRIES_BY_RIVERS`, whose answer is a country).
- Don't add it to `GameTypeResponse`: the client doesn't need it, and the contract stays smaller.

### 4. Name lookup

- The normaliser exists only in the offline loaders (`loaders/features/AliasBuilder.normalise`), which the app must not depend on. Move it to `helper/PlaceNames.normalise(String)` (or a similarly named helper class) with the same comment ("A Java copy of the frontend's `normaliseString`"), and make `AliasBuilder.normalise` call it, so there is one copy.
- New `@Service` `service/KnownPlaceNames` with `Optional<String> resolve(PlaceKind kind, String guess)`. It builds, once per kind, a `Map<String, String>` from the normalised name or alias to its spelling in the list, using `CountryLoaderService.loadCountryNames()`, `RiverLoaderService.loadRiverNames()` and `LakeLoaderService.loadLakeNames()` (already cached). Use `putIfAbsent`, names before aliases, so a name wins over another place's alias. These maps are small and keyed by place name, not by round or game, so caching them is allowed (root `CLAUDE.md`, "Memory"). A lazy holder or `@Cacheable` both work.

### 5. Service

Add to `GameService`:

```java
@Transactional
public Optional<RoundGuessesResponse> recordGuess(UUID gameId, int round, String guess, boolean correct)
```

Empty when the game or round doesn't exist (the controller makes it a 404). Otherwise, in this order:
1. The round must be `UNANSWERED`, or throw a new `RoundAlreadyAnsweredForGuessException` (or a better name). `ApiExceptionHandler` maps it to a **409** `ProblemDetail`.
2. `guess` is null, blank or longer than 255 characters: `IllegalArgumentException` (**400**).
3. `knownPlaceNames.resolve(round.getGameType().placeKind(), guess)` is empty: **400** ("not a known country/river/lake name"). Don't echo the guess into the log.
4. If a stored guess has the same `PlaceNames.normalise` value as the resolved spelling, change nothing and return the current list. This makes a retried request harmless, and it matches the client's duplicate rule (`typedBefore` in `guessReducer`).
5. `correct` is false and the round already has `guessLimit` wrong guesses, or `correct` is true and it already has `answerCount` correct ones: **400**.
6. Append `new RoundGuess(resolved, correct)`, `gameRepository.save(game)` and return the list.

The server doesn't check whether `correct` is true, because the client checks answers (root `CLAUDE.md`). It doesn't publish a stats event: stats are recorded once per answered round, as today.

`submitAnswer` doesn't change. Don't reject an answer whose `wrongGuesses` disagrees with the stored guesses: a guess POST that failed on the network would then block a legitimate answer. Add a one-line comment saying so.

`toResponse` (the `GET` round) adds the stored guesses.

### 6. API records and controller

- New `api/RecordedGuess(String text, boolean correct)` for the JSON (keep the JPA embeddable out of the API, as for the other records).
- New `api/GameRoundGuessRequest(String guess, Boolean correct)`. A missing `correct` is a 400.
- New `api/RoundGuessesResponse(List<RecordedGuess> guesses)`.
- Add `List<RecordedGuess> guesses` to `GameRound`, last. It is `[]` for a round with none, never null.
- `GameController`: `@PostMapping("/games/{gameId}/rounds/{round}/guesses")`. Parse the id with `parseGameId` (a malformed id is a 404) and retry once on `ObjectOptimisticLockingFailureException` exactly as `submitAnswerWithRetry` does. Pull that retry into one private helper that takes a `Supplier`, used by both endpoints, rather than copying it.

### 7. Contract

- `ApiContractIT`: add an endpoint shape `"POST /api/games/{id}/rounds/{round}/guesses"`. Make the round shapes include a non-empty `guesses` (post one known wrong guess before reading the round), so the element shape is recorded, not just an empty array.
- Regenerate: `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend`, and commit `client/src/test/api-shapes.json`.

## Frontend

Read `client/CLAUDE.md` first.

### 8. Types and fetching (`client/src/api/`)

- `types.ts`: `export interface RecordedGuess { text: string; correct: boolean }`. Add `guesses?: RecordedGuess[]` to `GameRoundResponse` ("Missing when talking to an older backend") and `guesses: RecordedGuess[]` to `GameRoundView`. Add `RoundGuessesResponse { guesses: RecordedGuess[] }`.
- `games.ts`:
  - `toRoundView`: `guesses: Array.isArray(response.guesses) ? response.guesses : []`.
  - New `useRecordGuess(gameId, round)`: a `useMutation` that POSTs `{ guess, correct }` to `roundUrl(gameId, round) + "/guesses"` with `postJson`. `onSuccess` patches `guesses` into the cached `queryKeys.round(gameId, round)` with `setQueryData`. This matters because round queries have `staleTime: Infinity`: without the patch, going to another round and back would remount `MapGame` from a cache with no guesses. `onError` invalidates the game, like `useSubmitAnswer`.
  - Give both `useRecordGuess` and `useSubmitAnswer` the same `scope: { id: "round-writes:" + gameId }`. Mutations in one scope run one after another (TanStack Query v5; the project has 5.107), so the answer that a guess led to always reaches the server after that guess.

### 9. Round model

In `games/common/Model.ts`, add to `Round`:
```ts
/** The guesses the server has recorded for this round, in order. */
guesses: RecordedGuess[];
/** Saves a guess that the guess reducer recorded, so a refresh restores it. */
recordGuess: (guess: RecordedGuess) => void;
```
`GameRoundPage` fills them from `data.guesses` and `useRecordGuess(gameId, currentRound)`. Putting them on `Round` means no game component changes: every game passes `round` to `MapGame` already.

`tsc` will then flag every place a test builds a `Round`. Add the two fields to the shared test fixture or helper in `client/src/test/` if there is one; otherwise add them where `tsc` points.

### 10. Rebuilding the state (`games/common/guessReducer.ts`)

Replace `initialGuessState(answerState, answers, hintsUsed)` with:

```ts
export function restoreGuessState(
  answerState: AnswerState,
  hintsUsed: number,
  saved: RecordedGuess[],
  config: GuessConfig,
): { state: GuessState; finished?: GuessOutcome["finished"] }
```

- Replay: start from the empty state and apply each `saved[i].text` with `guessReducer(state, { type: "guess", text }, configWithoutIsKnownGuess)`. Leave out `isKnownGuess`: the server only stored known names, and the place names may not have loaded yet at mount. The reducer recomputes each guess's answer with `matchAnswer`; trust that over the stored `correct`. Stop replaying once the state is `over`.
- `UNANSWERED`: return the replayed state, and its `finished` if the replay ended the round. That happens when a guess was saved but the answer POST after it was lost.
- `CORRECT`: as today (every answer found, `over`, the recorded `hintsUsed`), but put the replayed wrong guesses first so the reopened round shows them.
- `GAVE_UP` / `OUT_OF_GUESSES`: the replayed guesses (so the answers found before giving up show as found, not revealed) with `gaveUp: true`, `over: true`, `outOfGuesses` as today and the recorded `hintsUsed`.
- `foundAtHint` stays `hintsUsed` empty arrays for answered rounds and `[]` for unanswered ones, as today. Update the comments that say "The server doesn't keep the guesses".

### 11. `MapGame`

- Initial state: `useState(() => restoreGuessState(round.answerState, round.hintsUsed, round.guesses, config).state)`. Build the reducer config once, so `dispatch` and the restore use the same one.
- If the restore returned `finished` and `round.answerState` is `UNANSWERED`, submit that answer once on mount (a `useEffect` with a ref guard, so React's development double-run of effects doesn't send it twice). Report `wrongGuesses` with `wrongGuessCount`, as `dispatch` does.
- In `dispatch`: when `outcome.state.guesses.length > guessState.guesses.length`, call `round.recordGuess({ text: last.text, correct: last.answer !== null })` with the new last guess, **before** `submitAnswer`. The scope only orders mutations in the order they were started.

## Docs

- Root `CLAUDE.md`, "Game flow / API": add the guesses endpoint and the `guesses` field of `GameRound`. "Storage and privacy": guesses are stored, as known place names only.
- Backend `CLAUDE.md`, "API rules": the guess rules from step 5 (409 when answered, 400 for unknown, blank, too long or over the limit, a duplicate is a no-op). "Persistence": the `game_round_guess` table and its cascade.
- `client/CLAUDE.md`, "Answers": the server keeps the guesses; `MapGame` saves each recorded guess and replays them with `restoreGuessState`; hints are still not saved before the answer.
- `client/src/components/PrivacyPage.tsx`, "What we store": "A game is a random id plus the questions asked, the guesses made (only names from our lists) and how each round ended."

## Tests

Backend (`bash scripts/check-java.sh`, then `bash scripts/check-it.sh`):
- `GameServiceTest`: a wrong and a correct guess are stored in order with the list's spelling (send an alias in a different case and with accents); a duplicate is a no-op; an unknown name, a blank guess and a too-long guess are 400s; the `guessLimit`-th wrong guess is accepted and the next is a 400; more correct guesses than `answerCount` is a 400; a guess on an answered round is a 409; `getGameRound` returns the guesses; a river round accepts a river name and rejects a country name.
- `GameControllerTest`: the route, the 404 for a malformed or unknown id, a missing `correct` is a 400, and the retry after one optimistic-lock failure.
- `KnownPlaceNames` and `PlaceNames.normalise`: their own unit tests. Keep `AliasBuilder`'s tests passing.
- `GameRepositoryTest`: the guesses round-trip in order.
- `GameApiIT`: guess, read the round, answer, read again.
- `GameCleanupIT`: a game with guesses is deleted, and its guess rows go with it.
- Two guesses on one round at the same moment both end up stored (or one is a 409 duplicate). Follow how the existing concurrent-answer test does it, if there is one.
- `PostgresMigrationIT` runs the new migration on Postgres; make sure it passes.

Frontend (`bash scripts/check-frontend.sh`):
- `guessReducer.test.ts`: `restoreGuessState` for each answer state; the replay recomputes answers from `matchAnswer`; a replay that reaches the guess limit returns `finished: OUT_OF_GUESSES`; one that finds every answer returns `CORRECT`.
- `MapGame.test.tsx`: a wrong and a correct guess call `recordGuess`, while empty, duplicate and unknown guesses don't; the guess that ends the round is recorded before `submitAnswer` is called; a round mounted with saved guesses shows them, the right guesses-left count and a duplicate message for a repeat; a restored finished-but-unanswered round submits once.
- `games.test.tsx`: `useRecordGuess` posts the body and patches the cached round; a guess followed at once by an answer reaches the `fetch` mock in that order.
- `contract.test.ts`: update `src/test/fixtures.ts` until it passes against the new shapes.

Browser (`e2e/`, read `e2e/CLAUDE.md`): in `guessing.spec.ts`, make a wrong guess, reload the page, check "4 guesses left" and the wrong-guesses list, then guess the same name again and check it is rejected as already guessed without using a guess.

## Done when

- `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh`, `bash scripts/check-it.sh` and `bash scripts/check-no-skipped-tests.sh` pass, and the e2e suite passes.
- In the running app (`./mvnw clean install -DskipTests` then `java -jar target/geogames-0.0.1-SNAPSHOT.jar`), a refresh mid-round keeps the guesses and the guesses-left count, and a repeat guess after the refresh is rejected as a duplicate.
- The work is on a new branch from `main` (not `cities-labels-capitals`), pushed, with a PR. Never commit to `main`.

No heap measurement is needed: no data or loaders change, and a round holds at most `guessLimit + answerCount` short strings.
