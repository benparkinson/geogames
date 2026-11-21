import { firstLetterHint as firstLetter, nameLengthHint as nameLength } from "../common/hints";
import type { City, HasCountry } from "./model";

/** A hint builder for a round with a country: the other rounds' builders only need what they use. */
export type HintBuilder<T extends HasCountry = HasCountry> = (round: T) => string | null;

/**
 * Names the neighbour with the longest border (neighbours are sorted longest first). It prefers a mainland neighbour,
 * since the map shows the mainland, and falls back to the first of any kind: the United Kingdom's one land border is
 * Northern Ireland's.
 */
export const longestBorderHint: HintBuilder = ({ country }) => {
  const longest = country.neighbours.find((n) => n.mainland) ?? country.neighbours[0];
  return longest ? `Longest border: ${longest.name}` : null;
};

export const firstLetterHint: HintBuilder = ({ country }) => firstLetter(country.name);

/** How long the country's name is, in letters and words. */
export const nameLengthHint: HintBuilder = ({ country }) => nameLength(country.name);

/** Says the coastline is on the map, or, for a landlocked country, that it has none, which is itself a clue. */
export const coastHint: HintBuilder = ({ country }) =>
  country.coastKm > 0 ? "Its coastline is on the map" : "It has no coastline";

/** For the hint that highlights the capital. A few countries have more than one. */
export const capitalHint: HintBuilder<HasCountry & { cities: City[] }> = ({ cities }) =>
  cities.filter((city) => city.capital).length > 1 ? "Its capitals are highlighted" : "Its capital is highlighted";

/**
 * Names the country's longest river. The name helps even when the player doesn't know the river, because its language
 * narrows the answer down. Falls back to the length of the country's name when it has no named river.
 */
export const longestRiverHint: HintBuilder<HasCountry & { longestRiver: string | null }> = (round) =>
  round.longestRiver ? `Its longest river is the ${round.longestRiver}` : nameLengthHint(round);
