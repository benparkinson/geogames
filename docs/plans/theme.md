# Plan: one theme for the whole site, ready to drop Bootstrap later

Read `CLAUDE.md` first. This plan changes only the frontend (`client/`). Work on a new branch from an up-to-date `main`, one branch per phase (`theme-tokens`, `theme-primitives`, `theme-classes`). Do not commit, push or open a PR unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Don't start the next phase until that step passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Goal

1. **One file sets the look of the whole site**: `client/src/theme/tokens.css`. Changing the accent colour, the radius or the font there changes every button, card, chip and panel, in light and dark mode.
2. **One component per kind of control** (`client/src/ui/`): a `Button` with *meaning-based* variants (`primary`, `secondary`, `hint`, `danger`, `quiet`) instead of Bootstrap colour names, so "make all buttons rounder" or "make the hint button orange" is one edit.
3. **Nothing new depends on Bootstrap.** By the end of phase 3, `react-bootstrap` is gone and no Bootstrap *colour or component* class is used. Bootstrap's CSS stays only for layout utilities (`d-flex`, `gap-2`, `mb-2`, `row`/`col`…) and its reset. Removing those is phase 4, which is outlined here but is a separate piece of work.

## Ground rules

- **No visible change.** The first values of every token are today's values (Bootstrap 5.3's defaults and the current `--gg-*` values), so the site looks the same after each phase, in light and dark mode, at phone and desktop widths. The point is to make later changes cheap, not to restyle now.
- **No behaviour, text or accessibility change.** Keep every label, `aria-*`, role, focus behaviour and keyboard behaviour. Tests that query by role or name must pass unchanged.
- **No new dependencies.** Plain CSS custom properties, no Sass, no CSS-in-JS, no Tailwind, no component library. Keep `react-bootstrap-icons`: it is only SVG icons drawn with `currentColor` and doesn't use Bootstrap's CSS.
- **Prefix everything new with `gg-`** (`.gg-btn`, `--gg-color-accent`) so it can't collide with Bootstrap while both are loaded.
- **Components use semantic tokens only.** A component's CSS says `var(--gg-color-correct)`, never `#198754` and never `var(--bs-success)`.
- **Derive, don't duplicate.** Hover, active, subtle and focus colours are computed from the base token with `color-mix(in srgb, …)`, so changing `--gg-color-accent` also changes its hover and focus ring. (`color-mix` works in every current browser; that is fine for this site.)
- Use the Node Maven installed: put `target/node` (repo root) on `PATH` for npm commands.
- Frontend tests: the conventions in `CLAUDE.md` apply (import from `vitest`, `renderWithClient`, colocated `*.test.tsx`). The Vitest coverage thresholds (99/98/99/99) apply to `client/src/ui/**` too, so every new component needs its own test.

## Where things stand (checked on 2026-11-09)

- `pages/_app.tsx` imports `bootstrap/dist/css/bootstrap.css`, then `leaflet/dist/leaflet.css`, then `src/styles.css`.
- `pages/_document.tsx` sets `data-bs-theme="light|dark"` on `<html>` before React runs (stored choice in `localStorage` key `gg-theme`, else the system setting). `components/ThemeToggle.tsx` flips it. Bootstrap's own dark mode keys off this attribute.
- `src/styles.css` (189 lines) holds the layout (game screen grid, home layout, round dots, map caption, animations) and four variables: `--gg-bg`, `--gg-panel-bg`, `--gg-panel-width`, `--gg-map-blank`, with dark values under `[data-bs-theme="dark"]`. It also has literal colours and Bootstrap fallbacks, e.g. `.round-dot--correct { background: var(--bs-success, #198754) }`, `.map-caption { background: rgb(0 0 0 / 0.65); color: #fff }`, and several `box-shadow` literals.
- **react-bootstrap components** (the only imports from `react-bootstrap`):
  - `Button`: `HomePage` (Start), `MapGameInput` (Guess, Hint, Reveal), `GameTopBar` (Results), `RoundResult` (Next round / See results), `GameSummary` (Play again).
  - `ToggleButtonGroup` + `ToggleButton`: `HomePage` (rounds picker). It renders a `radio` input plus a label per option; `HomePage.test.tsx` queries `getByRole("radio", { name: "5" })`.
  - `Spinner`: `LoadingSpinner`, `HomePage` (inside Start), `GameSummary` (inside Play again).
  - `Alert`: `HomePage` (start error).
