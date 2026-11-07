import L from "leaflet";

function flagIcon(colour: string): L.DivIcon {
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">` +
    `<line x1="6" y1="3" x2="6" y2="30" stroke="#222" stroke-width="2.5" stroke-linecap="round"/>` +
    `<path d="M7 4h20l-5 6 5 6H7z" fill="${colour}" stroke="#fff" stroke-width="1.5" stroke-linejoin="round"/>` +
    `</svg>`;
  return L.divIcon({
    html: svg,
    className: "map-flag",
    iconSize: [32, 32],
    // The bottom of the pole.
    iconAnchor: [6, 30],
    tooltipAnchor: [12, -14],
  });
}

/** Green flag: where a river starts. */
export const startFlagIcon = flagIcon("#1e9e4a");

/** Red flag: where a river ends. */
export const endFlagIcon = flagIcon("#d62828");
