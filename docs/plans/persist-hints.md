# Persist hints across a page refresh

## Goal

Today the hints taken in a round reach the server only with the answer (`hintsUsed` in `POST .../answers`). Refreshing the page in the middle of a round resets the hints to 0. A player can take every hint, refresh, and still score 100. After this change:

- A refresh shows the same hints, and the map shows them the same way (Tripoint highlights the same countries).
- A hint taken before the refresh still costs its points.
- An answer can't report fewer hints than the server saw taken.

**Prerequisite:** `docs/plans/persist-guesses.md` must be merged first. This plan reuses its pieces: the mutation `scope`, `restoreGuessState` and its replay, `Round.guesses`, the shared retry helper in `GameController`, and the "409 on an answered round" exception. Read that plan and the code it produced before starting.

## Why the order of hints and guesses matters

`GuessState.foundAtHint[i]` lists the answers found at the moment hint `i` was taken. Tripoint uses it (`games/tripoint/model.ts`, `hintedCountries`): a hint reveals a country the player hadn't found yet when the hint was taken. A count of hints isn't enough to rebuild that. The server records each hint with **how many guesses had been recorded when it was taken** (`guessesBefore`). On restore, the client replays guesses and hints interleaved in that order, so `foundAtHint` comes out exactly as it was.

## Flow

