# Phase 1: quick wins

Read `README.md` in this folder first. This phase changes only the frontend (`client/`).

## Why

A new player can't tell what the controls do:

- 🌍 submits the guess.
- 💡 reveals a country outline in Tripoint, but in Rivers it does nothing (`displayMapHint` is undefined there).
- "?" opens a popover holding Give up, Game explanation and Show clue.
- The Rivers continent clue is a second hint system, hidden in that popover.
- Rivers says "Guess a country...".
- Empty and duplicate guesses are silently ignored.

Two bugs also hurt play:

- `CenterUpdater` calls `map.setView` on **every render**, so each guess snaps the map back and undoes the player's zoom.
- `GameRoundPage` shows a full-page spinner whenever `isFetching` is true, so submitting an answer unmounts and remounts the whole game.

## Current code you'll touch

- `client/src/games/common/MapGame.tsx`: the generic game. Its props include `explanation`, `clues` and `displayMapHint`. It holds the guess state, the explanation modal, the clue modal, and the `clearInput` / `enrichRoundFunctions` hack that resets state when the round changes.
- `client/src/games/common/MapGameInput.tsx`: the input row (GuessBox, 🌍, 💡, ⏮️, ⏭️) and the list of past guesses.
- `client/src/games/common/HelpButton.tsx`: the "?" popover. **Delete it.**
- `client/src/components/GuessBox.tsx`: a text input with its own Enter-key handler.
- `client/src/games/Tripoint.tsx`, `client/src/games/RiverShapes.tsx`: these pass props into `MapGame`.
- `client/src/games/common/GameRoundPage.tsx`: fetches the round, renders the game. It also defines `CORRECT_ANSWER`, `GAVE_UP` and `UNANSWERED`, which `MapGame` imports (a circular import).
- `client/src/map/CenterUpdater.tsx`
- `client/src/helper/stringHelper.ts`

## Steps

### 1. Add the icon library

From `client/`, run `npm install react-bootstrap-icons@1.11.4`. Import icons individually, e.g. `import { Lightbulb } from "react-bootstrap-icons";`.

### 2. Move the answer-state constants

Move `CORRECT_ANSWER`, `GAVE_UP` and `UNANSWERED` from `GameRoundPage.tsx` to `games/common/Model.tsx`, and update the imports. This removes the `MapGame` → `GameRoundPage` import.

### 3. More forgiving answer matching

In `helper/stringHelper.ts`, make `normaliseString` remove diacritics and anything that isn't a letter or digit:

```ts
export function normaliseString(input: string): string {
  return input
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}
```

With this, "cote d'ivoire" matches "Côte d'Ivoire" and "guinea bissau" matches "Guinea-Bissau". Change the parameter and return types from `String` to `string`, and fix any callers that the type change breaks.

### 4. Reset state per round by keying, not by hack

- In `GameRoundPage.renderGame`, pass `key={currentRound}` to `<Tripoint>` and `<RiverShapes>`. Each round then gets fresh component state, including Tripoint's hint state.
- In `MapGame`, delete `clearInput` and `enrichRoundFunctions`, along with the `if (round) enrichRoundFunctions()` call.
- Replace the render-time `if (gameOver && attemptedGuesses.length === 0) setCorrectAnswers(...)` with a lazy initial state:
  `useState(() => answerState !== UNANSWERED ? correctAnswersFunction(data).map(a => new AnswerModel(a, true)) : [])`.

### 5. Stop the spinner flashing after each answer

In `GameRoundPage`:
- Read `isLoading` from `useQuery` and render the spinner only when `isLoading`, not when `isFetching`.
  - Moving to a new round changes the query key, so `isLoading` is true there and the spinner still shows.
  - The refetch after an answer only sets `isFetching`, so the game stays mounted.
- In `renderTitle`, base the "Loading..." title on `!data`, not `isFetching`.

### 6. Fix the map snapping back on every guess

Rewrite `map/CenterUpdater.tsx` so it moves the map only when the centre actually changes:

