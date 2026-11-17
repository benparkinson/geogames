# Plan: make the backend safe to run as several instances

Read `CLAUDE.md`, `src/main/CLAUDE.md` and `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md` first. This plan changes only the backend (Java, one Flyway migration, the backend `CLAUDE.md`). The API, the response shapes (`client/src/test/api-shapes.json`), the frontend and the e2e tests stay as they are.

Work on a new branch `multi-instance-safety` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

Do the three parts in order. Each part ends with the check named in it; the last step runs everything. If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output. Where this plan says "if X turns out not to hold", follow the fallback it gives rather than inventing a third approach.

## Goal

The backend keeps all game state in the database, so several instances can already serve the same games. Three places still assume there is only one instance (or only one request at a time):

1. **Cleanup.** Every instance runs the cleanup job at start-up and hourly. It deletes expired games one row at a time, so two instances running it together collide: the loser fails with a stale-state exception and a stack trace. The games do get deleted, but the logs are noisy and the row-by-row delete is slow.
2. **Statistics.** `RoundStatsStore` does select-then-insert on its lookup tables and on `stat_game`, relying on being called from one thread. With several instances there is one such thread per instance. When two see a new value at the same moment, the unique constraint rejects one insert and that statistics row is dropped.
3. **Answers.** `GameService.submitAnswer` reads a round, checks it is `UNANSWERED`, then writes it, with nothing to stop two requests doing that at once. Two different answers to one round sent together both return 200 and the last write wins, although the rule is "a round's answer is final; a different answer is a 409".

After this plan: cleanup is one idempotent statement, a lost insert race in the stats re-reads the winner's id instead of dropping the row, and a concurrent second answer gets the same result it would have got had it arrived a moment later (200 for the identical answer, 409 for a different one).

## Part 1: cleanup as one bulk delete

`game_round_entity.game_id` is declared `references game_entity (id) on delete cascade` in `V1__games.sql`, so the database removes a game's rounds itself. The JPA cascade (load each game, delete each round, delete the game) is not needed for cleanup.

**`dao/GameRepository.java`**: replace `deleteByCreatedAtBefore` with a bulk query:

```java
/**
 * Deletes games created before the cutoff in one statement. The rounds go with them through the
 * foreign key's {@code on delete cascade}, not through JPA, so no game is loaded. Deleting nothing
 * is not an error, so several instances can run this at the same time.
 */
@Modifying
@Query("delete from GameEntity g where g.createdAt < :cutoff")
int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
```

The return type is `int` because Spring Data modifying queries return `int`, `Integer` or `void`.

**`service/GameCleanupService.java`**: call `games.deleteCreatedBefore(...)`. `deleteGamesOlderThanMaxAge` keeps returning `long` (the `int` widens). Keep `@Transactional` on it: a modifying query needs a transaction. Add one sentence to the class Javadoc saying the delete is a single statement and safe to run from several instances at once.

**Tests**
- `service/GameCleanupServiceTest`: rename the mocked method and return `3` instead of `3L`.
- `dao/GameRepositoryTest`: `deletingOldGamesRemovesTheirRoundsToo` and `deletingAGameRemovesItsRoundRows` call the new method. They already assert that the round rows are gone, which is what proves the foreign-key cascade works on H2. Keep their `flush()` before the delete and add `entityManager.clear()` after it where the test reads entities afterwards (a bulk delete does not update the persistence context).
- Add `deletingWhenNothingIsOldDeletesNothing`: with only a recent game saved, the method returns 0 and the game is still there.

**Fallback.** If the JPQL delete fails with a foreign-key violation (Hibernate did not leave the rounds to the database), switch to a native query with the same signature: `@Query(value = "delete from game_entity where created_at < :cutoff", nativeQuery = true)`.

Check: `bash scripts/check-java.sh -Dtest=GameRepositoryTest+GameCleanupServiceTest`.

## Part 2: the stats store recovers from a lost insert race

**`service/stats/RoundStatsStore.java`**. Today `findOrCreate` selects, and inserts if nothing was found. Change the "insert" half so that a duplicate-key failure means "another writer just created it" and re-selects:

