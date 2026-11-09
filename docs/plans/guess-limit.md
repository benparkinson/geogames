# Plan: a limit on wrong guesses per round

Audience: the agent implementing this. Read `CLAUDE.md` first. Its conventions all apply: tests,
formatting, using the Node in `target/node`, no network in tests, and never committing to `main`.

## Prerequisite: build on `scoring-frontend`

The frontend half of hint scoring (branch `scoring-frontend`, commit `ee6d126`) is **not merged
into `main` yet**, and it rewrites most of the files this plan touches (`MapGame.tsx`,
`MapGameInput.tsx`, `RoundResult.tsx`, `guessReducer.ts`, `Model.ts`, `api/types.ts`,
`api/games.ts`, `GameTopBar.tsx`, `RoundProgress.tsx`, `GameSummary.tsx`, `test/fixtures.ts`).
Everything below describes the code **as it is on that branch**.

- If `scoring-frontend` has been merged into `origin/main` by the time you start, branch from
  `origin/main`.
- Otherwise, branch from `origin/scoring-frontend`, and say in the PR that it depends on that PR.

Use two branches and two PRs, as the scoring work did: `guess-limit-backend` (from `main`) and
`guess-limit-frontend` (from the base above). The backend must merge first. Write the frontend so
that it still works against a backend without the change (see step F1).

## Goal

- Each round allows only a limited number of **wrong guesses**.
- When the player makes the last allowed wrong guess, the round ends at once: every answer not
  found yet is shown as revealed, and the round scores **0 points**, exactly like giving up.
- While the round is in play, a clean, always-visible indicator shows how many guesses are left.

## Decisions (made while planning; don't reopen them)

1. **Only wrong guesses count.** A correct guess, a duplicate, an empty guess and an "unknown"
   guess (a name `isKnownGuess` rejects, for example a misspelled country) never use up a guess.
   The reducer already leaves duplicates, empty guesses and unknown guesses out of
   `state.guesses`, so "guesses used" is just `guesses.filter(g => g.answer === null).length`.
   *Why:* the owner hasn't decided whether rounds with more answers (Tripoint has 3, rivers have
   1) should allow more guesses. Counting only mistakes makes the limit mean the same thing
   whatever the answer count is ("you can be wrong N times"), so the question mostly goes away.
   If the owner later wants it to scale, step 2 still lets them set it per game type.
2. **The limit belongs to the game type and is set on the backend**, as `hintCount` is: a new
   `guessLimit` component on the `GameType` enum, sent to the client in `GameTypeResponse`. The
   owner tunes it in one place. Start every type at **5**:

   | GameType              | answerCount | hintCount | guessLimit |
   |-----------------------|-------------|-----------|------------|
   | `TRIPOINT`            | 3           | 3         | 5          |
   | `RIVERS_BY_SHAPE`     | 1           | 1         | 5          |
   | `RIVERS_ON_MAP`       | 1           | 2         | 5          |
   | `RIVERS_BY_ENDS`      | 1           | 2         | 5          |
   | `RIVERS_BY_COUNTRIES` | 1           | 2         | 5          |

   `guessLimit` must be at least 1. The enum constructor rejects anything less.
3. **Running out is a new `AnswerState`: `OUT_OF_GUESSES`.** It is not `GAVE_UP`, so the round
   result, the round dots and the summary can say "Out of guesses" rather than "Answer
   revealed", and a reopened round still says so. It scores 0 like `GAVE_UP`. Everything that
   treats `GAVE_UP` as "answers revealed" must treat `OUT_OF_GUESSES` the same way.
4. **The server trusts the client**, as it already does for correctness. The client doesn't send
   its guesses or a guess count, so the API request body doesn't change, and the server doesn't
   check that the limit was really reached.
5. **Scoring doesn't change.** `Scoring.roundPoints` / `roundPoints` already return 0 for any
   state other than `CORRECT`. Don't touch the formula or its shared test table. Add one row for
   `OUT_OF_GUESSES` to the test of each language.

