# Plan: frontend clean-up and pluggable game modules

Read `CLAUDE.md` first. All work is in `client/`. Do not change anything outside `client/` except the frontend parts of `CLAUDE.md` (phase 7). A separate plan (`cleanup-backend.md`) covers the backend and may run in parallel. **Do not rely on any backend change:** the HTTP API and its JSON shapes stay exactly as they are today.

Work on a new branch, `cleanup-frontend`, created from the branch that holds the unit tests (see phase 0). Do not commit unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Do not start the next phase until that step passes.** If a step fails and the fix is not obvious after two attempts, stop and report to the user with the error output.

## Goal

Today, knowledge about each game type is spread across `games/gameTypes.ts`, `GameRoundPage.renderGame` / `renderTitle`, `games/common/answers.ts`, the per-game components (which each re-implement answer matching) and the map wrappers. When you're done, **adding a game type takes one new folder under `src/games/` and one line in `src/games/registry.ts`**, and nothing in `games/common/`, `GameRoundPage`, `GameSummary` or `HomePage` needs to change.

## Ground rules

- **Behaviour stays the same**, except for the deliberate changes listed below. Keep all user-visible text, CSS class names, ARIA labels and roles unchanged unless a phase says otherwise. The unit tests query by role, label and text, so they act as your safety net.
- **Deliberate behaviour changes** (the only ones allowed):
  1. Tripoint autocomplete starts working. `MapGameInput` receives `suggestions` but never passes them to `GuessBox`, so the datalist never renders (phase 1).
  2. A guess that matches an alias of an answer you've already found is a duplicate. Today, guessing "Germany" and then "Deutschland" counts as two correct answers and fills two slots (phase 3).
  3. The browser tab title comes from the game module's `title`, so a Tripoint round shows "Tripoints | Geogames" instead of "Tripoint | Geogames" (phase 5).
- **Tests.** If a test fails because a module moved or a function's signature changed by design, update the test to the new API and keep the same coverage. If a test fails because of one of the deliberate changes above (for example a test marked `// Current behaviour:`), update its assertion to the new behaviour. Any other failure means your refactor changed behaviour: fix the code, not the test. Never delete a test without replacing its coverage, and never skip one. List every test you changed, and why, in your final report.
- No new runtime dependencies.
- Don't use `any`, `@ts-ignore` or `@ts-expect-error`. Use `eslint-disable` only on a single line, with a reason, for a genuine false positive.
- Keep the existing code style: 2-space indent for new files, double quotes, function components.

## Verification commands

"**Verify (standard)**" below means all of these pass, run from `client/`:

1. `npm run lint`
2. `npm test` (the Vitest suite added by the unit-tests work; check `package.json` for the exact script name)
3. `npm run build` (this also type-checks)

## Phase 0: baseline

1. Check that the unit tests exist: `client/package.json` has `vitest` and a test script, and test files exist under `client/src`. If they don't, **stop and ask the user** whether to continue without them.
2. `git checkout -b cleanup-frontend`.
3. `npm ci`, then run the standard verification. Note the test counts. Everything must pass before you change anything. If something fails, stop and report.
4. If you can, start the backend (`mvn spring-boot:run` from the repo root) and `npm run dev`. Play one round of each game type to get a feel for the current behaviour. Keep them running for the smoke tests in later phases.

## Phase 1: small fixes and hygiene

