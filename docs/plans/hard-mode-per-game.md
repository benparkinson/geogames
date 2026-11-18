# Plan: make Hard mode a fixed setting of each game

## Goal

- Hard mode is chosen before a game starts and cannot change during it. Remove the switch from the round panel.
- The choice is stored with the game, so a refresh, a new tab or another device shows the game the same way.
- The choice is recorded in the anonymous statistics, so hard and normal play can be compared.

Hard mode still only hides the distance and direction clues. Scoring, guess limits and hints do not change.

## Before you start

1. **Do not work in the main checkout.** Use the worktree below, so this change stays separate from whatever is checked out in `c:\work\git\geogames`. This plan may be uncommitted there, in which case the worktree will not contain it: read it from `c:\work\git\geogames\docs\plans\hard-mode-per-game.md`, or copy it into the worktree's `docs/plans/` so it is committed with the change.
2. **PR #155 (`compact-round-panel`) must be merged first.** It changes `MapGame.tsx`, `GuessForm.tsx`, `RoundBrief.tsx` and many game tests, and this plan is written against that version. Check with `gh pr view 155 --json state`. If it is not `MERGED`, stop and tell the user.
3. Create a worktree from up-to-date main:
   ```
   git fetch origin
   git worktree add ../geogames-hard-mode -b hard-mode-per-game origin/main
   ```
4. Read the root, backend, `src/main` and `client` `CLAUDE.md` files in the worktree.
5. Find the highest migration in `src/main/resources/db/migration`. This plan assumes `V9`, so the new one is `V10`. Use the next free number if that has changed.

Line numbers are deliberately left out. Find the code by name.

## Design decisions

These are settled. Do not revisit them while implementing.

| Decision | Choice | Reason |
| --- | --- | --- |
| Source of truth during a game | `game_entity.hard_mode`, returned on every `GameRound` as `hardMode` | Survives refresh and works across devices |
| What `gg-hardcore` in localStorage means | Only the preselected value for the **next** game | The home and daily start screens keep their switch |
| Stored type | Nullable boolean in `game_entity` and `stat_game` | `NULL` means unknown: a game from before this change, or an old client that did not send it. Same convention as `wrong_guesses` |
| API read value | Plain `boolean`; `NULL` reads as `false` | The client needs no third state |
| Where the stat lives | `stat_game`, not `stat_round` | It is per game, and `stat_round` must stay at 8 columns |
| Game with no clues (Tripoints, rivers, lakes) | Client sends `false` even if the preference is on | `hard_mode = true` must mean the player chose it for a game where it can matter |
| Play again | Uses the finished game's `hardMode` | The player repeats the same game |
| "Play a Mixed game" after a daily | Uses the finished daily's `hardMode` | Same rule, one code path |
| Indicator during the round | None. Do not add one to `RoundBrief`, `RoundStatus` or the top bar | After PR #155 the round title and question share one wrapping row, so extra text pushes the question onto another line on a phone |
| Indicator on the final summary | `Hard mode` at the end of the existing breakdown line, for normal and daily games | Adds no extra line |
| Daily share text | Keeps its existing ` · Hard mode` title suffix, now driven by the game's stored setting | Shared results must match how the game was played |

## Step 1: database

Create `V10__hard_mode.sql`:

```sql
-- Hard mode is chosen when a game starts and never changes. NULL means unknown: a game created
-- before this column existed, or by a client that did not send the setting.
alter table game_entity add column hard_mode boolean;

-- The same value for statistics. Rows from before this migration stay NULL.
alter table stat_game add column hard_mode boolean;

drop view round_result;

create view round_result as
select g.played_on      as answered_on,
       g.play_id,
       m.name           as game_mode,
       g.rounds_in_game,
       r.round_number,
       t.name           as game_type,
       k.round_key,
       a.name           as answer_state,
       r.hints_used,
       r.wrong_guesses,
       t.hint_count,
       t.guess_limit,
       r.points,
       g.hard_mode
from stat_round r
join stat_game g         on g.id = r.game_id
join stat_game_mode m    on m.id = g.game_mode_id
join stat_game_type t    on t.id = r.game_type_id
join stat_round_key k    on k.id = r.round_key_id
join stat_answer_state a on a.id = r.answer_state_id;
```

Do not backfill, and do not add a default. Do not edit any earlier migration.

## Step 2: backend

**`api/GameCreationRequest`**: add `Boolean hardMode` (boxed, so a missing field is `null`). Document that `null` means an old client.

**`api/DailyGameRequest`**: add `Boolean hardMode`. Update the Javadoc.

**`api/GameRound`**: add `boolean hardMode` as the last component.

