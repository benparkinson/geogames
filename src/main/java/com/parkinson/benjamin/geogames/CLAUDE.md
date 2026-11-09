# Backend

Spring Boot 4, Java 25. The root `CLAUDE.md` has the commands, the API overview and the scoring rules.

## Conventions

- Loader services read classpath JSON through `service/JsonResourceReader`, and the offline `loaders/` programs share the Jackson settings in `helper/GeoJson`. Data that fails to load is an `UncheckedIOException` (a packaging bug), not a checked exception.
- `GameService` methods are `@Transactional` and `spring.jpa.open-in-view` is off, so the lazy `rounds` collection must be read inside the service.
- `config/` holds caching, CORS (`WebConfig`, applied to `/api/**`), `Random`, logging and the stats executor.

## Tests

JUnit 5, AssertJ and Mockito. Boot 4 moved the test slices out of `spring-boot-test-autoconfigure`, so `@WebMvcTest` / `@AutoConfigureMockMvc` come from `spring-boot-starter-webmvc-test` (package `org.springframework.boot.webmvc.test.autoconfigure`) and `@DataJpaTest` from `spring-boot-starter-data-jpa-test`. Use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`), not `@MockBean`. No test may hit the network. `TripointComputer` is tested against the hand-written HTML in `src/test/resources/fixtures/`. Run them with `bash scripts/check-java.sh` (unit) and `bash scripts/check-it.sh` (`*IT`).

## API rules

- Invalid input (rounds outside 1–10, an unknown `gameType`, more rounds than there is data for, or answering with `UNANSWERED`) is a 400 with a `ProblemDetail` body (`controllers/ApiExceptionHandler`).
- A malformed game id (including an old numeric one) is a 404, the same as an unknown game.
- Answers: a missing `hintsUsed` counts as 0, and more than the type's `hintCount` is a 400. A missing `wrongGuesses` is stored as unknown (`null`), not 0, so an old client can be told apart from a round with no wrong guesses. `wrongGuesses` is a 400 when it is negative, more than the type's `guessLimit`, not equal to `guessLimit` for `OUT_OF_GUESSES`, or equal to `guessLimit` for `CORRECT` / `GAVE_UP`. It is stored on the round but not returned, so the response contract is unchanged. Repeating the identical answer is a no-op; any other answer to an answered round is a 409.
- `/api/countries`, `/api/rivers` and `/api/lakes` are cached.

## Persistence

- H2 in memory (PostgreSQL mode) by default, so games are lost on restart; Postgres under `prod`. Flyway owns the schema (`src/main/resources/db/migration`, `ddl-auto=validate`), and the same migrations run on H2 in every test. Any entity change needs a new `V<n>__...sql` migration, never an edit to an applied one.
- `GameEntity.publicId` (random UUID) is the id in URLs; the `long` key never leaves the server. `created_at` is rounded down to the hour.
- `GameCleanupService` deletes each game `geogames.games.max-age` (24 h) after it is created, in a job that runs at start-up and then every `geogames.games.cleanup-interval` (1 h). The free Render service rarely stays up an hour, so the start-up run usually does the work.
- Each `GameRoundEntity` stores only a short `roundKey` (such as `country:France`, `river:Q46841` or `tripoint:A|B|C@lat,lon`), not the round's data: the geometry is already in the jar and cached by the loaders, and storing it made a 10-round game about 0.5 MB of heap. `GameService` rebuilds the round with its type's generator (`RoundGenerator.rebuild(roundKey)`) and serialises it into `GameRound.jsonBlob` on every read. A key whose data has gone (a deploy removed a river or renamed a country) throws `RoundDataMissingException`, which `ApiExceptionHandler` turns into a 410 with a `ProblemDetail`. Don't cache the rebuilt JSON: it would bring back the heap this avoids.

## Usage statistics

`GameService.submitAnswer` publishes a `RoundAnswered` event on the first answer to a round (not on a repeat or a 409, and not for a game with no `playId`). `service/stats/RoundStatsRecorder` handles it after the answer's transaction commits, on the one-thread `statsExecutor` (`config/StatsConfig`, queue of 1000), and `RoundStatsStore` splits the flat `RoundResult` across a small star schema with `JdbcClient`, in the same database as the games: one `stat_round` row per answered round, one `stat_game` row per game, and lookup tables (`stat_game_mode`, `stat_game_type` with its hint count and guess limit, `stat_answer_state`, `stat_round_key`). `round_result` is a read-only view that joins them back to the flat columns; write through `RoundStatsStore`. The lookup rows are created on first use and cached by id, so they are never deleted or updated, and tests never clean them. `stat_round` is kept at 8 fixed-width columns on purpose (see `docs/plans/normalise-game-stats.md`, alignment note). A failure or a full queue only logs a WARN and drops the row, so stats never break or slow an answer. The `stat_*` tables are created by Flyway migrations, have no foreign keys to the game tables and are never deleted by `GameCleanupService`.

Anonymity rules: never store an IP, a header, either game id, a timestamp (only the UTC date) or anything from the browser, and never expose or log `GameEntity.playId` (the random id that links the rounds of one game, not the `publicId`). `wrong_guesses` is `NULL` when the client didn't report it; don't turn that into 0. `geogames.stats.enabled=false` turns recording off. A new `GameType` needs nothing extra for stats.

## Game types

`GameType` is persisted with `@Enumerated(EnumType.STRING)` and sent to clients as `api/GameTypeResponse` (`{name, answerCount, hintCount, guessLimit}`), so the JSON shape doesn't depend on the enum. `RANDOM` is a request-only value, parsed by `service/GameSelection`, and is not a `GameType`.

Adding a type (the root `CLAUDE.md` has the four steps):
- **The `GameData` record** in `model/`: `gameType()` returns the constant, `subjectId()` says what the round is about, and `roundKey()` (defaults to `subjectId()`) is what is stored. The key must be unique within the type, so override it as `Tripoint` does when `subjectId()` isn't.
- **The generator** implements `service/RoundGenerator`: a `@Service`, or a `@Bean` in a `@Configuration` when several types share one class. Startup fails with an `IllegalStateException` if a `GameType` has no generator, or has two. Generators take the shared `java.util.Random` bean (`config/RandomConfig`) and pick with `helper/RandomPicker`, so tests can pass a seeded `Random`.
  - `generate(count, excludedSubjects)` must not return a round whose `subjectId` is excluded. `GameService` passes the subjects already used in a `RANDOM` game, so a mixed game never asks about the same tripoint or river twice (a river by shape and the same river on the map).
  - `rebuild(roundKey)` must build the round with the same private method `generate` uses (so the two can't drift), must not apply the eligibility filter, and throws `RoundDataMissingException` for a key with no data. `RoundRebuildIT` checks every generator against all the real data automatically.

## Game families

**Tripoints.** `TripointFinderService` picks random definitions from `processed/tripoints.json` and builds each country with `CountryShapeService.loadShape(name).toCountry()`, so `jsonBlob` carries the simplified `processed/country-shapes` outline (in the usual `Country` shape), not the full-resolution geometry of `processed/countries`.

**Rivers.** Four types share the 50 rivers in `processed/rivers` (read by classpath glob, cached as `rivers`): `RIVERS_BY_SHAPE` (the line on a blank map), `RIVERS_ON_MAP` (the line on satellite imagery, limited zoom, no panning), `RIVERS_BY_ENDS` (a flag at the source and the mouth) and `RIVERS_BY_COUNTRIES` (the outlines of the countries it flows through; only rivers through three or more). `model/River` is the file format and `model/RiverRound` (`gameType`, `river`, `countries`) the round data. `service/RiverRoundGenerator` is one class, registered as four beans in `config/RiverGamesConfig`, each with an eligibility filter and a function that attaches countries. To remove a variant, delete its `GameType` constant, its `@Bean`, and its frontend module, registry entry and map pair.

**Lakes.** `LAKES_BY_SHAPE` (`answerCount` 1, `hintCount` 3, `guessLimit` 5). `model/Lake` is the file format in `processed/lakes` and `model/LakeRound` (`gameType`, `lake`) the round data; its subject id is `lake:<QID>`. `service/LakeLoaderService` reads the files (cached as `lakes`, plus `lakeNames` for `GET /api/lakes`), and `service/LakeRoundGenerator` is a plain `@Service`.

**Country shapes.** Three types use `processed/country-shapes`: `COUNTRIES_BY_NEIGHBOURS` ("Country Gaps": the neighbours' filled outlines, the country is the gap), `COUNTRIES_BY_COASTLINE` (only the coast, as lines) and `COUNTRIES_BY_LAND_BORDERS` (only the land borders, as lines). `model/CountryShape` is the file format and `model/CountryShapeRound` (`gameType`, `country`, `neighbours` as `CountryOutline`s) the round data; its subject id is `country:<name>`, shared by all three. `service/CountryShapeRoundGenerator` is one class, registered as three beans in `config/CountryGamesConfig`, with the eligibility thresholds (and the reason for each) in `helper/CountryShapeEligibility`. Removing a variant works as for rivers.
