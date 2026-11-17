# Daily challenge

## Goal

One puzzle a day, the same for everyone, in the style of Wordle:

- 5 rounds, each of a **different standard game type** (never River Shapes, Lake Shapes or Border Shapes, the "Extra difficult" ones), and never the same subject twice.
- Everyone who plays on a given date gets the same 5 rounds, in the same order.
- Each player gets **their own game** (answers are stored per game), built from that day's rounds.
- At the end, a **Share** button copies a spoiler-free result with a link to `/daily` (not to the game).
- The browser remembers today's game, so coming back to `/daily` resumes it, or shows the result once it is finished, instead of starting again.

"No repeats" means within one day's 5 rounds; a subject may come back on a later day. Out of scope for this plan: streaks and history, past dailies, a "no repeats across days" rule (see Follow-ups).

## Decisions (and why)

1. **Pin each day's rounds in the database the first time they are needed.** A seed alone (`new Random(date)`) is not enough. The rounds depend on the code and the data, which change with every deploy, and this repo deploys often, so a merge at lunchtime would give afternoon players a different puzzle from the morning ones and the shared scores would no longer be comparable. The first request for a date generates its rounds with a seeded `Random` and stores them. Every later request copies the stored rows. The seed still matters: two instances racing on the first request generate the same rounds, and tests are reproducible.
2. **Each visit creates a new game from the pinned rounds.** It is a normal `GameEntity` with `gameMode = "DAILY"`, so answering, scoring, the summary, the 24-hour cleanup and the statistics all work unchanged. A shared link must never point at a game id: whoever opens it would see, and could answer, the sharer's game.
3. **The share link is `/daily`, a real static page.** `/games/{id}` can't be reloaded or opened from a link in the jar (the known bug pinned by `e2e/tests/deep-link.spec.ts`), so the daily page shows the game itself: `pages/daily.tsx` renders `GameRoundPage` with the stored game id. The URL stays `/daily` all the way through, so reloading works.
4. **The date is the player's local date**, as in Wordle, so the puzzle changes at the player's midnight and not at 00:00 UTC (which is 11:00 in Sydney). The client sends `date` (`YYYY-MM-DD`). The server accepts it only within one day of its own UTC date, which covers every time zone (UTC−12 to UTC+14). With no `date`, the server uses its own UTC date: the client sends no date when its own clock is rejected.
5. **localStorage is the right tool here, with limits we accept.** It is a convenience (resume, show the result instead of a fresh game) and not enforcement. The server already keeps each round's guesses and hints (`persist-guesses` / `persist-hints`), so reloading mid-round restores them and can't be used to retry a round. But a private window, cleared storage or another device lets a player play again. Real enforcement would need an account or a fingerprint, and the privacy rules (root `CLAUDE.md`) forbid both. Answers are already checked on the client and every round's data can be read through the API, so the game runs on trust anyway. The stored value never leaves the browser, and it is strictly necessary for a feature the player asked for, so it needs no consent banner, but the privacy page must list it (it lists `gg-theme` and `gg-hardcore` today).

## Backend

### 1. `GameType`: mark the extra difficult types

The backend has no notion of "extra difficult" today; it lives only on the frontend modules (`extraDifficult: true` in `games/lakes/index.ts`, `games/pairs/borderShapes.ts` and `games/rivers/shapes.ts`). The constructor already takes four arguments (`answerCount`, `hintCount`, `guessLimit`, `placeKind`). Add a fifth, `boolean extraDifficult`, and an accessor, `true` for `RIVERS_BY_SHAPE`, `LAKES_BY_SHAPE` and `BORDERS_BY_SHAPE`. Don't add it to `GameTypeResponse` (that would change the contract for nothing; the home page needs the flag before any API call).

Keep the two lists in step with a test on each side that spells out the same set:
- Java: `GameTypeTest`: the extra difficult types are exactly those three.
- Vitest: `registry.test.ts`: the modules with `extraDifficult` are exactly `RIVERS_BY_SHAPE`, `LAKES_BY_SHAPE`, `BORDERS_BY_SHAPE`. Each test has a comment pointing at the other.

