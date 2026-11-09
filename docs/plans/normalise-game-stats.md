# Plan: normalise the round statistics

Read `CLAUDE.md` first, then `docs/plans/game-stats.md` (the plan that built what this one replaces). Work on a new branch, `normalise-stats`, from an up-to-date `main`. Don't commit unless the user asks. Don't push or open a PR unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Don't start the next phase until that step passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Why

`game-stats.md` records one row per answered round in a single flat table, `round_result` (`V2__round_stats.sql`). Every row repeats the same few strings and the same per-game facts:

| Repeated in every row | Distinct values | Bytes per row |
|---|---|---|
| `game_mode` (`RANDOM`, `COUNTRIES_BY_LAND_BORDERS`...) | 10 | 7–26 |
| `game_type` | 9 | 9–26 |
| `answer_state` (`CORRECT`, `GAVE_UP`, `OUT_OF_GUESSES`) | 3 | 8–15 |
| `round_key` (`river:Q3392`, `tripoint:A\|B\|C@lat,lon`...) | about 500 (164 of them tripoints) | 12–60 |
| `hint_count`, `guess_limit` (fixed per type) | about 9 pairs | 4 |
| `play_id`, `answered_on`, `rounds_in_game` (the same for every round of a game) | one per game | 22 |
| `id bigint` surrogate key, plus its index entry | – | 8 + about 22 |

Postgres storage rules: a 23-byte tuple header padded to 24, plus a 4-byte line pointer per row; a null bitmap of 1 bit per column when the row holds a NULL, which pushes the header to 32 once a table has more than 8 columns; data padded to 8 bytes; short `varchar` = 1 + length; `uuid` = 16. By those rules a `round_result` row is **116–172 bytes in the heap** (+8 when `wrong_guesses` is NULL), plus about 22 in the primary-key index: **about 160 bytes per answered round**. 40–110 bytes of that is repeated text.

After this plan, one answered round costs **about 74 bytes**: a 52-byte `stat_round` row (8 fixed-width columns, no text) and its 22-byte key entry. On top of that, each game costs about 120 bytes once in `stat_game`, which is about 24 bytes a round at the default 5 rounds. That makes **about 98 bytes a round, down from about 160 (−40%)**, and the gain grows with longer games. A million rounds goes from about 160 MB to about 100 MB.

Don't expect more than that. About 50 of the 74 bytes are Postgres's fixed per-row cost (header, line pointer, index entry), and every design with one row per round pays it. Saving more means not storing one row per round (see "Not in this plan").

## Decisions already made (don't revisit them)

1. **Scrap the old data.** The new migration drops `round_result`. Its rows aren't migrated.
2. **Star schema: one fact table, one per-game table, four lookup tables.** Every repeated string moves to a lookup table, and rows refer to it by an `integer` id. Use lookup tables, not Postgres `ENUM` types or `CHECK`ed text. A new `GameType` must still need no migration, and adding an enum value is an `ALTER TYPE`. H2 also doesn't share Postgres's enum syntax.
3. **The lookup rows are created at run time** by the store (get-or-create), not seeded by the migration. So a new `GameType`, game mode or answer state needs nothing extra, as today.
4. **`hint_count` and `guess_limit` live on the game-type row**, keyed by `(name, hint_count, guess_limit)`. If a type's settings change, the next round gets a new `stat_game_type` row, and old rounds keep their settings. `points` stays on the round: it costs no space (see the alignment note) and it freezes the score if `Scoring` changes.
5. **The date moves to the game.** `stat_game.played_on` is the UTC date of the game's first recorded answer. This keeps `stat_round` at 8 columns, so a NULL `wrong_guesses` costs nothing (at 9 columns the null bitmap makes the header 32 bytes). The date is still a date, never a timestamp.
6. **Every id is `integer`.** In `stat_round` a `smallint` id would be padded away and save nothing. `integer` also avoids depending on H2 accepting `smallint ... generated as identity`.
7. **The key of `stat_round` is `(game_id, round_number)`, not a surrogate `id`.** It saves the 8-byte column, and the database now rejects a second row for the same round. No other indexes: the analysis queries scan whole tables, which stay small. Add an index only when a real query needs one.
8. **A view named `round_result`** joins everything back to the old flat columns. It costs no space, and the analysis query in `game-stats.md` and the existing `RoundStatsIT` keep working on it.
9. **Only the store changes.** `RoundAnswered`, `GameService`, `RoundStatsRecorder`, `StatsConfig` and `RoundResult` keep their shape and behaviour. `RoundResult` is still the flat, logical row; `RoundStatsStore` splits it across the tables. `game_entity.play_id` and `game_entity.game_mode` (from `V2`) stay as they are.
10. **Everything from `game-stats.md` decisions 3 and 4 still holds:** written after commit on the one `stats-` thread, failures logged at WARN and dropped, no foreign keys to the game tables, nothing deletes stats rows, and the anonymity rules (no IP, headers, either game id, timestamps; `play_id` never exposed or logged).

