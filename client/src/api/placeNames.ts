import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { fetchJson } from "./http";
import { queryKeys } from "./queryKeys";

/** A family of answers with a list endpoint: GET /api/<kind>. */
export type PlaceKind = "countries" | "rivers" | "lakes";

export interface PlaceName {
  name: string;
  additionalNames: string[];
}

/** A country, with the position distance clues are measured from (the centroid of its largest part). */
export interface CountryName extends PlaceName {
  lat: number;
  lon: number;
}

function placeNamesQuery<T extends PlaceName>(kind: PlaceKind) {
  return {
    queryKey: queryKeys.placeNames(kind),
    queryFn: () => fetchJson<T[]>(`/api/${kind}`, `load ${kind}`),
    staleTime: Infinity,
    gcTime: Infinity,
    retry: 1,
  };
}

/** The names (and aliases) of every place of this kind, for autocomplete and spell checking. */
export function usePlaceNames(kind: PlaceKind): UseQueryResult<PlaceName[]> {
  return useQuery<PlaceName[]>(placeNamesQuery(kind));
}

/**
 * The countries with their positions. Shares its query, and so its single fetch, with `usePlaceNames("countries")`.
 * With `enabled` false it fetches nothing.
 */
export function useCountryNames(enabled = true): UseQueryResult<CountryName[]> {
  return useQuery<CountryName[]>({ ...placeNamesQuery<CountryName>("countries"), enabled });
}
