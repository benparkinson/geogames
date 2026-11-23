import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { fetchJson } from "./http";
import { queryKeys } from "./queryKeys";

/** A family of answers with a list endpoint: GET /api/<kind>. */
export type PlaceKind = "countries" | "rivers" | "lakes";

export interface PlaceName {
  name: string;
  additionalNames: string[];
}

/**
 * A place with the position distance clues are measured from. Countries always have one; rivers and lakes
 * may not (an older backend), and then give no clue.
 */
export interface LocatedPlaceName extends PlaceName {
  lat?: number;
  lon?: number;
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
 * The places of this kind with their positions. Shares its query, and so its single fetch, with
 * `usePlaceNames(kind)`. With `enabled` false it fetches nothing.
 */
export function useLocatedPlaceNames(kind: PlaceKind, enabled = true): UseQueryResult<LocatedPlaceName[]> {
  return useQuery<LocatedPlaceName[]>({ ...placeNamesQuery<LocatedPlaceName>(kind), enabled });
}
