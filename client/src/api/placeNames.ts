import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { fetchJson } from "./http";
import { queryKeys } from "./queryKeys";

/** A family of answers with a list endpoint: GET /api/<kind>. */
export type PlaceKind = "countries" | "rivers" | "lakes";

export interface PlaceName {
  name: string;
  additionalNames: string[];
}

/** The names (and aliases) of every place of this kind, for autocomplete and spell checking. */
export function usePlaceNames(kind: PlaceKind): UseQueryResult<PlaceName[]> {
  return useQuery<PlaceName[]>({
    queryKey: queryKeys.placeNames(kind),
    queryFn: () => fetchJson<PlaceName[]>(`/api/${kind}`, `load ${kind}`),
    staleTime: Infinity,
    gcTime: Infinity,
    retry: 1,
  });
}
