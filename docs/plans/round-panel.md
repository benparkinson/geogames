# A tidier round panel, shorter distance clues, and "Hard mode"

## Goal

Three small changes and one layout change to the game panel (`games/common/MapGame` and `MapGameInput`):

1. The feedback after a wrong guess is too wordy. Make it short.
2. The wrong-guess chips are struck through, so the distance on them is hard to read. Make them easy to read.
3. Rename "Hardcore mode" to "Hard mode" everywhere the player sees it.
4. Lay out the panel so each piece of information sits in one clear place, instead of a long list of separate lines.

This is a frontend-only change. The API, the server, scoring and the stored data stay the same.

## What is wrong today

While a round is being played, the panel shows, from top to bottom:

```
Which country has these neighbours?              ← prompt
💡 Its capital is Vienna                          ← hint text (one line per hint)
[ ? ]                                            ← answer slots
[ Country name…                    ] [Guess]
[💡 Hint (1 left)] [👁 Reveal answer]  Worth 75 pts
"France" isn't right. The answer is 1,240 km to the north-east. 4 guesses left.
●●●●○ 4 guesses left
Wrong guesses
~~✗ France · 1,240 km ↗~~                        ← small, grey, struck through
Hardcore [Off|On]
```

The problems:

- **The guesses left appear twice**: in the feedback sentence and in the dots line right under it.
- **The round's value ("Worth 75 pts") is at the end of the button row**, where it reads like a third button and moves when the reveal button changes to "Tap again to reveal".
- **The distance appears twice in different forms**: a long sentence in the feedback and a struck-through badge on the chip.
- **The status of the round (guesses left, value) is split** around the feedback line rather than read together.
- **The Hardcore switch is a setting**, but it sits in the flow of the game like another piece of round information.

## Options considered

- **Move the round status (guesses left, value) into the top bar.** Rejected: the top bar is about the whole game (rounds, total score) and is already full on a phone.
- **Put the guesses left inside the input box, or on the Guess button.** Rejected: clever but cramped at phone width, and the input already has a placeholder and autocomplete.
- **Make Hard mode an icon toggle beside the theme toggle in the top bar.** Rejected: an icon alone doesn't say what it does, and the top bar is the same for every game, including the ones without clues.
- **Drop the feedback line for wrong guesses and let the new chip speak for itself.** Rejected: screen readers need the `aria-live` line, and unknown or duplicate guesses make no chip.
- **Chosen: group the panel into zones**, each with one job, and say each fact once. See the next section.

## Decisions

These are settled. Don't revisit them while implementing.

### 1. The panel layout

While the round is being played:

```
Which country has these neighbours?              ┐ the question
│ 💡 Its capital is Vienna                        │  (prompt, hints taken, answer slots)
[ ? ]                                            ┘

●●●●○ 4 guesses left                Worth 75 pts  ← round status: one row
[ Country name…                    ] [Guess]      ┐ answering
Not France · 1,240 km north-east                  │  (feedback directly under the input)
[💡 Hint (1 left)] [👁 Reveal answer]              ┘

WRONG GUESSES                                     ┐ what you've tried
(✗ France  1,240 km ↗) (✗ Spain  2,100 km ↗)      ┘
───────────────────────────────────────────────
Hard mode [Off|On]  No distance clues             ← setting, only in games with clues
```

- **The question** (`MapGame`): the prompt, then the hints taken, then the answer slots, as today. The hint lines become a list (`<ul className="round-hints">`, no bullets) with a 3px left border in `--gg-color-hint` and the text in `--gg-color-text` (not muted), so they read as part of the question rather than as fine print. No other change to the slots.
- **The round status**: one row, directly above the input. On the left, `GuessesLeft` (dots and "4 guesses left"); on the right, "Worth 75 pts". Use `d-flex justify-content-between align-items-center`. When the round has no guess limit, `GuessesLeft` renders nothing and the value still sits on the right (use `ms-auto` on it). Put the row in a new component, `games/common/RoundStatus.tsx`, which takes `guessesLeft`, `guessLimit`, `points` and renders the two parts. Remove the `mb-1` from `GuessesLeft`'s root, so the row controls the spacing.
- **Answering**: the form, then the feedback line (`.guess-feedback`, `aria-live="polite"`) directly under it, then the Hint and Reveal buttons. The "Worth" text leaves the button row.
- **What you've tried**: the wrong-guess chips, as today, with the new chip style (decision 3). Keep the "Wrong guesses" label.
- **The setting**: the compact Hard mode switch moves to the bottom of the panel, below a hairline (`border-top: 1px solid var(--gg-color-border)`, a little padding above), in a `.round-settings` row. The compact form shows "Hard mode", the switch and, in small muted text, "No distance clues". It still shows only when the game has clues (`clueFor` given) and the round isn't over.
- **When the round is over**: the question stays (the slots show the answers), and `RoundResult` takes the place of the status row, the form, the feedback line and the buttons, as it does today for the form and buttons. The feedback line is hidden once the round is over: `RoundResult` already says "Correct!" or "Out of guesses" and is itself `aria-live`. The wrong-guess chips stay. The setting row is hidden (as today).

