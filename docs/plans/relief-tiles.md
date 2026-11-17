# Plan: replace the EOX satellite imagery with self-hosted Natural Earth relief tiles

Read `CLAUDE.md`, `client/CLAUDE.md` and `src/main/java/com/parkinson/benjamin/geogames/loaders/CLAUDE.md` first.

Work on a new branch `relief-tiles` from an up-to-date `main`. Do not commit, push or open a PR unless the user asks.

## Goal

Every map's background tiles come from EOX's free Sentinel-2 service (`tiles.maps.eox.at`). That service is a demo "without any guarantee", rate-limits under load, and each tile request sends the visitor's IP address to EOX. Replace it with the **Natural Earth II** raster (land cover, shaded relief, water and drainages). It is public domain and has no labels. Cut it into map tiles once, offline, and serve them from the site itself (`client/public/tiles/relief/`). After this plan:

- No browser request leaves the site for map tiles, and no file outside `docs/plans/` mentions EOX, EOxCloudless, Sentinel or Copernicus.
- The tile layer component is `ReliefTiles`, not `SatelliteTiles`, and the user-facing text says "relief map" where it said "satellite map".
- The backend, the API, the contract shapes and the game logic do not change.

## Facts about the source

Checked on 2026-11-17.

- **File:** `https://naciscdn.org/naturalearth/10m/raster/NE2_HR_LC_SR_W_DR.zip` (Natural Earth II with Shaded Relief, Water and Drainages, high resolution, 325,795,333 bytes; it answered `200`).
- **Raster:** a GeoTIFF of 21,600 × 10,800 pixels in plain longitude/latitude (equirectangular, EPSG:4326), covering −180…180 and −90…90. A pixel is 1/60° (about 1.85 km at the equator). Step 1 confirms these numbers from the `.tfw` file in the zip.
- **Licence:** Natural Earth's terms say its raster and vector data are in the public domain. No credit is required. The maps already show "Map data: Natural Earth" (`map/MapCredits`), and the footer links Natural Earth.
- **Useful zoom:** Web Mercator zoom 6 is 16,384 pixels around the world, a little coarser than the source (21,600). So tiles are made for **zooms 0–6 only**. Leaflet stretches the zoom 6 tiles at deeper zooms (`maxNativeZoom`). The maps keep their current zoom limits (up to 10, and 12 for Lake Shapes). Past zoom 7 the background gets soft; that is accepted.
- **No labels.** The raster has no names, so Tripoint (the only game that shows the tiles while playing) gives nothing away. The drainage lines are unnamed rivers, which is fine.
- **Tools:** this machine has no GDAL, no Docker and no usable Python. The tiles are cut by a small Java program, like the other offline loaders. It uses only the JDK (`javax.imageio` reads TIFF since Java 9).

## Step 1: download and check the source (the only step that uses the network)

1. Download the zip once into `geodata/relief/` and unzip it there. Use `curl -L -o` and `unzip`, or PowerShell `Expand-Archive`.
2. Add `geodata/relief/` to `.gitignore`: the zip and the TIFF are too big to commit. Add `client/public/tiles/` to `.ignore` (the ripgrep ignore file) under a comment like the others.
3. Record the SHA-256 of the zip (`sha256sum`). Step 6 puts it in the loaders `CLAUDE.md`.
4. Read the `.tfw` file. Its six lines should be 0.0166666…, 0, 0, −0.0166666…, about −179.99 and about 89.99 (the centre of the top-left pixel).
5. Open `https://www.naturalearthdata.com/about/terms-of-use/` and check it still says the raster data is public domain.

**Stop and report to the user with the output** if the download fails, the raster is not 21,600 × 10,800 pixels of plain lon/lat, or the terms are no longer public domain. Do not pick another file yourself.

## Step 2: the tile cutter (Java, offline)

Create the package `com.parkinson.benjamin.geogames.loaders.relief`:

- **`ReliefTiler`:** pure, testable logic.
  - `static double[] tilePixelToLonLat(int z, double globalX, double globalY)`. Web Mercator, 256-pixel tiles, `y = 0` at the top (north). It works like this:
    - `n = 256 · 2^z`
    - `lon = globalX / n · 360 − 180`
    - `lat = toDegrees(atan(sinh(π · (1 − 2 · globalY / n))))`
  - `static double[] lonLatToSourcePixel(double lon, double lat, int width, int height)`:
    - `col = (lon + 180) / 360 · width − 0.5`
    - `row = (90 − lat) / 180 · height − 0.5`
  - Sampling: bilinear, wrapping in longitude and clamping in latitude. To avoid aliasing at low zooms, average a k × k grid of bilinear samples inside each output pixel, where `k = max(1, ceil(width / (256 · 2^z)))`. That is 1 at zoom 6 and about 85 at zoom 0. Every zoom is rendered straight from the source; no tile is built from other tiles.
  - `void writeTiles(<source image>, Path outDir, int maxZoom)`. It writes every tile of zooms `0..maxZoom` as `outDir/{z}/{x}/{y}.jpg`: the x folder, then the y file, which is Leaflet's default `{z}/{x}/{y}` order. It writes them as JPEG with quality 0.85 (`ImageWriter` with `ImageWriteParam.MODE_EXPLICIT`). The output must be byte-for-byte deterministic: the same input gives the same files.
  - For speed, copy the source into one `byte[]` of RGB once (read the raster with `getDataElements` or the `DataBufferByte`), rather than calling `getRGB` per pixel. Zoom 6 alone is 268 million output pixels.
- **`ReliefTilesMain <source.tif> <outDir> <maxZoom>`:**
  - Deletes `outDir` if it exists (only that folder; refuse to run if `outDir` is not a folder named `relief`).
  - Reads the TIFF with `ImageIO`.
  - Checks it is 2:1, writes the tiles and prints a summary: the tile count per zoom, the total count and the total size in MB.

**Tests** in `src/test/java/.../loaders/relief/`, with the same style as the other loader tests. The JaCoCo gate counts the loaders, so the new code needs real coverage:

- `tilePixelToLonLat`:
  - zoom 0, pixel (128, 128) is (0, 0)
  - (0, 0) is (−180, about 85.0511)
  - zoom 1, pixel (256, 256) is (0, 0)
- `lonLatToSourcePixel`: the corners and the centre of a 360 × 180 image.
- A synthetic source of 360 × 180 pixels, with the western half one colour and the eastern half another, cut to zoom 1:
  - five files exist (`0/0/0.jpg`, `1/0/0.jpg`, `1/0/1.jpg`, `1/1/0.jpg` and `1/1/1.jpg`)
  - the zoom-1 tiles of x = 0 are the western colour and those of x = 1 the eastern one (allow a JPEG tolerance of about 10 per channel)
  - cutting twice gives identical bytes.
- `ReliefTilesMain`:
  - the refusal when `outDir` is not named `relief`
  - a run on a small TIFF written by the test with `ImageIO`, checking the summary and the files.

Check: `bash scripts/check-java.sh -Dtest='Relief*'`, then `bash scripts/check-java.sh` for the gate.

## Step 3: cut the tiles

Build the classes (`./mvnw -q -DskipTests -Dskip.frontend compile`), then run:

```
java -Xmx4g -cp target/classes com.parkinson.benjamin.geogames.loaders.relief.ReliefTilesMain \
  geodata/relief/NE2_HR_LC_SR_W_DR.tif client/public/tiles/relief 6
```

It should write 5,461 tiles (1 + 4 + 16 + … + 4,096). Then:

