# Browser tests

Playwright tests that play the games in Chromium against the jar, which serves the built site and the API from one port. They need no internet: every request that is not to `localhost` is blocked, and a test fails if the page tried to reach `onrender.com`.

This is a separate npm package so Playwright stays out of the client's dependencies and `next build`.

## Run them locally

From the repo root, build the jar with the site pointed at the local backend. Without `NEXT_PUBLIC_API_URL` the bundled site calls the production API and the tests fail with a message saying so.

```sh
NEXT_PUBLIC_API_URL=http://localhost:8080 mvn -B -DskipTests package
cd e2e
npm ci
npx playwright install chromium
npx playwright test
```

Playwright starts `java -jar ../target/geogames-0.0.1-SNAPSHOT.jar` itself (and reuses a server already running on that port when not in CI). Java 25 must be on the `PATH`. Use `E2E_BASE_URL` to test another port, for example `E2E_BASE_URL=http://localhost:9090 npx playwright test`.

Useful options: `--project=desktop` or `--project=mobile` (Pixel 7), `--headed`, `--ui`, and `npx playwright show-report` for the HTML report of the last run.

## What is covered

| Spec | Checks |
|---|---|
| `home.spec.ts` | The home page lists every game and the round counts. |
| `every-game.spec.ts` | Every game type, and the mix, played start to finish with the right answers. |
| `full-game.spec.ts` | A Tripoint game with a hint, a reveal and running out of guesses, and its score. |
| `guessing.spec.ts` | Unknown names, wrong guesses and aliases. |
| `deep-link.spec.ts` | Reloading a game page. Marked `test.fail()`: it is a known bug (see the comment in the file). |

`fixtures.ts` gives every test the internet block and the page-error check, plus `api` to read the correct answers from the backend.
