# Browser tests

Playwright with Chromium, with its own `package.json`. The tests play every game in a real browser against the jar, with no internet: all non-local requests are blocked, and a test fails if the page called `onrender.com`.

Build the jar with the API override, then run them (Playwright starts the jar itself; see `README.md`):

```
NEXT_PUBLIC_API_URL=http://localhost:8080 ./mvnw -B -DskipTests package
cd e2e && npm ci && npx playwright install chromium && npx playwright test
```

`deep-link.spec.ts` is marked `test.fail()` for a known bug (reloading `/games/{id}` served by the jar is a 404). When the bug is fixed that test starts failing, so remove the marker.