1. **Autocomplete bug.** In `games/common/MapGameInput.tsx`, pass `suggestions={suggestions}` to `GuessBox`. Check that Tripoint shows country suggestions and Rivers shows none (Rivers never passes `suggestions`).
2. **Provider in `_app`.** Rename `pages/_app.js` to `pages/_app.tsx` and type it (`AppProps` from `next/app`). Wrap `<Component>` in `<QueryClientProvider client={queryClient}>` and render `<ReactQueryDevtools initialIsOpen={false} />` there. Remove the providers from `pages/index.tsx` and `pages/games/[gameId].tsx`, and the devtools from `GameRoundPage`. With no JS left, set `"allowJs": false` in `tsconfig.json`.
3. **Query client module.** Move the `QueryClient` from `api/gateway.ts` into `api/queryClient.ts` (export it as `queryClient`). Merge `config/environment.ts` into `api/gateway.ts` (keep `serverEndpoint()`'s behaviour) and delete `src/config/`.
4. **Fix import paths.** `GameRoundPage` imports `../../../src/...`. Use normal relative paths.
5. **Classes to types.** Every `export class X { fields }` with no behaviour (`GameRoundModel`, `GameResultModel`, `GameTypeModel`, `Round`, `MapProps`, `MapGameProps`, `MapGameInputProps`, `GuessBoxProps`, `CenterUpdaterProps`, `TripointMapProps`, `RiverShapesMapProps`, the river/tripoint models) becomes an `interface` or a `type`. `Round` loses its constructor: `GameRoundPage` builds a plain object literal. `AnswerModel` becomes `interface Guess { text: string; correct: boolean }` (phase 3 reshapes it again).
6. **State unions.** In `games/common/Model.ts` (rename from `.tsx`, since it has no JSX), add:
   ```ts
   export type AnswerState = "UNANSWERED" | "CORRECT" | "GAVE_UP";
   export type GameState = "CREATED" | "IN_PROGRESS" | "FINISHED";
   ```
   Type `CORRECT_ANSWER`, `GAVE_UP` and `UNANSWERED` as their literal types (`as const`, or annotate them as `AnswerState`), and use `AnswerState` everywhere `answerState: string` appears today, including `submitAnswer`. Replace the bare `"UNANSWERED"` and `"FINISHED"` string literals in `GameSummary` and `GameRoundPage` with the constants or types.
7. **Hook-like names.** `useHint` in `MapGame` / `MapGameInput` is not a hook. Rename it to `takeHint` (and the prop to `onHint`).
8. **Shared UI pieces.**
   - `components/LoadingSpinner.tsx` replaces the centred-spinner markup that's repeated in `GameRoundPage`, `GameSummary` and both map wrappers. Give it a `fullPage?: boolean` prop: full page uses `d-flex min-vh-100 …`, otherwise `d-flex h-100 …`. Keep the `variant` the callers use today (`GameRoundPage` uses `success`).
   - `components/HomeLink.tsx` replaces the duplicated "house icon + Geogames" link in `GameTopBar` and `GameSummary`. Give it a prop that hides the text below `sm`, as `GameTopBar` does today.
   - Move the repeated inline `style={{ background: "var(--gg-bg)" }}` and the "Wrong guesses" `style={{ fontSize: "0.7rem" }}` into classes in `styles.css` (for example `.page-bg` and `.wrong-guesses-label`).
9. Delete `client/README.md` (stale Create React App boilerplate), and remove the line in `CLAUDE.md` that says to ignore it.

**Verify (standard).** Smoke test: Tripoint shows country suggestions, both game types still play, and the summary page still works.

## Phase 2: API layer

Goal: components never call `fetch`, never build URLs or query keys, and never `JSON.parse`.

1. `api/http.ts`: `fetchJson<T>(path: string, init?: RequestInit): Promise<T>` prefixes `serverEndpoint()`, sets the JSON `Content-Type` header when there is a body, and throws an `Error` with the status code when `!response.ok`. Use it in every API function.
2. `api/queryKeys.ts`:
   ```ts
   export const queryKeys = {
     countries: ["countries"] as const,
     round: (gameId: string, round: number) => ["games", gameId, "rounds", round] as const,
   };
   ```
   Use these everywhere instead of `[roundUrl(...)]`. `roundUrl` becomes a private helper in `api/games.ts`.
3. `api/types.ts`: move the wire types (`GameRoundModel` → `GameRoundResponse`, `GameResultModel`, `GameTypeModel`) out of `games/common/Model.ts`. These describe the JSON exactly as the server sends it. Keep `Round`, `AnswerState` and the constants in `Model.ts`.
4. **Parse once, in the API layer.** `fetchRound` returns a `GameRoundView`:
   ```ts
   export interface GameRoundView {
     gameType: GameTypeModel;   // { name, answerCount }
     data: unknown;             // JSON.parse(jsonBlob)
     totalRoundCount: number;
     answerState: AnswerState;
     gameResult: { gameState: GameState; score: number; roundAnswerStates: AnswerState[] };
   }
   ```
   Move the "older backends don't send `roundAnswerStates`" fallback out of `GameRoundPage` into this normalisation step, and keep the comment. (The production backend is deployed separately and may be older, so the fallback stays.)
5. Hooks in `api/games.ts`:
   - `useRound(gameId, round)` wraps `useQuery` with `queryKeys.round`.
   - `useSubmitAnswer(gameId, round)` wraps `useMutation` and posts `{answerState}`. Its `onSuccess` invalidates `queryKeys.round(gameId, round)` through `useQueryClient()`, **not** the imported singleton.
   - `useCreateGame` stays, but uses `fetchJson`.
   - `GameSummary` uses `useQueries` with `queryKeys.round` and `fetchRound`. It must share the cache with `useRound`, so use the same key and function.
6. `api/countries.ts` uses `fetchJson` and `queryKeys.countries`. Keep `retry: 1` and the infinite `staleTime` / `gcTime`.
7. `GameRoundPage` and `GameSummary` now only use these hooks. Delete the inline `fetch` and `createUrl()` from `GameRoundPage`.

**Verify (standard).** Smoke test: answer a round. The top-bar score and progress dots update after the refetch, and the summary still lists every round with its answers.

## Phase 3: answer logic out of components

Goal: games describe their answers as data. One pure, React-free module decides whether a guess is right.

1. `games/common/answers.ts` (this replaces today's `answerNames` / `gameTypeLabel`, which move to the registry in phase 5):
   ```ts
   export interface AnswerCandidate { name: string; additionalNames: string[] }

   /** The canonical name of the candidate this guess matches (by name or alias, using normaliseString), or null. */
   export function matchAnswer(guess: string, candidates: AnswerCandidate[]): string | null;
   ```
2. `games/common/guessReducer.ts`, a pure reducer with no React imports:
   ```ts
   interface GuessState { guesses: Guess[]; gaveUp: boolean; over: boolean; hintsUsed: number }
   interface Guess { text: string; answer: string | null }   // answer = canonical name when correct
   type GuessAction =
     | { type: "guess"; text: string }
     | { type: "giveUp" }
     | { type: "hint" };
   function initialGuessState(answerState: AnswerState, answers: AnswerCandidate[]): GuessState;
   function guessReducer(state, action, config: { answers; answerCount; isKnownGuess?; hintsTotal }): { state: GuessState; result?: GuessResult };
   ```
   Because a reducer passed to `useReducer` can't take extra arguments, either export a factory `createGuessReducer(config)` or have `MapGame` call a pure `applyGuess(state, action, config)` and store the result with `useState`. Pick one. The logic must be a plain function either way, so it's easy to unit test.
   It must reproduce today's `submitGuess` order exactly: over → empty (after trim) → duplicate → unknown (`isKnownGuess`) → wrong / correct. The one deliberate difference: a guess is a **duplicate** if its normalised text matches an earlier guess's text, **or** if it matches a candidate whose canonical name is already found (deliberate change 2). For the duplicate feedback, use the canonical name when the guess was an alias ("You've already guessed Germany."). Found answers are stored and shown as the **canonical** name. Today they show what the player typed, so "germany" shows as "germany". Showing "Germany" is part of deliberate change 2. If a test pins the typed text, update it.
   When the round is already answered, the initial state lists every candidate's name as found. That matches today's behaviour.
3. `MapGame`:
   - Replace the `correctAnswersFunction` and `isAnswerCorrect` props with `answers: AnswerCandidate[]`.
   - Remove the redundant `answerState` prop; read `round.answerState`.
   - Call `submitAnswer(CORRECT_ANSWER)` / `submitAnswer(GAVE_UP)` when the reducer result says the round just ended. Don't call it inside the reducer.
   - Add an `answerNoun: string` prop ("country", "river"). `MapGameInput` uses it in the unknown-guess message: `We don't recognise "${guess}" as a ${answerNoun}. Check the spelling.`, so Tripoint's text is unchanged.
4. `Tripoint` passes `answers={tripoint.countries}`, and `RiverShapes` passes `answers={[river]}`. Delete their `correctAnswers`, `checkAdditionalAnswers` and `isAnswerCorrect` functions. `Tripoint`'s `knownNames` set stays (it covers every country, not just the answers), but build it with `normaliseString` exactly as now.

**Verify (standard).** Smoke test, Tripoint: guess a country by an alias (check `geodata/countries/additionalNames.json`), then by its main name. The second guess is reported as a duplicate and only one slot fills. Also check a wrong guess, an unknown guess (shakes), give up, and that revisiting a finished round shows all answers.

## Phase 4: hints and maps

Goal: no game mutates its round data, and maps share one pattern.

1. **Hint count reaches the map.** Add `hintsUsed: number` to `MapProps<T>` (now `{ data: T; gameOver: boolean; gaveUp: boolean; hintsUsed: number }`), and have `MapGame` pass it from the guess state. Change `HintConfig` to `{ total: number; description: string; text?: (hintIndex: number) => string | null }`. Hints that change the map, like Tripoint's, need no text function.
2. **Tripoint hints without mutation.** Today `showRandomCountryBorder` sets `showAsHint = true` on an object inside the state, and the map filters on it. Instead:
   - In `Tripoint`, build a random reveal order once per round: `const [hintOrder] = useState(() => shuffle(tripoint.countries.map(c => c.name)))`. Put a small `shuffle` helper (Fisher–Yates, returns a new array) in `helper/`. If the `react-hooks/purity` lint rule flags `Math.random` inside the initializer, add a one-line disable with a reason, as the old code did.
   - Pass `data={tripointRound}` to `MapGame`, where `tripointRound` is `{ ...tripoint, hintOrder }`, memoised or held in state so it stays stable.
   - `TripointMap` shows the borders of `hintOrder.slice(0, hintsUsed)` while the round is in play.
   - Delete `showAsHint` from the model, and delete `showRandomCountryBorder` and Tripoint's `useState(tripoint)`.
3. **Maps take `MapProps<T>` directly.** `TripointMap` and `RiverShapesMap` accept `MapProps<T>` (`data`, not `tripoint` / `river`). Each wrapper becomes just:
   ```tsx
   const TripointMapWrapper = dynamic(() => import("./TripointMap"), { ssr: false, loading: () => <LoadingSpinner /> });
   export default TripointMapWrapper;
   ```
   Keep the `dynamic(() => import("./X"), {...})` call **literally in each wrapper file**. Don't hide it behind a shared helper function; Next's compiler needs to see the import call where `dynamic` is used.
4. **Shared map constants.** Put the satellite tile URL (duplicated now) and the answer colours (`"LightGoldenRodYellow"`, `"darkred"`) in `map/constants.ts`, and use them in both maps. Move `TripointMap`'s `L.icon(...)` to module scope so it isn't rebuilt on every render.

**Verify (standard).** Smoke test, Tripoint: each hint shows one more country outline, in random order, until none are left. All borders show on give up / finish. Rivers: the continent hint text still shows, the map is blank before you answer and satellite after.

## Phase 5: game module registry

Goal: one folder per game, one registry, and generic shells that never mention a specific game.

1. `games/types.ts`:
   ```ts
   export interface GameComponentProps<T> {
     data: T;
     round: Round;
     answerCount: number;
     submitAnswer: (state: Exclude<AnswerState, "UNANSWERED">) => void;
   }

   export interface GameModule<T> {
     type: string;          // must equal the backend GameType name, e.g. "TRIPOINT"
     title: string;         // "Tripoints": home card and page title
     singular: string;      // "Tripoint": summary rows
     description: string;   // home card text
     Icon: Icon;            // react-bootstrap-icons
     Component: React.ComponentType<GameComponentProps<T>>;
     answerNames: (data: T) => string[];   // summary page
   }

   export type AnyGameModule = GameModule<unknown>;
   export function defineGame<T>(module: GameModule<T>): AnyGameModule;  // the one place the type is erased
   ```
   `data` arrives as `unknown` from the API, and each module trusts that the server sent its own shape. `defineGame` holds the single cast (`as unknown as AnyGameModule`), with a comment explaining why.
2. Per-game folders (use `git mv` so history follows):
   ```
   src/games/tripoint/  index.ts (the module), TripointGame.tsx, TripointMap.tsx, TripointMapWrapper.tsx, model.ts
   src/games/rivers/    index.ts, RiverShapesGame.tsx, RiverShapesMap.tsx, RiverShapesMapWrapper.tsx, model.ts
   ```
   `src/map/` keeps only the game-agnostic Leaflet helpers (`CenterUpdater`, `FitBounds`, `MapResizer`, `MapProps`, `constants`). Nothing under `games/common/` or `map/` imports from a game folder.
3. `games/registry.ts`:
   ```ts
   export const GAME_MODULES: AnyGameModule[] = [tripoint, rivers];
   export function findGameModule(type: string): AnyGameModule | undefined;
   export const RANDOM_OPTION = { type: "RANDOM", title: "Mixed", singular: "Mixed", description: "A random mix of every game.", Icon: Shuffle };
   export const MENU_OPTIONS = [...GAME_MODULES, RANDOM_OPTION];   // type it as the shared subset (type, title, description, Icon)
   export const ROUND_OPTIONS = [3, 5, 10];
   ```
   The `RANDOM` description change ("both" → "every game") is allowed, because "both" goes wrong as soon as a third game exists. Delete `games/gameTypes.ts`.
4. Use the registry everywhere:
   - `GameRoundPage.renderGame`: look up `findGameModule(view.gameType.name)` and render `<module.Component key={currentRound} data={view.data} round={round} answerCount={view.gameType.answerCount} submitAnswer={submitAnswer} />`. Keep the "Unknown game type..." fallback. Keep the `key={currentRound}`; it resets per-round state, and the type can change between rounds of a mixed game.
   - `renderTitle`: `${module.title} | Geogames`, keeping the loading and unknown titles (deliberate change 3).
   - `GameSummary`: the row label is `module.singular` and the answers are `module.answerNames(view.data)`. Fall back to the raw type name and `[]` for unknown types, as today. Move the "Play again" type inference (one type → that type, otherwise `RANDOM`) into a small named function.
   - `HomePage`: iterate `MENU_OPTIONS`; the default selection is `MENU_OPTIONS[0].type`, not the hard-coded `"TRIPOINT"`.
5. After this phase, `grep -rn "TRIPOINT\|RIVERS_BY_SHAPE" client/src` should only hit the two game modules' `type` fields (and tests).

**Verify (standard).** Smoke test: the home page shows all three cards, keyboard arrow navigation between cards still works, and Mixed games switch between game types round to round. The summary shows the right label per row, and "Play again" keeps the game type.

## Phase 6: strict TypeScript

1. Set `"strict": true` in `tsconfig.json`.
2. Fix every error properly: typed props on every component, no implicit `any`, null checks where values really can be missing (for example `useRouter().query.gameId` can be `string | string[] | undefined`; handle it rather than casting). The only cast allowed is the one in `defineGame`. Test files must also compile; fixing types in tests is fine.

**Verify (standard).** `npx tsc --noEmit` reports zero errors.

## Phase 7: prove the modularity, then document it

1. **Dry run.** Temporarily add a throwaway game module, `src/games/dummy/`, with type `"DUMMY"`. It renders `MapGame` over the river map and data, with fixed answers. Register it with one line in `registry.ts`. Confirm that `npm run build` passes and that the home page shows a fourth card, **without touching any other file**. If you had to change anything else, fix the abstraction so you don't, then repeat. Then delete the dummy module and its registry line. (Starting a DUMMY game will fail on the backend; that's expected and doesn't matter.)
2. Update `CLAUDE.md` (frontend parts only):
   - In "Game types", rewrite frontend step 3 as: create `client/src/games/<name>/` with a module built by `defineGame` (fields as in `games/types.ts`), a component (usually built on `games/common/MapGame`), a `*Map.tsx` + `*MapWrapper.tsx` pair taking `MapProps<T>`, and add the module to `GAME_MODULES` in `games/registry.ts`. Answers are given to `MapGame` as `AnswerCandidate[]`, and matching (aliases, duplicates) is handled centrally.
   - In "Architecture", add a short "Frontend structure" paragraph: `api/` owns fetching, query keys and JSON parsing; `games/common/` holds the game-agnostic shell; `games/<name>/` holds one game each; `map/` holds the shared Leaflet helpers; and `QueryClientProvider` lives in `pages/_app.tsx`.
   - In "Leaflet and SSR", say the wrapper is now just the `dynamic(...)` call, and that the map takes `MapProps<T>`.
   - Update "The client checks answers" to point at `games/common/answers.ts`.

**Verify (standard)**, plus a final full smoke test of every game type. If you can't drive a browser, list the manual checks from phases 1–5 for the user in your report.

## Final report

Include:
- The test counts before and after.
- Every test you changed, with the reason.
- Anything you couldn't verify by hand.
- Any other bugs you noticed but didn't fix.
- A tree of `client/src` after the change.
