# Plan: points per round, with hints costing points

Audience: the agent implementing this. Read `CLAUDE.md` first. Its conventions all apply: tests,
formatting, using the Node in `target/node`, no network in tests, and never committing to `main`.

## Goal

At the moment a round is either right (1 point) or revealed (0). Change this to points:

- A round answered correctly **without hints** is **perfect** and scores the most: **100 points**.
- Each hint lowers what the round is worth. Hints can take at most **50 points** off a round,
  shared equally between that game type's hints. A round answered after **all** of its hints scores
  50, whether the game type has 1 hint or 3.
- A revealed round (`GAVE_UP`) scores **0**, however many hints were used.
- A game's maximum score is `100 × rounds`. A game where every round is perfect is a
  **perfect game**.
- Every game type, including future ones, scores this way with no code of its own. A new game
  type only chooses how many hints it has.

Wrong guesses cost nothing. This is deliberate and out of scope.

### The formula (both languages must agree exactly)

```
MAX_ROUND_POINTS   = 100
MAX_HINT_PENALTY   = 50

points(answerState, hintsUsed, hintCount):
  if answerState != CORRECT: return 0
  if hintCount == 0:         return 100
  return 100 - round_half_up(50 * hintsUsed / hintCount)
```

Use integer arithmetic so Java and TypeScript can't differ: `penalty = (100 * hintsUsed + hintCount) / (2 * hintCount)`
with integer division. This equals `round_half_up(50 * hintsUsed / hintCount)` for non-negative inputs.

Test vectors. Put **this exact table** in both the Java and the TypeScript test:

| hintCount | hintsUsed | CORRECT | GAVE_UP |
|-----------|-----------|---------|---------|
| 0         | 0         | 100     | 0       |
| 1         | 0         | 100     | 0       |
| 1         | 1         | 50      | 0       |
| 2         | 1         | 75      | 0       |
| 2         | 2         | 50      | 0       |
| 3         | 1         | 83      | 0       |
| 3         | 2         | 67      | 0       |
| 3         | 3         | 50      | 0       |

## Facts established while planning (don't re-derive)

- **The client decides whether an answer is correct** (`guessReducer.ts`) and only sends
  `{answerState}`. The server never hears about hints. `GameService.calculateGameResult` sets
  `score` to the number of `CORRECT` rounds.
- **Hint counts today live only in the frontend**, and they differ by game:

  | GameType              | Hints now | Where they're defined                                                        |
  |-----------------------|-----------|------------------------------------------------------------------------------|
  | `TRIPOINT`            | 3         | `tripoint/TripointGame.tsx`, `total: tripoint.countries.length` (always 3), map only |
  | `RIVERS_BY_SHAPE`     | 1         | `rivers/shapes.ts`, `textHints(river, continentHint)`                         |
  | `RIVERS_ON_MAP`       | 2         | `rivers/onMap.ts`, flows through, then first letter                           |
  | `RIVERS_BY_ENDS`      | 2         | `rivers/ends.ts`, flows through, then first letter                            |
  | `RIVERS_BY_COUNTRIES` | 2         | `rivers/countries.ts`, written by hand: end flags (map), then first letter    |

- **All 50 processed rivers have at least one country**, so `flowsThroughHint` never returns null
  for real data. It still returns null for an empty list, and that branch can stay.
- `guessReducer`'s `hint` action already counts `hintsUsed` and caps it at `hintsTotal`.
  `initialGuessState` sets `hintsUsed: 0` when a round is reopened, because the server doesn't keep it.
- `MapGame` keeps `hintTexts` in its own state, so reopening an answered round shows no hint texts.
- `api/games.ts` already copes with an older backend: `roundAnswerStates` is optional and has a
  fallback. Keep the new fields **additive** in the same way, since the static frontend and the
  Render backend can be deployed at different times.
- `ApiExceptionHandler` maps `IllegalArgumentException` to 400 and `RoundAlreadyAnsweredException`
  to 409.
- The H2 database is in memory and the schema is generated, so a new column needs no migration.

