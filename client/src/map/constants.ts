import type { PathOptions } from "leaflet";
import { MAP_PALETTE } from "./palette";

/** The self-hosted relief tiles (`public/tiles/relief`, served from the site root). */
export const RELIEF_TILE_URL = "/tiles/relief/{z}/{x}/{y}.jpg";

/** The tiles stop at zoom 6; Leaflet stretches them at deeper zooms. */
export const RELIEF_MAX_NATIVE_ZOOM = 6;

const ANSWER_COLOR = MAP_PALETTE.answer;
const REVEALED_COLOR = MAP_PALETTE.revealed;

/** Outline style for an answer: one colour while playing, another once the player gave up. */
export function answerPathOptions(gaveUp: boolean): PathOptions {
  return { color: gaveUp ? REVEALED_COLOR : ANSWER_COLOR };
}

/** Outline style for an answer that is a filled area, such as a lake. */
export function answerFillOptions(gaveUp: boolean): PathOptions {
  return { ...answerPathOptions(gaveUp), fillOpacity: 0.35 };
}

const CLUE_COLOR = MAP_PALETTE.clue;

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

const MERGED_COLOR = MAP_PALETTE.merged;
const HIDDEN_BORDER_COLOR = MAP_PALETTE.hiddenBorder;

/**
 * Style for the merged shape of two countries. The fill is opaque and the stroke is the same colour:
 * two adjacent polygons filled without a stroke would show a hairline seam where they meet, which
 * would give the border away.
 */
export function mergedShapeOptions(): PathOptions {
  return { color: MERGED_COLOR, weight: 2, opacity: 1, fillColor: MERGED_COLOR, fillOpacity: 1 };
}

/** Style for the border a hint draws over the merged shape: light, so it contrasts with the fill. */
export function hiddenBorderOptions(): PathOptions {
  return { color: HIDDEN_BORDER_COLOR, weight: 3, fill: false };
}

/** The smallest and largest radius, in pixels, of a city dot. */
export const CITY_MIN_RADIUS = 3;
export const CITY_MAX_RADIUS = 14;

/**
 * The radius of the dot for a city: the area follows the population, so the radius follows its square
 * root, between the minimum and the maximum size. `largest` is the biggest population in the round.
 */
export function cityRadius(population: number, largest: number): number {
  if (largest <= 0 || population <= 0) {
    return CITY_MIN_RADIUS;
  }
  const share = Math.min(1, Math.sqrt(population / largest));
  return CITY_MIN_RADIUS + (CITY_MAX_RADIUS - CITY_MIN_RADIUS) * share;
}

const CITY_COLOR = MAP_PALETTE.city;
const CAPITAL_COLOR = MAP_PALETTE.capital;

/** Style for a city dot while playing. */
export function cityDotOptions(): PathOptions {
  return { color: CITY_COLOR, weight: 1, fillColor: CITY_COLOR, fillOpacity: 0.55 };
}

/** Style for the capital's dot once a hint highlights it: a different colour, opaque and outlined in white. */
export function capitalDotOptions(): PathOptions {
  return { color: MAP_PALETTE.white, weight: 2, fillColor: CAPITAL_COLOR, fillOpacity: 1 };
}

/** Style for the minor rivers of Country Rivers: thinner than the clue line the major rivers use. */
export function minorRiverOptions(): PathOptions {
  return { color: CLUE_COLOR, weight: 1.25, fill: false };
}

/** Style for a lake that a hint draws: filled more strongly than a clue outline, so it reads as water. */
export function lakeFillOptions(): PathOptions {
  return { color: CLUE_COLOR, weight: 1, fillColor: CLUE_COLOR, fillOpacity: 0.5 };
}
