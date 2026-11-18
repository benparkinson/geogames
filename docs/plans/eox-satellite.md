# Plan: replace Esri satellite imagery with EOX Sentinel-2 cloudless

Read `CLAUDE.md` and `client/CLAUDE.md` first. This plan changes only the frontend (`client/`) and its docs. The backend, the API, the contract shapes and the e2e tests stay as they are.

Work on a new branch `eox-satellite` from an up-to-date `main`. If the theme work (`theme-classes`) is not merged into `main` yet, ask the user whether to branch from it instead: both touch the footer in `HomePage.tsx`. Do not commit, push or open a PR unless the user asks.

Line numbers below are from the `theme-classes` branch and may be a few lines off; find each place by its text.

## Goal

Every satellite map loads its tiles from Esri's World Imagery server. Esri's terms expect an account or API key for that, and each tile request sends the visitor's IP address to a US company. Switch to EOX's "Sentinel-2 cloudless" 2016 mosaic, which is licensed CC BY 4.0, needs no key and is served from Austria.

After this plan no file in the repo mentions Esri, Maxar, Earthstar or `arcgisonline`.

## Facts about the new source

These were checked on 2026-11-17 against `https://tiles.maps.eox.at/wmts/1.0.0/WMTSCapabilities.xml` and `https://cloudless.eox.at/documentation/license`.

- **Layer:** `s2cloudless_3857`. This is the 2016 mosaic, and its name has no year in it. Its abstract in the capabilities document says it is under the Creative Commons Attribution 4.0 International License.
- **Do not use a later year.** `s2cloudless-2018_3857` and every later layer are CC BY-NC-SA 4.0, and commercial use of them needs a paid licence. The site has a donation link, so stay on 2016. (The capabilities document was summarised to me as putting 2017 under CC BY 4.0 too, but I only confirmed 2016.)
- **Tile URL for Leaflet:** `https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/{z}/{y}/{x}.jpg`. The order is `{z}/{y}/{x}` (row before column), the same order the Esri URL uses. `g` is the Web Mercator tile matrix set. Tiles are JPEG.
- **Attribution EOX asks for:** `EOxCloudless https://cloudless.eox.at by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2016)`.
- **Zoom:** the tile matrix goes to level 21. The deepest any map in the app goes is 12 (`LakeShapesMap`), where Sentinel-2's 10 m pixels are still sharp. No `maxZoom` or `maxNativeZoom` change is needed.
- **Labels:** the layer has no place names or borders, so `RevealTiles`' comment ("Its tiles carry no labels") stays true and no game gives away an answer.

## Step 1: confirm the source before editing

No test may use the network, but you may, once, from the shell:

1. Fetch one tile and check it is an image: `curl -sI "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/3/3/4.jpg"` should answer `200` with `Content-Type: image/jpeg`. Do the same for a zoom-12 tile, for example `.../g/12/1430/2200.jpg`.
2. Fetch the capabilities document and read the `<ows:Abstract>` of the layer whose identifier is exactly `s2cloudless_3857`. It should name the Creative Commons Attribution 4.0 International License. If the abstract gives its own attribution wording, use that wording in steps 2 and 3 instead of the string above.

If either check fails (not 200, not JPEG, or a licence other than CC BY 4.0), stop and report to the user with the output. Do not pick another layer or provider yourself.

## Step 2: the tile layer

`client/src/map/constants.ts`:

- Set `SATELLITE_TILE_URL` to the tile URL above.
- Set `SATELLITE_ATTRIBUTION` to the credit as HTML, since Leaflet's attribution control renders it:
  `'<a href="https://cloudless.eox.at">EOxCloudless</a> by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2016)'`
- Rewrite the doc comment above `SATELLITE_ATTRIBUTION`: it is now EOX's required credit for Sentinel-2 cloudless.