- **Size budget.** Report the total from the summary. If it is over 80 MB, **stop and ask the user** before going on. Lowering the JPEG quality or stopping at zoom 5 are the options; don't choose one yourself. Every byte is committed to git and goes into the jar and the Render static site.
- Open `client/public/tiles/relief/0/0/0.jpg`, one zoom-3 tile over Europe (`3/4/2.jpg`) and one zoom-6 tile with land (for example `6/33/22.jpg`, the Alps) with the Read tool. Check that north is up, the land is in the right place (not mirrored or shifted by half a world) and the colours look natural.

The tiles are committed: the source is not in the repo and CI has no network, so they can't be made during the build. This program is **not** added to the `regenerate-data` profile or `check-processed-data.sh`, for the same reason.

## Step 4: the frontend

`client/src/map/constants.ts`:
- Replace `SATELLITE_TILE_URL` with `RELIEF_TILE_URL = "/tiles/relief/{z}/{x}/{y}.jpg"`. It's a root-relative path: the Next dev server, the static export on Render and the jar all serve `public/` at the root.
- Add `RELIEF_MAX_NATIVE_ZOOM = 6`, with a comment saying the tiles stop at zoom 6 and Leaflet stretches them beyond that.
- Delete `SATELLITE_ATTRIBUTION`. Natural Earth is already credited on every map by `MapCredits`, so the tile layer needs no attribution of its own.

`client/src/map/SatelliteTiles.tsx`:
- Rename it to `ReliefTiles.tsx`, exporting `ReliefTiles`. It renders `<TileLayer url={RELIEF_TILE_URL} maxNativeZoom={RELIEF_MAX_NATIVE_ZOOM} />`.
- Rewrite its doc comment: Natural Earth II relief, public domain, self-hosted under `public/tiles/relief`, made by `ReliefTilesMain`, no labels.

`client/src/map/RevealTiles.tsx`: use `ReliefTiles`, and change the comment to "The relief map, shown once the round is over. Its tiles carry no labels."

`client/src/games/tripoint/TripointMap.tsx`: import and render `ReliefTiles`.

`client/src/test/leafletMock.tsx`: give the mock `TileLayer` a `maxNativeZoom?: number` prop and render it as `data-max-native-zoom`. Add a small `client/src/map/ReliefTiles.test.tsx` that checks the URL, the max native zoom and that no attribution is set.

Map tests (`TripointMap`, `CoastlineMap`, `CountryCitiesMap`, `CountryRiversMap`, `LandBordersMap`, `NeighboursMap`, `LakeShapesMap`, `BorderShapesMap`, `MergedCountriesMap`, `RiverCountriesMap`, `RiverShapesMap`):
- Import `RELIEF_TILE_URL` instead of `SATELLITE_TILE_URL`.
- Where a test checked `drawnTileAttributions()` against `SATELLITE_ATTRIBUTION` (Tripoint, Coastline), expect `[""]` instead.
- Rename "satellite imagery" / "satellite map" to "relief map" in the test names.

User-facing text and comments:
- `client/src/games/rivers/shapes.ts`: the caption becomes "Shape only. The relief map appears when you answer." Update `CAPTION` in `RiverGame.test.tsx` to match.
- Replace "satellite" with "relief" in the comments of:
  - `CountryCitiesMap.tsx`, `CountryRiversMap.tsx`, `LakeShapesMap.tsx` and `RiverCountriesMap.tsx`
  - the test names in `LakeGame.test.tsx` and `RiverGame.test.tsx`
  - `client/scripts/check-theme.mjs` (the comment on line 11)
  - `src/main/java/.../helper/CountryPairEligibility.java`, the `MIN_BORDER_KM` comment: the map would have to zoom in so far that the relief map at the end is a blur.

Then `git grep -n -i -E "satellite|eox|sentinel|copernicus|cloudless" -- ':!docs/plans'` must print nothing.

Check: `bash scripts/check-frontend.sh`.

## Step 5: credits and privacy