**`dao/GameEntity`**
- Add `@Column(updatable = false) private Boolean hardMode;` with a comment explaining `null`.
- Replace the two-argument constructor with `GameEntity(List<GameRoundEntity> rounds, String gameMode, Boolean hardMode)`. Do not keep a two-argument overload.
- Add `getHardMode()` returning `Boolean` (for statistics) and `isHardMode()` returning `Boolean.TRUE.equals(hardMode)` (for the API).

**`service/GameService`**
- `createGame(String gameType, int numberOfRounds, Boolean hardMode)`
- `createGame(List<PinnedRound> rounds, String gameMode, Boolean hardMode)`
- `toResponse` passes `game.isHardMode()`.
- `publishRoundAnswered` passes `game.getHardMode()`.

**`service/daily/DailyChallengeService`**: `createDailyGame(Optional<LocalDate> requested, Boolean hardMode)`, passed through. Update the `{@link}` in the class Javadoc.

**Controllers**
- `GameController.createGame` passes `request.hardMode()`.
- `DailyGameController` passes `request == null ? null : request.hardMode()`.

**Statistics**
- `service/stats/RoundAnswered`: add `Boolean hardMode` as the last component.
- `model/stats/RoundResult`: add `Boolean hardMode` as the last component, matching the view. Say in the Javadoc that `null` is unknown and is not `false`.
- `RoundStatsRecorder`: pass it through.
- `RoundStatsStore`:
  - Add `hard_mode` / `:hardMode` to `GAME_INSERT`.
  - In `findOrCreateGame`, add `"hardMode", new SqlParameterValue(Types.BOOLEAN, row.hardMode())` to the `Map.of`.

  **`Map.of` throws on a null value**, so the raw `row.hardMode()` cannot go in. `SqlParameterValue` is a non-null wrapper and also gives Postgres the type of a null. `RoundStatsStoreTest` must prove the null case (Step 5). If it fails, switch the map to a `HashMap` holding the same `SqlParameterValue`.

## Step 3: frontend

**API (`client/src/api/`)**
- `types.ts`: add `hardMode: boolean` to `GameRoundResponse` and `GameRoundView`.
- `games.ts`:
  - `toRoundView` copies `hardMode`.
  - `useCreateGame().createGame(gameType, numberOfRounds, hardMode: boolean)` sends all three in the body.
- `daily.ts`: the mutation takes `{ date, hardMode }`. The first POST sends both. The fallback after a 400 sends `{ hardMode }`, so a retry keeps the choice.

**Round model**
- `games/common/Model.ts`: add `hardMode: boolean` to `Round`, documented as fixed when the game was created.
- `games/common/GameRoundPage.tsx`: `buildRound` sets `hardMode: data.hardMode`.

**`games/common/MapGame.tsx`**
- Remove the `useHardMode` and `HardModeToggle` imports, the `useHardMode()` call and the whole `round-settings` block.
- Every `showClues={!hardMode}` becomes `showClues={!round.hardMode}`.
- Keep the `clueFor` prop and its behaviour.

**Toggle and CSS**
- `components/HardModeToggle.tsx`: remove the `compact` prop and its branches. It is now only the start-screen form.
- `client/src/styles.css`: delete `.round-settings`.
- `settings/hardMode.ts`: update the comment. This is the preference for the next game, and each game keeps its own setting on the server.

**Starting a game**
- `components/StartPanel.tsx`: read `useHardMode()` and call `createGame(option.type, rounds, !!option.hasClues && hardMode)`.
- `games/common/GameSummary.tsx`: both buttons pass `all[0].hardMode`:
  - `createGame(RANDOM_OPTION.type, 5, all[0].hardMode)`
  - `createGame(gameType, totalRoundCount, all[0].hardMode)`

**Final summary (`games/common/GameSummary.tsx`)**

Add `Hard mode` to the end of the breakdown line, keeping the `0 perfect` fallback for the outcome part:

```tsx
const outcomes = breakdown.length > 0 ? breakdown : ["0 perfect"];
const summaryLine = [...outcomes, ...(all[0].hardMode ? ["Hard mode"] : [])].join(" · ");
// ...
<p className="gg-text-muted mb-3">{summaryLine}</p>
```

This gives, for example, `3 perfect · 1 with hints · Hard mode`. It applies to daily summaries too.

**Daily (`client/src/daily/`)**

`DailyEntry.hardcore` stays, under the same stored name, because the finished page must work after the server has deleted the game. It is no longer tracked during play.