## The migration

`src/main/resources/db/migration/V3__normalise_round_stats.sql` (use the next free version if `V3` is taken). It must run on both H2 in PostgreSQL mode and Postgres. Never edit `V1` or `V2`.

```sql
-- Replaces V2's flat round_result table with a star schema: the repeated strings move to lookup
-- tables, and the per-game facts move to stat_game. See docs/plans/normalise-game-stats.md.
-- The old rows are deliberately not migrated.
drop table round_result;

-- Lookup tables. Rows are created by RoundStatsStore the first time a value is seen, and never
-- deleted or updated (the store caches their ids).
create table stat_game_mode (
    id   integer generated by default as identity primary key,
    name varchar(40) not null unique            -- a GameType name, or RANDOM
);

create table stat_game_type (
    id          integer generated by default as identity primary key,
    name        varchar(40) not null,           -- a GameType name
    hint_count  smallint    not null,           -- the type's settings when the round was played
    guess_limit smallint    not null,
    constraint uq_stat_game_type unique (name, hint_count, guess_limit)
);

create table stat_answer_state (
    id   integer generated by default as identity primary key,
    name varchar(20) not null unique            -- CORRECT, GAVE_UP or OUT_OF_GUESSES
);

create table stat_round_key (
    id        integer generated by default as identity primary key,
    round_key varchar(255) not null unique      -- GameData.roundKey(), e.g. river:Q3392
);

-- One row per game with at least one recorded round. No foreign key to game_entity: it outlives it.
create table stat_game (
    id             integer generated by default as identity primary key,
    played_on      date     not null,           -- UTC date of the first recorded answer, never a time
    game_mode_id   integer  not null references stat_game_mode (id),
    rounds_in_game smallint not null,
    play_id        uuid     not null unique     -- random per game; links its rounds, nothing else
);

-- One row per answered round. Column order matters: the 4-byte columns first, then the 2-byte
-- ones, so Postgres adds no padding between them. Keep it at 8 columns (see the plan).
create table stat_round (
    game_id         integer  not null references stat_game (id),
    round_key_id    integer  not null references stat_round_key (id),
    game_type_id    integer  not null references stat_game_type (id),
    answer_state_id integer  not null references stat_answer_state (id),
    round_number    smallint not null,          -- from 0, as in the API
    hints_used      smallint not null,
    wrong_guesses   smallint,                   -- NULL = the client didn't report it; never 0 instead
    points          smallint not null,          -- Scoring.roundPoints at the time
    constraint pk_stat_round primary key (game_id, round_number)
);

-- The old flat shape, for analysis. Read-only: write through RoundStatsStore.
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
       r.points
from stat_round r
join stat_game g         on g.id = r.game_id
join stat_game_mode m    on m.id = g.game_mode_id
join stat_game_type t    on t.id = r.game_type_id
join stat_round_key k    on k.id = r.round_key_id
join stat_answer_state a on a.id = r.answer_state_id;
```

