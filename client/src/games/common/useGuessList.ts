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
  /** True while the list is still being fetched: a guess can't be checked yet. A failed fetch isn't loading. */
  loading: boolean;
} {
  const { data, isPending } = usePlaceNames(kind);

  return useMemo(() => {
    if (!data) {
      return { loading: isPending };
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
    return { suggestions, isKnownGuess: (guess: string) => known.has(normaliseString(guess)), loading: false };
  }, [data, isPending]);
}
