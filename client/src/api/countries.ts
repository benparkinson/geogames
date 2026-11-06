import { useQuery } from "@tanstack/react-query";
import { serverEndpoint } from "./gateway";

export interface CountryName {
  name: string;
  additionalNames: string[];
}

export function useCountryNames() {
  return useQuery<CountryName[]>({
    queryKey: ["countries"],
    queryFn: async () => {
      const response = await fetch(serverEndpoint() + "/api/countries");
      if (!response.ok) {
        throw new Error("Failed to load countries (" + response.status + ")");
      }
      return response.json();
    },
    staleTime: Infinity,
    gcTime: Infinity,
    retry: 1,
  });
}