The daily pool is then the other 8 types: `TRIPOINT`, `RIVERS_BY_COUNTRIES`, `COUNTRIES_BY_NEIGHBOURS`, `COUNTRIES_BY_COASTLINE`, `COUNTRIES_BY_LAND_BORDERS`, `MERGED_COUNTRIES`, `COUNTRIES_BY_CITIES` and `COUNTRIES_BY_RIVERS`.

### 2. `RoundGenerator.generate` takes the `Random`

Every generator holds the shared `Random` bean from its constructor, so a daily can't pass a seeded one. Change the interface to

```java
List<GameData> generate(int count, Set<String> excludedSubjects, Random random);
```

and remove `Random` from the generator constructors and their `@Bean` methods (`config/RiverGamesConfig`, `CountryGamesConfig`, `CountryPairGamesConfig`, plus `TripointFinderService`, which the tripoint generator uses: check how it is wired). `GameService` passes its own `random` for normal games. Update the generator tests to pass their seeded `Random` to `generate`. This is a mechanical refactor: make it its own commit and run `bash scripts/check-java.sh` and `bash scripts/check-it.sh -Dit.test=RoundRebuildIT` before going on.

### 3. `DailyRoundPicker` (pure, unit-tested)

`service/daily/DailyRoundPicker`, a `@Service` that takes `List<RoundGenerator>` (build the same `EnumMap` as `GameService`, or share a small helper):

```java
List<GameData> pick(LocalDate date)
```

1. `Random random = new Random(seedFor(date))`, where `seedFor` is the first 8 bytes of SHA-256 of `"geogauntlet-daily:" + date` as a `long`. Don't use `date.toEpochDay()` itself: `java.util.Random` gives very similar first values for neighbouring seeds, so consecutive days would start alike.
2. The pool is the `GameType` values that aren't `extraDifficult`, in enum order (a stable starting order), shuffled with `random`. Take the first `DAILY_ROUNDS = 5`. That gives 5 different types, already in a random order.
3. For each type in turn, `generators.get(type).generate(1, Set.copyOf(used), random)` and add its `subjectId()` to `used`, as `generateMixedRounds` does. A river and a country game can't then ask about the same subject.
4. Fail at startup (constructor) if the pool has fewer than `DAILY_ROUNDS` types.

Note the limit of the subject rule: `country:France` (Country Gaps) and `pair:France|Spain` (Merged Countries) are different subjects, so France can be an answer twice in one daily. This is the same as Mixed today; accept it for now.

### 4. Migration and `DailyChallengeStore`

Add the next free `V<n>__daily_challenge.sql`. That is V8 today (`V7__round_hints.sql` exists), but check the folder on `main` just before PR 2 merges and renumber if another migration got there first (see "Pull requests"):

```sql
-- The rounds of each day's daily challenge, pinned the first time anyone asks for that date, so
-- a deploy during the day can't change the puzzle. No user data. Never deleted: a few hundred
-- bytes a day.
create table daily_challenge (
    challenge_date date primary key,
    rounds         text not null    -- JSON: [{"gameType": "TRIPOINT", "roundKey": "..."}, ...]
);
```

A single row per date, so writing it is atomic: there is no half-written day for another instance to read.

`service/daily/DailyChallengeStore` uses `JdbcClient`, like `service/stats/RoundStatsStore`, and is **not** `@Transactional`:

- `Optional<List<PinnedRound>> find(LocalDate date)`
- `List<PinnedRound> findOrCreate(LocalDate date, Supplier<List<PinnedRound>> generate)`: `find`; if empty, `generate`, then `insert ... on conflict (challenge_date) do nothing`, then `find` again and return what is stored (the winner's rows if another instance got there first). Check that H2 in PostgreSQL mode accepts `on conflict do nothing`. If it doesn't, catch `DuplicateKeyException` and re-read, as `RoundStatsStore` does. Keep it outside a transaction: on Postgres a failed insert aborts the transaction it is in.

`PinnedRound` is a record `(GameType gameType, String roundKey)`, written with the app's `ObjectMapper`.

Don't cache the pinned rows: reading one row by primary key is cheap, and the memory rules say not to cache per-game data.

### 5. Creating a daily game

`service/daily/DailyChallengeService.createDailyGame(Optional<LocalDate> requested)`:

1. `today = LocalDate.now(clock)` in UTC. Inject a `java.time.Clock` bean (add one to `config/` if there is none) so tests can fix the date.
2. `date = requested.orElse(today)`. A 400 (`IllegalArgumentException`) if it is more than one day either side of `today`, or before `DAILY_EPOCH`.
3. `rounds = store.findOrCreate(date, () -> picker.pick(date) mapped to PinnedRound)`.
4. `GameService` gets a new `@Transactional` method `createGame(List<PinnedRound> rounds, String gameMode)` that builds the `GameRoundEntity`s and saves them, the same way `createGame` does now. Refactor `createGame` to call it.
5. Return `DailyGameResponse(id, date, number)`, where `number = DAYS.between(DAILY_EPOCH, date) + 1` ("Daily #42"). Set `DAILY_EPOCH` to the day this ships.

The service doesn't check that the pinned keys still rebuild. If a deploy removes the data behind one (rare), that round is a 410 like any other missing round. To mend a day, delete its row (`delete from daily_challenge where challenge_date = '...'`) and the next request generates it again. Write this in the backend `CLAUDE.md`.

### 6. API

`POST /api/daily-games`, body `{"date": "2026-11-17"}` or `{}`. The response is `{"id": "<uuid>", "date": "2026-11-17", "number": 42}`. A malformed date, or one out of range, is a 400 `ProblemDetail` (make sure Jackson's parse error for a bad date also becomes a 400, not a 500; check `ApiExceptionHandler`).

Use its own path, not `gameType: "DAILY"` on `POST /api/games`: the daily takes a date and not a round count, and returns more than `{id}`.

`GameEntity.gameMode` is `"DAILY"`. The statistics record it as a new `stat_game_mode` row automatically, so the daily's difficulty can be checked later. Don't store the date on the game: the client knows it, and the statistics already have their own UTC date.

### 7. Backend tests

- `DailyRoundPickerTest` (real generators over the real data, as the generator tests do, or mocks): the same date gives the same rounds; 10 different dates don't all give the same rounds; every pick has 5 distinct types, none of them extra difficult, and 5 distinct subjects; run it over 365 consecutive dates to show that no date throws.
- `DailyChallengeServiceTest` with a fixed `Clock`: the date range (yesterday, today and tomorrow are accepted; two days out and before the epoch are 400s), no date means UTC today, the number.
- `DailyChallengeIT` (no test transaction, like `GameCleanupIT`): two `POST /api/daily-games` for the same date give different game ids with identical rounds (compare `jsonBlob`s round by round); a row already in `daily_challenge` is used as it is, even if the picker would now choose differently (insert a hand-written row first); two threads creating the same new date both get the same rounds and the table has one row.
- `PostgresMigrationIT` covers the new migration automatically. Check that `on conflict` works there too.
- `ApiContractIT`: add the new endpoint to the contract and regenerate `client/src/test/api-shapes.json` (root `CLAUDE.md`, "Contract test").
- `HttpServerIT`: `GET /daily` returns the page (see step 8). This goes in PR 3, with the page.

### 8. Serving `/daily` from the jar

`config/WebConfig`: add `registry.addViewController("/daily").setViewName("forward:/daily.html")`, next to `/privacy`, and update its Javadoc.

## Frontend

### 9. `api/daily.ts`

- `localDateString(now = new Date())`: `YYYY-MM-DD` from `getFullYear()`, `getMonth() + 1` and `getDate()`. **Never `toISOString()`**: that is the UTC date, which is the wrong day for half the world for part of every day.
- `useCreateDailyGame()`: a mutation that posts `{date}` to `/api/daily-games`. If that is a 400, it posts `{}` once more (the device's clock is wrong, so use the server's date). Unlike `useCreateGame`, it does **not** navigate: it returns the response.
- Add a fixture for the new response to `src/test/fixtures.ts` (e.g. `makeDailyGameResponse()`) and a `["POST /api/daily-games", makeDailyGameResponse()]` row to the endpoint table in `contract.test.ts`, next to the guesses and hints rows, so the contract passes. (This part goes in PR 2.)

### 10. `daily/dailyStorage.ts`

One localStorage key, `gg-daily`, holding the latest daily only:

```ts
interface DailyEntry {
  v: 1;
  date: string;         // the player's local date it was started on
  number: number;
  gameId: string;
  hardcore: boolean;    // Hardcore mode was on for the whole game so far (step 13)
  result?: {            // set once the game is finished
    score: number;
    maxScore: number;
    rounds: ("perfect" | "hints" | "revealed")[];
  };
}
```

- **Copy the pattern of `settings/hardcoreMode.ts`**, which already solves the same problems for `gg-hardcore`: a `useDailyEntry()` hook built on `useSyncExternalStore`, whose server snapshot is `null` (so the static pre-render and the first client render agree, with no hydration error), subscribed to the `storage` event (another tab's change shows up at once) and to a custom `gg-daily-change` event that `writeDaily` fires (this tab's changes), and an in-memory fallback when storage throws. `useSyncExternalStore` compares snapshots with `Object.is`, so `read()` must return the same object while the stored string is unchanged: cache the last raw string and its parsed value, or the hook re-renders forever.
- `readDaily()` / `writeDaily(entry)`. Wrap every access in `try/catch` (storage can be disabled, full, or throw in some private modes), and ignore anything that doesn't parse or has the wrong `v`. The daily must still work with no storage: it just can't resume.
- Keep only the latest entry, so storage doesn't grow. An entry for another date counts as no entry.
- Store the **result** as well as the game id, because the game is deleted about 24 hours after it is made (created time rounded down to the hour), so a player who comes back late in the evening may find it gone. The result is enough to show the score and the Share button with no server.

### 11. `pages/daily.tsx` and `daily/DailyPage.tsx`

The page is pre-rendered at **build time** (static export), so nothing may read the date or localStorage while rendering: the build date would be baked into the HTML, and the server and client renders would differ (a hydration error). Storage comes through `useDailyEntry()` (its server snapshot is `null`). Work out today's date in a `useEffect` and show a spinner until then.

States:

1. **No entry for today**: a short intro ("Daily #—: 5 rounds, one of each kind, the same for everyone today") and a **Play today's challenge** button. Create the game **on click, not on load**: link previews (Slack, WhatsApp, iMessage) and crawlers fetch `/daily`, and they must not create games. Work out the date at click time, so a page left open past midnight starts the new day's game. Just before creating, read storage again (another tab may have started the game meanwhile) and use that entry if it is there. Call `wakeServer()` on mount and show "Waking up the server…" after 3 s, as `HomePage` does.
2. **An entry for today, unfinished**: render `<GameRoundPage gameId={entry.gameId} daily={{ number, date }} />`. It resumes at the first unanswered round, and that round's guesses and hints come back from the server (`restoreGuessState`), so a reload mid-round loses nothing. Unlike `/games/{id}`, a reload of `/daily` works in the jar (step 8), so the browser test needs no `reloadGamePage` workaround. If the game is gone (round 0 is a 404), clear the entry and go to state 1. That lets the player replay from the start, which we accept.
3. **An entry for today, finished**: show the stored result with **Share**, and **See the rounds** if the game still loads (it opens `GameRoundPage` on its summary).

Hardcore: when the game is created, set `entry.hardcore` from `useHardcoreMode()`. While the game is unfinished, if the hook ever reads `false`, set it to `false` and leave it there. The game panel has its own compact switch, so a player can turn Hardcore off mid-game, and only a game played in Hardcore throughout earns the marker.

Saving the result: while the game is shown, `DailyPage` watches `useRound(gameId, 0).data?.gameResult`. Every answer patches each cached round's result, so round 0's changes too. When `gameState` becomes `FINISHED`, write `result` into the entry, building `rounds` with `isPerfect` and `isRevealed` from `games/common/`.

Give the page a `<title>` and the same header (`HomeLink`, `ThemeToggle`) as the other pages.

### 12. `GameRoundPage` / `GameSummary` / `GameTopBar`

- `GameRoundPage` takes an optional `daily?: { number: number; date: string }` and passes it down.
- `GameTopBar` (or wherever the game title shows) says "Daily #42" for a daily game.
- `GameSummary` with `daily`: replace **Play again** (it would start a Mixed game, or a single-type game whenever the rounds happen to match) with the share panel (step 13) and a "Play a Mixed game" button (`createGame("RANDOM", 5)`). Without `daily`, nothing changes.

### 13. Share

`daily/shareText.ts` (pure, unit-tested):

```
GeoGauntlet Daily #42
430 / 500
🟩🟨🟩🟥🟩
https://geogauntlet.onrender.com/daily
```

- One square per round, in round order: 🟩 perfect (correct, no hints), 🟨 correct with hints, 🟥 revealed (gave up or out of guesses). No answers, no game type names, no hint counts: the squares are all a recipient should see before playing.
- If `entry.hardcore` is still `true`, the first line becomes `GeoGauntlet Daily #42 · Hardcore`, as Wordle marks hard mode. The distance clues don't change the score, so the score line is the same either way. The marker only tells others the score was earned without them. It is self-reported, like everything else on the client.
- The link is `window.location.origin + "/daily"`, so it is right on Render, in the jar and in tests. Never hard-code the host.

`daily/ShareButton.tsx`:

- On click, `navigator.clipboard.writeText(text)`. It only works in a secure context (https or localhost) and in response to a click, so call it straight from the handler with no `await` before it.
- On success, show "Copied!" in an `aria-live="polite"` region for a few seconds.
- If `navigator.clipboard` is missing or the promise rejects, show the text in a read-only `<textarea>`, selected, with "Copy this text". Don't use `document.execCommand("copy")` (deprecated).
- Optional: when `navigator.share` exists (mostly phones), add a second **Share…** button that opens the system share sheet with `{ text }`. Don't replace the Copy button: some desktop browsers have `navigator.share` with a poor share sheet.
- Use `Button` from `src/ui/` and theme tokens only (`npm run lint` rejects colour literals). The emoji are in the shared text, not in our CSS, so they don't count.

### 14. Home page

Add a **Daily challenge** card above the "Choose a game" heading (and so above the `HardcoreToggle` under it). It is a `Link` to `/daily` and creates no game. Read `gg-daily` with `useDailyEntry()` (no hydration issue), and today's date after mount: if today is finished, show "Done: 430 / 500"; if it is started, show "Continue". Keep it outside the radio group, so arrow-key navigation through the game cards doesn't change.

### 15. Privacy page

`components/PrivacyPage.tsx` and its test:

- The "On your device" paragraph lists `gg-theme` and `gg-hardcore` today ("up to two items"). Make it "up to three", and add `gg-daily`: today's daily (the game id, whether Hardcore was on and, once finished, the score and how each round ended). It is set only when you play the daily, replaced the next day, never sent anywhere, and cleared with the site data.
- One sentence on the daily: everyone gets the same rounds on a date; the server stores which rounds those are for each date, and nothing about who played them.
- Copying a result writes to your clipboard only when you click Share.
- Update "Last updated" to the day PR 3 merges.

### 16. Frontend tests (Vitest)

- `localDateString`: just before and just after local midnight with fake timers and a non-UTC `TZ` (or a hand-built `Date`); the result is the local date, not the UTC one.
- `dailyStorage`: round-trips; ignores bad JSON, the wrong `v`, an old date; works when `localStorage` throws (stub `getItem`/`setItem` to throw); a `storage` event from another tab updates `useDailyEntry`; the snapshot is the same object while the stored string is unchanged. Model these on `settings/hardcoreMode.test.tsx`.
- `shareText`: the squares for each round state, the score line, the link from a given origin, the Hardcore marker only when `hardcore` is true.
- `DailyPage` and Hardcore: on at the start and throughout keeps `hardcore: true`; turned off mid-game sets it to `false` for good.
- `ShareButton`: success shows "Copied!"; a rejected or missing `clipboard` shows the textarea.
- `DailyPage`: no entry → the button, and nothing is posted until it is clicked; a click posts the local date and stores the entry; a 400 retries with `{}`; an unfinished entry renders the game; a 404 on round 0 clears the entry; a finished entry shows the result without fetching; finishing the last round writes `result`.
- `GameSummary` with `daily`: Share instead of Play again.
- `HomePage`: the daily card links to `/daily` and shows Done or Continue from storage.
- `registry.test.ts`: the extra difficult set (step 1).

### 17. Browser test

`e2e/tests/daily.spec.ts` against the jar (read `e2e/CLAUDE.md` and reuse the helpers in `fixtures.ts`):

1. Open `/daily` and click Play. Give up on all 5 rounds (or answer from the round data, as `every-game.spec.ts` does if it does).
2. The summary shows the share panel. Grant `clipboard-read` / `clipboard-write` (Chromium) and check that the copied text has `Daily #`, five 🟥 and `/daily`.
3. Reload `/daily`: the result shows and no new game is created (count `POST /api/daily-games` requests with `page.on("request")`).
4. In a new browser context (empty storage), open `/daily` and play: the first round has the same subject as before. This shows two players get the same daily.

## Pitfalls and edge cases (checklist)

| Case | What happens |
|---|---|
| Deploy during the day | The rounds are pinned in `daily_challenge`, so later players get the same puzzle. |
| Two instances, first request of the day | `on conflict do nothing`, then re-read: both use the winner's row (and both generate the same rounds anyway). |
| A pinned round's data removed by a deploy | That round is a 410. Fix: delete the day's row. Documented in the backend `CLAUDE.md`. |
| Player in UTC+14 or UTC−12 | Their local date is within ±1 day of the server's UTC date, which is accepted. |
| Device clock wrong by days | 400, then the client retries with no date and gets the server's UTC date. |
| Launch day, player whose date is before `DAILY_EPOCH` | 400, then retry with no date. Or set the epoch a day early. |
| Page open across midnight | The date is read when Play is clicked, not when the page loads. A game started before midnight is finished as that day's daily (its entry keeps its own date). |
| Started yesterday, not finished, opened today | The entry's date isn't today, so it counts as none: today's daily starts and yesterday's is forgotten. Accepted. |
| Two tabs | `useDailyEntry` hears the other tab's `storage` event, so a second tab picks up the first tab's game straight away, and storage is read again just before creating. Only a true race makes two games, and the last one written wins. Harmless. |
| Reload mid-round | The round's guesses and hints come back from the server, so a reload can't be used to retry a round. |
| Hardcore turned off mid-game | `entry.hardcore` becomes `false` for good, so the share text has no Hardcore marker. |
| Game deleted after about 24 h | The finished result is in storage, so Share still works. If it wasn't finished, the entry is cleared and the daily can be played again from the start. Accepted. |
| Private window, storage disabled or cleared, another device | The daily can be played again. This is a convenience, not enforcement (decision 5). |
| Link previews and crawlers fetching `/daily` | Static HTML with no game made on load: games are created only on click. |
| Build-time pre-render | No `Date` during render (read it in an effect). Storage comes through `useSyncExternalStore` with a `null` server snapshot. |
| Clipboard unavailable (http, old browser, permissions) | The textarea fallback. |
| `toISOString()` for the date | Wrong (UTC). Use `localDateString`. |
| Predictable future puzzles | The seed is public in the source, so anyone can work out tomorrow's rounds. Accepted: answers are already checked on the client and all round data can be read through the API. If it ever matters, add a secret salt property (`geogames.daily.salt`, from an env var on Render). |
| Sharing a game link instead | Never: anyone with it could see or answer that player's game. The share link is always `/daily`. |
| `/daily` on the Render static site | Works: `https://geogauntlet.onrender.com/privacy` loads directly (checked 2026-11-17), and the same mechanism serves `daily.html`. Add `/daily` to `smoke-production.yml` so a hosting change is caught. |
| Memory | Nothing new is cached. The pinned row is read by primary key on each creation. |
| Privacy | `daily_challenge` holds dates and round keys only. The game itself is an ordinary game. Nothing new is logged. |

## Pull requests

Three stacked PRs. **Every PR must pass the required `test` check against its own base, and `main` must be green and deployable after each merge.** So no PR may depend on code from a later one, and no PR may leave a check failing for a later PR to fix.

| PR | Branch | Base | Contents |
|---|---|---|---|
| 1 | `daily-1-generator-random` | `main` | Step 2 only: `generate(count, excludedSubjects, random)`, `Random` removed from the generator constructors and beans, tests updated. No behaviour change. |
| 2 | `daily-2-backend` | `daily-1-generator-random` | Steps 1 (Java half: `GameType.extraDifficult`, `GameTypeTest`) and 3–7: picker, migration, store, service, `POST /api/daily-games`, their tests, the regenerated `client/src/test/api-shapes.json` **and the matching fixture in `client/src/test/fixtures.ts` plus its row in `contract.test.ts`** (otherwise `contract-frontend` fails; this one part of step 9 moves here). Docs: the root `CLAUDE.md` API list and `daily_challenge` under "Storage and privacy", and the backend `CLAUDE.md` "Daily challenge" section (pinning, the date range, how to mend a day). |
| 3 | `daily-3-frontend` | `daily-2-backend` | Step 8 (the `/daily` forward in `WebConfig` and its `HttpServerIT` test; they need `daily.html`, which only exists from this PR), step 1's Vitest half, steps 9–17 (except the fixture), `/daily` in `smoke-production.yml`, the privacy page. Docs: the `/daily` forward next to `/privacy` in the root `CLAUDE.md`, and the `daily/` folder and localStorage rules in `client/CLAUDE.md`. |

Why each one is safe to merge alone:
- **PR 1** is a refactor: the same games come out, and every existing test still runs.
- **PR 2** adds an endpoint nothing calls yet, plus one table. Deployed alone, users see no change.
- **PR 3** is the only PR users see, and it lands on a backend that already has the endpoint.

### Working on the stack

- Before opening each PR, run `bash scripts/check-java.sh`, `bash scripts/check-frontend.sh`, `bash scripts/check-it.sh` and `bash scripts/check-no-skipped-tests.sh` on that branch. PR 3 also needs the e2e suite.
- Branch each PR from the one below it, and open it **with the one below as its base**, so its diff shows only its own changes. CI's `pull_request` trigger has no branch filter, so stacked PRs get the full check.
- When a lower branch changes (review fixes, or `main` moved on), **merge** it up the stack: `main` → 1 → 2 → 3. Don't rebase. Merging needs no force-push and keeps the review comments attached.
- Merge on GitHub with **"Create a merge commit"**, as this repo already does, never squash. A squash merge gives the lower PR new commit ids, so every PR above it shows the lower PR's changes again and conflicts until it is rebased.

### Order of merging

**Recommended: one at a time into `main`, bottom up.**

1. Merge PR 1 into `main`.
2. Change PR 2's base to `main` (GitHub does this by itself if the merged branch is deleted; otherwise edit the base). Merge `main` into `daily-2-backend`, wait for `test` to pass, and check the migration number against `main` (renumber on the branch if another migration landed: the migration has only ever run on test databases, so renaming it is safe until it reaches `main`). Merge PR 2.
3. **Wait for Render to deploy the backend**, then check the endpoint: `POST https://geogames-web-service.onrender.com/api/daily-games` with `{}` returns an id, a date and `number: 1`. This is the one manual call to production in the plan; run it by hand, not from a test.
4. Change PR 3's base to `main`, merge `main` into `daily-3-frontend`, wait for `test`, and merge PR 3. After the static site deploys, open `/daily` on `geogauntlet.onrender.com` and play it once.

**Allowed alternative: fold the stack, then merge once.** Merge PR 3 into `daily-2-backend`, then PR 2 into `daily-1-generator-random`, waiting for `test` on the receiving PR after each step. Then merge PR 1 (now holding everything) into `main`. This gives one deploy, but the backend and the static site then deploy at the same time. For a few minutes the site can be live before the backend, so the daily's Play button would show "Couldn't start" until the backend is up. Use this only if the PRs were reviewed together anyway. **Never** merge PR 2 or PR 3 into `main` before the PRs below it: their diff against `main` would include the lower PRs without their review.

Don't set `DAILY_EPOCH` until the day PR 2 merges, so "Daily #1" is the first day anyone can play. If PR 3 lands days later, the first daily players see starts at a higher number. Either accept that, or move the epoch in PR 3 (no stored data depends on it: the number is computed on each request).

## Follow-ups (not in this plan)

- Streaks and a stats panel, kept in `gg-daily` as a short history (the privacy page would need updating again).
- No repeats across days: exclude the subjects of the last N days' pinned rounds. This needs the subject id stored next to the round key (they differ for tripoints).
- Playing past dailies from `daily_challenge`.
- A per-round type icon in the share text (the same for everyone, so no spoiler), if players want it.