1. The player takes hint `i` (0-based). `guessReducer` applies it as today.
2. `MapGame` calls `round.recordHint({ hint: i, guessesBefore })`, where `guessesBefore` is `guessState.guesses.length` at that moment. This POSTs `/api/games/{id}/rounds/{round}/hints`, in the same mutation scope as guesses and answers, so the server sees everything in the order the player did it.
3. `GET` round returns `hintsTakenAt: number[]` (entry `i` is hint `i`'s `guessesBefore`).
4. On mount, `restoreGuessState` replays guesses and hints interleaved.

## Backend

### 1. Migration

Add the next free `V<n>__round_hints.sql`. It is V7 if the guesses plan took V6; check the folder.

```sql
-- The hints taken in a round before it was answered, so a refreshed page can restore them.
-- guesses_before is how many guesses the round had when the hint was taken, which decides what
-- some hints show (Tripoint reveals a country not found yet).
create table game_round_hint (
    round_id       bigint not null references game_round_entity (id) on delete cascade,
    hint_index     int    not null,
    guesses_before int    not null,
    primary key (round_id, hint_index)
);
```

The `on delete cascade` is required for `GameCleanupService`, as in the guesses plan.

### 2. Entity

In `GameRoundEntity`, add an ordered element collection of `Integer`:

```java
@ElementCollection
@CollectionTable(name = "game_round_hint", joinColumns = @JoinColumn(name = "round_id"))
@OrderColumn(name = "hint_index")
@Column(name = "guesses_before", nullable = false)
private List<Integer> hintsTakenAt = new ArrayList<>();
```

Add a getter that returns an unmodifiable view, and an `addHint(int guessesBefore)` method. Like the guesses, a change increments the round's `@Version`.

The existing `hints_used` column keeps its meaning: the hints counted in the score, set when the round is answered. While a round is unanswered, the hints taken are the size of `hintsTakenAt`. Don't write `hintsUsed` before the answer. The round's points and the `GameResult` would then still be correct, but other code would see hints for an unanswered round, so keep it simple.

### 3. Service: `recordHint`

```java
@Transactional
public Optional<RoundHintsResponse> recordHint(UUID gameId, int round, int hint, int guessesBefore)
```

Empty for an unknown game or round (a 404). Otherwise:
1. If the round is answered, it is a **409**, with the same exception the guesses plan added.
2. If `hint < hintsTakenAt.size()`, the hint is already recorded. Change nothing and return the current state, so a retried request is harmless. Don't check `guessesBefore` against the stored value: the first one wins.
3. If `hint != hintsTakenAt.size()` (a skipped hint), it is a **400**.
4. If `hint >= gameType.hintCount()`, it is a **400**.
5. If `guessesBefore` is negative or larger than the number of stored guesses, it is a **400**. A guess whose POST failed can make the client's count larger than the server's, and that would turn a real hint into a 400, so clamp instead: store `min(guessesBefore, guesses.size())`, and reject only a negative value. Say so in a comment.
6. Otherwise call `addHint(...)`, then `gameRepository.save(game)`.

The method returns `RoundHintsResponse(List<Integer> hintsTakenAt)`. Like guesses, recording a hint doesn't publish a stats event.

### 4. Answers: hints can't go down

In `submitAnswer` / `recordAnswer`, the hints that count become `max(reported hintsUsed, hintsTakenAt.size())`.
- A client that reports fewer hints than it took (a cheat, or an old tab) is charged for the hints the server saw.
- More than recorded is accepted, since a hint POST may have been lost. Validation against `hintCount` is unchanged.
- Compute the effective value **before** the "identical answer is a no-op / different answer is 409" comparison, so a retry of the same request is still a no-op.
- The stats event, the stored `hintsUsed` and the response all use the effective value. Add a test where the client reports 0 hints after two recorded hints. Its points must equal `Scoring.roundPoints(CORRECT, 2, hintCount)`; compute that in the assertion rather than writing a literal.

Document this in the backend `CLAUDE.md` "API rules": a missing `hintsUsed` still means 0, but the stored hints are the larger of that and the hints recorded with `POST .../hints`.

### 5. API and controller

- `api/GameRoundHintRequest(Integer hint, Integer guessesBefore)`: either missing is a 400.
- `api/RoundHintsResponse(List<Integer> hintsTakenAt)`.
- Add `List<Integer> hintsTakenAt` to `GameRound` (always present, `[]` when none). Keep `hintsUsed` as it is: 0 until answered.
- `GameController`: `@PostMapping("/games/{gameId}/rounds/{round}/hints")`, using `parseGameId` and the shared optimistic-lock retry helper.

### 6. Contract

In `ApiContractIT`, add `"POST /api/games/{id}/rounds/{round}/hints"`. Record a hint before reading the round shapes so `hintsTakenAt` isn't empty. Then regenerate with `bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend` and commit `client/src/test/api-shapes.json`.

## Frontend

### 7. API (`client/src/api/`)

- `types.ts`: add `hintsTakenAt?: number[]` to `GameRoundResponse` (missing on an older backend) and `hintsTakenAt: number[]` to `GameRoundView`. Add `RoundHintsResponse`.
- `games.ts`:
  - `toRoundView` defaults `hintsTakenAt` to `[]`.
  - New `useRecordHint(gameId, round)`, in the **same** `scope` as `useRecordGuess` and `useSubmitAnswer`. `onSuccess` patches `hintsTakenAt` into the cached round (round queries never go stale, so without the patch, leaving the round and coming back would lose the hints). `onError` invalidates the game, as the others do.

### 8. Round model

Add to `Round` in `games/common/Model.ts`:

```ts
/** For each hint the server recorded before the answer: how many guesses had been made when it was taken. */
hintsTakenAt: number[];
/** Saves a hint as soon as it is taken, so a refresh keeps it. */
recordHint: (hint: { hint: number; guessesBefore: number }) => void;
```

`GameRoundPage` fills them. Update the test fixtures that build a `Round`, as in the guesses plan.

### 9. Restore (`guessReducer.ts`)

Give `restoreGuessState` a `hintsTakenAt: number[]` parameter, and change the replay for an **unanswered** round to interleave the two lists:

```
for each guess index g from 0 to saved.length:
  apply every hint whose guessesBefore === g (in hint order), with { type: "hint" }
  if g < saved.length, apply guess g
```

Apply hints to the config's `hintsTotal` as `dispatch` does, so a stale extra entry can't go over the limit. A hint whose `guessesBefore` is beyond the last guess is applied at the end, which the loop already does when `g === saved.length`.

Answered rounds don't change: `hintsUsed` comes from the server, and `foundAtHint` is still empty arrays. Use `hintsTakenAt` there too if it is cheap, so a reopened Tripoint round shows the same countries. It is optional.

### 10. `MapGame`

- Pass `round.hintsTakenAt` into the restore.
- In `takeHint` (or in `dispatch` for a `"hint"` action that changed `hintsUsed`), call `round.recordHint({ hint: guessState.hintsUsed, guessesBefore: guessState.guesses.length })` using the state **before** the hint. That index is the hint just taken.

### 11. Points shown during play

`GameTopBar` computes the live points from the local `hintsUsed`, so it is already right after a restore. Check that `RoundProgress` and `GameSummary` read only answered rounds' hints. The server doesn't change `hintsUsed` or `roundHintsUsed` before the answer, so they shouldn't need anything.

## Docs

- Root `CLAUDE.md`, "Game flow / API": the hints endpoint and `hintsTakenAt`. "Scoring": the client reports `hintsUsed`, but the server charges at least the hints it recorded.
- Backend `CLAUDE.md`, "API rules": the rules from steps 3 and 4. "Persistence": the `game_round_hint` table.
- `client/CLAUDE.md`, "Answers": hints are saved as they are taken, with `guessesBefore`, and replayed interleaved with the guesses. Remove "hints are still not saved before the answer", which the guesses plan added.
- `PrivacyPage.tsx`, "What we store": add "the hints taken".

## Tests

Backend:
- `GameServiceTest`:
  - Hints are recorded in order.
  - A repeated hint is a no-op, and a skipped one is a 400. So is one past `hintCount`, and a negative `guessesBefore`.
  - A `guessesBefore` larger than the stored guess count is clamped.
  - A hint on an answered round is a 409.
  - `getGameRound` returns `hintsTakenAt`.
  - An answer reporting fewer hints than recorded is stored, scored and published with the recorded count. A repeat of that same request is a no-op, not a 409.
- `GameControllerTest`: the route, the 404s, the 400 for a missing field, and the retry.
- `GameRepositoryTest`: the hints round-trip in order.
- `GameCleanupIT`: the hint rows are deleted with the game.
- `GameApiIT`: take a hint, read the round, then answer reporting 0 hints and see the points of one hint.
- `RoundStatsIT`: the stats row has the effective hint count.

Frontend:
- `guessReducer.test.ts`: the interleaved replay. Take a Tripoint-like round with three answers: guess one, take a hint, guess another, take a hint. The restored `foundAtHint` should equal the state the original dispatches produced. Also cover a hint taken after the last guess, and extra entries capped at `hintsTotal`.
- `MapGame.test.tsx`: taking a hint calls `recordHint` with the index and the guess count before it. A round mounted with `hintsTakenAt` shows the hint texts and the reduced live points. A hint, a guess and the answer all go through in that order.
- `games.test.tsx`: `useRecordHint` posts and patches the cache, and it is ordered with the guess and answer mutations.
- `contract.test.ts`: update `src/test/fixtures.ts`.

Browser: in `e2e/tests/`, take a hint, reload, and check that the hint text is still shown and the hint button shows one used. Then answer correctly and check the round shows the points of one hint, not 100.

## Done when

- `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh`, `bash scripts/check-it.sh`, `bash scripts/check-no-skipped-tests.sh` and the e2e suite pass.
- In the running jar, a refresh after a hint keeps the hint and its cost. In Tripoint, a refresh after "guess, hint, guess, hint" highlights the same countries as before.
- The work is on its own branch from `main` (after the guesses PR has merged), pushed, with a PR.

No heap measurement is needed: a round holds at most `hintCount` integers.
