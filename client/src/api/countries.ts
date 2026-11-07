import { useQuery } from "@tanstack/react-query";
import { fetchJson } from "./http";
import { queryKeys } from "./queryKeys";

export interface CountryName {
  name: string;
  additionalNames: string[];
}

export function useCountryNames() {
  return useQuery<CountryName[]>({
    queryKey: queryKeys.countries,
    queryFn: () => fetchJson<CountryName[]>("/api/countries", "load countries"),
    staleTime: Infinity,
    gcTime: Infinity,
    retry: 1,
  });
}
