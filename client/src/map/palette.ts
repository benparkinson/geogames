/**
 * Every colour a Leaflet layer or map pin uses. Leaflet styles these from JavaScript, so they can't be
 * CSS tokens, and they don't follow light/dark. This is the only file with colour literals besides
 * `theme/tokens.css`.
 */
export const MAP_PALETTE = {
  /** The answer's outline while playing. */
  answer: "#1a1a1a",
  /** The answer's outline once the player gave up. */
  revealed: "#d9481c",
  /** Clues, hint lines, minor rivers and lakes. */
  clue: "#1d5c63",
  /** The merged shape of two countries. */
  merged: "#1d5c63",
  /** The border a hint draws over the merged shape. */
  hiddenBorder: "#f7f7f4",
  /** City dots. */
  city: "#1d5c63",
  /** The capital's dot. */
  capital: "#d9481c",
  /** The capital's outline and every pin's outline and glyph. */
  white: "#ffffff",
  /** Pin: where a river starts. */
  sourcePin: "#1d5c63",
  /** Pin: where a river ends. */
  mouthPin: "#d9481c",
  /** Pin: the country to find in Country Gaps. */
  targetPin: "#1a1a1a",
  /** Pin: the tripoint. */
  tripointPin: "#d9481c",
} as const;