- `DailyPage.tsx`, `Intro`: call `mutate({ date, hardMode }, ...)`. Keep writing `hardcore: hardMode` in `onSuccess`.
- `DailyPage.tsx`, `Game`:
  - Remove `useHardMode` and the effect that turns `hardcore` off.
  - When the game finishes, take the value from the server, in the same `writeDaily` as the result. Do not use a second effect: two writes from one stale `entry` would overwrite each other.
  ```ts
  const game = first.data;
  // ...
  useEffect(() => {
    if (game?.gameResult.gameState === FINISHED && !hasResult) {
      writeDaily({ ...entry, hardcore: game.hardMode, result: resultFromGame(game.gameResult) });
      onFinished();
    }
  }, [game, hasResult, entry, onFinished]);
  ```
  Keep the 404 effect.
- `useDailyShareText.ts`: take the round (`GameRoundView | undefined`) instead of `gameResult`, and use `round.hardMode`. It no longer needs `useDailyEntry`. `GameSummary` passes `all[0]`.
- `shareText.ts`: no change. Its title already ends in ` · Hard mode` when `hardMode` is true. The summary's share button gets the value from the round, and the `Finished` page gets it from `entry.hardcore`, which holds the server's value once the game has finished.
- `dailyStorage.ts`: change the `hardcore` comment to "Hard mode was chosen for this game".

## Step 4: documentation and privacy

- `client/src/components/PrivacyPage.tsx`:
  - "What we store": add that a game records whether Hard mode is on.
  - "Anonymous statistics": add "and whether Hard mode was on".
  - Update "Last updated" to the implementation date.
- Root `CLAUDE.md`: add `hardMode` to `POST /api/games`, `POST /api/daily-games` and the `GameRound` description.
- Backend `CLAUDE.md`:
  - Persistence: `game_entity.hard_mode`, nullable, fixed at creation.
  - Usage statistics: `stat_game.hard_mode` and the `round_result` column, with `NULL` meaning unknown. It is a game setting the player chose, so it is allowed under the anonymity rules, which otherwise forbid browser data.
- `client/CLAUDE.md`:
  - Rewrite the end of "Distance clues". The server now stores the setting, `MapGame` reads `round.hardMode`, and `gg-hardcore` is only the preference for the next game. The switch is on the start screens only.
  - Update the daily section: `hardcore` is set at the start and replaced by the server's value when the game finishes.
  - Say that Hard mode is shown on the final summary and in the daily share text, and deliberately not in the round panel.

## Step 5: tests

**Backend**
- Update every caller for the new signatures: `GameServiceTest`, `GameControllerTest`, `DailyGameControllerTest`, `DailyChallengeServiceTest`, `GameRepositoryTest`, `GameResultsTest`, `GameCleanupIT`, `RoundStatsRecorderTest`, `RoundStatsStoreTest`.
- `RoundStatsStoreTest`:
  - Give `row(...)` a hard mode value.
  - Add `hardMode` to the params in `aLostInsertRaceReadsTheOtherWritersGameRow`, because `GAME_INSERT` now needs it.
  - Add tests: `true` and `false` read back from `round_result`, and `null` reads back as null and not `false`.
- `GameServiceTest`: the setting reaches the entity, the `GameRound` and the `RoundAnswered` event; a `null` setting gives `hardMode == false` in the response and `null` in the event.
- `GameControllerTest` and `DailyGameControllerTest`: `hardMode` is passed through; a body without it passes `null`.
- `GameApiIT`: create with `hardMode: true`, and every round returns `true`; create without it, and rounds return `false`.
- `RoundStatsIT`: a game created with `hardMode: true` gives `hard_mode = true` on its rows; one created with no field gives `NULL`.

**Contract**
```
bash scripts/check-it.sh -Dcontract.update=true -Dit.test=ApiContractIT -Dskip.frontend
```
Commit the updated `client/src/test/api-shapes.json`, and add `hardMode: false` to the round fixture in `client/src/test/fixtures.ts` and to `makeRoundProps`.

**Frontend**
- `MapGame.test.tsx`:
  - Replace the test that switches Hard mode on in the panel with: `round.hardMode: false` shows the clue, and `true` hides it.
  - Change "shows no clue when Hard mode was already on" to use `makeRoundProps({ hardMode: true })`.
  - Delete "puts the Hard mode switch after the wrong guesses".
  - Add: no Hard mode switch is rendered for a game with `clueFor`.
  - Add: `gg-hardcore` set to `on` with `round.hardMode: false` still shows clues.