- **Bootstrap component classes written by hand**: `btn btn-outline-secondary` on `<Link>` Home in `GameSummary`, on `ThemeToggle` and on `KofiBadge` (which also has a literal `#72a4f2` icon colour); `badge rounded-pill bg-success|bg-secondary` in `AnswerSlots`; `badge bg-body-secondary text-body-secondary … border` for wrong guesses in `MapGameInput`; `card` + inline `style={{ maxWidth: 560 }}` and `list-group` / `list-group-item list-group-item-action` in `GameSummary`; `form-control` in `GuessBox`.
- **Bootstrap colour utilities**: `text-success`, `text-secondary`, `text-warning`, `text-warning-emphasis`, `text-danger`, `text-muted`, `text-body`, `bg-body`, `bg-primary-subtle`, `border border-2 border-primary` (selected home card).
- **Bootstrap layout and type utilities** (left alone until phase 4): `d-flex`, `flex-*`, `align-items-*`, `justify-content-*`, `gap-*`, `m*-*`, `p*-*`, `row g-3`, `col-12 col-sm-4`, `w-100`, `h-100`, `min-vh-100`, `d-none d-sm-inline`, `text-center`, `text-start`, `text-nowrap`, `small`, `fw-*`, `fs-6`, `h3`/`h5`/`h6`, `lead`, `rounded-*`, `visually-hidden`, `overflow-hidden`, `text-decoration-*`.
- **Map colours** are already in one place: `src/map/constants.ts` (answer, revealed and clue colours) and `src/map/flagIcons.ts` (flag markers). They are drawn on satellite imagery or the blank map and don't change with light/dark. Leave them where they are (see "Not in this plan").
- **Tests that assert Bootstrap classes** (update them in the phase that changes the markup):
  - `components/GuessBox.test.tsx:32` expects `form-control`.
  - `components/HomePage.test.tsx:125-126` expects the Ko-fi link to have `btn-outline-secondary` and not `text-white`.
  - `games/common/GameRoundPage.test.tsx:81` and `games/common/GameSummary.test.tsx:38` look for `.spinner-border`.
  - `games/common/GuessesLeft.test.tsx:10,18` expect `text-muted` / `text-warning-emphasis`.
  - `e2e/tests/full-game.spec.ts:78` locates `.list-group-item`.

## Target structure

```
client/src/theme/
  tokens.css            the one file to edit: every colour, radius, shadow, font, duration
  bootstrap-bridge.css  points Bootstrap's own variables at the tokens (deleted in phase 4)
  components.css        .gg-btn, .gg-toggle, .gg-spinner, .gg-notice, .gg-chip, .gg-card, .gg-list, .gg-input, .gg-text-*
client/src/ui/
  Button.tsx            Button, plus buttonClass() for links that look like buttons
  ToggleGroup.tsx       radio group drawn as joined buttons (the rounds picker)
  Spinner.tsx
  Notice.tsx            replaces Alert
  Chip.tsx              replaces badge
  index.ts              re-exports
client/src/styles.css   layout only (game screen, home layout, round dots, map caption, animations), using tokens
```

`pages/_app.tsx` imports, in this order: `bootstrap.css`, `leaflet.css`, `theme/tokens.css`, `theme/bootstrap-bridge.css`, `theme/components.css`, `styles.css`. Order matters: later files win at equal specificity.

---

## Phase 1: tokens and the bridge

### 1.1 `src/theme/tokens.css`

Two tiers, in this order, with a short comment above each group:

1. **Palette**: the raw colours, named by what they are (`--gg-blue-600: #0d6efd`). Only the semantic tokens below may use these.
2. **Semantic tokens**: named by role. These are what every other file uses.

Light values go under `:root, [data-bs-theme="light"]`; dark overrides under `[data-bs-theme="dark"]`. Only list a token under dark when its dark value differs. Take each light and dark value from what renders today: read them from `client/node_modules/bootstrap/dist/css/bootstrap.css` (the `:root, [data-bs-theme=light]` block near the top, the `[data-bs-theme=dark]` block after it, and `.btn-primary` etc. for button colours) and from `src/styles.css`. Don't guess a value.

Semantic tokens to define (add others if a phase needs them; keep the naming pattern):

| Token | Today's source |
|---|---|
| `--gg-color-page-bg` | `--gg-bg` |
| `--gg-color-panel-bg` | `--gg-panel-bg` |
| `--gg-color-surface` | `--bs-body-bg` (cards, inputs, list items) |
| `--gg-color-surface-muted` | `--bs-secondary-bg` (wrong-guess chips) |
| `--gg-color-text` | `--bs-body-color` |
| `--gg-color-text-muted` | `--bs-secondary-color` |
| `--gg-color-border` | `--bs-border-color` |
| `--gg-color-link` / `--gg-color-link-hover` | `--bs-link-color` / `--bs-link-hover-color` |
| `--gg-color-accent` | `--bs-primary` (main actions, selection, focus) |
| `--gg-color-on-accent` | text on the accent (`#fff`) |
| `--gg-color-accent-subtle` | `--bs-primary-bg-subtle` (selected home card) |
| `--gg-color-correct` | `--bs-success` |
| `--gg-color-perfect` / `--gg-color-on-perfect` | `--bs-warning` / `#3d2e00` (round dots) |
| `--gg-color-revealed` | `--bs-secondary` |
| `--gg-color-hint` | `--bs-warning` (the outline-warning Hint button) |
| `--gg-color-warning-text` | `--bs-warning-text-emphasis` ("1 guess left") |
| `--gg-color-danger` | `--bs-danger` |
| `--gg-color-danger-text` / `--gg-color-danger-subtle` | `--bs-danger-text-emphasis` / `--bs-danger-bg-subtle` (the Alert) |
| `--gg-color-coffee` | `#72a4f2` (Ko-fi icon) |
| `--gg-color-map-blank` | `--gg-map-blank` (same in both themes) |
| `--gg-color-map-caption-bg` / `--gg-color-map-caption-text` | `rgb(0 0 0 / 0.65)` / `#fff` |
| `--gg-font-sans`, `--gg-font-mono` | the `body` and `code` stacks in `styles.css` |
| `--gg-radius-sm`, `--gg-radius`, `--gg-radius-lg`, `--gg-radius-xl`, `--gg-radius-pill` | `.25rem`, `.375rem`, `.5rem`, `1rem` (panels), `50rem` |
| `--gg-shadow-panel`, `--gg-shadow-panel-side`, `--gg-shadow-floating`, `--gg-shadow-hover` | the `box-shadow` literals in `styles.css` |
| `--gg-focus-ring` | `0 0 0 .25rem color-mix(in srgb, var(--gg-color-accent) 25%, transparent)` |
| `--gg-duration-fast`, `--gg-duration`, `--gg-duration-slow` | `120ms`, `150ms`, `200ms` |
| `--gg-panel-width` | `--gg-panel-width` |

At the top of the file, a comment of a few lines: this is the one place to change the look; components use only the semantic tokens; dark values go in the dark block; colours that must not change with the theme (the blank map) are listed once in the light block.

### 1.2 `src/styles.css` uses only tokens

