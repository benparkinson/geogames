# Plan: update dependencies and fix known vulnerabilities

Work on a new branch, `dependency-updates`. Do not commit unless the user asks. Read `CLAUDE.md` first.

Do the phases in order. Each phase ends with a verification step. **Do not start the next phase until that step passes.** If a step fails and the fix is not obvious after two attempts, stop and report to the user with the error output. Do not paper over failures (no `--legacy-peer-deps`, `--force`, `@ts-ignore`, `-DskipTests`, or deleted tests).

Pin exact versions you look up (no `latest` or open ranges in `pom.xml`). For npm use caret ranges of the version that `npm view <pkg> version` reports today. Always check the current latest version yourself: the version numbers below are minimums, not targets.

## Where things stand (checked on 2026-11-06)

**Frontend (`client/`)**: `npm audit` reports 71 vulnerabilities (5 critical, 32 high).
- Almost all come from `react-scripts@5.0.1` (Create React App, abandoned). **Nothing uses it**: the app is built with Next.js. The same goes for `@testing-library/*` and `web-vitals` (no tests and no imports exist).
- `next@13.5.8` itself has a critical advisory. Latest is 16.x.
- `react-query@3` is the old package name. Its successor is `@tanstack/react-query` v5.
- `react@18` → 19, `react-leaflet@4` → 5 (v5 requires React 19).
- `@types/react-leaflet@3` is a stale type package for an older react-leaflet. react-leaflet ships its own types. `@types/react-modal` is unused.
- The `build` script runs `next export`, which was removed in Next 14.
- `next lint` was removed in Next 16.

**Repo root**: `package.json`, `package-lock.json` and `node_modules/` at the root only list `bootstrap` and `react-bootstrap` as devDependencies. They look like a stray `npm install` run in the wrong folder.

**Backend**:
- Spring Boot `3.4.1`. The 3.x line is out of open-source support, so move to Spring Boot 4.x.
- Boot 4 uses **Jackson 3**, which renames packages (`com.fasterxml.jackson.databind` → `tools.jackson.databind`). Seven files use Jackson directly (list in phase 3).
- `logbook-spring-boot-starter` `3.10.0` does not work on Boot 4. Use Logbook 4.x (4.1.0 or later).
- `h2` is pinned to `2.2.220`. Let the Boot BOM manage it instead.
- `frontend-maven-plugin` `1.6` (very old) installs Node `v18.15.0`, which is end of life. Next 16 needs Node 20.9 or later.
- `spotless-maven-plugin` `2.43.0`, google-java-format `1.25.2`.
- Local Maven is 3.5.2, but Boot 4 needs Maven 3.6.3 or later. `mvnw` exists, but `.mvn/wrapper/` is missing, so the wrapper is broken.

**Docker / CI**:
- The `Dockerfile` uses `maven:3.8.3-openjdk-17` and `openjdk:17.0.1-jdk-slim`. The `openjdk` images are deprecated and no longer patched.
- `.github/workflows/codeql.yml` uses `actions/checkout@v3`, `actions/setup-java@v3.10.0` and `github/codeql-action/*@v2`. The v2 CodeQL action is retired.

**Security issues that are not dependency versions** (handled in phase 5):
- `spring.h2.console.enabled=true` is in `application.properties`, so the deployed app exposes the H2 web console with user `sa` and an empty password. The H2 console can be used for remote code execution.
- `spring-boot-starter-data-rest` auto-exposes `GameRepository` as a writable REST resource (`/gameEntities` with GET/POST/PUT/PATCH/DELETE). The frontend only uses the hand-written `/api/...` controllers.

## Phase 0: baseline

1. `git checkout -b dependency-updates`.
2. Record a baseline in your notes (not a file): `cd client && npm audit --omit=dev` (the summary line), and `mvn test` (pass/fail counts). If `mvn` is too old to build, note that and continue. Phase 2 fixes it.
3. In `client/`, run `npm run build` and confirm it currently succeeds. Note which routes it outputs, especially how `pages/games/[gameId].tsx` is exported (look in `client/out/games/`). The new build must produce the same layout.
4. Check how Spring serves `/games/<id>` when the page is loaded directly (search `src/main` for a controller or `WebMvcConfigurer` that forwards it, and check how `HomePage.tsx` navigates to a game). Write down the current behaviour. Later phases must keep it the same. Don't fix it if it's broken. Just report it.

