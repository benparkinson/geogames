# Plan: privacy page, credits on every page, and a licence

Read `CLAUDE.md`, `client/CLAUDE.md` and `e2e/CLAUDE.md` first. This plan adds a page and a footer component to the frontend, one forward to the backend's web configuration, and a `LICENSE` file. The API, the contract shapes, the database and the game logic stay as they are.

Work on a new branch `privacy-and-licence` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

Do the steps in order. If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Why

A review of the app found it stores no personal data itself, but four things are missing:

1. Nothing tells visitors that third parties see their IP address: Render (the host) on every request, and EOX (map tiles, Austria) whenever a satellite map is shown.
2. There is no notice saying who runs the site and how to reach them, which GDPR Art. 13 expects once any personal data (an IP address counts) is processed.
3. The credits and the privacy sentences exist only in the home page footer. A visitor who opens a link to a game sees Natural Earth and Wikipedia-derived data with no credit.
4. The repo is public with no licence. The owner wants the code to be readable and reusable, but not copied for commercial gain.

## Step 0: ask the user three things

The privacy page needs facts only the owner knows. Ask before writing anything, in one message:

1. The name to show as the person who runs the site (for example "Ben Parkinson").
2. A contact for privacy questions: an email address, or a link such as the repo's issues page. Never take an address from git history or anywhere else; use only what the user gives you.
3. The country the owner is based in, which decides the regulator named on the page (the ICO for the UK, otherwise that country's data protection authority).

## Step 1: the licence

The owner's wish (source public, non-commercial reuse allowed, commercial reuse not) matches the **PolyForm Noncommercial License 1.0.0**. Every commit is by the owner or Dependabot, so no one else's consent is needed.

1. Download the licence text; do not write it from memory:
   `curl -fsSL https://raw.githubusercontent.com/polyformproject/polyform-licenses/1.0.0/PolyForm-Noncommercial-1.0.0.md`
   Check that the result starts with the heading "PolyForm Noncommercial License 1.0.0" and contains the sections "Noncommercial Purposes" and "No Liability". If the download fails or the text looks wrong, stop and tell the user. Do not substitute another licence.
2. Save it unchanged as `LICENSE` at the repo root.
3. Create `NOTICE.md` at the repo root with:
   - The line the licence calls a required notice: `Required Notice: Copyright (c) <first year>-<this year> <owner name> (https://github.com/benparkinson/geogames)`. Take the first year from `git log --reverse --format=%ad --date=format:%Y | head -1`, and the name from step 0.
   - One sentence in plain words: anyone may read, run, change and share this code for non-commercial purposes; commercial use needs the owner's permission; `LICENSE` is the binding text.
   - A section "Not covered by this licence", because these files belong to others and keep their own terms:
     - `geodata/` and `src/main/resources/processed/` hold data from Natural Earth (public domain) and Wikidata (CC0).
     - `src/main/resources/processed/tripoints.json` is derived from Wikipedia's list of tripoints and is under CC BY-SA 4.0.
     - `client/public/flag.png` and `client/public/globe.png` are Flaticon icons (by Earthz Stocker and Freepik) under Flaticon's licence.
     - Third-party packages keep their own licences; the built site lists them in `third-party-licenses.txt`.
4. `README.md`: add a short "Licence" section at the end that says the code is source-available under the PolyForm Noncommercial License 1.0.0 and points to `LICENSE` and `NOTICE.md`. Do not call it "open source": this licence does not meet the Open Source Definition, and saying so would mislead.
5. `client/package.json` and `e2e/package.json`: add `"license": "SEE LICENSE IN ../LICENSE"`. Leave `pom.xml` alone.

## Step 2: a shared footer

Create `client/src/components/SiteFooter.tsx` and move the `<footer>` out of `HomePage.tsx` into it, unchanged apart from these edits:

- After "Tripoints: Wikipedia (CC BY-SA 4.0)" add ", positions adjusted" inside the same credit. CC BY-SA asks that changes are indicated, and the loader snaps each coordinate to a border vertex.
- After the two existing privacy sentences, add a Next `<Link href="/privacy">Privacy</Link>` on its own line. Keep both existing sentences word for word: tests match them.
- Take the outer `className` as an optional prop so each page can set its own spacing.

Use `SiteFooter` in three places:

- `HomePage.tsx`, where the footer was.
- `GameSummary.tsx`, below the card.
- The privacy page (step 3).

Tests:

- Move the two footer tests from `HomePage.test.tsx` into a new `SiteFooter.test.tsx`, add an assertion that the "Privacy" link points to `/privacy`, and one that the footer contains "positions adjusted".
- Leave one assertion in `HomePage.test.tsx` and add one to `GameSummary.test.tsx` that a `contentinfo` landmark is rendered.

Check: `bash scripts/check-frontend.sh src/components src/games/common`.

## Step 3: the privacy page

Create `client/pages/privacy.tsx` (a thin page like `pages/index.tsx`, with `<title>Privacy | GeoGauntlet</title>`) and its content in `client/src/components/PrivacyPage.tsx`. Never put a test file under `pages/`.

Layout: follow `GameSummary.tsx`: a `min-vh-100 page-bg` wrapper, a top row with `HomeLink` and `ThemeToggle`, the text in a `gg-card`, then `SiteFooter`. Use only theme classes and tokens; `npm run lint` rejects colour literals. One `<h1>` "Privacy", and an `<h2>` per section.

Write the page in plain, short sentences. Every statement below was checked against the code; do not add claims that aren't here (no hosting region, no log retention periods, no promises about what Render or EOX do).

- **Who runs this site.** The name and contact from step 0.
- **What we store.** No accounts, no cookies, no analytics, no advertising. A game is a random id plus the questions asked and how each round ended; it is deleted after 24 hours.
- **Anonymous statistics.** For each answered round we keep the date, the game type, the question, the result, and the hints and wrong guesses used. Nothing in it identifies a person or a device, so it is kept indefinitely.
- **On your device.** One item in the browser's local storage, `gg-theme`, set only if you use the light/dark toggle. Clearing site data removes it.
- **Who else receives data.** Loading any web page sends your IP address to the server that hosts it. Two companies receive yours:
  - Render (render.com, a United States company) hosts this site and its game server. Link to `https://render.com/privacy`.
  - EOX IT Services GmbH (Austria) serves the satellite images. Your browser asks EOX for them directly whenever a satellite map is shown. Link to `https://cloudless.eox.at`.
  - The "Buy me a coffee" link opens Ko-fi; nothing is sent to Ko-fi unless you click it.
- **Why this is allowed.** The legal basis is legitimate interests: the site cannot be delivered without these requests.
- **Your rights.** You can ask what data we hold about you and ask for it to be deleted, using the contact above. We hold nothing that identifies you, so there will usually be nothing to find. You can also complain to the regulator from step 0 (name it and link to its website).
- **Changes.** A "Last updated" line with today's date as a fixed string.

Tests, in `PrivacyPage.test.tsx`: the heading renders; the owner's name and contact are shown; the Render and EOX links have the right `href`; the text mentions `gg-theme`; the footer is present.

Check: `bash scripts/check-frontend.sh src/components`.

## Step 4: make `/privacy` load directly from the jar

The site is a static export, so the build writes `privacy.html`. Clicking the footer link works everywhere (client-side navigation), but typing `/privacy` or reloading it is a 404 from the jar, the same known problem `/games/{id}` has (`HttpServerIT`, `e2e/tests/deep-link.spec.ts`). Fix it for this one page only; leave the games bug as it is.

- `config/WebConfig.java`: override `addViewControllers` and register `/privacy` with the view name `forward:/privacy.html`.
- Add a unit test so the JaCoCo gate still passes: a `@WebMvcTest` (see the backend `CLAUDE.md` for the Boot 4 packages) that requests `/privacy` and asserts `forwardedUrl("/privacy.html")`.
- `HttpServerIT`: add a test that `GET /privacy` is a 200 whose body contains "Privacy". Keep the existing 404 test for `/games/1`.

Check: `bash scripts/check-java.sh`, then `bash scripts/check-it.sh -Dit.test=HttpServerIT` (this one builds the frontend, so it is slow).

## Step 5: credits on the game screen

The game screen fills the viewport and has no room for the footer. Put the credits where map credits conventionally go: Leaflet's attribution control, which every map already shows in its corner.

- Create `client/src/map/MapCredits.tsx`: a component that returns `null`, takes an optional `extra` string, and in an effect calls `map.attributionControl.addAttribution(...)` (removing it again on cleanup) with this HTML: `Map data: Natural Earth | <a href="/privacy">Privacy</a>`, preceded by `extra` when given. `MapResizer.tsx` is the model for a component built on `useMap()`.
- Add `<MapCredits />` to each of the seven maps: `CoastlineMap`, `LandBordersMap`, `NeighboursMap`, `LakeShapesMap`, `RiverCountriesMap`, `RiverShapesMap` and `TripointMap`. In `TripointMap` pass `extra="Tripoints: Wikipedia (CC BY-SA 4.0), positions adjusted"`.
- The link is a plain `<a>`, so it does a full page load. That is why step 4 comes first.

Tests: a `MapCredits.test.tsx` with a fake map, in the style of `FitBounds.test.tsx`, checking the text added and that it is removed on unmount. The map tests render through `src/test/leafletMock`; extend the mock so `MapCredits` renders there without a real map (for example as a `<div>` carrying its text), and add one assertion to `TripointMap.test.tsx` that the Wikipedia credit is drawn.

Check: `bash scripts/check-frontend.sh src/map src/games`.

## Step 6: browser test and docs

- `e2e/tests/home.spec.ts`: add one test: from the home page, click "Privacy" in the footer and expect the "Privacy" heading; then `page.goto("/privacy")` directly and expect it again. Follow `e2e/CLAUDE.md` to run it. Do not add another `test.fail()`.
- `client/CLAUDE.md`, the **Credits** section: say the credits now live in `components/SiteFooter` (home, summary and privacy pages) and `map/MapCredits` (every map); every new map must include `MapCredits`; a new data source or third party that receives requests from the browser needs a line on the privacy page as well as a credit.
- Root `CLAUDE.md`: in "Storage and privacy", add one sentence that `/privacy` describes what is stored and who receives requests, and must be updated when that changes. In the "Environment switching" or "Build coupling" paragraph, note the `/privacy` forward in `WebConfig`.
- `client/scripts/third-party-licenses.mjs`: the header says the data credits "are on the home page"; change it to "are in the site footer".

## Step 7: check everything

1. `bash scripts/check-frontend.sh`
2. `bash scripts/check-java.sh`
3. `bash scripts/check-it.sh`
4. The e2e test from step 6.

## Report to the user

Say what changed, then pass on these points, which need the owner and not more code:

- **Read the privacy page before it goes live.** The wording was written from the code, not by a lawyer.
- **Check `/privacy` on the deployed Render site.** Open `https://geogauntlet.onrender.com/privacy` directly after the deploy. If it is a 404, add a rewrite in the Render dashboard for the static site: source `/privacy`, destination `/privacy.html`, action Rewrite. Render's docs don't say whether it resolves this on its own.
- **The licence is not retroactive in practice and is not "open source".** Anyone who copied the repo before the licence was added had no licence at all, so nothing is lost. PolyForm Noncommercial is a source-available licence; GitHub may show it as "Other".
- **Where `geodata/countries/countries.geo.json` came from.** Its properties (`name`, `ISO_A3`, `ISO_A2`) match the `datasets/geo-countries` distribution of Natural Earth, which is public domain (PDDL). If that is right, the existing Natural Earth credit covers it; if it came from somewhere else, that source's licence needs checking.

## Not in this plan

- The `/games/{id}` reload 404.
- H2 or request logging in the default profile.
- Proxying or self-hosting map tiles.
- Any change to what the server stores.