- Delete its `:root { --gg-* }` and `[data-bs-theme="dark"] { … }` blocks (they moved to `tokens.css`).
- Replace every literal colour, radius, shadow and duration with a token, and every `var(--bs-…, #fallback)` with the matching `--gg-` token. Rename uses of the old names (`--gg-bg` → `--gg-color-page-bg`, `--gg-panel-bg` → `--gg-color-panel-bg`, `--gg-map-blank` → `--gg-color-map-blank`). Leave sizes and layout numbers (`360px`, `20px`, `992px`) alone: they're layout, not theme.
- `body` uses `var(--gg-font-sans)`, `code` uses `var(--gg-font-mono)`.

### 1.3 `src/theme/bootstrap-bridge.css`

Make the Bootstrap pieces still on the page follow the tokens. Use the same selectors as Bootstrap so this file wins by coming later:

```css
/* Temporary: points Bootstrap's variables at our tokens until Bootstrap is removed (docs/plans/theme.md, phase 4). */
:root, [data-bs-theme="light"], [data-bs-theme="dark"] {
  --bs-body-bg: var(--gg-color-surface);
  --bs-body-color: var(--gg-color-text);
  --bs-body-font-family: var(--gg-font-sans);
  --bs-secondary-color: var(--gg-color-text-muted);
  --bs-border-color: var(--gg-color-border);
  --bs-border-radius: var(--gg-radius);
  /* …only variables that something still on the page reads */
}
a { color: var(--gg-color-link); }
a:hover { color: var(--gg-color-link-hover); }
```

Bootstrap's links and `text-*`/`bg-*` utilities read `--bs-*-rgb` triplets, which can't be derived from a token, which is why links get a plain rule here. Don't try to bridge the `-rgb` variables or the per-button `--bs-btn-*` variables: phases 2 and 3 replace everything that uses them. Keep this file short. Check that `a` inside `.btn` classes isn't affected in a visible way (the Home link and Ko-fi link in phase 1 still use `btn` classes; if the bridge's `a` rule changes their colour, scope it as `a:not(.btn)`).

### 1.4 Imports

Update `pages/_app.tsx` to the import order in "Target structure" (create `components.css` empty, with a one-line comment, so the order is in place).

### 1.5 Verify phase 1

From `client/`: `npm run lint`, `npm run typecheck`, `npm test`, `npm run coverage`, `npm run build`. Then look at the site (`npm run dev` with the backend running, see `CLAUDE.md`) in light and dark mode at about 390 px and 1280 px wide: the home page (with a game selected), a game round while playing (after a hint and a wrong guess), the round result, and the summary. It must look the same as on `main`. Then **try the tokens**: set `--gg-color-page-bg` to a loud colour and `--gg-radius-xl` to `0`, check the page background and the panels change everywhere, and revert. If you can't open a browser, say so in the report and list these screens for the user to check.

---

## Phase 2: UI primitives

Write the components in `src/ui/` and their styles in `theme/components.css`. **No Bootstrap class inside a primitive.** Each variant is a block of local custom properties so a new variant is a few lines:

```css
.gg-btn {
  --gg-btn-bg: transparent; --gg-btn-fg: var(--gg-color-text); --gg-btn-border: var(--gg-color-border);
  display: inline-flex; align-items: center; justify-content: center; gap: .375rem;
  padding: .375rem .75rem; font: inherit; line-height: 1.5;
  border: 1px solid var(--gg-btn-border); border-radius: var(--gg-radius);
  background: var(--gg-btn-bg); color: var(--gg-btn-fg); text-decoration: none; cursor: pointer;
  transition: background-color var(--gg-duration-fast), border-color var(--gg-duration-fast), color var(--gg-duration-fast);
}
.gg-btn:focus-visible { outline: 0; box-shadow: var(--gg-focus-ring); }
.gg-btn:disabled, .gg-btn[aria-disabled="true"] { opacity: .65; pointer-events: none; }
.gg-btn--primary { --gg-btn-bg: var(--gg-color-accent); --gg-btn-fg: var(--gg-color-on-accent); --gg-btn-border: var(--gg-color-accent); }
.gg-btn--primary:hover { --gg-btn-bg: color-mix(in srgb, var(--gg-color-accent) 85%, black); }
/* outline variants: border and text in the role colour, filled on hover */
```

Match today's sizes: Bootstrap `btn` is `.375rem .75rem` at `1rem`; `btn-sm` is `.25rem .5rem` at `.875rem` with `--gg-radius-sm`; `btn-lg` is `.5rem 1rem` at `1.25rem` with `--gg-radius-lg`. Check `bootstrap.css` for the exact values (and the outline variants' hover colours) rather than trusting these.

