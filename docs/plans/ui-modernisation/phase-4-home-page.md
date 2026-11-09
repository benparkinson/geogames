# Phase 4: home page

Read `README.md` in this folder first. This phase assumes phases 1–3 are done (it uses `useCreateGame` from `client/src/api/games.ts`). It changes only the frontend.

## Why

`client/src/components/HomePage.tsx` currently has these problems:
- It shows "Welcome to Geogames (working title)" and one "New game" button.
- That button opens a modal with two dropdowns, and the dropdowns give no hint of what each game is.
- It only offers 1–5 rounds, although the API allows 1–10.
- It has no error handling. The backend is on Render's free tier and can take tens of seconds to wake up, but the page shows only a tiny spinner on the "Go!" button.

## Target

Everything sits on the page itself, with no modal:

```
            Geogames
  Quick geography puzzles on a real map.

  Choose a game
  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
  │ 🚩 Tripoints   │ │ 〰 Rivers      │ │ 🔀 Mixed       │
  │ Name the three│ │ Name a river  │ │ A random mix  │
  │ countries that│ │ from its shape│ │ of both       │
  │ meet at a     │ │ alone         │ │               │
  │ point         │ │               │ │               │
  └───────────────┘ └───────────────┘ └───────────────┘
  Rounds   [ 3 ][ 5 ][ 10 ]

           [   Start game   ]
     (status / error line here)
```

At 360px the cards stack in one column. From 576px up they sit in a three-column row.

## Steps

### 1. Move the game-type definitions

Create `client/src/games/gameTypes.ts`. Move `GameType` and `GameRequest` out of `HomePage.tsx`; drop `GameRequest` if `useCreateGame` makes it redundant. Define:

```ts
export interface GameOption { name: "TRIPOINT" | "RIVERS_BY_SHAPE" | "RANDOM"; title: string; description: string; Icon: Icon }
export const GAME_OPTIONS: GameOption[] = [
  { name: "TRIPOINT",        title: "Tripoints", description: "Name the three countries that meet at a point.", Icon: FlagFill },
  { name: "RIVERS_BY_SHAPE", title: "Rivers",    description: "Name a river from its shape alone.",             Icon: Water },
  { name: "RANDOM",          title: "Mixed",     description: "A random mix of both.",                          Icon: Shuffle },
];
export const ROUND_OPTIONS = [3, 5, 10];
```

The icons come from `react-bootstrap-icons`, and `Icon` is its exported type.

If `answers.ts` from phase 3 has its own `gameTypeLabel`, make it use these titles, mapping the singular form for display ("Tripoint", "River"), so there's a single source of truth.

### 2. Rewrite `HomePage.tsx`

- **Layout.** A `min-vh-100` wrapper with `background: var(--gg-bg)`, holding a centred `container` with `max-width: 720px`. Use an `<h1>` "Geogames" and a lead paragraph with the tagline.
- **Game cards.** Make them a radio group, so they work with the keyboard and screen readers:
  - Render `<div role="radiogroup" aria-label="Choose a game">`. Each card is a `<button type="button" role="radio" aria-checked={selected}>` containing the icon (size 28), the title in bold, and the description in muted small text.
  - The selected card gets a 2px primary border and a light primary background (`border-primary bg-primary-subtle`). Unselected cards get `border` and a white background, with a hover lift (`box-shadow`).
  - Left/right arrow keys move the selection between cards. That's a small `onKeyDown` on the group; Tab focuses the selected card.
  - Use `row g-3` with `col-12 col-sm-4` so the cards stack on phones.
  - The default selection is `TRIPOINT`.
- **Rounds selector.** Use react-bootstrap `ToggleButtonGroup type="radio" name="rounds"`, with one `ToggleButton variant="outline-primary"` per entry in `ROUND_OPTIONS`. Label it with a visible "Rounds" text tied to the group by `aria-labelledby`. The default is 5.
- **Start game.** A large primary button (`btn-lg`, `w-100` below 576px, auto width above). It calls `createGame(selected.name, rounds)`, and while `isLoading` it shows `<Spinner size="sm"/> Starting…` and is disabled.
- **Status line** under the button, inside `aria-live="polite"`:
  - If `isLoading` has lasted more than 3 seconds (a `setTimeout` started when loading begins, cleared when it ends or on unmount), show: "Waking up the server. The first game can take up to a minute."
  - If `error` is set, show `<Alert variant="danger">` with: "Couldn't start a game. The server may be down. Try again." The Start button remains as the retry.
- **Pre-warm.** In a `useEffect` on mount, fire `fetch(serverEndpoint() + "/health").catch(() => {})` and ignore the result. That way a cold Render instance starts waking while the player is still choosing.
- Delete the modal, the dropdowns, and the "(working title)" text. Remove the now-unused react-bootstrap imports.

### 3. Page title

In `pages/index.tsx`, change the `<title>` from "Geogames!" to "Geogames". Also check that the game page titles use the same `… | Geogames` pattern.

## Manual checks

- At 360px:
  - The cards stack, there's no horizontal scroll, and the Start button spans the full width.
- At 1440px:
  - The cards sit in one row and the content is centred, not stretched.
- Keyboard:
  - Tab to the cards, change the game with the arrow keys, Tab to the rounds and pick one with Space, Tab to Start and press Enter.
  - A game starts with the chosen type and round count. Check that the round count in the top bar matches.
- Stop the backend and press Start: the error alert appears. Start the backend again and press Start: the game starts.
- With a slow backend (you can simulate one with DevTools network throttling), the "Waking up the server" message appears after about 3 seconds.

## Done when

- [ ] There's no modal or dropdown on the home page. The cards, the rounds toggle, Start, and the status/error line all work as described.
- [ ] 10 rounds can be chosen.
- [ ] `GAME_OPTIONS` is the single source for game names and descriptions.
- [ ] `/health` is pre-fetched on mount.
- [ ] `npm run lint` and `npm run build` pass.