## Backend (`guess-limit-backend`, from `main`)

### B1. `model/AnswerState`
Add `OUT_OF_GUESSES` after `GAVE_UP`, with a one-line Javadoc on each constant. The column is
`EnumType.STRING` and the database is in-memory H2, so no migration is needed.

### B2. `model/GameType`
- Add an `int guessLimit` as a third constructor argument, set from the table above, with an
  accessor `guessLimit()` whose Javadoc says: how many wrong guesses a round of this type
  allows; the round is lost (`OUT_OF_GUESSES`) when they are all used.
- In the constructor, throw `IllegalArgumentException` if `guessLimit < 1`.
- `model/GameTypeTest`: assert that every constant has `guessLimit() >= 1`.

### B3. `api/GameTypeResponse`
Add `guessLimit` as a record component (`{name, answerCount, hintCount, guessLimit}`), filled by
`from(...)`, and update the Javadoc example. Update `GameTypeResponseTest`.

### B4. `service/GameService`
- `submitAnswer` (around line 125) already rejects `UNANSWERED`. `OUT_OF_GUESSES` must be
  accepted like `GAVE_UP`: check that nothing else rejects it.
- `calculateGameResult` (around lines 203–237): `OUT_OF_GUESSES` is neither correct nor perfect,
  so make sure it isn't counted in `roundsCorrect` or `perfectRounds` and its points are 0. It
  should already come out that way. Add a test that proves it.
- Validating `hintsUsed` and the 409 / repeated-answer rules apply to `OUT_OF_GUESSES` without
  changes.

### B5. Tests
- `ScoringTest`: add `OUT_OF_GUESSES` → 0 (for example with hintCount 2, hintsUsed 1).
- `GameServiceTest`: answering `OUT_OF_GUESSES` stores it, scores 0, doesn't count as correct;
  sending it again is a no-op; following it with `CORRECT` is a 409.
- `GameControllerTest` / `GameApiIntegrationTest`: `gameType.guessLimit` is in the round JSON,
  and posting `{"answerState": "OUT_OF_GUESSES", "hintsUsed": 0}` returns 200.
- Run `mvn spotless:apply`, then `mvn test`.

### B6. `CLAUDE.md`
- Game flow / API: `GameTypeResponse` is `{name, answerCount, hintCount, guessLimit}`, and the
  answer states are `CORRECT` / `GAVE_UP` / `OUT_OF_GUESSES`.
- Game types: `GameType` has a `guessLimit` (the number of wrong guesses allowed, at least 1)
  next to `hintCount`. A new game type chooses both.
- Scoring: an `OUT_OF_GUESSES` round scores 0. Change "wrong guesses cost nothing" to say that
  they cost nothing until the limit is reached, which ends the round with 0.

## Frontend (`guess-limit-frontend`)