### 2.1 `Button` and `buttonClass`

```ts
type ButtonVariant = "primary" | "secondary" | "hint" | "danger" | "quiet";
type ButtonSize = "sm" | "md" | "lg";
export function buttonClass(opts?: { variant?: ButtonVariant; size?: ButtonSize; block?: boolean; pill?: boolean; className?: string }): string;
export function Button(props: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?; size?; block?; pill? }): React.JSX.Element;
```

- `Button` defaults to `type="button"`, `variant="secondary"`, `size="md"`, and passes every other prop (`autoFocus`, `onClick`, `disabled`, `aria-*`, `title`) through.
- `buttonClass` is for elements that aren't buttons but look like one: the Home `<Link>` in `GameSummary`, the Ko-fi `<a>`, and the theme toggle. One place builds the class string.
- Variant mapping from today: `primary` → `primary`; `outline-primary` → `secondary`; `outline-warning` → `hint`; `outline-danger` → `danger`; `outline-secondary` → `quiet`.

### 2.2 `ToggleGroup`

Replaces `ToggleButtonGroup`/`ToggleButton` in `HomePage`. Props: `name`, `value`, `options: { value: T; label: React.ReactNode }[]`, `onChange(value: T)`, `size`, and `aria-labelledby`. Render the same structure react-bootstrap renders today, so the tests and screen readers see the same thing: a `div role="group"` holding, per option, `<input type="radio" class="gg-toggle__input" id=… name=… checked=… />` and `<label class="gg-btn gg-btn--secondary …" for=…>`. Hide the input visually but keep it focusable (the same technique as Bootstrap's `.btn-check`), draw the checked label filled (`input:checked + label`) and show the focus ring on the label when the input has `:focus-visible`. Join the buttons (no gap, inner radii 0) as today. `HomePage.test.tsx`'s `getByRole("radio", { name: "5" })` must keep working unchanged.

### 2.3 `Spinner`

`<Spinner size="sm" | "md" decorative? />`: a `span.gg-spinner` drawn with a border and `currentColor` (copy the look of `.spinner-border`, including its `prefers-reduced-motion` behaviour). When not decorative it has `role="status"` and a visually hidden "Loading…"; inside a button that already says "Starting…" it is `decorative` (`aria-hidden="true"`). Use it in `LoadingSpinner`, `HomePage` and `GameSummary`. Drop `LoadingSpinner`'s `variant` prop if nothing passes it. Change the two `.spinner-border` test queries to `getByRole("status")` (or `.gg-spinner` if a role query is ambiguous).

### 2.4 `Notice`

`<Notice tone="danger">` renders a `div.gg-notice.gg-notice--danger` with `role="alert"` (react-bootstrap's `Alert` sets that role too; check the current HomePage test still finds it). Colours from `--gg-color-danger-text` / `--gg-color-danger-subtle` and a border mixed from them; padding and radius as Bootstrap's alert.

### 2.5 Replace the react-bootstrap imports

Swap every use listed in "Where things stand" for the primitives, keeping the same variant meaning, size, `autoFocus`, `disabled`, `onClick` and `className` (layout utilities such as `w-100` and `flex-grow-1` stay for now; prefer `block` over `w-100` on a `Button`). Also convert the hand-written `btn …` classes on the `GameSummary` Home link, `ThemeToggle` and `KofiBadge` to `buttonClass(...)`; move the Ko-fi icon colour to `color: var(--gg-color-coffee)` via a class and drop its inline `style`. Update `HomePage.test.tsx:125-126` to assert the new classes (or drop the class check if it only guarded the styling, keeping the test of the link itself).

Then remove `react-bootstrap` from `client/package.json` and run `npm install` with the Maven Node to update the lock file. `grep -r "react-bootstrap\"" client/src client/pages` must find nothing (`react-bootstrap-icons` stays).

### 2.6 Tests for the primitives

`src/ui/*.test.tsx`: each variant and size produces its class; `Button` defaults to `type="button"` and forwards props and clicks; `buttonClass` combines options; `ToggleGroup` checks the current value, calls `onChange` with the option's value on click and with the arrow keys (native radio behaviour; use `userEvent`), and is labelled by `aria-labelledby`; `Spinner` has a status role unless decorative; `Notice` has `role="alert"`.

### 2.7 Verify phase 2

Everything in 1.5 (including the visual check of every button state: hover, focus with the keyboard, disabled, and the rounds picker), plus: set `--gg-color-accent` to a loud colour and check that it changes the Start button, the rounds picker, the Guess button, Next round, Results, Play again and the focus rings; set `--gg-color-hint` and see only the Hint button change; set `--gg-radius` to `0` and see every button go square. Revert.

---

## Phase 3: the remaining Bootstrap colour and component classes

### 3.1 Text tones

Add `.gg-text-muted`, `.gg-text-correct`, `.gg-text-revealed`, `.gg-text-perfect` (uses `--gg-color-perfect`, for the star), `.gg-text-warning` (uses `--gg-color-warning-text`) and `.gg-text-danger`. Replace `text-muted`, `text-success`, `text-secondary`, `text-warning`, `text-warning-emphasis`, `text-danger` and `text-body` everywhere in `client/src`. Note `GuessesLeft` uses `text-warning` (dots) and `text-warning-emphasis` (text) for different colours today; keep the difference (a `--gg-color-hint`-based class for the dots if needed). Update `GuessesLeft.test.tsx`.

### 3.2 Chips

`<Chip tone="correct" | "revealed" | "neutral" strike? />` (`span.gg-chip`, pill radius, today's badge padding and `fs-6 fw-normal` look). Use it for `AnswerSlots` (found and revealed) and the wrong-guess list in `MapGameInput`. Move `.answer-slot-empty` into `components.css` using tokens, keeping its class name (nothing tests it, but it's descriptive). Keep the `slot--filled` animation class on the chips.

### 3.3 Cards, list, input, home cards

- `.gg-card` (surface, border, `--gg-radius-lg`, padding) replaces `card` in `GameSummary`; move the inline `maxWidth: 560` into a class.
- `.gg-list` / `.gg-list__item` replace `list-group` / `list-group-item list-group-item-action` (borders between items, hover background, focus ring, first/last radius). Update `e2e/tests/full-game.spec.ts:78` to `.gg-list__item`.
- `.gg-input` replaces `form-control` in `GuessBox` (surface, text, border, radius, padding, `:focus` border and `--gg-focus-ring`, `:disabled`, placeholder colour). Keep `answer-attempt` and `shake`. Update `GuessBox.test.tsx:32`.
- Home game cards: replace `border border-2 border-primary bg-primary-subtle` / `border bg-body` with `.game-card` styles in `styles.css` plus a `.game-card--selected` modifier using `--gg-color-accent` and `--gg-color-accent-subtle`. Keep the 2 px selected border so the layout doesn't shift (today unselected is 1 px and selected 2 px; keep that exactly or use an inset shadow, but don't change what it looks like).

### 3.4 Shrink the bridge

Delete every line of `bootstrap-bridge.css` that nothing on the page reads any more. What's left should be the body and reset variables (`--bs-body-*`, headings) for phase 4 to remove.

### 3.5 A check that keeps it this way

Add `client/scripts/check-theme.mjs` and run it at the end of `npm run lint` (`"lint": "eslint . --max-warnings 0 && node scripts/check-theme.mjs"`), so `mvn verify` and the CI `lint-frontend` job enforce it. It scans `client/src` and `client/pages` (not tests, not `lint-probe`) and fails, naming file and line, when:

- a `.css` file other than `theme/tokens.css` contains a colour literal (`#rgb`/`#rrggbb`/`#rrggbbaa`, `rgb(`, `rgba(`, `hsl(`, `hsla(`), apart from `black`/`white`/`transparent` inside `color-mix(...)`;
- a `.ts`/`.tsx` file imports from `"react-bootstrap"` or has `style={{` with a colour;
- a `className` contains one of the Bootstrap colour/component classes replaced in phases 2 and 3 (`btn`, `btn-*`, `badge`, `card`, `list-group*`, `form-control`, `alert*`, `spinner-*`, `text-(muted|success|secondary|warning|warning-emphasis|danger|body|body-secondary|primary)`, `bg-*`, `border-(primary|success|secondary|danger|warning)`).

`src/map/constants.ts` and `src/map/flagIcons.ts` are allowed colour literals (Leaflet styles; see below); list them as exceptions with the reason in a comment. Add a Vitest test for the script's matching function (export it, and keep the file-walking part thin), or exclude the script from coverage like the other `scripts/`; check how `vitest.config.mts` treats `scripts/` and follow that.

### 3.6 `CLAUDE.md`

Add a short **Theme** paragraph under Architecture: `src/theme/tokens.css` is the only place for colours, fonts, radii, shadows and durations (light block, dark block); components use semantic `--gg-color-*` tokens; controls come from `src/ui/` (`Button` variants by meaning, `buttonClass` for links, `ToggleGroup`, `Spinner`, `Notice`, `Chip`); `npm run lint` runs `scripts/check-theme.mjs`; Bootstrap is still loaded for layout utilities and its reset only, with `theme/bootstrap-bridge.css` pointing its variables at the tokens. Fix the Overview line that says the frontend uses react-bootstrap. Map colours live in `map/constants.ts`.

### 3.7 Verify phase 3

Everything in 2.7, plus `npm run lint` (now including the theme check), the e2e suite (see `CLAUDE.md`: build the jar with `NEXT_PUBLIC_API_URL=http://localhost:8080`, then Playwright), and finally `mvn verify` from the repo root. Then the token test once more: change `--gg-color-correct`, `--gg-color-revealed` and `--gg-color-perfect` and check the answer chips, round dots, round result and summary all follow. Revert.

---

## Phase 4 (later, separate plan): remove Bootstrap's CSS

Not part of this work; listed so phases 1–3 don't make it harder.

- Replace the layout utilities with component classes in `styles.css` (or a small `theme/utilities.css` with only the handful used everywhere: flex row with gap, stack, visually-hidden).
- Replace Bootstrap's reset (Reboot) with a short one: `box-sizing`, body margin, heading sizes for `h1`–`h6` and the `.h3`/`.h5`/`.h6`/`.lead`/`small` used today, button and input font inheritance.
- Rename `data-bs-theme` to `data-theme` in `_document.tsx`, `ThemeToggle.tsx` (and its test) and `tokens.css`. Keep the `gg-theme` localStorage key so visitors keep their choice.
- Delete `bootstrap-bridge.css`, the `bootstrap.css` import and the `bootstrap` dependency. Extend `check-theme.mjs` to reject the layout utilities too.

## Not in this plan

- **Map colours.** They stay in `src/map/constants.ts` and `src/map/flagIcons.ts`, which already makes them a one-file change. Moving them into `tokens.css` would mean giving every Leaflet layer a `className` and styling `stroke`/`fill` from CSS; only worth it if the map colours should follow light/dark, which they currently shouldn't (they sit on satellite imagery and a fixed blank map).
- **Leaflet's own controls** (zoom buttons, attribution) keep Leaflet's CSS.
- **Restyling.** Choosing new colours, fonts or shapes is the user's call once the tokens exist.

## Final report

List per phase: files added, files changed, tests changed (and why), anything that looked different and how you resolved it, and any screen you couldn't check visually.
