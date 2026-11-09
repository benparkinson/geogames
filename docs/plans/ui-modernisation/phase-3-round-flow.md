# Phase 3: round flow and end-of-game summary

Read `README.md` in this folder first. This phase assumes phases 1 and 2 are done. It changes the backend a little and the frontend a lot.

## Why

- After a round is solved, nothing tells the player what to do next. They have to notice that the small "Next round" button has become enabled.
- The game never ends. On the last round the label changes to "Final score" and that's it: no summary, and no way to play again.
- The Previous/Next buttons only allow stepping one round at a time, and they don't show which rounds were solved.

## Target flow

1. Guess until the round is solved, or reveal the answer.
2. The input area becomes a **result panel** with one big primary button: **Next round →**, or **See results** on the last round. The button has focus, so pressing Enter continues.
3. The top bar shows **progress dots**, one per round, marked correct, revealed or unplayed. Clicking a finished round revisits it.
4. After the last round comes the **summary view**: the score, a list of each round's answer, and **Play again** and **Home** buttons.

## Backend steps

### 1. Per-round states in `GameResult`

The dots need every round's `AnswerState`. Today `GameRound` only carries the current one.

- `model/GameResult.java` becomes:
  `public record GameResult(GameState gameState, int roundsUnanswered, int score, List<AnswerState> roundAnswerStates) {}`
- In `GameService.calculateGameResult`, set `roundAnswerStates` to the rounds sorted by `GameRoundEntity::getIndex`, mapped to `getAnswerState()`. When the game is missing, use an empty list.
- Make `calculateGameResult` package-private (`static GameResult calculateGameResult(Optional<GameEntity> game)`) so it can be tested.

### 2. Fix the stale result returned by `submitAnswer`

`GameService.submitAnswer` computes `gameResult` *before* applying the answer, so its response is out of date. Move the `calculateGameResult(game)` call inside the lambda, after `r.setAnswerState(answerState)` and the save. The frontend refetches anyway, but the response should be correct.

### 3. Test

Add `src/test/java/com/parkinson/benjamin/geogames/service/GameServiceTest.java`, a plain JUnit 5 test without Spring.
- Build a `GameEntity` from three `GameRoundEntity`s, constructed with `new GameRoundEntity(index, GameType.TRIPOINT, "{}")`. Add them **out of index order**, and set their states to CORRECT, GAVE_UP and UNANSWERED. Check `GameRoundEntity` for the default `answerState` and the available setters.
- Assert that `calculateGameResult(Optional.of(game))`:
  - returns `roundAnswerStates` in index order;
  - has `score == 1` and `roundsUnanswered == 1`;
  - has `gameState == IN_PROGRESS`.
- Add a second case where every round is answered, and assert `FINISHED`.

Run `mvn spotless:apply` and `mvn test`.

## Frontend steps

### 4. Shared API helpers

Create `client/src/api/games.ts`:

```ts
export function roundUrl(gameId: string, round: number): string  // "/api/games/{id}/rounds/{round}" (same string as today's query key)
export async function fetchRound(gameId: string, round: number): Promise<GameRoundModel> // throws on !response.ok
export function useCreateGame(): { createGame: (gameType: string, numberOfRounds: number) => void; isLoading: boolean; error: unknown }
// useCreateGame: useMutation POST /api/games; throws on !response.ok; onSuccess router.push("/games/" + id)
```

- Switch `GameRoundPage` to `roundUrl` and `fetchRound`. **Keep the query key exactly `roundUrl(gameId, n)`**, because the summary reuses the cache.
- Switch `HomePage` to `useCreateGame`. Keep its UI as it is; phase 4 redesigns it.
- Add `roundAnswerStates: string[]` to `GameResultModel` in `Model.tsx`.

### 5. Remount on a new game

In `pages/games/[gameId].tsx`, render `<GameRoundPage key={gameId as string} gameId={gameId} />`. Otherwise **Play again** navigates to the new game id but keeps the old `currentRound` and summary state.

### 6. Answer-name helper

Create `client/src/games/common/answers.ts`:

```ts
export function answerNames(round: GameRoundModel): string[]   // TRIPOINT → parsed.countries.map(c => c.name); RIVERS_BY_SHAPE → [parsed.name]
export function gameTypeLabel(gameTypeName: string): string     // "Tripoint" | "River"
```

Use `answerNames` wherever the code builds the list of correct answers from parsed round data.

### 7. Result panel

Create `client/src/games/common/RoundResult.tsx`. While `gameOver`, `MapGameInput` renders it **instead of** the form and the Hint/Reveal buttons. Keep showing the wrong-guess chips below it.

- **Solved:** a `<CheckCircleFill/>` icon with "Correct!", in `text-success fw-semibold`.
- **Revealed:** an `<EyeFill/>` icon with "Answer revealed", in `text-secondary fw-semibold`. The slots already show the names.
- **Primary button:** large and full width, with `autoFocus`.
  - If `round.hasNextRound`: the text is `Next round <ArrowRight/>` and it calls `round.nextRound`.
  - Otherwise: the text is `See results` and it calls a new `onShowSummary` prop, threaded from `GameRoundPage` through the game components and `MapGame` into `MapGameInput`.

  Don't wait for `gameCompleted` from the server before showing "See results". Rounds must be played in order (step 8), so the last round being finished means the game is finished.