- `PlaceGame.test.tsx`: remove the switch assertions and reword the two test names.
- `neighbours.test.tsx`: "gives no clue in Hard mode" uses `hardMode: true` on the round. It should also assert that no `km` text appears, since today it would pass even with a clue.
- `HomePage.test.tsx`: request bodies include `hardMode`. Add: preference on with a clue game or Mixed sends `true`; preference on with a game without clues sends `false`.
- `GameSummary.test.tsx`:
  - Request bodies include `hardMode`, and Play again keeps the game's value even if the preference differs.
  - The breakdown line ends in `Hard mode` for a hard game, including a daily, and does not mention it otherwise.
  - A hard game with no answered rounds shows `0 perfect · Hard mode`.
  - A hard daily's copied text has ` · Hard mode` in its title, and a normal daily's does not, whatever `gg-hardcore` and the stored entry say.
- `games.test.tsx`, `daily.test.tsx`: new arguments and bodies, including the retry after a 400.
- `DailyPage.test.tsx`:
  - The POST body includes `hardMode`.
  - Delete "keeps Hard mode while it stays on" and "turns Hard mode off for good if it is switched off mid-game".
  - Add: finishing stores the server's `hardMode` in the entry along with the result, and the `Finished` page's share text then shows ` · Hard mode`.

**Browser (`e2e/tests/guessing.spec.ts`)**

Extend the Hard mode test, or add one next to it:
- Start Country Gaps with Hard mode on, make a wrong guess, and see no `km`.
- No `group` named "Hard mode" is visible during the round.
- `reloadGamePage(page, id)`: the wrong guess is restored and still has no `km`.
- Clear `gg-hardcore` from localStorage and reload: still no `km`. This proves the server decides.

## Step 6: verify

Run from the worktree:

```
bash scripts/check-java.sh
bash scripts/check-frontend.sh
bash scripts/check-it.sh
NEXT_PUBLIC_API_URL=http://localhost:8080 ./mvnw -B -DskipTests package
cd e2e && npm ci && npx playwright install chromium && npx playwright test
```

`PostgresMigrationIT` needs Docker and is skipped locally without it, so say so if that happens. CI will run it. Search for leftovers: `compact`, `round-settings`, and `useHardMode` outside `settings/`, `HardModeToggle`, `StartPanel` and the daily `Intro`.

Commit to `hard-mode-per-game`. Push or open a PR only if the user asks.

## Rollout behaviour

Nothing needs doing, but report these to the user:

- Games already in progress at deploy have `hard_mode = NULL`. They read as not hard, so a player in one will see clues again. Games last 24 hours, so this clears itself.
- Their statistics are `NULL` rather than wrongly `false`, as are games started from a browser tab still running the old client.
- The backend and static site deploy separately, and either order is safe. With a new client on the old backend, `hardMode` is missing and clues show until the backend is up.

## Using the data

No code is needed. Only compare game types that have clues, because Hard mode changes nothing in the others (a Mixed or daily game also contains rounds without clues).

Round-level difficulty and behaviour:

```sql
select game_type,
       hard_mode,
       count(*)                                                          as rounds,
       round(avg(points), 1)                                             as avg_points,
       round(avg(case when answer_state = 'CORRECT' then 1.0 else 0 end), 3)        as solved,
       round(avg(case when answer_state = 'GAVE_UP' then 1.0 else 0 end), 3)        as gave_up,
       round(avg(case when answer_state = 'OUT_OF_GUESSES' then 1.0 else 0 end), 3) as out_of_guesses,
       round(avg(hints_used), 2)                                         as avg_hints,
       round(avg(wrong_guesses), 2)                                      as avg_wrong_guesses
from round_result
where hard_mode is not null
  and game_type in ('COUNTRIES_BY_NEIGHBOURS', 'COUNTRIES_BY_COASTLINE', 'COUNTRIES_BY_LAND_BORDERS',
                    'COUNTRIES_BY_CITIES', 'COUNTRIES_BY_RIVERS', 'BORDERS_BY_SHAPE', 'MERGED_COUNTRIES')
group by game_type, hard_mode
order by game_type, hard_mode;
```

Whether players finish the game:

```sql
select hard_mode,
       count(*) as games,
       round(avg(case when answered = rounds_in_game then 1.0 else 0 end), 3) as finished
from (
  select play_id, hard_mode, rounds_in_game, count(*) as answered
  from round_result
  where hard_mode is not null
  group by play_id, hard_mode, rounds_in_game
) games
group by hard_mode;
```

Limits on what this shows:
- Players choose Hard mode, so stronger players are likely to be over-represented. The numbers show how the two groups differ, not purely how much harder the mode is.
- `game_mode = 'DAILY'` is the fairest comparison, because everyone gets the same rounds on a date.
- A game with no answered round has no statistics row, so games abandoned before the first answer are not counted.