`client/src/components/SiteFooter.tsx`:
- Delete the two "Satellite imagery: … (CC BY 4.0) ·" lines.
- Change `Map data:` to `Maps and relief:` before the Natural Earth link. Nothing else in the footer changes.

`SiteFooter.test.tsx`:
- Remove the `EOxCloudless` and `CC BY 4.0` assertions.
- Add `expect(footer().queryByText(/EOX/)).not.toBeInTheDocument();` next to the Esri one.

`client/src/components/PrivacyPage.tsx`, "Who else receives data":
- Only Render remains. Replace the sentence "Two companies receive yours:" and the list with one paragraph: Render (linked as now, "a United States company") hosts this site, its map images and its game server.
- Keep the Ko-fi paragraph.

`PrivacyPage.test.tsx`:
- Drop the EOX assertion and keep the Render one.
- Add an assertion that the page does not mention EOX.

`client/scripts/third-party-licenses.mjs`: remove `EOX, ` from the "Data credits (...)" header line.

Check: `bash scripts/check-frontend.sh src/components`.

## Step 6: the docs

- **`client/CLAUDE.md`, Credits:**
  - Replace the EOX part with: the relief tiles are Natural Earth II (public domain), self-hosted in `public/tiles/relief/` (zooms 0–6, made by `ReliefTilesMain`, committed) and always drawn with `map/ReliefTiles`, never a bare `TileLayer`.
  - Delete the sentence about later years of the mosaic being non-commercial.
  - Keep the rule that a new data source or a third party receiving browser requests needs a footer credit and a privacy-page line.
  - In the Lakes family entry, change "the satellite map" to "the relief map".
- **Loaders `CLAUDE.md`:**
  - Under Sources, add `geodata/relief/` with the download URL, the date, the SHA-256 from step 1 and the fact that it is git-ignored, not in CI and public domain.
  - Under Programs, add `relief/ReliefTilesMain <source.tif> <outDir> <maxZoom>`: what it writes, that the tiles are committed under `client/public/tiles/relief/`, the command from step 3 (with `-Xmx4g`), and that it is not part of `regenerate-data` because the source is not in the repo.
- **Root `CLAUDE.md`:** under "Working efficiently", add one line saying `client/public/tiles/` holds about 5,500 committed JPEG tiles and is hidden from ripgrep.

## Step 7: check everything

1. `bash scripts/check-frontend.sh`
2. `bash scripts/check-java.sh`
3. `bash scripts/check-it.sh` (it builds the frontend, so `HttpServerIT` runs against an export that includes the tiles)
4. `bash scripts/check-no-skipped-tests.sh`

If a check fails and the fix isn't obvious after two attempts, stop and report to the user with the error output.

## Report to the user

Say what changed, the tile count and the total size, and list these for the user to look at in a browser (`npm run dev` in `client/` with the backend running):

- Tripoint at zoom 4–6 (sharp) and at its closest zoom, 10 (stretched): is it still usable for finding the tripoint?
- Lake Shapes after a round, up to zoom 12: is the blurred background acceptable, or should those maps cap their zoom lower?
- A country reveal (Coastline or Neighbours) and a river reveal: do the outline colours still stand out against the relief? If not, the colours are in `map/constants.ts`.

Also pass on these points, which are decisions for the user and not for this plan:

- The tiles are now served by Render's static site, so they count against its bandwidth. Watch the usage in the Render dashboard. Long cache headers for `/tiles/*` (set in the Render dashboard, not the repo) would cut repeat downloads.
- If the background is too plain or soft, the alternative from the same source is `HYP_HR_SR_W_DR` (hypsometric tints, more atlas-like). Re-running step 3 on it is the only change.

## Not in this plan

- Changing any map's zoom limits, colours or layout.
- Tiles above zoom 6, hillshade from a DEM, or any satellite source.
- Adding the tile cutter to the `regenerate-data` profile or CI.
- Moving the tiles to a separate CDN or bucket.
