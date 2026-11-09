import type { PathOptions } from "leaflet";

export const SATELLITE_TILE_URL =
  "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}";

/** Esri's required credit for World Imagery (HTML, shown by Leaflet's attribution control). */
export const SATELLITE_ATTRIBUTION =
  'Powered by <a href="https://www.esri.com/">Esri</a> | Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community';

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
