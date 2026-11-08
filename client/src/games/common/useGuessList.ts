import { useMemo } from "react";
import { usePlaceNames, type PlaceKind } from "../../api/placeNames";
import { normaliseString } from "../../helper/stringHelper";

/** Autocomplete names and a spell check for a family of answers, from GET /api/<kind>. */
export function useGuessList(kind: PlaceKind): {
  /** Display names, sorted and de-duplicated. */
  suggestions?: string[];
  /** Whether the guess is a name or alias, via normaliseString. */
  isKnownGuess?: (guess: string) => boolean;
} {
  const { data } = usePlaceNames(kind);

  return useMemo(() => {
    if (!data) {
      return {};
    }
    const suggestions = [...new Set(data.map((place) => place.name))].sort((a, b) => a.localeCompare(b));
    const known = new Set<string>();
    data.forEach((place) => {
      known.add(normaliseString(place.name));
      place.additionalNames.forEach((alias) => known.add(normaliseString(alias)));
    });
    return { suggestions, isKnownGuess: (guess: string) => known.has(normaliseString(guess)) };
  }, [data]);
}
