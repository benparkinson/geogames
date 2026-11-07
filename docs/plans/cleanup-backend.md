# Plan: backend clean-up

Read `CLAUDE.md` first. This plan covers the Spring Boot app, the offline loaders under `loaders/`, and repo-root hygiene. Do not change anything under `client/`. A separate plan (`cleanup-frontend.md`) covers the frontend and may run in parallel, so only edit the backend parts of `CLAUDE.md`.

Work on a new branch, `cleanup-backend`, created from the branch that holds the unit tests (see phase 0). Do not commit unless the user asks.

Do the phases in order. Each phase ends with a verification step. **Do not start the next phase until that step passes.** If a step fails and the fix is not obvious after two attempts, stop and report to the user with the error output.

## Goal

Make adding a game type a small, local change: a `GameType` constant, a `GameData` record and one `RoundGenerator` bean, with no edits to `GameService`. Remove duplication and dead code along the way, fix error handling, and make randomness injectable.

## Ground rules

- **The HTTP API contract must not change.** Same paths, methods and JSON field names and shapes, including `gameType` as `{"name": "...", "answerCount": n}` and `jsonBlob` as a JSON **string**. The frontend is deployed separately and isn't being changed in step with this plan.
- **Deliberate behaviour changes** (the only ones allowed):
  1. Invalid input returns **400** with a JSON `ProblemDetail` body instead of 500. Invalid input means: rounds out of 1–10, an unknown `gameType`, more rounds than there is data for, or submitting `answerState: "UNANSWERED"` (accepted today).
  2. An unknown game type name read from the database fails loudly. Today `GameTypeConverter` silently maps it to `RIVERS_BY_SHAPE`. The database is in-memory and only ever written by this code, so this can't happen in practice.
- **Tests.** If a test fails because a class moved, was renamed or deleted, or a signature changed by design, update or move it to the new API and keep the same coverage. A test for a deleted class (for example `GameTypeConverterTest`, `OptionalHelper`) moves to whatever replaces it. If a test fails because of one of the deliberate changes above (for example one marked `// Current behaviour:`), update its assertion. Any other failure means your refactor changed behaviour: fix the code, not the test. Never delete coverage, and never `@Disabled` or skip a test. List every test you changed, and why, in your final report.
- Don't edit anything under `src/main/resources/processed/` or `geodata/`. Don't re-run the loader mains: `TripointLoaderMain` hits Wikipedia.
- Constructor injection only, with no `@Autowired` on single constructors. Prefer records for data carriers.
- Run `mvn spotless:apply` before every verification step.

## Verification commands