```tsx
export function CenterUpdater({ center, zoom = 4 }: CenterUpdaterProps): null {
  const map = useMap();
  const [lat, lng] = center;
  useEffect(() => {
    map.setView([lat, lng], zoom);
  }, [map, lat, lng, zoom]);
  return null;
}
```

Add `zoom?: number` to `CenterUpdaterProps`. Keep the existing callers' behaviour (zoom 4). Phase 2 replaces this for rivers.

### 7. Replace `explanation`, `clues` and `displayMapHint` with `prompt`, `inputPlaceholder` and `hints`

Change `MapGameProps`:

```ts
export interface HintConfig {
  total: number;        // how many hints this round offers
  description: string;  // shown as the Hint button's tooltip/title
  use: (hintIndex: number) => string | null; // perform hint #hintIndex; return text to show, or null if the hint is shown on the map
}
// MapGameProps: remove explanation, clues, displayMapHint. Add:
prompt: string;            // one-line instruction shown above the input
inputPlaceholder: string;
hints?: HintConfig;
```

Wire it up in the two games:

- **Tripoint**
  - `prompt`: `` `Name the ${answerCount} countries that meet at the flag.` ``
  - `inputPlaceholder`: `"Country name…"`
  - `hints`:
    - `total`: `tripoint.countries.length`
    - `description`: `"Shows the outline of one of the countries"`
    - `use`: calls the existing `showRandomCountryBorder()`, then returns `null`.
- **RiverShapes**
  - `prompt`: `"Name this river from its shape."`
  - `inputPlaceholder`: `"River name…"`
  - `hints`:
    - `total`: `1`
    - `description`: `"Shows which continent the river is in"`
    - `use`: returns `` `Continent: ${river.continent}` ``.

Then:
- Delete both `explanation` constants.
- In `MapGame`, add `const [hintsUsed, setHintsUsed] = useState(0)` and `const [hintTexts, setHintTexts] = useState<string[]>([])`.
- `useHint()` does nothing if `gameOver` or if `hintsUsed >= hints.total`. Otherwise it calls `hints.use(hintsUsed)`, appends any returned text to `hintTexts`, and increments `hintsUsed`.
- Hints are free, so don't touch the score.
- Delete the explanation modal, the clue modal, their state, and `HelpButton.tsx`.

### 8. Make `submitGuess` report what happened

Change `MapGame.submitGuess` to return a result instead of a boolean:

```ts
export type GuessResult =
  | { kind: "empty" }
  | { kind: "duplicate"; guess: string }
  | { kind: "wrong"; guess: string }
  | { kind: "correct"; remaining: number } // remaining correct answers still needed; 0 = round solved
  | { kind: "over" };                      // round already finished
```

`MapGame` passes `submitGuess`, `useHint`, `hintsUsed`, `hints`, `giveUp` and `inputPlaceholder` to `MapGameInput`. It renders `prompt` and the `hintTexts`, each with a `<Lightbulb/>` icon in a small muted line, above the input.

### 9. Rebuild `MapGameInput`

Lay out the input area like this:

```
Name the 3 countries that meet at the flag.
💡 Continent: Europe                                  (only once a text hint is used)
[ Country name…                        ] [ Guess ]
[💡 Hint (2 left)]   [👁 Reveal answer]      [‹ Previous] [Next round ›]
"Spain" isn't right. Try again.                       (aria-live feedback line)
Spain ✗   France ✓                                     (existing guess list)
```

- **Form.** Wrap the input and the Guess button in `<form onSubmit={...}>`, with `e.preventDefault()`, so Enter submits natively.
  - The **Guess** button is `type="submit"` with `variant="primary"` and the text "Guess".
  - Remove the `onKeyDown` handler and the unused `correct` prop from `GuessBox`.
  - Add `aria-label` to the input, set to the placeholder text.
