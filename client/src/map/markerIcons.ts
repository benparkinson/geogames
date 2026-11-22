import L from "leaflet";
import { MAP_PALETTE } from "./palette";

/**
 * Map markers: one pin shape whose white glyph says what it marks. The tip of the pin is the point,
 * so the place itself stays visible beneath it.
 */

const WHITE = MAP_PALETTE.white;
const WIDTH = 28;
const HEIGHT = 36;

/** A teardrop pin with its round head centred on (14, 13) and its tip at (14, 34). */
const PIN_PATH = "M14 34C14 34 3 22.5 3 13a11 11 0 0 1 22 0c0 9.5-11 21-11 21z";

function pinIcon(colour: string, glyph: string): L.DivIcon {
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}" aria-hidden="true">` +
    `<path d="${PIN_PATH}" fill="${colour}" stroke="${WHITE}" stroke-width="1.5" stroke-linejoin="round"/>` +
    glyph +
    `</svg>`;
  return L.divIcon({
    html: svg,
    className: "map-marker",
    iconSize: [WIDTH, HEIGHT],
    // The tip of the pin.
    iconAnchor: [14, 34],
    tooltipAnchor: [12, -20],
  });
}

/** A drop of water: a spring. */
const DROPLET = `<path d="M14 6.5c-2.6 3.2-4.4 5.7-4.4 8.1a4.4 4.4 0 0 0 8.8 0c0-2.4-1.8-4.9-4.4-8.1z" fill="${WHITE}"/>`;

/** Two waves: the sea or lake the river flows into. */
const WAVES =
  `<path d="M7.5 11.2q1.6-1.6 3.25 0t3.25 0 3.25 0 3.25 0M7.5 15.6q1.6-1.6 3.25 0t3.25 0 3.25 0 3.25 0" ` +
  `fill="none" stroke="${WHITE}" stroke-width="1.8" stroke-linecap="round"/>`;

/** A question mark: name what is here. */
const QUESTION =
  `<path d="M10.6 10.4a3.4 3.4 0 1 1 5 3c-1.1.6-1.6 1.2-1.6 2.3v.4" fill="none" stroke="${WHITE}" stroke-width="2.2" stroke-linecap="round"/>` +
  `<circle cx="14" cy="19.4" r="1.4" fill="${WHITE}"/>`;

/** Three borders meeting at one point. */
const THREE_WAYS =
  `<path d="M14 13V5.8M14 13l-6.2 3.6M14 13l6.2 3.6" fill="none" stroke="${WHITE}" stroke-width="2" stroke-linecap="round"/>` +
  `<circle cx="14" cy="13" r="2.2" fill="${WHITE}"/>`;

/** Green pin with a droplet: where a river starts. */
export const sourceIcon = pinIcon(MAP_PALETTE.sourcePin, DROPLET);

/** Red pin with waves: where a river ends. */
export const mouthIcon = pinIcon(MAP_PALETTE.mouthPin, WAVES);

/** Blue pin with a question mark: marks the country to find in Country Gaps, inside the gap. */
export const targetIcon = pinIcon(MAP_PALETTE.targetPin, QUESTION);

/** Plum pin with three lines meeting: the tripoint, where three countries' borders meet. */
export const tripointIcon = pinIcon(MAP_PALETTE.tripointPin, THREE_WAYS);