"**Verify (standard)**" below means all of these pass, from the repo root (use `./mvnw` if `mvn` isn't installed):

1. `mvn spotless:apply`, then `mvn spotless:check`
2. `mvn test`: all tests pass. Compare the counts with phase 0; they may only go up, or change where you moved or replaced tests.

"**API smoke test**" means: start the app (`mvn spring-boot:run`, or the jar after `mvn clean install -DskipTests`), then with `curl`:
- `POST /api/games` with `{"gameType":"TRIPOINT","numberOfRounds":3}`, then the same with `RIVERS_BY_SHAPE` and `RANDOM`. Each returns `{"id": n}`.
- `GET /api/games/{id}/rounds/0` returns all five fields. `gameType` is an object with `name` and `answerCount`, and `jsonBlob` is a string.
- `POST /api/games/{id}/rounds/0/answers` with `{"answerState":"CORRECT"}`. Then a GET shows `answerState` `CORRECT` and `gameResult.score` 1.
- `GET /api/countries` returns the list, with the `Cache-Control` header.
- `GET /health` returns `Server running!`.
- From phase 2 on: `numberOfRounds: 0`, `gameType: "NOPE"` and `answerState: "UNANSWERED"` each return 400. An unknown game id returns 404.

## Phase 0: baseline

1. Check that the unit tests exist: `src/test/java/...` contains more than the original 9 test classes (for example `GameTypeConverterTest` and controller tests). If it doesn't, **stop and ask the user** whether to continue without them.
2. `git checkout -b cleanup-backend`.
3. Run the standard verification and note the pass/fail/skip counts.
4. Run the API smoke test. Save the three responses (create, round, answer) to files in your scratch directory, so you can compare the JSON **structure** later. The data is random, so compare keys and types, not values.

## Phase 1: repo hygiene and dead code

1. **Stray root `node_modules/`.** About 2,860 files under the repo root's `node_modules/` are committed. There is no root `package.json`; the real one is in `client/`. Run `git rm -r --cached node_modules`, delete the directory, and add `/node_modules/` to `.gitignore`. Check that `client/node_modules` is still ignored and that `mvn clean install -DskipTests` still builds the frontend.
2. Delete `model/Game.java` (unused; `grep -rnw Game src` confirms it).
3. Delete the unused `GameEntity(long id)` constructor. Check with grep first.
4. Remove `@Autowired` from every single-constructor class (`GameController`, `CountryController`, `GameService`, `TripointFinderService`, `CountryLoaderService`, `RiverLoaderService`).
5. Controllers: put `@RequestMapping("/api")` on the class and use `@GetMapping` / `@PostMapping` with paths relative to it. Paths must resolve to exactly the same URLs as today.
6. `Tripoint.TripointBuilder` is only used by the offline `TripointComputer`. Move it into `loaders/tripoint` as a package-private class, so the runtime model is just the record.

**Verify (standard)** + API smoke test.

## Phase 2: CORS config and error handling

1. **CORS.** Delete both `@CrossOrigin` annotations. Add `config/WebConfig implements WebMvcConfigurer` that registers CORS for `/api/**`, taking the origins from a property:
   ```properties
   geogames.cors.allowed-origins=http://localhost:3000,https://geogauntlet.onrender.com
   ```
   Bind it with `@Value("${geogames.cors.allowed-origins}") String[] origins`, or a small `@ConfigurationProperties` record. Allow the same methods the annotations allowed by default (GET, POST, HEAD). Check: `curl -i -X OPTIONS -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: POST" localhost:8080/api/games` returns `Access-Control-Allow-Origin: http://localhost:3000`, and an unlisted origin gets no such header.
2. **Errors.** Add `controllers/ApiExceptionHandler` (`@RestControllerAdvice`) that maps `IllegalArgumentException` to `400` with a `ProblemDetail` (`ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage())`). Unexpected exceptions keep Spring's default 500.
3. `GameService.createGame` returns `GameCreationResponse`, not `Optional`; it was never empty. The controller returns `ResponseEntity.ok(...)` directly. Delete the unreachable `badRequest()` branch.
4. `submitAnswer` rejects `AnswerState.UNANSWERED` with `IllegalArgumentException`. A malformed `answerState` (not one of the enum values) already fails JSON binding with 400; leave it.

**Verify (standard)** + API smoke test, including the 400 and 404 cases. Check `CLAUDE.md`'s "Environment switching" paragraph and update it: the CORS origins are now the `geogames.cors.allowed-origins` property in `application.properties`.

## Phase 3: `GameType` as an enum, model in the right package

1. Replace the `dao/GameType` class with `model/GameType`:
   ```java
   public enum GameType {
     TRIPOINT(3),
     RIVERS_BY_SHAPE(1);

     private final int answerCount;
     GameType(int answerCount) { this.answerCount = answerCount; }
     public int answerCount() { return answerCount; }
   }
   ```
2. Move `dao/GameData` to `model/GameData`. `dao/` keeps only the entities and the repository.
3. JPA: annotate `GameRoundEntity.gameType` with `@Enumerated(EnumType.STRING)`, which stores the same strings as before. Delete `GameTypeConverter`, and move its tests to a round-trip persistence test (or adapt the existing repository test) proving a saved round reads back with the same `GameType`.
4. **Keep the wire format.** Don't serialise the enum directly. Add `GameTypeResponse(String name, int answerCount)` with `static GameTypeResponse from(GameType t)`, and use it in the `GameRound` response record. The JSON stays `{"name":"TRIPOINT","answerCount":3}`.
5. Parse the request's `gameType` string in one place: a `GameSelection` type, or a static `parse` method, that returns either "random" or a specific `GameType`, and throws `IllegalArgumentException("Unknown game type X")` otherwise. `RANDOM` stays a request-only value, not a `GameType` constant.

**Verify (standard)** + API smoke test. Diff the saved phase 0 response structure against the new one: it must be identical.

## Phase 4: pluggable round generation

1. Add `service/RoundGenerator`:
   ```java
   public interface RoundGenerator {
     GameType gameType();
     /** {@code count} distinct rounds; throws IllegalArgumentException if there isn't enough data. */
     List<GameData> generate(int count);
   }
   ```
2. **Injectable randomness.** Add `config/RandomConfig` exposing one `java.util.Random` bean (`new Random()`). Use `java.util.Random`, not `RandomGenerator`: `Collections.shuffle` only accepts `Random` on Java 17. Inject it wherever a `Random` is created today (`GameService`, `TripointFinderService`, `RiverFinderService`) and delete the three `new Random(System.currentTimeMillis())` fields. Tests can now pass a seeded `Random`.
3. Add a small `helper/RandomPicker.pickDistinct(List<T> items, int count, Random random)`. It copies the list, shuffles it, returns the first `count`, and throws `IllegalArgumentException` (with today's message wording, "Can't give you %d …, I only know about %d!", parameterised by a noun) when there are too few. It replaces both the index shuffle in `TripointFinderService` and the rejection-sampling `HashSet` loop in `RiverFinderService`.
4. `TripointFinderService implements RoundGenerator` (type `TRIPOINT`): `generate(count)` picks definitions with `RandomPicker` and loads countries as now.
5. `RiverFinderService implements RoundGenerator` (type `RIVERS_BY_SHAPE`): it now injects `RiverLoaderService`, and `generate(count)` loads the rivers and picks with `RandomPicker`. Delete `GameService`'s direct dependency on `RiverLoaderService`.
6. `GameService` takes `List<RoundGenerator>` and builds an `EnumMap<GameType, RoundGenerator>`. In the constructor, fail fast with `IllegalStateException` if any `GameType` has no generator or more than one. Then `createGame`:
   - specific type: `generators.get(type).generate(n)`.
   - random: for each of the `n` rounds pick a random `GameType.values()[...]`, count per type, generate each count, combine, then `Collections.shuffle(all, random)`. This is the same distribution as today.
   - Delete `createTripointRounds`, `createRiversByShapeRounds`, `getXRandomTripoints` and `getXRandomRivers`.
7. **IOException.** Classpath data that won't load is a packaging bug, not a recoverable condition. The loader services should catch `IOException` and rethrow it as `UncheckedIOException`, so `RoundGenerator`, `GameService` and the controllers no longer declare `throws IOException`.

**Verify (standard)** + API smoke test (all three game types). Check: `grep -rn "TRIPOINT\|RIVERS_BY_SHAPE" src/main/java` only hits the `GameType` enum and each generator's `gameType()`.

## Phase 5: `GameService` and entity tidy-up

1. **Transactions.** Annotate `getGameRound` with `@Transactional(readOnly = true)` and `submitAnswer` / `createGame` with `@Transactional` (`org.springframework.transaction.annotation`). Then set `spring.jpa.open-in-view=false` in `application.properties`. The lazy `rounds` collection must now load inside the transaction; the API smoke test proves it does.
2. **Remove `OptionalHelper`.** Add a private `Optional<GameRoundEntity> findRound(GameEntity game, int roundNumber)` and a private `GameRound toResponse(GameEntity game, GameRoundEntity round)`. Then:
   ```java
   return gameRepository.findById(gameId)
       .flatMap(game -> findRound(game, round).map(r -> toResponse(game, r)));
   ```
   `submitAnswer` does the same, setting the state first. With `@Transactional`, the explicit `save` is unnecessary; keep it or drop it, but keep the behaviour the same. Delete `helper/OptionalHelper`.
3. `calculateGameResult` takes a `GameEntity`, not an `Optional<GameEntity>`, and uses `count()` instead of `.toList().size()`. Keep it package-private and static so the existing tests can still call it (update their call sites).
4. **Entities.**
   - `GameEntity.rounds`: remove the bogus `@Lob @Column` (they don't belong on a `@OneToMany`). Add `@OrderBy("roundNumber")`.
   - Rename `GameRoundEntity.index` to `roundNumber` (getter and setter too). `index` collides with the JPQL `INDEX()` function, and the database is in-memory, so the column rename is free. Then `calculateGameResult` can drop its explicit sort, though keeping it is harmless.
   - Remove `final` from both entity classes, because Hibernate needs to subclass them for lazy proxies. Delete their hand-written `equals`/`hashCode`: the round one compares mutable fields and the generated id. Identity equality is correct for how these entities are used. If a test asserts entity equality, change it to compare ids or fields.
   - Make both ids `Long` / `long` consistently (round id is `int` today).

**Verify (standard)** + API smoke test. Check the app log: there's no open-in-view warning, and no `LazyInitializationException`.

## Phase 6: data loading without inheritance

1. **One JSON configuration.** Add `helper/GeoJson` (or `config/JsonMappers`) with two static factories: `lenientReader()` (`FAIL_ON_UNKNOWN_PROPERTIES` disabled, as `FileLoaderService` / `RawCountryReader` / `ProcessedCountryReader` do now) and `prettyWriter()` (`SORT_PROPERTIES_ALPHABETICALLY` disabled plus the default pretty printer, as `CountryWriter` / `TripointWriter` do now). Replace the four hand-built `JsonMapper`s in `loaders/` with these. The offline mains stay Spring-free.
2. **`JsonResourceReader` component** (`service/`) replaces the abstract `FileLoaderService`. It takes `ResourcePatternResolver`, uses `GeoJson.lenientReader()`, and provides:
   - `<T> T read(String classpathLocation, Class<T> type)`
   - `<T> T read(String classpathLocation, TypeReference<T> type)`
   - `<T> List<T> readAll(String pattern, Class<T> type)`: every matching resource, each stream closed with try-with-resources.

   All of them throw `UncheckedIOException` with the resource name in the message. `CountryLoaderService`, `RiverLoaderService` and `TripointLoaderService` inject it instead of extending a base class. Delete `FileLoaderService`.
3. **`RiverLoaderService`.**
   - Replace the raw `Map` and unchecked casts with `record RiverMetadata(List<String> names, String continent)`, read as `Map<String, RiverMetadata>` through a `TypeReference`.
   - Use `ResourcePatternResolver` instead of `ApplicationContext`.
   - If a river file has no metadata entry, throw `IllegalStateException` naming the river, instead of a bare NPE. Default missing `names` to `List.of()` (as `CountryLoaderService` already does for countries), so the frontend never gets `null` aliases.
   - The input streams are currently never closed; `readAll` fixes that.
4. Keep every `@Cacheable` annotation and cache name unchanged.

**Verify (standard)** + API smoke test. Also: `ProcessedDataIntegrityTest` and `CountryLoaderServiceTest` still pass, and a rivers round's `jsonBlob` still contains `name`, `additionalNames`, `continent` and `geoData`.

## Phase 7: package layout and docs

1. Move the HTTP request/response records out of `model/` into a new `api/` package: `GameCreationRequest`, `GameCreationResponse`, `GameRound`, `GameRoundAnswerRequest`, `GameResult` and `GameTypeResponse`. `model/` keeps the domain types (`GameType`, `GameData`, `AnswerState`, `GameState`, `Coordinate`, `Country`, `CountryName`, `River`, `Tripoint`, `TripointDefinition`, `geojson/`). `CountryName` stays in `model/`: it's both a file-reading type and a response. Use `git mv`, and update the imports in main and test code.
2. Final layout:
   ```
   api/          request/response records
   config/       CachingConfig, LogConfig, WebConfig, RandomConfig
   controllers/  GameController, CountryController, HealthController, ApiExceptionHandler
   dao/          GameEntity, GameRoundEntity, GameRepository
   helper/       CountryHelper, TripointHelper, RandomPicker, GeoJson
   loaders/      offline mains and their readers/writers (unchanged apart from GeoJson + TripointBuilder)
   model/        domain types
   service/      GameService, RoundGenerator, TripointFinderService, RiverFinderService,
                 CountryLoaderService, RiverLoaderService, TripointLoaderService, JsonResourceReader
   ```
3. Update `CLAUDE.md` (backend parts only):
   - "Game types": `GameType` is now a Java **enum** with an `answerCount`, persisted with `@Enumerated(STRING)` and sent to clients as `GameTypeResponse`. Adding a game type on the backend means: (1) an enum constant, (2) a `GameData` record in `model/`, and (3) a `@Service` implementing `RoundGenerator`, which `GameService` picks up automatically (startup fails if a type has no generator). `RANDOM` mixes every registered type.
   - "Game flow / API": mention that invalid input returns 400 with a `ProblemDetail`.
   - "Environment switching": CORS origins live in `geogames.cors.allowed-origins` (done in phase 2; check it's still right).
   - Fix any class names or packages the file mentions that have moved.

**Verify (standard)**, then the full build: `mvn clean install` (with tests), `java -jar target/geogames-0.0.1-SNAPSHOT.jar`, and the full API smoke test against the jar. Also open `http://localhost:8080/` and check that the bundled frontend loads and a game starts.

## Out of scope (mention in your report, don't do)

These need the frontend to change in step, so leave them for a later joint change:
- Sending `jsonBlob` as embedded JSON (`@JsonRawValue`) instead of a string.
- Storing the player's original selection (`RANDOM` versus a specific type) on `GameEntity`, so "Play again" doesn't have to infer it.
- Making answers final (409 on re-answering a round).
- Preloading the data caches at startup to cut the first-game delay.

## Final report

Include:
- The test counts before and after.
- Every test you changed, with the reason.
- The before/after JSON structure comparison.
- Any other bugs you noticed but didn't fix.
- The final package tree.
