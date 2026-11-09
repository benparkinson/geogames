# Phase 2: responsive game layout

Read `README.md` in this folder first. This phase assumes phase 1 is done. It changes only the frontend.

## Why

- `.map-game { min-width: 400px; max-width: 65vh }` scrolls sideways on phones narrower than 400px.
- On desktop the same rule makes the game a thin column whose width depends on the window's *height*.
- The map is capped at `max-height: 60vh`. Everything else stacks below it, so on a laptop the input can drop below the fold.
- The answer-progress "chips" are grey disabled buttons with no label.
- The Rivers map is a blank grey box until you answer, with no explanation.

## Target layout

The game screen fills exactly one viewport. The map takes all the space the controls don't use.

```
Mobile / tablet (<992px)          Desktop (≥992px)
┌──────────────────────────┐      ┌───────────────────────────────┬──────────────┐
│ ⌂ Geogames  Round 2 of 5 │      │ ⌂ Geogames      Round 2 of 5  │  Score 1     │
│               Score 1    │      ├───────────────────────────────┼──────────────┤
├──────────────────────────┤      │                               │ Prompt       │
│                          │      │                               │ [France✓][?] │
│          MAP             │      │             MAP               │ [input][Guess]│
│        (1fr)             │      │       (fills the left)        │ Hint  Reveal │
│                          │      │                               │ feedback     │
├──────────────────────────┤      │                               │ wrong: ✗ ✗   │
│ Prompt                   │      │                               │ ‹Prev  Next› │
│ [France ✓][  ?  ][  ?  ] │      │                               │              │
│ [input        ][Guess]   │      │                               │              │
│ Hint · Reveal  ‹ ›       │      │                               │              │
│ feedback / wrong guesses │      │                               │              │
└──────────────────────────┘      └───────────────────────────────┴──────────────┘
```

## Steps

### 1. Viewport meta

In `pages/_app.js`, add a `next/head` block:

```jsx
<Head>
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content" />
</Head>
```

`interactive-widget=resizes-content` makes Android Chrome shrink the layout when the keyboard opens, so the input stays visible above it and the map shrinks instead. iOS ignores it, and that's fine. Put the tag in `_app`, not `_document`: Next warns about viewport tags in `_document`.

### 2. New `GameLayout` component

Create `client/src/games/common/GameLayout.tsx`. It takes three slots and only does layout:

```tsx
export default function GameLayout({ topBar, map, panel }: { topBar: ReactNode; map: ReactNode; panel: ReactNode }) {
  return (
    <div className="game-screen">
      <header className="game-top">{topBar}</header>
      <main className="game-map">{map}</main>
      <aside className="game-panel">{panel}</aside>
    </div>
  );
}
```

`MapGame` renders `<GameLayout>`:
- `map` = the map component.
- `panel` = the prompt, hint texts, answer slots (step 4) and `MapGameInput`.
- `topBar` = `<GameTopBar>` (step 3).

### 3. `GameTopBar` (replaces `MapGameScore`)

Create `client/src/games/common/GameTopBar.tsx` and delete `MapGameScore.tsx`. It shows:

- **Left:** a Home link, `<Link href="/">`, containing a `<HouseFill/>` icon and the text "Geogames". The text uses `d-none d-sm-inline`, and the link has `aria-label="Home"`.
- **Right:** `Round {n} of {total}` and `Score {score}` (or `Final score {score} / {total}` when the game is complete), in a compact, muted style. Reuse the fields already on `Round`.
- **Round buttons:** Previous / Next round move here from `MapGameInput`, as small `outline-secondary` buttons in the top bar. Keep the labels, `aria-label`s and disabled tooltips from phase 1. Phase 3 replaces these buttons with progress dots.

### 4. Answer slots (replace the grey chips)

Create `client/src/games/common/AnswerSlots.tsx`, and remove `renderCorrectAnswerChips` and `determineAnswerChipColour` from `MapGame`. Inputs: `answerCount`, the correct `AnswerModel`s found so far, `gaveUp`, and the full answer list (for when the player gave up).

- Render `answerCount` slots in a row (`d-flex gap-2 flex-wrap`), **for every game, including Rivers** (one slot). This tells the player how many answers are expected without any text.
- **Empty slot:** a dashed-border pill holding "?", muted, with `aria-label="Not found yet"`.
- **Found slot:** a solid success-coloured pill holding the country or river name and a `<CheckLg/>` icon.
- **Revealed slot** (after Reveal answer): a neutral or secondary pill holding the name and an `<EyeFill/>` icon, with `title="Revealed"`. Don't use red here: revealing isn't an error.
- Wrap the row in an element with `aria-label="Answers: 1 of 3 found"`.

Wrong guesses move out of the mixed list:
- `MapGameInput`'s list now shows **only wrong guesses**, as small struck-through `text-muted` chips with an `<XLg/>` icon, under a tiny "Wrong guesses" label.
- Correct guesses now appear only in the slots.
- Delete `Answer.tsx` if nothing uses it any more.

### 5. CSS

In `client/src/styles.css`, delete the `.map`, `.leaflet-container`, `.map-game`, `.background`, `#input`, `#answer-header`, `#result`, `#give-up`, `#guess`, `.correct` and `.incorrect` rules. Check for usages first, and remove the matching `className`/`id`s. Then add:

```css
:root {
  --gg-bg: #c8cfe8;
  --gg-panel-bg: #ffffff;
  --gg-panel-width: 360px;
}

.game-screen {
  display: grid;
  height: 100vh;      /* fallback */
  height: 100dvh;
  grid-template-rows: auto minmax(0, 1fr) auto;
  grid-template-areas: "top" "map" "panel";
  background: var(--gg-bg);
}
.game-top   { grid-area: top; }
.game-map   { grid-area: map; position: relative; min-height: 0; }
.game-panel {
  grid-area: panel;
  background: var(--gg-panel-bg);
  max-height: 50dvh;
  overflow-y: auto;
  padding: 0.75rem 1rem calc(0.75rem + env(safe-area-inset-bottom));
  border-top-left-radius: 1rem;
  border-top-right-radius: 1rem;
  box-shadow: 0 -2px 12px rgb(0 0 0 / 0.12);
}
.game-map .leaflet-container { position: absolute; inset: 0; height: 100%; width: 100%; }

@media (min-width: 992px) {
  .game-screen {
    grid-template-columns: minmax(0, 1fr) var(--gg-panel-width);
    grid-template-rows: auto minmax(0, 1fr);
    grid-template-areas: "top top" "map panel";
  }
  .game-panel { max-height: none; border-radius: 0; box-shadow: -2px 0 12px rgb(0 0 0 / 0.08); }
}
```

Keep the `body` and `.tooltip-inner` rules. Make the top bar a slim `d-flex align-items-center justify-content-between px-3 py-2` row.

### 6. Map sizing and framing

- **Resize handling.** Add `client/src/map/MapResizer.tsx`, a child of `MapContainer`. Inside a `useEffect`, attach a `ResizeObserver` to `map.getContainer()` that calls `map.invalidateSize()`, and disconnect it on cleanup. Use it in both maps. Without it, Leaflet draws grey tiles when the layout changes size: on breakpoint changes, keyboard open/close, or the panel growing.
- **Rivers framing.** Replace the `arrayMid` centring and `CenterUpdater` in `RiverShapesMap.tsx` with a `FitBounds` child:
  - Use `useMap()`. In a `useEffect` keyed on the river name, call `map.fitBounds(L.geoJSON(river.geoData).getBounds(), { padding: [24, 24], maxZoom: 7 })`.
  - Keep `minZoom={3}` and `maxZoom={7}`. Give `MapContainer` an initial `center={[0, 0]} zoom={3}`; `FitBounds` replaces it immediately.
  - The whole river is then visible at any screen size.
- **Tripoint.** Keep `CenterUpdater` with zoom 5 instead of 4. On a full-height map, 4 is too far out.
- **Loading placeholder.** In both `*MapWrapper.tsx` files, replace the `loading: () => <p>A map is loading</p>` placeholder with a centred spinner that fills the map area: `<div className="d-flex h-100 align-items-center justify-content-center"><Spinner animation="border" /></div>`.

### 7. Rivers "shape only" caption

While the Rivers round is unanswered, the map has no tile layer.
- Give the map a plain dark background for that state by adding a `game-map--blank` class when `!gameOver` in Rivers. The simplest route is for `MapGame` to accept an optional `mapClassName` prop.
  - Add `--gg-map-blank: #2f3b52` to `:root`, plus `.game-map--blank .leaflet-container { background: var(--gg-map-blank); }`.
  - The background must be dark because the river is drawn in `LightGoldenRodYellow`, which barely shows on the light page background.
- Overlay a small pill at the top-centre of the map: "Shape only. The satellite map appears when you answer." Use `position: absolute; z-index: 500; pointer-events: none` so it stays above the Leaflet panes but doesn't block dragging.

### 8. Full-page spinner

`GameRoundPage.renderSpinner` uses the `height-80-vh` class, which isn't defined anywhere. Change it to `min-vh-100`, or `style={{height: '100dvh'}}`.

## Manual checks

- At **360px**:
  - There is no horizontal scroll.
  - The map fills the space between the top bar and the panel.
  - The panel scrolls internally when needed.
  - With the keyboard open (Chrome device emulation, or a real Android phone), the input stays visible.
- At **768px**: the same layout as mobile, with no wasted side space.
- At **1440px**:
  - The map fills the left side and the panel sits on the right at 360px.
  - Nothing scrolls the page.
- Resize the window across 992px. The map redraws without grey areas.
- Rivers:
  - The whole river is in view.
  - The caption shows before answering and disappears after.
- Tripoint:
  - The slots fill in as countries are found.
  - Reveal fills the remaining slots with the eye style.
  - Wrong guesses appear only as struck-through chips.

## Done when

- [ ] `GameLayout`, `GameTopBar`, `AnswerSlots` and `MapResizer` exist and are used. `MapGameScore.tsx` is deleted, along with `Answer.tsx` if it's unused.
- [ ] The old fixed-size CSS rules are gone. The game screen is a `100dvh` grid with the two breakpoint layouts above.
- [ ] Rivers uses `fitBounds`. Both maps call `invalidateSize` on resize.
- [ ] The viewport meta is set in `_app.js`.
- [ ] `npm run lint` and `npm run build` pass.