- **Feedback.** Hold `feedback: string | null` in `useState`, and show it in `<div className="guess-feedback small" aria-live="polite">`. Map each `GuessResult` to a message:

  | Result | Message | Clear the input? |
  |---|---|---|
  | `empty` | Type a guess first. | n/a |
  | `duplicate` | You've already guessed {guess}. | no |
  | `wrong` | "{guess}" isn't right. Try again. | yes |
  | `correct`, remaining > 0 | Correct! {remaining} to go. | yes |
  | `correct`, remaining = 0 | Correct! | yes |
  | `over` | no message | n/a |

- **Shake on a wrong guess.** Add a `shake` class to the input for 400ms. Re-trigger it by toggling a counter in `key` or a state flag. Add the CSS to `styles.css`:

  ```css
  @keyframes shake { 0%,100% { transform: translateX(0) } 25% { transform: translateX(-6px) } 75% { transform: translateX(6px) } }
  .shake { animation: shake 0.3s; }
  @media (prefers-reduced-motion: reduce) { .shake { animation: none; } }
  ```

- **Hint button.** Only render it when `hints` is set.
  - Look: `variant="outline-warning"`, a `<Lightbulb/>` icon, and the text `Hint (N left)`.
  - Disable it when `gameOver` or when no hints remain.
  - Set `title` to `hints.description`. Wrap the button in a `<span title=…>` so the tooltip still shows while it is disabled. When none remain, change the title to "No hints left".
- **Reveal answer button.** This replaces "Give up". Look: `variant="outline-danger"`, an `<EyeFill/>` icon, and the text "Reveal answer".
  - It needs two taps. The first tap changes the text to "Tap again to reveal" for 3 seconds (`setTimeout`, cleared on unmount). The second tap within that window calls `giveUp`.
  - Disable it when `gameOver`.
- **Round buttons.** Replace ⏮️ and ⏭️ with `variant="outline-secondary"` buttons: `<ChevronLeft/> Previous` and `Next round <ChevronRight/>`.
  - Below 576px, hide the text with `d-none d-sm-inline` around it, and keep the `aria-label`.
  - Keep the existing enabled/disabled logic. While Next is disabled because the round isn't answered, its wrapper `<span>` gets `title="Answer or reveal this round first"`.
- **Layout.** Use Bootstrap utilities (`d-flex flex-wrap gap-2`) so the row wraps on narrow screens instead of overflowing. Phase 2 redoes the layout properly, so don't over-invest here.

## Out of scope for this phase

The page layout and CSS sizing (phase 2), the answer chips (phase 2), round progress, the result panel and the summary (phase 3), and the home page (phase 4).

## Manual checks

- Tripoint:
  - A wrong guess shows the message and shakes the input.
  - A duplicate guess shows the "already guessed" message and keeps your text.
  - Hint reveals an outline, and the count goes down to 0 and then disables.
  - Zoom in, then make a guess: the map stays where you zoomed it.
- Rivers:
  - The placeholder says "River name…".
  - Hint shows the continent line, then disables.
- Reveal answer needs two taps, then shows the answers.
- After answering, the page doesn't flash a spinner.
- Next round loads with an empty input, no feedback and fresh hints.
- Going Previous to a solved round shows its answers.
- At 360px wide, the controls wrap and there's no horizontal scroll (the map container may still be too wide until phase 2; note it if so).
- Every button has visible text or a tooltip.

## Done when

- [ ] `HelpButton.tsx`, the explanation modal and the clue modal are gone. Nothing references `explanation`, `clues` or `displayMapHint`.
- [ ] No emoji-only buttons remain in `MapGameInput`.
- [ ] Rivers has a working hint, and both games have correct placeholders and prompts.
- [ ] `submitGuess` returns `GuessResult`, and every case shows the right feedback in an `aria-live` region.
- [ ] `CenterUpdater` only moves the map when the centre changes.
- [ ] The spinner is driven by `isLoading`. The games are keyed by round, and `clearInput` / `enrichRoundFunctions` are deleted.
- [ ] `npm run lint` and `npm run build` pass.
