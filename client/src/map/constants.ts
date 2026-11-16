import type { PathOptions } from "leaflet";

export const SATELLITE_TILE_URL =
  "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless_3857/default/g/{z}/{y}/{x}.jpg";

/** EOX's required credit for Sentinel-2 cloudless (HTML, shown by Leaflet's attribution control). */
export const SATELLITE_ATTRIBUTION =
  '<a href="https://cloudless.eox.at">EOxCloudless</a> by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2016)';

const ANSWER_COLOR = "LightGoldenRodYellow";
const REVEALED_COLOR = "darkred";

/** Outline style for an answer: one colour while playing, another once the player gave up. */
export function answerPathOptions(gaveUp: boolean): PathOptions {
  return { color: gaveUp ? REVEALED_COLOR : ANSWER_COLOR };
}

const CLUE_COLOR = "#4a6fa5";

/** Outline style for a clue shown before the answer: distinct from the answer colours. */
export function clueOutlineOptions(): PathOptions {
  return { color: CLUE_COLOR, weight: 2, fillColor: CLUE_COLOR, fillOpacity: 0.25 };
}

/** Style for clue lines (a coastline or land borders) shown while playing. */
export function clueLineOptions(): PathOptions {
  return { color: CLUE_COLOR, weight: 3, fill: false };
}

/** Style for lines a hint adds: dashed and fainter than the clue itself. */
export function hintLineOptions(): PathOptions {
  return { color: CLUE_COLOR, weight: 3, fill: false, dashArray: "6 6", opacity: 0.7 };
}