## Design decisions

1. **The server owns the hint count.** Add `hintCount` to the `GameType` enum, next to
   `answerCount`, and send it in `GameTypeResponse`. The frontend reads it from there, so the
   server's scoring and the client's hint button can't disagree. A game module supplies the
   hints' *content*, not how many there are.
2. **The client reports `hintsUsed` with the answer**: `{answerState, hintsUsed}`. The server
   validates `0 ≤ hintsUsed ≤ hintCount`, stores it on `GameRoundEntity`, and works out the points.
   Points are computed when needed, never stored, so changing the formula changes old rounds too.
   That's fine, because games are in memory.
3. **The formula exists in both languages.** The server computes the authoritative score. The
   client computes the same thing to show "worth 83 points" while the round is played and the
   result straight after answering, before the refetch. The shared table above keeps them in step.
4. **Additive API.** Keep `score` as the field name, but it now means points. Add new fields and
   don't rename old ones. An old client that sends no `hintsUsed` is treated as 0.

## Delivery

`main` is protected. Use one branch and PR per phase, in order, each green on `mvn clean install`
(which runs the Java and Vitest suites) before you open it. Run `mvn spotless:apply` and `npm run
lint` (in `client/`) before each commit.

---

## Phase 1: backend scoring (branch `scoring-backend`)

### 1.1 `GameType.hintCount`

`model/GameType.java`: add a second constructor argument, `hintCount`, with an accessor whose
Javadoc says: "How many hints the client offers for a round of this type. Using all of them costs
`Scoring.MAX_HINT_PENALTY` points." Use today's counts so nothing changes for players yet:
`TRIPOINT(3, 3)`, `RIVERS_BY_SHAPE(1, 1)`, `RIVERS_ON_MAP(1, 2)`, `RIVERS_BY_ENDS(1, 2)`,
`RIVERS_BY_COUNTRIES(1, 2)`.

`GameTypeTest`: pin every type's `hintCount` (a `@ParameterizedTest` / `@CsvSource` table), and
assert that every type has `hintCount >= 0`.

### 1.2 `model/Scoring.java`

A `final` class with a private constructor:

```java
public static final int MAX_ROUND_POINTS = 100;
public static final int MAX_HINT_PENALTY = 50;

/** The points a round earns: 100 if perfect, less for each hint, 0 unless correct. */
public static int roundPoints(AnswerState answerState, int hintsUsed, int hintCount)
```

Use the integer formula above. Throw `IllegalArgumentException` if `hintsUsed < 0`,
`hintCount < 0` or `hintsUsed > hintCount`. `ScoringTest` holds the shared table as a
`@CsvSource`, plus the three exception cases.

### 1.3 Store `hintsUsed`

`dao/GameRoundEntity`: add `private int hintsUsed;` (0 in the constructor) with a getter and setter.

### 1.4 Accept `hintsUsed` with the answer

- `api/GameRoundAnswerRequest`: `record GameRoundAnswerRequest(AnswerState answerState, Integer hintsUsed)`.
  It's `Integer` so a missing field is `null`, and the controller passes `hintsUsed == null ? 0 : hintsUsed`.
  Put that in a small accessor on the record, `int hintsUsedOrZero()`, so the controller stays one line.
- `GameService.submitAnswer(long gameId, int round, AnswerState answerState, int hintsUsed)`:
  - Reject `UNANSWERED` as now.
  - Validate `hintsUsed` against `round.getGameType().hintCount()`. Throw
    `IllegalArgumentException("A round of %s has %d hints, so %d can't have been used")`.
    Validate *after* finding the round (it needs the type), but before `recordAnswer`.
  - `recordAnswer`: an identical repeat (same state **and** same `hintsUsed`) is still a no-op.
    Anything else on an answered round is a conflict. Extend `RoundAlreadyAnsweredException` to say
    what it was and what was sent, including hints. Set both fields when recording.

### 1.5 Send the points back