**Alignment note** (why the counts work out): `stat_round` holds 4 × 4 + 4 × 2 = 24 bytes of data, which is exactly 8-byte aligned, after a 24-byte header (8 columns means a 1-byte null bitmap, which fits in the header's padding). Dropping `points`, or shrinking any id to `smallint`, would leave the row at 48 bytes. Adding a ninth column makes rows with a NULL 8 bytes bigger. If you must change the table, keep it at 8 columns or work the sizes out again.

## Ground rules

- Don't touch `client/` or `e2e/`. Don't change any API record in `api/`, `GameService`, `RoundAnswered`, `RoundStatsRecorder` or `StatsConfig`.
- No test may reach the network. Every test uses the default in-memory H2 database, with the migrations applied by Flyway.
- Bind every value as a parameter. No string concatenation in SQL.
- Format with `mvn spotless:apply` before each verification step.
- Environment on this machine: `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.0.2"`, and put `target/node` on `PATH` for npm.

## Phase 0: baseline

1. `git checkout main && git pull && git checkout -b normalise-stats`.
2. Check that `V2__round_stats.sql` creates `round_result` as described above, and that no migration after `V2` exists. If one does, stop and tell the user.
3. `mvn -B verify` passes. Note the test counts.

## Phase 1: the migration

1. Add the migration above.
2. Run `mvn -B verify -Dskip.frontend -DskipITs -Dspotless.check.skip=true`. It is expected to fail only in `RoundStatsStoreTest` (it inserts into a view now). Any other failure, in particular a Flyway or SQL error on H2, must be fixed in the migration first. If H2 rejects a statement, change the SQL so it is valid on both databases; don't make it H2-only.

**Verify:** the migration applies on H2 (the Spring tests start), and the only failures are in `RoundStatsStoreTest`.

## Phase 2: the store

Rewrite `service/stats/RoundStatsStore`. Its public API stays `void insert(RoundResult row)`.

1. **Lookups, cached.** For each of mode, game type, answer state and round key, a get-or-create: `select id ... where <natural key> = :...`; if there is none, `insert` and read the generated id (`JdbcClient`'s `.update(keyHolder, "id")`). Cache the id in a `ConcurrentHashMap` keyed by the natural key (for game types, a small private record `(name, hintCount, guessLimit)`). A cache hit makes no database call. The lookup tables are tiny (about 500 round keys in all), so the caches need no bound. Don't split round keys any further (for example a tripoint into its countries and coordinate): each key is stored once, so all the tripoint keys together are about 25 KB, and that total doesn't grow as people play.
2. **The game, not cached.** `select id from stat_game where play_id = :playId`; if there is none, insert `(played_on, game_mode_id, rounds_in_game, play_id)` from the row and read the generated id.
3. **The round.** One `insert into stat_round (...)`. Bind `wrong_guesses` with `java.sql.Types.SMALLINT`, as today, so a `null` stays NULL.
4. **No `@Transactional`.** Every statement commits on its own (auto-commit). This is deliberate:
   - A cached lookup id must be committed, or a rolled-back insert would leave a cached id with no row, and every later insert using it would fail its foreign key until a restart.
   - If the round insert fails, a lookup row or a `stat_game` row with no rounds may be left behind. That is harmless: the next round of that game reuses the game row, and a game with no rounds is just a game nobody finished a round of.
5. **Concurrency.** The store is only called from the one `stats-` thread, so a get-or-create can't race. If it ever did, the `unique` constraints reject the second insert, the recorder logs its WARN and that one row is dropped: never a duplicate. Say this in the class's Javadoc.
6. Steady state is 2–3 statements per answered round (find or create the game, insert the round), the same order of cost as today, still off the request thread.
7. Update the Javadoc of `RoundResult` and `RoundStatsStore`: `RoundResult` is the flat, logical row (the same columns as the `round_result` view), and the store splits it across the `stat_*` tables.

**Tests (`RoundStatsStoreTest`, rewritten).** Keep `@SpringBootTest(webEnvironment = NONE)`. In `@AfterEach` delete from `stat_round`, then `stat_game`, in that order (foreign keys). **Never delete from the lookup tables in any test**: the store caches their ids and the H2 database is shared by every cached Spring context in the JVM. So tests count lookup rows by name, not in total.
- An insert reads back through the `round_result` view with every column intact (the same assertions as today's test).
- A `null` `wrongGuesses` reads back as NULL, not 0.
- Two rounds with the same `playId` make one `stat_game` row and two `stat_round` rows.
- Two games with the same type and round key reuse the same `stat_game_type` and `stat_round_key` rows (count `where name = ...` / `where round_key = ...` is 1).
- The same type name with a different `hintCount` makes a second `stat_game_type` row, and each round reads back its own `hint_count` through the view.
- Inserting the same `(playId, roundNumber)` twice throws (the primary key), and still leaves one row.
- No `stat_*` table, and no column of the view, holds anything but the values given (no time of day: `played_on` is a `DATE`).

**Verify:** `mvn -B verify -Dskip.frontend` passes, including the ITs.

## Phase 3: the integration tests

1. `RoundStatsIT` reads through the `round_result` view, so its queries should still work unchanged. Run it and keep it unchanged if it passes. Then add:
   - After both rounds of the first game are recorded, there is exactly one `stat_game` row for its `play_id`, with `rounds_in_game` 2 and the mode `RIVERS_BY_SHAPE`.
   - The public-id scan also covers the tables: `select * from stat_game` contains neither game's public id.
2. `RoundStatsFailureIT` and `RoundStatsDisabledIT` mock or turn off the store. Check they still pass unchanged.
3. Check the JaCoCo gate still passes. Add unit tests rather than lowering a threshold.

**Verify:** `mvn -B verify` passes in full (frontend, unit tests, ITs, JaCoCo, Spotless). `git diff main -- client/ e2e/ src/main/java/com/parkinson/benjamin/geogames/api` is empty.

## Phase 4: docs

1. `README.md`, "Usage statistics": the rows now go to the `stat_*` tables (one row per answered round in `stat_round`, one per game in `stat_game`, the repeated names in small lookup tables), and the `round_result` view shows them in the flat shape. Keep the "never recorded" paragraph as it is.
2. `CLAUDE.md`, the **Usage statistics** paragraph under **Game flow / API**: replace "inserts one row into `round_result`" with the star schema in one or two sentences, and add:
   - `round_result` is a read-only view; write through `RoundStatsStore`.
   - The lookup rows are created on first use and cached by id, so they are never deleted or updated, and tests never clean them.
   - `stat_round` is kept at 8 fixed-width columns on purpose (see this plan's alignment note).
3. `docs/plans/game-stats.md`: one line under the title: "The `round_result` table was replaced by a normalised schema and a view of the same name; see `normalise-game-stats.md`." Change nothing else in it.

**Verify:** `mvn -B verify` passes. `bash scripts/check-versions.sh` passes.

## When you're done

Report to the user:
- what changed, phase by phase, and the test counts before and after;
- that the deploy drops the existing `round_result` rows (agreed) and recording carries on into the new tables with no new Render variable; games created before the deploy still have a `play_id`, so their later rounds are recorded too;
- that the size of the new tables can be checked in the Neon SQL editor with:
  ```sql
  select relname, pg_size_pretty(pg_total_relation_size(oid)) as total, reltuples::bigint as rows
  from pg_class where relname like 'stat\_%' and relkind = 'r' order by pg_total_relation_size(oid) desc;
  ```
  and the bytes per round as `pg_total_relation_size('stat_round') / count(*)` (expect about 75 once there are a few thousand rows; a small table rounds up to whole 8 KB pages);
- anything you weren't sure about.

## Not in this plan (possible follow-ups)

- **Daily roll-ups instead of rows.** If the data ever outgrows the free tier, the next real saving isn't a better row layout but fewer rows: a table of counts per `(day, game_type, round_key, answer_state, hints_used, wrong_guesses)`, updated with an upsert. Its size depends on how many combinations there are, not how many rounds. It loses the per-game link (no abandonment analysis), so it's a product decision.
- **Dropping the `play_id` index.** The unique `uuid` index is about 40 of the roughly 120 bytes per game. Assigning the stats game id from a database sequence when the game is created would remove it, but that adds a statement to game creation, which `game-stats.md` keeps free of stats work.
- **Insertion order.** Identity ids (and the physical order of rows) show the order in which games and answers were recorded within a day, as `round_result.id` already did. That is not a timestamp, but a careful analyst with request logs could line orders up. Nothing here makes it worse.
