# Plan: a persistent database, at the lowest cost that works

Read `CLAUDE.md` first. `round-references.md` is merged (PR #83), so a round is now a short key, not about 60 KB of geometry, and the cheap options below work. Work on a new branch, `persistent-database`, from an up-to-date `main`. Don't commit unless the user asks. Don't push, open a PR, or change anything in Render, Neon or GitHub unless the user asks. The section "What the user does" lists the account and console steps, which are the user's to do.

Do the phases in order. Each phase ends with a verification step. **Don't start the next phase until that step passes.** If a step fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Where things stand (checked on 2026-11-09)

- **Games live in an in-memory H2 database** (`spring.datasource.url=jdbc:h2:mem:testdb`), with the schema created by Hibernate on start-up. Every restart, deploy or free-tier spin-down loses every game, including games in progress. `GameCleanupService` deletes each game 24 hours after it is created (`geogames.games.max-age`).
- **The backend runs on Render's free web service**, built from the `Dockerfile` with `-Xmx400M`. It spins down after 15 minutes with no traffic and takes about a minute to wake. It has 750 free instance hours a month and 5 GB of outbound bandwidth on the Hobby workspace, then $0.15/GB. Its filesystem is ephemeral, and free services can't attach a disk.
- **The map data is not in the database as a dataset.** It is in the jar (`src/main/resources/processed/`, about 31 MB with the full-resolution `countries/`) and cached in memory by the loader services. Before `round-references.md`, each round copied its geometry into a `@Lob jsonBlob` (about 60 KB a round, 0.5 MB a game). After it, a round is a key of about 100 bytes and a game, with its rounds and indexes, is about 1–2 KB. So 1 GB holds hundreds of thousands of games, far more than exist at once with a 24-hour expiry.
- **Mapping details that matter for a real schema:** both entities use `@Id @GeneratedValue` (Hibernate's default, a sequence per entity, allocation size 50). `GameEntity.rounds` is a `@OneToMany` with no `@JoinColumn`, so Hibernate uses a join table (`game_entity_rounds`). `GameRoundEntity.answerState` has no `@Enumerated`, so it is stored as an ordinal number. `GameRoundEntity.wrongGuesses` is a nullable `Integer`.
- **Game ids are sequential `long`s and they are in the URLs** (`POST /api/games` returns `{id}`, the API is `/api/games/{id}/...`, and the page is `/games/{id}`). Anyone can open or answer another player's game by changing the number. Once games persist, players will bookmark and share these URLs, so the id format has to be settled before the switch: phase 1.

## The decision

Prices from the official pages on 2026-11-09 ([Render pricing](https://render.com/pricing.md), [Render free tier](https://render.com/docs/free), [Neon plans](https://neon.com/docs/introduction/plans)). The Supabase figures are from third-party summaries and are less certain.

| Option | Monthly cost | What decides it |
|---|---|---|
| **Neon Free** (serverless Postgres) | $0 | 1 GB per project, 100 compute-hours a month (enough for the smallest compute, 0.25 CU, for 400 hours), sleeps after 5 minutes idle, can't be kept awake, no expiry, 6-hour restore window. **Reaching the compute limit suspends the database until next month.** |
| Neon Launch | Usage, no minimum | $0.106 per CU-hour and $0.35 per GB a month. Cheap while traffic is occasional. A database awake all month at 0.25 CU is about $19. |
| Render Postgres Free | $0 | **Deleted 30 days after creation** (then a 14-day grace period). Not an option. |
| **Render Postgres Basic** (0.1 CPU, 256 MB) | $6 + $0.30/GB | Fixed price, same network as the app, no sleep. |
| Supabase Free / Pro | $0 / $25 | Free pauses after a week without activity and has a 500 MB cap. Pro is too expensive for this. |
| H2 or SQLite file on a Render disk | $7 instance + $0.25/GB | Needs a paid instance anyway. A service with a disk can't run more than one instance, and deploys have downtime. |

**Stage 1, when real players arrive: Neon Free.** It costs nothing, has no expiry, and holds about as much data as Render's free database. With the 24-hour expiry, the data is small and disposable, so the free tier's limits on backups and support don't matter.

**Stage 2, once traffic is steady all day: Render paid**, the $7 web service (0.5 CPU, 512 MB) and Render Postgres Basic at $6, so **about $13 a month**. Move when either of these happens:
- Neon's monthly compute use is on course to pass about 70 of its 100 free hours. A database awake all day uses about 180 hours at 0.25 CU, and at the limit it suspends, which takes the game down. Neon Launch would then cost more than Render's fixed $6.
- Players complain about the slow first load. That is the **web service** waking from sleep, not the database, so the $7 web service comes first, and the database can stay on Neon until its compute use forces the move.

Moving databases needs no data migration: the games are disposable. Point the app at the new database, and at most the last 24 hours of games are lost. Flyway creates the schema.

**Not doing:** Supabase (paid tier too expensive, free tier pauses), a disk on Render (the cost of a paid instance without the benefits of managed Postgres), keeping games for ever (only add a longer retention when a feature needs it, such as share links, a history or leaderboards, and size the database then).

## Ground rules

- **No test may reach the network** (`CLAUDE.md`). Tests keep using H2, in PostgreSQL compatibility mode, with the same Flyway migrations as production, so the migrations are tested on every PR. Don't add Testcontainers: pulling a Postgres image is network access from a PR. The check against real Postgres is a manual step (phase 5).
- **The default profile stays H2 in memory**, so `mvn spring-boot:run`, the jar, the browser tests and the CI `docker` job work with no database and no secrets. Postgres is only used by a `prod` profile that Render turns on.
- **No secrets in the repo.** The connection details come from environment variables set in Render.
- Don't change the API or the frontend, except for the game id in phase 1.
- Format with `mvn spotless:apply` before each verification step.
- Environment on this machine: `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.0.2"`, put `target/node` on `PATH` for npm.

## Phase 0: baseline

1. Check `round-references.md` is merged: `GameRoundEntity` has `roundKey`, not `jsonBlob`. If not, stop.
2. `git checkout main && git pull && git checkout -b persistent-database`.
3. `mvn -B verify` passes. Note the test counts.

## Phase 1: unguessable public game ids

The `long` id stays as the internal primary key (round rows reference it, and it never leaves the server). A random UUID becomes the id the API returns and the URLs use. This comes before the Flyway phase, so the first migration already has the column.

1. **Backend:**
   - `GameEntity`: add `@Column(nullable = false, unique = true, updatable = false) private UUID publicId`, set in the constructor with `UUID.randomUUID()`. That uses `SecureRandom` and has 122 random bits, so it can't be guessed. Don't use the shared `java.util.Random` bean, which tests seed. Add a getter, and a setter only if JPA needs one. Say in the javadoc why there are two ids.
   - `GameRepository`: add `Optional<GameEntity> findByPublicId(UUID publicId)`. Keep `deleteByCreatedAtBefore`.
   - `api/GameCreationResponse`: `record GameCreationResponse(UUID id)`. Jackson writes it as a string. **Keep the field name `id`**, so the frontend code that reads it keeps working.
   - `GameService.getGameRound` and `submitAnswer` take a `UUID` and look the game up with `findByPublicId`. `createGame` returns the public id.
   - `GameController`: take the path variable as a `String` and parse it in one small helper. **A malformed id, including an old numeric one, is a 404**, the same as an unknown game, not a 400: from the player's side it is a game that doesn't exist. Check what `ApiExceptionHandler` does with a type mismatch today, and make sure no path still turns a bad id into a 400 or a 500.
   - Search the backend for other uses of the game id (logs, `toString`, exception messages) and use the public id wherever the id leaves the server.
2. **Frontend** (`client/`):
   - `src/api/games.ts`: `postJson<{ id: string }>`. The rest already treats `gameId` as a string (`roundUrl`, `queryKeys`, `router.push("/games/" + data.id)`). Grep `client/src` and `client/pages` for `Number(`, `parseInt` or arithmetic on a game id, and fix any you find.
   - Test fixtures and tests that use numeric ids (`src/test/fixtures.ts`, `src/pages/gameIdPage.test.tsx`, `src/games/common/GameRoundPage.test.tsx`, `GameSummary.test.tsx`, and any others the grep finds): use a UUID string such as `"0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d"`.
   - `e2e/fixtures.ts` already accepts `string | number`. Narrow it to `string`.
3. **Contract:** regenerate `client/src/test/api-shapes.json` with the command in `CLAUDE.md` (`mvn verify -Dcontract.update=true -Dit.test=ApiContractIT ...`). The only change should be `"POST /api/games": {"id": "string"}`. If anything else changes, stop and find out why. Then make `client/src/api/contract.test.ts` pass with the updated fixtures.
4. **Deploy order:** the frontend and backend deploy separately on Render, and both orders work. An old frontend puts the UUID string into its URLs unchanged, and a new frontend handles an old backend's number as a string. Check this claim against the code while you work, and say in the report if it turns out false. Old numeric links give a 404, which is what they give today anyway, because a deploy wipes the in-memory games.
5. **Tests:**
   - `GameRepositoryTest`: a saved game has a public id, two games get different ids, and `findByPublicId` finds the game.
   - `GameServiceTest`: lookups go by the public id, and an unknown UUID is empty.
   - `GameControllerTest`: a malformed id (`abc`) and a numeric id (`42`) are 404s, for both the round `GET` and the answer `POST`.
   - `GameApiIT`: the id from `POST /api/games` matches the UUID pattern, and a full game works through it.
6. `CLAUDE.md`, "Game flow / API": `{id}` is a random UUID (the public id), the `long` primary key never leaves the server, and a malformed or unknown id is a 404.

**Verify:** `mvn -B verify` passes (it includes `npm test`, the contract test and `ApiContractIT`), `npm run typecheck` passes in `client/` and `e2e/`, and the browser tests pass (`CLAUDE.md`, "Browser tests"). The address bar shows `/games/<uuid>`.

## Phase 2: a schema owned by Flyway

1. **Simplify the mapping before freezing it in a migration:**
   - `GameEntity.rounds`: add `@JoinColumn(name = "game_id", nullable = false)`, so a round row points at its game and there is no join table. Update the comment on `GameRepository.deleteByCreatedAtBefore` that mentions join-table rows.
   - Both ids: `@GeneratedValue(strategy = GenerationType.IDENTITY)`, so the schema is a plain `bigint generated by default as identity` with no sequences to keep in step. A game inserts at most 11 rows, so losing JDBC batching doesn't matter.
2. **Add the dependencies** to `pom.xml`, with versions managed by the Spring Boot parent (don't set them):
   - `org.postgresql:postgresql`, `runtime` scope.
   - Flyway. Boot 4 split its auto-configuration into modules: use the Flyway starter (`spring-boot-starter-flyway`; check the exact artifact id in the Boot 4 reference), plus `org.flywaydb:flyway-database-postgresql`, which Flyway needs to talk to Postgres.
   - Keep `com.h2database:h2`, still `runtime` scope.
3. **Write `src/main/resources/db/migration/V1__games.sql`** in SQL that both Postgres and H2 (in PostgreSQL mode) accept:
   - `game_entity (id bigint generated by default as identity primary key, public_id uuid not null unique, created_at timestamp with time zone not null)`.
   - `game_round_entity (id ... primary key, game_id bigint not null references game_entity(id) on delete cascade, round_number int not null, game_type varchar(64) not null, round_key varchar(255) not null, answer_state ..., hints_used int not null, wrong_guesses int)`. `wrong_guesses` stays nullable, as the entity's `Integer` is.
   - Check the entities for any other field added since this plan was written, and include it.
   - Check how `answerState` is mapped: it has no `@Enumerated`, so today Hibernate stores it as an **ordinal** number. Make it `@Enumerated(EnumType.STRING)` like `gameType` first (an ordinal breaks silently if someone reorders the enum, and stored data now outlives a deploy), and use `varchar(32)` for it.
   - An index on `game_entity(created_at)`, for the cleanup job, and a unique constraint on `game_round_entity(game_id, round_number)`.
   - Use the table and column names Hibernate's default naming gives the entities (snake case), or set them explicitly with `@Table` and `@Column` if you prefer. Either way, this phase's verify step proves they agree.
4. **Let Flyway own the schema everywhere:** in `application.properties` set `spring.jpa.hibernate.ddl-auto=validate`, and change the H2 URL to `jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`, so the same migration runs on H2 in every test and every local run.
5. **Tests:**
   - `GameRepositoryTest` (`@DataJpaTest`) runs the migration and validates the mapping just by starting. Add a test that deleting a game removes its rounds (`deleteByCreatedAtBefore`, then count the rows in `game_round_entity` with a `JdbcTemplate` or `TestEntityManager` query).
   - Add a test that the `answer_state` column holds the enum name, not a number.

**Verify:** `mvn -B verify` passes. In the log of one Spring test, Flyway reports applying `V1` and Hibernate validates without errors.

## Phase 3: a production profile for Postgres

1. Add `src/main/resources/application-prod.properties`:
   ```properties
   spring.datasource.url=${DATABASE_URL}
   spring.datasource.username=${DATABASE_USERNAME}
   spring.datasource.password=${DATABASE_PASSWORD}
   spring.datasource.driver-class-name=org.postgresql.Driver
   # A single small instance: a few connections are plenty, and with none idle the pool lets
   # Neon's compute sleep when nobody is playing.
   spring.datasource.hikari.maximum-pool-size=5
   spring.datasource.hikari.minimum-idle=0
   spring.datasource.hikari.idle-timeout=60000
   # A database that has just woken can take a few seconds to accept connections.
   spring.datasource.hikari.connection-timeout=15000
   ```
   `DATABASE_URL` is a JDBC URL, `jdbc:postgresql://<host>/<db>?sslmode=require&channelBinding=require`, not Neon's `postgresql://user:password@...` form: the JDBC driver takes the user and password separately. **Use Neon's direct endpoint, not the `-pooler` one.** One instance with a pool of 5 doesn't need PgBouncer, and the direct endpoint avoids prepared-statement problems with transaction pooling. Write this in a comment.
2. Startup must fail fast with a clear message if the `prod` profile is on and `DATABASE_URL` isn't set. Spring's own error for an unresolved `${DATABASE_URL}` may already say so: try it (`java -jar ... --spring.profiles.active=prod`) and only add code if the message is unclear.
3. `GameCleanupService`: keep the hourly job. On Neon, each run wakes the database for at least 5 minutes, which is about 15 compute-hours a month at most. That is acceptable, and the job doesn't run while the free web service is asleep anyway. Note this in the class javadoc. Its comment that games live in memory is no longer true: update it to say old games are deleted to keep the database small.
4. A unit test for `GameCleanupService` doesn't need to change. Add a test that `application-prod.properties` loads, and that the context fails to start without `DATABASE_URL` (a `SpringApplicationBuilder` with the `prod` profile and no variable, expecting the failure). Nothing in it may connect anywhere.

**Verify:** `mvn -B verify` passes, and the jar with no profile still starts on H2: `java -jar target/geogames-0.0.1-SNAPSHOT.jar`, create a game, fetch a round.

## Phase 4: docs

Update `CLAUDE.md`:
- "Game flow / API": games are stored in H2 in memory by default and in Postgres under the `prod` profile (Render), with the schema in Flyway migrations under `db/migration` and `ddl-auto=validate`. **Any change to an entity needs a new migration** (`V2__...sql`), never an edit to an applied one. Tests run the migrations on H2 in PostgreSQL mode.
- "Environment switching": the `prod` profile and its three environment variables, and that the default profile needs none.
- Commands: how to run the app locally against a Postgres, for the manual check below.
- Link this plan's "The decision" section, so the stage 2 move and its triggers are easy to find.

**Verify:** `mvn -B verify` passes.

## Phase 5: manual check against real Postgres (needs the user)

Ask the user for the connection details of a **Neon development branch** (not the production branch) or a local Postgres, then:
1. Run the jar with `--spring.profiles.active=prod` and the three environment variables.
2. Flyway creates the schema on the empty database and Hibernate validates it.
3. Play a game through the API: create a `RANDOM` game, fetch each round, answer each one, and check the summary.
4. Restart the jar. **The game is still there**, and fetching a round still works.
5. Wait 5 minutes or more, so Neon's compute sleeps (check its console), then fetch a round. It succeeds, perhaps after a short delay. Note the delay in the report.

Report the results. Don't deploy.

## What the user does

These are the user's account and console steps. Claude shouldn't do them.
1. ~~Create a Neon account and a project on the Free plan~~ (done, 2026-11-09). Check it is **in the AWS region nearest the Render service's region** (Render's Oregon region pairs with AWS `us-west-2`, Frankfurt with `eu-central-1`, and so on). Every query crosses the network between them.
2. Create a development branch in Neon for the phase 5 check.
3. In Render, open the backend web service's **Environment** page and add three variables, taken from Neon's **direct** connection string for the main branch (the host without `-pooler`; the pooled string isn't needed):
   - `DATABASE_URL` = `jdbc:postgresql://<host>/<dbname>?sslmode=require&channelBinding=require`
   - `DATABASE_USERNAME` = `<user>`
   - `DATABASE_PASSWORD` = `<password>`

   These are safe to add before the merge, because nothing reads them until the `prod` profile is on. **Don't add any `SPRING_DATASOURCE_*` variable**: Spring reads those directly, and a build without the Postgres driver would then fail to start.
4. After this plan is merged and deployed, add `SPRING_PROFILES_ACTIVE=prod` and redeploy. Check the log: Flyway applies `V1`, and a game survives a restart.
5. Once a month, look at Neon's usage page. When compute use is on course for about 70 of the 100 free hours, or storage passes 0.5 GB, move to stage 2 (see "The decision").

## Later, not in this plan

- **Next: `game-stats.md`.** It adds a `round_result` table to this database with a `V2` migration, and two columns to `game_entity`. Keep `V1` as this plan leaves it.

- **Longer retention** for share links or a history: raise `geogames.games.max-age` and check the storage estimate (about 1–2 KB a game).
- **Stage 2:** create the Render Postgres, change the three environment variables, deploy. Flyway creates the schema, and no code changes.
