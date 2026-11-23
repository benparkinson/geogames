import { useMemo } from "react";
import { useLocatedPlaceNames, type PlaceKind } from "../../api/placeNames";
import { normaliseString } from "../../helper/stringHelper";
import { bearingDegrees, distanceKm, type LatLon } from "./distance";
import type { GuessClue } from "./guessReducer";

const SINGULAR: Record<PlaceKind, string> = { countries: "country", rivers: "river", lakes: "lake" };

/**
 * Builds `clueFor` for a game whose answers are countries, rivers or lakes: how far the guessed place is from the
 * nearest answer still to find, and which way to go. Undefined until the list of places has loaded,
 * so a game that opts in has no clues until then, and always undefined when `enabled` is false (the game
 * has no clues, so the list isn't fetched for it, even if it is already cached). Everything is worked out
 * in the browser.
 */
export function usePlaceClues(
  kind: PlaceKind,
  enabled = true,
): ((guess: string, targets: string[]) => GuessClue | null) | undefined {
  const { data } = useLocatedPlaceNames(kind, enabled);

  return useMemo(() => {
    if (!enabled || !data) {
      return undefined;
    }
    const positions = new Map<string, LatLon>();
    for (const place of data) {
      if (place.lat === undefined || place.lon === undefined) {
        continue;
      }
      const position = { lat: place.lat, lon: place.lon };
      positions.set(normaliseString(place.name), position);
      place.additionalNames.forEach((alias) => positions.set(normaliseString(alias), position));
    }

    return (guess: string, targets: string[]): GuessClue | null => {
      const from = positions.get(normaliseString(guess));
      if (!from) {
        return null;
      }
      let best: GuessClue | null = null;
      for (const target of targets) {
        const to = positions.get(normaliseString(target));
        if (!to) {
          if (process.env.NODE_ENV === "development") {
            console.error(`The answer "${target}" is not in the ${SINGULAR[kind]} list, so it has no position.`);
          }
          continue;
        }
        const km = distanceKm(from, to);
        if (!best || km < best.km) {
          best = { km, bearing: bearingDegrees(from, to) };
        }
      }
      return best;
    };
  }, [data, enabled, kind]);
}
