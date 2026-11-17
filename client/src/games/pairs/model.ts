import type { Bounds, CountryOutline, CountryShape } from "../countries/model";
import type { LineGeoData } from "../common/country";

/** The data of a round of a pair game: two neighbouring countries and the border between them. */
export interface CountryPairRound {
  /** The two countries, sorted by name. */
  countries: CountryShape[];
  /** The lines the two countries share. */
  sharedBorder: LineGeoData;
  /** The length of the shared border, in whole kilometres. */
  borderKm: number;
  /** Merged Countries only: the mainland neighbours of either country except the pair. Empty otherwise. */
  neighbours: CountryOutline[];
  /** The union of the two countries' focus boxes: what to fit the map to once the whole pair matters. */
  focus: Bounds;
  /** The bounding box of the shared border. */
  borderBounds: Bounds;
}