## Phase 1: frontend

All commands run in `client/`.

### 1.1 Remove unused packages

`npm uninstall react-scripts @testing-library/jest-dom @testing-library/react @testing-library/user-event web-vitals @types/react-leaflet @types/react-modal`

Before removing each one, grep `client/src` and `client/pages` to confirm it has no imports. Also delete the `browserslist` block from `package.json` only if nothing else needs it. Next uses `browserslist` for CSS prefixing, so **keep it**.

### 1.2 Upgrade packages

Install the latest versions:
- `next`, `eslint-config-next`, `@next/eslint-plugin-next` (all the same major, 16.x or later)
- `react`, `react-dom` (19.x)
- `react-leaflet` (5.x), `leaflet` (latest 1.9.x; **do not** move to Leaflet 2.x), `@types/leaflet`
- `bootstrap` (latest 5.x), `react-bootstrap` (latest 2.x), `react-bootstrap-icons`
- Add as devDependencies: `typescript`, `@types/react`, `@types/react-dom` (19.x), `eslint` (the major that `eslint-config-next` declares as a peer dependency)
- Replace `react-query` with `@tanstack/react-query` (5.x): `npm uninstall react-query && npm install @tanstack/react-query`

Then `rm -rf node_modules package-lock.json && npm install` so the lockfile has no leftover CRA dependencies. `npm install` must finish without peer-dependency errors.

### 1.3 Code changes

**Static export.** Create `client/next.config.mjs`:

```js
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
};
export default nextConfig;
```

Change the `build` script to `next build`. It still writes to `client/out/`, which is what `maven-antrun-plugin` copies, so `pom.xml` needs no change for this.

If the build complains that `pages/games/[gameId].tsx` needs `getStaticPaths`, compare with the phase 0 output and keep the same exported file layout. Do not switch to the App Router.

**React Query v5** (`src/api/gateway.ts`, `src/api/countries.ts`, `src/api/games.ts`, `src/games/common/GameRoundPage.tsx`, `pages/index.tsx`, `pages/games/[gameId].tsx`):
- Imports go from `"react-query"` to `"@tanstack/react-query"`.
- `useQuery(key, fn, options)` becomes `useQuery({ queryKey: [key], queryFn: fn, ...options })`. Query keys must be arrays.
- `useMutation(fn, options)` becomes `useMutation({ mutationFn: fn, ...options })`.
- For mutations, `isLoading` is now `isPending`. For queries, `isLoading` still exists. Keep the same UI behaviour.
- `cacheTime` is now `gcTime`. `onSuccess`/`onError`/`onSettled` options were **removed from `useQuery`** (they still exist on `useMutation`). If a query uses them, move the logic to a `useEffect` on `data`/`error`.
- Check every option in the `QueryClient` constructor in `gateway.ts` against the v5 names.