### F1. API types and normalisation
- `client/src/api/types.ts`: in `GameTypeModel`, add `guessLimit?: number` ("Missing when talking
  to an older backend").
- `client/src/api/games.ts` (line 21, where `hintCount` gets its default): give `guessLimit` a
  default of `Infinity` when it is missing, so an older backend means no limit.
  `GameRoundView.gameType` is `Required<GameTypeModel>`, so the type then holds a number.
- `client/src/games/common/Model.ts`: add `OUT_OF_GUESSES` to the `AnswerState` union, export
  `const OUT_OF_GUESSES = "OUT_OF_GUESSES" as const satisfies AnswerState`, and add
  `guessLimit: number` to `Round` with a comment ("Wrong guesses allowed this round, from the
  backend. Infinity means no limit.").
- `GameRoundPage.tsx`: set `guessLimit: data.gameType.guessLimit` when building `round`.
- Add a helper to `Model.ts` and use it everywhere `=== GAVE_UP` currently means "the answers
  were revealed":
  ```ts
  /** The round ended without being solved, so its answers are shown as revealed. */
  export function isRevealed(state: AnswerState): boolean {
    return state === GAVE_UP || state === OUT_OF_GUESSES;
  }
  ```

### F2. `guessReducer.ts` (pure; most of the logic lives here)
- `GuessState` gets `outOfGuesses: boolean`. Keep `gaveUp` meaning "the answers are shown as
  revealed", so it is **true in both lost cases**. That keeps `MapProps.gaveUp` and every map
  component as they are. `outOfGuesses` only tells the two cases apart for text.
- `GuessConfig` gets `guessLimit: number` (it may be `Infinity`).
- `GuessOutcome.finished` widens to `CORRECT_ANSWER | GAVE_UP | OUT_OF_GUESSES`.
- `GuessResult`'s `"wrong"` case gets `guessesLeft: number`, and a new case
  `{ kind: "outOfGuesses"; guess: string }` covers the wrong guess that used up the last one.
- Export `wrongGuessCount(state)` and `guessesLeft(state, guessLimit)`
  (`Math.max(0, guessLimit - wrongGuessCount(state))`) so the UI never counts guesses itself.
- In `applyGuess`, when `matched === null`: work out the new wrong count. If it is at least
  `config.guessLimit`, return state `{ ...state, guesses, gaveUp: true, outOfGuesses: true,
  over: true }`, result `{ kind: "outOfGuesses", guess }`, and `finished: OUT_OF_GUESSES`.
  Otherwise return the `"wrong"` result with `guessesLeft`.
- `initialGuessState`: add the `OUT_OF_GUESSES` case. It is like `GAVE_UP` (no guesses, since
  the server doesn't keep them; `gaveUp: true`, `over: true`) plus `outOfGuesses: true`.
  Set `outOfGuesses: false` everywhere else.
- `giveUp` sets `outOfGuesses: false`. Hints don't change.
- Tests in `guessReducer.test.ts`: with limit 3, two wrong guesses report 2 then 1 left and the
  round continues; the third ends the round with `OUT_OF_GUESSES` and keeps the correct guesses
  found so far; duplicates, unknown guesses, empty guesses and correct guesses don't use a guess;
  `Infinity` never ends the round; a guess after the round is over returns `"over"`;
  `initialGuessState("OUT_OF_GUESSES", ...)`.

### F3. `MapGame.tsx`
- Pass `guessLimit: round.guessLimit` in the reducer config.
- `submitAnswer` already forwards `outcome.finished` with `hintsUsed`, so `OUT_OF_GUESSES`
  reaches the server unchanged.
- Pass `guessesLeft`, `guessLimit` and `outOfGuesses` down to `MapGameInput` (and on to
  `RoundResult`), and `outOfGuesses` to `GameTopBar`.

### F4. The guesses-left indicator: new `client/src/games/common/GuessesLeft.tsx`
Props: `{ left: number; limit: number }`. It renders nothing when `limit` is `Infinity`.

Put it **on the same line as the "Wrong guesses" label**, above the crossed-out badges in
`MapGameInput`, so it is always visible while the round is played, even before the first wrong
guess. (While the round is being played, render that row whenever there is a limit, not only
when there are wrong guesses.) Layout, aligned left, wrapping on narrow phones:

```
[ Guess box ...................... ] [Guess]
[Hint (2 left)] [Reveal answer]  Worth 75 pts
● ● ● ○ ○   3 guesses left
Wrong guesses
[✕ France] [✕ Spain]
```

- One dot per allowed guess: `left` filled dots (`CircleFill`) then `limit - left` hollow ones
  (`Circle`) from `react-bootstrap-icons`, all `aria-hidden`.
- Text after the dots: `"N guesses left"`, `"1 guess left"` when one is left (in
  `text-warning-emphasis`, and the dots in the warning colour too), `text-muted` otherwise.
- The wrapper gets `role="status"` and an `aria-label` that repeats the text, so screen readers
  read the count and not the dots.
- If `limit > 10`, show only the text, without dots.
- Add any CSS to `client/src/styles.css`, using the existing Bootstrap variables so it works in
  dark mode. Keep it small.
- When the round is over, hide the indicator. The round result says what happened.

### F5. Feedback text (`MapGameInput.handleSubmit`)
- `"wrong"`: `"France" isn't right. 2 guesses left.` When one is left:
  `"France" isn't right. Last guess!` (Leave out the count when the limit is `Infinity`.)
- `"outOfGuesses"`: `"France" isn't right. You're out of guesses.` Clear the input and shake,
  as for a wrong guess.

### F6. `RoundResult.tsx`
- Take `outOfGuesses`. When it is true, show the heading
  `<XCircleFill /> Out of guesses · 0 pts` in `text-secondary`, instead of "Answer revealed".
- Work out `answerState` as `outOfGuesses ? OUT_OF_GUESSES : gaveUp ? GAVE_UP : CORRECT_ANSWER`
  before calling `roundPoints` / `isPerfect`.

### F7. Round dots, top bar, summary
- `GameTopBar.tsx`: the local override of the current round's state (line 18) must use
  `OUT_OF_GUESSES` when `outOfGuesses` is true.
- `RoundProgress.tsx`: treat `OUT_OF_GUESSES` like a revealed round (same class and icon), with
  the label `Round N: out of guesses`. Use `isRevealed` for the class and icon.
- `GameSummary.tsx` (line 46): count revealed rounds with `isRevealed`. If the summary shows a
  per-round state, label it "Out of guesses".
- `scoring.ts` doesn't change. Add an `OUT_OF_GUESSES` → 0 row to `scoring.test.ts`.

### F8. Tests and fixtures
- `client/src/test/fixtures.ts`: add `guessLimit: 5` wherever a `gameType` or `Round` is built,
  so existing tests still type-check.
- `GuessesLeft.test.tsx`: the text for 5, 1 and 0 left; nothing for `Infinity`; the aria label.
- `MapGameInput.test.tsx`: the indicator is shown before any guess; the feedback text from F5.
- `MapGame.test.tsx`: with `guessLimit: 2`, two wrong guesses end the round, call `submitAnswer`
  with `{ answerState: "OUT_OF_GUESSES", hintsUsed }`, reveal the remaining answers in
  `AnswerSlots`, and show "Out of guesses · 0 pts". A correct guess in between doesn't change the
  count.
- `RoundResult.test.tsx`, `RoundProgress.test.tsx`, `GameTopBar.test.tsx`,
  `GameSummary.test.tsx`: one `OUT_OF_GUESSES` case each.
- `api/games` test: a response without `guessLimit` gives `Infinity`.
- Run `npm run lint`, `npm test` and `npm run build` in `client/` (with `target/node`).

### F9. `CLAUDE.md`
- Frontend: `guessReducer` also enforces `guessLimit` (only wrong guesses count) and can finish
  a round as `OUT_OF_GUESSES`. `GuessState.gaveUp` means "the answers are revealed" and is true
  for both giving up and running out.

## Manual check
Run the backend and `npm run dev`, then play one Tripoint round and one river round:
- The indicator starts at 5.
- A misspelled country (an "unknown" guess) and a repeated guess don't change it.
- A wrong guess removes one dot.
- At one left, the indicator and the feedback turn to the warning style.
- The fifth wrong guess ends the round, reveals the answers on the map and in the slots, and
  shows 0 pts and a revealed-style dot in the top bar.
- Reopening that round from the summary still says "Out of guesses".
- Check a 375 px wide viewport for wrapping.

## Out of scope
- Different limits per round or by difficulty, and limits that scale with `answerCount`. If
  wanted later, change only the `GameType` table.
- Partial points for answers found before running out (Tripoint 2 of 3). The round scores 0.
- The server checking or storing individual guesses.
