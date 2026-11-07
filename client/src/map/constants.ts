import type { PathOptions } from "leaflet";

export const SATELLITE_TILE_URL =
  "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}";

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