**React 19 types:**
- The global `JSX` namespace is gone. Replace `JSX.Element` return types with `React.JSX.Element` (import React's types if needed), or just drop the annotation.
- `useRef()` with no argument is a type error. Pass `null` or an initial value.
- Remove any `defaultProps` on function components (use default parameter values instead).

**react-leaflet 5:** the API used here (`MapContainer`, `TileLayer`, `GeoJSON`, `Marker`, `Tooltip`, `useMap`) is unchanged. Fix any type errors the compiler reports. Keep the `*MapWrapper.tsx` + `next/dynamic` with `ssr: false` pattern.

**Lint.** `next lint` no longer exists.
- Delete `client/.eslintrc.json` and create `client/eslint.config.mjs` using the flat config that the installed `eslint-config-next` README documents (equivalent to `next/core-web-vitals`, plus the TypeScript config).
- Change the `lint` script to `eslint .`. Make sure `out/` and `.next/` are ignored.
- Fix the new lint errors. Only change behaviour if a rule points at a real bug, and list any such fixes in your final report. Turn off a rule only if it needs a large refactor, and say so in the report.

### 1.4 Verify phase 1

- `npx tsc --noEmit` passes.
- `npm run lint` passes.
- `npm run build` succeeds and `out/` has the same page layout as in phase 0.
- `npm audit --omit=dev` reports **0 critical and 0 high**. If any remain, run `npm explain <pkg>` and report what pulls them in. Use `overrides` in `package.json` only for a transitive dependency with a patched version that's compatible, and list each override in your report.
- Run `npm run dev`. Ask the user to check in a browser that the home page, a Tripoint game and a Rivers game work, including dark mode and the country autocomplete. You cannot check this yourself, so don't claim it was checked.

## Phase 2: build tooling

1. **Maven wrapper.** Regenerate it: `mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=<latest 3.9.x>` (look up the newest wrapper plugin and Maven 3.9.x versions first). This creates `.mvn/wrapper/maven-wrapper.properties` and updates `mvnw`/`mvnw.cmd`. If local Maven 3.5.2 can't run the plugin, stop and ask the user to install Maven 3.9 or to download the wrapper files themselves. From now on use `./mvnw` (Git Bash) or `mvnw.cmd`.
2. **Node via Maven.** In `pom.xml`, set `frontend-maven-plugin` to its latest version (1.15.x or later). Set `nodeVersion` to the latest Node 24 LTS (`v24.x.y`) and remove `npmVersion` so the npm bundled with that Node is used. In the `npm install` execution, change the argument to `ci` so builds use the lockfile exactly.
3. Run `./mvnw -DskipTests clean package` **only** to confirm the frontend half of the build works with the new Node. (Skipping tests is OK here only because phase 3 has not started and tests run at the end of phase 3.) Expect it to pass with Boot 3.4.1 still in place.

## Phase 3: Spring Boot 4 and Jackson 3

Read the official "Spring Boot 4.0 Migration Guide" on the spring-projects/spring-boot GitHub wiki before starting, and follow it where it differs from this plan.

### 3.1 `pom.xml`

- `spring-boot-starter-parent`: the latest GA 4.x.
- Keep `java.version` at 17. Boot 4 supports it, and the local JDK is 17.
- Starter renames in Boot 4: `spring-boot-starter-web` → `spring-boot-starter-webmvc`. Use the new name. Remove `spring-boot-starter`, because the other starters already include it.
- Tests: Boot 4 splits test support into per-technology modules. Keep `spring-boot-starter-test`. If `@SpringBootTest` or anything else fails to resolve, add the matching `spring-boot-starter-*-test` module that the migration guide names.
- `logbook-spring-boot-starter`: latest 4.x. Check that `org.zalando.logbook.Strategy` and `org.zalando.logbook.core.WithoutBodyStrategy` (used in `config/LogConfig.java`) still exist. If they moved, update the imports.
- `h2`: remove the `<version>` so the Boot BOM manages it.
- `jsoup`: latest.
- `spotless-maven-plugin` and `googleJavaFormat`: latest versions.
- The `spring-boot-maven-plugin` config excludes Lombok, which the project doesn't use. Delete that `<configuration>` block.

### 3.2 Jackson 3

These files use Jackson directly:

- `service/FileLoaderService.java`
- `service/GameService.java` (injects the Spring-managed `ObjectMapper` and declares `throws JsonProcessingException`)
- `loaders/countries/CountryWriter.java`
- `loaders/countries/ProcessedCountryReader.java`
- `loaders/countries/RawCountryReader.java`
- `loaders/tripoint/TripointWriter.java`
- plus anything else that `grep -rn "com.fasterxml.jackson" src/` finds, including tests and `model/geojson/`

Changes:
- `com.fasterxml.jackson.databind.*` → `tools.jackson.databind.*`, and `com.fasterxml.jackson.core.*` → `tools.jackson.core.*`.
- **Annotations keep their old package** (`com.fasterxml.jackson.annotation.*`). Do not rename those imports.
- Jackson 3 mappers are immutable. Replace `new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)` with `JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()`. Replace a plain `new ObjectMapper()` with `new JsonMapper()` or `JsonMapper.builder().build()`.
- `JsonProcessingException` is gone. Jackson 3 throws the unchecked `tools.jackson.core.JacksonException`. Remove `throws JsonProcessingException` clauses. Where a `catch (IOException e)` now fails to compile with "exception is never thrown", narrow or remove it. Keep `IOException` handling that still applies to real file or stream I/O.
- `GameService` should keep injecting the Spring-provided mapper (type `tools.jackson.databind.ObjectMapper` or `JsonMapper`).
- **Do not use the Jackson 2 compatibility module.** Migrate properly.

**Check JSON output against the baseline.** Jackson 3 changes some defaults, for example sorting properties alphabetically. The frontend `JSON.parse`s `jsonBlob`, and field order does not matter to it, but **field names and value shapes must not change**. Do this check:
1. Before switching the parent version, on the phase 2 build, start the app, create a `TRIPOINT` game and a `RIVERS_BY_SHAPE` game with `curl`, and save the round JSON and the `/api/countries` response in the scratchpad.
2. Do the same after the migration.
3. Compare the key sets and structure, for example with `jq 'paths | map(tostring) | join(".")'` on both and a `diff` of the sorted output. Report any difference. Only order differences are acceptable.

Also confirm `CountryLoaderMain` and `TripointLoaderMain` still compile. **Do not run them**: `TripointLoaderMain` scrapes Wikipedia and rewrites curated data.

### 3.3 Verify phase 3

- `./mvnw spotless:apply`, then `./mvnw clean install`. All tests pass, and the jar contains `public/index.html` (check with `jar tf target/geogames-0.0.1-SNAPSHOT.jar | grep public/index.html`).
- Run `java -jar target/geogames-0.0.1-SNAPSHOT.jar` and smoke-test:
  - `GET /api/countries` → 200 with a non-empty array.
  - `POST /api/games` `{"gameType":"RANDOM","numberOfRounds":3}` → 200, then `GET /api/games/{id}/rounds/1` → 200, then `POST .../answers` `{"answerState":"CORRECT"}` → 2xx.
  - `GET /` → 200 HTML.
  - Logbook still logs requests at TRACE.
  - Direct load of `/games/<id>` behaves as recorded in phase 0.
- The JSON comparison from 3.2 shows no structural changes.

## Phase 4: Docker and CI

1. `Dockerfile`:
   - Build stage: `maven:3.9-eclipse-temurin-17`.
   - Runtime stage: `eclipse-temurin:17-jre`.
   - Keep the copy paths, the `-Xmx400M`, the port and the entrypoint. Write `AS` in uppercase in `FROM ... AS build`.
   - If Docker is installed, run `docker build .` to check it. Otherwise say it wasn't checked.
2. `.github/workflows/codeql.yml`: bump `actions/checkout` and `actions/setup-java` to their current major versions and `github/codeql-action/*` to the current major (v3 or later). Keep Java 17. Leave everything else as it is.
3. Add `.github/dependabot.yml` with weekly updates for `maven` (directory `/`), `npm` (directory `/client`), `github-actions` (directory `/`) and `docker` (directory `/`). Group minor and patch updates per ecosystem so the user gets a few PRs instead of dozens.
4. The stray root `package.json`/`package-lock.json`/`node_modules`: grep the repo, `Dockerfile`, `pom.xml` and the workflows for references. If nothing uses them, **ask the user** before deleting them, because they may be intentional.

## Phase 5: hardening (ask first)

These change runtime behaviour, so **ask the user to confirm each one** before applying it (use AskUserQuestion with both questions at once):

1. **Disable the H2 console in the deployed app.** Remove `spring.h2.console.enabled=true` from `application.properties`. If the user wants it locally, put it in a dev profile (`application-dev.properties`) instead. On Boot 4 it also needs the `spring-boot-h2console` module, so add that dependency only for the dev case.
2. **Remove `spring-boot-starter-data-rest`.** First grep `client/` for any call to `/gameEntities`, `/gameRoundEntities` or `/profile`. If none exist, remove the dependency. Then check that `/gameEntities` returns 404 and that the phase 3 smoke tests still pass.

## Final report to the user

Keep it short. Include:
- The old → new version table for direct dependencies (frontend and backend).
- `npm audit` before and after (summary lines).
- Every behaviour or lint-driven code change outside plain upgrade mechanics, every npm `override`, and any lint rule you turned off.
- What you could not check (browser testing, Docker build if Docker wasn't available) and what the user should check by hand.
- That nothing has been committed.