`client/src/map/SatelliteTiles.tsx`: update the doc comment only (it names Esri World Imagery and Esri's terms). The component itself does not change. Keep the constant and component names: "satellite" is still accurate, and every map and test imports them.

The map tests (`TripointMap.test.tsx`, `CoastlineMap.test.tsx` and the others) compare against the two constants, not against literal strings, so they need no edits.

Check: `bash scripts/check-frontend.sh src/map src/games`.

## Step 3: the footer credit

`client/src/components/HomePage.tsx`, the footer near line 150. Replace

```
Satellite imagery: <a href="https://www.esri.com/">Esri</a>, Maxar, Earthstar Geographics ·{" "}
```

with a credit that carries the same four things CC BY 4.0 needs (who, the source link, the Copernicus notice, the licence):

```
Satellite imagery: <a href="https://cloudless.eox.at">EOxCloudless</a> by EOX IT Services GmbH,
contains modified Copernicus Sentinel data 2016{" "}
(<a href="https://creativecommons.org/licenses/by/4.0/">CC BY 4.0</a>) ·{" "}
```

Match the surrounding JSX for whitespace: the Wikipedia credit two lines above is the model for a name followed by a licence link in brackets.

`client/src/components/HomePage.test.tsx`, the test "credits the data, imagery and icons, and links to the software licences":

- Replace the `href("Esri")` assertion with `expect(href("EOxCloudless")).toBe("https://cloudless.eox.at");`.
- Add `expect(href("CC BY 4.0")).toBe("https://creativecommons.org/licenses/by/4.0/");`. The existing `"CC BY-SA 4.0"` link is a different accessible name, so the two don't clash.
- Add `expect(footer.queryByText(/Esri/)).not.toBeInTheDocument();` next to the existing OpenStreetMap one.

Check: `bash scripts/check-frontend.sh src/components`.

## Step 4: the other mentions

- `client/scripts/third-party-licenses.mjs`, in the `header` array: change `Esri` to `EOX` in the "Data credits (...)" line.
- `client/CLAUDE.md`, the **Credits** section: replace "Esri (satellite imagery)" with "EOX (Sentinel-2 cloudless satellite imagery, the 2016 layer, CC BY 4.0)". Rewrite the sentence that starts "Esri's terms also need its credit on the map itself" so it says the CC BY licence needs the credit on the map itself. Keep the rule that follows it (always use `map/SatelliteTiles`, never a bare `TileLayer`). Add one sentence: later years of the same mosaic are non-commercial, so don't change the layer year without checking its licence.

Then search the whole repo (not only `client/`) for `Esri`, `esri`, `arcgis`, `Maxar` and `Earthstar`. The only hits allowed are in this plan file and in the generated `client/public/third-party-licenses.txt` (git-ignored; it is rewritten by the next `npm run build` or `npm run dev`).

## Step 5: check everything

1. `bash scripts/check-frontend.sh` (lint, the theme check, `tsc`, all Vitest tests).
2. Nothing else needs to run: no Java changed, and the e2e tests block every non-local request, so they never load a tile from either provider.

If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Report to the user

Say what changed, and list these for the user to look at in a browser, since you can't judge imagery yourself (`npm run dev` in `client/` with the backend running):

- Rivers on the Map and River Ends: these show the imagery while playing. Is the river still visible enough at the locked zoom?
- Tripoint at its closest zoom (10) and Lake Shapes after a round (up to 12): is the imagery sharp enough?
- The credit in the bottom-right corner of a satellite map reads correctly and its link works.

Also pass on these two points, which are decisions for the user and not for this plan:

- EOX calls the public endpoint a free service "provided as is without any guarantee" and applies rate limiting under heavy load. If tiles start failing in production, the options are EOX's paid service or hosting the 2016 tiles ourselves.
- The footer's privacy note does not yet say that map tiles are loaded from a third party (now EOX, in Austria). That belongs with the privacy page from the compliance review.

## Not in this plan

- A privacy page, or any change to the privacy sentences in the footer.
- Proxying or self-hosting tiles.
- Changing any map's zoom limits, colours or layout.
- Moving the credits into a footer shared by every page.
