import { useMemo } from "react";
import { useCountryNames } from "../../api/placeNames";
import { normaliseString } from "../../helper/stringHelper";
import { bearingDegrees, distanceKm, type LatLon } from "./distance";
import type { GuessClue } from "./guessReducer";

/**
 * Builds `clueFor` for a game whose answers are countries: how far the guessed country is from the
 * nearest answer still to find, and which way to go. Undefined until the country list has loaded,
 * so a game that opts in has no clues until then. Everything is worked out in the browser.
 */
export function useCountryClues(): ((guess: string, targets: string[]) => GuessClue | null) | undefined {
  const { data } = useCountryNames();

  return useMemo(() => {
    if (!data) {
      return undefined;
    }
    const positions = new Map<string, LatLon>();
    for (const country of data) {
      const position = { lat: country.lat, lon: country.lon };
      positions.set(normaliseString(country.name), position);
      country.additionalNames.forEach((alias) => positions.set(normaliseString(alias), position));
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
            console.error(`The answer "${target}" is not in the country list, so it has no position.`);
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
  }, [data]);
}