- `api/GameTypeResponse`: add `int hintCount`. Update `from` and the class Javadoc example.
- `api/GameRound`: add `int hintsUsed` and `int points` (this round's points) after `answerState`.
- `api/GameResult`: keep `gameState`, `roundsUnanswered`, `score` and `roundAnswerStates`. `score`
  is now the **sum of round points**. Add, in this order after them:
  - `int maxScore`: `MAX_ROUND_POINTS × rounds`.
  - `int roundsCorrect`: the old meaning of `score`.
  - `int perfectRounds`: `CORRECT` rounds with `hintsUsed == 0`.
  - `List<Integer> roundPoints`: ordered by round number, like `roundAnswerStates`.
  - `List<Integer> roundHintsUsed`: ordered the same way.
- `GameService.calculateGameResult` builds all of these from one sorted pass over the rounds.
  Don't sort three times. `toResponse` fills `GameRound.hintsUsed` and `points`.

### 1.6 Tests

- `GameServiceTest`:
  - Rename `scoreCountsOnlyCorrectAnswers` to `scoreIsTheSumOfRoundPoints`, using a mix of perfect,
    hinted and revealed rounds, and check `score`, `maxScore`, `roundsCorrect`, `perfectRounds`,
    `roundPoints` and `roundHintsUsed`.
  - Add `submitAnswerStoresHintsUsed`, `submitAnswerRejectsMoreHintsThanTheTypeHas` (400 and the
    round left alone), `submitAnswerRejectsNegativeHints`,
    `sameAnswerWithDifferentHintsIsAConflict` and `revealedRoundScoresZeroWhateverTheHints`.
  - Update the existing `submitAnswer` calls to pass `hintsUsed` (0 unless the test is about hints).
- `GameControllerTest`: an answer body without `hintsUsed` is treated as 0. A body with
  `hintsUsed: 9` gets 400 with a `ProblemDetail`. The round JSON has `hintsUsed`, `points`,
  `gameType.hintCount` and the new `gameResult` fields.
- `GameTypeResponseTest` and `GameApiIntegrationTest`: update them for the new fields. In the
  integration test, play one round with a hint and assert the points end to end.

### 1.7 Docs

`CLAUDE.md`, under "Game flow / API": the answer body is `{answerState, hintsUsed}`, and the round
and result carry points. Under "Game types": the enum has `answerCount` **and `hintCount`**. Add a
short **Scoring** paragraph: 100 per round, hints share 50, revealed is 0, formula in `model/Scoring`
and `games/common/scoring.ts`, which must agree (shared test table).

---

## Phase 2: frontend scoring (branch `scoring-frontend`)

### 2.1 Types and API

- `api/types.ts`:
  - `GameTypeModel` gets `hintCount?: number`. It's optional because of older backends.
  - `GameRoundResponse` gets `hintsUsed?: number` and `points?: number`.
  - `GameResultResponse` gets optional `maxScore`, `roundsCorrect`, `perfectRounds`, `roundPoints`
    and `roundHintsUsed`.
  - `GameRoundView` makes all of them **required**, with `gameType.hintCount` included.
- `api/games.ts` `toRoundView`: fill the gaps for an older backend the same way `roundAnswerStates`
  does now. In that case `score` counted correct rounds, so the fallbacks are: `hintCount`: 0,
  `hintsUsed`: 0, `points`: `roundPoints(answerState, 0, 0)`, `roundPoints`: derived from
  `roundAnswerStates` with 0 hints, `roundHintsUsed`: all 0, `maxScore`: `100 × totalRoundCount`,
  `score`: the sum of the derived `roundPoints` (**not** the old `score`, which is a count), and
  `perfectRounds` / `roundsCorrect`: counted from the derived states. Test both paths in
  `api/games.test.tsx`.
- `useSubmitAnswer`: the mutation takes a `RoundAnswer` (`{ answerState, hintsUsed }`, a new type in
  `games/common/Model.ts`) and posts it as the body.

### 2.2 `games/common/scoring.ts`

```ts
export const MAX_ROUND_POINTS = 100;
export const MAX_HINT_PENALTY = 50;
export function roundPoints(answerState: AnswerState, hintsUsed: number, hintCount: number): number;
export function isPerfect(answerState: AnswerState, hintsUsed: number): boolean;
```

This is the same integer formula. `scoring.test.ts` holds the shared table (`it.each`). Keep the
module free of React.

### 2.3 Thread `hintCount` and `hintsUsed` through

- `games/common/Model.ts` `Round`: add `hintCount`, `hintsUsed` (the server's value for this round),
  `points`, `maxScore`, `roundPoints: number[]` and `perfectRounds`. Update `makeRoundProps` in
  `test/fixtures.ts` (and `makeRound`, which gets `hintCount: 3` for `TRIPOINT` and `1` for
  `RIVERS_BY_SHAPE`, plus the new result fields).
- `GameRoundPage.renderGame`: fill them from the view. `submitAnswer` takes a `RoundAnswer`.
- `games/types.ts` `GameComponentProps.submitAnswer` takes a `RoundAnswer`. Add a JSDoc line to
  `GameModule` saying that the number of hints comes from the backend `GameType.hintCount`, and that
  the component must offer exactly that many.

### 2.4 `MapGame` uses the server's hint count

- `HintConfig` **loses `total`**. It becomes `{ description: string; text?: (index) => string | null }`.
  `MapGame` reads the count from `round.hintCount`. Removing `total` makes TypeScript flag every
  game that set its own count, so fix each one it reports. A game with `hintCount > 0` must pass
  `hints`. If it doesn't, `MapGame` shows no hint button and logs `console.error` in development.
- `textHints(river, ...builders)` in `riverHints.ts` still returns a `HintConfig`, without `total`.
  Its description still uses `builders.length`. Add a test to `riverHints.test.ts` that the
  description counts the builders.
- `guessReducer`: `GuessConfig.hintsTotal` is now fed from `round.hintCount`. `initialGuessState`
  takes `hintsUsed` (from `round.hintsUsed`) and uses it for answered rounds instead of 0.
- **Hint texts are derived, not stored.** Delete the `hintTexts` state. Compute
  `Array.from({ length: hintsUsed }, (_, i) => hints?.text?.(i)).filter(Boolean)`. Then a reopened
  round shows the hints that were used, and the tripoint map shows its hinted outlines from
  `hintsUsed` as before.
- When the reducer says `finished`, call `submitAnswer({ answerState: outcome.finished, hintsUsed: outcome.state.hintsUsed })`.

### 2.5 Show the points

- **While playing** (`MapGameInput`): next to the hint button, in small muted text, show
  `Worth {roundPoints(CORRECT, hintsUsed, hintCount)} pts`. The hint button's title is the existing
  description plus `(−N pts)`, where N is the drop the *next* hint causes. Work it out as the
  difference of two `roundPoints` calls, not as `50 / hintCount`, so the rounding matches. When
  `hintCount` is 0, show only `Worth 100 pts`.
- **Round over** (`RoundResult`, which gets `hintsUsed` and `hintCount`):
  - perfect: `<StarFill/> Perfect! +100`
  - correct with hints: `<CheckCircleFill/> Correct! +83`, then a muted line, `1 hint used`
    or `2 hints used`
  - revealed: `<EyeFill/> Answer revealed · 0 pts`

  Use local state for these, so they appear without waiting for the refetch.
- **Top bar** (`GameTopBar`): `Score {score}` while playing, then `Final score {score} / {maxScore}`.
  As it does for the dots, reflect the current round straight away. If
  `gameOver && round.answerState === UNANSWERED` (the round ended here, and the server's copy hasn't
  been refetched yet), show `round.score + localPoints`. Otherwise show `round.score`. `GameTopBar`
  therefore takes `hintsUsed` as well as `gameOver` and `gaveUp`.
- **Progress dots** (`RoundProgress`): take `points: number[]` alongside `states`. A perfect round
  (`CORRECT` with 100) shows `StarFill` and the class `round-dot--perfect`, a correct round with
  hints shows the check as now, and a revealed round shows the eye. `describe` says "perfect",
  "correct, 83 points" or "revealed". Add the `.round-dot--perfect` style next to the existing
  dot styles (find them with Grep `round-dot--correct` in `client/styles`), in the same colour
  family as correct but filled or golden. It must be readable in both themes.
- **Summary** (`GameSummary`): use `gameResult` from any round's view, since they all carry the
  whole result. Fetch the rounds as now.
  - Heading: `You scored {score} / {maxScore}`. If `perfectRounds === totalRoundCount`, it's
    `Perfect game! {score} / {maxScore}` with a star.
  - Subline: `{perfect} perfect · {hinted} with hints · {revealed} revealed`. Leave out any part
    that is 0, but always show at least one.
  - Each row on the right: the icon as in the dots, plus `+{points}`. Under the answer, in muted
    text, `2 hints` when hints were used.

### 2.6 Tests

- `scoring.test.ts`: the shared table.
- `guessReducer.test.ts`: `initialGuessState` restores `hintsUsed` for answered rounds, and the
  `hint` action is capped at `hintsTotal`.
- `MapGame.test.tsx`: finishing after one hint calls `submitAnswer` with `{CORRECT, 1}`. The
  "Worth" text goes 100 → 83 → 67 → 50 for a 3-hint game (tripoint fixture) and 100 → 50 for a
  1-hint game. A reopened answered round with `hintsUsed: 2` shows two hint texts and the hint
  button isn't there.
- `RoundResult.test.tsx`, `GameTopBar.test.tsx`, `RoundProgress.test.tsx` and
  `GameSummary.test.tsx`: cover perfect, hinted and revealed, plus "Perfect game!". Update the
  existing `X / totalRoundCount` assertions to points.
- Each game's test (`TripointGame`, `RiverGame`, `onMap`, `ends`, `countries`): with the fixture's
  `hintCount`, every hint index `< hintCount` gives a non-null text for text-hint games. This catches
  a module offering fewer hints than the server counts.
- `api/games.test.tsx`: the posted body includes `hintsUsed`. Test the older-backend fallbacks.

### 2.7 Docs

`CLAUDE.md`: in step 4 of "Adding a game type", the component provides `hints` content for
`round.hintCount` hints, and doesn't set its own count. Mention `games/common/scoring.ts`.

---

## Phase 3: more hints for River Shapes (branch `river-shape-hints`)

The shape game is the hardest and has only one hint. Give it three, with the weakest first:

1. `continentHint`: "Continent: Europe"
2. `flowsThroughHint`: "Flows through: Germany, Austria, …"
3. `firstLetterHint`: "Starts with “D”"

Changes:
- `GameType.RIVERS_BY_SHAPE(1, 3)`, and update `GameTypeTest`.
- `rivers/shapes.ts`: `textHints(river, continentHint, flowsThroughHint, firstLetterHint)`.
- `flowsThroughHint` lists the countries in the order stored, which is the order along the river.
  For a river through many countries (the Danube has 9) that gives the answer away more than the
  first letter does. Cap the list in this hint at 3 names and add "and N more" (for example
  "Flows through: Germany, Austria, Slovakia and 6 more"). This also affects On the Map and Ends,
  and that's intended. Test it in `riverHints.test.ts`.
- Update `test/fixtures.ts` if a test pins the shape hint count, and the `RiverGame` /
  shapes tests.

The other games keep their counts. On the Map and Ends already have 2 text hints. In Tripoint each
hint shows one of its 3 countries. River Countries already shows the countries, so a continent or
"flows through" hint would add nothing.

**To change a game's hints in future**, edit its `GameType` `hintCount` and its module's hints in
the same PR. Scoring needs no other change.

---

## Out of scope (note in the PR descriptions, don't build)

- **Hints taken before a page refresh are forgotten.** They live only in client state until the
  answer is sent. The fix would be to record each hint on the server
  (`POST …/rounds/{round}/hints`). Since the client is already trusted with correctness, it's
  not worth it now.
- Penalties for wrong guesses, time bonuses or streaks.
- A high-score table.
