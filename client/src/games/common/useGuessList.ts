import { useMemo } from "react";
import { usePlaceNames, type PlaceKind } from "../../api/placeNames";
import { normaliseString } from "../../helper/stringHelper";
import type { Suggestion } from "./suggestions";

/** Autocomplete names and a spell check for a family of answers, from GET /api/<kind>. */
export function useGuessList(kind: PlaceKind): {
  /** One entry per display name, sorted, with the aliases of every place that has the name. */
  suggestions?: Suggestion[];
  /** Whether the guess is a name or alias, via normaliseString. */
  isKnownGuess?: (guess: string) => boolean;
} {
  const { data } = usePlaceNames(kind);

  return useMemo(() => {
    if (!data) {
      return {};
    }
    const byName = new Map<string, Set<string>>();
    data.forEach((place) => {
      const aliases = byName.get(place.name) ?? new Set<string>();
      place.additionalNames.forEach((alias) => {
        if (normaliseString(alias) !== normaliseString(place.name)) aliases.add(alias);
      });
      byName.set(place.name, aliases);
    });
    const suggestions: Suggestion[] = [...byName.entries()]
      .map(([name, aliases]) => ({ name, aliases: [...aliases] }))
      .sort((a, b) => a.name.localeCompare(b.name));
    const known = new Set<string>();
    data.forEach((place) => {
      known.add(normaliseString(place.name));
      place.additionalNames.forEach((alias) => known.add(normaliseString(alias)));
    });
    return { suggestions, isKnownGuess: (guess: string) => known.has(normaliseString(guess)) };
  }, [data]);
}
