# UI modernisation: overview

This folder holds five phase plans. Do them **in order, one phase per session**, because each phase builds on the one before. Read `CLAUDE.md` and this README before starting any phase.

| Phase | File | Summary |
|---|---|---|
| 1 | [phase-1-quick-wins.md](phase-1-quick-wins.md) | Labelled controls, one hint system, visible "Reveal answer", guess feedback, map and loading bug fixes |
| 2 | [phase-2-responsive-layout.md](phase-2-responsive-layout.md) | Full-viewport game screen: map plus a bottom sheet (mobile) or side panel (desktop), answer slots |
| 3 | [phase-3-round-flow.md](phase-3-round-flow.md) | Result panel after each round, round progress dots, end-of-game summary (includes a small backend change) |
| 4 | [phase-4-home-page.md](phase-4-home-page.md) | Home page without the modal: game cards, rounds selector, cold-start handling |
| 5 | [phase-5-polish.md](phase-5-polish.md) | `GET /api/countries` with autocomplete, dark mode, motion, clean-up |

## Goal

A new player should understand the game without reading instructions. Today they can't: there are unlabelled emoji buttons (🌍 submits, 💡 hints), "Give up" sits inside a "?" menu, the game has no end screen, and the layout breaks on phones and wastes space on desktop.

## Decisions already made (do not revisit)

- **Hints are free.** Using a hint never affects the score.
- **The country autocomplete endpoint (`GET /api/countries`) is in scope** (phase 5).
- Keep the current stack: Next 13 pages router, react-bootstrap, react-query v3, react-leaflet. **Do not upgrade Next, React or react-query.**
- Add exactly one UI dependency: `react-bootstrap-icons@1.11.4` (phase 1). Don't add other UI libraries.
- Keep the static export (`next build && next export`). **Do not add new routes.** In particular, `pages/games/[gameId].tsx` works only because it is a single dynamic page, so the summary screen in phase 3 is a view inside that page, not a new URL.
- The client still checks answers. The server only stores `AnswerState`.

## Design rules (apply them in every phase)

1. **No icon-only buttons**, except "close" (✕) and the Home link. Every button has visible text. If space is tight below 576px, an icon may replace the text, but the button must then have `aria-label` and `title`.
2. **Instructions live on screen.** The game shows a one-line prompt next to the input. There is no explanation modal.
3. **Every action gives visible feedback**, announced through an `aria-live="polite"` region.
4. **Never rely on colour alone.** Correct, revealed and wrong states always carry an icon or text as well (✓, ✗, eye).
5. Respect `prefers-reduced-motion` for any animation.

## Working rules

- Work on a branch named `ui-modernisation`, created from `complete-country-migration` (phase 5 relies on its `processed/countries` files). If that branch has been merged into `main`, branch from `main` instead. If `ui-modernisation` already exists, keep working on it.
- **Do not commit unless the user asks.**
- The frontend has no tests. At the end of every phase, run these from `client/` and fix anything they report:
  - `npm run lint`
  - `npm run build`, which type-checks and does the static export.
- If the phase touches Java, also run `mvn spotless:apply` and `mvn test` from the repo root.
- **Manual check:** run the backend (`mvn spring-boot:run`) and `npm run dev`, then use the app at 360px, 768px and 1440px wide. Each phase file lists what to check. If you can't drive a browser, say so in your final report and give the user the checklist instead. Don't claim it was checked.
- Before you finish, compare the "Done when" list at the end of the phase file against your work, and report each item.
- If the code differs from what a phase file describes (for example, because an earlier phase was done differently), adapt to the code and mention the difference in your report.