```java
private int findOrCreate(String select, String insert, Map<String, Object> params) {
  return find(select, params).orElseGet(() -> insertOrFind(select, insert, params));
}

/**
 * Inserts the row. If another writer (the stats thread of another instance) inserted the same
 * value first, the unique constraint rejects this insert, and the winner's id is read instead.
 */
int insertOrFind(String select, String insert, Map<String, Object> params) {
  try {
    return insertReturningId(insert, params);
  } catch (DuplicateKeyException e) {
    return find(select, params).orElseThrow(() -> e);
  }
}

private Optional<Integer> find(String select, Map<String, Object> params) {
  return jdbc.sql(select).params(params).query(Integer.class).optional();
}
```

- `insertOrFind` is package-private on purpose, so the test can call it directly.
- Make `findOrCreateGame` use `findOrCreate` too, with one params map for both statements (`playedOn`, `modeId`, `roundsInGame`, `playId`). The select only uses `:playId`; unused named parameters in the map are ignored.
- Leave the `stat_round` insert alone. A duplicate there (same game and round number) must still fail and be dropped by the recorder: the first recorded answer stays.
- The store must stay without `@Transactional`. On Postgres a failed statement aborts the surrounding transaction, so the re-select only works because each statement commits on its own. Say so in the class Javadoc.
- Rewrite the Javadoc's "Concurrency" paragraph: there is one `stats-` thread per instance, so with several instances a get-or-create can race; the unique constraints make the loser's insert fail, and it then reads the winner's row. Never a duplicate, and no dropped row.

**Tests** in `service/stats/RoundStatsStoreTest` (it is a full-context test on H2 and never cleans the lookup tables, so use values no other test uses):
- `aLostInsertRaceReadsTheOtherWritersLookupRow`: insert a `stat_round_key` row with `jdbc` directly (a unique key such as `"river:race-" + UUID.randomUUID()`), then call `store.insertOrFind(...)` with the round-key select and insert statements and that key. It returns the existing row's id, and the table still has exactly one row with that key. This is the state the store is in when another instance inserts between its select and its insert.
- `aLostInsertRaceReadsTheOtherWritersGameRow`: the same for `stat_game` and a random `play_id` (insert a mode row first, or reuse an existing mode id, for `game_mode_id`).
- The existing tests must pass unchanged, in particular `theSameRoundTwiceIsRejectedAndLeavesOneRow`.

To call `insertOrFind` from the test, the SQL strings must be reachable: either make them package-private constants on the store, or add small package-private methods (for example `roundKeyId(String)` and `gameId(RoundResult, int)`) that the test calls on a fresh code path. Choose whichever keeps the store simpler; do not make anything public.

**Fallback.** If the duplicate insert on H2 surfaces as a `DataIntegrityViolationException` that is not a `DuplicateKeyException`, catch `DataIntegrityViolationException` instead (it is the parent class). The re-select then throws the original exception when the row is not there, so a different integrity failure is still reported.

Check: `bash scripts/check-java.sh -Dtest=RoundStatsStoreTest+RoundStatsRecorderTest`.

## Part 3: a concurrent second answer gets a 200 or a 409

Use optimistic locking on the round, then retry once so the loser sees the winner's committed answer and the existing rules decide the response.

**Migration** `src/main/resources/db/migration/V4__round_version.sql` (never edit V1 to V3):

```sql
-- Optimistic locking for answers: two requests answering one round at the same moment can't both
-- write it. The default covers rounds that exist already and rows inserted by an older version of
-- the app during a deploy.
alter table game_round_entity add column version integer not null default 0;
```

**`dao/GameRoundEntity.java`**: add the field, with no setter and no getter unless a test needs one:

```java
/** Optimistic lock: a second writer of the same round fails rather than overwriting the first. */
@Version
@Column(nullable = false)
private int version;
```

Put it on the round, not on `GameEntity`: answers to two different rounds of one game must not conflict.

**How it then behaves.** In `submitAnswer` the update is flushed when the transaction commits. If another request answered the round in between, the commit throws `ObjectOptimisticLockingFailureException` (`org.springframework.orm`) out of `gameService.submitAnswer(...)`. The transaction rolls back, so no `RoundAnswered` event is handled for the loser. The same exception appears if the cleanup job deleted the game in between.

**`controllers/GameController.java`**: in `submitAnswer`, call the service through a small private method that retries once:

```java
/**
 * Another request changed this round between this one's read and its write. Trying again reads
 * what that request committed, so the usual rules apply: the identical answer is a no-op, a
 * different one is a 409, and a game deleted meanwhile is a 404.
 */
private Optional<GameRound> submitAnswerOnce(UUID id, int round, GameRoundAnswerRequest answer) { ... }
```

Catch only `ObjectOptimisticLockingFailureException`, retry exactly once, and let a second failure propagate. The retry belongs in the controller because it must happen outside the service's transaction; do not call `submitAnswer` from inside `GameService` (a self-call skips the transaction proxy).

**`controllers/ApiExceptionHandler.java`**: map `ObjectOptimisticLockingFailureException` to a 409 `ProblemDetail` with a fixed detail such as "This round was changed by another request. Please try again." Do not put the exception's message in the response or the log: it names the entity and its id. Update the class Javadoc.

`GameService` itself needs no code change. Update the Javadoc of `recordAnswer` (or `submitAnswer`) with one sentence: concurrent answers to one round are stopped by the round's version, and the controller retries once.

**Tests**
- `dao/GameRepositoryTest`, new `aRoundChangedElsewhereCannotBeOverwritten`: save a game, flush and clear, load it again and touch `getRounds()` so the rounds are loaded with version 0. Bump the row behind Hibernate's back with a native query through `entityManager.getEntityManager()`: `update game_round_entity set version = version + 1 where id = ?`. Only then set the round's answer state and call `entityManager.flush()`. Assert it throws (`jakarta.persistence.OptimisticLockException` or Spring's `ObjectOptimisticLockingFailureException`; accept either with `isInstanceOfAny`). The order matters: a native query flushes pending changes first, so the in-memory change must come after it.
- `dao/GameRepositoryTest`, new `answeringARoundBumpsOnlyThatRoundsVersion` if a getter makes it cheap; otherwise skip it.
- `controllers/GameControllerTest` (`@WebMvcTest`, the service is a `@MockitoBean`), three new tests next to `changingAnAnsweredRoundIsAConflict`:
  - the service throws `ObjectOptimisticLockingFailureException` once and then returns a round: the response is 200 and the service was called twice;
  - it throws that once and then `RoundAlreadyAnsweredException`: 409;
  - it throws that both times: 409 with a `ProblemDetail` body, and the service was called exactly twice.
  Build the exception with `new ObjectOptimisticLockingFailureException(GameRoundEntity.class, 1L)`.
- `PostgresMigrationIT` and `GameApiIT` need no change; they prove the new column validates on a real Postgres and that a normal game still plays.

**The contract does not change.** `version` is not in any response, so do not regenerate `api-shapes.json`. If `ApiContractIT` fails, something leaked: fix that rather than updating the shapes.

Check: `bash scripts/check-java.sh -Dtest=GameRepositoryTest+GameControllerTest+GameServiceTest`.

## Documentation

Update `src/main/java/com/parkinson/benjamin/geogames/CLAUDE.md` (keep its plain style, a sentence or two each):
- **API rules**, the answers bullet: two answers to one round at the same moment behave as if one came after the other (the round has a version; the controller retries once).
- **Persistence**: the cleanup is one bulk delete, the rounds go through the foreign key's `on delete cascade`, and it is safe for every instance to run it. `GameRoundEntity` has a `version` column for optimistic locking.
- **Usage statistics**: there is one stats thread per instance; a lost race on a lookup or game row re-reads the winner's row, and the store must stay non-transactional for that to work on Postgres.

Do not edit the root `CLAUDE.md` or `src/main/CLAUDE.md` unless something written there has become false.

## Final checks

1. `bash scripts/check-java.sh` (Spotless, all unit tests, the JaCoCo gate).
2. `bash scripts/check-it.sh` (the `*IT` classes; `PostgresMigrationIT` needs Docker and is skipped locally without it, so say in your report whether it ran).

Report to the user: what changed in each part, which fallbacks (if any) were used, the check results, and whether `PostgresMigrationIT` ran.

## Out of scope

Do not add a distributed lock for the scheduled job (ShedLock or similar), a shared cache, connection-pool changes, or anything in `client/` or `e2e/`. The other points from the analysis (the `prod` profile on every instance, the connection budget, migrations compatible with the previous version during a deploy) are operational and need no code here.