On a phone the panel is at most half the screen and scrolls, so the setting row at the bottom may be off screen. That is fine: it is a setting, and the home page and daily page also have the switch.

### 2. Shorter feedback

Only the wrong-guess sentences change. The guesses left are no longer in the sentence, because the status row says it (and `GuessesLeft` is a `role="status"`, so a screen reader announces the change).

| Result | Today | New |
|---|---|---|
| wrong, clue, one answer | `"France" isn't right. The answer is 1,240 km to the north-east. 4 guesses left.` | `Not France · 1,240 km north-east` |
| wrong, clue, several answers | `"France" isn't right. The nearest country you still need is 480 km to the north. Last guess!` | `Not France · nearest 480 km north` |
| wrong, no clue or Hard mode | `"Spain" isn't right. 2 guesses left.` | `Not Spain.` |
| wrong, no guess limit | `"Spain" isn't right. Try again.` | `Not Spain.` |
| out of guesses | `"Spain" isn't right. You're out of guesses.` | hidden (the round is over, see decision 1) |
| empty, duplicate, unknown, correct | unchanged | unchanged |

- The guess is shown as `result.guess`, without quotes.
- `clueSentence` becomes `clueText(clue, answerCount)`: `` `${formatKm(clue.km)} km ${compassPoint(clue.bearing)}` ``, with `nearest ` in front when `answerCount > 1`. `answerNoun` is no longer needed by it (it is still used by the "unknown" sentence).
- **"Last guess!"** moves to the status row: when one guess is left, `GuessesLeft` shows the text `Last guess!` instead of `1 guess left`, in the warning colour it already uses. Keep its `aria-label` as `1 guess left`, so the tests that find the status by name (`e2e/tests/full-game.spec.ts`) still pass.

### 3. Readable wrong-guess chips

- **No strike-through.** The ✗ icon already says the guess was wrong. Remove the `strike` prop from `ui/Chip` and the `.gg-chip--strike` rule (`MapGameInput` is the only user), and update `ui/Chip.test.tsx`.
- **Normal size.** Drop `size="sm"` on the wrong-guess chips, so they are the same size as the answer slots above. Keep `size="sm"` in `Chip` for other uses.
- **Two parts.** A wrong-guess chip with a clue reads `✗ France  1,240 km ↗`:
  - the ✗ icon in `--gg-color-danger`;
  - the name in `--gg-color-text` (the neutral chip uses the muted colour; override it for these chips);
  - a gap, then the distance and arrow in `fw-semibold`. Replace the ` · ` separator with a left margin (`ms-2`) on the distance part.
  - Keep the hidden text with the compass point (`gg-visually-hidden`, `1,240 km east`) for screen readers.
- Add a class for this (`.guess-chip`, with `.guess-chip__x` for the icon) in `styles.css`, using tokens only (`scripts/check-theme.mjs` rejects colour literals).
- Without a clue (Hard mode, or a game without clues) the chip is just `✗ France`.

### 4. Hard mode

- **Everything the player sees** says "Hard mode": the switch label (home page, daily page and the game panel), its `title`, the privacy page, the site footer, and the share text of the daily challenge (`GeoGauntlet Daily #42 · Hard mode`).
- **Rename the code** to match, so the names don't drift from the UI:
  - `settings/hardcoreMode.ts` → `settings/hardMode.ts`, `useHardcoreMode` → `useHardMode` (and its test file).
  - `components/HardcoreToggle.tsx` → `components/HardModeToggle.tsx`, `HardcoreToggle` → `HardModeToggle`.
  - Local variables and comments (`hardcore` → `hardMode`) in `MapGame`, `MapGameInput`, `DailyPage`, `GameSummary`, `registry.ts`, `types.ts`.
