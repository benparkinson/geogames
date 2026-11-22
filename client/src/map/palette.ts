/**
 * Every colour a Leaflet layer or map pin uses. Leaflet styles these from JavaScript, so they can't be
 * CSS tokens, and they don't follow light/dark. This is the only file with colour literals besides
 * `theme/tokens.css`.
 */
export const MAP_PALETTE = {
  /** The answer's outline while playing. */
  answer: "LightGoldenRodYellow",
  /** The answer's outline once the player gave up. */
  revealed: "darkred",
  /** Clues, hint lines, minor rivers and lakes. */
  clue: "#4a6fa5",
  /** The merged shape of two countries. */
  merged: "#2f7a6b",
  /** The border a hint draws over the merged shape. */
  hiddenBorder: "#f4f1de",
  /** City dots. */
  city: "#4a6fa5",
  /** The capital's dot. */
  capital: "#d9822b",
  /** The capital's outline and every pin's outline and glyph. */
  white: "#ffffff",
  /** Pin: where a river starts. */
  sourcePin: "#1e9e4a",
  /** Pin: where a river ends. */
  mouthPin: "#d62828",
  /** Pin: the country to find in Country Gaps. */
  targetPin: "#1d6fd6",
  /** Pin: the tripoint. */
  tripointPin: "#7a3e9d",
} as const;