- **Input focus.** When a new round mounts, focus the guess input automatically, but only on devices with a precise pointer. Opening the keyboard on a phone would cover the map.
  `autoFocus={typeof window !== "undefined" && window.matchMedia("(pointer: fine)").matches}`.

### 8. Progress dots

Create `client/src/games/common/RoundProgress.tsx`. It replaces the Previous/Next buttons in `GameTopBar`; delete those buttons and `Round.prevRound` if nothing else uses it.

- Render `<nav aria-label="Rounds">` containing one `<button>` per entry in `roundAnswerStates`.
- **Dot state, shown by icon as well as colour:**
  - Unanswered: an outline circle holding the round number.
  - `CORRECT`: a success-filled circle with `<CheckLg/>`.
  - `GAVE_UP`: a secondary-filled circle with `<EyeFill/>`.
  - The current round gets a thicker ring and `aria-current="step"`.
- **Labels:** `aria-label` and `title` read `Round 3: correct`, `Round 3: revealed`, `Round 3: not played` or `Round 3: current`.
- **Which dots can be clicked:**
  - `canVisit(i) = i === currentRound || states[i] !== "UNANSWERED" || i === firstUnansweredIndex`.
  - Other dots are disabled. Wrap each one in a `<span title="Finish round N first">`.
  - Clicking a dot calls a new `goToRound(i)` on `Round`, which `GameRoundPage` implements with `setRound(i)` and `setShowSummary(false)`.
- **Live state.** The current round's dot must update as soon as it's solved, without waiting for the refetch. Pass the local `gameOver` / `gaveUp` state from `MapGame` to `GameTopBar` and override `states[currentRound]` with it.
- **Size:** dots are 28px from 576px up and 20px below, with a 4px gap. The `nav` has `overflow-x: auto`, so 10 rounds fit at 360px with a scroll fallback.
- **Top bar contents:**
  - Below 576px: Home icon · dots · `Score N`.
  - From 576px up: Home + "Geogames" · `Round n of N` + dots · `Score N`.
  - If the game is complete, add a small `outline-primary` **Results** button that opens the summary. That covers someone who reloads a finished game.

### 9. Summary view

Add `const [showSummary, setShowSummary] = useState(false)` to `GameRoundPage`. When it's true, render `<GameSummary gameId totalRoundCount onOpenRound onPlayAgain />` instead of the game. It's a view inside the same page; **don't add a route**, for the static-export reason given in the README.

Create `client/src/games/common/GameSummary.tsx`:

- **Data.** Fetch every round with react-query v3 `useQueries`, using `queryKey: roundUrl(gameId, i)` and `queryFn: () => fetchRound(gameId, i)`. Played rounds are usually already cached. Show a spinner until all of them have loaded.
- **Layout.** A centred card (`max-width: 560px`, `mx-auto`, `my-4`, inside a `min-vh-100` background `var(--gg-bg)` wrapper) with the same `GameTopBar` Home link at the top.
- **Heading.** `You scored {correct} / {total}`. Under it, a muted line: `{correct} correct · {revealed} revealed`.
- **List.** One row per round, each a `<button>` styled as a list-group item:
  - Contents: round number, `gameTypeLabel`, `answerNames(round).join(", ")`, and an outcome icon plus word (`✓ Correct` / `👁 Revealed`, using the bootstrap icons).
  - Clicking a row calls `onOpenRound(i)`, which sets the round and closes the summary.
- **Buttons:**
  - **Play again** (primary): calls `useCreateGame().createGame(type, total)`. `type` is the shared `gameType.name` if every round has the same type, otherwise `"RANDOM"`. It shows a spinner while loading.
  - **Home** (outline-secondary): `<Link href="/">`.

## Manual checks

- Play a 3-round Tripoint game using only the keyboard: type guesses with Enter, then press Enter on "Next round" and on "See results".
- The dots update immediately when a round is solved or revealed.
- Future dots are disabled and show the "Finish round N first" tooltip.
- Clicking a past dot shows that round with its answers, and its result panel offers "Next round".
- The summary:
  - shows the correct score and answers;
  - opens a round when you click its row;
  - starts a fresh game at round 1 with **Play again**, with no summary showing.
- Reload a finished game: the **Results** button appears in the top bar.
- Play a 10-round Mixed game at 360px: the dots fit, or scroll, without breaking the layout.

## Done when

- [ ] `GameResult.roundAnswerStates` exists and is ordered by round. The `submitAnswer` response is up to date. `GameServiceTest` passes. `mvn spotless:apply` and `mvn test` are clean.
- [ ] `api/games.ts` provides `roundUrl`, `fetchRound` and `useCreateGame`, and `HomePage` and `GameRoundPage` use them.
- [ ] `RoundResult`, `RoundProgress` and `GameSummary` exist. The Previous/Next buttons are gone.
- [ ] `GameRoundPage` is keyed by `gameId`.
- [ ] `npm run lint` and `npm run build` pass.