- **Keep the stored names.** The `localStorage` key stays `gg-hardcore`, and the `gg-daily` entry keeps its `hardcore` field, so players keep their setting and today's daily entry stays valid (`dailyStorage.ts` rejects an entry without a boolean `hardcore`). Add a one-line comment at each: "Stored name kept from when this was called Hardcore mode." The privacy page still lists the key `gg-hardcore`; describe it as "Hard mode".
- The full switch (home and daily pages) keeps its description: "No distance or direction clues after a wrong guess."

## Steps

1. **Rename to Hard mode** (decision 4). Use `git mv` for the two files. Run `bash scripts/check-frontend.sh` and fix the tests that find the switch by name (`"Hardcore mode"` → `"Hard mode"`) or check the share text.
2. **Feedback** (decision 2) in `MapGameInput.feedbackText`, and "Last guess!" in `GuessesLeft`.
3. **Chips** (decision 3).
4. **Layout** (decision 1): `RoundStatus`, the new order in `MapGameInput`, the hint list and the setting row in `MapGame`, and the CSS (`.round-hints`, `.round-settings`, `.guess-chip`) in `client/src/styles.css`.
5. **Docs**: `client/CLAUDE.md` (the `gg-daily` paragraph and the **Distance clues** paragraph name `hardcoreMode.ts`, `useHardcoreMode` and `HardcoreToggle`), `e2e/README.md` (the `guessing.spec.ts` row).

## Tests

Update the text the tests expect; don't weaken what they check.

- **`MapGameInput.test.tsx`**:
  - The wrong-guess sentences: `Not Spain.`, `Not France · 1,240 km north-east`, `Not France · nearest 480 km north`, and `Not France.` when clues are off.
  - "says when it was the next to last guess" now checks the status row shows `Last guess!`, and the feedback is `Not Spain.`.
  - The chip: no `gg-chip--strike` class; the distance is still there and the hidden direction text still has `gg-visually-hidden`.
  - New: the status row shows the guesses left and the value together, and the value is no longer inside the button row.
  - New: once the round is over, the feedback line is empty.
- **`GuessesLeft.test.tsx`**: one left shows `Last guess!` with the warning class, and the status is still named `1 guess left`.
- **`MapGame.test.tsx`**: the clue tests (`"MapGame clues"`) expect the new text; switching Hard mode on in the panel still hides the clue in the feedback and on the chip. New: the switch is after the wrong guesses in the panel (compare with `compareDocumentPosition`), and the hint text is in a list.
- **Game tests** that expect the old sentences: `countries/neighbours.test.tsx`, `lakes/LakeGame.test.tsx`, `rivers/RiverGame.test.tsx`, `tripoint/TripointGame.test.tsx`. Find them with `grep -rn "isn't right" client/src e2e/tests`.
- **`ui/Chip.test.tsx`**: drop the `strike` case.
- **Hard mode**: `settings/hardMode.test.tsx` (renamed), `HomePage.test.tsx`, `DailyPage.test.tsx`, `shareText.test.ts` (`· Hard mode`), `PrivacyPage.test.tsx` (still finds `gg-hardcore`).
- **Browser tests** (`e2e/tests/guessing.spec.ts`): the clue regex becomes `/^Not .+ · [\d,]+ km (north|south|east|west)/`; the Hard mode test finds the group `"Hard mode"` and expects `Not ${wrong}.`; the other wrong-guess texts become `Not ${wrong}.`. `full-game.spec.ts` should need no change (it finds the status by its `aria-label`); check it.

Run `bash scripts/check-frontend.sh` until it is clean, then `bash scripts/check-no-skipped-tests.sh`. The browser tests run in CI (`e2e`); if Playwright is set up locally, run `guessing.spec.ts` and `full-game.spec.ts`.

## Check by eye

Run the app (`./mvnw spring-boot:run` and `npm run dev` in `client/`, or the jar) and play Country Gaps and Border Shapes at phone width (about 375px) and on a desktop, in light and dark mode:

- the status row stays on one line at 375px, including with 10 dots;
- the chips wrap neatly, and a long name with a distance is still readable;
- the hint list, the status row and the setting row line up with the panel's padding;
- with Hard mode on, the chips show only names and the feedback is `Not France.`;
- after the round ends, `RoundResult` sits where the input was, with nothing left over from the status row or the feedback.

## Out of scope

- Marking Hard mode on the results page (it needs the server to record it; see `guess-distance.md`, Follow-ups).
- Changing the "unknown" and "duplicate" sentences.
- Clues for rivers, lakes or Tripoint.
